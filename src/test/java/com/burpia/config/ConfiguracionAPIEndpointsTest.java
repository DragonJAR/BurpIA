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
        );
    }

    @Test
    @DisplayName("Construye endpoint Z.ai chat completions")
    void testEndpointZai() {
        assertEquals(
            "https://api.z.ai/api/paas/v4/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Z.ai", "https://api.z.ai/api/paas/v4", "glm-5")
        );
    }

    @Test
    @DisplayName("Construye endpoint minimax chat completions")
    void testEndpointMinimax() {
        assertEquals(
            "https://api.minimax.io/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("minimax", "https://api.minimax.io/v1", "minimax-m2.5")
        );
    }

    @Test
    @DisplayName("Construye endpoint Moonshot chat completions")
    void testEndpointMoonshot() {
        assertEquals(
            "https://api.moonshot.cn/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Moonshot (Kimi)", "https://api.moonshot.cn/v1", "moonshot-v1-8k")
        );
    }

    @Test
    @DisplayName("Construye endpoint Claude")
    void testEndpointClaude() {
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            ConfiguracionAPI.construirUrlApiProveedor("Claude", "https://api.anthropic.com/v1", "claude-sonnet-4-6")
        );
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
        );
    }

    @Test
    @DisplayName("Construye endpoint Ollama")
    void testEndpointOllama() {
        assertEquals(
            "http://localhost:11434/api/chat",
            ConfiguracionAPI.construirUrlApiProveedor("Ollama", "http://localhost:11434", "llama3.2")
        );
    }

    @Test
    @DisplayName("Construye endpoint Custom 01/02/03 OpenAI-compatible")
    void testEndpointCustom() {
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01, "https://example.local/v1", "my-model")
        );
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_02, "https://example.local/v1", "my-model")
        );
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(ProveedorAI.PROVEEDOR_CUSTOM_03, "https://example.local/v1", "my-model")
        );
    }

    @Test
    @DisplayName("Construye endpoint proveedor desconocido usa chat completions por defecto")
    void testEndpointProveedorDesconocido() {
        assertEquals(
            "https://unknown.api/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("UnknownProvider", "https://unknown.api/v1", "model-x")
        );
    }

    @Test
    @DisplayName("Construye endpoint con URL base con trailing slash")
    void testEndpointConUrlBaseConTrailingSlash() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            ConfiguracionAPI.construirUrlApiProveedor("OpenAI", "https://api.openai.com/v1/", "gpt-4o")
        );
    }

    @Test
    @DisplayName("Construye endpoint con URL base null usa string vacio")
    void testEndpointConUrlBaseNull() {
        String resultado = ConfiguracionAPI.construirUrlApiProveedor("OpenAI", null, "gpt-4o");
        assertNotNull(resultado);
        assertEquals("/responses", resultado);
    }

    @Test
    @DisplayName("Construye endpoint con modelo null usa default")
    void testEndpointConModeloNull() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent",
            ConfiguracionAPI.construirUrlApiProveedor("Gemini", "https://generativelanguage.googleapis.com/v1beta", null)
        );
    }

    @Test
    @DisplayName("Construye endpoint con proveedor null usa chat completions")
    void testEndpointConProveedorNull() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(null, "https://api.example.com/v1", "model-x")
        );
    }

    @Test
    @DisplayName("Extrae URL base desde endpoint completo")
    void testExtraerUrlBase() {
        assertEquals(
            "https://api.z.ai/api/paas/v4",
            ConfiguracionAPI.extraerUrlBase("https://api.z.ai/api/paas/v4/chat/completions")
        );
        assertEquals(
            "https://api.openai.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.openai.com/v1/responses")
        );
        assertEquals(
            "https://api.anthropic.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.anthropic.com/v1/messages")
        );
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            ConfiguracionAPI.extraerUrlBase(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent"
            )
        );
        assertEquals(
            "http://localhost:11434",
            ConfiguracionAPI.extraerUrlBase("http://localhost:11434/api/chat")
        );
    }

    @Test
    @DisplayName("Extrae URL base desde endpoint con trailing slash")
    void testExtraerUrlBaseConTrailingSlash() {
        assertEquals(
            "https://api.openai.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.openai.com/v1/responses/")
        );
        assertEquals(
            "https://api.z.ai/api/paas/v4",
            ConfiguracionAPI.extraerUrlBase("https://api.z.ai/api/paas/v4/chat/completions/")
        );
    }

    @Test
    @DisplayName("Extrae URL base desde URL sin sufijo conocido")
    void testExtraerUrlBaseSinSufijo() {
        assertEquals(
            "https://api.custom.com/v1",
            ConfiguracionAPI.extraerUrlBase("https://api.custom.com/v1")
        );
    }

    @Test
    @DisplayName("Extrae URL base con null retorna string vacio")
    void testExtraerUrlBaseNull() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase(null));
    }

    @Test
    @DisplayName("Extrae URL base con string vacio retorna vacio")
    void testExtraerUrlBaseVacio() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase(""));
    }

    @Test
    @DisplayName("Extrae URL base con solo espacios retorna vacio")
    void testExtraerUrlBaseSoloEspacios() {
        assertEquals("", ConfiguracionAPI.extraerUrlBase("   "));
    }
}
