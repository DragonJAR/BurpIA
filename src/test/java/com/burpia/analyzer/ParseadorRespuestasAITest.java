package com.burpia.analyzer;

import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParseadorRespuestasAI Tests")
class ParseadorRespuestasAITest {

    private ParseadorRespuestasAI crearParseador() {
        return new ParseadorRespuestasAI(GestorLoggingUnificado.crearMinimal(null, null), "es");
    }

    private SolicitudAnalisis crearSolicitud() {
        return new SolicitudAnalisis("https://example.com/api", "GET", "", "", "hash-test");
    }

    @Nested
    @DisplayName("Manejo de solicitud null")
    class SolicitudNull {

        @Test
        @DisplayName("Lanza ParseExceptionAI (no NPE) cuando la solicitud es null y el parseo falla")
        void lanzaParseExceptionNoNpeConSolicitudNull() {
            ParseadorRespuestasAI parseador = crearParseador();

            ParseadorRespuestasAI.ParseExceptionAI error = assertThrows(
                ParseadorRespuestasAI.ParseExceptionAI.class,
                () -> parseador.parsearRespuesta("respuesta invalida {{{", null, "OpenAI"),
                "El catch debe ser null-safe y envolver la excepción original en ParseExceptionAI");

            assertNotNull(error.getCause(), "La causa original debe conservarse");
        }
    }

    @Nested
    @DisplayName("Patrones de etiquetas de texto plano")
    class PatronesEtiquetas {

        @Test
        @DisplayName("La palabra 'vulnerabilidad' sin dos puntos no se borra de la descripcion")
        void palabraVulnerabilidadSinDosPuntosSeConserva() {
            ParseadorRespuestasAI parseador = crearParseador();
            String respuestaTextoPlano =
                "Título: XSS Reflejado\n"
                + "Severidad: High\n"
                + "La vulnerabilidad crítica permite ejecutar JavaScript arbitrario en el navegador";

            ResultadoAnalisisMultiple resultado = parseador.parsearRespuesta(
                respuestaTextoPlano, crearSolicitud(), "OpenAI");

            assertFalse(resultado.obtenerHallazgos().isEmpty(), "Debe parsear el hallazgo de texto plano");
            Hallazgo hallazgo = resultado.obtenerHallazgos().get(0);
            assertTrue(hallazgo.obtenerHallazgo().contains("vulnerabilidad"),
                "El patrón anclado no debe borrar 'vulnerabilidad' cuando aparece sin ':' en prosa. "
                    + "Descripción: " + hallazgo.obtenerHallazgo());
        }

        @Test
        @DisplayName("La etiqueta 'Description:' al inicio de linea sigue recortandose")
        void etiquetaDescriptionAncladaSigueRecortandose() {
            ParseadorRespuestasAI parseador = crearParseador();
            String respuestaTextoPlano =
                "Title: SQL Injection\n"
                + "Severity: High\n"
                + "Description: el parametro id concatena entrada sin sanitizar en la consulta";

            ResultadoAnalisisMultiple resultado = parseador.parsearRespuesta(
                respuestaTextoPlano, crearSolicitud(), "OpenAI");

            assertFalse(resultado.obtenerHallazgos().isEmpty(), "Debe parsear el hallazgo de texto plano");
            Hallazgo hallazgo = resultado.obtenerHallazgos().get(0);
            assertFalse(hallazgo.obtenerHallazgo().startsWith("Description:"),
                "La etiqueta anclada debe eliminarse del contenido");
            assertTrue(hallazgo.obtenerHallazgo().contains("parametro id"),
                "El contenido real de la descripción debe conservarse");
        }
    }
}
