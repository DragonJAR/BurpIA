package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import com.burpia.util.PromptTruncador;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class OrquestadorAnalisis {
    private static final String ORIGEN_LOG = "OrquestadorAnalisis";

    private final SolicitudAnalisis solicitud;
    // Snapshot inmutable: el llamador crea una copia via crearSnapshot() antes de pasarla.
    // No se reasigna ni muta después del constructor; 'final' garantiza visibilidad entre hilos.
    private final ConfiguracionAPI config;
    private final Runnable alInicioAnalisis;
    private final ControlCancelacionPausa controlCancelacionPausa;
    private final ConstructorPrompts constructorPrompt;
    private final GestorLoggingUnificado gestorLogging;
    private final AnalizadorHTTP analizadorHTTP;
    private final ParseadorRespuestasAI parseador;
    private final PromptTruncador promptTruncador;

    public OrquestadorAnalisis(SolicitudAnalisis solicitud,
                              ConfiguracionAPI config,
                              PrintWriter stdout,
                              PrintWriter stderr,
                              Runnable alInicioAnalisis,
                              GestorConsolaGUI gestorConsola,
                              ControlCancelacionPausa controlCancelacionPausa) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        PrintWriter out = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        PrintWriter err = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.alInicioAnalisis = alInicioAnalisis;
        this.controlCancelacionPausa = controlCancelacionPausa != null ? controlCancelacionPausa : new ControlCancelacionPausa(null, null);
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, out, err, null, null);
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

        controlCancelacionPausa.verificarCancelacion();
        controlCancelacionPausa.esperarSiPausada();
        notificarInicioAnalisis();

        String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
        if (Normalizador.noEsVacio(alertaConfiguracion)) {
            gestorLogging.error(ORIGEN_LOG, alertaConfiguracion);
            throw new IOException(alertaConfiguracion);
        }

        // El limitador de tasa y el despacho multi-proveedor viven en el
        // llamador (AnalizadorAI): este pipeline solo ejecuta el análisis de
        // proveedor único. No adquirimos ni liberamos permisos aquí para no
        // romper el balance del semáforo compartido.
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Analizando: %s", solicitud.obtenerUrl()));
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("PROVEEDOR: Usando proveedor único: %s",
                config.obtenerProveedorAI()));

        String respuesta = llamarAPIAIConRetries();
        ResultadoAnalisisMultiple resultadoMultiple = parseador.parsearRespuesta(
                respuesta, solicitud, config.obtenerProveedorAI());

        long duracion = System.currentTimeMillis() - tiempoInicio;
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Análisis completado: %s (tomo %dms)",
                solicitud.obtenerUrl(), duracion));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Severidad maxima: %s",
                nombreHilo, resultadoMultiple.obtenerSeveridadMaxima()));

        return resultadoMultiple;
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
        // config nunca es null: el constructor normaliza a new ConfiguracionAPI()
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

    /**
     * Parsea una respuesta JSON usando el parseador interno.
     * Exist for test access only — production code should use the full pipeline.
     */
    ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, config != null ? config.obtenerProveedorAI() : "");
    }
}
