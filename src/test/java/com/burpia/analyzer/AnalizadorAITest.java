package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AnalizadorAI Tests")
class AnalizadorAITest {

    private static final String PROVEEDOR_ZAI = "Z.ai";
    private static final String PROVEEDOR_OPENAI = "OpenAI";
    private static final String MODELO_GLM5 = "glm-5";

    /**
     * Callback vacío reutilizable para tests que no necesitan verificar callbacks.
     * Principio DRY: Evita duplicar implementación de callback vacío en cada test.
     */
    private static final AnalizadorAI.Callback CALLBACK_VACIO = new AnalizadorAI.Callback() {
        @Override
        public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
        }

        @Override
        public void alErrorAnalisis(String error) {
        }
    };

    /**
     * Crea una configuración básica con el proveedor especificado.
     * Principio DRY: Centraliza creación de configuración de test.
     */
    private ConfiguracionAPI crearConfiguracionBasica(String proveedor) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI(proveedor);
        return config;
    }

    /**
     * Crea una solicitud de análisis básica para tests.
     * Principio DRY: Centraliza creación de solicitudes de test.
     */
    private SolicitudAnalisis crearSolicitudBasica(String url, String metodo, String hash) {
        return new SolicitudAnalisis(url, metodo, "", "", hash);
    }

    /**
     * Crea un AnalizadorAI con configuración estándar para tests de parsing.
     * Principio DRY: Centraliza creación de analizadores de test.
     */
    private AnalizadorAI crearAnalizadorParaTest(ConfiguracionAPI config, SolicitudAnalisis solicitud) {
        return new AnalizadorAI(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            CALLBACK_VACIO
        );
    }

    /**
     * Invoca el método privado parsearRespuesta mediante reflexión.
     * Principio DRY: Centraliza acceso reflexivo al método de parsing.
     */
    private ResultadoAnalisisMultiple invocarParsearRespuesta(AnalizadorAI analizador, String respuesta)
            throws Exception {
        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);
        return (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);
    }

    @Test
    @DisplayName("Parseo de hallazgos tolera elementos JSON inválidos sin romper")
    void testParseoHallazgosInvalidosNoRompe() throws Exception {
        ConfiguracionAPI config = crearConfiguracionBasica(PROVEEDOR_ZAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com", "GET", "hash-test");
        AnalizadorAI analizador = crearAnalizadorParaTest(config, solicitud);

        String respuesta = "{\"hallazgos\":[123,{\"descripcion\":\"SQLi detectada\",\"severidad\":\"High\",\"confianza\":\"Medium\"},{\"descripcion\":{},\"severidad\":{},\"confianza\":{}}]}";
        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(analizador, respuesta);

        assertTrue(resultado.obtenerNumeroHallazgos() >= 1, "Debe haber al menos 1 hallazgo válido");
        assertEquals("Sin título", resultado.obtenerHallazgos().get(0).obtenerTitulo(), "assertEquals failed at AnalizadorAITest.java:93");
        assertEquals("SQLi detectada", resultado.obtenerHallazgos().get(0).obtenerHallazgo(), "assertEquals failed at AnalizadorAITest.java:94");
    }

    @Test
    @DisplayName("Fallback no estricto recupera hallazgos cuando evidencia trae comillas sin escapar")
    void testParseoNoEstrictoConComillasSinEscaparEnEvidencia() throws Exception {
        ConfiguracionAPI config = crearConfiguracionBasica(PROVEEDOR_ZAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com", "POST", "hash-nonstrict");
        AnalizadorAI analizador = crearAnalizadorParaTest(config, solicitud);

        String respuesta = "{\"hallazgos\":["
            + "{\"descripcion\":\"HTTP sin cifrado\",\"severidad\":\"Medium\",\"confianza\":\"High\",\"evidencia\":\"POST http://example.com\"},"
            + "{\"descripcion\":\"Falta token CSRF\",\"severidad\":\"Low\",\"confianza\":\"Low\",\"evidencia\":\"<form action=\"search.php\" method=\"post\">\"}"
            + "]}";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(analizador, respuesta);

        assertEquals(2, resultado.obtenerNumeroHallazgos(), "Debe recuperar 2 hallazgos");
        assertTrue(resultado.obtenerHallazgos().get(1).obtenerHallazgo().contains("Evidencia: <form action=\"search.php\" method=\"post\">"),
            "La evidencia debe contener el HTML del formulario");
    }

    @Test
    @DisplayName("Fallback no estricto extrae el titulo correctamente")
    void testParseoNoEstrictoExtraeTitulo() throws Exception {
        ConfiguracionAPI config = crearConfiguracionBasica(PROVEEDOR_ZAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/api", "PUT", "hash-title-extract");
        AnalizadorAI analizador = crearAnalizadorParaTest(config, solicitud);

        String respuesta = "{\"hallazgos\":["
            + "{\"titulo\":\"Inyeccion SQL Detectada\",\"descripcion\":\"Se encontro una inyeccion SQL...\",\"severidad\":\"Critical\",\"confianza\":\"High\"}"
            + "]}";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(analizador, respuesta);

        assertEquals(1, resultado.obtenerNumeroHallazgos(), "Debe haber exactamente 1 hallazgo");
        assertEquals("Inyeccion SQL Detectada", resultado.obtenerHallazgos().get(0).obtenerTitulo(), "assertEquals failed at AnalizadorAITest.java:130");
    }

    @Test
    @DisplayName("Fallback no estricto soporta clave findings con campos en inglés")
    void testParseoNoEstrictoConFindingsEnIngles() throws Exception {
        ConfiguracionAPI config = crearConfiguracionBasica(PROVEEDOR_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/api", "GET", "hash-findings-en");
        AnalizadorAI analizador = crearAnalizadorParaTest(config, solicitud);

        String respuesta = "{\"findings\":["
            + "{\"title\":\"Missing HSTS\",\"description\":\"No Strict-Transport-Security header\",\"severity\":\"Medium\",\"confidence\":\"High\",\"evidence\":\"Strict-Transport-Security absent\"}"
            + "]} trailing";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(analizador, respuesta);

        assertEquals(1, resultado.obtenerNumeroHallazgos(), "Debe haber exactamente 1 hallazgo");
        assertEquals("Missing HSTS", resultado.obtenerHallazgos().get(0).obtenerTitulo(), "assertEquals failed at AnalizadorAITest.java:147");
        assertEquals("Medium", resultado.obtenerHallazgos().get(0).obtenerSeveridad(), "assertEquals failed at AnalizadorAITest.java:148");
        assertEquals("High", resultado.obtenerHallazgos().get(0).obtenerConfianza(), "assertEquals failed at AnalizadorAITest.java:149");
    }

    @Test
    @DisplayName("Constructor tolera dependencias nulas sin lanzar excepcion")
    void testConstructorConDependenciasNulasNoRompe() {
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com", "GET", "hash-test");

        assertDoesNotThrow(() -> new AnalizadorAI(
            solicitud,
            null,
            null,
            null,
            null,
            null
        ), "El constructor no debe lanzar excepción con dependencias nulas");
    }

    @Test
    @DisplayName("AnalizadorHTTP resuelve cliente HTTP según la configuración actual")
    void testResuelveClienteHttpSegunConfiguracionActual() throws Exception {
        ConfiguracionAPI config = crearConfiguracionBasica(PROVEEDOR_ZAI);
        config.establecerModeloParaProveedor(PROVEEDOR_ZAI, MODELO_GLM5);
        config.establecerTiempoEsperaAI(60);
        config.establecerTiempoEsperaParaModelo(PROVEEDOR_ZAI, MODELO_GLM5, 180);

        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com", "GET", "hash-timeout");
        AnalizadorAI analizador = crearAnalizadorParaTest(config, solicitud);

        // Acceder al campo analizadorHTTP mediante reflexión
        Field campoAnalizadorHTTP = AnalizadorAI.class.getDeclaredField("analizadorHTTP");
        campoAnalizadorHTTP.setAccessible(true);
        Object analizadorHTTP = campoAnalizadorHTTP.get(analizador);

        // Acceder al método obtenerClienteHttp del AnalizadorHTTP
        Class<?> claseAnalizadorHTTP = Class.forName("com.burpia.analyzer.AnalizadorHTTP");
        Method metodoObtener = claseAnalizadorHTTP.getDeclaredMethod("obtenerClienteHttp");
        metodoObtener.setAccessible(true);
        okhttp3.OkHttpClient clienteInicial = (okhttp3.OkHttpClient) metodoObtener.invoke(analizadorHTTP);
        assertEquals(180_000, clienteInicial.readTimeoutMillis(),
            "El timeout inicial debe respetar la configuración del modelo");

        ConfiguracionAPI configActualizada = crearConfiguracionBasica(PROVEEDOR_OPENAI);
        configActualizada.establecerModeloParaProveedor(PROVEEDOR_OPENAI, "gpt-5-mini");
        configActualizada.establecerTiempoEsperaAI(45);
        configActualizada.establecerTiempoEsperaParaModelo(PROVEEDOR_OPENAI, "gpt-5-mini", 45);

        // Actualizar el campo config del analizadorHTTP
        Field campoConfig = claseAnalizadorHTTP.getDeclaredField("config");
        campoConfig.setAccessible(true);
        campoConfig.set(analizadorHTTP, configActualizada);

        okhttp3.OkHttpClient clienteActualizado = (okhttp3.OkHttpClient) metodoObtener.invoke(analizadorHTTP);
        assertEquals(45_000, clienteActualizado.readTimeoutMillis(),
            "El timeout debe recalcularse si cambia la configuración efectiva");
    }
}
