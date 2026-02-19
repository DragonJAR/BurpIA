package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ParserRespuestasAI Tests")
class ParserRespuestasAITest {

    @Test
    @DisplayName("Extrae contenido formato OpenAI/Z.ai")
    void testExtraerOpenAi() {
        String json = "{\"choices\":[{\"message\":{\"content\":\"OK desde OpenAI\"}}]}";
        assertEquals("OK desde OpenAI", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
        assertEquals("OK desde OpenAI", ParserRespuestasAI.extraerContenido(json, "Z.ai"));
    }

    @Test
    @DisplayName("Extrae reasoning_content cuando content viene vacío")
    void testExtraerReasoningContent() {
        String json = "{\"choices\":[{\"message\":{\"content\":\"\",\"reasoning_content\":\"Analisis interno\"}}]}";
        assertEquals("Analisis interno", ParserRespuestasAI.extraerContenido(json, "Z.ai"));
    }

    @Test
    @DisplayName("Extrae contenido formato Claude")
    void testExtraerClaude() {
        String json = "{\"content\":[{\"type\":\"text\",\"text\":\"Hola Claude\"}]}";
        assertEquals("Hola Claude", ParserRespuestasAI.extraerContenido(json, "Claude"));
    }

    @Test
    @DisplayName("Extrae contenido formato Gemini")
    void testExtraerGemini() {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK Gemini\"}]}}]}";
        assertEquals("OK Gemini", ParserRespuestasAI.extraerContenido(json, "Gemini"));
    }

    @Test
    @DisplayName("Extrae contenido formato Ollama")
    void testExtraerOllama() {
        String json = "{\"message\":{\"content\":\"OK Ollama\"}}";
        assertEquals("OK Ollama", ParserRespuestasAI.extraerContenido(json, "Ollama"));
    }

    @Test
    @DisplayName("Valida respuestas esperadas de prueba")
    void testValidarRespuestaPrueba() {
        assertTrue(ParserRespuestasAI.validarRespuestaPrueba("OK"));
        assertTrue(ParserRespuestasAI.validarRespuestaPrueba("Hola"));
        assertTrue(ParserRespuestasAI.validarRespuestaPrueba("hola mundo"));
        assertFalse(ParserRespuestasAI.validarRespuestaPrueba("respuesta sin palabra esperada"));
    }

    @Test
    @DisplayName("Valida conexión con contenido no vacío")
    void testValidarRespuestaConexion() {
        assertTrue(ParserRespuestasAI.validarRespuestaConexion("contenido cualquiera"));
        assertFalse(ParserRespuestasAI.validarRespuestaConexion(""));
    }
}
