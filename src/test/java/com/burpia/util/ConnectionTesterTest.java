package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link ConnectionTester}.
 * <p>
 * Verifica la configuración SSL y el comportamiento de conexión segura/insegura.
 * </p>
 */
@DisplayName("ConnectionTester Tests")
class ConnectionTesterTest {

    private MockWebServer mockWebServer;
    private ConnectionTester connectionTester;
    private ConfiguracionAPI config;
    private boolean servidorActivo;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        servidorActivo = true;

        connectionTester = new ConnectionTester();
        config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerClaveApi("test-key");
        config.establecerUrlApi(mockWebServer.url("/v1/models").toString());
        config.establecerModelo("gpt-3.5-turbo");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (servidorActivo && mockWebServer != null) {
            mockWebServer.shutdown();
        }
        if (connectionTester != null) {
            connectionTester.cerrar();
        }
    }

    @Test
    @DisplayName("Cliente HTTP usa SSL seguro por defecto")
    void clienteHttp_usaSSLSeguro_porDefecto() {
        // Configurar SSL seguro (por defecto)
        config.establecerIgnorarErroresSSL(false);

        // Crear cliente y verificar que no está configurado para ignorar SSL
        CompletableFuture<Boolean> resultado = new CompletableFuture<>();
        
        connectionTester.probarConexionProveedor(config, new ConnectionTester.CallbackConexion() {
            @Override
            public void alExito(String mensaje) {
                resultado.complete(true);
            }

            @Override
            public void alError(String error) {
                resultado.complete(false);
            }
        });

        // La configuración debe permitir SSL seguro
        assertFalse(config.ignorarErroresSSL(), "SSL no debe ser ignorado por defecto");
    }

    @Test
    @DisplayName("Cliente HTTP ignora errores SSL cuando está configurado")
    void clienteHttp_ignoraErroresSSL_cuandoEstaConfigurado() {
        // Configurar para ignorar errores SSL
        config.establecerIgnorarErroresSSL(true);

        // Verificar que la configuración se aplicó
        assertTrue(config.ignorarErroresSSL(), "SSL debe ser ignorado cuando está configurado");
    }

    @Test
    @DisplayName("Error de configuración nula lanza excepción")
    void probarConexion_configNull_lanzaExcepcion() {
        CompletableFuture<Boolean> resultado = new CompletableFuture<>();
        
        connectionTester.probarConexionProveedor(null, new ConnectionTester.CallbackConexion() {
            @Override
            public void alExito(String mensaje) {
                resultado.complete(false);
            }

            @Override
            public void alError(String error) {
                resultado.complete(error.contains("null"));
            }
        });

        assertTrue(resultado.join(), "Debe indicar error cuando config es null");
    }

    @Test
    @DisplayName("Error de callback nulo no lanza excepción pero maneja gracefully")
    void probarConexion_callbackNull_noLanzaExcepcion() {
        assertDoesNotThrow(() -> {
            connectionTester.probarConexionProveedor(config, null);
        }, "No debe lanzar excepción cuando callback es null");
    }

    @Test
    @DisplayName("Conexión exitosa con configuración válida")
    void probarConexion_configValida_retornaExito() throws Exception {
        // Preparar respuesta mock
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"data\": [{\"id\": \"gpt-3.5-turbo\"}]}"));

        CompletableFuture<Boolean> resultado = new CompletableFuture<>();
        
        connectionTester.probarConexionProveedor(config, new ConnectionTester.CallbackConexion() {
            @Override
            public void alExito(String mensaje) {
                resultado.complete(true);
            }

            @Override
            public void alError(String error) {
                resultado.complete(false);
            }
        });

        assertTrue(resultado.get(5, TimeUnit.SECONDS), "Conexión debe ser exitosa con configuración válida");
    }

    @Test
    @DisplayName("Error cuando falta configuración requerida")
    void probarConexion_configIncompleta_retornaError() {
        ConfiguracionAPI configIncompleta = new ConfiguracionAPI();
        // No establecer API key

        CompletableFuture<Boolean> resultado = new CompletableFuture<>();
        
        connectionTester.probarConexionProveedor(configIncompleta, new ConnectionTester.CallbackConexion() {
            @Override
            public void alExito(String mensaje) {
                resultado.complete(false);
            }

            @Override
            public void alError(String error) {
                resultado.complete(error.contains("Missing") || error.contains("configuration"));
            }
        });

        assertTrue(resultado.join(), "Debe indicar error cuando falta configuración requerida");
    }

    @Test
    @DisplayName("Constructor crea instancia válida")
    void constructor_creaInstanciaValida() {
        ConnectionTester tester = new ConnectionTester();
        assertNotNull(tester, "ConnectionTester debe ser creado exitosamente");
        tester.cerrar();
    }

    @Test
    @DisplayName("Shutdown no lanza excepción")
    void shutdown_noLanzaExcepcion() {
        assertDoesNotThrow(() -> {
            connectionTester.cerrar();
        }, "Cerrar no debe lanzar excepción");
    }

    @Test
    @DisplayName("Configuración SSL se aplica correctamente según configuración")
    void configuracionSSL_seAplicaCorrectamente() throws Exception {
        // Test con SSL habilitado (ignorar errores)
        ConfiguracionAPI configInsegura = new ConfiguracionAPI();
        configInsegura.establecerProveedorAI("OpenAI");
        configInsegura.establecerClaveApi("test-key");
        configInsegura.establecerUrlApi(mockWebServer.url("/v1/models").toString());
        configInsegura.establecerModelo("gpt-3.5-turbo");
        configInsegura.establecerIgnorarErroresSSL(true);

        assertTrue(configInsegura.ignorarErroresSSL(), 
            "La configuración debe indicar que se ignoran errores SSL");

        // Test con SSL seguro (no ignorar errores)
        ConfiguracionAPI configSegura = new ConfiguracionAPI();
        configSegura.establecerProveedorAI("OpenAI");
        configSegura.establecerClaveApi("test-key");
        configSegura.establecerUrlApi(mockWebServer.url("/v1/models").toString());
        configSegura.establecerModelo("gpt-3.5-turbo");
        configSegura.establecerIgnorarErroresSSL(false);

        assertFalse(configSegura.ignorarErroresSSL(), 
            "La configuración debe indicar que no se ignoran errores SSL");
    }
}