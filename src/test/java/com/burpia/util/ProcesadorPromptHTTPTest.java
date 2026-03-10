package com.burpia.util;

import com.burpia.analyzer.ConstructorPrompts;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para ProcesadorPromptHTTP.
 * 
 * <p>Valida el procesamiento de variables dinamicas en prompts:
 * - {REQUEST}, {RESPONSE}, {OUTPUT_LANGUAGE}
 * - {REQUEST_N}, {RESPONSE_N} para flujos
 * - Deteccion automatica de modo (SINGLE, REQUEST_ONLY)
 * </p>
 * 
 * @see ProcesadorPromptHTTP
 */
@DisplayName("ProcesadorPromptHTTP Tests")
class ProcesadorPromptHTTPTest {

    private static final int MINIMO_LONGITUD_PROMPT_DEFAULT = 100;
    private static final int TAMANO_CUERPO_GRANDE = 15000;
    private static final String HASH_TEST = "hash-test-123";
    private static final String PROMPT_VACIO = "";
    private static final String URL_EJEMPLO = "https://example.com/api/test";
    private static final String METODO_GET = "GET";
    private static final String METODO_POST = "POST";

    private ConfiguracionAPI configEn;
    private ConfiguracionAPI configEs;

    @BeforeEach
    void setUp() {
        configEn = new ConfiguracionAPI();
        configEn.establecerIdiomaUi("en");

        configEs = new ConfiguracionAPI();
        configEs.establecerIdiomaUi("es");
    }

    // ============ TESTS DE procesarIndividual() ============

    @Nested
    @DisplayName("procesarIndividual")
    class ProcesarIndividualTests {

        @Test
        @DisplayName("Reemplaza {REQUEST} con contenido HTTP")
        void reemplazaRequestCorrectamente() {
            String prompt = "Analyze: {REQUEST}";
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("GET https://example.com/api/test HTTP/1.1"),
                "Debe contener la linea de request HTTP");
            assertTrue(resultado.contains("Host: example.com"),
                "Debe contener los headers del request");
            assertFalse(resultado.contains("{REQUEST}"),
                "No debe contener el marcador {REQUEST} sin reemplazar");
        }

