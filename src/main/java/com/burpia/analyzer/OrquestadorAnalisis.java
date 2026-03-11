package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ConstantesJsonAI;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GsonProvider;
import com.burpia.util.JsonParserUtil;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.ParserRespuestasAI;
import com.burpia.util.PromptTruncador;
import com.burpia.util.ReparadorJson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final Gson gson;
    private final ConstructorPrompts constructorPrompt;
    private final GestorLoggingUnificado gestorLogging;
    private final AnalizadorHTTP analizadorHTTP;
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
        this.gson = GsonProvider.get();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);
        this.analizadorHTTP = new AnalizadorHTTP(this.config, this.tareaCancelada, this.tareaPausada, this.gestorLogging);
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

        gestorLogging.info(ORIGEN_LOG, "[" + nombreHilo + "] Orquestador iniciado para URL: " + solicitud.obtenerUrl());
        gestorLogging.verbose(ORIGEN_LOG, "[" + nombreHilo + "] Hash de solicitud: " + solicitud.obtenerHashSolicitud());

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

            int retrasoSegundos = config.obtenerRetrasoSegundos();
            gestorLogging.verbose(ORIGEN_LOG, "[" + nombreHilo + "] Durmiendo por " + retrasoSegundos + " segundos antes de llamar a la API");
            esperarConControl(retrasoSegundos * 1000L);

            gestorLogging.info(ORIGEN_LOG, "Analizando: " + solicitud.obtenerUrl());

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            gestorLogging.verbose(ORIGEN_LOG, "DIAGNOSTICO: multiHabilitado=" + multiHabilitado + ", proveedoresConfig=" +
                    (proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null"));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                gestorLogging.verbose(ORIGEN_LOG, "DIAGNOSTICO: Ejecutando multi-proveedor con " + proveedoresConfig.size() + " proveedores");
                resultadoMultiple = ejecutarAnalisisMultiProveedorSecuencial();
            } else {
                if (multiHabilitado) {
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Multi-proveedor habilitado pero solo " +
                            (proveedoresConfig != null ? proveedoresConfig.size() : 0) +
                            " proveedor(es) configurado(s). Usando proveedor único: " + config.obtenerProveedorAI());
                } else {
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Usando proveedor único: " + config.obtenerProveedorAI());
                }
                String respuesta = llamarAPIAIConRetries();
                resultadoMultiple = parsearRespuesta(respuesta);
            }

            long duracion = System.currentTimeMillis() - tiempoInicio;
            gestorLogging.info(ORIGEN_LOG, "Análisis completado: " + solicitud.obtenerUrl() + " (tomo " + duracion + "ms)");
            gestorLogging.verbose(ORIGEN_LOG, "[" + nombreHilo + "] Severidad maxima: " + resultadoMultiple.obtenerSeveridadMaxima());

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
            gestorLogging.verbose(ORIGEN_LOG, "No se pudo notificar inicio de análisis: " + e.getMessage());
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
            throw new InterruptedException("Tarea cancelada por usuario");
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
        gestorLogging.verbose(ORIGEN_LOG, "Construyendo prompt para URL: " + solicitud.obtenerUrl());
        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        gestorLogging.verbose(ORIGEN_LOG, "Longitud de prompt: " + prompt.length() + " caracteres");
        gestorLogging.verbose(ORIGEN_LOG, "Prompt (preview):\n" + resumirParaLog(prompt));
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
                gestorLogging.info(ORIGEN_LOG, "Longitud de respuesta de API: " + respuesta.length() + " caracteres");
                gestorLogging.verbose(ORIGEN_LOG, "Respuesta de API (preview):\n" + resumirParaLog(respuesta));
                return respuesta;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Sistema de retry cancelado/interrumpido", e);
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
            
            gestorLogging.info(ORIGEN_LOG, "Longitud de respuesta de API: " + respuesta.length() + " caracteres");
            gestorLogging.verbose(ORIGEN_LOG, "Respuesta de API (preview):\n" + resumirParaLog(respuesta));
            return respuesta;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Sistema de retry cancelado/interrumpido", e);
        } catch (ContextExceededException e) {
            // Para multi-proveedor, no implementamos truncado complejo por ahora
            // Simplemente propagamos el error
            throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO(), e);
        }
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        gestorLogging.verbose(ORIGEN_LOG, "Parseando respuesta JSON");
        List<Hallazgo> hallazgos = new ArrayList<>();
        String respuestaOriginal = respuestaJson != null ? respuestaJson : "";

        try {
            String jsonReparado = ReparadorJson.repararJson(respuestaOriginal);
            String respuestaProcesada = respuestaOriginal;
            if (jsonReparado != null && !jsonReparado.equals(respuestaOriginal)) {
                gestorLogging.verbose(ORIGEN_LOG, "JSON reparado exitosamente");
                respuestaProcesada = jsonReparado;
            }

            String proveedor = config.obtenerProveedorAI() != null ? config.obtenerProveedorAI() : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaProcesada, proveedor);
            if (Normalizador.esVacio(contenido)) {
                contenido = respuestaProcesada;
            }

            gestorLogging.verbose(ORIGEN_LOG, "Contenido extraído - Longitud: " + contenido.length() + " caracteres");
            gestorLogging.verbose(ORIGEN_LOG, "Contenido (preview):\n" + resumirParaLog(contenido));

            try {
                String contenidoLimpio = limpiarBloquesMarkdownJson(contenido);
                JsonElement raiz = gson.fromJson(contenidoLimpio, JsonElement.class);
                List<JsonObject> objetosHallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, ConstantesJsonAI.CAMPOS_HALLAZGOS);

                if (!objetosHallazgos.isEmpty()) {
                    gestorLogging.verbose(ORIGEN_LOG, "Se encontraron " + objetosHallazgos.size() + " hallazgos en JSON");
                    for (JsonObject obj : objetosHallazgos) {
                        agregarHallazgoNormalizado(
                                hallazgos,
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA),
                                JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA));
                    }
                } else {
                    gestorLogging.verbose(ORIGEN_LOG, "JSON sin objetos de hallazgo, intentando parsing de texto plano");
                    hallazgos.addAll(parsearTextoPlano(contenido));
                }
            } catch (Exception e) {
                gestorLogging.verbose(ORIGEN_LOG, "No se pudo parsear como JSON de hallazgos: " + e.getMessage());
                List<Hallazgo> hallazgosNoEstrictos = parsearHallazgosJsonNoEstricto(contenido);
                if (!hallazgosNoEstrictos.isEmpty()) {
                    gestorLogging.verbose(ORIGEN_LOG, "Fallback JSON no estricto recuperó " + hallazgosNoEstrictos.size() + " hallazgos");
                    hallazgos.addAll(hallazgosNoEstrictos);
                } else {
                    gestorLogging.verbose(ORIGEN_LOG, "Intentando parsing de texto plano como fallback");
                    hallazgos.addAll(parsearTextoPlano(contenido));
                }
            }

            if (!respuestaProcesada.equals(respuestaOriginal)) {
                String contenidoOriginal = ParserRespuestasAI.extraerContenido(respuestaOriginal, proveedor);
                if (Normalizador.esVacio(contenidoOriginal)) {
                    contenidoOriginal = respuestaOriginal;
                }

                List<Hallazgo> hallazgosOriginalesNoEstrictos = parsearHallazgosJsonNoEstricto(contenidoOriginal);
                if (hallazgosOriginalesNoEstrictos.size() > hallazgos.size()) {
                    gestorLogging.verbose(ORIGEN_LOG, "Se detecto perdida de hallazgos tras reparación JSON; " +
                            "se conserva parseo no estricto del payload original (" +
                            hallazgosOriginalesNoEstrictos.size() + " > " + hallazgos.size() + ")");
                    hallazgos.clear();
                    hallazgos.addAll(hallazgosOriginalesNoEstrictos);
                }
            }

            gestorLogging.verbose(ORIGEN_LOG, "Total de hallazgos parseados: " + hallazgos.size());

            return new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    hallazgos,
                    solicitud.obtenerSolicitudHttp(),
                    Collections.emptyList());

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido de parseo";
            gestorLogging.error(ORIGEN_LOG,
                    "Error crítico al parsear respuesta de API para " + solicitud.obtenerUrl() + ": " + errorMsg);
            gestorLogging.verbose(ORIGEN_LOG, "JSON fallido (preview):\n" + resumirParaLog(respuestaJson));

            throw new RuntimeException(descripcionErrorParseo(errorMsg), e);
        }
    }

    private List<Hallazgo> parsearHallazgosJsonNoEstricto(String contenido) {
        if (Normalizador.esVacio(contenido)) {
            return new ArrayList<>();
        }

        JsonArray arrayHallazgos = ParserRespuestasAI.extraerArrayJsonInteligente(contenido, gson);

        if (arrayHallazgos != null && arrayHallazgos.size() > 0) {
            return convertirArrayAHallazgos(arrayHallazgos);
        }

        JsonArray arrayRecuperado = ParserRespuestasAI.extraerHallazgosPorDelimitadores(contenido, gson);

        if (arrayRecuperado != null && arrayRecuperado.size() > 0) {
            gestorLogging.verbose(ORIGEN_LOG, "Recuperación extrema: " + arrayRecuperado.size() + " hallazgos");
            return convertirArrayAHallazgos(arrayRecuperado);
        }

        return parsearHallazgosCampoPorCampo(contenido);
    }

    private List<Hallazgo> convertirArrayAHallazgos(JsonArray array) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (JsonElement item : array) {
            if (item != null && item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                agregarHallazgoNormalizado(
                        hallazgos,
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_TITULO),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_DESCRIPCION),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_SEVERIDAD),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_CONFIANZA),
                        JsonParserUtil.extraerCampoFlexible(obj, ConstantesJsonAI.CAMPOS_EVIDENCIA));
            }
        }
        gestorLogging.verbose(ORIGEN_LOG, "Array JSON convertido a " + hallazgos.size() + " hallazgos");
        return hallazgos;
    }

    private List<Hallazgo> parsearHallazgosCampoPorCampo(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        String bloqueHallazgos = extraerBloqueArrayHallazgos(contenido);
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return hallazgos;
        }

        for (String objeto : extraerObjetosNoEstrictos(bloqueHallazgos)) {
            String bloqueHallazgo = normalizarObjetoNoEstricto(objeto);
            if (Normalizador.esVacio(bloqueHallazgo)) {
                continue;
            }

            String titulo = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_TITULO, bloqueHallazgo);
            String descripcion = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_DESCRIPCION, bloqueHallazgo);
            String severidad = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_SEVERIDAD, bloqueHallazgo);
            String confianza = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_CONFIANZA, bloqueHallazgo);
            String evidencia = extraerCampoConFallback(ConstantesJsonAI.CAMPOS_EVIDENCIA, bloqueHallazgo);

            agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia);
        }

        if (!hallazgos.isEmpty()) {
            gestorLogging.verbose(ORIGEN_LOG, "Parseo campo por campo recuperó " + hallazgos.size() + " hallazgos");
        }

        return hallazgos;
    }

    private String extraerCampoConFallback(String[] campos, String bloque) {
        for (String campo : campos) {
            String valor = ParserRespuestasAI.extraerCampoNoEstricto(campo, bloque);
            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    private String extraerBloqueArrayHallazgos(String contenido) {
        int indiceHallazgos = -1;
        String[] claves = { "\"hallazgos\"", "\"findings\"", "\"issues\"", "\"vulnerabilidades\"" };
        for (String clave : claves) {
            int indice = contenido.indexOf(clave);
            if (indice >= 0 && (indiceHallazgos < 0 || indice < indiceHallazgos)) {
                indiceHallazgos = indice;
            }
        }
        if (indiceHallazgos < 0) {
            return "";
        }
        int inicioArray = contenido.indexOf('[', indiceHallazgos);
        if (inicioArray < 0) {
            return "";
        }

        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = inicioArray; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
                continue;
            }
            if (!enComillas) {
                if (c == '[') {
                    profundidad++;
                } else if (c == ']') {
                    profundidad--;
                    if (profundidad == 0) {
                        return contenido.substring(inicioArray + 1, i);
                    }
                }
            }
        }
        return "";
    }

    private String normalizarObjetoNoEstricto(String objeto) {
        if (objeto == null) {
            return "";
        }
        String bloque = objeto.trim();
        if (Normalizador.esVacio(bloque)) {
            return "";
        }
        if (!bloque.startsWith("{")) {
            bloque = "{" + bloque;
        }
        if (!bloque.endsWith("}")) {
            bloque = bloque + "}";
        }
        return bloque;
    }

    private List<String> extraerObjetosNoEstrictos(String bloqueHallazgos) {
        List<String> objetos = new ArrayList<>();
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return objetos;
        }
        int inicioObjeto = -1;
        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = 0; i < bloqueHallazgos.length(); i++) {
            char c = bloqueHallazgos.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
                continue;
            }
            if (enComillas) {
                continue;
            }
            if (c == '{') {
                if (profundidad == 0) {
                    inicioObjeto = i;
                }
                profundidad++;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0 && inicioObjeto >= 0) {
                    objetos.add(bloqueHallazgos.substring(inicioObjeto, i + 1));
                    inicioObjeto = -1;
                }
            }
        }
        if (objetos.isEmpty()) {
            String[] partes = bloqueHallazgos.split("\\}\\s*,\\s*\\{");
            for (String parte : partes) {
                if (Normalizador.noEsVacio(parte)) {
                    objetos.add(parte);
                }
            }
        }
        return objetos;
    }

    private String etiquetaEvidencia() {
        return "en".equalsIgnoreCase(config.obtenerIdiomaUi()) ? "Evidence" : "Evidencia";
    }

    private String limpiarBloquesMarkdownJson(String contenido) {
        String limpio = contenido != null ? contenido.trim() : "";
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(?:json)?\\s*", "");
            limpio = limpio.replaceFirst("\\s*```\\s*$", "");
        }
        return limpio.trim();
    }

    private void agregarHallazgoNormalizado(List<Hallazgo> destino,
            String tituloRaw,
            String descripcionRaw,
            String severidadRaw,
            String confianzaRaw,
            String evidenciaRaw) {
        if (destino == null) {
            return;
        }
        String titulo = normalizarTextoSimple(tituloRaw, tituloPorDefecto());
        String descripcion = normalizarTextoSimple(descripcionRaw, "");
        String evidencia = normalizarTextoSimple(evidenciaRaw, "");
        if (Normalizador.esVacio(descripcion)) {
            descripcion = evidencia;
        } else if (Normalizador.noEsVacio(evidencia) && !descripcion.contains(evidencia)) {
            StringBuilder sb = new StringBuilder(
                    descripcion.length() + evidencia.length() + 32);
            sb.append(descripcion).append("\n").append(etiquetaEvidencia()).append(": ").append(evidencia);
            descripcion = sb.toString();
        }
        if (Normalizador.esVacio(descripcion)) {
            descripcion = descripcionPorDefecto();
        }
        String severidad = Hallazgo.normalizarSeveridad(normalizarTextoSimple(severidadRaw, Hallazgo.SEVERIDAD_INFO));
        String confianza = Hallazgo.normalizarConfianza(normalizarTextoSimple(confianzaRaw, Hallazgo.CONFIANZA_BAJA));
        destino.add(new Hallazgo(
                solicitud.obtenerUrl(),
                titulo,
                descripcion,
                severidad,
                confianza,
                solicitud.obtenerSolicitudHttp()));
        if (config.esDetallado()) {
            gestorLogging.verbose(ORIGEN_LOG, "Hallazgo agregado: " + titulo + " (" + severidad + ", " + confianza + ")");
        }
    }

    private String normalizarTextoSimple(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto != null ? porDefecto : "";
        }
        String normalizado = Normalizador.normalizarTexto(valor);
        if (normalizado.isEmpty()) {
            return porDefecto != null ? porDefecto : "";
        }
        return normalizado;
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedorSecuencial() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        if (Normalizador.esVacia(proveedores)) {
            gestorLogging.info(ORIGEN_LOG, "Multi-consulta: No hay proveedores seleccionados, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            gestorLogging.info(ORIGEN_LOG, "Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único");
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
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Proveedor no existe: " + proveedor + ", omitiendo");
                    continue;
                }

                String modelo = config.obtenerModeloParaProveedor(proveedor);
                if (Normalizador.esVacio(modelo)) {
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Proveedor " + proveedor + " no tiene modelo configurado, omitiendo");
                    continue;
                }

                if (!todosHallazgos.isEmpty()) {
                    long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Esperando " + delaySegundos + " segundos antes del siguiente proveedor");
                    esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
                }

                gestorLogging.info(ORIGEN_LOG, LINEA_SEPARADORA_PROVEEDOR);
                gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: " + proveedor + " (" + modelo + ")");

                try {
                    ConfiguracionAPI configProveedor = new ConfiguracionAPI();
                    configProveedor.aplicarDesde(configOriginal);
                    configProveedor.establecerProveedorAI(proveedor);

                    String respuesta = llamarAPIAIConRetriesConConfig(configProveedor);
                    ResultadoAnalisisMultiple resultado = parsearRespuestaConEtiqueta(respuesta, proveedor, modelo);

                    List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: " + proveedor + " completado - " + hallazgosProveedor.size()
                            + " hallazgo(s) encontrado(s)");
                    todosHallazgos.addAll(hallazgosProveedor);

                } catch (Exception e) {
                    gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Error con " + proveedor + ": " + e.getMessage());
                    proveedoresFallidos.add(proveedor);
                }
            }

            if (!proveedoresFallidos.isEmpty()) {
                gestorLogging.error(ORIGEN_LOG, "PROVEEDOR: " + proveedoresFallidos.size() +
                        " proveedor(es) fallaron: " + String.join(", ", proveedoresFallidos));
            }

            gestorLogging.info(ORIGEN_LOG, LINEA_SEPARADORA_PROVEEDOR);
            gestorLogging.info(ORIGEN_LOG, "PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados: " + todosHallazgos.size());

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
        ResultadoAnalisisMultiple resultado = parsearRespuesta(respuestaJson);
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

    private List<Hallazgo> parsearTextoPlano(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (Normalizador.esVacio(contenido)) {
            return hallazgos;
        }

        try {
            String[] lineas = contenido.split("\n");
            StringBuilder descripcion = new StringBuilder();
            String severidad = Hallazgo.SEVERIDAD_INFO;
            String confianza = Hallazgo.CONFIANZA_BAJA;

            for (String linea : lineas) {
                String lineaNormalizada = linea.trim();
                String lineaLower = lineaNormalizada.toLowerCase();

                if (lineaLower.contains("título:") || lineaLower.contains("title:")) {
                    if (descripcion.length() > 0) {
                        agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza);
                        descripcion.setLength(0);
                    }
                    descripcion.append(lineaNormalizada.replaceFirst("(?i)(título:|title:)", "").trim()).append(" - ");
                } else if (lineaLower.contains("severidad:") || lineaLower.contains("severity:")) {
                    String severidadLinea = lineaNormalizada.replaceFirst("(?i)(severidad:|severity:)", "").trim();
                    severidad = Hallazgo.normalizarSeveridad(severidadLinea);
                } else if (lineaLower.contains("confianza:") || lineaLower.contains("confidence:")) {
                    String confianzaLinea = lineaNormalizada.replaceFirst("(?i)(confianza:|confidence:)", "").trim();
                    confianza = Hallazgo.normalizarConfianza(confianzaLinea);
                } else if (lineaLower.contains("descripción:") || lineaLower.contains("description:")) {
                    descripcion.append(lineaNormalizada.replaceFirst("(?i)(descripción:|description:)", "").trim()).append(" ");
                } else if (Normalizador.noEsVacio(lineaNormalizada)) {
                    descripcion.append(lineaNormalizada).append(" ");
                }
            }

            if (descripcion.length() > 0) {
                agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza);
            }

        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, "Error parsing texto plano: " + e.getMessage());
        }

        return hallazgos;
    }

    private void agregarHallazgoDesdeDescripcion(List<Hallazgo> hallazgos, String descripcion, String severidad, String confianza) {
        String descripcionLimpia = normalizarTextoSimple(descripcion, "");
        if (Normalizador.esVacio(descripcionLimpia)) {
            return;
        }
        
        String titulo = tituloPorDefecto();
        if (descripcionLimpia.length() > 50) {
            int espacio = descripcionLimpia.indexOf(' ', 47);
            if (espacio > 0) {
                titulo = descripcionLimpia.substring(0, espacio) + "...";
            } else {
                titulo = descripcionLimpia.substring(0, 47) + "...";
            }
        } else {
            titulo = descripcionLimpia;
        }
        
        hallazgos.add(new Hallazgo(
                solicitud.obtenerUrl(),
                titulo,
                descripcionLimpia,
                severidad,
                confianza,
                solicitud.obtenerSolicitudHttp()));
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

    private static String tituloPorDefecto() {
        return I18nUI.tr("Sin título", "Untitled");
    }

    private static String descripcionPorDefecto() {
        return I18nUI.tr("Sin descripción", "No description");
    }

    private static String descripcionErrorParseo(String detalle) {
        return I18nUI.trf("Error al parsear respuesta: %s", "Error parsing response: %s", detalle);
    }
}