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
        assertEquals("es", config.obtenerIdiomaUi());
        assertFalse(config.esDetallado());
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla());
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

        assertEquals("https://api.test.com/v1/responses", config.obtenerUrlApi());
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
        assertTrue(prompt.contains("{RESPONSE}"));
        assertTrue(prompt.contains("{OUTPUT_LANGUAGE}"));
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

    @Test
    @DisplayName("Maximo hallazgos tabla se normaliza en rango permitido")
    void testMaximoHallazgosNormalizaRango() {
        config.establecerMaximoHallazgosTabla(1);
        assertEquals(ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA, config.obtenerMaximoHallazgosTabla());

        config.establecerMaximoHallazgosTabla(999999);
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA, config.obtenerMaximoHallazgosTabla());
    }

    @Test
    @DisplayName("Snapshot de configuracion no se contamina por cambios posteriores")
    void testSnapshotInmutablePorCopia() {
        config.establecerProveedorAI("Ollama");
        config.establecerIdiomaUi("en");
        config.establecerModeloParaProveedor("Ollama", "llama3.2:latest");
        config.establecerUrlBaseParaProveedor("Ollama", "http://localhost:11434");
        config.establecerApiKeyParaProveedor("Ollama", "");

        ConfiguracionAPI snapshot = config.crearSnapshot();

        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5.2-pro");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "new-key");

        assertEquals("Ollama", snapshot.obtenerProveedorAI());
        assertEquals("en", snapshot.obtenerIdiomaUi());
        assertEquals("llama3.2:latest", snapshot.obtenerModeloParaProveedor("Ollama"));
        assertEquals("http://localhost:11434", snapshot.obtenerUrlBaseParaProveedor("Ollama"));
        assertEquals("", snapshot.obtenerApiKeyParaProveedor("Ollama"));
    }

    @Test
    @DisplayName("Idioma UI invalido vuelve a espanol")
    void testIdiomaInvalidoVuelveAEspanol() {
        config.establecerIdiomaUi("fr");
        assertEquals("es", config.obtenerIdiomaUi());

        config.establecerIdiomaUi("en");
        assertEquals("en", config.obtenerIdiomaUi());
    }

    @Test
    @DisplayName("AplicarDesde actualiza configuracion con copia profunda")
    void testAplicarDesdeCopiaProfunda() {
        ConfiguracionAPI origen = new ConfiguracionAPI();
        origen.establecerProveedorAI("OpenAI");
        origen.establecerIdiomaUi("en");
        origen.establecerApiKeyParaProveedor("OpenAI", "key-openai");
        origen.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        origen.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        origen.establecerRetrasoSegundos(7);
        origen.establecerMaximoConcurrente(4);

        config.aplicarDesde(origen);

        assertEquals("OpenAI", config.obtenerProveedorAI());
        assertEquals("en", config.obtenerIdiomaUi());
        assertEquals("key-openai", config.obtenerApiKeyParaProveedor("OpenAI"));
        assertEquals("gpt-5-mini", config.obtenerModeloParaProveedor("OpenAI"));
        assertEquals(7, config.obtenerRetrasoSegundos());
        assertEquals(4, config.obtenerMaximoConcurrente());

        origen.establecerApiKeyParaProveedor("OpenAI", "otro-valor");
        assertEquals("key-openai", config.obtenerApiKeyParaProveedor("OpenAI"));
    }

    @Test
    @DisplayName("Max tokens por proveedor normaliza valores invalidos")
    void testMaxTokensPorProveedorNormalizaInvalidos() {
        config.establecerProveedorAI("OpenAI");
        int defectoOpenAi = ProveedorAI.obtenerProveedor("OpenAI").obtenerMaxTokensPorDefecto();

        config.establecerMaxTokensParaProveedor("OpenAI", 0);
        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"));

        config.establecerMaxTokensParaProveedor("OpenAI", -50);
        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"));
    }

    @Test
    @DisplayName("Max tokens nulo en mapa no rompe y usa default")
    void testMaxTokensNuloEnMapaUsaDefault() {
        config.establecerProveedorAI("OpenAI");
        int defectoOpenAi = ProveedorAI.obtenerProveedor("OpenAI").obtenerMaxTokensPorDefecto();
        config.establecerMaxTokensPorProveedor(new java.util.HashMap<>(java.util.Map.of("OpenAI", defectoOpenAi)));
        config.obtenerMaxTokensPorProveedor().put("OpenAI", null);

        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"));
    }

    @Test
    @DisplayName("Validacion tolera tema nulo sin excepcion")
    void testValidacionTemaNuloNoRompe() {
        config.establecerTema(null);
        Map<String, String> errores = config.validar();
        assertTrue(errores.containsKey("tema"));
    }
}
