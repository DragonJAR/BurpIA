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
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.FiltroContenidoAnalizable;
import com.burpia.util.DeduplicadorSolicitudes;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ManejadorHttpBurpIA implements HttpHandler {
    private static final String ORIGEN_LOG = "BurpIA";
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final PestaniaPrincipal pestaniaPrincipal;
    private volatile LimitadorTasa limitador;
    private final DeduplicadorSolicitudes deduplicador;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final ThreadPoolExecutor executorService;
    private final Object logLock;
    private final boolean esBurpProfessional;
    private volatile boolean capturaActiva;
    private volatile boolean avisoIssuesNoDisponiblesEmitido;

    private final Estadisticas estadisticas;
    private final GestorTareas gestorTareas;
    private final GestorConsolaGUI gestorConsola;
    private final ModeloTablaHallazgos modeloTablaHallazgos;
    private final ControlBackpressureGlobal controlBackpressure;
    private final Map<String, ContextoReintento> contextosReintento;
    private final Map<String, Future<?>> ejecucionesActivas;

    private static final class ContextoReintento {
        private final SolicitudAnalisis solicitudAnalisis;
        private final HttpRequestResponse evidenciaHttp;

        private ContextoReintento(SolicitudAnalisis solicitudAnalisis,
                                  HttpRequestResponse evidenciaHttp) {
            this.solicitudAnalisis = solicitudAnalisis;
            this.evidenciaHttp = evidenciaHttp;
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
        this.contextosReintento = new ConcurrentHashMap<>();
        this.ejecucionesActivas = new ConcurrentHashMap<>();
        this.esBurpProfessional = ExtensionBurpIA.esBurpProfessional(api);
        this.capturaActiva = configSegura.escaneoPasivoHabilitado();
        this.avisoIssuesNoDisponiblesEmitido = false;

        int maxThreads = configSegura.obtenerMaximoConcurrente() > 0 ? configSegura.obtenerMaximoConcurrente() : 10;
        this.limitador = limitador != null ? limitador : new LimitadorTasa(maxThreads);
        int capacidadCola = Math.max(50, maxThreads * 20);
        this.executorService = new ThreadPoolExecutor(
            maxThreads,
            maxThreads,
            60L,
            java.util.concurrent.TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(capacidadCola),
            new java.util.concurrent.ThreadFactory() {
                private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(1);
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setName("BurpIA-Thread-" + counter.getAndIncrement());
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()
        );

        this.logLock = new Object();

        registrar(I18nUI.Consola.LOG_MANEJADOR_INICIALIZADO(
            configSegura.obtenerMaximoConcurrente(),
            configSegura.obtenerRetrasoSegundos(),
            configSegura.esDetallado()
        ));
        registrar("NOTA: Solo se analizaran solicitudes DENTRO del SCOPE de Burp Suite");
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

        this.limitador = new LimitadorTasa(nuevoMaximoConcurrente);
        actualizarPoolEjecucion(nuevoMaximoConcurrente);

        String proveedor = nuevaConfig.obtenerProveedorAI();
        String modelo = nuevaConfig.obtenerModelo();
        int timeout = nuevaConfig.obtenerTiempoEsperaParaModelo(proveedor, modelo);

        registrar(I18nUI.Consola.LOG_CONFIGURACION_ACTUALIZADA(
            nuevoMaximoConcurrente,
            nuevaConfig.obtenerRetrasoSegundos(),
            modelo,
            timeout
        ));
    }

    private void actualizarPoolEjecucion(int nuevoMaximoConcurrente) {
        synchronized (executorService) {
            int maxActual = executorService.getMaximumPoolSize();
            if (nuevoMaximoConcurrente == maxActual) {
                return;
            }

            if (nuevoMaximoConcurrente > maxActual) {
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
            } else {
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
            }
        }
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

        if (respuestaRecibida == null) {
            registrarError("Respuesta recibida es null");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (respuestaRecibida.initiatingRequest() == null) {
            registrarError("Solicitud iniciadora es null");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        final HttpResponseReceived respuestaCapturada = respuestaRecibida;

        String tempUrl = respuestaRecibida.initiatingRequest().url();
        final String url = tempUrl != null ? tempUrl : "[URL NULL]";

        String tempMetodo = respuestaRecibida.initiatingRequest().method();
        final String metodo = tempMetodo != null ? tempMetodo : "[METHOD NULL]";

        int codigoEstado = respuestaRecibida.statusCode();

        if (config.soloProxy() && !respuestaRecibida.toolSource().isFromTool(ToolType.PROXY)) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (estadisticas != null) {
            estadisticas.incrementarTotalSolicitudes();
        }

        rastrear(() -> "Respuesta recibida: " + metodo + " " + url + " (estado: " + codigoEstado + ")");

        if (!estaEnScope(respuestaRecibida.initiatingRequest())) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        rastrear(() -> "DENTRO DE SCOPE - Procesando: " + metodo + " " + url);

        if (esRecursoEstatico(url)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            rastrear(() -> "Omitiendo recurso estatico: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        String contentTypeRespuesta = obtenerContentTypeRespuesta(respuestaRecibida);
        if (!FiltroContenidoAnalizable.esAnalizable(contentTypeRespuesta, metodo, codigoEstado)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            String contentTypeLog = (contentTypeRespuesta == null || contentTypeRespuesta.trim().isEmpty())
                ? "desconocido"
                : contentTypeRespuesta.trim();
            rastrear(() -> "Omitiendo contenido no analizable: " + url + " (Content-Type: " + contentTypeLog + ")");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        String hashSolicitud = HttpUtils.generarHashRapido(respuestaRecibida.initiatingRequest(), respuestaRecibida);
        String hashAbreviado = abreviarHash(hashSolicitud);
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

        SolicitudAnalisis solicitudAnalisis = new SolicitudAnalisis(
            url,
            metodo,
            hashSolicitud,
            respuestaRecibida.initiatingRequest(),
            respuestaRecibida,
            codigoEstado
        );
        programarAnalisis(
            solicitudAnalisis,
            construirEvidenciaHttp(respuestaCapturada.initiatingRequest(), respuestaCapturada),
            "Analisis HTTP"
        );

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
        String encabezados = HttpUtils.extraerEncabezados(solicitud);
        String cuerpo = HttpUtils.extraerCuerpo(solicitud);
        String hashSolicitud = HttpUtils.generarHashPartes(metodo, url, encabezados, cuerpo);
        int codigoEstadoRespuesta = -1;
        String encabezadosRespuesta = "";
        String cuerpoRespuesta = "";
        if (solicitudRespuestaOriginal != null) {
            try {
                if (solicitudRespuestaOriginal.hasResponse() && solicitudRespuestaOriginal.response() != null) {
                    codigoEstadoRespuesta = solicitudRespuestaOriginal.response().statusCode();
                    encabezadosRespuesta = HttpUtils.extraerEncabezados(solicitudRespuestaOriginal.response());
                    cuerpoRespuesta = HttpUtils.extraerCuerpo(solicitudRespuestaOriginal.response());
                }
            } catch (Exception e) {
                rastrear("No se pudo capturar la respuesta para analisis manual", e);
            }
        }

        SolicitudAnalisis solicitudAnalisis = new SolicitudAnalisis(
            url,
            metodo,
            encabezados,
            cuerpo,
            hashSolicitud,
            solicitud,
            codigoEstadoRespuesta,
            encabezadosRespuesta,
            cuerpoRespuesta
        );
        registrar("Analisis forzado solicitado desde menu contextual: " + metodo + " " + url);
        programarAnalisis(
            solicitudAnalisis,
            normalizarEvidenciaManual(solicitud, solicitudRespuestaOriginal),
            "Analisis Manual"
        );
    }

    public boolean reencolarTarea(String tareaId) {
        if (tareaId == null || tareaId.isEmpty()) {
            return false;
        }
        depurarContextosHuerfanos();
        ContextoReintento contexto = contextosReintento.get(tareaId);
        if (contexto == null) {
            registrarError("No existe contexto para reintentar tarea: " + tareaId);
            return false;
        }

        if (gestorTareas != null) {
            gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_EN_COLA, "Reintentando...");
        }

        ejecutarAnalisisExistente(
            tareaId,
            contexto.solicitudAnalisis,
            contexto.evidenciaHttp
        );

        registrar("Tarea reencolada: " + tareaId);
        return true;
    }

    public void cancelarEjecucionActiva(String tareaId) {
        if (tareaId == null || tareaId.isEmpty()) {
            return;
        }
        Future<?> future = ejecucionesActivas.remove(tareaId);
        if (future != null) {
            boolean cancelada = future.cancel(true);
            if (cancelada) {
                rastrear("Cancelacion activa aplicada para tarea: " + tareaId);
            }
        }
    }

    private String programarAnalisis(SolicitudAnalisis solicitudAnalisis,
                                     HttpRequestResponse evidenciaHttp,
                                     String tipoTarea) {
        if (solicitudAnalisis == null) {
            registrarError("No se pudo programar analisis: solicitud null");
            return null;
        }

        depurarContextosHuerfanos();
        final String url = solicitudAnalisis.obtenerUrl();
        final AtomicReference<String> tareaIdRef = new AtomicReference<>();

        if (gestorTareas != null) {
            Tarea tarea = gestorTareas.crearTarea(
                tipoTarea,
                url,
                Tarea.ESTADO_EN_COLA,
                "Esperando analisis"
            );
            tareaIdRef.set(tarea.obtenerId());
            contextosReintento.put(
                tarea.obtenerId(),
                new ContextoReintento(solicitudAnalisis, evidenciaHttp)
            );
            ejecutarAnalisisExistente(tarea.obtenerId(), solicitudAnalisis, evidenciaHttp);
        }
        return tareaIdRef.get();
    }

    private void ejecutarAnalisisExistente(String tareaId,
                                          SolicitudAnalisis solicitudAnalisis,
                                          HttpRequestResponse evidenciaHttp) {
        if (tareaId == null || solicitudAnalisis == null) {
            return;
        }

        final String url = solicitudAnalisis.obtenerUrl();
        final AtomicReference<String> tareaIdRef = new AtomicReference<>(tareaId);

        SwingUtilities.invokeLater(() -> {
            if (pestaniaPrincipal != null) {
                pestaniaPrincipal.registrar("Iniciando análisis (continuar/reintentar) para: " + url);
            }
        });

        AnalizadorAI analizador = new AnalizadorAI(
            solicitudAnalisis,
            config.crearSnapshot(),
            stdout,
            stderr,
            limitador,
            new ManejadorResultadoAI(tareaIdRef, url, evidenciaHttp),
            () -> {
                final String id = tareaIdRef.get();
                if (gestorTareas == null || id == null) {
                    return;
                }
                boolean marcada = gestorTareas.marcarTareaAnalizando(id, "Analizando");
                if (!marcada) {
                    rastrear("No se pudo marcar tarea como analizando (estado no valido): " + id);
                }
            },
            gestorConsola,
            () -> {
                final String id = tareaIdRef.get();
                return gestorTareas != null && id != null && gestorTareas.estaTareaCancelada(id);
            },
            () -> {
                final String id = tareaIdRef.get();
                return gestorTareas != null && id != null && gestorTareas.estaTareaPausada(id);
            },
            controlBackpressure
        );

        try {
            Future<?> future = executorService.submit(analizador);
            String id = tareaIdRef.get();
            if (id != null) {
                ejecucionesActivas.put(id, future);
            }
            registrar("Hilo de analisis iniciado para: " + url + " (ID: " + id + ")");
            rastrearEstadoCola();
        } catch (RejectedExecutionException ex) {
            String id = tareaIdRef.get();
            finalizarEjecucionActiva(id);
            if (gestorTareas != null && id != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_ERROR, "Descartada por saturación de cola");
            }
            if (estadisticas != null) {
                estadisticas.incrementarErrores();
            }
            registrarError("Cola de análisis saturada, solicitud descartada: " + url);
        }
    }

    private HttpRequestResponse construirEvidenciaHttp(HttpRequest solicitud, burp.api.montoya.http.message.responses.HttpResponse respuesta) {
        if (solicitud == null || respuesta == null) {
            return null;
        }
        try {
            return HttpRequestResponse.httpRequestResponse(solicitud, respuesta);
        } catch (Exception e) {
            rastrear("No se pudo construir HttpRequestResponse para evidencia de Issue", e);
            return null;
        }
    }

    private HttpRequestResponse normalizarEvidenciaManual(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitudRespuestaOriginal == null) {
            rastrear("Analisis manual sin request/response original: se registraran hallazgos, pero no Issue.");
            return null;
        }

        try {
            if (!solicitudRespuestaOriginal.hasResponse()) {
                rastrear("Analisis manual sin response asociada: se registraran hallazgos, pero no Issue.");
                return null;
            }
            if (solicitudRespuestaOriginal.request() != null && solicitudRespuestaOriginal.response() != null) {
                return solicitudRespuestaOriginal;
            }
        } catch (Exception e) {
            rastrear("No se pudo reutilizar la evidencia original del analisis manual", e);
        }

        try {
            return construirEvidenciaHttp(solicitud, solicitudRespuestaOriginal.response());
        } catch (Exception e) {
            rastrear("No se pudo construir evidencia desde analisis manual", e);
            return null;
        }
    }

    private void finalizarEjecucionActiva(String tareaId) {
        if (tareaId == null || tareaId.isEmpty()) {
            return;
        }
        ejecucionesActivas.remove(tareaId);
    }

    private void depurarContextosHuerfanos() {
        if (gestorTareas == null || contextosReintento.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, ContextoReintento>> it = contextosReintento.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ContextoReintento> entry = it.next();
            if (entry == null) {
                continue;
            }
            String tareaId = entry.getKey();
            if (tareaId == null || gestorTareas.obtenerTarea(tareaId) == null) {
                it.remove();
            }
        }
    }

    private void rastrearEstadoCola() {
        if (!config.esDetallado()) {
            return;
        }
        rastrear(
            "Estado cola analisis: activos=" + executorService.getActiveCount() +
                ", enCola=" + executorService.getQueue().size() +
                ", completadas=" + executorService.getCompletedTaskCount()
        );
    }

    private void guardarHallazgoEnIssuesSiAplica(Hallazgo hallazgo, HttpRequestResponse evidenciaHttp) {
        if (hallazgo == null) {
            return;
        }
        if (!esBurpProfessional) {
            registrarIssuesNoDisponiblesPorEdicionUnaVez();
            return;
        }
        HttpRequestResponse evidenciaIssue = evidenciaHttp != null
            ? evidenciaHttp
            : hallazgo.obtenerEvidenciaHttp();
        if (evidenciaIssue == null) {
            rastrear("Hallazgo sin evidencia HTTP: no se puede crear AuditIssue");
            return;
        }

        boolean enviarIssues = pestaniaPrincipal != null
            && pestaniaPrincipal.obtenerPanelHallazgos() != null
            && pestaniaPrincipal.obtenerPanelHallazgos().isGuardadoAutomaticoIssuesActivo();
        if (!enviarIssues) {
            rastrear("Hallazgo omitido en Issues (Autoguardado deshabilitado): " + hallazgo.obtenerHallazgo());
            return;
        }

        try {
            if (api == null || api.siteMap() == null) {
                registrarError("No se pudo guardar AuditIssue: SiteMap API no disponible");
                return;
            }
            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, evidenciaIssue);
            if (!guardado) {
                rastrear("AuditIssue no creado: hallazgo sin datos suficientes");
                return;
            }
            rastrear("AuditIssue creado en Burp Suite para: " + hallazgo.obtenerHallazgo());
        } catch (Exception e) {
            registrarError("Error al crear AuditIssue en Burp Suite: " + e.getMessage());
            rastrear("Stack trace:", e);
        }
    }

    private void registrarIssuesNoDisponiblesPorEdicionUnaVez() {
        if (avisoIssuesNoDisponiblesEmitido) {
            return;
        }
        avisoIssuesNoDisponiblesEmitido = true;
        registrar("Integracion con Issues deshabilitada: solo disponible en Burp Professional");
    }

    private Hallazgo adjuntarEvidenciaSiDisponible(Hallazgo hallazgo, HttpRequestResponse evidenciaHttp) {
        if (hallazgo == null) {
            return null;
        }
        if (hallazgo.obtenerEvidenciaHttp() != null || evidenciaHttp == null) {
            return hallazgo;
        }
        return hallazgo.conEvidenciaHttp(evidenciaHttp);
    }

    private boolean estaEnScope(HttpRequest solicitud) {
        if (solicitud == null) {
            return false;
        }

        try {
            String url = solicitud.url();
            if (url == null || url.isEmpty()) {
                return false;
            }

            if (api != null && api.scope() != null) {
                return api.scope().isInScope(url);
            }

            return false;

        } catch (Exception e) {
            registrarError("Error al verificar scope: " + e.getMessage());
            return false;
        }
    }

    private boolean esRecursoEstatico(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        boolean esEstatico = HttpUtils.esRecursoEstatico(url);

        if (esEstatico && config.esDetallado()) {
            rastrear("Recurso coincidio con filtro estatico: " + url);
        }

        return esEstatico;
    }

    private String obtenerContentTypeRespuesta(HttpResponseReceived respuestaRecibida) {
        if (respuestaRecibida == null || respuestaRecibida.headers() == null) {
            return "";
        }
        try {
            for (burp.api.montoya.http.message.HttpHeader header : respuestaRecibida.headers()) {
                if (header == null || header.name() == null) {
                    continue;
                }
                if ("Content-Type".equalsIgnoreCase(header.name())) {
                    return header.value() != null ? header.value() : "";
                }
            }
        } catch (Exception e) {
            rastrear("No se pudo extraer Content-Type de respuesta", e);
        }
        return "";
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
        registrarInterno(mensaje != null ? mensaje : "", GestorConsolaGUI.TipoLog.VERBOSE, false, "[BurpIA] [RASTREO] ");
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

    private String abreviarHash(String hashSolicitud) {
        if (hashSolicitud == null || hashSolicitud.isEmpty()) {
            return "";
        }
        return hashSolicitud.substring(0, Math.min(8, hashSolicitud.length()));
    }

    public void shutdown() {
        for (Map.Entry<String, Future<?>> entry : ejecucionesActivas.entrySet()) {
            if (entry != null && entry.getValue() != null) {
                entry.getValue().cancel(true);
            }
        }
        ejecucionesActivas.clear();
        contextosReintento.clear();

        if (executorService != null && !executorService.isShutdown()) {
            registrar("Deteniendo ExecutorService de ManejadorHttpBurpIA...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    registrar("ExecutorService no termino en 5 segundos, forzando shutdown...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                registrarError("Error al esperar terminacion de ExecutorService: " + e.getMessage());
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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

    private class ManejadorResultadoAI implements AnalizadorAI.Callback {
        private final AtomicReference<String> tareaIdRef;
        private final String url;
        private final HttpRequestResponse evidenciaHttp;

        ManejadorResultadoAI(AtomicReference<String> tareaIdRef, String url, HttpRequestResponse evidenciaHttp) {
            this.tareaIdRef = tareaIdRef;
            this.url = url;
            this.evidenciaHttp = evidenciaHttp;
        }

        @Override
        public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
            final String id = tareaIdRef.get();
            finalizarEjecucionActiva(id);
            contextosReintento.remove(id);
            boolean cancelada = gestorTareas != null && id != null && gestorTareas.estaTareaCancelada(id);
            if (cancelada) {
                registrar("Resultado descartado porque la tarea fue cancelada: " + url);
                return;
            }

            if (estadisticas != null) estadisticas.incrementarAnalizados();

            if (gestorTareas != null && id != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_COMPLETADO,
                    "Completado: " + (resultado != null ? resultado.obtenerNumeroHallazgos() : 0) + " hallazgos");
            }

            if (resultado != null && resultado.obtenerHallazgos() != null) {
                List<Hallazgo> hallazgosValidos = new ArrayList<>();
                for (Hallazgo hallazgo : resultado.obtenerHallazgos()) {
                    if (hallazgo == null) continue;
                    Hallazgo hConEvidencia = adjuntarEvidenciaSiDisponible(hallazgo, evidenciaHttp);
                    hallazgosValidos.add(hConEvidencia);
                    if (estadisticas != null) {
                        String sev = hConEvidencia.obtenerSeveridad();
                        if (sev != null) estadisticas.incrementarHallazgoSeveridad(sev);
                    }
                    guardarHallazgoEnIssuesSiAplica(hConEvidencia, evidenciaHttp);
                }
                if (modeloTablaHallazgos != null && !hallazgosValidos.isEmpty()) {
                    modeloTablaHallazgos.agregarHallazgos(hallazgosValidos);
                }
            }

            String sevMax = resultado != null ? resultado.obtenerSeveridadMaxima() : "N/A";
            registrar("Analisis completado: " + url + " (severidad maxima: " + sevMax + ")");

            SwingUtilities.invokeLater(() -> {
                if (pestaniaPrincipal != null) pestaniaPrincipal.actualizarEstadisticas();
            });
        }

        @Override
        public void alErrorAnalisis(String error) {
            final String id = tareaIdRef.get();
            finalizarEjecucionActiva(id);
            boolean cancelada = gestorTareas != null && id != null && gestorTareas.estaTareaCancelada(id);
            if (cancelada) {
                registrar("Analisis detenido por cancelacion: " + url);
                return;
            }
            if (estadisticas != null) estadisticas.incrementarErrores();
            if (gestorTareas != null && id != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_ERROR, "Error: " + (error != null ? error : "Error desconocido"));
            }
            registrarError("Analisis fallido para " + url + ": " + (error != null ? error : "Error desconocido"));
            SwingUtilities.invokeLater(() -> {
                if (pestaniaPrincipal != null) pestaniaPrincipal.actualizarEstadisticas();
            });
        }

        @Override
        public void alCanceladoAnalisis() {
            final String id = tareaIdRef.get();
            finalizarEjecucionActiva(id);
            if (gestorTareas != null && id != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_CANCELADO, "Cancelado por usuario");
            }
            registrar("Analisis cancelado: " + url);
        }
    }
}
