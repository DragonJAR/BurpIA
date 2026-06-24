package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public class GestorMultiProveedor {
    private static final String ORIGEN_LOG = "GestorMultiProveedor";
    private static final long DELAY_ENTRE_PROVEEDORES_MS = 2000L;
    private static final String LINEA_SEPARADORA_PROVEEDOR = "========================================";
    
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final ControlCancelacionPausa control;
    private final GestorLoggingUnificado gestorLogging;
    private final Object logLock;
    private final ConstructorPrompts constructorPrompt;
    private final ParseadorRespuestasAI parseador;
    private final com.burpia.util.PromptTruncador promptTruncador;
    // AnalizadorHTTP del proveedor en curso (asignado antes de llamar a la API,
    // limpiado en finally). Permite cancelar la Call OkHttp activa desde fuera
    // cuando el usuario pulsa Cancelar durante un análisis multi-proveedor.
    private volatile AnalizadorHTTP analizadorHttpActivo;

    /**
     * Cancela la llamada HTTP en curso del proveedor actual. No-op si no hay
     * llamada activa. Llamado por {@link AnalizadorAI#cancelarLlamadaHttpActiva()}
     * para que la cancelación del usuario llegue al socket también en modo multi.
     */
    public void cancelarLlamadaActiva() {
        AnalizadorHTTP activo = analizadorHttpActivo;
        if (activo != null) {
            activo.cancelarLlamadaActiva();
        }
    }

    public GestorMultiProveedor(SolicitudAnalisis solicitud,
                               ConfiguracionAPI config,
                               PrintWriter stdout,
                               PrintWriter stderr,
                               GestorConsolaGUI gestorConsola,
                               BooleanSupplier tareaCancelada,
                               BooleanSupplier tareaPausada,
                               GestorLoggingUnificado gestorLogging) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.gestorConsola = gestorConsola;
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.control = new ControlCancelacionPausa(tareaCancelada, tareaPausada);
        this.gestorLogging = gestorLogging;
        this.logLock = new Object();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.parseador = new ParseadorRespuestasAI(this.gestorLogging, this.config.obtenerIdiomaUi());
        this.promptTruncador = new com.burpia.util.PromptTruncador();
    }

    public ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedor() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        
        if (Normalizador.esVacia(proveedores)) {
            registrar(I18nLogs.MultiProveedor.SIN_PROVEEDORES());
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            // Usar el proveedor real de la lista (no el default de config),
            // coherente con el path multi-proveedor. Antes delegaba a
            // ejecutarAnalisisProveedorUnico() que ignoraba el proveedor
            // seleccionado. Latente: hoy los callers guard con size()>1,
            // pero esto previene un bug si ese guard se relaja.
            String proveedor = proveedores.get(0);
            registrar(I18nLogs.MultiProveedor.UN_PROVEEDOR());
            return ejecutarAnalisisProveedor(proveedor, config.obtenerModeloParaProveedor(proveedor));
        }

        return ejecutarAnalisisSecuencialProveedores(proveedores);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisSecuencialProveedores(List<String> proveedores)
            throws IOException, InterruptedException {
        
        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();
        boolean proveedorEjecutadoPreviamente = false;

        for (String proveedor : proveedores) {
            control.verificarCancelacion();
            control.esperarSiPausada();

            if (Normalizador.esVacio(proveedor)) {
                continue;
            }
            
            if (!ProveedorAI.existeProveedor(proveedor)) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_NO_EXISTE(proveedor));
                continue;
            }

            String modelo = config.obtenerModeloParaProveedor(proveedor);
            if (Normalizador.esVacio(modelo)) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_SIN_MODELO(proveedor));
                continue;
            }

            if (proveedorEjecutadoPreviamente) {
                long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                registrar(I18nLogs.MultiProveedor.ESPERANDO_SIGUIENTE(delaySegundos));
                control.esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
            }

            registrar(LINEA_SEPARADORA_PROVEEDOR);
            registrar(I18nLogs.MultiProveedor.PROVEEDOR_EJECUTANDO(proveedor, modelo));

            try {
                ResultadoAnalisisMultiple resultado = ejecutarAnalisisProveedor(proveedor, modelo);
                List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();

                registrar(I18nLogs.MultiProveedor.PROVEEDOR_COMPLETADO(proveedor, hallazgosProveedor.size()));
                todosHallazgos.addAll(hallazgosProveedor);

            } catch (InterruptedException ie) {
                // Solo abortar todo el multi-run si el usuario canceló de verdad.
                // InterruptedException también surge de la propia llamada/backoff de
                // ESTE proveedor (timeout que interrumpe el sleep, future.cancel, etc.).
                // En ese caso NO debe matar a los demás: antes cualquier interrupción
                // abortaba el run y el 3er proveedor nunca se ejecutaba. Se trata como
                // fallo de este proveedor y se continúa, igual que cualquier otro error.
                if (control.esCancelada()) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                Thread.interrupted(); // limpiar el flag para no envenenar al siguiente proveedor
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_ERROR(proveedor, ie.getMessage()));
                proveedoresFallidos.add(proveedor);
            } catch (Exception e) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_ERROR(proveedor, e.getMessage()));
                proveedoresFallidos.add(proveedor);
            } finally {
                proveedorEjecutadoPreviamente = true;
            }
        }

        if (!proveedoresFallidos.isEmpty()) {
            registrarError(I18nLogs.MultiProveedor.PROVEEDORES_FALLIDOS(
                    proveedoresFallidos.size(), String.join(", ", proveedoresFallidos)));
        }

        registrar(LINEA_SEPARADORA_PROVEEDOR);
        registrar(I18nLogs.MultiProveedor.MULTI_CONSULTA_COMPLETADA(todosHallazgos.size()));

        return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                solicitud.obtenerSolicitudHttp(), proveedoresFallidos);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException, InterruptedException {
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(config, tareaCancelada, tareaPausada, gestorLogging);
        analizadorHttpActivo = analizadorHTTP;
        try {
            String respuesta = llamarAPIAIConRetries(analizadorHTTP, config, constructorPrompt);
            return parseador.parsearRespuesta(respuesta, solicitud, config.obtenerProveedorAI());
        } finally {
            analizadorHttpActivo = null;
        }
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedor(String proveedor, String modelo)
            throws IOException, InterruptedException {

        ConfiguracionAPI configProveedor = crearConfiguracionParaProveedor(proveedor);
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(configProveedor, tareaCancelada, tareaPausada, gestorLogging);
        analizadorHttpActivo = analizadorHTTP;
        // Construir prompt y parsear con la configuración específica del
        // proveedor (no la base): los límites de prompt y el idioma de salida
        // dependen del proveedor activo.
        ConstructorPrompts constructorProveedor = new ConstructorPrompts(configProveedor);
        ParseadorRespuestasAI parseadorProveedor =
                new ParseadorRespuestasAI(gestorLogging, configProveedor.obtenerIdiomaUi());
        try {
            String respuesta = llamarAPIAIConRetries(analizadorHTTP, configProveedor, constructorProveedor);
            ResultadoAnalisisMultiple resultado = parseadorProveedor.parsearRespuesta(respuesta, solicitud, proveedor);
            return etiquetarResultado(resultado, proveedor, modelo);
        } finally {
            analizadorHttpActivo = null;
        }
    }

    private ConfiguracionAPI crearConfiguracionParaProveedor(String proveedor) {
        ConfiguracionAPI configProveedor = new ConfiguracionAPI();
        configProveedor.aplicarDesde(config);
        configProveedor.establecerProveedorAI(proveedor);
        return configProveedor;
    }

    private String llamarAPIAIConRetries(AnalizadorHTTP analizadorHTTP,
                                         ConfiguracionAPI configActual,
                                         ConstructorPrompts constructorActual)
            throws IOException, InterruptedException {

        control.verificarCancelacion();
        control.esperarSiPausada();

        String prompt = constructorActual.construirPromptAnalisis(solicitud);

        // Comparte el mismo ejecutor con truncado-y-reintento que el modo único:
        // antes el multi-proveedor marcaba el proveedor como fallido ante un
        // error de contexto en vez de truncar y reintentar.
        String respuesta = new EjecutorLlamadaConTruncado(
                configActual, analizadorHTTP, promptTruncador, control, gestorLogging)
                .ejecutar(prompt);
        registrar(I18nLogs.MultiProveedor.LONGITUD_RESPUESTA_API(respuesta.length()));
        return respuesta;
    }

    private ResultadoAnalisisMultiple etiquetarResultado(ResultadoAnalisisMultiple resultado,
                                                         String proveedor,
                                                         String modelo) {
        List<Hallazgo> hallazgos = resultado.obtenerHallazgos();
        List<Hallazgo> hallazgosConEtiqueta = new ArrayList<>();

        for (Hallazgo hallazgo : hallazgos) {
            String descripcionOriginal = hallazgo.obtenerHallazgo();
            String etiqueta = I18nUI.Configuracion.TXT_DESCUBIERTO_CON(proveedor, modelo);
            String descripcionConEtiqueta = descripcionOriginal + etiqueta;

            Hallazgo hallazgoEtiquetado = hallazgo.editar(
                    hallazgo.obtenerUrl(),
                    hallazgo.obtenerTitulo(),
                    descripcionConEtiqueta,
                    hallazgo.obtenerSeveridad(),
                    hallazgo.obtenerConfianza());

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosConEtiqueta,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
    }

    private void registrar(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.info(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.INFO, false, "[BurpIA] ", false);
        }
    }

    private void registrarError(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.error(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ", false);
        }
    }

    private void registrarInterno(String mensaje, GestorConsolaGUI.TipoLog tipo, boolean esError, 
                                 String prefijoSalida, boolean mensajeTecnico) {
        String mensajeSeguro = mensaje != null ? mensaje : "";
        GestorConsolaGUI consolaActual = this.gestorConsola;

        if (consolaActual != null) {
            if (mensajeTecnico) {
                consolaActual.registrarTecnico(ORIGEN_LOG, mensajeSeguro, tipo);
            } else {
                consolaActual.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
            }
            return;
        }

        PrintWriter destinoStr;
        synchronized (logLock) {
            destinoStr = esError ? stderr : stdout;
            if (destinoStr != null) {
                destinoStr.println(prefijoSalida + mensajeSeguro);
                destinoStr.flush();
            }
        }
    }

    /**
     * Parsea una respuesta JSON usando el parseador interno.
     * Exist for test access only — production code should use the full pipeline.
     */
    ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, String proveedor) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
    }
}
