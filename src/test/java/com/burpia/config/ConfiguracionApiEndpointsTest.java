package com.burpia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ConfiguracionAPI Endpoint Builder Tests")
class ConfiguracionApiEndpointsTest {

    @Test
    @DisplayName("Construye endpoint OpenAI responses y Z.ai/minimax chat completions")
    void testEndpointChatCompletions() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            ConfiguracionAPI.construirUrlApiProveedor("OpenAI", "https://api.openai.com/v1", "gpt-4o")
        );

        assertEquals(
            "https://api.z.ai/api/paas/v4/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("Z.ai", "https://api.z.ai/api/paas/v4", "glm-5")
        );

        assertEquals(
            "https://api.minimax.io/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("minimax", "https://api.minimax.io/v1", "minimax-m2.5")
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
    @DisplayName("Construye endpoint Custom OpenAI-compatible")
    void testEndpointCustom() {
        assertEquals(
            "https://example.local/v1/chat/completions",
            ConfiguracionAPI.construirUrlApiProveedor("-- Custom --", "https://example.local/v1", "my-model")
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
}
