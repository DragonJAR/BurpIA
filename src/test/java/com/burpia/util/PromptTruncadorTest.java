package com.burpia.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PromptTruncador.
 * 
 * <p>Verifica las estrategias de truncado de prompts para ajustar
 * al límite de tokens del modelo.</p>
 */
@DisplayName("PromptTruncador Tests")
class PromptTruncadorTest {

    private PromptTruncador truncador;

    @BeforeEach
    void setUp() {
        truncador = new PromptTruncador();
    }

    @Nested
    @DisplayName("Estimación de tokens")
    class EstimacionTokensTests {

        @Test
        @DisplayName("estima tokens correctamente")
        void estimaTokens() {
            String texto = "Hola mundo"; // 10 caracteres = ~2.5 tokens
            
            int tokens = truncador.estimarTokens(texto);
            
            assertEquals(3, tokens, "10 caracteres ≈ 2.5 tokens redondeado a 3");
        }

        @Test
        @DisplayName("retorna 0 para texto null")
        void retornaCeroParaNull() {
            int tokens = truncador.estimarTokens(null);
            
            assertEquals(0, tokens, "Debe retornar 0 para null");
        }

        @Test
        @DisplayName("retorna 0 para texto vacío")
        void retornaCeroParaVacio() {
            int tokens = truncador.estimarTokens("");
            
            assertEquals(0, tokens, "Debe retornar 0 para string vacío");
        }
    }

    @Nested
    @DisplayName("Truncado de prompt")
    class TruncadoPromptTests {

        @Test
        @DisplayName("no trunca si el prompt cabe")
        void noTruncaSiCabe() {
            String prompt = "Prompt corto";
            int tokensObjetivo = 100;
            
            String resultado = truncador.truncarPrompt(prompt, tokensObjetivo);
            
            assertEquals(prompt, resultado, "No debe truncar si ya cabe");
        }

        @Test
        @DisplayName("trunca prompt largo")
        void truncaPromptLargo() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("abcdefghij"); // 10 caracteres
            }
            String prompt = sb.toString(); // 100000 caracteres ≈ 25000 tokens
            
            String resultado = truncador.truncarPrompt(prompt, 1000);
            
