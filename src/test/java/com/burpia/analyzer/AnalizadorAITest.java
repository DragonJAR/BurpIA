package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AnalizadorAI Tests")
class AnalizadorAITest {

    @Test
    @DisplayName("Parseo de hallazgos tolera elementos JSON invalidos sin romper")
    void testParseoHallazgosInvalidosNoRompe() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        AnalizadorAI analizador = new AnalizadorAI(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                }

                @Override
                public void alErrorAnalisis(String error) {
                }
            }
        );

        String respuesta = "{\"hallazgos\":[123,{\"descripcion\":\"SQLi detectada\",\"severidad\":\"High\",\"confianza\":\"Medium\"},{\"descripcion\":{},\"severidad\":{},\"confianza\":{}}]}";
        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);

        ResultadoAnalisisMultiple resultado = (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);

        assertTrue(resultado.obtenerNumeroHallazgos() >= 1);
        assertEquals("SQLi detectada", resultado.obtenerHallazgos().get(0).obtenerHallazgo());
    }

    @Test
    @DisplayName("Constructor tolera dependencias nulas sin lanzar excepcion")
    void testConstructorConDependenciasNulasNoRompe() {
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        assertDoesNotThrow(() -> new AnalizadorAI(
            solicitud,
            null,
            null,
            null,
            null,
            null
        ));
    }
}
