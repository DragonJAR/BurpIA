package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link ConstructorSolicitudesProveedor}.
 * <p>
 * Verifica la construcción de solicitudes HTTP para diferentes proveedores AI,
 * el listado de modelos disponibles, y el sistema de caché para Gemini.
 * </p>
 */
@DisplayName("ConstructorSolicitudesProveedor Tests")
class ConstructorSolicitudesProveedorTest {

    private OkHttpClient clienteHttp;
    private MockWebServer servidor;

    @BeforeEach
    void setUp() throws Exception {
        clienteHttp = new OkHttpClient.Builder().build();
        servidor = new MockWebServer();
        limpiarCacheGemini();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (servidor != null) {
            servidor.shutdown();
        }
        if (clienteHttp != null) {
            clienteHttp.dispatcher().executorService().shutdown();
        }
        limpiarCacheGemini();
    }

    @SuppressWarnings("unchecked")
    private void limpiarCacheGemini() throws Exception {
        Field cacheField = ConstructorSolicitudesProveedor.class.getDeclaredField("CACHE_GEMINI");
        cacheField.setAccessible(true);
        Map<String, ?> cache = (Map<String, ?>) cacheField.get(null);
        cache.clear();
    }

    @Nested
    @DisplayName("listarModelosGemini")
    class ListarModelosGemini {

        @Test
        @DisplayName("Lista modelos válidos desde respuesta Gemini")
        void listaModelosValidosDesdeRespuesta() throws Exception {
            String respuesta = "{"
                + "\"models\":["
                + "{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":[\"generateContent\"]},"
                + "{\"name\":\"models/gemini-1.5-flash-002\",\"supportedGenerationMethods\":[\"generateContent\"]}"
                + "]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosGemini(
                servidor.url("/v1beta").toString(), "test-key", clienteHttp);

            assertEquals(2, modelos.size());
            assertTrue(modelos.contains("gemini-1.5-pro-002"));
            assertTrue(modelos.contains("gemini-1.5-flash-002"));
        }

        @Test
        @DisplayName("Filtra modelos que no soportan generateContent")
        void filtraModelosSinGenerateContent() throws Exception {
            String respuesta = "{"
                + "\"models\":["
                + "{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":[\"generateContent\"]},"
                + "{\"name\":\"models/embedding-001\",\"supportedGenerationMethods\":[\"embedContent\"]}"
                + "]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosGemini(
                servidor.url("/v1beta").toString(), "test-key", clienteHttp);

            assertEquals(1, modelos.size());
            assertEquals("gemini-1.5-pro-002", modelos.get(0));
        }

        @Test
        @DisplayName("Cache de modelos Gemini no colisiona cuando dos API keys tienen mismo hashCode")
        void cacheNoColisionaConMismoHashCode() throws Exception {
            assertEquals("Aa".hashCode(), "BB".hashCode(), "Aa y BB deben tener mismo hashCode");

            String respuestaModelosA = "{"
                + "\"models\":[{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":[\"generateContent\"]}]}";
            String respuestaModelosB = "{"
                + "\"models\":[{\"name\":\"models/gemini-1.5-flash-002\",\"supportedGenerationMethods\":[\"generateContent\"]}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuestaModelosA));
            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuestaModelosB));
            servidor.start();

            String urlBase = servidor.url("/v1beta").toString();

