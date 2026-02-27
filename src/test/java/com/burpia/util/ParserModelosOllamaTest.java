package com.burpia.util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("ParserModelosOllama Tests")
class ParserModelosOllamaTest {

    @Test
    @DisplayName("Extrae modelos validos desde /api/tags")
    void testExtraeModelosValidos() {
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
    @DisplayName("Usa fallback model cuando name no existe")
    void testFallbackCampoModel() {
        String json = "{\"models\":[{\"model\":\"qwen2.5:latest\"}]}";
        List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
        assertEquals(List.of("qwen2.5:latest"), modelos);
    }

    @Test
    @DisplayName("Filtra vacios y dos puntos invalidos")
    void testFiltraInvalidos() {
        String json = "{\"models\":[{\"name\":\" : \"},{\"name\":\"\"},{\"name\":\"ok:latest\"},{\"name\":\":\"}]}";
        List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
        assertEquals(List.of("ok:latest"), modelos);
        assertTrue(modelos.stream().noneMatch(m -> m.trim().equals(":")));
    }

    @Test
    @DisplayName("JSON invalido retorna lista vacia sin lanzar excepcion")
    void testJsonInvalidoNoRompe() {
        List<String> modelos = ParserModelosOllama.extraerModelosDesdeTags("{invalid-json");
        assertTrue(modelos.isEmpty());
    }
}
