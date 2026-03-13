package com.burpia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConfigValidator Tests")
class ConfigValidatorTest {

    @Test
    @DisplayName("OpenAI acepta claves largas mientras mantengan el prefijo")
    void testValidarApiKeyOpenAiAceptaClaveLargaConPrefijo() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey(
            "sk-" + "a".repeat(120),
            "OpenAI"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:18");
    }

    @Test
    @DisplayName("OpenAI rechaza claves sin contenido despues del prefijo")
    void testValidarApiKeyOpenAiRechazaPrefijoIncompleto() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey("sk-", "OpenAI");

        assertFalse(resultado.esValido(), "assertFalse failed at ConfigValidatorTest.java:26");
        assertEquals("apiKey", resultado.obtenerCampo(), "assertEquals failed at ConfigValidatorTest.java:27");
    }

    @Test
    @DisplayName("Claude acepta claves largas mientras mantengan el prefijo")
    void testValidarApiKeyClaudeAceptaClaveLargaConPrefijo() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey(
            "sk-ant-" + "b".repeat(160),
            "Claude"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:37");
    }

    @Test
    @DisplayName("Gemini acepta claves largas mientras mantengan el prefijo AIza")
    void testValidarApiKeyGeminiAceptaClaveLargaConPrefijo() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey(
            "AIza" + "c".repeat(80),
            "Gemini"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:47");
    }

    @Test
    @DisplayName("Gemini rechaza claves sin el prefijo esperado")
    void testValidarApiKeyGeminiRechazaClaveSinPrefijo() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey(
            "clave-gemini-sin-prefijo",
            "Gemini"
        );

        assertFalse(resultado.esValido(), "assertFalse failed at ConfigValidatorTest.java:57");
        assertEquals("apiKey", resultado.obtenerCampo(), "assertEquals failed at ConfigValidatorTest.java:58");
    }

    @Test
    @DisplayName("Z.ai ya no exige longitud minima fija")
    void testValidarApiKeyZaiAceptaClaveCortaSinLongitudMinima() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey("k", "Z.ai");

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:66");
    }

    @Test
    @DisplayName("OpenAI rechaza claves con espacios")
    void testValidarApiKeyOpenAiRechazaClaveConEspacios() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarApiKey("sk-abc def", "OpenAI");

        assertFalse(resultado.esValido(), "assertFalse failed at ConfigValidatorTest.java:74");
    }

    @Test
    @DisplayName("Ollama permite URL HTTP sin HTTPS")
    void testValidarUrlApiOllamaPermiteHttp() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarUrlApi(
            "http://192.168.1.50:11434",
            "Ollama"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:82");
    }

    @Test
    @DisplayName("Custom 01 permite URL HTTP sin HTTPS")
    void testValidarUrlApiCustomPermiteHttp() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarUrlApi(
            "http://custom-gateway.local:8080/v1",
            ProveedorAI.PROVEEDOR_CUSTOM_01
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:92");
    }

    @Test
    @DisplayName("OpenAI mantiene requisito de HTTPS fuera de localhost")
    void testValidarUrlApiOpenAiRequiereHttpsFueraDeLocalhost() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarUrlApi(
            "http://api.openai.internal/v1",
            "OpenAI"
        );

        assertFalse(resultado.esValido(), "assertFalse failed at ConfigValidatorTest.java:102");
        assertEquals("url", resultado.obtenerCampo(), "assertEquals failed at ConfigValidatorTest.java:103");
    }

    @Test
    @DisplayName("Ruta de agente acepta tilde con argumentos")
    void testValidarRutaBinarioAgenteAceptaTildeConArgumentos() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarRutaBinarioAgente(
            "~/.local/bin/claude --dangerously-skip-permissions",
            "CLAUDE_CODE"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:84");
    }

    @Test
    @DisplayName("Ruta de agente acepta ejecutable entre comillas con tilde")
    void testValidarRutaBinarioAgenteAceptaEjecutableEntreComillasConTilde() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarRutaBinarioAgente(
            "\"~/bin/claude\" --dangerously-skip-permissions",
            "CLAUDE_CODE"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:94");
    }

    @Test
    @DisplayName("Ruta de agente acepta comando resoluble por PATH")
    void testValidarRutaBinarioAgenteAceptaComandoResolublePorPath() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarRutaBinarioAgente(
            "claude --dangerously-skip-permissions",
            "CLAUDE_CODE"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:104");
    }

    @Test
    @DisplayName("Ruta de agente rechaza traversal en ejecutable")
    void testValidarRutaBinarioAgenteRechazaTraversalEnEjecutable() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarRutaBinarioAgente(
            "../bin/claude --dangerously-skip-permissions",
            "CLAUDE_CODE"
        );

        assertFalse(resultado.esValido(), "assertFalse failed at ConfigValidatorTest.java:114");
        assertEquals("rutaBinario", resultado.obtenerCampo(), "assertEquals failed at ConfigValidatorTest.java:115");
    }

    @Test
    @DisplayName("Ruta de agente permite traversal solo en argumentos")
    void testValidarRutaBinarioAgentePermiteTraversalSoloEnArgumentos() {
        ConfigValidator.ValidationResult resultado = ConfigValidator.validarRutaBinarioAgente(
            "claude --config ../perfil/test.json",
            "CLAUDE_CODE"
        );

        assertTrue(resultado.esValido(), "assertTrue failed at ConfigValidatorTest.java:125");
    }
}
