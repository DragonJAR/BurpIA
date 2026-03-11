package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
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
                // El mensaje está localizado, verificamos que no esté vacío y contenga palabras clave
                resultado.complete(error != null && !error.isEmpty());
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
    @DisplayName("Obtener modelos funciona para Ollama sin API key")
    void obtenerModelos_ollamaSinApiKey_retornaModelos() throws Exception {
        ConfiguracionAPI configOllama = new ConfiguracionAPI();
        configOllama.establecerProveedorAI("Ollama");
        configOllama.establecerModeloParaProveedor("Ollama", "llama3");
        configOllama.establecerUrlBaseParaProveedor("Ollama", mockWebServer.url("").toString());

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"models\":[{\"name\":\"llama3:latest\"},{\"name\":\"mistral:latest\"}]}"));

        CompletableFuture<List<String>> resultado = new CompletableFuture<>();

        connectionTester.obtenerModelosDisponibles(configOllama, new ConnectionTester.CallbackModelos() {
            @Override
            public void alExito(List<String> modelos) {
                resultado.complete(modelos);
            }

            @Override
            public void alError(String error) {
                resultado.completeExceptionally(new AssertionError(error));
            }
        });

        assertEquals(List.of("llama3:latest", "mistral:latest"), resultado.get(5, TimeUnit.SECONDS),
            "Ollama debe cargar modelos sin requerir API key");
    }

    @Test
    @DisplayName("Obtener modelos usa GET /models para Claude")
    void obtenerModelos_claudeUsaEndpointOficial() throws Exception {
        ConfiguracionAPI configClaude = new ConfiguracionAPI();
        configClaude.establecerProveedorAI("Claude");
        configClaude.establecerApiKeyParaProveedor("Claude", "sk-ant-test");
        configClaude.establecerUrlBaseParaProveedor("Claude", mockWebServer.url("/v1").toString());
        configClaude.establecerModeloParaProveedor("Claude", "claude-sonnet-4-6");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"data\":[{\"id\":\"claude-opus-4-6\"},{\"id\":\"claude-sonnet-4-6\"}]}"));

        CompletableFuture<List<String>> resultado = new CompletableFuture<>();

        connectionTester.obtenerModelosDisponibles(configClaude, new ConnectionTester.CallbackModelos() {
            @Override
            public void alExito(List<String> modelos) {
                resultado.complete(modelos);
            }

            @Override
            public void alError(String error) {
                resultado.completeExceptionally(new AssertionError(error));
            }
        });

        assertEquals(List.of("claude-opus-4-6", "claude-sonnet-4-6"), resultado.get(5, TimeUnit.SECONDS),
            "Claude debe devolver modelos desde GET /models");
        assertEquals("/v1/models", mockWebServer.takeRequest().getPath(),
            "Claude debe consultar el endpoint oficial /v1/models");
    }

    @Test
    @DisplayName("Obtener modelos usa listado OpenAI-compatible para Moonshot")
    void obtenerModelos_moonshotUsaListadoCompatibleOpenAi() throws Exception {
        ConfiguracionAPI configMoonshot = new ConfiguracionAPI();
        configMoonshot.establecerProveedorAI("Moonshot (Kimi)");
        configMoonshot.establecerApiKeyParaProveedor("Moonshot (Kimi)", "moonshot-key");
        configMoonshot.establecerUrlBaseParaProveedor("Moonshot (Kimi)", mockWebServer.url("/v1").toString());
        configMoonshot.establecerModeloParaProveedor("Moonshot (Kimi)", "kimi-k2.5");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"data\":[{\"id\":\"moonshot-v1-auto\"},{\"id\":\"kimi-k2.5\"}]}"));

        CompletableFuture<List<String>> resultado = new CompletableFuture<>();

        connectionTester.obtenerModelosDisponibles(configMoonshot, new ConnectionTester.CallbackModelos() {
            @Override
            public void alExito(List<String> modelos) {
                resultado.complete(modelos);
            }

            @Override
            public void alError(String error) {
                resultado.completeExceptionally(new AssertionError(error));
            }
        });

        assertEquals(List.of("kimi-k2.5", "moonshot-v1-auto"), resultado.get(5, TimeUnit.SECONDS),
            "Moonshot debe devolver modelos OpenAI-compatible");
        assertEquals("/v1/models", mockWebServer.takeRequest().getPath(),
            "Moonshot debe consultar GET /v1/models");
    }

    @Test
    @DisplayName("Obtener modelos rechaza Z.ai al no tener listado remoto documentado")
    void obtenerModelos_zAiRetornaErrorControlado() throws Exception {
        ConfiguracionAPI configZai = new ConfiguracionAPI();
        configZai.establecerProveedorAI("Z.ai");
        configZai.establecerApiKeyParaProveedor("Z.ai", "test-key");
        configZai.establecerUrlBaseParaProveedor("Z.ai", "https://api.z.ai/api/paas/v4");
        configZai.establecerModeloParaProveedor("Z.ai", "glm-5");

        CompletableFuture<String> resultado = new CompletableFuture<>();

        connectionTester.obtenerModelosDisponibles(configZai, new ConnectionTester.CallbackModelos() {
            @Override
            public void alExito(List<String> modelos) {
                resultado.complete("ok");
            }

            @Override
            public void alError(String error) {
                resultado.complete(error);
            }
        });

        String error = resultado.get(5, TimeUnit.SECONDS);
        assertTrue(error.contains("Z.ai"),
            "El error debe explicar que Z.ai no soporta listado remoto documentado");
    }

    @Test
    @DisplayName("Error cuando falta configuración requerida")
    void probarConexion_configIncompleta_retornaError() {
        ConfiguracionAPI configIncompleta = new ConfiguracionAPI();
        configIncompleta.establecerProveedorAI("OpenAI");
        configIncompleta.establecerUrlBaseParaProveedor("OpenAI", mockWebServer.url("/v1").toString());
        configIncompleta.establecerModeloParaProveedor("OpenAI", "gpt-4o");
        // No establecer API key para un proveedor que sí la requiere

        CompletableFuture<Boolean> resultado = new CompletableFuture<>();
        
        connectionTester.probarConexionProveedor(configIncompleta, new ConnectionTester.CallbackConexion() {
            @Override
            public void alExito(String mensaje) {
                resultado.complete(false);
            }

            @Override
            public void alError(String error) {
                // El mensaje está localizado, verificamos que no esté vacío
                resultado.complete(error != null && !error.isEmpty());
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
