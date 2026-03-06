package com.burpia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ConfiguracionAPI Endpoint Builder Tests")
class ConfiguracionAPIEndpointsTest {

    @Test
    @DisplayName("Construye endpoint OpenAI responses")
    void testEndpointOpenAI() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            ConfiguracionAPI.construirUrlApiProveedor("OpenAI", "https://api.openai.com/v1", "gpt-4o")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:15");
    }

    @Test
    @DisplayName("Construye endpoint Z.ai chat completions")
    void testEndpointZai() {
        assertEquals(
            "https://api.z.ai/api/paas/v4/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Z.ai", "https://api.z.ai/api/paas/v4", "glm-5")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:24");
    }

    @Test
    @DisplayName("Construye endpoint minimax chat completions")
    void testEndpointMinimax() {
        assertEquals(
            "https://api.minimax.io/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("minimax", "https://api.minimax.io/v1", "minimax-m2.5")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:33");
    }

    @Test
    @DisplayName("Construye endpoint Moonshot chat completions")
    void testEndpointMoonshot() {
        assertEquals(
            "https://api.moonshot.cn/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Moonshot (Kimi)", "https://api.moonshot.cn/v1", "moonshot-v1-8k")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:42");
    }

    @Test
    @DisplayName("Construye endpoint Claude")
    void testEndpointClaude() {
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            ConfiguracionAPI.construirUrlApiProveedor("Claude", "https://api.anthropic.com/v1", "claude-sonnet-4-6")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:51");
    }

    @Test
    @DisplayName("Construye endpoint Gemini con modelo")
    void testEndpointGemini() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent",
            ConfiguracionAPI.construirUrlApiProveedor(
                "Gemini",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro-002"
            )
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:60");
    }

    @Test
    @DisplayName("Construye endpoint Ollama")
    void testEndpointOllama() {
        assertEquals(
            "http://localhost:11434/api/chat",
            ConfiguracionAPI.construirUrlApiProveedor("Ollama", "http://localhost:11434", "llama3.2")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:73");
    }

    @Test
    @DisplayName("Construye endpoint Custom 01/02/03 OpenAI-compatible")
    void testEndpointCustom() {
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01, "https://example.local/v1", "my-model")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:82");
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_02, "https://example.local/v1", "my-model")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:86");
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_03, "https://example.local/v1", "my-model")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:90");
    }

    @Test
    @DisplayName("Construye endpoint proveedor desconocido usa chat completions por defecto")
    void testEndpointProveedorDesconocido() {
        assertEquals(
            "https://unknown.api/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("UnknownProvider", "https://unknown.api/v1", "model-x")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:99");
    }

    @Test
    @DisplayName("Construye endpoint con URL base con trailing slash")
    void testEndpointConUrlBaseConTrailingSlash() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            ConfiguracionAPI.construirUrlApiProveedor("OpenAI", "https://api.openai.com/v1/", "gpt-4o")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:108");
    }

    @Test
    @DisplayName("Construye endpoint con URL base null usa string vacio")
    void testEndpointConUrlBaseNull() {
        String resultado = ConfiguracionAPI.construirUrlApiProveedor("OpenAI", null, "gpt-4o");
        assertNotNull(resultado, "assertNotNull failed at ConfiguracionAPIEndpointsTest.java:118");
        assertEquals("/responses", resultado, "assertEquals failed at ConfiguracionAPIEndpointsTest.java:119");
    }

    @Test
    @DisplayName("Construye endpoint con modelo null usa default")
    void testEndpointConModeloNull() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent",
            ConfiguracionAPI.construirUrlApiProveedor("Gemini", "https://generativelanguage.googleapis.com/v1beta", null)
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:125");
    }

    @Test
    @DisplayName("Construye endpoint con proveedor null usa chat completions")
    void testEndpointConProveedorNull() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(null, "https://api.example.com/v1", "model-x")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:134");
    }

    @Test
    @DisplayName("Extrae URL base desde endpoint completo")
    void testExtraerUrlBase() {
        assertEquals(
            "https://api.z.ai/api/paas/v4",
            ConfiguracionAPI.extraerUrlBase("https://api.z.ai/api/paas/v4/chat/completions")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:143");
        assertEquals(
            "https://api.openai.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.openai.com/v1/responses")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:147");
        assertEquals(
            "https://api.anthropic.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.anthropic.com/v1/messages")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:151");
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            ConfiguracionAPI.extraerUrlBase(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent"
            )
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:155");
        assertEquals(
            "http://localhost:11434",
            ConfiguracionAPI.extraerUrlBase("http://localhost:11434/api/chat")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:161");
    }

    @Test
    @DisplayName("Extrae URL base desde endpoint con trailing slash")
    void testExtraerUrlBaseConTrailingSlash() {
        assertEquals(
            "https://api.openai.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.openai.com/v1/responses/")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:170");
        assertEquals(
            "https://api.z.ai/api/paas/v4",
            ConfiguracionAPI.extraerUrlBase("https://api.z.ai/api/paas/v4/chat/completions/")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:174");
    }

    @Test
    @DisplayName("Extrae URL base desde URL sin sufijo conocido")
    void testExtraerUrlBaseSinSufijo() {
        assertEquals(
            "https://api.custom.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.custom.com/v1")
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:183");
    }

    @Test
    @DisplayName("Extrae URL base con null retorna string vacio")
    void testExtraerUrlBaseNull() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase(null), "assertEquals failed at ConfiguracionAPIEndpointsTest.java:192");
    }

    @Test
    @DisplayName("Extrae URL base con string vacio retorna vacio")
    void testExtraerUrlBaseVacio() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase(""), "assertEquals failed at ConfiguracionAPIEndpointsTest.java:198");
    }

    @Test
    @DisplayName("Extrae URL base con solo espacios retorna vacio")
    void testExtraerUrlBaseSoloEspacios() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase("   "), "assertEquals failed at ConfiguracionAPIEndpointsTest.java:204");
    }
}
