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

                assertEquals(2, modelos.size());
                assertEquals("deepseek-r1:latest", modelos.get(0));
                assertEquals("llama3.2:latest", modelos.get(1));
            }

            @Test
            @DisplayName("Usa campo 'model' como fallback cuando 'name' no existe")
            void usaFallbackCampoModel() {
                String json = "{\"models\":[{\"model\":\"qwen2.5:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("qwen2.5:latest"), modelos);
            }

            @Test
            @DisplayName("Prefiere campo 'name' sobre 'model' cuando ambos existen")
            void prefiereCampoNameSobreModel() {
                String json = "{\"models\":[{\"name\":\"llama3:latest\",\"model\":\"qwen2.5:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("llama3:latest"), modelos);
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
                assertEquals("llama3:latest", modelos.get(0));
                assertEquals("mistral:latest", modelos.get(1));
                assertEquals("codellama:latest", modelos.get(2));
            }

            @Test
            @DisplayName("Maneja modelos con espacios en blanco (normalización aplicada)")
            void manejaModelosConEspacios() {
                String json = "{\"models\":[{\"name\":\"  llama3:latest  \"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(List.of("llama3:latest"), modelos);
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

                assertEquals(3, modelos.size());
                assertTrue(modelos.contains("model-with-dashes:latest"));
                assertTrue(modelos.contains("model_with_underscores:v1.0"));
                assertTrue(modelos.contains("model.with.dots:2024-01-01"));
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

                assertEquals(List.of("ok:latest"), modelos);
                assertTrue(modelos.stream().noneMatch(m -> m.trim().equals(":")));
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

                assertEquals(1, modelos.size());
                assertEquals("valido:latest", modelos.get(0));
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

                assertEquals(1, modelos.size());
                assertEquals("valido:latest", modelos.get(0));
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
                assertNotNull(modelos);
                assertTrue(modelos.isEmpty());
            }

            @Test
            @DisplayName("Retorna lista vacía para JSON inválido sin lanzar excepción")
            void retornaVacioParaJsonInvalido() {
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags("{invalid-json");
                assertTrue(modelos.isEmpty());
            }

            @Test
            @DisplayName("Retorna lista vacía cuando falta clave 'models'")
            void retornaVacioCuandoFaltaClaveModels() {
                String json = "{\"other\":[{\"name\":\"test:latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertTrue(modelos.isEmpty());
            }

            @Test
            @DisplayName("Retorna lista vacía para array 'models' vacío")
            void retornaVacioParaArrayModelsVacio() {
                String json = "{\"models\":[]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertTrue(modelos.isEmpty());
            }

            @Test
            @DisplayName("Retorna lista vacía para JSON que no es objeto (array, primitivo)")
            void retornaVacioParaJsonNoObjeto() {
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("[]").size());
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("\"string\"").size());
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("123").size());
                assertEquals(0, ParserModelosOllama.extraerModelosDesdeTags("true").size());
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
                assertEquals(4, modelos.size());
                assertTrue(modelos.contains("123"));
                assertTrue(modelos.contains("true"));
                assertTrue(modelos.contains("456.78"));
                assertTrue(modelos.contains("valido:latest"));
            }

            @Test
            @DisplayName("Maneja JSON con caracteres de escape en nombres")
            void manejaCaracteresEscapeEnNombres() {
                String json = "{\"models\":[{\"name\":\"model\\\"quoted\\\":latest\"}]}";
                List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                assertEquals(1, modelos.size());
                assertEquals("model\"quoted\":latest", modelos.get(0));
            }
        }
    }
}
