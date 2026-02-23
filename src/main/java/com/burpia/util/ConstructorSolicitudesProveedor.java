package com.burpia.util;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;




public final class ConstructorSolicitudesProveedor {

    private static final long CACHE_MODELOS_GEMINI_MS = 5 * 60 * 1000L;
    private static final int MAX_ENTRADAS_CACHE_GEMINI = 128;
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final Map<String, CacheModelosGemini> CACHE_GEMINI = new ConcurrentHashMap<>();

    private ConstructorSolicitudesProveedor() {
    }

    public static SolicitudPreparada construirSolicitud(ConfiguracionAPI config,
                                                        String prompt,
                                                        OkHttpClient clienteHttp) throws IOException {
        String proveedor = config.obtenerProveedorAI();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            proveedor = "OpenAI";
        }

        String endpoint = ConfiguracionAPI.construirUrlApiProveedor(
            proveedor,
            config.obtenerUrlApi(),
            config.obtenerModelo()
        );
        String modeloUsado = config.obtenerModelo();
        JsonObject carga = new JsonObject();
        Request.Builder builder = new Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json");
        String advertencia = null;

        switch (proveedor) {
            case "Claude":
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
                break;

            case "Gemini":
                String modeloConfigurado = modeloUsado;
                List<String> modelosValidosGemini = listarModelosGemini(
                    ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()),
                    config.obtenerClaveApi(),
                    clienteHttp
                );
                if (!modelosValidosGemini.contains(modeloConfigurado)) {
                    modeloUsado = modelosValidosGemini.get(0);
                    advertencia = "Modelo Gemini \"" + modeloConfigurado +
                        "\" no soportado para generateContent. Se usó \"" + modeloUsado + "\".";
                }
                endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) +
                    "/models/" + modeloUsado + ":generateContent";
                JsonArray contenidos = new JsonArray();
                JsonObject contenido = new JsonObject();
                JsonArray partes = new JsonArray();
                JsonObject parte = new JsonObject();
                parte.addProperty("text", prompt);
                partes.add(parte);
                contenido.add("parts", partes);
                contenidos.add(contenido);
                carga.add("contents", contenidos);
                builder.addHeader("x-goog-api-key", config.obtenerClaveApi());
                break;

            case "Ollama":
                endpoint = ConfiguracionAPI.extraerUrlBase(config.obtenerUrlApi()) + "/api/chat";
                carga.addProperty("model", modeloUsado);
                carga.addProperty("stream", false);
                agregarMensajeUsuario(carga, prompt);
                break;

            case "OpenAI":
            case ProveedorAI.PROVEEDOR_CUSTOM:
            case "Z.ai":
            case "minimax":
                endpoint = ConfiguracionAPI.construirUrlApiProveedor(proveedor, config.obtenerUrlApi(), modeloUsado);
                prepararSolicitudOpenAICompatible(carga, builder, config, prompt, modeloUsado);
                break;

            default:
                endpoint = ConfiguracionAPI.construirUrlApiProveedor(proveedor, config.obtenerUrlApi(), modeloUsado);
                prepararSolicitudOpenAICompatible(carga, builder, config, prompt, modeloUsado);
                break;
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

    private static void prepararSolicitudOpenAICompatible(JsonObject carga, Request.Builder builder, ConfiguracionAPI config, String prompt, String modelo) {
        carga.addProperty("model", modelo);
        carga.addProperty("stream", false);
        if ("OpenAI".equals(config.obtenerProveedorAI())) {
            carga.addProperty("input", prompt);
        } else {
            agregarMensajeUsuario(carga, prompt);
        }
        agregarAuthorizationSiExiste(builder, config.obtenerClaveApi());
    }

    public static List<String> listarModelosGemini(String urlBase,
                                                   String apiKey,
                                                   OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (base == null || base.trim().isEmpty()) {
            throw new IOException("URL base de Gemini vacia o invalida");
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
            throw new IOException("URL base de Gemini invalida: " + base);
        }
        HttpUrl.Builder urlBuilder = urlModelos.newBuilder();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            urlBuilder.addQueryParameter("key", apiKey);
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = clienteHttp.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "sin cuerpo";
                throw new IOException("HTTP " + response.code() + ": " + err);
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = parsearModelosGemini(body);
            if (modelos.isEmpty()) {
                throw new IOException("Gemini no reportó modelos compatibles con generateContent");
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
        if (apiKey == null || apiKey.isEmpty()) {
            return "sin_clave";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return convertirHex(hash, 12);
        } catch (Exception e) {
            return Integer.toHexString(apiKey.hashCode());
        }
    }

    private static String convertirHex(byte[] bytes, int maxBytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        int limite = Math.min(bytes.length, Math.max(1, maxBytes));
        StringBuilder sb = new StringBuilder(limite * 2);
        for (int i = 0; i < limite; i++) {
            int valor = bytes[i] & 0xff;
            sb.append(HEX[valor >>> 4]);
            sb.append(HEX[valor & 0x0f]);
        }
        return sb.toString();
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
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (base == null || base.trim().isEmpty()) {
            throw new IOException("URL base de Ollama vacia o invalida");
        }

        String endpoint = base + "/api/tags";
        Request request = new Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = clienteHttp.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "sin cuerpo";
                throw new IOException("HTTP " + response.code() + ": " + err);
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(body);
            if (modelos.isEmpty()) {
                throw new IOException("Ollama no reportó modelos válidos en /api/tags");
            }
            return modelos;
        }
    }

    public static List<String> listarModelosOpenAI(String urlBase, String apiKey, OkHttpClient clienteHttp) throws IOException {
        String base = ConfiguracionAPI.extraerUrlBase(urlBase);
        if (base == null || base.trim().isEmpty()) {
            throw new IOException("URL base vacia o invalida");
        }

        String endpoint = base + "/models";
        Request.Builder builder = new Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json");

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiKey.trim());
        }

        try (Response response = clienteHttp.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "sin cuerpo";
                throw new IOException("HTTP " + response.code() + ": " + err);
            }
            String body = response.body() != null ? response.body().string() : "{}";
            List<String> modelos = parsearModelosOpenAI(body);
            if (modelos.isEmpty()) {
                throw new IOException("No se encontraron modelos validos en la respuesta");
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
        // Ordenar alfabéticamente para mejor experiencia de usuario
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
