package com.burpia.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility class for testing connections to AI providers and checking updates.
 * Provides async operations with proper timeout handling and callbacks.
 */
public class ConnectionTester {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/DragonJAR/BurpIA/releases/latest";
    
    private final ExecutorService executorService;
    private final GestorLoggingUnificado gestorLogging;
    private final Gson gson;
    
    public ConnectionTester() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ConnectionTester-Thread");
            t.setDaemon(true);
            return t;
        });
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(
            new PrintWriter(System.out), 
            new PrintWriter(System.err));
        this.gson = GsonProvider.get();
    }
    
    /**
     * Tests connection to an AI provider
     */
    public void probarConexionProveedor(ConfiguracionAPI config, CallbackConexion callback) {
        if (config == null || callback == null) {
            if (callback != null) {
                callback.alError("Configuration or callback cannot be null");
            }
            return;
        }
        
        String proveedor = config.obtenerProveedorAI();
        String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
        String urlBase = config.obtenerUrlBaseParaProveedor(proveedor);
        
        if (Normalizador.esVacio(proveedor) || Normalizador.esVacio(apiKey) || Normalizador.esVacio(urlBase)) {
            callback.alError("Missing provider configuration");
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                OkHttpClient client = crearClienteParaConfiguracion(config);
                Request request = crearSolicitudTestConexion(proveedor, apiKey, urlBase);
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return "Connection successful";
                    } else {
                        return "HTTP " + response.code() + ": " + response.message();
                    }
                }
            } catch (Exception e) {
                return "Connection failed: " + e.getMessage();
            }
        }, executorService).orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .whenComplete((resultado, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    callback.alError("Connection timeout after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
                } else {
                    callback.alError("Connection error: " + throwable.getMessage());
                }
            } else {
                if (resultado.startsWith("Connection successful")) {
                    callback.alExito(resultado);
                } else {
                    callback.alError(resultado);
                }
            }
        });
    }
    
    /**
     * Fetches available models from AI provider
     */
    public void obtenerModelosDisponibles(ConfiguracionAPI config, CallbackModelos callback) {
        if (config == null || callback == null) {
            if (callback != null) {
                callback.alError("Configuration or callback cannot be null");
            }
            return;
        }
        
        String proveedor = config.obtenerProveedorAI();
        String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
        String urlBase = config.obtenerUrlBaseParaProveedor(proveedor);
        
        if (Normalizador.esVacio(proveedor) || Normalizador.esVacio(apiKey) || Normalizador.esVacio(urlBase)) {
            callback.alError("Missing provider configuration");
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return listarModelosParaProveedor(proveedor, apiKey, urlBase, config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch models: " + e.getMessage(), e);
            }
        }, executorService).orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .whenComplete((modelos, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    callback.alError("Request timeout after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
                } else {
                    callback.alError("Failed to fetch models: " + throwable.getCause().getMessage());
                }
            } else {
                callback.alExito(modelos);
            }
        });
    }
    
    /**
     * Checks for application updates from GitHub
     */
    public void verificarActualizaciones(String versionActual, CallbackActualizacion callback) {
        if (Normalizador.esVacio(versionActual) || callback == null) {
            if (callback != null) {
                callback.alError("Version or callback cannot be null");
            }
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(GITHUB_API_URL)
                    .get()
                    .build();
                
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.message());
                    }
                    
                    String responseBody = response.body().string();
                    JsonObject release = gson.fromJson(responseBody, JsonObject.class);
                    
                    String ultimaVersion = release.get("tag_name").getAsString();
                    String nombreRelease = release.get("name").getAsString();
                    String cuerpoRelease = release.has("body") ? release.get("body").getAsString() : "";
                    String urlDescarga = "";
                    
                    if (release.has("assets") && release.get("assets").isJsonArray()) {
                        JsonArray assets = release.get("assets").getAsJsonArray();
                        for (JsonElement asset : assets) {
                            JsonObject assetObj = asset.getAsJsonObject();
                            if (assetObj.get("name").getAsString().endsWith(".jar")) {
                                urlDescarga = assetObj.get("browser_download_url").getAsString();
                                break;
                            }
                        }
                    }
                    
                    boolean hayActualizacion = compararVersiones(versionActual, ultimaVersion) < 0;
                    
                    return new InfoActualizacion(ultimaVersion, nombreRelease, cuerpoRelease, 
                        urlDescarga, hayActualizacion);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to check updates: " + e.getMessage(), e);
            }
        }, executorService).orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .whenComplete((info, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    callback.alError("Request timeout after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
                } else {
                    callback.alError("Failed to check updates: " + throwable.getCause().getMessage());
                }
            } else {
                callback.alExito(info);
            }
        });
    }
    
    /**
     * Performs a generic network operation with timeout
     */
    public void ejecutarOperacionRed(Request request, int timeoutSegundos, CallbackRed callback) {
        if (request == null || callback == null) {
            if (callback != null) {
                callback.alError("Request or callback cannot be null");
            }
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(timeoutSegundos, TimeUnit.SECONDS)
                    .readTimeout(timeoutSegundos, TimeUnit.SECONDS)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    return new RespuestaRed(response.code(), response.message(), responseBody, 
                        response.isSuccessful());
                }
            } catch (Exception e) {
                throw new RuntimeException("Network operation failed: " + e.getMessage(), e);
            }
        }, executorService).orTimeout(timeoutSegundos, TimeUnit.SECONDS)
        .whenComplete((respuesta, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    callback.alError("Operation timeout after " + timeoutSegundos + " seconds");
                } else {
                    callback.alError("Network error: " + throwable.getCause().getMessage());
                }
            } else {
                callback.alExito(respuesta);
            }
        });
    }
    
    /**
     * Shuts down the executor service
     */
    public void cerrar() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
