package com.burpia.analyzer;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import okhttp3.OkHttpClient;
import java.lang.reflect.Field;
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
        assertEquals("Sin título", resultado.obtenerHallazgos().get(0).obtenerTitulo());
        assertEquals("SQLi detectada", resultado.obtenerHallazgos().get(0).obtenerHallazgo());
    }

    @Test
    @DisplayName("Fallback no estricto recupera hallazgos cuando evidencia trae comillas sin escapar")
    void testParseoNoEstrictoConComillasSinEscaparEnEvidencia() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "POST",
            "",
            "",
            "hash-nonstrict"
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

        String respuesta = "{\"hallazgos\":["
            + "{\"descripcion\":\"HTTP sin cifrado\",\"severidad\":\"Medium\",\"confianza\":\"High\",\"evidencia\":\"POST http://example.com\"},"
            + "{\"descripcion\":\"Falta token CSRF\",\"severidad\":\"Low\",\"confianza\":\"Low\",\"evidencia\":\"<form action=\"search.php\" method=\"post\">\"}"
            + "]}";

        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);

        ResultadoAnalisisMultiple resultado = (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);

        assertEquals(2, resultado.obtenerNumeroHallazgos());
        assertTrue(resultado.obtenerHallazgos().get(1).obtenerHallazgo().contains("Evidencia: <form action=\"search.php\" method=\"post\">"));
    }

    @Test
    @DisplayName("Fallback no estricto extrae el titulo correctamente")
    void testParseoNoEstrictoExtraeTitulo() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api",
            "PUT",
            "",
            "",
            "hash-title-extract"
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

        String respuesta = "{\"hallazgos\":["
            + "{\"titulo\":\"Inyeccion SQL Detectada\",\"descripcion\":\"Se encontro una inyeccion SQL...\",\"severidad\":\"Critical\",\"confianza\":\"High\"}"
            + "]}";

        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);

        ResultadoAnalisisMultiple resultado = (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);

        assertEquals(1, resultado.obtenerNumeroHallazgos());
        assertEquals("Inyeccion SQL Detectada", resultado.obtenerHallazgos().get(0).obtenerTitulo());
    }

    @Test
    @DisplayName("Fallback no estricto soporta clave findings con campos en ingles")
    void testParseoNoEstrictoConFindingsEnIngles() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api",
            "GET",
            "",
            "",
            "hash-findings-en"
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

        String respuesta = "{\"findings\":["
            + "{\"title\":\"Missing HSTS\",\"description\":\"No Strict-Transport-Security header\",\"severity\":\"Medium\",\"confidence\":\"High\",\"evidence\":\"Strict-Transport-Security absent\"}"
            + "]} trailing";

        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);

        ResultadoAnalisisMultiple resultado = (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);

        assertEquals(1, resultado.obtenerNumeroHallazgos());
        assertEquals("Missing HSTS", resultado.obtenerHallazgos().get(0).obtenerTitulo());
        assertEquals("Medium", resultado.obtenerHallazgos().get(0).obtenerSeveridad());
        assertEquals("High", resultado.obtenerHallazgos().get(0).obtenerConfianza());
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

    @Test
    @DisplayName("Constructor usa timeout efectivo por proveedor y modelo")
    void testConstructorUsaTimeoutPorModelo() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");
        config.establecerTiempoEsperaAI(60);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-timeout"
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

        Field campoCliente = AnalizadorAI.class.getDeclaredField("clienteHttp");
        campoCliente.setAccessible(true);
        OkHttpClient cliente = (OkHttpClient) campoCliente.get(analizador);

        assertEquals(180_000, cliente.readTimeoutMillis());
    }
}