            assertTrue(resultado.length() < prompt.length(), "Debe truncar el prompt");
            assertTrue(resultado.length() > 0, "El resultado no debe estar vacío");
        }

        @Test
        @DisplayName("maneja prompt null")
        void manejaPromptNull() {
            String resultado = truncador.truncarPrompt(null, 100);
            
            assertNull(resultado, "Debe retornar null para input null");
        }

        @Test
        @DisplayName("maneja prompt vacío")
        void manejaPromptVacio() {
            String resultado = truncador.truncarPrompt("", 100);
            
            assertEquals("", resultado, "Debe retornar vacío para input vacío");
        }

        @Test
        @DisplayName("respeta mínimo de 100 tokens")
        void respetaMinimoTokens() {
            String prompt = "Prompt de prueba con suficiente contenido para truncar";
            int tokensObjetivo = 10; // Muy bajo, debe usar mínimo 100
            
            String resultado = truncador.truncarPrompt(prompt, tokensObjetivo);
            
            // El resultado debe ser válido (no excepción)
            assertNotNull(resultado);
        }
    }

    @Nested
    @DisplayName("Cálculo de tokens disponibles")
    class CalculoTokensDisponiblesTests {

        @ParameterizedTest
        @CsvSource({
            "128000, 4000, 123000", // GPT-4o: 128000 - 4000 - 1000 = 123000
            "8192, 2000, 5192",     // GPT-4: 8192 - 2000 - 1000 = 5192
            "4096, 1000, 2096",     // GPT-3.5: 4096 - 1000 - 1000 = 2096
            "200000, 8000, 191000"  // Claude: 200000 - 8000 - 1000 = 191000
        })
        @DisplayName("calcula tokens disponibles correctamente")
        void calculaTokensDisponibles(int contextWindow, int maxOutput, int esperado) {
            int resultado = truncador.calcularTokensDisponibles(contextWindow, maxOutput);
            
            assertEquals(esperado, resultado, "Debe calcular correctamente los tokens disponibles");
        }

        @Test
        @DisplayName("garantiza mínimo de 1000 tokens")
        void garantizaMinimo() {
            int resultado = truncador.calcularTokensDisponibles(2000, 500);
            
            assertEquals(1000, resultado, "Debe garantizar mínimo 1000 tokens");
        }
    }

    @Nested
    @DisplayName("Estrategias de truncado")
    class EstrategiasTruncadoTests {

        @Test
        @DisplayName("preserva inicio del prompt (system prompt)")
        void preservaInicioPrompt() {
            String systemPrompt = "INSTRUCCIONES CRÍTICAS: Analiza el siguiente request HTTP...\n\n";
            StringBuilder sb = new StringBuilder(systemPrompt);
            for (int i = 0; i < 5000; i++) {
                sb.append("Datos de request HTTP línea ").append(i).append("\n");
            }
            String prompt = sb.toString();
            
            String resultado = truncador.truncarPrompt(prompt, 500);
            
            assertTrue(resultado.startsWith(systemPrompt.substring(0, 20)), 
                "Debe preservar el inicio del prompt");
        }

        @Test
        @DisplayName("agrega marcador de truncado")
        void agregaMarcadorTruncado() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5000; i++) {
                sb.append("abcdefghij"); // 50000 caracteres
            }
            String prompt = sb.toString();

            String resultado = truncador.truncarPrompt(prompt, 100);

            assertTrue(resultado.contains("[TRUNCATED]") || resultado.contains("trunc"),
                "Debe contener indicador de truncado");
        }

        @Test
        @DisplayName("trunca bodies con marcadores en MAYÚSCULAS de ConstructorPrompts")
        void truncaBodiesConMarcadoresMayusculas() {
            // Regresión H2: ConstructorPrompts emite "BODY:"/"RESPONSE:" en mayúsculas;
            // el truncador debe detectarlos (antes comparaba "Body:"/"Response:" y la
            // estrategia 1 nunca se activaba sobre prompts reales).
            com.burpia.config.ConfiguracionAPI config = new com.burpia.config.ConfiguracionAPI();
            config.establecerIdiomaUi("en");
            config.establecerPromptConfigurable("Analyze: {REQUEST}\n{RESPONSE}");

            String cuerpoGrande = "x".repeat(4000);
            com.burpia.model.SolicitudAnalisis solicitud = new com.burpia.model.SolicitudAnalisis(
                "https://example.com/api",
                "POST",
                "Host: example.com",
                cuerpoGrande,
                "hash-trunc-test",
                null,
                200,
                "Content-Type: text/plain",
                cuerpoGrande
            );

            com.burpia.analyzer.ConstructorPrompts constructor = new com.burpia.analyzer.ConstructorPrompts(config);
            String promptReal = constructor.construirPromptAnalisis(solicitud);
            assertTrue(promptReal.contains(PromptTruncador.MARCADOR_BODY),
                "El prompt real debe contener el marcador canónico BODY:");

            String resultado = truncador.truncarPrompt(promptReal, 1000);

            assertTrue(resultado.contains("[TRUNCATED]"),
                "La estrategia 1 debe activarse y truncar los bodies del prompt real");
            assertTrue(resultado.length() < promptReal.length(),
                "El prompt truncado debe ser más corto que el original");
        }
    }

    @Nested
    @DisplayName("Integración")
    class IntegracionTests {

        @Test
        @DisplayName("truncado progresivo reduce tamaño")
        void truncadoProgresivo() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("x");
            }
            String promptOriginal = sb.toString();
            
            // El truncado debe producir un resultado más pequeño
            String truncado = truncador.truncarPrompt(promptOriginal, 1000);
            
            assertTrue(truncado.length() < promptOriginal.length(), 
                "El truncado debe ser más pequeño que el original");
            assertTrue(truncado.length() > 0, "El resultado no debe estar vacío");
        }
    }
}