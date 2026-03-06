package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link ParserModelosOllama}.
 * <p>
 * Verifica el parsing robusto de respuestas JSON del endpoint /api/tags de Ollama,
 * incluyendo manejo de edge cases, deduplicación y validación de modelos.
 * </p>
 */
@DisplayName("ParserModelosOllama Tests")
class ParserModelosOllamaTest {

    @Nested
    @DisplayName("extraerModelosDesdeTags")
    class ExtraerModelosDesdeTags {

        @Nested
        @DisplayName("Casos válidos")
        class CasosValidos {

            @Test
            @DisplayName("Extrae modelos válidos desde JSON con formato estándar")
            void extraeModelosValidosDesdeApiTags() {
                String json = "{\n" +
                    "  \"models\": [\n" +
                    "    {\"name\":\"deepseek-r1:latest\"},\n" +
                    "    {\"name\":\"llama3.2:latest\"}\n" +
                    "  ]\n" +
                    "}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(2, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:44");
                assertEquals("deepseek-r1:latest", modelos.get(0), "assertEquals failed at ParserModelosOllamaTest.java:45");
                assertEquals("llama3.2:latest", modelos.get(1), "assertEquals failed at ParserModelosOllamaTest.java:46");
            }

            @Test
            @DisplayName("Usa campo 'model' como fallback cuando 'name' no existe")
            void usaFallbackCampoModel() {
                String json = "{\"models\":[{\"model\":\"qwen2.5:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("qwen2.5:latest"), modelos, "assertEquals failed at ParserModelosOllamaTest.java:54");
            }

            @Test
            @DisplayName("Prefiere campo 'name' sobre 'model' cuando ambos existen")
            void prefiereCampoNameSobreModel() {
                String json = "{\"models\":[{\"name\":\"llama3:latest\",\"model\":\"qwen2.5:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("llama3:latest"), modelos, "assertEquals failed at ParserModelosOllamaTest.java:62");
            }

            @Test
            @DisplayName("Deduplica modelos manteniendo orden de aparición")
            void deduplicaModelosManteniendoOrden() {
                String json = "{\"models\":[" +
                    "{\"name\":\"llama3:latest\"}," +
                    "{\"name\":\"mistral:latest\"}," +
                    "{\"name\":\"llama3:latest\"}," +
                    "{\"name\":\"codellama:latest\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(3, modelos.size(), "Debe deduplicar modelos repetidos");
                assertEquals("llama3:latest", modelos.get(0), "assertEquals failed at ParserModelosOllamaTest.java:78");
                assertEquals("mistral:latest", modelos.get(1), "assertEquals failed at ParserModelosOllamaTest.java:79");
                assertEquals("codellama:latest", modelos.get(2), "assertEquals failed at ParserModelosOllamaTest.java:80");
            }

            @Test
            @DisplayName("Maneja modelos con espacios en blanco (normalización aplicada)")
            void manejaModelosConEspacios() {
                String json = "{\"models\":[{\"name\":\"  llama3:latest  \"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("llama3:latest"), modelos, "assertEquals failed at ParserModelosOllamaTest.java:88");
            }

