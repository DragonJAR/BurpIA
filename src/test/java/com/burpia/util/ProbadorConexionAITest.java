package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProbadorConexionAI Endpoint Tests")
class ProbadorConexionAITest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Z.ai prueba /chat/completions")
    void testEndpointZai() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("Z.ai", "glm-5");
        config.establecerUrlApi(server.url("/api/paas/v4").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/api/paas/v4/chat/completions", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        assertTrue(resultado.mensaje.contains("Endpoint probado: " + server.url("/api/paas/v4/chat/completions")));
    }

    @Test
    @DisplayName("OpenAI prueba /responses")
    void testEndpointOpenAi() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"output_text\":\"OK\"}"
        ));

        ConfiguracionAPI config = baseConfig("OpenAI", "gpt-4o");
        config.establecerUrlApi(server.url("/v1").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1/responses", request.getPath());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("minimax prueba /chat/completions")
    void testEndpointMinimax() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("minimax", "minimax-m2.5");
        config.establecerUrlApi(server.url("/v1").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1/chat/completions", request.getPath());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Custom prueba /chat/completions")
    void testEndpointCustom() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("-- Custom --", "custom-model");
        config.establecerUrlApi(server.url("/v1").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1/chat/completions", request.getPath());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Custom omite Authorization cuando API key esta vacia")
    void testEndpointCustomSinApiKey() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("-- Custom --", "custom-model");
        config.establecerClaveApi("");
        config.establecerUrlApi(server.url("/v1").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1/chat/completions", request.getPath());
        assertNull(request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Claude prueba /messages con headers Anthropic")
    void testEndpointClaude() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}]}"
        ));

        ConfiguracionAPI config = baseConfig("Claude", "claude-sonnet-4-6");
        config.establecerUrlApi(server.url("/v1").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1/messages", request.getPath());
        assertEquals("test-key", request.getHeader("x-api-key"));
        assertEquals("2023-06-01", request.getHeader("anthropic-version"));
    }

    @Test
    @DisplayName("Gemini prueba /models/{model}:generateContent")
    void testEndpointGemini() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"models\":[{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":[\"generateContent\"]}]}"
        ));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}"
        ));

        ConfiguracionAPI config = baseConfig("Gemini", "gemini-1.5-pro-002");
        config.establecerUrlApi(server.url("/v1beta").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest requestList = server.takeRequest();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/v1beta/models?key=test-key", requestList.getPath());
        assertEquals("/v1beta/models/gemini-1.5-pro-002:generateContent", request.getPath());
        assertEquals("test-key", request.getHeader("x-goog-api-key"));
    }

    @Test
    @DisplayName("Ollama prueba /api/chat sin Authorization")
    void testEndpointOllama() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"message\":{\"content\":\"OK\"}}"
        ));

        ConfiguracionAPI config = baseConfig("Ollama", "llama3.2");
        config.establecerClaveApi("");
        config.establecerUrlApi(server.url("").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        RecordedRequest request = server.takeRequest();

        assertTrue(resultado.exito);
        assertEquals("/api/chat", request.getPath());
        assertNull(request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Z.ai acepta reasoning_content cuando content viene vacío")
    void testConexionValidaConReasoningContent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"choices\":[{\"message\":{\"content\":\"\",\"reasoning_content\":\"Analisis OK\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("Z.ai", "glm-5");
        config.establecerUrlApi(server.url("/api/paas/v4").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();
        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains("Analisis OK"));
    }

    @Test
    @DisplayName("Gemini falla con URL base invalida sin lanzar excepcion")
    void testGeminiUrlInvalida() {
        ConfiguracionAPI config = baseConfig("Gemini", "gemini-1.5-pro-002");
        config.establecerUrlApi("ht!tp://url-invalida");

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("URL base de Gemini"));
    }

    @Test
    @DisplayName("Gemini falla de forma controlada cuando /models responde JSON invalido")
    void testGeminiModelosJsonInvalido() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{json invalido"));

        ConfiguracionAPI config = baseConfig("Gemini", "gemini-1.5-pro-002");
        config.establecerUrlApi(server.url("/v1beta").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("Gemini no reportó modelos compatibles"));
    }

    @Test
    @DisplayName("Gemini ignora modelos con supportedGenerationMethods en tipo inválido")
    void testGeminiModelosConMetodosInvalidos() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"models\":[{\"name\":\"models/gemini-1.5-pro-002\",\"supportedGenerationMethods\":{\"name\":\"generateContent\"}}]}"
        ));

        ConfiguracionAPI config = baseConfig("Gemini", "gemini-1.5-pro-002");
        config.establecerUrlApi(server.url("/v1beta").toString());

        ProbadorConexionAI.ResultadoPrueba resultado = new ProbadorConexionAI(config).probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("Gemini no reportó modelos compatibles"));
    }

    private ConfiguracionAPI baseConfig(String proveedor, String modelo) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI(proveedor);
        config.establecerModelo(modelo);
        config.establecerClaveApi("test-key");
        config.establecerMaximoConcurrente(1);
        config.establecerRetrasoSegundos(0);
        return config;
    }
}
