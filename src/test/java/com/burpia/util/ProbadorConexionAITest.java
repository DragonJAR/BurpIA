package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link ProbadorConexionAI}.
 * <p>
 * Verifica la lógica de prueba de conexión con proveedores de IA,
 * incluyendo casos de éxito, error de configuración, errores HTTP y fallos de red.
 * </p>
 */
@DisplayName("ProbadorConexionAI Tests")
class ProbadorConexionAITest {

    private MockWebServer mockWebServer;
    private ConfiguracionAPI config;
    private boolean servidorActivo;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        servidorActivo = true;

        config = new ConfiguracionAPI();
        config.establecerProveedorAI(ProveedorAI.PROVEEDOR_CUSTOM_02);
        config.establecerClaveApi("test-key");
        config.establecerUrlApi(mockWebServer.url("/v1/chat/completions").toString());
        config.establecerModelo("gpt-3.5-mock");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (servidorActivo && mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    @DisplayName("Constructor lanza IllegalArgumentException con configuración null")
    void constructor_conConfigNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ProbadorConexionAI(null)
        );
        assertEquals("La configuración no puede ser null", ex.getMessage());
    }

    @Test
    @DisplayName("Retorna error cuando la configuración es inválida")
    void probarConexion_configInvalida_retornaError() {
        config.establecerProveedorAI("OpenAI");
        config.establecerClaveApi("");
        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.ERRORES_CONFIGURACION().trim()));
    }

    @Test
    @DisplayName("Retorna éxito cuando el modelo responde con OK")
    void probarConexion_respuestaExitosaConOK() {
        String jsonResponse = "{ \"choices\": [ { \"message\": { \"content\": \"OK\" } } ] }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.EXITO_CONEXION()));
        assertNotNull(resultado.respuestaRaw);
    }

    @Test
    @DisplayName("Retorna éxito cuando el modelo responde sin OK pero con contenido válido")
    void probarConexion_respuestaExitosaSinOK() {
        String jsonResponse = "{ \"choices\": [ { \"message\": { \"content\": \"Alguna otra respuesta valida\" } } ] }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.EXITO_CONEXION()));
    }

    @Test
    @DisplayName("Retorna éxito con advertencia cuando la respuesta no es parseable")
    void probarConexion_respuestaNoParseable() {
        String jsonResponse = "{ \"error\": \"bad format\" }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.EXITO_CONEXION()));
    }

    @Test
    @DisplayName("Retorna error cuando el servidor responde con error HTTP")
    void probarConexion_servidorRespondeErrorHTTP() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.ERROR_CONEXION()));
        assertTrue(resultado.mensaje.contains("HTTP 500"));
    }

    @Test
    @DisplayName("Retorna error cuando hay fallo de red")
    void probarConexion_fallaDeRed() throws IOException {
        mockWebServer.shutdown();
        servidorActivo = false;

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains(I18nUI.Conexion.ERROR_CONEXION()));
    }
}
