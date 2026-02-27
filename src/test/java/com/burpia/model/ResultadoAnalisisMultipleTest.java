package com.burpia.model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;




@DisplayName("ResultadoAnalisisMultiple Tests")
class ResultadoAnalisisMultipleTest {

    @Test
    @DisplayName("Tolera lista de hallazgos nula")
    void testToleraListaNula() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            "https://example.com",
            "2026-02-22 10:00:00",
            null,
            null
        );

        assertNotNull(resultado.obtenerHallazgos());
        assertEquals(0, resultado.obtenerNumeroHallazgos());
        assertEquals(Hallazgo.SEVERIDAD_INFO, resultado.obtenerSeveridadMaxima());
    }

    @Test
    @DisplayName("Ignora hallazgos nulos al calcular severidad maxima")
    void testSeveridadMaximaIgnoraNulos() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            "https://example.com",
            "2026-02-22 10:00:00",
            java.util.Arrays.asList(
                null,
                new Hallazgo("https://example.com", "T", "detalle", Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.CONFIANZA_MEDIA)
            ),
            null
        );

        assertEquals(Hallazgo.SEVERIDAD_MEDIUM, resultado.obtenerSeveridadMaxima());
    }
}