            @Test
            @DisplayName("Maneja modelos con caracteres especiales")
            void manejaModelosConCaracteresEspeciales() {
                String json = "{\"models\":[" +
                    "{\"name\":\"model-with-dashes:latest\"}," +
                    "{\"name\":\"model_with_underscores:v1.0\"}," +
                    "{\"name\":\"model.with.dots:2024-01-01\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(3, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:102");
                assertTrue(modelos.contains("model-with-dashes:latest"), "assertTrue failed at ParserModelosOllamaTest.java:103");
                assertTrue(modelos.contains("model_with_underscores:v1.0"), "assertTrue failed at ParserModelosOllamaTest.java:104");
                assertTrue(modelos.contains("model.with.dots:2024-01-01"), "assertTrue failed at ParserModelosOllamaTest.java:105");
            }
        }

        @Nested
        @DisplayName("Filtrado de inválidos")
        class FiltradoInvalidos {

            @Test
            @DisplayName("Filtra modelos vacíos y con solo dos puntos")
            void filtraVaciosYSoloDosPuntos() {
                String json = "{\"models\":[" +
                    "{\"name\":\" : \"}," +
                    "{\"name\":\"\"}," +
                    "{\"name\":\"ok:latest\"}," +
                    "{\"name\":\":\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(List.of("ok:latest"), modelos, "assertEquals failed at ParserModelosOllamaTest.java:125");
                assertTrue(modelos.stream().noneMatch(m -> ":".equals(m.trim())), "assertTrue failed at ParserModelosOllamaTest.java:126");
            }

            @Test
            @DisplayName("Filtra elementos que no son objetos JSON")
            void filtraElementosNoObjeto() {
                String json = "{\"models\":[" +
                    "null," +
                    "\"string-directo\"," +
                    "123," +
                    "[\"array\"]," +
                    "{\"name\":\"valido:latest\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(1, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:142");
                assertEquals("valido:latest", modelos.get(0), "assertEquals failed at ParserModelosOllamaTest.java:143");
            }

            @Test
            @DisplayName("Filtra objetos sin campos name ni model")
            void filtraObjetosSinCamposValidos() {
                String json = "{\"models\":[" +
                    "{\"other\":\"field\"}," +
                    "{\"name\":null}," +
                    "{\"model\":null}," +
                    "{\"name\":\"valido:latest\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                assertEquals(1, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:158");
                assertEquals("valido:latest", modelos.get(0), "assertEquals failed at ParserModelosOllamaTest.java:159");
            }
        }

        @Nested
        @DisplayName("Casos edge")
        class CasosEdge {

            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {"   ", "\t", "\n"})
            @DisplayName("Retorna lista vacía para input null, vacío o solo espacios")
            void retornaVacioParaInputInvalido(String input) {
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(input);
                assertNotNull(modelos, "assertNotNull failed at ParserModelosOllamaTest.java:173");
                assertTrue(modelos.isEmpty(), "assertTrue failed at ParserModelosOllamaTest.java:174");
            }

            @Test
            @DisplayName("Retorna lista vacía para JSON inválido sin lanzar excepción")
            void retornaVacioParaJsonInvalido() {
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags("{invalid-json");
                assertTrue(modelos.isEmpty(), "assertTrue failed at ParserModelosOllamaTest.java:181");
            }

            @Test
            @DisplayName("Retorna lista vacía cuando falta clave 'models'")
            void retornaVacioCuandoFaltaClaveModels() {
                String json = "{\"other\":[{\"name\":\"test:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertTrue(modelos.isEmpty(), "assertTrue failed at ParserModelosOllamaTest.java:189");
            }

            @Test
            @DisplayName("Retorna lista vacía para array 'models' vacío")
            void retornaVacioParaArrayModelsVacio() {
                String json = "{\"models\":[]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertTrue(modelos.isEmpty(), "assertTrue failed at ParserModelosOllamaTest.java:197");
            }

            @Test
            @DisplayName("Retorna lista vacía para JSON que no es objeto (array, primitivo)")
            void retornaVacioParaJsonNoObjeto() {
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("[]").size(), "assertEquals failed at ParserModelosOllamaTest.java:203");
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("\"string\"").size(), "assertEquals failed at ParserModelosOllamaTest.java:204");
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("123").size(), "assertEquals failed at ParserModelosOllamaTest.java:205");
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("true").size(), "assertEquals failed at ParserModelosOllamaTest.java:206");
            }

            @Test
            @DisplayName("Maneja campo name/model que no es string (number, boolean, object)")
            void manejaCamposNoString() {
                String json = "{\"models\":[" +
                    "{\"name\":123}," +
                    "{\"name\":true}," +
                    "{\"name\":{\"nested\":\"object\"}}," +
                    "{\"model\":456.78}," +
                    "{\"name\":\"valido:latest\"}" +
                    "]}";

                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);

                // Gson convierte number y boolean a string ("123", "true", "456.78")
                // Solo objetos JSON anidados son filtrados (no son JsonPrimitive)
                assertEquals(4, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:224");
                assertTrue(modelos.contains("123"), "assertTrue failed at ParserModelosOllamaTest.java:225");
                assertTrue(modelos.contains("true"), "assertTrue failed at ParserModelosOllamaTest.java:226");
                assertTrue(modelos.contains("456.78"), "assertTrue failed at ParserModelosOllamaTest.java:227");
                assertTrue(modelos.contains("valido:latest"), "assertTrue failed at ParserModelosOllamaTest.java:228");
            }

            @Test
            @DisplayName("Maneja JSON con caracteres de escape en nombres")
            void manejaCaracteresEscapeEnNombres() {
                String json = "{\"models\":[{\"name\":\"model\\\"quoted\\\":latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(1, modelos.size(), "assertEquals failed at ParserModelosOllamaTest.java:236");
                assertEquals("model\"quoted\":latest", modelos.get(0), "assertEquals failed at ParserModelosOllamaTest.java:237");
            }
        }
    }
}
