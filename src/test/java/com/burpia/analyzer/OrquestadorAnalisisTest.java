package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrquestadorAnalisis Tests")
class OrquestadorAnalisisTest {

    private static final String PROVEEDOR_OPENAI = "OpenAI";
    private static final String MODELO_OPENAI = "gpt-5-mini";

    private ConfiguracionAPI crearConfiguracionValida(String proveedor, String modelo) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI(proveedor);
        config.establecerModeloParaProveedor(proveedor, modelo);
        config.establecerUrlBaseParaProveedor(proveedor, "https://api.example.com/v1");
        config.establecerApiKeyParaProveedor(proveedor, "sk-test");
        return config;
    }

    private SolicitudAnalisis crearSolicitudBasica(String url, String metodo, String hash) {
        return new SolicitudAnalisis(url, metodo, "", "", hash);
    }

    private OrquestadorAnalisis crearOrquestador(ConfiguracionAPI config, SolicitudAnalisis solicitud) {
        return new OrquestadorAnalisis(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            null,
            null,
            null,
            null,
            null
        );
    }

    private ResultadoAnalisisMultiple invocarParsearRespuesta(OrquestadorAnalisis orquestador, String respuesta)
            throws Exception {
        Method metodoParseo = OrquestadorAnalisis.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);
        return (ResultadoAnalisisMultiple) metodoParseo.invoke(orquestador, respuesta);
    }

    @Test
    @DisplayName("Orquestador recupera evidencia HTML con comillas sin escapar")
    void testParsearRespuestaRecuperaComillasSinEscaparEnEvidencia() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/form", "POST", "hash-orq-parse");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        String respuesta = "{\"hallazgos\":["
            + "{\"descripcion\":\"HTTP sin cifrado\",\"severidad\":\"Medium\",\"confianza\":\"High\",\"evidencia\":\"POST http://example.com\"},"
            + "{\"descripcion\":\"Falta token CSRF\",\"severidad\":\"Low\",\"confianza\":\"Low\",\"evidencia\":\"<form action=\"search.php\" method=\"post\">\"}"
            + "]}";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(orquestador, respuesta);

        assertEquals(2, resultado.obtenerNumeroHallazgos(),
            "El orquestador debe recuperar 2 hallazgos aunque la evidencia rompa el JSON");
        assertTrue(
            resultado.obtenerHallazgos().get(1).obtenerHallazgo()
                .contains("Evidencia: <form action=\"search.php\" method=\"post\">"),
            "La evidencia HTML debe conservarse en el hallazgo recuperado"
        );
    }

    @Test
    @DisplayName("Orquestador preserva evidencia de banner HTML largo")
    void testParsearRespuestaRecuperaBannerHtmlLargo() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/banner", "GET", "hash-orq-banner");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        String respuesta = "{\"hallazgos\":["
            + "{\"titulo\":\"Uno\",\"descripcion\":\"Detalle 1\",\"severidad\":\"Low\",\"confianza\":\"High\",\"evidencia\":\"a\"},"
            + "{\"titulo\":\"Dos\",\"descripcion\":\"Detalle 2\",\"severidad\":\"Low\",\"confianza\":\"High\",\"evidencia\":\"b\"},"
            + "{\"titulo\":\"Tres\",\"descripcion\":\"Detalle 3\",\"severidad\":\"Medium\",\"confianza\":\"Medium\",\"evidencia\":\"c\"},"
            + "{\"titulo\":\"Banner\",\"descripcion\":\"Detalle 4\",\"severidad\":\"Info\",\"confianza\":\"High\",\"evidencia\":\"<div style=\"background-color:lightgray;width:100%;text-align:center\"><p style=\"padding-left:5%\"><b>Warning</b>: demo</p></div>\"}"
            + "]}";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(orquestador, respuesta);

        assertEquals(4, resultado.obtenerNumeroHallazgos(),
            "El orquestador debe recuperar 4 hallazgos aunque la evidencia HTML rompa el JSON");
        assertTrue(
            resultado.obtenerHallazgos().get(3).obtenerHallazgo()
                .contains("Evidencia: <div style=\"background-color:lightgray;width:100%;text-align:center\"><p style=\"padding-left:5%\"><b>Warning</b>: demo</p></div>"),
            "La evidencia del banner debe conservarse íntegra"
        );
    }

    @Test
    @DisplayName("Orquestador no aplica retraso adicional antes de llamar a la API")
    void testEjecutarAnalisisCompletoNoAplicaRetrasoPropio() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        config.establecerRetrasoSegundos(2);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/delay", "GET", "hash-orq-delay");

        try (MockedConstruction<AnalizadorHTTP> analizadorHttpMock = mockConstruction(
                AnalizadorHTTP.class,
                (mock, context) -> when(mock.llamarAPI(anyString())).thenReturn("{\"hallazgos\":[]}"))) {
            OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

            long inicio = System.nanoTime();
            ResultadoAnalisisMultiple resultado = orquestador.ejecutarAnalisisCompleto();
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000L;

            assertNotNull(resultado, "El orquestador debe retornar un resultado aunque la respuesta esté vacía");
            assertTrue(duracionMs < 1500,
                "El orquestador no debe dormir " + config.obtenerRetrasoSegundos() + "s por su cuenta; duración=" + duracionMs + "ms");
            assertEquals(1, analizadorHttpMock.constructed().size(),
                "Debe construirse un solo AnalizadorHTTP para el orquestador");
            verify(analizadorHttpMock.constructed().get(0)).llamarAPI(anyString());
        }
    }
}
