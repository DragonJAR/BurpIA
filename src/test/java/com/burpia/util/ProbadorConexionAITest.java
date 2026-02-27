package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ProbadorConexionAITest {

    private MockWebServer mockWebServer;
    private ConfiguracionAPI config;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        config = new ConfiguracionAPI();
        config.establecerProveedorAI(ProveedorAI.PROVEEDOR_CUSTOM);
        config.establecerClaveApi("test-key");
        config.establecerUrlApi(mockWebServer.url("/v1/chat/completions").toString());
        config.establecerModelo("gpt-3.5-mock");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void probarConexion_configInvalida_retornaError() {
        config.establecerProveedorAI("OpenAI");
        config.establecerClaveApi("");
        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("Errores de conf"));
    }

    @Test
    void probarConexion_respuestaExitosaConOK() {
        String jsonResponse = "{ \"choices\": [ { \"message\": { \"content\": \"OK\" } } ] }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains("✅ Conexión exitosa a"));
        assertTrue(resultado.mensaje.contains("¡El modelo respondió correctamente!"));
    }

    @Test
    void probarConexion_respuestaExitosaSinOK() {
        String jsonResponse = "{ \"choices\": [ { \"message\": { \"content\": \"Alguna otra respuesta valida\" } } ] }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains("✅ El proveedor respondió y el contenido fue extraído"));
        assertTrue(resultado.mensaje.contains("La respuesta no incluyó literalmente \"OK\", pero la conexión es valida."));
    }

    @Test
    void probarConexion_respuestaNoParseable() {
        String jsonResponse = "{ \"error\": \"bad format\" }";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertTrue(resultado.exito);
        assertTrue(resultado.mensaje.contains("⚠️ No se pudo extraer el contenido"));
    }

    @Test
    void probarConexion_servidorRespondeErrorHTTP() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("Error de conexión: "));
        assertTrue(resultado.mensaje.contains("HTTP 500"));
    }

    @Test
    void probarConexion_fallaDeRed() throws IOException {
        mockWebServer.shutdown();

        ProbadorConexionAI probador = new ProbadorConexionAI(config);
        ProbadorConexionAI.ResultadoPrueba resultado = probador.probarConexion();

        assertFalse(resultado.exito);
        assertTrue(resultado.mensaje.contains("Error de conexión: "));
    }
}