        @Test
        @DisplayName("Reemplaza {RESPONSE} con contenido HTTP")
        void reemplazaResponseCorrectamente() {
            String prompt = "Response: {RESPONSE}";
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("HTTP/1.1 200"),
                "Debe contener la linea de status del response");
            assertTrue(resultado.contains("{\"error\":null"),
                "Debe contener el body del response");
            assertFalse(resultado.contains("{RESPONSE}"),
                "No debe contener el marcador {RESPONSE} sin reemplazar");
        }

        @Test
        @DisplayName("Reemplaza {OUTPUT_LANGUAGE} con English para config en ingles")
        void reemplazaIdiomaIngles() {
            String prompt = "Lang: {OUTPUT_LANGUAGE}";
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("Lang: English"),
                "Debe reemplazar con 'English' para config en inglés");
            assertFalse(resultado.contains("{OUTPUT_LANGUAGE}"),
                "No debe contener el marcador {OUTPUT_LANGUAGE} sin reemplazar");
        }

        @Test
        @DisplayName("Reemplaza {OUTPUT_LANGUAGE} con Spanish para config en espanol")
        void reemplazaIdiomaEspanol() {
            String prompt = "Lang: {OUTPUT_LANGUAGE}";
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEs);

            assertTrue(resultado.contains("Lang: Spanish"),
                "Debe reemplazar con 'Spanish' para config en español");
            assertFalse(resultado.contains("{OUTPUT_LANGUAGE}"),
                "No debe contener el marcador {OUTPUT_LANGUAGE} sin reemplazar");
        }

        @Test
        @DisplayName("Usa prompt por defecto cuando promptBase es vacio")
        void usaPromptPorDefectoCuandoVacio() {
            String resultado = ProcesadorPromptHTTP.procesarIndividual(
                PROMPT_VACIO, 
                crearSolicitudConResponse(), 
                configEn
            );

            assertNotNull(resultado, "El resultado no debe ser null");
            assertTrue(resultado.length() > MINIMO_LONGITUD_PROMPT_DEFAULT,
                "Debe tener contenido sustancial del prompt por defecto (>" + MINIMO_LONGITUD_PROMPT_DEFAULT + " chars)");
            assertFalse(resultado.contains("{REQUEST}"),
                "Debe reemplazar REQUEST en el prompt por defecto");
        }

        @Test
        @DisplayName("Usa prompt por defecto cuando promptBase es null")
        void usaPromptPorDefectoCuandoNull() {
            String resultado = ProcesadorPromptHTTP.procesarIndividual(
                null, 
                crearSolicitudConResponse(), 
                configEn
            );

            assertNotNull(resultado, "El resultado no debe ser null");
            assertTrue(resultado.length() > MINIMO_LONGITUD_PROMPT_DEFAULT,
                "Debe tener contenido sustancial del prompt por defecto");
        }

        @Test
        @DisplayName("NO modifica texto del usuario fuera de variables")
        void noModificaTextoDelUsuario() {
            String prompt = "You are a security expert. Analyze this: {REQUEST}";
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("You are a security expert. Analyze this:"),
                "Debe preservar el texto del usuario exactamente");
            assertTrue(resultado.contains("example.com"),
                "Debe incluir datos HTTP del request");
        }

        @Test
        @DisplayName("Maneja solicitud sin body correctamente")
        void manejaSolicitudSinBody() {
            String prompt = "{REQUEST}";
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "https://example.com/",
                METODO_GET,
                "Host: example.com",
                null,
                HASH_TEST,
                null,
                200,
                "Content-Type: text/html",
                "{\"ok\":true}"
            );

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("GET https://example.com/ HTTP/1.1"),
                "Debe contener la linea de request");
            assertFalse(resultado.contains("null"),
                "No debe contener la palabra 'null' literal");
        }

        @Test
        @DisplayName("Trunca cuerpos muy grandes")
        void truncaCuerposGrandes() {
            String prompt = "{RESPONSE}";
            String responseGrande = "x".repeat(TAMANO_CUERPO_GRANDE);
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "https://example.com/",
                METODO_POST,
                "Host: example.com",
                null,
                HASH_TEST,
                null,
                200,
                "Content-Type: application/json",
                responseGrande
            );

            String resultado = ProcesadorPromptHTTP.procesarIndividual(prompt, solicitud, configEn);

            assertTrue(resultado.contains("[TRUNCATED"),
                "Debe contener indicador de truncamiento para cuerpos > 10KB");
        }

        @Test
        @DisplayName("Lanza excepcion cuando solicitud es null")
        void lanzaExcepcionCuandoSolicitudNull() {
            IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ProcesadorPromptHTTP.procesarIndividual("prompt", null, configEn)
            );

            assertEquals("La solicitud no puede ser null", excepcion.getMessage(),
                "Debe lanzar excepcion con mensaje descriptivo");
        }

        @Test
        @DisplayName("Lanza excepcion cuando config es null")
        void lanzaExcepcionCuandoConfigNull() {
            IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ProcesadorPromptHTTP.procesarIndividual("prompt", crearSolicitudConResponse(), null)
            );

            assertEquals("La configuración no puede ser null", excepcion.getMessage(),
                "Debe lanzar excepcion con mensaje descriptivo");
        }
    }

    // ============ TESTS DE detectarModo() ============

    @Nested
    @DisplayName("detectarModo")
    class DetectarModoTests {

        @Test
        @DisplayName("Retorna SINGLE cuando hay response")
        void detectaModoSingle() {
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String modo = ProcesadorPromptHTTP.detectarModo(solicitud);

            assertEquals("SINGLE", modo,
                "Debe detectar modo SINGLE cuando hay response con codigo de estado");
        }

        @Test
        @DisplayName("Retorna REQUEST_ONLY cuando no hay response")
        void detectaModoRequestOnly() {
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "https://example.com/api",
                METODO_POST,
                "Content-Type: application/json",
                "{\"data\":\"test\"}",
                HASH_TEST,
                null,
                0,
                "",
                ""
            );

            String modo = ProcesadorPromptHTTP.detectarModo(solicitud);

            assertEquals("REQUEST_ONLY", modo,
                "Debe detectar modo REQUEST_ONLY cuando codigo estado es 0 y sin headers/body response");
        }

        @Test
        @DisplayName("Retorna REQUEST_ONLY cuando codigo de estado es 0")
        void detectaModoRequestOnlyCuandoCodigoCero() {
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "https://example.com/api",
                METODO_GET,
                "Host: example.com",
                null,
                HASH_TEST,
                null,
                0,
                "",
                ""
            );

            String modo = ProcesadorPromptHTTP.detectarModo(solicitud);

            assertEquals("REQUEST_ONLY", modo,
                "Debe detectar modo REQUEST_ONLY cuando codigo estado es 0");
        }

        @Test
        @DisplayName("Retorna SINGLE cuando hay headers de response")
        void detectaModoSingleCuandoHayHeadersResponse() {
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "https://example.com/api",
                METODO_GET,
                "Host: example.com",
                null,
                HASH_TEST,
                null,
                0,
                "Content-Type: application/json",
                null
            );

            String modo = ProcesadorPromptHTTP.detectarModo(solicitud);

            assertEquals("SINGLE", modo,
                "Debe detectar modo SINGLE cuando hay headers de response aunque codigo sea 0");
        }

        @Test
        @DisplayName("Lanza excepcion cuando solicitud es null")
        void lanzaExcepcionCuandoSolicitudNull() {
            IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ProcesadorPromptHTTP.detectarModo(null)
            );

            assertEquals("La solicitud no puede ser null", excepcion.getMessage(),
                "Debe lanzar excepcion con mensaje descriptivo");
        }
    }

    // ============ TESTS DE procesarFlujo() ============

    @Nested
    @DisplayName("procesarFlujo")
    class ProcesarFlujoTests {

        @Test
        @DisplayName("Construye bloques de transaccion automaticos")
        void construyeBloquesTransaccion() {
            String prompt = PROMPT_VACIO;
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("https://example.com/api1", METODO_GET, "req1", "resp1"),
                crearSolicitud("https://example.com/api2", METODO_POST, "req2", "resp2")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEn);

            assertTrue(resultado.contains("<http_transaction id=\"1\">"),
                "Debe contener bloque de transaccion 1");
            assertTrue(resultado.contains("<http_transaction id=\"2\">"),
                "Debe contener bloque de transaccion 2");
            assertTrue(resultado.contains("example.com/api1"),
                "Debe contener URL del primer request");
            assertTrue(resultado.contains("example.com/api2"),
                "Debe contener URL del segundo request");
        }

        @Test
        @DisplayName("Reemplaza marcadores numerados {REQUEST_N} y {RESPONSE_N}")
        void reemplazaMarcadoresNumerados() {
            String prompt = "Step 1: {REQUEST_1}\nStep 2: {REQUEST_2}\nResponses: {RESPONSE_1} | {RESPONSE_2}";
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("url1", METODO_GET, "body1", "resp1"),
                crearSolicitud("url2", METODO_POST, "body2", "resp2")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEn);

            assertTrue(resultado.contains("Step 1:"),
                "Debe preservar texto del usuario para Step 1");
            assertTrue(resultado.contains("url1"),
                "Debe reemplazar REQUEST_1 con datos de la primera solicitud");
            assertTrue(resultado.contains("Step 2:"),
                "Debe preservar texto del usuario para Step 2");
            assertTrue(resultado.contains("url2"),
                "Debe reemplazar REQUEST_2 con datos de la segunda solicitud");
            assertFalse(resultado.contains("{REQUEST_1}"),
                "No debe contener marcador REQUEST_1 sin reemplazar");
            assertFalse(resultado.contains("{REQUEST_2}"),
                "No debe contener marcador REQUEST_2 sin reemplazar");
        }

        @Test
        @DisplayName("Usa prompt por defecto cuando vacio")
        void usaPromptPorDefecto() {
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("url", METODO_GET, "body", "resp")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(PROMPT_VACIO, solicitudes, configEn);

            assertNotNull(resultado, "El resultado no debe ser null");
            assertTrue(resultado.length() > MINIMO_LONGITUD_PROMPT_DEFAULT,
                "Debe tener contenido sustancial del prompt por defecto");
        }

        @Test
        @DisplayName("Maneja lista vacia")
        void manejaListaVacia() {
            String prompt = "Analyze: {REQUEST}. Language: {OUTPUT_LANGUAGE}";

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, Collections.emptyList(), configEn);

            assertNotNull(resultado, "Debe retornar resultado no null para lista vacia");
            assertTrue(resultado.contains("English"),
                "Debe reemplazar OUTPUT_LANGUAGE con English aunque la lista este vacia");
            assertFalse(resultado.contains("{OUTPUT_LANGUAGE}"),
                "No debe contener el marcador {OUTPUT_LANGUAGE} sin reemplazar");
            assertTrue(resultado.contains("Analyze: {REQUEST}"),
                "Debe preservar marcadores de request cuando no hay solicitudes para reemplazar");
        }

        @Test
        @DisplayName("Reemplaza OUTPUT_LANGUAGE en flujo")
        void reemplazaIdiomaEnFlujo() {
            String prompt = "Language: {OUTPUT_LANGUAGE}";
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("url", METODO_GET, "body", "resp")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEs);

            assertTrue(resultado.contains("Language: Spanish"),
                "Debe reemplazar OUTPUT_LANGUAGE con Spanish para config en español");
            assertFalse(resultado.contains("{OUTPUT_LANGUAGE}"),
                "No debe contener marcador OUTPUT_LANGUAGE sin reemplazar");
        }

        @Test
        @DisplayName("Lanza excepcion cuando lista de solicitudes es null")
        void lanzaExcepcionCuandoListaNull() {
            IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ProcesadorPromptHTTP.procesarFlujo("prompt", null, configEn)
            );

            assertEquals("La lista de solicitudes no puede ser null", excepcion.getMessage(),
                "Debe lanzar excepcion con mensaje descriptivo");
        }

        @Test
        @DisplayName("Lanza excepcion cuando config es null")
        void lanzaExcepcionCuandoConfigNull() {
            List<SolicitudAnalisis> solicitudes = List.of(crearSolicitudConResponse());

            IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ProcesadorPromptHTTP.procesarFlujo("prompt", solicitudes, null)
            );

            assertEquals("La configuración no puede ser null", excepcion.getMessage(),
                "Debe lanzar excepcion con mensaje descriptivo");
        }
    }

    // ============ TESTS DE INTEGRACION CON ConstructorPrompts ============

    @Nested
    @DisplayName("Integracion con ConstructorPrompts")
    class IntegracionTests {

        @Test
        @DisplayName("ConstructorPrompts construye prompt correctamente")
        void constructorPromptsConstruyePrompt() {
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerIdiomaUi("en");
            config.establecerPromptConfigurable("Analyze: {REQUEST}");

            ConstructorPrompts constructor = new ConstructorPrompts(config);
            SolicitudAnalisis solicitud = crearSolicitudConResponse();

            String prompt = constructor.construirPromptAnalisis(solicitud);

            assertTrue(prompt.contains("Analyze:"),
                "Debe contener el texto del prompt del usuario");
            assertTrue(prompt.contains("example.com"),
                "Debe contener datos HTTP del request");
        }
    }

    // ============ METODOS AUXILIARES ============

    private SolicitudAnalisis crearSolicitudConResponse() {
        return new SolicitudAnalisis(
            URL_EJEMPLO,
            METODO_GET,
            "Host: example.com\nAuthorization: Bearer token123",
            null,
            HASH_TEST,
            null,
            200,
            "Content-Type: application/json",
            "{\"error\":null,\"data\":{\"id\":1}}"
        );
    }

    private SolicitudAnalisis crearSolicitud(String url, String metodo, String cuerpo, String response) {
        return new SolicitudAnalisis(
            url,
            metodo,
            "Host: example.com",
            cuerpo,
            HASH_TEST,
            null,
            200,
            "Content-Type: text/html",
            response
        );
    }
}
