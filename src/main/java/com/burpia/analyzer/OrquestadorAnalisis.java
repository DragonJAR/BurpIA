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
    private static final int MAX_INTENTOS_RETRY = 5;
    private static final int MAX_TRUNCADOS = 3;
    private static final long BACKOFF_INICIAL_MS = 1000L;
    private static final long BACKOFF_MAXIMO_MS = 8000L;

    private final SolicitudAnalisis solicitud;
    private ConfiguracionAPI config;
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
            gestorLogging.error(ORIGEN_LOG, "[" + nombreHilo + "] " + error);
            throw new IOException(error);
        }

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Orquestador iniciado para URL: " + solicitud.obtenerUrl()));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Hash de solicitud: " + solicitud.obtenerHashSolicitud()));

        try {
            verificarCancelacion();
            esperarSiPausada();
            notificarInicioAnalisis();
            
            String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
            if (Normalizador.noEsVacio(alertaConfiguracion)) {
                gestorLogging.error(ORIGEN_LOG, alertaConfiguracion);
                throw new IOException(alertaConfiguracion);
            }

            // NOTA: El limitador YA fue adquirido por el llamador (AnalizadorAI o FlowAnalysisManager)
            // No adquirimos aquí para evitar doble adquisición que causa permisos negativos

            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Analizando: " + solicitud.obtenerUrl()));

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("DIAGNOSTICO: multiHabilitado=" + multiHabilitado + ", proveedoresConfig=" +
                    (proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null")));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("DIAGNOSTICO: Ejecutando multi-proveedor con " + proveedoresConfig.size() + " proveedores"));
                resultadoMultiple = ejecutarAnalisisMultiProveedorSecuencial();
            } else {
                if (multiHabilitado) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Multi-proveedor habilitado pero solo " +
                            (proveedoresConfig != null ? proveedoresConfig.size() : 0) +
                            " proveedor(es) configurado(s). Usando proveedor único: " + config.obtenerProveedorAI()));
                } else {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Usando proveedor único: " + config.obtenerProveedorAI()));
                }
                String respuesta = llamarAPIAIConRetries();
                resultadoMultiple = parsearRespuesta(respuesta);
            }

            long duracion = System.currentTimeMillis() - tiempoInicio;
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Análisis completado: " + solicitud.obtenerUrl() + " (tomo " + duracion + "ms)"));
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Severidad maxima: " + resultadoMultiple.obtenerSeveridadMaxima()));

            return resultadoMultiple;

        } finally {
            // NOTA: El limitador es liberado por el llamador (AnalizadorAI o FlowAnalysisManager)
            // No liberamos aquí para evitar doble liberación que causaría permisos negativos
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

    private void verificarCancelacion() throws InterruptedException {
        controlCancelacionPausa.verificarCancelacion();
    }

    private void esperarSiPausada() throws InterruptedException {
        controlCancelacionPausa.esperarSiPausada();
    }

    private void esperarConControl(long milisegundos) throws InterruptedException {
        controlCancelacionPausa.esperarConControl(milisegundos);
    }

    private String construirPromptAnalisis() {
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Construyendo prompt para URL: " + solicitud.obtenerUrl()));
        String promptPreconstruido = solicitud.obtenerPromptPreconstruido();
        String prompt = Normalizador.noEsVacio(promptPreconstruido)
            ? promptPreconstruido
            : constructorPrompt.construirPromptAnalisis(solicitud);
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Longitud de prompt: " + prompt.length() + " caracteres"));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Prompt (preview):\n" + resumirParaLog(prompt)));
        return prompt;
    }

    private String llamarAPIAIConRetries() throws IOException {
        String promptActual = construirPromptAnalisis();
        int intentosTruncado = 0;
        
        while (intentosTruncado <= MAX_TRUNCADOS) {
            try {
                verificarCancelacion();
                esperarSiPausada();
                
                String respuesta = analizadorHTTP.llamarAPI(promptActual);
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Longitud de respuesta de API: " + respuesta.length() + " caracteres"));
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Respuesta de API (preview):\n" + resumirParaLog(respuesta)));
                return respuesta;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(I18nUI.Tareas.ERROR_RETRY_INTERRUPPIDO(), e);
            } catch (ContextExceededException e) {
                // Error de contexto - podemos truncar y reintentar
                if (intentosTruncado < MAX_TRUNCADOS) {
                    intentosTruncado++;
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.TRUNCANDO(intentosTruncado));
                    
                    // Calcular tokens objetivo
                    int tokensObjetivo = obtenerTokensObjetivo(e.obtenerCuerpoError());
                    
                    // Truncar prompt
                    int longitudOriginal = promptActual.length();
                    promptActual = promptTruncador.truncarPrompt(promptActual, tokensObjetivo);
                    
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.TRUNCADO(longitudOriginal, promptActual.length()));
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.RETRY_CON_TRUNCADO());
                    continue;
                }
                // Agotamos intentos de truncado
                gestorLogging.error(ORIGEN_LOG, I18nLogs.ContextoExcedido.MAX_INTENTOS(MAX_TRUNCADOS));
                throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO(), e);
            }
        }
        
        // No debería llegar aquí, pero por seguridad
        throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO());
    }
    
    /**
     * Calcula el número objetivo de tokens basado en el error y configuración.
     */
    private int obtenerTokensObjetivo(String cuerpoError) {
        // Intentar extraer límite del error
        ContextExceededDetector detector = new ContextExceededDetector();
        int limiteExtraido = detector.extraerLimiteTokens(cuerpoError);
        
        if (limiteExtraido > 0) {
            // Dejar margen para respuesta (25% del context window)
            return promptTruncador.calcularTokensDisponibles(limiteExtraido, limiteExtraido / 4);
        }
        
        // Fallback: usar configuración del modelo
        String proveedor = config.obtenerProveedorAI();
        String modelo = config.obtenerModelo();
        int maxTokens = config.obtenerMaxTokensParaProveedor(proveedor);
        
        if (maxTokens > 0) {
            return promptTruncador.calcularTokensDisponibles(maxTokens, maxTokens / 4);
        }
        
        // Último fallback: estimar según modelo conocido
        int contextWindow = estimarContextWindow(modelo);
        return promptTruncador.calcularTokensDisponibles(contextWindow, contextWindow / 4);
    }
    
    /**
     * Estima el context window de un modelo conocido.
     * Delegado a ConfiguracionAPI.estimarContextWindow() para centralización.
     */
    private int estimarContextWindow(String modelo) {
        return ConfiguracionAPI.estimarContextWindow(modelo);
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parsearRespuesta(respuestaJson, config != null ? config.obtenerProveedorAI() : "");
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, String proveedor) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
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
        return I18nUI.tr("ALERTA: Configuracion de IA no disponible", "ALERT: AI configuration is unavailable");
    }

}