private List<String> parsearModelosDesdeRespuesta(String proveedor, String responseBody) {
        List<String> modelos = new ArrayList<>();
        
        try {
            if ("OpenAI".equals(proveedor)) {
                JsonObject openaiResponse = gson.fromJson(responseBody, JsonObject.class);
                if (openaiResponse.has("data") && openaiResponse.get("data").isJsonArray()) {
                    JsonArray data = openaiResponse.get("data").getAsJsonArray();
                    for (JsonElement element : data) {
                        JsonObject model = element.getAsJsonObject();
                        if (model.has("id")) {
                            modelos.add(model.get("id").getAsString());
                        }
                    }
                }
            } else if ("Claude".equals(proveedor)) {
                JsonObject claudeResponse = gson.fromJson(responseBody, JsonObject.class);
                if (claudeResponse.has("data") && claudeResponse.get("data").isJsonArray()) {
                    JsonArray data = claudeResponse.get("data").getAsJsonArray();
                    for (JsonElement element : data) {
                        JsonObject model = element.getAsJsonObject();
                        if (model.has("id")) {
                            modelos.add(model.get("id").getAsString());
                        }
                    }
                }
            } else if ("Gemini".equals(proveedor)) {
                JsonObject geminiResponse = gson.fromJson(responseBody, JsonObject.class);
                if (geminiResponse.has("models") && geminiResponse.get("models").isJsonArray()) {
                    JsonArray models = geminiResponse.get("models").getAsJsonArray();
                    for (JsonElement element : models) {
                        JsonObject model = element.getAsJsonObject();
                        if (model.has("name")) {
                            String nombre = model.get("name").getAsString();
                            if (nombre.startsWith("models/")) {
                                modelos.add(nombre.substring(7));
                            } else {
                                modelos.add(nombre);
                            }
                        }
                    }
                }
            } else if ("Ollama".equals(proveedor)) {
                JsonObject ollamaResponse = gson.fromJson(responseBody, JsonObject.class);
                if (ollamaResponse.has("models") && ollamaResponse.get("models").isJsonArray()) {
                    JsonArray models = ollamaResponse.get("models").getAsJsonArray();
                    for (JsonElement element : models) {
                        JsonObject model = element.getAsJsonObject();
                        if (model.has("name")) {
                            modelos.add(model.get("name").getAsString());
                        }
                    }
                }
            } else {
                gestorLogging.info("ConnectionTester", "Unsupported provider for model listing: " + proveedor);
            }
        } catch (Exception e) {
            gestorLogging.error("ConnectionTester", "Error parsing models response", e);
        }
        
        return modelos;
    }
    
    private int compararVersiones(String version1, String version2) {
        String[] v1Parts = version1.replaceAll("[^0-9.]", "").split("\\.");
        String[] v2Parts = version2.replaceAll("[^0-9.]", "").split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }
        return 0;
    }
    
    private OkHttpClient crearClienteParaConfiguracion(ConfiguracionAPI config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        if (config.ignorarErroresSSL()) {
            configurarSslInseguro(builder);
        }
        
        return builder.build();
    }
    
    private void configurarSslInseguro(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[] {};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            gestorLogging.error("ConnectionTester", 
                "SSL inseguro no se pudo configurar, se usará configuración por defecto: " + e.getMessage());
        }
    }
    
    private Request crearSolicitudTestConexion(String proveedor, String apiKey, String urlBase) {
        String endpoint;
        Request.Builder builder = new Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json");
        
        if ("OpenAI".equals(proveedor) || "Z.ai".equals(proveedor) || "minimax".equals(proveedor)) {
            endpoint = urlBase + "/models";
            if (Normalizador.noEsVacio(apiKey)) {
                builder.addHeader("Authorization", "Bearer " + apiKey.trim());
            }
        } else if ("Claude".equals(proveedor)) {
            endpoint = urlBase + "/messages";
            builder.addHeader("x-api-key", apiKey);
            builder.addHeader("anthropic-version", "2023-06-01");
        } else if ("Gemini".equals(proveedor)) {
            endpoint = urlBase + "/models";
            if (Normalizador.noEsVacio(apiKey)) {
                endpoint += "?key=" + apiKey.trim();
            }
        } else if ("Ollama".equals(proveedor)) {
            endpoint = urlBase + "/api/tags";
            // Ollama no requiere API key
        } else {
            // Default para proveedores custom (OpenAI-compatible)
            endpoint = urlBase + "/models";
            if (Normalizador.noEsVacio(apiKey)) {
                builder.addHeader("Authorization", "Bearer " + apiKey.trim());
            }
        }
        
        return builder.url(endpoint).get().build();
    }
    
    private Request crearSolicitudListarModelos(String proveedor, String apiKey, String urlBase) {
        return crearSolicitudTestConexion(proveedor, apiKey, urlBase);
    }
    
    private List<String> listarModelosParaProveedor(String proveedor, String apiKey, String urlBase, ConfiguracionAPI config) {
        try {
            OkHttpClient client = crearClienteParaConfiguracion(config);
            Request request = crearSolicitudListarModelos(proveedor, apiKey, urlBase);
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
                
                String responseBody = response.body().string();
                return parsearModelosDesdeRespuesta(proveedor, responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch models: " + e.getMessage(), e);
        }
    }
    
    /**
     * Callback interface for connection testing
     */
    public interface CallbackConexion {
        void alExito(String mensaje);
        void alError(String error);
    }
    
    /**
     * Callback interface for model fetching
     */
    public interface CallbackModelos {
        void alExito(List<String> modelos);
        void alError(String error);
    }
    
    /**
     * Callback interface for update checking
     */
    public interface CallbackActualizacion {
        void alExito(InfoActualizacion info);
        void alError(String error);
    }
    
    /**
     * Callback interface for generic network operations
     */
    public interface CallbackRed {
        void alExito(RespuestaRed respuesta);
        void alError(String error);
    }
    
    /**
     * Information about an available update
     */
    public static class InfoActualizacion {
        private final String version;
        private final String nombreRelease;
        private final String notas;
        private final String urlDescarga;
        private final boolean hayActualizacion;
        
        public InfoActualizacion(String version, String nombreRelease, String notas, 
            String urlDescarga, boolean hayActualizacion) {
            this.version = version;
            this.nombreRelease = nombreRelease;
            this.notas = notas;
            this.urlDescarga = urlDescarga;
            this.hayActualizacion = hayActualizacion;
        }
        
        public String obtenerVersion() { return version; }
        public String obtenerNombreRelease() { return nombreRelease; }
        public String obtenerNotas() { return notas; }
        public String obtenerUrlDescarga() { return urlDescarga; }
        public boolean hayActualizacion() { return hayActualizacion; }
    }
    
    /**
     * Network operation response
     */
    public static class RespuestaRed {
        private final int codigoEstado;
        private final String mensaje;
        private final String cuerpo;
        private final boolean exitoso;
        
        public RespuestaRed(int codigoEstado, String mensaje, String cuerpo, boolean exitoso) {
            this.codigoEstado = codigoEstado;
            this.mensaje = mensaje;
            this.cuerpo = cuerpo;
            this.exitoso = exitoso;
        }
        
        public int obtenerCodigoEstado() { return codigoEstado; }
        public String obtenerMensaje() { return mensaje; }
        public String obtenerCuerpo() { return cuerpo; }
        public boolean fueExitoso() { return exitoso; }
    }
}