package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.LimitadorTasa;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Method;
import java.util.List;

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
            new ControlCancelacionPausa(() -> false, () -> false)
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

    // Envuelve el contenido del modelo en un envelope OpenAI real, dejando que Gson
    // haga el escape correcto (evita pelear con niveles de \" a mano).
    private String envolverOpenAI(String contenidoModelo) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "assistant");
        message.addProperty("content", contenidoModelo);
        JsonObject choice = new JsonObject();
        choice.add("message", message);
        JsonArray choices = new JsonArray();
        choices.add(choice);
        JsonObject envelope = new JsonObject();
        envelope.add("choices", choices);
        return new Gson().toJson(envelope);
    }

    private JsonObject hallazgoJson(String titulo, String severidad, String confianza,
                                    String descripcion, String evidencia) {
        JsonObject h = new JsonObject();
        h.addProperty("titulo", titulo);
        h.addProperty("severidad", severidad);
        h.addProperty("confianza", confianza);
        h.addProperty("descripcion", descripcion);
        h.addProperty("evidencia", evidencia);
        return h;
    }

    @Test
    @DisplayName("C1: envelope con content JSON y evidencia HTML (comillas escapadas) conserva TODOS los hallazgos")
    void testParsearEnvelopeConEvidenciaHtmlEscapadaConservaHallazgos() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica(
            "http://zero.webappsecurity.com/online-banking.html", "GET", "hash-c1");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        // Caso real del usuario: 8 hallazgos; #3 y #8 con evidencia HTML que lleva
        // comillas (válidas y escapadas en el JSON interno). Antes el desescape doble
        // rompía el JSON y los 8 colapsaban a 1 con evidencia truncada.
        JsonArray hallazgos = new JsonArray();
        hallazgos.add(hallazgoJson("Transmisión en texto claro (HTTP sin TLS)", "Medium", "High",
            "La app de banca se sirve sobre HTTP sin TLS.",
            "GET http://zero.webappsecurity.com/online-banking.html"));
        hallazgos.add(hallazgoJson("CORS mal configurado con comodín", "Medium", "High",
            "Access-Control-Allow-Origin con comodín.", "Access-Control-Allow-Origin: *"));
        hallazgos.add(hallazgoJson("Librería jQuery 1.7.2 obsoleta y vulnerable", "Medium", "Medium",
            "jQuery 1.7.2 con XSS conocidos.",
            "<script src=\"/resources/js/jquery-1.7.2.min.js\"></script>"));
        hallazgos.add(hallazgoJson("Ausencia de cabeceras anti-clickjacking", "Low", "High",
            "Sin X-Frame-Options ni frame-ancestors.",
            "Respuesta sin cabeceras X-Frame-Options ni Content-Security-Policy"));
        hallazgos.add(hallazgoJson("Falta de cabecera HSTS", "Low", "High",
            "Sin Strict-Transport-Security.", "Respuesta sin cabecera Strict-Transport-Security"));
        hallazgos.add(hallazgoJson("Falta de cabecera X-Content-Type-Options", "Low", "High",
            "Sin nosniff.", "Respuesta sin cabecera X-Content-Type-Options"));
        hallazgos.add(hallazgoJson("Fingerprinting del servidor (Apache-Coyote)", "Low", "High",
            "Cabecera Server revela tecnología.", "Server: Apache-Coyote/1.1"));
        hallazgos.add(hallazgoJson("Formulario de búsqueda por GET sin token CSRF", "Low", "Low",
            "searchTerm reflejable por GET.",
            "<form action=\"/search.html\"><input type=\"text\" id=\"searchTerm\" name=\"searchTerm\"/></form>"));
        JsonObject root = new JsonObject();
        root.add("hallazgos", hallazgos);
        String contenidoModelo = new Gson().toJson(root);

        ResultadoAnalisisMultiple resultado =
            invocarParsearRespuesta(orquestador, envolverOpenAI(contenidoModelo));

        assertEquals(8, resultado.obtenerNumeroHallazgos(),
            "Los 8 hallazgos deben conservarse íntegros");
        assertTrue(resultado.obtenerHallazgos().get(2).obtenerHallazgo()
                .contains("<script src=\"/resources/js/jquery-1.7.2.min.js\"></script>"),
            "La evidencia <script> con comillas debe quedar intacta");
        assertTrue(resultado.obtenerHallazgos().get(7).obtenerHallazgo()
                .contains("<form action=\"/search.html\">"),
            "La evidencia <form> con comillas debe quedar intacta");
    }

    @Test
    @DisplayName("Caso real (log 24-jun): 7 hallazgos con evidencia escapada se conservan")
    void testCasoRealSieteHallazgosConEvidenciaEscapada() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica(
            "http://zero.webappsecurity.com/online-banking.html", "GET", "hash-real7");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        // Reproducción del content del log real: 7 hallazgos; #3 (jQuery) y #7 (searchTerm)
        // llevan comillas escapadas en la evidencia. Con el bug C1 se aplastaba a 1; debe dar 7.
        JsonArray hallazgos = new JsonArray();
        hallazgos.add(hallazgoJson("Transmisión en texto claro sobre HTTP", "Medium", "High",
            "La aplicación se sirve sobre HTTP sin cifrado.",
            "GET http://zero.webappsecurity.com/online-banking.html"));
        hallazgos.add(hallazgoJson("CORS permisivo con comodín", "Medium", "High",
            "Access-Control-Allow-Origin con comodín.", "Access-Control-Allow-Origin: *"));
        hallazgos.add(hallazgoJson("Biblioteca jQuery desactualizada y vulnerable", "Medium", "Medium",
            "jQuery 1.7.2 con XSS conocidos (CVE-2011-4969, CVE-2015-9251).",
            "<script src=\"/resources/js/jquery-1.7.2.min.js\"></script>"));
        hallazgos.add(hallazgoJson("Divulgación de versión del servidor", "Low", "High",
            "La cabecera Server revela Apache-Coyote/1.1.", "Server: Apache-Coyote/1.1"));
        hallazgos.add(hallazgoJson("Ausencia de protección contra clickjacking", "Low", "High",
            "Sin X-Frame-Options ni frame-ancestors.",
            "Sin cabecera X-Frame-Options ni Content-Security-Policy en la respuesta"));
        hallazgos.add(hallazgoJson("Falta de cabeceras de seguridad (CSP, HSTS, nosniff)", "Low", "High",
            "Faltan CSP, HSTS, X-Content-Type-Options, Referrer-Policy y Permissions-Policy.",
            "Cabeceras de respuesta sin X-Content-Type-Options, CSP, HSTS, Referrer-Policy ni Permissions-Policy"));
        hallazgos.add(hallazgoJson("Parámetro de búsqueda como posible punto de inyección", "Info", "Low",
            "El parámetro searchTerm podría reflejarse sin codificar.",
            "<input type=\"text\" id=\"searchTerm\" name=\"searchTerm\" class=\"search-query\" placeholder=\"Search\"/>"));
        JsonObject root = new JsonObject();
        root.add("hallazgos", hallazgos);

        ResultadoAnalisisMultiple resultado =
            invocarParsearRespuesta(orquestador, envolverOpenAI(new Gson().toJson(root)));

        assertEquals(7, resultado.obtenerNumeroHallazgos(),
            "Los 7 hallazgos del log real deben conservarse (el bug C1 los aplastaba a 1)");
        assertTrue(resultado.obtenerHallazgos().get(2).obtenerHallazgo()
                .contains("<script src=\"/resources/js/jquery-1.7.2.min.js\"></script>"),
            "Evidencia jQuery con comillas escapadas debe quedar íntegra");
        assertTrue(resultado.obtenerHallazgos().get(6).obtenerHallazgo()
                .contains("<input type=\"text\" id=\"searchTerm\""),
            "Evidencia searchTerm con comillas escapadas debe quedar íntegra");
    }

    @Test
    @DisplayName("#1: evidencia con secuencias backslash (rutas Windows / regex) no se corrompe")
    void testEvidenciaConBackslashNoSeCorrompe() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/path", "GET", "hash-bs");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        // \t \n \r literales en la evidencia: antes el desescape doble los convertía
        // en TAB/salto/CR y mutilaba rutas y regex.
        JsonArray hallazgos = new JsonArray();
        hallazgos.add(hallazgoJson("Path disclosure", "Low", "High",
            "Ruta de Windows expuesta", "C:\\temp\\notes\\readme.txt"));
        hallazgos.add(hallazgoJson("Regex en evidencia", "Info", "Medium",
            "Patrón observado", "regex \\d{3}\\t\\n fin"));
        JsonObject root = new JsonObject();
        root.add("hallazgos", hallazgos);

        ResultadoAnalisisMultiple resultado =
            invocarParsearRespuesta(orquestador, envolverOpenAI(new Gson().toJson(root)));

        assertEquals(2, resultado.obtenerNumeroHallazgos(), "Ambos hallazgos deben conservarse");
        assertTrue(resultado.obtenerHallazgos().get(0).obtenerHallazgo().contains("C:\\temp\\notes\\readme.txt"),
            "La ruta Windows con \\t/\\n/\\r debe quedar literal, no convertida en TAB/salto");
        assertTrue(resultado.obtenerHallazgos().get(1).obtenerHallazgo().contains("\\d{3}\\t\\n"),
            "Las secuencias de regex deben quedar literales");
    }

    @Test
    @DisplayName("H1: respuesta 'sin hallazgos' no genera hallazgos fantasma")
    void testRespuestaSinHallazgosNoGeneraFantasmas() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/clean", "GET", "hash-h1");
        OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

        String[] vacias = {"{\"hallazgos\":[]}", "{\"findings\":[]}", "{}", "[]"};
        for (String vacia : vacias) {
            ResultadoAnalisisMultiple resultado =
                invocarParsearRespuesta(orquestador, envolverOpenAI(vacia));
            assertEquals(0, resultado.obtenerNumeroHallazgos(),
                "No debe crear hallazgos fantasma para respuesta vacía explícita: " + vacia);
        }
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

    @Test
    @DisplayName("Orquestador delega multi proveedor al gestor compartido")
    void testEjecutarAnalisisCompletoDelegaMultiProveedor() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        config.establecerMultiProveedorHabilitado(true);
        config.establecerProveedoresMultiConsulta(List.of(PROVEEDOR_OPENAI, "Claude"));
        config.establecerModeloParaProveedor("Claude", "claude-sonnet-4-6");
        config.establecerUrlBaseParaProveedor("Claude", "https://api.anthropic.com/v1");
        config.establecerApiKeyParaProveedor("Claude", "sk-claude");

        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/multi", "GET", "hash-orq-multi");
        ResultadoAnalisisMultiple esperado = new ResultadoAnalisisMultiple(
            solicitud.obtenerUrl(),
            List.of(new Hallazgo(solicitud.obtenerUrl(), "Titulo", "Detalle", Hallazgo.SEVERIDAD_HIGH, Hallazgo.CONFIANZA_ALTA)),
            solicitud.obtenerSolicitudHttp(),
            List.of()
        );

        try (MockedConstruction<GestorMultiProveedor> gestorMultiMock = mockConstruction(
                GestorMultiProveedor.class,
                (mock, context) -> when(mock.ejecutarAnalisisMultiProveedor()).thenReturn(esperado))) {
            OrquestadorAnalisis orquestador = crearOrquestador(config, solicitud);

            ResultadoAnalisisMultiple resultado = orquestador.ejecutarAnalisisCompleto();

            assertEquals(esperado, resultado, "El orquestador debe reutilizar el resultado del gestor multi proveedor");
            assertEquals(1, gestorMultiMock.constructed().size(),
                "Debe construirse un único GestorMultiProveedor compartido");
            verify(gestorMultiMock.constructed().get(0)).ejecutarAnalisisMultiProveedor();
        }
    }
}
