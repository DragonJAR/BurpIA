package com.burpia.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

@DisplayName("ConfiguracionAPI Tests")
class ConfiguracionAPITest {

    private ConfiguracionAPI config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionAPI();
    }

    @Test
    @DisplayName("Configuracion por defecto tiene valores validos")
    void testConfiguracionPorDefecto() {
        assertNotNull(config.obtenerProveedorAI());
        assertNotNull(config.obtenerUrlApi());
        assertNotNull(config.obtenerModelo());
        assertNotNull(config.obtenerPromptConfigurable());
        assertFalse(config.esDetallado());
    }

    @Test
    @DisplayName("Establecer y obtener valores por proveedor")
    void testEstablecerYObtenerValores() {
        config.establecerProveedorAI("OpenAI");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.test.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "test-key-123");
        config.establecerModeloParaProveedor("OpenAI", "gpt-4");
        config.establecerRetrasoSegundos(10);
        config.establecerMaximoConcurrente(5);
        config.establecerDetallado(true);

        assertEquals("https://api.test.com/v1/chat/completions", config.obtenerUrlApi());
        assertEquals("test-key-123", config.obtenerClaveApi());
        assertEquals("gpt-4", config.obtenerModelo());
        assertEquals(10, config.obtenerRetrasoSegundos());
        assertEquals(5, config.obtenerMaximoConcurrente());
        assertTrue(config.esDetallado());
    }

    @Test
    @DisplayName("Validacion con errores")
    void testValidacionConErrores() {
        ConfiguracionAPI configInvalida = new ConfiguracionAPI();
        configInvalida.establecerProveedorAI("OpenAI");
        configInvalida.establecerModeloParaProveedor("OpenAI", "");
        configInvalida.establecerApiKeyParaProveedor("OpenAI", "");

        Map<String, String> errores = configInvalida.validar();

        assertFalse(errores.isEmpty());
    }

    @Test
    @DisplayName("Validacion sin errores")
    void testValidacionSinErrores() {
        config.establecerClaveApi("valid-key");
        config.establecerModelo("valid-model");

        Map<String, String> errores = config.validar();

        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("esConfiguracionValida funciona correctamente")
    void testEsConfiguracionValida() {
        assertTrue(config.esConfiguracionValida());

        ConfiguracionAPI invalida = new ConfiguracionAPI();
        invalida.establecerModeloParaProveedor("Z.ai", "");

        assertFalse(invalida.esConfiguracionValida());
    }

    @Test
    @DisplayName("tieneApiKey funciona correctamente")
    void testTieneApiKey() {
        config.establecerClaveApi("my-key");
        assertTrue(config.tieneApiKey());

        config.establecerClaveApi("");
        assertFalse(config.tieneApiKey());

        config.establecerClaveApi("   ");
        assertFalse(config.tieneApiKey());
    }

    @Test
    @DisplayName("Prompt por defecto no es null ni vacio")
    void testPromptPorDefecto() {
        String prompt = ConfiguracionAPI.obtenerPromptPorDefecto();

        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("{REQUEST}"));
    }

    @Test
    @DisplayName("Establecer prompt null usa default")
    void testEstablecerPromptNull() {
        config.establecerPromptConfigurable(null);
        assertNotNull(config.obtenerPromptConfigurable());
    }

    @Test
    @DisplayName("Establecer prompt vacio usa default")
    void testEstablecerPromptVacio() {
        config.establecerPromptConfigurable("");
        assertNotNull(config.obtenerPromptConfigurable());
    }

    @Test
    @DisplayName("API key por proveedor funciona correctamente")
    void testApiKeyPorProveedor() {
        config.establecerApiKeyParaProveedor("OpenAI", "openai-key");
        config.establecerApiKeyParaProveedor("Claude", "claude-key");

        assertEquals("openai-key", config.obtenerApiKeyParaProveedor("OpenAI"));
        assertEquals("claude-key", config.obtenerApiKeyParaProveedor("Claude"));
        assertEquals("", config.obtenerApiKeyParaProveedor("Gemini"));
    }

    @Test
    @DisplayName("No arrastra API key entre proveedores")
    void testNoArrastreApiKey() {
        config.establecerProveedorAI("OpenAI");
        config.establecerApiKeyParaProveedor("OpenAI", "openai-key");

        config.establecerProveedorAI("Claude");
        assertEquals("", config.obtenerClaveApi());
    }
}
