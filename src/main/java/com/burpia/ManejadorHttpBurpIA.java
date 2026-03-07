package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.HttpUtils;
import com.burpia.util.Normalizador;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.FiltroContenidoAnalizable;
import com.burpia.util.DeduplicadorSolicitudes;
import com.burpia.evidence.EvidenceManager;
import com.burpia.util.AlmacenEvidenciaHttp;
import com.burpia.processor.HttpRequestProcessor;
import com.burpia.execution.TaskExecutionManager;
import com.burpia.util.PoliticaMemoria;
import javax.swing.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ManejadorHttpBurpIA implements HttpHandler {
    private static final String ORIGEN_LOG = "BurpIA";
    private static final long INTERVALO_ALERTA_CONFIG_MS = 120000L;
    // TTL específicos por estado de tarea (state-aware retention policy)
    private static final long TTL_CONTEXTO_COMPLETADO_MS = 0L; // Purga inmediata para tareas exitosas
    private static final long TTL_CONTEXTO_REINTENTABLE_MS = 15 * 60 * 1000L; // 15 minutos (aumentado según
                                                                              // recomendación) para ERROR/CANCELADO
    private static final long TTL_CONTEXTO_ACTIVO_MS = Long.MAX_VALUE; // Mantener hasta cambio de estado
    private static final int MAX_CONTEXTO_REINTENTO = 1000;
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final PestaniaPrincipal pestaniaPrincipal;
    private volatile LimitadorTasa limitador;
    private final DeduplicadorSolicitudes deduplicador;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final Object logLock;
    private final boolean esBurpProfessional;
    private volatile boolean capturaActiva;
    private volatile boolean avisoIssuesNoDisponiblesEmitido;

    private final Estadisticas estadisticas;
    private final GestorTareas gestorTareas;
    private final GestorConsolaGUI gestorConsola;
    private final ModeloTablaHallazgos modeloTablaHallazgos;
    private final ControlBackpressureGlobal controlBackpressure;
    private final EvidenceManager evidenceManager;
    private final HttpRequestProcessor httpRequestProcessor;
    private final TaskExecutionManager taskExecutionManager;
    private final Map<ConfiguracionAPI.CodigoValidacionConsulta, Long> alertasConfiguracionEmitidas;

    private static final class ContextoReintento {
        private final SolicitudAnalisis solicitudAnalisis;
        private final String evidenciaId;
        private final long creadoMs;

        private ContextoReintento(SolicitudAnalisis solicitudAnalisis,
                String evidenciaId) {
            this.solicitudAnalisis = solicitudAnalisis;
            this.evidenciaId = evidenciaId;
            this.creadoMs = System.currentTimeMillis();
        }
    }

    public ManejadorHttpBurpIA(MontoyaApi api, ConfiguracionAPI config, PestaniaPrincipal pestaniaPrincipal,
            PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador,
            Estadisticas estadisticas, GestorTareas gestorTareas,
            GestorConsolaGUI gestorConsola, ModeloTablaHallazgos modeloTablaHallazgos) {
        ConfiguracionAPI configSegura = config != null ? config : new ConfiguracionAPI();
        this.api = api;
        this.config = configSegura;
        this.pestaniaPrincipal = pestaniaPrincipal;
        this.stdout = stdout;
        this.stderr = stderr;
        this.deduplicador = new DeduplicadorSolicitudes();
        this.estadisticas = estadisticas;
        this.gestorTareas = gestorTareas;
        this.gestorConsola = gestorConsola;
        this.modeloTablaHallazgos = modeloTablaHallazgos;
        this.controlBackpressure = new ControlBackpressureGlobal();
        this.evidenceManager = new EvidenceManager(api);
        this.httpRequestProcessor = new HttpRequestProcessor(api, configSegura);
        this.taskExecutionManager = new TaskExecutionManager(configSegura, gestorTareas, gestorConsola, pestaniaPrincipal, stdout, stderr, limitador, controlBackpressure);
        this.alertasConfiguracionEmitidas = new ConcurrentHashMap<>();
        Hallazgo.establecerResolutorEvidencia(evidenceManager::obtenerEvidencia);
        this.esBurpProfessional = ExtensionBurpIA.esBurpProfessional(api);
        this.capturaActiva = configSegura.escaneoPasivoHabilitado();
        this.avisoIssuesNoDisponiblesEmitido = false;

        int maxThreads = configSegura.obtenerMaximoConcurrente() > 0 ? configSegura.obtenerMaximoConcurrente() : 10;
        this.limitador = limitador != null ? limitador : new LimitadorTasa(maxThreads);
        this.logLock = new Object();

        registrar(I18nUI.Consola.LOG_MANEJADOR_INICIALIZADO(
                configSegura.obtenerMaximoConcurrente(),
                configSegura.obtenerRetrasoSegundos(),
                configSegura.esDetallado()));
        // EFICIENCIA: Solo registrar notas de scope en modo detallado
        if (configSegura.esDetallado()) {
            registrar(I18nUI.Consola.NOTA_SCOPE_ANALISIS());
            registrar(I18nUI.Consola.NOTA_SCOPE_ANALISIS_ACCION());
        }
        registrarEstadoInicialLlM();
    }

    public ManejadorHttpBurpIA(MontoyaApi api, ConfiguracionAPI config, PestaniaPrincipal pestaniaPrincipal,
            PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador) {
        this(api, config, pestaniaPrincipal, stdout, stderr, limitador,
                null, null, null, null);
    }

    public void actualizarConfiguracion(ConfiguracionAPI nuevaConfig) {
        if (nuevaConfig == null) {
            registrarError("No se pudo actualizar configuracion: objeto de configuracion nulo");
            return;
        }

        if (nuevaConfig != this.config) {
            this.config.aplicarDesde(nuevaConfig);
        }

        int nuevoMaximoConcurrente = nuevaConfig.obtenerMaximoConcurrente() > 0
                ? nuevaConfig.obtenerMaximoConcurrente()
                : 1;

        this.limitador.ajustarMaximoConcurrente(nuevoMaximoConcurrente);
        
        // Usar TaskExecutionManager para actualizar configuración
        taskExecutionManager.actualizarConfiguracion(nuevaConfig);

        String proveedor = nuevaConfig.obtenerProveedorAI();
        String modelo = nuevaConfig.obtenerModelo();
        int timeout = nuevaConfig.obtenerTiempoEsperaParaModelo(proveedor, modelo);

        registrar(I18nUI.Consola.LOG_CONFIGURACION_ACTUALIZADA(
                nuevoMaximoConcurrente,
                nuevaConfig.obtenerRetrasoSegundos(),
                modelo,
                timeout));
    }



    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent solicitudAEnviar) {
        if (capturaActiva && config.esDetallado()) {
            rastrear("Solicitud a enviar: " + solicitudAEnviar.method() + " " + solicitudAEnviar.url());
        }
        return RequestToBeSentAction.continueWith(solicitudAEnviar);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived respuestaRecibida) {
        if (!capturaActiva) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        // Usar HttpRequestProcessor para validación
        if (!httpRequestProcessor.esSolicitudValida(respuestaRecibida)) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        final HttpResponseReceived respuestaCapturada = respuestaRecibida;
        final String url = respuestaRecibida.initiatingRequest().url() != null ? 
                respuestaRecibida.initiatingRequest().url() : "[URL NULL]";
        final String metodo = respuestaRecibida.initiatingRequest().method() != null ? 
                respuestaRecibida.initiatingRequest().method() : "[METHOD NULL]";
        final int codigoEstado = respuestaRecibida.statusCode();

        if (httpRequestProcessor.esSoloProxy(respuestaRecibida)) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (estadisticas != null) {
            estadisticas.incrementarTotalSolicitudes();
        }

        rastrear(() -> "Respuesta recibida: " + metodo + " " + url + " (estado: " + codigoEstado + ")");

        if (!httpRequestProcessor.estaEnScope(respuestaRecibida.initiatingRequest())) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        rastrear(() -> "DENTRO DE SCOPE - Procesando: " + metodo + " " + url);

        if (httpRequestProcessor.esRecursoEstatico(url)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            rastrear(() -> "Omitiendo recurso estatico: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        String contentTypeRespuesta = httpRequestProcessor.obtenerContentTypeRespuesta(respuestaRecibida);
        if (!httpRequestProcessor.esContenidoAnalizable(contentTypeRespuesta, metodo, codigoEstado)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            String contentTypeLog = Normalizador.esVacio(contentTypeRespuesta)
                    ? "desconocido"
                    : contentTypeRespuesta.trim();
            rastrear(() -> "Omitiendo contenido no analizable: " + url + " (Content-Type: " + contentTypeLog + ")");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (!puedeIniciarAnalisis("Analisis HTTP", url)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        String hashSolicitud = HttpUtils.generarHashRapido(respuestaRecibida.initiatingRequest(), respuestaRecibida);
        String hashAbreviado = httpRequestProcessor.abreviarHash(hashSolicitud);
        rastrear(() -> "Hash de solicitud: " + hashAbreviado + "...");

        if (deduplicador.esDuplicadoYAgregar(hashSolicitud)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosDuplicado();
            }
            rastrear(() -> "Solicitud duplicada omitida: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        rastrear(() -> "Nueva solicitud registrada: " + url + " (hash: " + hashAbreviado + "...)");

        int conteoEncabezados = 0;
        if (respuestaRecibida.initiatingRequest().headers() != null) {
            conteoEncabezados = respuestaRecibida.initiatingRequest().headers().size();
        }
        final int numEncabezados = conteoEncabezados;

        rastrear(() -> "Detalles de solicitud: Metodo=" + metodo + ", URL=" + url +
                ", Encabezados=" + numEncabezados + ", Codigo respuesta=" + codigoEstado);

        // Usar HttpRequestProcessor para crear SolicitudAnalisis
        SolicitudAnalisis solicitudAnalisis = httpRequestProcessor.crearSolicitudAnalisisDesdeRespuesta(respuestaRecibida);
        HttpRequestResponse evidenciaHttp = httpRequestProcessor.construirEvidenciaHttp(
                respuestaCapturada.initiatingRequest(), respuestaCapturada);
        
        programarAnalisis(
                solicitudAnalisis,
                evidenciaHttp,
                "Analisis HTTP");

        return ResponseReceivedAction.continueWith(respuestaRecibida);
    }

    public void analizarSolicitudForzada(HttpRequest solicitud) {
        analizarSolicitudForzada(solicitud, null);
    }

    public void analizarSolicitudForzada(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitud == null) {
            registrarError("No se pudo analizar solicitud forzada: request null");
            return;
        }

        String url = solicitud.url() != null ? solicitud.url() : "[URL NULL]";
        String metodo = solicitud.method() != null ? solicitud.method() : "[METHOD NULL]";
        if (!puedeIniciarAnalisis("Analisis Manual", url)) {
            return;
        }

        // Usar HttpRequestProcessor para crear SolicitudAnalisis y normalizar evidencia
        SolicitudAnalisis solicitudAnalisis = httpRequestProcessor.crearSolicitudAnalisisForzada(solicitud, solicitudRespuestaOriginal);
        if (solicitudAnalisis == null) {
            registrarError("No se pudo crear solicitud de análisis forzada");
            return;
        }

        registrar("Analisis forzado solicitado desde menu contextual: " + metodo + " " + url);
        HttpRequestResponse evidenciaHttp = httpRequestProcessor.normalizarEvidenciaManual(solicitud, solicitudRespuestaOriginal);
        programarAnalisis(
                solicitudAnalisis,
                evidenciaHttp,
                "Analisis Manual");
    }

    public boolean reencolarTarea(String tareaId) {
        // Usar TaskExecutionManager para reencolar
        boolean resultado = taskExecutionManager.reencolarTarea(tareaId);
        if (resultado) {
            registrar("Tarea reencolada: " + tareaId);
        }
        return resultado;
    }

    public void cancelarEjecucionActiva(String tareaId) {
        // Usar TaskExecutionManager para cancelar ejecución activa
        taskExecutionManager.cancelarEjecucionActiva(tareaId);
    }

    private String programarAnalisis(SolicitudAnalisis solicitudAnalisis,
            HttpRequestResponse evidenciaHttp,
            String tipoTarea) {
        if (solicitudAnalisis == null) {
            registrarError("No se pudo programar analisis: solicitud null");
            return null;
        }
        if (!puedeIniciarAnalisis(tipoTarea, solicitudAnalisis.obtenerUrl())) {
            return null;
        }

        // Usar TaskExecutionManager para programar análisis
        return taskExecutionManager.programarAnalisis(solicitudAnalisis, evidenciaHttp, tipoTarea);
    }









    private void guardarHallazgoEnIssuesSiAplica(Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            return;
        }
        
        boolean enviarIssues = pestaniaPrincipal != null
                && pestaniaPrincipal.obtenerPanelHallazgos() != null
                && pestaniaPrincipal.obtenerPanelHallazgos().isGuardadoAutomaticoIssuesActivo();
        if (!enviarIssues) {
            rastrear("Hallazgo omitido en Issues (Autoguardado deshabilitado): " + hallazgo.obtenerHallazgo());
            return;
        }
        
        evidenceManager.guardarHallazgoComoIssue(api, hallazgo, evidenciaId);
    }

    private void registrarIssuesNoDisponiblesPorEdicionUnaVez() {
        if (avisoIssuesNoDisponiblesEmitido) {
            return;
        }
        avisoIssuesNoDisponiblesEmitido = true;
        registrar("Integracion con Issues deshabilitada: solo disponible en Burp Professional");
    }

    private Hallazgo adjuntarEvidenciaSiDisponible(Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            return null;
        }
        if (hallazgo.obtenerEvidenciaId() != null || hallazgo.obtenerEvidenciaHttp() != null) {
            return hallazgo;
        }
        if (Normalizador.esVacio(evidenciaId)) {
            return hallazgo;
        }
        return hallazgo.conEvidenciaId(evidenciaId);
    }



    private boolean puedeIniciarAnalisis(String origen, String url) {
        ConfiguracionAPI.CodigoValidacionConsulta codigo = codigoValidacionConsulta();
        if (codigo == ConfiguracionAPI.CodigoValidacionConsulta.OK) {
            return true;
        }
        registrarAlertaConfiguracionLimitada(codigo, origen, url);
        return false;
    }

    private void registrarEstadoInicialLlM() {
        ConfiguracionAPI.CodigoValidacionConsulta codigo = codigoValidacionConsulta();
        if (codigo == ConfiguracionAPI.CodigoValidacionConsulta.OK) {
            // Verificar si multi-proveedor está habilitado
            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            List<String> proveedores = config.obtenerProveedoresMultiConsulta();

            // CONFIABILIDAD: Logs de diagnóstico solo en modo detallado
            if (config.esDetallado()) {
                rastrear("DIAGNOSTICO: Configuración multi-proveedor al inicio:");
                rastrear("DIAGNOSTICO:   - Habilitado: " + multiHabilitado);
                rastrear("DIAGNOSTICO:   - Proveedores: " +
                        (proveedores != null ? proveedores.size() + " elemento(s)" : "null"));
                if (proveedores != null && !proveedores.isEmpty()) {
                    rastrear("DIAGNOSTICO:   - Lista: " + String.join(", ", proveedores));
                }
            }

            if (multiHabilitado && proveedores != null && proveedores.size() > 1) {
                // Multi-proveedor con 2+ proveedores
                String proveedorPrincipal = config.obtenerProveedorAI();
                List<String> proveedoresAdicionales = new ArrayList<>();
                for (String p : proveedores) {
                    if (!p.equals(proveedorPrincipal)) {
                        proveedoresAdicionales.add(p);
                    }
                }
                registrar(
                        I18nUI.Consola.ESTADO_INICIAL_LLM_MULTIPROVEEDOR(
                                proveedorPrincipal,
                                config.obtenerModelo(),
                                proveedoresAdicionales));
                return;
            }

            // Caso normal: proveedor único
            // CONFIABILIDAD: Solo advertir sobre multi-proveedor en modo detallado
            if (multiHabilitado && config.esDetallado()) {
                int numProveedores = proveedores != null ? proveedores.size() : 0;
                if (numProveedores <= 1) {
                    registrar("AVISO: Multi-proveedor habilitado con " + numProveedores +
                            " proveedor(s). Se usará proveedor único: " + config.obtenerProveedorAI());
                }
            }

            registrar(
                    I18nUI.Consola.ESTADO_INICIAL_LLM_LISTO(
                            config.obtenerProveedorAI(),
                            config.obtenerModelo()));
            return;
        }
        String razon = config != null ? config.validarParaConsultaModelo()
                : I18nUI.Configuracion.MSG_CONFIGURACION_NULA();
        registrarError(I18nUI.Consola.ESTADO_INICIAL_LLM_BLOQUEADO_CABECERA(razon));
        registrar(I18nUI.Consola.ESTADO_INICIAL_LLM_BLOQUEADO_ACCION());
    }

    private ConfiguracionAPI.CodigoValidacionConsulta codigoValidacionConsulta() {
        if (config == null) {
            return ConfiguracionAPI.CodigoValidacionConsulta.CONFIGURACION_NULA;
        }
        ConfiguracionAPI.CodigoValidacionConsulta codigo = config.validarCodigoParaConsultaModelo();
        return codigo != null ? codigo : ConfiguracionAPI.CodigoValidacionConsulta.CONFIGURACION_NULA;
    }

    private void registrarAlertaConfiguracionLimitada(
            ConfiguracionAPI.CodigoValidacionConsulta codigo,
            String origen,
            String url) {
        if (codigo == null || codigo == ConfiguracionAPI.CodigoValidacionConsulta.OK) {
            return;
        }
        long ahora = System.currentTimeMillis();
        synchronized (alertasConfiguracionEmitidas) {
            Long ultimaEmision = alertasConfiguracionEmitidas.get(codigo);
            if (ultimaEmision != null && (ahora - ultimaEmision) < INTERVALO_ALERTA_CONFIG_MS) {
                return;
            }
            alertasConfiguracionEmitidas.put(codigo, ahora);
        }

        String razon = config != null ? config.validarParaConsultaModelo()
                : I18nUI.Configuracion.MSG_CONFIGURACION_NULA();
        String origenSeguro = Normalizador.noEsVacio(origen) ? origen : "desconocido";
        String urlSegura = Normalizador.noEsVacio(url) ? url : "[URL NULL]";
        registrarError(I18nUI.Consola.ANALISIS_BLOQUEADO_CONFIG(razon, origenSeguro, urlSegura));
    }



    private void registrar(String mensaje) {
        registrarInterno(mensaje, GestorConsolaGUI.TipoLog.INFO, false, "[BurpIA] ");
    }

    private void rastrear(String mensaje) {
        if (config.esDetallado()) {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.VERBOSE, false, "[BurpIA] [RASTREO] ");
        }
    }

    private void rastrear(Supplier<String> proveedorMensaje) {
        if (!config.esDetallado()) {
            return;
        }
        if (proveedorMensaje == null) {
            registrarInterno("", GestorConsolaGUI.TipoLog.VERBOSE, false, "[BurpIA] [RASTREO] ");
            return;
        }
        String mensaje = proveedorMensaje.get();
        registrarInterno(mensaje != null ? mensaje : "", GestorConsolaGUI.TipoLog.VERBOSE, false,
                "[BurpIA] [RASTREO] ");
    }

    private void registrarError(String mensaje) {
        registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ");
    }

    private void registrarInterno(String mensaje, GestorConsolaGUI.TipoLog tipo, boolean error, String prefijoSalida) {
        String mensajeSeguro = mensaje != null ? mensaje : "";
        if (gestorConsola != null) {
            gestorConsola.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
            return;
        }
        String prefijoLocalizado = I18nLogs.tr(prefijoSalida);
        String mensajeLocalizado = prefijoLocalizado + I18nLogs.tr(mensajeSeguro);
        synchronized (logLock) {
            escribirSalida(error, mensajeLocalizado);
        }
    }

    private void escribirSalida(boolean error, String mensaje) {
        PrintWriter destino = error ? stderr : stdout;
        if (destino != null) {
            destino.println(mensaje);
            destino.flush();
            return;
        }

        if (api != null) {
            try {
                if (error) {
                    api.logging().logToError(mensaje);
                } else {
                    api.logging().logToOutput(mensaje);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void rastrear(String mensaje, Throwable e) {
        if (config.esDetallado()) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            rastrear(mensaje + "\n" + sw.toString());
        }
    }



    public void shutdown() {
        // Usar TaskExecutionManager para shutdown
        taskExecutionManager.shutdown();
        
        alertasConfiguracionEmitidas.clear();
        evidenceManager.limpiarEvidenciasAntiguas();
    }

    public void pausarCaptura() {
        capturaActiva = false;
        registrar("Captura pausada por usuario");
    }

    public void reanudarCaptura() {
        capturaActiva = true;
        registrar("Captura reanudada por usuario");
    }

    public boolean estaCapturaActiva() {
        return capturaActiva;
    }


}
