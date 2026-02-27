package com.burpia.util;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;





@DisplayName("ConstructorSolicitudesProveedor Tests")
class ConstructorSolicitudesProveedorTest {

    @BeforeEach
    @AfterEach
    void limpiarCacheGemini() throws Exception {
        Field cacheField = ConstructorSolicitudesProveedor.class.getDeclaredField("CACHE_GEMINI");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> cache = (Map<String, ?>) cacheField.get(null);
        cache.clear();
    }

    @Test
    @DisplayName("Cache de modelos Gemini no colisiona cuando dos API keys tienen mismo hashCode")
    void testCacheGeminiSinColisionHashCodeApiKey() throws Exception {
        assertEquals("Aa".hashCode(), "BB".hashCode());

        String respuestaModelosA = "{"
            + "\"models\":[{"
            + "\"name\":\"models/gemini-1.5-pro-002\","
            + "\"supportedGenerationMethods\":[\"generateContent\"]"
            + "}]}";
        String respuestaModelosB = "{"
            + "\"models\":[{"
            + "\"name\":\"models/gemini-1.5-flash-002\","
            + "\"supportedGenerationMethods\":[\"generateContent\"]"
            + "}]}";

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(respuestaModelosA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(respuestaModelosB));
        server.start();

        try {
            OkHttpClient client = new OkHttpClient();
            String urlBase = server.url("/v1beta").toString();

            List<String> modelosA = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "Aa", client);
            List<String> modelosB = ConstructorSolicitudesProveedor.listarModelosGemini(urlBase, "BB", client);

            assertEquals(List.of("gemini-1.5-pro-002"), modelosA);
            assertEquals(List.of("gemini-1.5-flash-002"), modelosB);
            assertEquals(2, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }
}
