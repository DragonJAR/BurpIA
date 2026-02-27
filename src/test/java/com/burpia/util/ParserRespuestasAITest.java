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
        assertEquals("OK desde OpenAI", ParserRespuestasAI.extraerContenido(json, "-- Custom --"));
    }

    @Test
    @DisplayName("Extrae contenido formato OpenAI Responses API")
    void testExtraerOpenAiResponsesApi() {
        String json = "{\"output_text\":\"OK desde Responses\"}";
        assertEquals("OK desde Responses", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
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
    @DisplayName("Claude extrae texto aunque falte campo type")
    void testExtraerClaudeSinType() {
        String json = "{\"content\":[{\"text\":\"Hola Claude sin type\"}]}";
        assertEquals("Hola Claude sin type", ParserRespuestasAI.extraerContenido(json, "Claude"));
    }

    @Test
    @DisplayName("Extrae contenido formato Gemini")
    void testExtraerGemini() {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK Gemini\"}]}}]}";
        assertEquals("OK Gemini", ParserRespuestasAI.extraerContenido(json, "Gemini"));
    }

    @Test
    @DisplayName("OpenAI ignora estructuras inválidas y usa fallback válido")
    void testOpenAiEstructuraMixta() {
        String json = "{\"choices\":[{\"message\":{\"content\":{}}},{\"text\":\"fallback texto\"}]}";
        assertEquals("fallback texto", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
    }

    @Test
    @DisplayName("OpenAI extrae texto cuando message.content llega como array estructurado")
    void testOpenAiContentArray() {
        String json = "{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"OK desde array\"}]}}]}";
        assertEquals("OK desde array", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
    }

    @Test
    @DisplayName("OpenAI concatena todas las partes de message.content array")
    void testOpenAiContentArrayMultiparte() {
        String json = "{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"hallazgos\\\":\"},{\"type\":\"text\",\"text\":\"[]}\"}]}}]}";
        assertEquals("{\"hallazgos\":[]}", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
    }

    @Test
    @DisplayName("Responses API concatena contenido multiparte de output.content")
    void testResponsesApiConcatenaOutputMultiparte() {
        String json = "{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"ok\\\":\"},{\"type\":\"output_text\",\"text\":\"true}\"}]}]}";
        assertEquals("{\"ok\":true}", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
    }

    @Test
    @DisplayName("Gemini ignora candidatos inválidos y toma primer texto util")
    void testGeminiCandidatosInvalidos() {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":{}}]}},{\"content\":{\"parts\":[{\"text\":\"OK Gemini 2\"}]}}]}";
        assertEquals("OK Gemini 2", ParserRespuestasAI.extraerContenido(json, "Gemini"));
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

    @Test
    @DisplayName("Elimina bloques <think> antes de retornar contenido")
    void testEliminaBloquesThink() {
        String json = "{\"choices\":[{\"message\":{\"content\":\"<think>paso interno</think>{\\\"hallazgos\\\":[]}\"}}]}";
        assertEquals("{\"hallazgos\":[]}", ParserRespuestasAI.extraerContenido(json, "OpenAI"));
    }

    @Test
    @DisplayName("Elimina múltiples bloques de pensamiento y mantiene JSON final")
    void testEliminaMultiplesBloquesThink() {
        String json = "{\"choices\":[{\"message\":{\"content\":\"<think>uno</think> <thinking>dos</thinking> {\\\"ok\\\":true}\"}}]}";
        assertEquals("{\"ok\":true}", ParserRespuestasAI.extraerContenido(json, "Z.ai"));
    }

    @Test
    @DisplayName("Extractor no estricto acepta alias en ingles")
    void testExtraerCampoNoEstrictoAliasIngles() {
        String contenido = "{\"title\":\"SQLi\",\"description\":\"detalle\",\"severity\":\"High\",\"confidence\":\"Low\",\"evidence\":\"id=1\"}";
        assertEquals("SQLi", ParserRespuestasAI.extraerCampoNoEstricto("title", contenido));
        assertEquals("detalle", ParserRespuestasAI.extraerCampoNoEstricto("description", contenido));
        assertEquals("High", ParserRespuestasAI.extraerCampoNoEstricto("severity", contenido));
        assertEquals("Low", ParserRespuestasAI.extraerCampoNoEstricto("confidence", contenido));
        assertEquals("id=1", ParserRespuestasAI.extraerCampoNoEstricto("evidence", contenido));
    }
}
