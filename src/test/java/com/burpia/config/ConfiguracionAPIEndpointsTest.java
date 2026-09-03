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
    @DisplayName("Construye endpoint Ollama Cloud (mismo /api/chat que Ollama local)")
    void testEndpointOllamaCloud() {
        assertEquals(
            "https://ollama.com/api/chat",
            ConfiguracionAPI.construirUrlApiProveedor("Ollama Cloud", "https://ollama.com", "llama3.2")
        );
    }

    @Test
    @DisplayName("Construye endpoint DeepSeek chat completions")
    void testEndpointDeepSeek() {
        assertEquals(
            "https://api.deepseek.com/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("DeepSeek", "https://api.deepseek.com", "deepseek-v4-flash")
        );
    }

    @Test
    @DisplayName("Construye endpoint xAI chat completions")
    void testEndpointXAI() {
        assertEquals(
            "https://api.x.ai/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("xAI", "https://api.x.ai/v1", "grok-4.3")
        );
    }

    @Test
    @DisplayName("Construye endpoint Sakana chat completions")
    void testEndpointSakana() {
        assertEquals(
            "https://api.sakana.ai/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Sakana", "https://api.sakana.ai/v1", "fugu")
        );
    }

    // --- Custom providers: URL verbatim (sin manipulación del plugin) ---

    @Test
    @DisplayName("Custom 01/02/03: URL verbatim — usuario escribe endpoint completo")
    void testCustomVerbatimEndpointCompleto() {
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_01,
                "https://example.local/v1/chat/completions", "my-model")
        );
        assertEquals(
            "http://127.0.0.1:1234/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_02,
                "http://127.0.0.1:1234/v1/chat/completions", "lm-model")
        );
        assertEquals(
            "https://api.openai.com/v1/responses",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_03,
                "https://api.openai.com/v1/responses", "gpt-4o")
        );
    }

    @Test
    @DisplayName("Custom: URL verbatim sin path — NO se agrega /v1/chat/completions")
    void testCustomVerbatimSoloHost() {
        assertEquals(
            "http://127.0.0.1:1234",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_01, "http://127.0.0.1:1234", "lm-model")
        );
    }

    @Test
    @DisplayName("Custom: URL verbatim con path raro — NO se modifica")
    void testCustomVerbatimPathRaro() {
        // Si el usuario tipea un path raro, el plugin lo respeta tal cual.
        // El preview en UI le permite verificarlo antes de guardar.
        assertEquals(
            "http://localhost:1234/api/v1/chat",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_01, "http://localhost:1234/api/v1/chat", "any")
        );
    }

    @Test
    @DisplayName("Custom: trim de espacios en URL")
    void testCustomVerbatimTrim() {
        assertEquals(
            "https://api.foo.com/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor(
                ProveedorAI.PROVEEDOR_CUSTOM_01, "  https://api.foo.com/v1/chat/completions  ", "any")
        );
    }

    @Test
    @DisplayName("Proveedor desconocido cae al default verbatim como Custom")
    void testEndpointProveedorDesconocido() {
        // Proveedor desconocido es tratado como Custom: URL verbatim.
        assertEquals(
            "https://unknown.api/v1",
            ConfiguracionAPI.construirUrlApiProveedor("UnknownProvider", "https://unknown.api/v1", "model-x")
        );
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
        // El default se lee del catálogo canónico para no acoplar el test a una
        // versión puntual del modelo por defecto de Gemini.
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/"
                + ProveedorAI.obtenerModeloPorDefecto("Gemini") + ":generateContent",
            ConfiguracionAPI.construirUrlApiProveedor("Gemini", "https://generativelanguage.googleapis.com/v1beta", null)
        , "assertEquals failed at ConfiguracionAPIEndpointsTest.java:125");
    }

    @Test
    @DisplayName("Construye endpoint con proveedor null cae al default verbatim")
    void testEndpointConProveedorNull() {
        // Provider null normaliza a "" → no matchea ningún case → default (verbatim).
        assertEquals(
            "https://api.example.com/v1",
            ConfiguracionAPI.construirUrlApiProveedor(null, "https://api.example.com/v1", "model-x")
        );
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
