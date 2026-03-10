package com.burpia.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ContextExceededDetector.
 * 
 * <p>Verifica la detección correcta de errores de contexto excedido
 * para todos los proveedores soportados.</p>
 */
@DisplayName("ContextExceededDetector Tests")
class ContextExceededDetectorTest {

    private ContextExceededDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ContextExceededDetector();
    }

    @Nested
    @DisplayName("Detección por proveedor")
    class DeteccionPorProveedorTests {

        @Test
        @DisplayName("detecta error de OpenAI context_length_exceeded")
        void detectaErrorOpenAI() {
            String cuerpoError = "{\"error\": {\"message\": \"This model's maximum context length is 128000 tokens. However, your messages resulted in 150000 tokens.\", \"type\": \"invalid_request_error\", \"code\": \"context_length_exceeded\"}}";
            
            boolean resultado = detector.esErrorContextoExcedido("OpenAI", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de contexto de OpenAI");
        }

        @Test
        @DisplayName("detecta error de Claude model_context_window_exceeded")
        void detectaErrorClaude() {
            String cuerpoError = "{\"error\": {\"type\": \"error\", \"message\": \"prompt is too long: 200001 tokens > 200000 maximum\"}}";
            
            boolean resultado = detector.esErrorContextoExcedido("Claude", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de contexto de Claude");
        }

        @Test
        @DisplayName("detecta error de Gemini RESOURCE_EXHAUSTED")
        void detectaErrorGemini() {
            String cuerpoError = "{\"error\": {\"code\": 429, \"message\": \"Resource exhausted. Token count exceeds limit.\", \"status\": \"RESOURCE_EXHAUSTED\"}}";
            
            boolean resultado = detector.esErrorContextoExcedido("Gemini", 429, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de contexto de Gemini");
        }

        @Test
        @DisplayName("detecta error de Z.ai Input token length too long")
        void detectaErrorZai() {
            String cuerpoError = "{\"error\": {\"message\": \"Input token length too long. Maximum: 128000, Actual: 150000\"}}";
            
            boolean resultado = detector.esErrorContextoExcedido("Z.ai", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de contexto de Z.ai");
        }

        @Test
        @DisplayName("detecta error de Ollama")
        void detectaErrorOllama() {
            String cuerpoError = "Error: prompt too long: 8192 tokens exceeds limit of 4096";
            
            boolean resultado = detector.esErrorContextoExcedido("Ollama", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de contexto de Ollama");
        }
    }

    @Nested
    @DisplayName("Normalización de proveedor")
    class NormalizacionProveedorTests {

        @Test
        @DisplayName("detecta error con proveedor openai normalizado")
        void detectaConOpenaiNormalizado() {
            String cuerpoError = "context_length_exceeded";
            boolean resultado = detector.esErrorContextoExcedido("openai", 400, cuerpoError);
            assertTrue(resultado, "Debe detectar error para openai");
        }

        @Test
        @DisplayName("detecta error con proveedor OPENAI mayúsculas")
        void detectaConOpenaiMayusculas() {
            String cuerpoError = "context_length_exceeded";
            boolean resultado = detector.esErrorContextoExcedido("OPENAI", 400, cuerpoError);
            assertTrue(resultado, "Debe detectar error para OPENAI");
        }

        @Test
        @DisplayName("detecta error con proveedor anthropic normalizado a Claude")
        void detectaConAnthropicNormalizado() {
            String cuerpoError = "model_context_window_exceeded";
            boolean resultado = detector.esErrorContextoExcedido("anthropic", 400, cuerpoError);
            assertTrue(resultado, "Debe detectar error para anthropic normalizado a Claude");
        }

        @Test
        @DisplayName("detecta error con proveedor google normalizado a Gemini")
        void detectaConGoogleNormalizado() {
            String cuerpoError = "RESOURCE_EXHAUSTED";
            boolean resultado = detector.esErrorContextoExcedido("google", 400, cuerpoError);
            assertTrue(resultado, "Debe detectar error para google normalizado a Gemini");
        }
    }

    @Nested
    @DisplayName("Extracción de límite de tokens")
    class ExtraccionLimiteTests {

        @Test
        @DisplayName("extrae límite de mensaje de OpenAI")
        void extraeLimiteOpenAI() {
            String cuerpoError = "This model's maximum context length is 128000 tokens";
            
            int limite = detector.extraerLimiteTokens(cuerpoError);
            
            assertEquals(128000, limite, "Debe extraer 128000 tokens");
        }

        @Test
        @DisplayName("extrae límite de mensaje de Claude")
        void extraeLimiteClaude() {
            String cuerpoError = "prompt is too long: 200001 tokens > 200000 maximum";
            
            int limite = detector.extraerLimiteTokens(cuerpoError);
            
            assertEquals(200001, limite, "Debe extraer el primer número de tokens encontrado");
        }

        @Test
        @DisplayName("retorna -1 si no encuentra límite")
        void retornaMenosUnoSinLimite() {
            String cuerpoError = "Error genérico sin información de tokens";
            
            int limite = detector.extraerLimiteTokens(cuerpoError);
            
            assertEquals(-1, limite, "Debe retornar -1 cuando no hay límite");
        }

        @Test
        @DisplayName("maneja mensaje null")
        void manejaMensajeNull() {
            int limite = detector.extraerLimiteTokens(null);
            
            assertEquals(-1, limite, "Debe retornar -1 para mensaje null");
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class CasosBordeTests {

        @Test
        @DisplayName("no detecta contexto con status code incorrecto")
        void noDetectaConStatusCodeIncorrecto() {
            String cuerpoError = "context_length_exceeded";
            
            boolean resultado = detector.esErrorContextoExcedido("OpenAI", 500, cuerpoError);
            
            assertFalse(resultado, "No debe detectar con status code 500");
        }

        @Test
        @DisplayName("detecta contexto con status 429 (Gemini)")
        void detectaConStatus429() {
            String cuerpoError = "RESOURCE_EXHAUSTED: Token count exceeds limit";
            
            boolean resultado = detector.esErrorContextoExcedido("Gemini", 429, cuerpoError);
            
            assertTrue(resultado, "Debe detectar contexto con status 429 para Gemini");
        }

        @Test
        @DisplayName("maneja mensaje null")
        void manejaMensajeNullEnDeteccion() {
            boolean resultado = detector.esErrorContextoExcedido("OpenAI", 400, null);
            
            assertFalse(resultado, "No debe detectar con mensaje null");
        }

        @Test
        @DisplayName("maneja mensaje vacío")
        void manejaMensajeVacio() {
            boolean resultado = detector.esErrorContextoExcedido("OpenAI", 400, "");
            
            assertFalse(resultado, "No debe detectar con mensaje vacío");
        }

        @Test
        @DisplayName("detecta patrones genéricos para proveedor desconocido")
        void detectaPatronesGenericos() {
            String cuerpoError = "Error: prompt too long for this model";
            
            boolean resultado = detector.esErrorContextoExcedido("ProveedorDesconocido", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar patrones genéricos para proveedor desconocido");
        }
    }

    @Nested
    @DisplayName("Patrones custom")
    class PatronesCustomTests {

        @Test
        @DisplayName("detecta error de Custom 01")
        void detectaCustom01() {
            String cuerpoError = "{\"error\": \"context_length_exceeded: your prompt is too long\"}";
            
            boolean resultado = detector.esErrorContextoExcedido("Custom 01", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de Custom 01");
        }

        @Test
        @DisplayName("detecta error de Custom 02 con patrón genérico")
        void detectaCustom02() {
            String cuerpoError = "token limit exceeded: prompt too long";
            
            boolean resultado = detector.esErrorContextoExcedido("Custom 02", 400, cuerpoError);
            
            assertTrue(resultado, "Debe detectar error de Custom 02");
        }
    }
}