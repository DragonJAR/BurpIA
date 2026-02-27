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
    @DisplayName("Estado de configuracion se determina con validar()")
    void testEstadoConfiguracionConValidar() {
        ConfiguracionAPI valida = new ConfiguracionAPI();
        valida.establecerProveedorAI("OpenAI");
        valida.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        valida.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        valida.establecerApiKeyParaProveedor("OpenAI", "test-key");
        assertTrue(valida.validar().isEmpty());

        ConfiguracionAPI invalida = valida.crearSnapshot();
        invalida.establecerModeloParaProveedor("OpenAI", "");
        Map<String, String> errores = invalida.validar();
        assertFalse(errores.isEmpty());
        assertTrue(errores.containsKey("modelo"));
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
    @DisplayName("Validacion para consulta exige API key en proveedores que la requieren")
    void testValidarParaConsultaExigeApiKey() {
        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "");

        String alerta = config.validarParaConsultaModelo();
        assertTrue(alerta.contains("ALERTA: Clave de API requerida para OpenAI"));
        assertEquals(
            ConfiguracionAPI.CodigoValidacionConsulta.API_KEY_REQUERIDA,
            config.validarCodigoParaConsultaModelo()
        );
    }

    @Test
    @DisplayName("Validacion para consulta permite proveedores sin API key obligatoria")
    void testValidarParaConsultaPermiteProveedorSinApiKey() {
        config.establecerProveedorAI("Ollama");
        config.establecerModeloParaProveedor("Ollama", "gemma3:12b");
        config.establecerUrlBaseParaProveedor("Ollama", "http://localhost:11434");
        config.establecerApiKeyParaProveedor("Ollama", "");

        assertEquals("", config.validarParaConsultaModelo());
        assertEquals(
            ConfiguracionAPI.CodigoValidacionConsulta.OK,
            config.validarCodigoParaConsultaModelo()
        );
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
        assertTrue(prompt.contains("\"evidencia\""));
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
    @DisplayName("Snapshot y aplicarDesde conservan preferencias runtime")
    void testSnapshotYAplicarDesdeConservanPreferenciasRuntime() {
        ConfiguracionAPI origen = new ConfiguracionAPI();
        origen.establecerEscaneoPasivoHabilitado(false);
        origen.establecerAutoGuardadoIssuesHabilitado(false);
        origen.establecerAutoScrollConsolaHabilitado(false);

        ConfiguracionAPI snapshot = origen.crearSnapshot();
        assertFalse(snapshot.escaneoPasivoHabilitado());
        assertFalse(snapshot.autoGuardadoIssuesHabilitado());
        assertFalse(snapshot.autoScrollConsolaHabilitado());

        ConfiguracionAPI destino = new ConfiguracionAPI();
        destino.aplicarDesde(origen);
        assertFalse(destino.escaneoPasivoHabilitado());
        assertFalse(destino.autoGuardadoIssuesHabilitado());
        assertFalse(destino.autoScrollConsolaHabilitado());
    }

    @Test
    @DisplayName("Snapshot y aplicarDesde conservan preferencias de proxy y Factory Droid")
    void testSnapshotYAplicarDesdeConservanPreferenciasFactoryDroidYProxy() {
        ConfiguracionAPI origen = new ConfiguracionAPI();
        origen.establecerSoloProxy(false);
        origen.establecerAgenteHabilitado(true);
        origen.establecerRutaBinarioAgente("FACTORY_DROID", "/tmp/droid");
        origen.establecerAgentePrompt("prompt-prueba");
        origen.establecerAgenteDelay(2500);

        ConfiguracionAPI snapshot = origen.crearSnapshot();
        assertFalse(snapshot.soloProxy());
        assertTrue(snapshot.agenteHabilitado());
        assertEquals("/tmp/droid", snapshot.obtenerRutaBinarioAgente("FACTORY_DROID"));
        assertEquals("prompt-prueba", snapshot.obtenerAgentePrompt());
        assertEquals(2500, snapshot.obtenerAgenteDelay());

        ConfiguracionAPI destino = new ConfiguracionAPI();
        destino.aplicarDesde(origen);
        assertFalse(destino.soloProxy());
        assertTrue(destino.agenteHabilitado());
        assertEquals("/tmp/droid", destino.obtenerRutaBinarioAgente("FACTORY_DROID"));
        assertEquals("prompt-prueba", destino.obtenerAgentePrompt());
        assertEquals(2500, destino.obtenerAgenteDelay());
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
    @DisplayName("Custom sin URL guardada usa default por idioma")
    void testCustomDefaultUrlByLanguage() {
        ConfiguracionAPI configEs = new ConfiguracionAPI();
        configEs.establecerIdiomaUi("es");
        assertEquals(
            "https://TU_BASE_URL_COMPATIBLE_CON_OPENAI/v1",
            configEs.obtenerUrlBaseParaProveedor("-- Custom --")
        );

        ConfiguracionAPI configEn = new ConfiguracionAPI();
        configEn.establecerIdiomaUi("en");
        assertEquals(
            "https://YOUR_OPENAI_COMPATIBLE_BASE_URL/v1",
            configEn.obtenerUrlBaseParaProveedor("-- Custom --")
        );
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
        java.util.HashMap<String, Integer> mapa = new java.util.HashMap<>();
        mapa.put("OpenAI", null);
        config.establecerMaxTokensPorProveedor(mapa);

        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"));
    }

    @Test
    @DisplayName("Timeout por modelo usa valor configurado y fallback global")
    void testTimeoutPorModeloConFallback() {
        config.establecerTiempoEsperaAI(60);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);

        assertEquals(180, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));
        assertEquals(60, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-4-air"));
        assertNull(config.obtenerTiempoEsperaConfiguradoParaModelo("Z.ai", "glm-4-air"));
    }

    @Test
    @DisplayName("Timeout por modelo normaliza rango permitido")
    void testTimeoutPorModeloNormalizaRango() {
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 1);
        assertEquals(10, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));

        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 999);
        assertEquals(300, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));
    }

    @Test
    @DisplayName("Mapa timeout por modelo filtra entradas invalidas")
    void testMapaTimeoutPorModeloFiltraInvalidos() {
        java.util.HashMap<String, Integer> mapa = new java.util.HashMap<>();
        mapa.put("Z.ai::glm-5", 120);
        mapa.put("   ", 99);
        mapa.put("OpenAI::gpt-5-mini", null);

        config.establecerTiempoEsperaPorModelo(mapa);

        Map<String, Integer> snapshot = config.obtenerTiempoEsperaPorModelo();
        assertEquals(1, snapshot.size());
        assertEquals(120, snapshot.get("Z.ai::glm-5"));
    }

    @Test
    @DisplayName("Tema nulo se normaliza y no genera error de validacion")
    void testTemaNuloSeNormalizaSinError() {
        config.establecerTema(null);
        Map<String, String> errores = config.validar();
        assertFalse(errores.containsKey("tema"));
        assertEquals("Light", config.obtenerTema());
    }

    @Test
    @DisplayName("Normaliza retraso y maximo concurrente en setters")
    void testNormalizaRetrasoYMaximoConcurrente() {
        config.establecerRetrasoSegundos(-5);
        assertEquals(ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS, config.obtenerRetrasoSegundos());

        config.establecerRetrasoSegundos(999);
        assertEquals(ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS, config.obtenerRetrasoSegundos());

        config.establecerMaximoConcurrente(0);
        assertEquals(ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE, config.obtenerMaximoConcurrente());

        config.establecerMaximoConcurrente(999);
        assertEquals(ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE, config.obtenerMaximoConcurrente());
    }
}
