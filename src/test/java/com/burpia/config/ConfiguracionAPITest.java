package com.burpia.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;




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
        assertNotNull(config.obtenerProveedorAI(), "assertNotNull failed at ConfiguracionAPITest.java:29");
        assertNotNull(config.obtenerUrlApi(), "assertNotNull failed at ConfiguracionAPITest.java:30");
        assertNotNull(config.obtenerModelo(), "assertNotNull failed at ConfiguracionAPITest.java:31");
        assertNotNull(config.obtenerPromptConfigurable(), "assertNotNull failed at ConfiguracionAPITest.java:32");
        assertEquals("es", config.obtenerIdiomaUi(), "assertEquals failed at ConfiguracionAPITest.java:33");
        assertFalse(config.esDetallado(), "assertFalse failed at ConfiguracionAPITest.java:34");
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla(), "assertEquals failed at ConfiguracionAPITest.java:35");
        assertEquals(ConfiguracionAPI.AGENTE_DELAY_DEFECTO_MS, config.obtenerAgenteDelay(), "assertEquals failed at ConfiguracionAPITest.java:36");
        assertTrue(config.alertasClickDerechoEnviarAHabilitadas(), "assertTrue failed at ConfiguracionAPITest.java:37");
    }

    @Test
    @DisplayName("Agente delay conserva valores sin normalizacion por rango")
    void testAgenteDelaySinNormalizacionPorRango() {
        config.establecerAgenteDelay(-250);
        assertEquals(-250, config.obtenerAgenteDelay(), "assertEquals failed at ConfiguracionAPITest.java:44");

        config.establecerAgenteDelay(120_000);
        assertEquals(120_000, config.obtenerAgenteDelay(), "assertEquals failed at ConfiguracionAPITest.java:47");
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

        assertEquals("https://api.test.com/v1/responses", config.obtenerUrlApi(), "assertEquals failed at ConfiguracionAPITest.java:61");
        assertEquals("test-key-123", config.obtenerClaveApi(), "assertEquals failed at ConfiguracionAPITest.java:62");
        assertEquals("gpt-4", config.obtenerModelo(), "assertEquals failed at ConfiguracionAPITest.java:63");
        assertEquals(10, config.obtenerRetrasoSegundos(), "assertEquals failed at ConfiguracionAPITest.java:64");
        assertEquals(5, config.obtenerMaximoConcurrente(), "assertEquals failed at ConfiguracionAPITest.java:65");
        assertTrue(config.esDetallado(), "assertTrue failed at ConfiguracionAPITest.java:66");
    }

    @Test
    @DisplayName("Validacion con errores")
    void testValidacionConErrores() {
        ConfiguracionAPI configInvalida = new ConfiguracionAPI();
        configInvalida.establecerProveedorAI("OpenAI");
        configInvalida.establecerModeloParaProveedor("OpenAI", "");
        configInvalida.establecerApiKeyParaProveedor("OpenAI", "");

        Map<String, String> errores = configInvalida.validar();

        assertFalse(errores.isEmpty(), "assertFalse failed at ConfiguracionAPITest.java:79");
    }

    @Test
    @DisplayName("Validacion sin errores")
    void testValidacionSinErrores() {
        config.establecerClaveApi("valid-key");
        config.establecerModelo("valid-model");

        Map<String, String> errores = config.validar();

        assertTrue(errores.isEmpty(), "assertTrue failed at ConfiguracionAPITest.java:90");
    }

    @Test
    @DisplayName("Estado de configuración se determina con validar()")
    void testEstadoConfiguracionConValidar() {
        ConfiguracionAPI valida = new ConfiguracionAPI();
        valida.establecerProveedorAI("OpenAI");
        valida.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        valida.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        valida.establecerApiKeyParaProveedor("OpenAI", "test-key");
        assertTrue(valida.validar().isEmpty(), "assertTrue failed at ConfiguracionAPITest.java:101");

        ConfiguracionAPI invalida = valida.crearSnapshot();
        invalida.establecerModeloParaProveedor("OpenAI", "");
        Map<String, String> errores = invalida.validar();
        assertFalse(errores.isEmpty(), "assertFalse failed at ConfiguracionAPITest.java:106");
        assertTrue(errores.containsKey("modelo"), "assertTrue failed at ConfiguracionAPITest.java:107");
    }

    @Test
    @DisplayName("tieneApiKey funciona correctamente")
    void testTieneApiKey() {
        config.establecerClaveApi("my-key");
        assertTrue(config.tieneApiKey(), "assertTrue failed at ConfiguracionAPITest.java:114");

        config.establecerClaveApi("");
        assertFalse(config.tieneApiKey(), "assertFalse failed at ConfiguracionAPITest.java:117");

        config.establecerClaveApi("   ");
        assertFalse(config.tieneApiKey(), "assertFalse failed at ConfiguracionAPITest.java:120");
    }

    @Test
    @DisplayName("Validacion para consulta exige API key en proveedores que la requieren")
    void testValidarParaConsultaExigeApiKey() {
        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "");

        String alerta = config.validarParaConsultaModelo();
        assertTrue(alerta.contains("ALERTA: Clave de API requerida para OpenAI"), "assertTrue failed at ConfiguracionAPITest.java:132");
        assertEquals(
            ConfiguracionAPI.CodigoValidacionConsulta.API_KEY_REQUERIDA,
            config.validarCodigoParaConsultaModelo()
        , "assertEquals failed at ConfiguracionAPITest.java:133");
    }

    @Test
    @DisplayName("Validacion para consulta permite proveedores sin API key obligatoria")
    void testValidarParaConsultaPermiteProveedorSinApiKey() {
        config.establecerProveedorAI("Ollama");
        config.establecerModeloParaProveedor("Ollama", "gemma3:12b");
        config.establecerUrlBaseParaProveedor("Ollama", "http://localhost:11434");
        config.establecerApiKeyParaProveedor("Ollama", "");

        assertEquals("", config.validarParaConsultaModelo(), "assertEquals failed at ConfiguracionAPITest.java:147");
        assertEquals(
            ConfiguracionAPI.CodigoValidacionConsulta.OK,
            config.validarCodigoParaConsultaModelo()
        , "assertEquals failed at ConfiguracionAPITest.java:148");
    }

    @Test
    @DisplayName("Prompt por defecto no es null ni vacio")
    void testPromptPorDefecto() {
        String prompt = ConfiguracionAPI.obtenerPromptPorDefecto();

        assertNotNull(prompt, "assertNotNull failed at ConfiguracionAPITest.java:159");
        assertFalse(prompt.isEmpty(), "assertFalse failed at ConfiguracionAPITest.java:160");
        assertTrue(prompt.contains("{REQUEST}"), "assertTrue failed at ConfiguracionAPITest.java:161");
        assertTrue(prompt.contains("{RESPONSE}"), "assertTrue failed at ConfiguracionAPITest.java:162");
        assertTrue(prompt.contains("{OUTPUT_LANGUAGE}"), "assertTrue failed at ConfiguracionAPITest.java:163");
        assertTrue(prompt.contains("\"evidencia\""), "assertTrue failed at ConfiguracionAPITest.java:164");
    }

    @Test
    @DisplayName("Prompt pre-flight fijo de agente arranca en modo espera")
    void testPromptAgentePreflightFijoModoEspera() {
        String promptPreflight = ConfiguracionAPI.obtenerAgentePreflightPromptPorDefecto();

        assertNotNull(promptPreflight, "assertNotNull failed at ConfiguracionAPITest.java:172");
        assertTrue(promptPreflight.startsWith("# BURPAI CRITICAL PRE-FLIGHT CHECK"), "assertTrue failed at ConfiguracionAPITest.java:173");
        assertTrue(promptPreflight.contains("**Level 1**: Burp Suite MCP tools (High Priority)."), "assertTrue failed at ConfiguracionAPITest.java:174");
        assertTrue(promptPreflight.contains("**Level 2**: Other MCP tools (Auxiliary)."), "assertTrue failed at ConfiguracionAPITest.java:175");
        assertTrue(promptPreflight.contains("**Level 3**: Native capabilities (Shell, Curl, Scripting)."), "assertTrue failed at ConfiguracionAPITest.java:176");
        assertTrue(promptPreflight.contains("reply exactly: **READY**"), "assertTrue failed at ConfiguracionAPITest.java:177");
        assertTrue(promptPreflight.contains("Burp Suite MCP is the PRIMARY interface."), "assertTrue failed at ConfiguracionAPITest.java:178");
    }

    @Test
    @DisplayName("Prompt pre-flight configurable usa default cuando viene vacio")
    void testPromptAgentePreflightConfigurableNormalizaDefault() {
        config.establecerAgentePreflightPrompt("PROMPT_PREFLIGHT_CUSTOM");
        assertEquals("PROMPT_PREFLIGHT_CUSTOM", config.obtenerAgentePreflightPrompt(), "assertEquals failed at ConfiguracionAPITest.java:185");

        config.establecerAgentePreflightPrompt("  ");
        assertEquals(
            ConfiguracionAPI.obtenerAgentePreflightPromptPorDefecto(),
            config.obtenerAgentePreflightPrompt()
        , "assertEquals failed at ConfiguracionAPITest.java:188");
    }

    @Test
    @DisplayName("Prompt de validacion de agente incluye issue_context y placeholders")
    void testPromptAgenteValidacionPorDefecto() {
        String promptValidacion = ConfiguracionAPI.obtenerAgentePromptPorDefecto();

        assertNotNull(promptValidacion, "assertNotNull failed at ConfiguracionAPITest.java:199");
        assertFalse(promptValidacion.startsWith("BURPAI CRITICAL PRE-FLIGHT CHECK:"), "assertFalse failed at ConfiguracionAPITest.java:200");
        assertTrue(promptValidacion.contains("# ROLE"), "assertTrue failed at ConfiguracionAPITest.java:201");
        assertTrue(promptValidacion.contains("<issue_context>"), "assertTrue failed at ConfiguracionAPITest.java:202");
        assertTrue(promptValidacion.contains("{TITLE}"), "assertTrue failed at ConfiguracionAPITest.java:203");
        assertTrue(promptValidacion.contains("{DESCRIPTION}"), "assertTrue failed at ConfiguracionAPITest.java:204");
        assertTrue(promptValidacion.contains("{REQUEST}"), "assertTrue failed at ConfiguracionAPITest.java:205");
        assertTrue(promptValidacion.contains("{RESPONSE}"), "assertTrue failed at ConfiguracionAPITest.java:206");
        assertTrue(promptValidacion.contains("{OUTPUT_LANGUAGE}"), "assertTrue failed at ConfiguracionAPITest.java:207");
    }

    @Test
    @DisplayName("Establecer prompt null usa default")
    void testEstablecerPromptNull() {
        config.establecerPromptConfigurable(null);
        assertNotNull(config.obtenerPromptConfigurable(), "assertNotNull failed at ConfiguracionAPITest.java:214");
    }

    @Test
    @DisplayName("Establecer prompt vacio usa default")
    void testEstablecerPromptVacio() {
        config.establecerPromptConfigurable("");
        assertNotNull(config.obtenerPromptConfigurable(), "assertNotNull failed at ConfiguracionAPITest.java:221");
    }

    @Test
    @DisplayName("API key por proveedor funciona correctamente")
    void testApiKeyPorProveedor() {
        config.establecerApiKeyParaProveedor("OpenAI", "openai-key");
        config.establecerApiKeyParaProveedor("Claude", "claude-key");

        assertEquals("openai-key", config.obtenerApiKeyParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:230");
        assertEquals("claude-key", config.obtenerApiKeyParaProveedor("Claude"), "assertEquals failed at ConfiguracionAPITest.java:231");
        assertEquals("", config.obtenerApiKeyParaProveedor("Gemini"), "assertEquals failed at ConfiguracionAPITest.java:232");
    }

    @Test
    @DisplayName("No arrastra API key entre proveedores")
    void testNoArrastreApiKey() {
        config.establecerProveedorAI("OpenAI");
        config.establecerApiKeyParaProveedor("OpenAI", "openai-key");

        config.establecerProveedorAI("Claude");
        assertEquals("", config.obtenerClaveApi(), "assertEquals failed at ConfiguracionAPITest.java:242");
    }

    @Test
    @DisplayName("Maximo hallazgos tabla se normaliza en rango permitido")
    void testMaximoHallazgosNormalizaRango() {
        config.establecerMaximoHallazgosTabla(1);
        assertEquals(ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA, config.obtenerMaximoHallazgosTabla(), "assertEquals failed at ConfiguracionAPITest.java:249");

        config.establecerMaximoHallazgosTabla(999999);
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA, config.obtenerMaximoHallazgosTabla(), "assertEquals failed at ConfiguracionAPITest.java:252");
    }

    @Test
    @DisplayName("Snapshot de configuración no se contamina por cambios posteriores")
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

        assertEquals("Ollama", snapshot.obtenerProveedorAI(), "assertEquals failed at ConfiguracionAPITest.java:271");
        assertEquals("en", snapshot.obtenerIdiomaUi(), "assertEquals failed at ConfiguracionAPITest.java:272");
        assertEquals("llama3.2:latest", snapshot.obtenerModeloParaProveedor("Ollama"), "assertEquals failed at ConfiguracionAPITest.java:273");
        assertEquals("http://localhost:11434", snapshot.obtenerUrlBaseParaProveedor("Ollama"), "assertEquals failed at ConfiguracionAPITest.java:274");
        assertEquals("", snapshot.obtenerApiKeyParaProveedor("Ollama"), "assertEquals failed at ConfiguracionAPITest.java:275");
    }

    @Test
    @DisplayName("Snapshot y aplicarDesde conservan preferencias runtime")
    void testSnapshotYAplicarDesdeConservanPreferenciasRuntime() {
        ConfiguracionAPI origen = new ConfiguracionAPI();
        origen.establecerEscaneoPasivoHabilitado(false);
        origen.establecerAutoGuardadoIssuesHabilitado(false);
        origen.establecerAutoScrollConsolaHabilitado(false);
        origen.establecerAlertasClickDerechoEnviarAHabilitadas(false);

        ConfiguracionAPI snapshot = origen.crearSnapshot();
        assertFalse(snapshot.escaneoPasivoHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:288");
        assertFalse(snapshot.autoGuardadoIssuesHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:289");
        assertFalse(snapshot.autoScrollConsolaHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:290");
        assertFalse(snapshot.alertasClickDerechoEnviarAHabilitadas(), "assertFalse failed at ConfiguracionAPITest.java:291");

        ConfiguracionAPI destino = new ConfiguracionAPI();
        destino.aplicarDesde(origen);
        assertFalse(destino.escaneoPasivoHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:295");
        assertFalse(destino.autoGuardadoIssuesHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:296");
        assertFalse(destino.autoScrollConsolaHabilitado(), "assertFalse failed at ConfiguracionAPITest.java:297");
        assertFalse(destino.alertasClickDerechoEnviarAHabilitadas(), "assertFalse failed at ConfiguracionAPITest.java:298");
    }

    @Test
    @DisplayName("Snapshot y aplicarDesde conservan preferencias de proxy y Factory Droid")
    void testSnapshotYAplicarDesdeConservanPreferenciasFactoryDroidYProxy() {
        ConfiguracionAPI origen = new ConfiguracionAPI();
        origen.establecerSoloProxy(false);
        origen.establecerAgenteHabilitado(true);
        origen.establecerRutaBinarioAgente("FACTORY_DROID", "/tmp/droid");
        origen.establecerAgentePreflightPrompt("preflight-prueba");
        origen.establecerAgentePrompt("prompt-prueba");
        origen.establecerAgenteDelay(2500);

        ConfiguracionAPI snapshot = origen.crearSnapshot();
        assertFalse(snapshot.soloProxy(), "assertFalse failed at ConfiguracionAPITest.java:313");
        assertTrue(snapshot.agenteHabilitado(), "assertTrue failed at ConfiguracionAPITest.java:314");
        assertEquals("/tmp/droid", snapshot.obtenerRutaBinarioAgente("FACTORY_DROID"), "assertEquals failed at ConfiguracionAPITest.java:315");
        assertEquals("preflight-prueba", snapshot.obtenerAgentePreflightPrompt(), "assertEquals failed at ConfiguracionAPITest.java:316");
        assertEquals("prompt-prueba", snapshot.obtenerAgentePrompt(), "assertEquals failed at ConfiguracionAPITest.java:317");
        assertEquals(2500, snapshot.obtenerAgenteDelay(), "assertEquals failed at ConfiguracionAPITest.java:318");

        ConfiguracionAPI destino = new ConfiguracionAPI();
        destino.aplicarDesde(origen);
        assertFalse(destino.soloProxy(), "assertFalse failed at ConfiguracionAPITest.java:322");
        assertTrue(destino.agenteHabilitado(), "assertTrue failed at ConfiguracionAPITest.java:323");
        assertEquals("/tmp/droid", destino.obtenerRutaBinarioAgente("FACTORY_DROID"), "assertEquals failed at ConfiguracionAPITest.java:324");
        assertEquals("preflight-prueba", destino.obtenerAgentePreflightPrompt(), "assertEquals failed at ConfiguracionAPITest.java:325");
        assertEquals("prompt-prueba", destino.obtenerAgentePrompt(), "assertEquals failed at ConfiguracionAPITest.java:326");
        assertEquals(2500, destino.obtenerAgenteDelay(), "assertEquals failed at ConfiguracionAPITest.java:327");
    }

    @Test
    @DisplayName("Idioma UI inválido vuelve a español")
    void testIdiomaInvalidoVuelveAEspanol() {
        config.establecerIdiomaUi("fr");
        assertEquals("es", config.obtenerIdiomaUi(), "assertEquals failed at ConfiguracionAPITest.java:334");

        config.establecerIdiomaUi("en");
        assertEquals("en", config.obtenerIdiomaUi(), "assertEquals failed at ConfiguracionAPITest.java:337");
    }

    @Test
    @DisplayName("Custom sin URL guardada usa default por idioma")
    void testCustomDefaultUrlByLanguage() {
        ConfiguracionAPI configEs = new ConfiguracionAPI();
        configEs.establecerIdiomaUi("es");
        assertEquals(
            "https://TU_BASE_URL_COMPATIBLE_CON_OPENAI/v1",
            configEs.obtenerUrlBaseParaProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01)
        , "assertEquals failed at ConfiguracionAPITest.java:345");

        ConfiguracionAPI configEn = new ConfiguracionAPI();
        configEn.establecerIdiomaUi("en");
        assertEquals(
            "https://YOUR_OPENAI_COMPATIBLE_BASE_URL/v1",
            configEn.obtenerUrlBaseParaProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01)
        , "assertEquals failed at ConfiguracionAPITest.java:352");
    }

    @Test
    @DisplayName("Proveedor desconocido no se acepta y vuelve a default")
    void testProveedorDesconocidoNoSeAcepta() {
        config.establecerProveedorAI("-- Custom --");
        assertEquals("Z.ai", config.obtenerProveedorAI(), "assertEquals failed at ConfiguracionAPITest.java:362");
    }

    @Test
    @DisplayName("Lista multi-proveedor descarta proveedores desconocidos")
    void testListaMultiProveedorDescartaDesconocidos() {
        config.establecerProveedoresMultiConsulta(Arrays.asList(
            ProveedorAI.PROVEEDOR_CUSTOM_02,
            "OpenAI",
            "NoExiste"
        ));

        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        assertEquals(2, proveedores.size(), "assertEquals failed at ConfiguracionAPITest.java:375");
        assertEquals(ProveedorAI.PROVEEDOR_CUSTOM_02, proveedores.get(0), "assertEquals failed at ConfiguracionAPITest.java:376");
        assertEquals("OpenAI", proveedores.get(1), "assertEquals failed at ConfiguracionAPITest.java:377");
    }

    @Test
    @DisplayName("Mapas por proveedor descartan claves de proveedor desconocido")
    void testMapasPorProveedorDescartanProveedorDesconocido() {
        Map<String, String> urls = new HashMap<>();
        urls.put("-- Custom --", "https://legacy.local/v1");
        urls.put(ProveedorAI.PROVEEDOR_CUSTOM_01, "https://custom01.local/v1");
        config.establecerUrlsBasePorProveedor(urls);

        assertEquals("https://custom01.local/v1",
            config.obtenerUrlBaseGuardadaParaProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01), "assertEquals failed at ConfiguracionAPITest.java:388");
        assertNull(config.obtenerUrlBaseGuardadaParaProveedor("-- Custom --"), "assertNull failed at ConfiguracionAPITest.java:390");
    }

    @Test
    @DisplayName("Timeout por modelo descarta proveedor desconocido")
    void testTimeoutPorModeloDescartaProveedorDesconocido() {
        int timeoutGlobal = config.obtenerTiempoEsperaAI();
        config.establecerTiempoEsperaParaModelo("-- Custom --", "custom-model", 180);
        assertEquals(timeoutGlobal, config.obtenerTiempoEsperaParaModelo("-- Custom --", "custom-model"), "assertEquals failed at ConfiguracionAPITest.java:398");
        assertNull(config.obtenerTiempoEsperaConfiguradoParaModelo("-- Custom --", "custom-model"), "assertNull failed at ConfiguracionAPITest.java:399");
    }

    @Test
    @DisplayName("AplicarDesde actualiza configuración con copia profunda")
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

        assertEquals("OpenAI", config.obtenerProveedorAI(), "assertEquals failed at ConfiguracionAPITest.java:416");
        assertEquals("en", config.obtenerIdiomaUi(), "assertEquals failed at ConfiguracionAPITest.java:417");
        assertEquals("key-openai", config.obtenerApiKeyParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:418");
        assertEquals("gpt-5-mini", config.obtenerModeloParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:419");
        assertEquals(7, config.obtenerRetrasoSegundos(), "assertEquals failed at ConfiguracionAPITest.java:420");
        assertEquals(4, config.obtenerMaximoConcurrente(), "assertEquals failed at ConfiguracionAPITest.java:421");

        origen.establecerApiKeyParaProveedor("OpenAI", "otro-valor");
        assertEquals("key-openai", config.obtenerApiKeyParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:424");
    }

    @Test
    @DisplayName("Max tokens por proveedor normaliza valores inválidos")
    void testMaxTokensPorProveedorNormalizaInvalidos() {
        config.establecerProveedorAI("OpenAI");
        int defectoOpenAi = ProveedorAI.obtenerProveedor("OpenAI").obtenerMaxTokensPorDefecto();

        config.establecerMaxTokensParaProveedor("OpenAI", 0);
        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:434");

        config.establecerMaxTokensParaProveedor("OpenAI", -50);
        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:437");
    }

    @Test
    @DisplayName("Max tokens nulo en mapa no rompe y usa default")
    void testMaxTokensNuloEnMapaUsaDefault() {
        config.establecerProveedorAI("OpenAI");
        int defectoOpenAi = ProveedorAI.obtenerProveedor("OpenAI").obtenerMaxTokensPorDefecto();
        Map<String, Integer> mapa = new HashMap<>();
        mapa.put("OpenAI", null);
        config.establecerMaxTokensPorProveedor(mapa);

        assertEquals(defectoOpenAi, config.obtenerMaxTokensParaProveedor("OpenAI"), "assertEquals failed at ConfiguracionAPITest.java:449");
    }

    @Test
    @DisplayName("Timeout por modelo usa valor configurado y fallback global")
    void testTimeoutPorModeloConFallback() {
        config.establecerTiempoEsperaAI(60);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);

        assertEquals(180, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"), "assertEquals failed at ConfiguracionAPITest.java:458");
        assertEquals(60, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-4-air"), "assertEquals failed at ConfiguracionAPITest.java:459");
        assertNull(config.obtenerTiempoEsperaConfiguradoParaModelo("Z.ai", "glm-4-air"), "assertNull failed at ConfiguracionAPITest.java:460");
    }

    @Test
    @DisplayName("Timeout por modelo normaliza rango permitido")
    void testTimeoutPorModeloNormalizaRango() {
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 1);
        assertEquals(10, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"), "assertEquals failed at ConfiguracionAPITest.java:467");

        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 999);
        assertEquals(300, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"), "assertEquals failed at ConfiguracionAPITest.java:470");
    }

    @Test
    @DisplayName("Mapa timeout por modelo filtra entradas invalidas")
    void testMapaTimeoutPorModeloFiltraInvalidos() {
        Map<String, Integer> mapa = new HashMap<>();
        mapa.put("Z.ai::glm-5", 120);
        mapa.put("   ", 99);
        mapa.put("OpenAI::gpt-5-mini", null);

        config.establecerTiempoEsperaPorModelo(mapa);

        Map<String, Integer> snapshot = config.obtenerTiempoEsperaPorModelo();
        assertEquals(1, snapshot.size(), "assertEquals failed at ConfiguracionAPITest.java:484");
        assertEquals(120, snapshot.get("Z.ai::glm-5"), "assertEquals failed at ConfiguracionAPITest.java:485");
    }

    @Test
    @DisplayName("Normaliza retraso y maximo concurrente en setters")
    void testNormalizaRetrasoYMaximoConcurrente() {
        config.establecerRetrasoSegundos(-5);
        assertEquals(ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS, config.obtenerRetrasoSegundos(), "assertEquals failed at ConfiguracionAPITest.java:492");

        config.establecerRetrasoSegundos(999);
        assertEquals(ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS, config.obtenerRetrasoSegundos(), "assertEquals failed at ConfiguracionAPITest.java:495");

        config.establecerMaximoConcurrente(0);
        assertEquals(ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE, config.obtenerMaximoConcurrente(), "assertEquals failed at ConfiguracionAPITest.java:498");

        config.establecerMaximoConcurrente(999);
        assertEquals(ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE, config.obtenerMaximoConcurrente(), "assertEquals failed at ConfiguracionAPITest.java:501");
    }
}
