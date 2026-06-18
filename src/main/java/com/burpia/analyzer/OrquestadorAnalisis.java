package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.PromptTruncador;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public class OrquestadorAnalisis {
    private static final String ORIGEN_LOG = "OrquestadorAnalisis";

    private final SolicitudAnalisis solicitud;
    // Snapshot inmutable: el llamador crea una copia via crearSnapshot() antes de pasarla.
    // No se reasigna ni muta después del constructor; 'final' garantiza visibilidad entre hilos.
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final Callback callback;
    private final Runnable alInicioAnalisis;
    private final GestorConsolaGUI gestorConsola;
    private final ControlCancelacionPausa controlCancelacionPausa;
    private final ConstructorPrompts constructorPrompt;
    private final GestorLoggingUnificado gestorLogging;
    private final AnalizadorHTTP analizadorHTTP;
    private final ParseadorRespuestasAI parseador;
    private final PromptTruncador promptTruncador;

    public interface Callback {
        void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);
        void alErrorAnalisis(String error);
        default void alCanceladoAnalisis() {}
    }

    public OrquestadorAnalisis(SolicitudAnalisis solicitud, 
                              ConfiguracionAPI config, 
                              PrintWriter stdout, 
                              PrintWriter stderr,
                              LimitadorTasa limitador, 
                              Callback callback, 
                              Runnable alInicioAnalisis,
                              GestorConsolaGUI gestorConsola, 
                              ControlCancelacionPausa controlCancelacionPausa) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(1);
        this.callback = callback != null ? callback : new Callback() {
            @Override
            public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {}
            @Override
            public void alErrorAnalisis(String error) {}
        };
        this.alInicioAnalisis = alInicioAnalisis;
        this.gestorConsola = gestorConsola;
        this.controlCancelacionPausa = controlCancelacionPausa != null ? controlCancelacionPausa : new ControlCancelacionPausa(null, null);
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);
        // Extraer suppliers para AnalizadorHTTP (API legacy)
        java.util.function.BooleanSupplier cancelada = this.controlCancelacionPausa::esCancelada;
        java.util.function.BooleanSupplier pausada = this.controlCancelacionPausa::esPausada;
        this.analizadorHTTP = new AnalizadorHTTP(this.config, cancelada, pausada, this.gestorLogging);
        this.parseador = new ParseadorRespuestasAI(this.gestorLogging, this.config.obtenerIdiomaUi());
        this.promptTruncador = new PromptTruncador();
    }

    public void cancelarLlamadaHttpActiva() {
        analizadorHTTP.cancelarLlamadaActiva();
    }

    public ResultadoAnalisisMultiple ejecutarAnalisisCompleto() throws IOException, InterruptedException {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();

        if (solicitud == null) {
            String error = mensajeErrorSolicitudNoDisponible();
            gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("[%s] %s", nombreHilo, error));
            throw new IOException(error);
        }

        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("[%s] Orquestador iniciado para URL: %s",
                nombreHilo, solicitud.obtenerUrl()));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Hash de solicitud: %s",
                nombreHilo, solicitud.obtenerHashSolicitud()));

        try {
            controlCancelacionPausa.verificarCancelacion();
            controlCancelacionPausa.esperarSiPausada();
            notificarInicioAnalisis();
            
            String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
            if (Normalizador.noEsVacio(alertaConfiguracion)) {
                gestorLogging.error(ORIGEN_LOG, alertaConfiguracion);
                throw new IOException(alertaConfiguracion);
            }

            // NOTA: El limitador YA fue adquirido por el llamador (AnalizadorAI o FlowAnalysisManager)
            // No adquirimos aquí para evitar doble adquisición que causa permisos negativos

            gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Analizando: %s", solicitud.obtenerUrl()));

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("DIAGNOSTICO: multiHabilitado=%s, proveedoresConfig=%s",
                    multiHabilitado,
                    proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null"));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("DIAGNOSTICO: Ejecutando multi-proveedor con %d proveedores",
                        proveedoresConfig.size()));
                resultadoMultiple = ejecutarAnalisisMultiProveedorSecuencial();
            } else {
                if (multiHabilitado) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.trf(
                            "PROVEEDOR: Multi-proveedor habilitado pero solo %d proveedor(es) configurado(s). Usando proveedor único: %s",
                            proveedoresConfig != null ? proveedoresConfig.size() : 0,
                            config.obtenerProveedorAI()));
                } else {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("PROVEEDOR: Usando proveedor único: %s",
                            config.obtenerProveedorAI()));
                }
                String respuesta = llamarAPIAIConRetries();
                resultadoMultiple = parseador.parsearRespuesta(respuesta, solicitud, config != null ? config.obtenerProveedorAI() : "");
            }

            long duracion = System.currentTimeMillis() - tiempoInicio;
            gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Análisis completado: %s (tomo %dms)",
                    solicitud.obtenerUrl(), duracion));
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Severidad maxima: %s",
                    nombreHilo, resultadoMultiple.obtenerSeveridadMaxima()));

            return resultadoMultiple;

        } finally { // NOPMD EmptyFinallyBlock
            // El limitador es liberado por el llamador (AnalizadorAI o FlowAnalysisManager).
            // No liberamos aquí para evitar doble liberación que causaría permisos negativos.
        }
    }

    private void notificarInicioAnalisis() {
        if (alInicioAnalisis == null) {
            return;
        }
        try {
            alInicioAnalisis.run();
        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("No se pudo notificar inicio de análisis"));
        }
    }

    private String validarConfiguracionAntesDeConsulta() {
        if (config == null) {
            return alertaConfiguracionNoDisponible();
        }
        String error = config.validarParaConsultaModelo();
        return error != null ? error.trim() : "";
    }

    private String construirPromptAnalisis() {
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("Construyendo prompt para URL: %s",
                solicitud.obtenerUrl()));
        String promptPreconstruido = solicitud.obtenerPromptPreconstruido();
        String prompt = Normalizador.noEsVacio(promptPreconstruido)
            ? promptPreconstruido
            : constructorPrompt.construirPromptAnalisis(solicitud);
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("Longitud de prompt: %d caracteres", prompt.length()));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("Prompt (preview):%n%s", resumirParaLog(prompt)));
        return prompt;
    }

    // Propaga InterruptedException sin rebundle: la cancelación debe llegar al
    // catch (InterruptedException) de AnalizadorAI.run() → alCanceladoAnalisis().
    // Antes se envolvía en IOException y caía al catch (Exception) → alErrorAnalisis(),
    // reportando la cancelación del usuario como un error.
    private String llamarAPIAIConRetries() throws IOException, InterruptedException {
        String prompt = construirPromptAnalisis();
        String respuesta = new EjecutorLlamadaConTruncado(
                config, analizadorHTTP, promptTruncador, controlCancelacionPausa, gestorLogging)
                .ejecutar(prompt);
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("Respuesta de API (preview):%n%s",
                resumirParaLog(respuesta)));
        return respuesta;
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedorSecuencial() throws IOException, InterruptedException {
        java.util.function.BooleanSupplier cancelada = controlCancelacionPausa::esCancelada;
        java.util.function.BooleanSupplier pausada = controlCancelacionPausa::esPausada;
        GestorMultiProveedor gestorMultiProveedor = new GestorMultiProveedor(
                solicitud,
                config,
                stdout,
                stderr,
                gestorConsola,
                cancelada,
                pausada,
                gestorLogging);
        return gestorMultiProveedor.ejecutarAnalisisMultiProveedor();
    }


    private String resumirParaLog(String texto) {
        // Cuando el modo detallado está activo, mostrar todo sin truncar
        if (config != null && config.esDetallado()) {
            return texto != null ? texto : "";
        }
        if (Normalizador.esVacio(texto)) {
            return "";
        }
        if (texto.length() <= 500) {
            return texto;
        }
        return texto.substring(0, 500) + "...";
    }

    private static String mensajeErrorSolicitudNoDisponible() {
        return I18nUI.tr("Solicitud de analisis no disponible", "Analysis request is not available");
    }

    private static String alertaConfiguracionNoDisponible() {
        return I18nUI.tr("ALERTA: Configuracion de IA no disponible", "ALERTA: AI configuration is unavailable");
    }

    /**
     * Parsea una respuesta JSON usando el parseador interno.
     * Exist for test access only — production code should use the full pipeline.
     */
    ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, config != null ? config.obtenerProveedorAI() : "");
    }
}
