package com.burpia.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class ConstructorSolicitudesProveedor {

    private static final long CACHE_MODELOS_GEMINI_MS = 5 * 60 * 1000L;
    private static final int MAX_ENTRADAS_CACHE_GEMINI = 128;
    private static final Map<String, CacheModelosGemini> CACHE_GEMINI =
        Collections.synchronizedMap(new LinkedHashMap<String, CacheModelosGemini>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheModelosGemini> eldest) {
                return size() > MAX_ENTRADAS_CACHE_GEMINI;
            }
        });

    private ConstructorSolicitudesProveedor() {
    }

    public static List<String> listarModelosRemotosProveedor(String proveedor,
                                                             String urlBase,
                                                             String apiKey,
                                                             OkHttpClient clienteHttp) throws IOException {
        String proveedorNormalizado = ProveedorAI.normalizarProveedor(proveedor);
        if (Normalizador.esVacio(proveedorNormalizado)) {
            throw new IOException(
                I18nUI.Conexion.ERROR_PROVEEDOR_LISTA_MODELOS_NO_SOPORTADO(I18nUI.Conexion.ERROR_DESCONOCIDO()));
        }

        switch (proveedorNormalizado) {
            case "Gemini":
                return listarModelosGemini(urlBase, apiKey, clienteHttp);
            case "Ollama":
                return listarModelosOllama(urlBase, clienteHttp);
            case "Ollama Cloud":
                return listarModelosOllamaCloud(urlBase, apiKey, clienteHttp);
            case "Claude":
                return listarModelosClaude(urlBase, apiKey, clienteHttp);
            case "OpenAI":
            case "Moonshot (Kimi)":
            case "DeepSeek":
            case "xAI":
            case "Sakana":
            case "LM Studio":
                return listarModelosCompatiblesOpenAI(urlBase, apiKey, clienteHttp);
            default:
                if (ProveedorAI.esProveedorCustom(proveedorNormalizado)) {
                    return listarModelosCompatiblesOpenAI(urlBase, apiKey, clienteHttp);
                }
                throw new IOException(
                    I18nUI.Conexion.ERROR_PROVEEDOR_LISTA_MODELOS_NO_SOPORTADO(proveedorNormalizado));
        }
    }

    public static SolicitudPreparada construirSolicitud(ConfiguracionAPI config,
                                                        String prompt,
                                                        OkHttpClient clienteHttp) throws IOException {
        String proveedor = config.obtenerProveedorAI();
        if (Normalizador.esVacio(proveedor)) {
            proveedor = "OpenAI";
        }

        String modeloUsado = config.obtenerModelo();
        String endpoint;
        JsonObject carga = new JsonObject();
        Request.Builder builder = new Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json");
        String advertencia = null;

        if ("Claude".equals(proveedor)) {
            endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) + "/messages";
            carga.addProperty("model", modeloUsado);
            carga.addProperty("max_tokens", config.obtenerMaxTokensParaProveedor(proveedor));
            JsonArray mensajesClaude = new JsonArray();
            JsonObject mensajeClaude = new JsonObject();
            mensajeClaude.addProperty("role", "user");
            mensajeClaude.addProperty("content", prompt);
            mensajesClaude.add(mensajeClaude);
            carga.add("messages", mensajesClaude);
            builder.addHeader("x-api-key", config.obtenerClaveApi());
            builder.addHeader("anthropic-version", "2023-06-01");
        } else if ("Gemini".equals(proveedor)) {
            String modeloConfigurado = modeloUsado;
            // La validación remota de modelos es una conveniencia, NO un
            // requisito para enviar la solicitud. Si /models falla (red, cuota,
            // 5xx) no abortamos el análisis: usamos el modelo configurado tal
            // cual. Solo cuando el listado responde y el modelo no está en él
            // hacemos fallback al primer modelo válido.
            try {
                List<String> modelosValidosGemini = listarModelosGemini(
                    ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()),
                    config.obtenerClaveApi(),
                    clienteHttp
                );
                if (!modelosValidosGemini.isEmpty() && !modelosValidosGemini.contains(modeloConfigurado)) {
                    modeloUsado = modelosValidosGemini.get(0);
                    advertencia = I18nUI.Conexion.WARNING_MODELO_GEMINI_FALLBACK(modeloConfigurado, modeloUsado);
                }
            } catch (IOException e) {
                // Best-effort: continuar con el modelo configurado.
                modeloUsado = modeloConfigurado;
            }
            endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) +
                "/models/" + URLEncoder.encode(modeloUsado, StandardCharsets.UTF_8) + ":generateContent";
            JsonArray contenidos = new JsonArray();
            JsonObject contenido = new JsonObject();
            JsonArray partes = new JsonArray();
            JsonObject parte = new JsonObject();
            parte.addProperty("text", prompt);
            partes.add(parte);
            contenido.add("parts", partes);
            contenidos.add(contenido);
            carga.add("contents", contenidos);
            int maxTokensGemini = config.obtenerMaxTokensParaProveedor(proveedor);
            if (maxTokensGemini > 0) {
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("maxOutputTokens", maxTokensGemini);
                carga.add("generationConfig", generationConfig);
            }
            builder.addHeader("x-goog-api-key", config.obtenerClaveApi());
        } else if ("Ollama".equals(proveedor)) {
            endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) + "/api/chat";
            carga.addProperty("model", modeloUsado);
            carga.addProperty("stream", false);
            agregarMensajeUsuario(carga, prompt);
            agregarOpcionesOllama(carga, config, proveedor);
        } else if ("Ollama Cloud".equals(proveedor)) {
            // Mismo body shape que Ollama local (/api/chat con messages array y
            // stream=false), pero con Authorization: Bearer porque el endpoint
            // cloud requiere autenticación.
            endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) + "/api/chat";
            carga.addProperty("model", modeloUsado);
            carga.addProperty("stream", false);
            agregarMensajeUsuario(carga, prompt);
            agregarOpcionesOllama(carga, config, proveedor);
            agregarAuthorizationSiExiste(builder, config.obtenerClaveApi());
        } else {
            // Familia OpenAI-compatible (OpenAI, Z.ai, minimax, DeepSeek, xAI,
            // Moonshot (Kimi), LM Studio, custom) y cualquier proveedor
            // desconocido: se tratan con el mismo formato OpenAI por defecto.
            endpoint = ConfiguracionAPI.construirUrlApiProveedor(proveedor, config.obtenerUrlApi(), modeloUsado);
            prepararSolicitudOpenAICompatible(carga, builder, config, prompt, modeloUsado, proveedor);
        }

        Request request = builder
            .url(endpoint)
            .post(RequestBody.create(carga.toString(), MediaType.parse("application/json")))
            .build();
        return new SolicitudPreparada(request, endpoint, modeloUsado, advertencia);
    }

    private static void agregarMensajeUsuario(JsonObject carga, String prompt) {
        JsonArray mensajes = new JsonArray();
        JsonObject mensajeUsuario = new JsonObject();
        mensajeUsuario.addProperty("role", "user");
        mensajeUsuario.addProperty("content", prompt);
        mensajes.add(mensajeUsuario);
        carga.add("messages", mensajes);
    }

    private static void agregarAuthorizationSiExiste(Request.Builder builder, String apiKey) {
        if (apiKey == null) {
            return;
        }
        String limpia = apiKey.trim();
        if (!limpia.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + limpia);
        }
    }

    /**
     * Agrega el límite de tokens de salida al payload de Ollama dentro del
     * objeto {@code options} usando la clave nativa {@code num_predict}.
     */
    private static void agregarOpcionesOllama(JsonObject carga, ConfiguracionAPI config, String proveedor) {
        int maxTokens = config.obtenerMaxTokensParaProveedor(proveedor);
        if (maxTokens > 0) {
            JsonObject options = new JsonObject();
            options.addProperty("num_predict", maxTokens);
            carga.add("options", options);
        }
    }

    private static void prepararSolicitudOpenAICompatible(JsonObject carga, Request.Builder builder,
            ConfiguracionAPI config, String prompt, String modelo, String proveedor) {
        carga.addProperty("model", modelo);
        carga.addProperty("stream", false);
        int maxTokens = config.obtenerMaxTokensParaProveedor(proveedor);
        if ("OpenAI".equals(proveedor)) {
            // Responses API: campo de entrada 'input' y límite 'max_output_tokens'.
            carga.addProperty("input", prompt);
            if (maxTokens > 0) {
                carga.addProperty("max_output_tokens", maxTokens);
            }
        } else {
            // Chat Completions: 'messages' y límite clásico 'max_tokens'.
            agregarMensajeUsuario(carga, prompt);
            if (maxTokens > 0) {
                carga.addProperty("max_tokens", maxTokens);
            }
        }
        agregarAuthorizationSiExiste(builder, config.obtenerClaveApi());
    }

    public static List<String> listarModelosGemini(String urlBase,
                                                   String apiKey,
                                                   OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (Normalizador.esVacio(base)) {
            throw new IOException(I18nUI.Conexion.ERROR_URL_BASE_GEMINI_INVALIDA());
        }
        long ahora = System.currentTimeMillis();
        depurarCacheGemini(ahora);

        String cacheKey = construirClaveCacheGemini(base, apiKey);
        CacheModelosGemini cache = CACHE_GEMINI.get(cacheKey);
        if (cache != null && (ahora - cache.timestampMs) < CACHE_MODELOS_GEMINI_MS) {
            return cache.modelos;
        }

        HttpUrl urlModelos = HttpUrl.parse(base + "/models");
        if (urlModelos == null) {
            throw new IOException(I18nUI.Conexion.ERROR_URL_BASE_GEMINI_INVALIDA(base));
        }

        // La API key viaja en el header x-goog-api-key, NO como query param
        // (?key=...): las query strings se loguean en proxies/historiales y
        // esta extensión corre detrás de Burp, exponiendo la credencial.
        Request.Builder solicitudBuilder = new Request.Builder()
            .url(urlModelos)
            .addHeader("Accept", "application/json");
        if (Normalizador.noEsVacio(apiKey)) {
            solicitudBuilder.addHeader("x-goog-api-key", apiKey.trim());
        }
        Request request = solicitudBuilder.build();

        try (Response response = clienteHttp.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : I18nUI.Conexion.DETALLE_SIN_CUERPO();
                throw new IOException(I18nUI.Conexion.DETALLE_HTTP(response.code(), err));
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = parsearModelosGemini(body);
            if (modelos.isEmpty()) {
                throw new IOException(I18nUI.Conexion.ERROR_GEMINI_SIN_MODELOS_COMPATIBLES());
            }
            CACHE_GEMINI.put(cacheKey, new CacheModelosGemini(modelos, ahora));
            return modelos;
        }
    }

    private static String construirClaveCacheGemini(String base, String apiKey) {
        String claveNormalizada = apiKey != null ? apiKey.trim() : "";
        return base + "|" + huellaApiKey(claveNormalizada);
    }

    private static String huellaApiKey(String apiKey) {
        if (Normalizador.esVacio(apiKey)) {
            return "sin_clave";
        }
        String hash = HttpUtils.generarHash(apiKey.getBytes(StandardCharsets.UTF_8));
        return hash.length() >= 12 ? hash.substring(0, 12) : hash;
    }

    private static void depurarCacheGemini(long ahoraMs) {
        if (CACHE_GEMINI.isEmpty()) {
            return;
        }

        CACHE_GEMINI.entrySet().removeIf(entry ->
            entry == null
                || entry.getValue() == null
                || (ahoraMs - entry.getValue().timestampMs) >= CACHE_MODELOS_GEMINI_MS
        );

        int excedente = CACHE_GEMINI.size() - MAX_ENTRADAS_CACHE_GEMINI;
        if (excedente <= 0) {
            return;
        }

        List<Map.Entry<String, CacheModelosGemini>> entradas = new ArrayList<>(CACHE_GEMINI.entrySet());
        entradas.sort(Comparator.comparingLong(entry -> entry.getValue().timestampMs));

        for (int i = 0; i < excedente && i < entradas.size(); i++) {
            Map.Entry<String, CacheModelosGemini> entry = entradas.get(i);
            if (entry != null) {
                CACHE_GEMINI.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    public static List<String> listarModelosOllama(String urlBase, OkHttpClient clienteHttp) throws IOException {
        return ejecutarListadoOllamaApiTags(urlBase, null, clienteHttp);
    }

    public static List<String> listarModelosOllamaCloud(String urlBase,
                                                        String apiKey,
                                                        OkHttpClient clienteHttp) throws IOException {
        return ejecutarListadoOllamaApiTags(urlBase, apiKey, clienteHttp);
    }

    /**
     * Helper DRY que centraliza el listado de modelos vía {@code /api/tags}
     * (formato Ollama). Si se proporciona {@code apiKey}, se envía
     * {@code Authorization: Bearer} (necesario para Ollama Cloud); si es
     * null/vacío, no se envía header de auth (Ollama local).
     */
    private static List<String> ejecutarListadoOllamaApiTags(String urlBase,
                                                              String apiKey,
                                                              OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (Normalizador.esVacio(base)) {
            throw new IOException(I18nUI.Conexion.ERROR_URL_BASE_OLLAMA_INVALIDA());
        }

        String endpoint = base + "/api/tags";
        Request.Builder builder = new Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json");
        agregarAuthorizationSiExiste(builder, apiKey);

        try (Response response = clienteHttp.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : I18nUI.Conexion.DETALLE_SIN_CUERPO();
                throw new IOException(I18nUI.Conexion.DETALLE_HTTP(response.code(), err));
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(body);
            if (modelos.isEmpty()) {
                throw new IOException(I18nUI.Conexion.ERROR_OLLAMA_SIN_MODELOS_VALIDOS());
            }
            return modelos;
        }
    }

    public static List<String> listarModelosClaude(String urlBase,
                                                   String apiKey,
                                                   OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (Normalizador.esVacio(base)) {
            throw new IOException(I18nUI.Conexion.ERROR_URL_BASE_MODELOS_INVALIDA());
        }

        String endpoint = base + "/models";
        Request.Builder builder = new Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json")
            .addHeader("anthropic-version", "2023-06-01");

        if (Normalizador.noEsVacio(apiKey)) {
            builder.addHeader("x-api-key", apiKey.trim());
        }

        try (Response response = clienteHttp.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : I18nUI.Conexion.DETALLE_SIN_CUERPO();
                throw new IOException(I18nUI.Conexion.DETALLE_HTTP(response.code(), err));
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = parsearModelosOpenAI(body);
            if (modelos.isEmpty()) {
                throw new IOException(I18nUI.Conexion.ERROR_MODELOS_RESPUESTA_VACIA());
            }
            return modelos;
        }
    }

    public static List<String> listarModelosOpenAI(String urlBase, String apiKey, OkHttpClient clienteHttp) throws IOException {
        return listarModelosCompatiblesOpenAI(urlBase, apiKey, clienteHttp);
    }

    public static List<String> listarModelosCompatiblesOpenAI(String urlBase,
                                                              String apiKey,
                                                              OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (Normalizador.esVacio(base)) {
            throw new IOException(I18nUI.Conexion.ERROR_URL_BASE_MODELOS_INVALIDA());
        }

        String endpoint = base + "/models";
        Request.Builder builder = new Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json");

        if (Normalizador.noEsVacio(apiKey)) {
            builder.addHeader("Authorization", "Bearer " + apiKey.trim());
        }

        try (Response response = clienteHttp.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : I18nUI.Conexion.DETALLE_SIN_CUERPO();
                throw new IOException(I18nUI.Conexion.DETALLE_HTTP(response.code(), err));
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = parsearModelosOpenAI(body);
            if (modelos.isEmpty()) {
                throw new IOException(I18nUI.Conexion.ERROR_MODELOS_RESPUESTA_VACIA());
            }
            return modelos;
        }
    }

    private static List<String> parsearModelosOpenAI(String body) {
        JsonElement element;
        try {
            element = JsonParser.parseString(body);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (!element.isJsonObject()) {
            return Collections.emptyList();
        }

        JsonElement dataElement = element.getAsJsonObject().get("data");
        if (dataElement == null || !dataElement.isJsonArray()) {
            return Collections.emptyList();
        }
        JsonArray data = dataElement.getAsJsonArray();

        List<String> result = new ArrayList<>();
        for (JsonElement item : data) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject modelObj = item.getAsJsonObject();
            String id = obtenerTexto(modelObj, "id");
            if (!id.isEmpty() && !result.contains(id)) {
                result.add(id);
            }
        }

        Collections.sort(result);
        return result;
    }

    private static List<String> parsearModelosGemini(String body) {
        JsonElement element;
        try {
            element = JsonParser.parseString(body);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (!element.isJsonObject()) {
            return Collections.emptyList();
        }

        JsonElement modelsElement = element.getAsJsonObject().get("models");
        if (modelsElement == null || !modelsElement.isJsonArray()) {
            return Collections.emptyList();
        }
        JsonArray models = modelsElement.getAsJsonArray();

        List<String> result = new ArrayList<>();
        for (JsonElement modelElem : models) {
            if (!modelElem.isJsonObject()) {
                continue;
            }
            JsonObject modelObj = modelElem.getAsJsonObject();
            JsonElement methodsElement = modelObj.get("supportedGenerationMethods");
            JsonArray methods = (methodsElement != null && methodsElement.isJsonArray())
                ? methodsElement.getAsJsonArray()
                : null;
            if (!soportaGenerateContent(methods)) {
                continue;
            }
            String name = obtenerTexto(modelObj, "name");
            String modelo = normalizarNombreModeloGemini(name);
            if (!modelo.isEmpty() && !result.contains(modelo)) {
                result.add(modelo);
            }
        }
        return result;
    }

    private static boolean soportaGenerateContent(JsonArray methods) {
        if (methods == null) {
            return false;
        }
        for (JsonElement method : methods) {
            if (method != null && method.isJsonPrimitive()
                && "generateContent".equalsIgnoreCase(method.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static String obtenerTexto(JsonObject obj, String field) {
        if (obj == null || field == null) {
            return "";
        }
        JsonElement value = obj.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return "";
        }
        try {
            return value.getAsString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String normalizarNombreModeloGemini(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.trim();
        if (limpio.startsWith("models/")) {
            limpio = limpio.substring("models/".length());
        }
        return limpio;
    }

    public static final class SolicitudPreparada {
        public final Request request;
        public final String endpoint;
        public final String modeloUsado;
        public final String advertencia;

        private SolicitudPreparada(Request request, String endpoint, String modeloUsado, String advertencia) {
            this.request = request;
            this.endpoint = endpoint;
            this.modeloUsado = modeloUsado;
            this.advertencia = advertencia;
        }
    }

    private static final class CacheModelosGemini {
        private final List<String> modelos;
        private final long timestampMs;

        private CacheModelosGemini(List<String> modelos, long timestampMs) {
            this.modelos = Collections.unmodifiableList(new ArrayList<>(modelos));
            this.timestampMs = timestampMs;
        }
    }
}