            List<String> modelosA = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "Aa", clienteHttp);
            List<String> modelosB = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "BB", clienteHttp);

            assertEquals(List.of("gemini-1.5-pro-002"), modelosA);
            assertEquals(List.of("gemini-1.5-flash-002"), modelosB);
            assertEquals(2, servidor.getRequestCount(), "Debe hacer dos peticiones distintas");
        }

        @Test
        @DisplayName("Usa cache para peticiones repetidas con misma API key")
        void usaCacheParaPeticionesRepetidas() throws Exception {
            String respuesta = "{"
                + "\"models\":[{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":[\"generateContent\"]}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            String urlBase = servidor.url("/v1beta").toString();

            List<String> modelos1 = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "key1", clienteHttp);
            List<String> modelos2 = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "key1", clienteHttp);

            assertEquals(modelos1, modelos2);
            assertEquals(1, servidor.getRequestCount(), "Solo debe hacer una petición HTTP gracias al cache");
        }

        @Test
        @DisplayName("Lanza IOException para respuesta HTTP no exitosa")
        void lanzaExcepcionParaRespuestaNoExitosa() throws Exception {
            servidor.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosGemini(
                    servidor.url("/v1beta").toString(), "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException cuando no hay modelos compatibles")
        void lanzaExcepcionCuandoNoHayModelosCompatibles() throws Exception {
            String respuesta = "{\"models\":[]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosGemini(
                    servidor.url("/v1beta").toString(), "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException para URL base vacía")
        void lanzaExcepcionParaUrlVacia() {
            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosGemini("", "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException para URL base null")
        void lanzaExcepcionParaUrlNull() {
            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosGemini(null, "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Envía API key como parámetro de query")
        void enviaApiKeyComoQueryParam() throws Exception {
            String respuesta = "{\"models\":[{\"name\":\"models/gemini-1.5-pro\",\"supportedGenerationMethods\":[\"generateContent\"]}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            ConstructorSolicitudesProveedor.listarModelosGemini(
                servidor.url("/v1beta").toString(), "my-api-key", clienteHttp);

            RecordedRequest request = servidor.takeRequest();
            assertTrue(request.getPath().contains("key=my-api-key"));
        }
    }

    @Nested
    @DisplayName("listarModelosOllama")
    class ListarModelosOllama {

        @Test
        @DisplayName("Lista modelos válidos desde respuesta Ollama")
        void listaModelosValidosDesdeRespuesta() throws Exception {
            String respuesta = "{\"models\":[{\"name\":\"llama3:latest\"},{\"name\":\"mistral:latest\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosOllama(
                servidor.url("/api").toString(), clienteHttp);

            assertEquals(2, modelos.size());
            assertTrue(modelos.contains("llama3:latest"));
            assertTrue(modelos.contains("mistral:latest"));
        }

        @Test
        @DisplayName("Lanza IOException para respuesta HTTP no exitosa")
        void lanzaExcepcionParaRespuestaNoExitosa() throws Exception {
            servidor.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOllama(
                    servidor.url("/api").toString(), clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException cuando no hay modelos válidos")
        void lanzaExcepcionCuandoNoHayModelos() throws Exception {
            servidor.enqueue(new MockResponse().setResponseCode(200).setBody("{\"models\":[]}"));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOllama(
                    servidor.url("/api").toString(), clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException para URL base vacía")
        void lanzaExcepcionParaUrlVacia() {
            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOllama("", clienteHttp));
        }
    }

    @Nested
    @DisplayName("listarModelosOpenAI")
    class ListarModelosOpenAI {

        @Test
        @DisplayName("Lista modelos válidos desde respuesta OpenAI")
        void listaModelosValidosDesdeRespuesta() throws Exception {
            String respuesta = "{\"data\":[{\"id\":\"gpt-4o\"},{\"id\":\"gpt-4o-mini\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosOpenAI(
                servidor.url("/v1").toString(), "test-key", clienteHttp);

            assertEquals(2, modelos.size());
            assertTrue(modelos.contains("gpt-4o"));
            assertTrue(modelos.contains("gpt-4o-mini"));
        }

        @Test
        @DisplayName("Modelos se retornan ordenados alfabéticamente")
        void modelosRetornadosOrdenados() throws Exception {
            String respuesta = "{\"data\":[{\"id\":\"gpt-4o-mini\"},{\"id\":\"gpt-4o\"},{\"id\":\"gpt-3.5-turbo\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosOpenAI(
                servidor.url("/v1").toString(), "test-key", clienteHttp);

            assertEquals(List.of("gpt-3.5-turbo", "gpt-4o", "gpt-4o-mini"), modelos);
        }

        @Test
        @DisplayName("Deduplica modelos repetidos")
        void deduplicaModelosRepetidos() throws Exception {
            String respuesta = "{\"data\":[{\"id\":\"gpt-4o\"},{\"id\":\"gpt-4o\"},{\"id\":\"gpt-4o-mini\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            List<String> modelos = ConstructorSolicitudesProveedor.listarModelosOpenAI(
                servidor.url("/v1").toString(), "test-key", clienteHttp);

            assertEquals(2, modelos.size());
            assertEquals(List.of("gpt-4o", "gpt-4o-mini"), modelos);
        }

        @Test
        @DisplayName("Lanza IOException para respuesta HTTP no exitosa")
        void lanzaExcepcionParaRespuestaNoExitosa() throws Exception {
            servidor.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOpenAI(
                    servidor.url("/v1").toString(), "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException cuando no hay modelos válidos")
        void lanzaExcepcionCuandoNoHayModelos() throws Exception {
            servidor.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[]}"));
            servidor.start();

            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOpenAI(
                    servidor.url("/v1").toString(), "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Lanza IOException para URL base vacía")
        void lanzaExcepcionParaUrlVacia() {
            assertThrows(IOException.class, () ->
                ConstructorSolicitudesProveedor.listarModelosOpenAI("", "test-key", clienteHttp));
        }

        @Test
        @DisplayName("Envía Authorization header con API key")
        void enviaAuthorizationHeader() throws Exception {
            String respuesta = "{\"data\":[{\"id\":\"gpt-4o\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            ConstructorSolicitudesProveedor.listarModelosOpenAI(
                servidor.url("/v1").toString(), "my-api-key", clienteHttp);

            RecordedRequest request = servidor.takeRequest();
            assertEquals("Bearer my-api-key", request.getHeader("Authorization"));
        }

        @Test
        @DisplayName("No envía Authorization header si API key es null")
        void noEnviaAuthorizationSiApiKeyNull() throws Exception {
            String respuesta = "{\"data\":[{\"id\":\"gpt-4o\"}]}";

            servidor.enqueue(new MockResponse().setResponseCode(200).setBody(respuesta));
            servidor.start();

            ConstructorSolicitudesProveedor.listarModelosOpenAI(
                servidor.url("/v1").toString(), null, clienteHttp);

            RecordedRequest request = servidor.takeRequest();
            assertNull(request.getHeader("Authorization"));
        }
    }

    @Nested
    @DisplayName("construirSolicitud")
    class ConstruirSolicitud {

        @Test
        @DisplayName("Construye solicitud válida para OpenAI")
        void construyeSolicitudOpenAI() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest("OpenAI", "gpt-4o", "https://api.openai.com/v1", "sk-test");

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
            assertTrue(solicitud.endpoint.contains("/responses"));
            assertEquals("gpt-4o", solicitud.modeloUsado);
            assertNull(solicitud.advertencia);
        }

        @Test
        @DisplayName("Construye solicitud válida para Claude")
        void construyeSolicitudClaude() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest("Claude", "claude-3-sonnet", "https://api.anthropic.com/v1", "sk-ant-test");

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
            assertTrue(solicitud.endpoint.contains("/messages"));
            assertEquals("claude-3-sonnet", solicitud.modeloUsado);
            assertNotNull(solicitud.request.header("x-api-key"));
            assertNotNull(solicitud.request.header("anthropic-version"));
        }

        @Test
        @DisplayName("Construye solicitud válida para Ollama")
        void construyeSolicitudOllama() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest("Ollama", "llama3", "http://localhost:11434/api", null);

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
            assertTrue(solicitud.endpoint.contains("/api/chat"));
            assertEquals("llama3", solicitud.modeloUsado);
        }

        @Test
        @DisplayName("Usa OpenAI como proveedor por defecto cuando es null")
        void usaOpenAIPorDefecto() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest(null, "gpt-4o", "https://api.openai.com/v1", "sk-test");

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
        }

        @Test
        @DisplayName("Construye solicitud válida para proveedor Custom")
        void construyeSolicitudCustom() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest("Custom", "custom-model", "https://custom.api/v1", "custom-key");

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
            assertEquals("custom-model", solicitud.modeloUsado);
        }
    }

    @Nested
    @DisplayName("SolicitudPreparada")
    class SolicitudPreparadaTests {

        @Test
        @DisplayName("Contiene todos los campos esperados")
        void contieneTodosLosCampos() throws Exception {
            ConfiguracionAPI config = crearConfiguracionTest("OpenAI", "gpt-4o", "https://api.openai.com/v1", "sk-test");

            ConstructorSolicitudesProveedor.SolicitudPreparada solicitud =
                ConstructorSolicitudesProveedor.construirSolicitud(config, "Test prompt", clienteHttp);

            assertNotNull(solicitud.request);
            assertNotNull(solicitud.endpoint);
            assertNotNull(solicitud.modeloUsado);
        }
    }

    private ConfiguracionAPI crearConfiguracionTest(String proveedor, String modelo, String url, String apiKey) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI(proveedor);
        config.establecerModelo(modelo);
        config.establecerUrlApi(url);
        config.establecerClaveApi(apiKey);
        config.establecerMaxTokensParaProveedor(proveedor != null ? proveedor : "OpenAI", 4096);
        return config;
    }
}
