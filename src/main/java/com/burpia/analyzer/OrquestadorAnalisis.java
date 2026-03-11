package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.PromptTruncador;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class OrquestadorAnalisis {
    private static final String ORIGEN_LOG = "OrquestadorAnalisis";
    private static final int MAX_INTENTOS_RETRY = 5;
    private static final int MAX_TRUNCADOS = 3;
    private static final long BACKOFF_INICIAL_MS = 1000L;
    private static final long BACKOFF_MAXIMO_MS = 8000L;
    private static final long DELAY_ENTRE_PROVEEDORES_MS = 2000L;
    private static final String LINEA_SEPARADORA_PROVEEDOR = "========================================";

    private final SolicitudAnalisis solicitud;
    private ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final Callback callback;
    private final Runnable alInicioAnalisis;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
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
                              BooleanSupplier tareaCancelada, 
                              BooleanSupplier tareaPausada) {
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
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);
        this.analizadorHTTP = new AnalizadorHTTP(this.config, this.tareaCancelada, this.tareaPausada, this.gestorLogging);
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
        if (tareaCancelada.getAsBoolean()) {
            throw new InterruptedException(I18nUI.Tareas.MSG_CANCELADO_USUARIO());
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (tareaPausada.getAsBoolean() && !tareaCancelada.getAsBoolean()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    private void esperarConControl(long milisegundos) throws InterruptedException {
        long restante = milisegundos;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
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
     * DRY - datos centralizados.
     */
    private int estimarContextWindow(String modelo) {
        if (Normalizador.esVacio(modelo)) {
            return 4000;
        }
        String m = modelo.toLowerCase();
        if (m.contains("gpt-4o") || m.contains("gpt-4-32k")) return 128000;
        if (m.contains("gpt-4")) return 8192;
        if (m.contains("gpt-3.5-turbo-16k")) return 16384;
        if (m.contains("gpt-3.5")) return 4096;
        if (m.contains("claude-3-5-sonnet") || m.contains("claude-3-opus")) return 200000;
        if (m.contains("claude-3")) return 100000;
        if (m.contains("claude")) return 100000;
        if (m.contains("gemini-1.5-pro")) return 1000000;
        if (m.contains("gemini")) return 32000;
        if (m.contains("llama-3") || m.contains("llama3")) return 8000;
        if (m.contains("llama")) return 4096;
        if (m.contains("mistral")) return 32000;
        return 4000;
    }

    private String llamarAPIAIConRetriesConConfig(ConfiguracionAPI configProveedor) throws IOException {
        AnalizadorHTTP analizadorHTTPProveedor = new AnalizadorHTTP(configProveedor, 
            this.tareaCancelada, this.tareaPausada, this.gestorLogging);

        try {
            verificarCancelacion();
            esperarSiPausada();
            
            String prompt = construirPromptAnalisis();
            String respuesta = analizadorHTTPProveedor.llamarAPI(prompt);
            
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Longitud de respuesta de API: " + respuesta.length() + " caracteres"));
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Respuesta de API (preview):\n" + resumirParaLog(respuesta)));
            return respuesta;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(I18nUI.Tareas.ERROR_RETRY_INTERRUPPIDO(), e);
        } catch (ContextExceededException e) {
            // Para multi-proveedor, no implementamos truncado complejo por ahora
            // Simplemente propagamos el error
            throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO(), e);
        }
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parsearRespuesta(respuestaJson, config != null ? config.obtenerProveedorAI() : "");
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, String proveedor) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedorSecuencial() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        if (Normalizador.esVacia(proveedores)) {
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Multi-consulta: No hay proveedores seleccionados, usando proveedor único"));
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único"));
            return ejecutarAnalisisProveedorUnico();
        }

        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();
        ConfiguracionAPI configOriginal = config;

        try {
            for (String proveedor : proveedores) {
                verificarCancelacion();
                esperarSiPausada();

                if (Normalizador.esVacio(proveedor)) {
                    continue;
                }
                if (!ProveedorAI.existeProveedor(proveedor)) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Proveedor no existe: " + proveedor + ", omitiendo"));
                    continue;
                }

                String modelo = config.obtenerModeloParaProveedor(proveedor);
                if (Normalizador.esVacio(modelo)) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Proveedor " + proveedor + " no tiene modelo configurado, omitiendo"));
                    continue;
                }

                if (!todosHallazgos.isEmpty()) {
                    long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Esperando " + delaySegundos + " segundos antes del siguiente proveedor"));
                    esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
                }

                gestorLogging.info(ORIGEN_LOG, LINEA_SEPARADORA_PROVEEDOR);
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: " + proveedor + " (" + modelo + ")"));

                try {
                    ConfiguracionAPI configProveedor = new ConfiguracionAPI();
                    configProveedor.aplicarDesde(configOriginal);
                    configProveedor.establecerProveedorAI(proveedor);

                    String respuesta = llamarAPIAIConRetriesConConfig(configProveedor);
                    ResultadoAnalisisMultiple resultado = parsearRespuestaConEtiqueta(respuesta, proveedor, modelo);

                    List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: " + proveedor + " completado - " + hallazgosProveedor.size()
                            + " hallazgo(s) encontrado(s)"));
                    todosHallazgos.addAll(hallazgosProveedor);

                } catch (Exception e) {
                    gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Error con " + proveedor), e);
                    proveedoresFallidos.add(proveedor);
                }
            }

            if (!proveedoresFallidos.isEmpty()) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: " + proveedoresFallidos.size() +
                        " proveedor(es) fallaron: " + String.join(", ", proveedoresFallidos)));
            }

            gestorLogging.info(ORIGEN_LOG, LINEA_SEPARADORA_PROVEEDOR);
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados: " + todosHallazgos.size()));

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                    solicitud.obtenerSolicitudHttp(), proveedoresFallidos);

        } finally {
            this.config = configOriginal;
        }
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException {
        String respuesta = llamarAPIAIConRetries();
        return parsearRespuesta(respuesta);
    }

    private ResultadoAnalisisMultiple parsearRespuestaConEtiqueta(String respuestaJson,
            String proveedor,
            String modelo) {
        ResultadoAnalisisMultiple resultado = parsearRespuesta(respuestaJson, proveedor);
        List<Hallazgo> hallazgos = resultado.obtenerHallazgos();
        List<Hallazgo> hallazgosConEtiqueta = new ArrayList<>();

        for (Hallazgo hallazgo : hallazgos) {
            String descripcionOriginal = hallazgo.obtenerHallazgo();
            String etiqueta = I18nUI.Configuracion.TXT_DESCUBIERTO_CON(proveedor, modelo);
            String descripcionConEtiqueta = descripcionOriginal + etiqueta;

            Hallazgo hallazgoEtiquetado = new Hallazgo(
                    hallazgo.obtenerUrl(),
                    hallazgo.obtenerTitulo(),
                    descripcionConEtiqueta,
                    hallazgo.obtenerSeveridad(),
                    hallazgo.obtenerConfianza(),
                    hallazgo.obtenerSolicitudHttp(),
                    hallazgo.obtenerEvidenciaHttp());

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosConEtiqueta,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
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
