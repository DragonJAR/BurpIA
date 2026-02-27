package com.burpia.config;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;





@DisplayName("GestorConfiguracion Tests")
class GestorConfiguracionTest {

    private String userHomeOriginal;

    @AfterEach
    void tearDown() {
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Guarda configuracion sin campos legacy de proveedor")
    void testGuardarSinCamposLegacy() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerApiKeyParaProveedor("OpenAI", "key");
        config.establecerModeloParaProveedor("OpenAI", "gpt-4o");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerDetallado(false);
        config.establecerIdiomaUi("en");
        config.establecerMaximoHallazgosTabla(2500);

        assertTrue(gestor.guardarConfiguracion(config));

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = Files.readString(configPath, StandardCharsets.UTF_8);

        assertFalse(json.contains("\"urlApi\""));
        assertFalse(json.contains("\"claveApi\""));
        assertFalse(json.contains("\"modelo\""));
        assertTrue(json.contains("\"apiKeysPorProveedor\""));
        assertTrue(json.contains("\"modelosPorProveedor\""));
        assertTrue(json.contains("\"idiomaUi\": \"en\""));
        assertTrue(json.contains("\"maximoHallazgosTabla\": 2500"));
    }

    @Test
    @DisplayName("Cuando detallado no existe en JSON queda deshabilitado por defecto")
    void testDetalladoDefaultFalseSinCampo() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"retrasoSegundos\": 5,\n" +
            "  \"maximoConcurrente\": 3,\n" +
            "  \"idiomaUi\": \"en\",\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-4o\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, json, StandardCharsets.UTF_8);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = gestor.cargarConfiguracion();
        assertFalse(config.esDetallado());
        assertEquals("en", config.obtenerIdiomaUi());
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla());
    }

    @Test
    @DisplayName("Guarda y carga preferencias runtime de usuario")
    void testPersistenciaPreferenciasRuntimeUsuario() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerEscaneoPasivoHabilitado(false);
        config.establecerAutoGuardadoIssuesHabilitado(false);
        config.establecerAutoScrollConsolaHabilitado(false);

        assertTrue(gestor.guardarConfiguracion(config));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertFalse(cargada.escaneoPasivoHabilitado());
        assertFalse(cargada.autoGuardadoIssuesHabilitado());
        assertFalse(cargada.autoScrollConsolaHabilitado());

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"escaneoPasivoHabilitado\": false"));
        assertTrue(json.contains("\"autoGuardadoIssuesHabilitado\": false"));
        assertTrue(json.contains("\"autoScrollConsolaHabilitado\": false"));
    }

    @Test
    @DisplayName("Guarda y carga prompts de agente inicial y validacion")
    void testPersistenciaPromptsAgente() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgentePreflightPrompt("PRE_FLIGHT_CUSTOM");
        config.establecerAgentePrompt("VALIDACION_CUSTOM");

        assertTrue(gestor.guardarConfiguracion(config));

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"agentePreflightPrompt\": \"PRE_FLIGHT_CUSTOM\""));
        assertTrue(json.contains("\"agentePrompt\": \"VALIDACION_CUSTOM\""));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals("PRE_FLIGHT_CUSTOM", cargada.obtenerAgentePreflightPrompt());
        assertEquals("VALIDACION_CUSTOM", cargada.obtenerAgentePrompt());
    }

    @Test
    @DisplayName("Guarda y carga timeout por modelo en JSON")
    void testPersistenciaTimeoutPorModelo() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTiempoEsperaAI(90);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);
        config.establecerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini", 240);

        assertTrue(gestor.guardarConfiguracion(config));

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"tiempoEsperaPorModelo\""));
        assertTrue(json.contains("\"Z.ai::glm-5\": 180"));
        assertTrue(json.contains("\"OpenAI::gpt-5-mini\": 240"));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals(180, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));
        assertEquals(240, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"));
        assertEquals(90, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-4-air"));
    }

    @Test
    @DisplayName("JSON legacy sin timeout por modelo mantiene fallback global")
    void testLegacySinTimeoutPorModeloMantieneFallback() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"tiempoEsperaAI\": 120,\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-5-mini\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, json, StandardCharsets.UTF_8);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI cargada = gestor.cargarConfiguracion();

        assertTrue(cargada.obtenerTiempoEsperaPorModelo().isEmpty());
        assertEquals(120, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"));
    }

    @Test
    @DisplayName("Logs de configuracion se localizan a ingles cuando idioma UI es EN")
    void testLogsConfiguracionEnIngles() {
        I18nUI.establecerIdioma("en");
        StringWriter salida = new StringWriter();
        StringWriter errores = new StringWriter();

        new GestorConfiguracion(new PrintWriter(salida, true), new PrintWriter(errores, true));

        String log = salida.toString();
        assertTrue(log.contains("[Configuration]"));
        assertTrue(log.contains("Configuration path"));
    }

    @Test
    @DisplayName("Guardar configuracion nula devuelve error controlado")
    void testGuardarConfiguracionNula() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        GestorConfiguracion gestor = new GestorConfiguracion();
        StringBuilder mensajeError = new StringBuilder();

        assertFalse(gestor.guardarConfiguracion(null, mensajeError));
        assertNotNull(mensajeError.toString());
        assertTrue(mensajeError.toString().toLowerCase().contains("null")
            || mensajeError.toString().toLowerCase().contains("nula"));
        assertFalse(Files.exists(tempDir.resolve(".burpia/config.json")));
        assertFalse(Files.exists(tempDir.resolve(".burpia/config.json.tmp")));
    }

    @Test
    @DisplayName("Si prompt no esta modificado se aplica prompt por defecto vigente al cargar")
    void testPromptNoModificadoUsaDefaultVigente() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-config-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        Path configPath = tempDir.resolve(".burpia/config.json");
        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"promptConfigurable\": \"PROMPT_ANTIGUO\",\n" +
            "  \"promptModificado\": false,\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-4o\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, json, StandardCharsets.UTF_8);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = gestor.cargarConfiguracion();

        assertFalse(config.esPromptModificado());
        assertEquals(ConfiguracionAPI.obtenerPromptPorDefecto(), config.obtenerPromptConfigurable());
    }
}
