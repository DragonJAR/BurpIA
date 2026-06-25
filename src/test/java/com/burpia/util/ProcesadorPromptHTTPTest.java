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

    // ============ TESTS DE procesarFlujo() ============

    @Nested
    @DisplayName("procesarFlujo")
    class ProcesarFlujoTests {

        @Test
        @DisplayName("Reemplaza REQUEST y RESPONSE preservando el prompt del usuario")
        void reemplazaRequestYResponsePreservandoPromptUsuario() {
            String prompt = "Contexto del usuario\nREQ={REQUEST}\nRES={RESPONSE}\nLANG={OUTPUT_LANGUAGE}";
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("https://example.com/api1", METODO_GET, "req1", "resp1"),
                crearSolicitudSinRespuesta("https://example.com/api2", METODO_POST, "req2")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEn);

            assertTrue(resultado.startsWith("Contexto del usuario"),
                "Debe preservar intacto el texto del usuario");
            assertTrue(resultado.contains("=== REQUEST 1 ==="),
                "Debe insertar la primera request en el token REQUEST");
            assertTrue(resultado.contains("=== REQUEST 2 ==="),
                "Debe insertar la segunda request en el token REQUEST");
            assertTrue(resultado.contains("https://example.com/api1"),
                "Debe contener URL del primer request");
            assertTrue(resultado.contains("https://example.com/api2"),
                "Debe contener URL del segundo request");
            assertTrue(resultado.contains("=== RESPONSE 1 ==="),
                "Debe insertar respuestas existentes en el token RESPONSE");
            assertFalse(resultado.contains("=== RESPONSE 2 ==="),
                "No debe inventar respuestas faltantes");
            assertFalse(resultado.contains("<http_transaction"),
                "No debe reconstruir el prompt con bloques extra");
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
        @DisplayName("Agrega respuestas de flujo cuando el prompt solo contiene REQUEST")
        void agregaResponsesFallbackCuandoPromptSoloTieneRequest() {
            String prompt = "Analiza el flujo completo: {REQUEST}";
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("https://example.com/api1", METODO_GET, "req1", "resp1"),
                crearSolicitudSinRespuesta("https://example.com/api2", METODO_POST, "req2")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEn);

            assertTrue(resultado.contains("Analiza el flujo completo:"),
                "Debe preservar el texto del usuario");
            assertTrue(resultado.contains("=== REQUEST 1 ==="),
                "Debe reemplazar el marcador REQUEST existente");
            assertTrue(resultado.contains("RESPONSE:\n=== RESPONSE 1 ==="),
                "Debe agregar las respuestas disponibles cuando falta el marcador RESPONSE");
            assertFalse(resultado.contains("=== RESPONSE 2 ==="),
                "No debe inventar respuestas faltantes");
        }

        @Test
        @DisplayName("Agrega respuestas de flujo cuando el prompt usa REQUEST numerado")
        void agregaResponsesFallbackConRequestNumerado() {
            String prompt = "Paso inicial: {REQUEST_1}";
            List<SolicitudAnalisis> solicitudes = List.of(
                crearSolicitud("https://example.com/api1", METODO_GET, "req1", "resp1")
            );

            String resultado = ProcesadorPromptHTTP.procesarFlujo(prompt, solicitudes, configEn);

            assertTrue(resultado.contains("Paso inicial:"),
                "Debe preservar el texto del usuario");
            assertFalse(resultado.contains("{REQUEST_1}"),
                "Debe reemplazar el marcador numerado");
            assertTrue(resultado.contains("RESPONSE:\n=== RESPONSE 1 ==="),
                "Debe agregar respuestas aunque el marcador de request sea numerado");
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

        @Test
        @DisplayName("ConstructorPrompts preserva headers HTTP literales en el request")
        void constructorPromptsPreservaHeadersHttpLiterales() {
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerIdiomaUi("es");
            config.establecerPromptConfigurable("{REQUEST}");

            ConstructorPrompts constructor = new ConstructorPrompts(config);
            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                URL_EJEMPLO,
                METODO_GET,
                "Host: example.com\nAccept-Language: es-419,es;q=0.9\nUser-Agent: Mozilla/5.0",
                null,
                HASH_TEST,
                null,
                200,
                "Content-Type: text/html",
                "<html></html>"
            );

            String prompt = constructor.construirPromptAnalisis(solicitud);

            assertTrue(prompt.contains("Accept-Language: es-419,es;q=0.9"),
                "Debe mantener Accept-Language literal en el prompt");
            assertTrue(prompt.contains("User-Agent: Mozilla/5.0"),
                "Debe mantener User-Agent literal en el prompt");
            assertTrue(prompt.contains("Content-Type: text/html"),
                "Debe mantener Content-Type literal en el prompt");
            assertFalse(prompt.contains("Accept-Idioma"),
                "No debe traducir nombres de headers en el prompt");
            assertFalse(prompt.contains("User-Agente"),
                "No debe traducir nombres de headers en el prompt");
            assertFalse(prompt.contains("Content-Tipo"),
                "No debe traducir nombres de headers en el prompt");
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

    private SolicitudAnalisis crearSolicitudSinRespuesta(String url, String metodo, String cuerpo) {
        return new SolicitudAnalisis(
            url,
            metodo,
            "Host: example.com",
            cuerpo,
            HASH_TEST,
            null,
            -1,
            "",
            ""
        );
    }
}
