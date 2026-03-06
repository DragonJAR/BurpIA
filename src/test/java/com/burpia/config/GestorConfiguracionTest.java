package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests para GestorConfiguracion.
 * <p>
 * Verifica la persistencia y carga de configuración, incluyendo:
 * - Preferencias runtime de usuario
 * - Prompts de agente
 * - Timeouts por modelo
 * - Localización de logs
 * </p>
 */
@DisplayName("GestorConfiguracion Tests")
class GestorConfiguracionTest {

    @TempDir
    Path tempDir;

    private String userHomeOriginal;
    private Path configDir;

    @BeforeEach
    void setUp() {
        RutasBurpIA.limpiarCacheParaTests();
        userHomeOriginal = System.getProperty("user.home");
    }

    @AfterEach
    void tearDown() {
        restaurarUserHome();
        I18nUI.establecerIdioma("es");
    }

    /**
     * Helper para configurar el directorio temporal como home del usuario.
     * Centraliza la lógica de setup para tests que manipulan configuración.
     */
    private void configurarDirectorioTemporalComoHome() {
        System.setProperty("user.home", tempDir.toString());
        configDir = tempDir.resolve(".burpia");
    }

    /**
     * Helper para restaurar el user.home original.
     * Garantiza limpieza incluso si el test falla.
     */
    private void restaurarUserHome() {
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
    }

    /**
     * Helper para escribir un archivo JSON de configuración.
     */
    private void escribirConfigJson(String json) throws IOException {
        Files.createDirectories(configDir);
        Path configPath = configDir.resolve("config.json");
        Files.writeString(configPath, json, StandardCharsets.UTF_8);
    }

    /**
     * Helper para leer el contenido del archivo de configuración.
     */
    private String leerConfigJson() throws IOException {
        Path configPath = configDir.resolve("config.json");
        return Files.readString(configPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Guarda configuración sin campos obsoletos de proveedor")
    void testGuardarSinCamposObsoletos() throws Exception {
        configurarDirectorioTemporalComoHome();

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

        String json = leerConfigJson();

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
        configurarDirectorioTemporalComoHome();

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
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = gestor.cargarConfiguracion();
        assertFalse(config.esDetallado());
        assertEquals("en", config.obtenerIdiomaUi());
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla());
    }

    @Test
    @DisplayName("Guarda y carga preferencias runtime de usuario")
    void testPersistenciaPreferenciasRuntimeUsuario() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerEscaneoPasivoHabilitado(false);
        config.establecerAutoGuardadoIssuesHabilitado(false);
        config.establecerAutoScrollConsolaHabilitado(false);
        config.establecerAlertasClickDerechoEnviarAHabilitadas(false);

        assertTrue(gestor.guardarConfiguracion(config));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertFalse(cargada.escaneoPasivoHabilitado());
        assertFalse(cargada.autoGuardadoIssuesHabilitado());
        assertFalse(cargada.autoScrollConsolaHabilitado());
        assertFalse(cargada.alertasClickDerechoEnviarAHabilitadas());

        String json = leerConfigJson();
        assertTrue(json.contains("\"escaneoPasivoHabilitado\": false"));
        assertTrue(json.contains("\"autoGuardadoIssuesHabilitado\": false"));
        assertTrue(json.contains("\"autoScrollConsolaHabilitado\": false"));
        assertTrue(json.contains("\"alertasClickDerechoEnviarAHabilitadas\": false"));
    }

    @Test
    @DisplayName("Guarda y carga prompts de agente inicial y validacion")
    void testPersistenciaPromptsAgente() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgentePreflightPrompt("PRE_FLIGHT_CUSTOM");
        config.establecerAgentePrompt("VALIDACION_CUSTOM");

        assertTrue(gestor.guardarConfiguracion(config));

        String json = leerConfigJson();
        assertTrue(json.contains("\"agentePreflightPrompt\": \"PRE_FLIGHT_CUSTOM\""));
        assertTrue(json.contains("\"agentePrompt\": \"VALIDACION_CUSTOM\""));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals("PRE_FLIGHT_CUSTOM", cargada.obtenerAgentePreflightPrompt());
        assertEquals("VALIDACION_CUSTOM", cargada.obtenerAgentePrompt());
    }

    @Test
    @DisplayName("Sin agenteDelay en JSON usa el valor por defecto")
    void testAgenteDelayUsaDefaultCuandoNoExisteEnJson() throws Exception {
        configurarDirectorioTemporalComoHome();

        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-4o\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI cargada = gestor.cargarConfiguracion();

        assertEquals(ConfiguracionAPI.AGENTE_DELAY_DEFECTO_MS, cargada.obtenerAgenteDelay());
    }

    @Test
    @DisplayName("agenteDelay en JSON respeta exactamente el valor del usuario")
    void testAgenteDelayJsonRespetaValorUsuario() throws Exception {
        configurarDirectorioTemporalComoHome();

        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"agenteDelay\": 75000,\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-4o\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI cargada = gestor.cargarConfiguracion();

        assertEquals(75000, cargada.obtenerAgenteDelay());
    }

    @Test
    @DisplayName("Guarda y carga timeout por modelo en JSON")
    void testPersistenciaTimeoutPorModelo() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTiempoEsperaAI(90);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);
        config.establecerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini", 240);

        assertTrue(gestor.guardarConfiguracion(config));

        String json = leerConfigJson();
        assertTrue(json.contains("\"tiempoEsperaPorModelo\""));
        assertTrue(json.contains("\"Z.ai::glm-5\": 180"));
        assertTrue(json.contains("\"OpenAI::gpt-5-mini\": 240"));

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals(180, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));
        assertEquals(240, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"));
        assertEquals(90, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-4-air"));
    }

    @Test
    @DisplayName("JSON sin timeout por modelo mantiene fallback global")
    void testJsonSinTimeoutPorModeloMantieneFallback() throws Exception {
        configurarDirectorioTemporalComoHome();

        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"tiempoEsperaAI\": 120,\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-5-mini\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI cargada = gestor.cargarConfiguracion();

        assertTrue(cargada.obtenerTiempoEsperaPorModelo().isEmpty());
        assertEquals(120, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"));
    }

    @Test
    @DisplayName("Logs de configuración se localizan a inglés cuando idioma UI es EN")
    void testLogsConfiguracionEnIngles() {
        // Este test no requiere manipular user.home, solo verificar localización
        I18nUI.establecerIdioma("en");
        StringWriter salida = new StringWriter();
        StringWriter errores = new StringWriter();

        new GestorConfiguracion(new PrintWriter(salida, true), new PrintWriter(errores, true));

        String log = salida.toString();
        assertTrue(log.contains("[Configuration]"));
        assertTrue(log.contains("Configuration path"));
    }

    @Test
    @DisplayName("Guardar configuración nula devuelve error controlado")
    void testGuardarConfiguracionNula() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        StringBuilder mensajeError = new StringBuilder();

        assertFalse(gestor.guardarConfiguracion(null, mensajeError));
        assertNotNull(mensajeError.toString());
        assertTrue(mensajeError.toString().toLowerCase().contains("null")
            || mensajeError.toString().toLowerCase().contains("nula"));
        assertFalse(Files.exists(configDir.resolve("config.json")));
        assertFalse(Files.exists(configDir.resolve("config.json.tmp")));
    }

    @Test
    @DisplayName("Si prompt no esta modificado se aplica prompt por defecto vigente al cargar")
    void testPromptNoModificadoUsaDefaultVigente() throws Exception {
        configurarDirectorioTemporalComoHome();

        String json = "{\n" +
            "  \"proveedorAI\": \"OpenAI\",\n" +
            "  \"promptConfigurable\": \"PROMPT_ANTIGUO\",\n" +
            "  \"promptModificado\": false,\n" +
            "  \"apiKeysPorProveedor\": {\"OpenAI\": \"\"},\n" +
            "  \"urlsBasePorProveedor\": {\"OpenAI\": \"https://api.openai.com/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"OpenAI\": \"gpt-4o\"},\n" +
            "  \"maxTokensPorProveedor\": {\"OpenAI\": 4096}\n" +
            "}";
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = gestor.cargarConfiguracion();

        assertFalse(config.esPromptModificado());
        assertEquals(ConfiguracionAPI.obtenerPromptPorDefecto(), config.obtenerPromptConfigurable());
    }

    @Test
    @DisplayName("Carga configuración con proveedor desconocido y descarta valores inválidos")
    void testCargaProveedorDesconocidoDescartaValoresInvalidos() throws Exception {
        configurarDirectorioTemporalComoHome();

        String json = "{\n" +
            "  \"proveedorAI\": \"-- Custom --\",\n" +
            "  \"apiKeysPorProveedor\": {\"-- Custom --\": \"legacy-key\"},\n" +
            "  \"urlsBasePorProveedor\": {\"-- Custom --\": \"https://legacy.local/v1\"},\n" +
            "  \"modelosPorProveedor\": {\"-- Custom --\": \"legacy-model\"},\n" +
            "  \"maxTokensPorProveedor\": {\"-- Custom --\": 3500},\n" +
            "  \"tiempoEsperaPorModelo\": {\"-- Custom --::legacy-model\": 210},\n" +
            "  \"multiProveedorHabilitado\": true,\n" +
            "  \"proveedoresMultiConsulta\": [\"-- Custom --\", \"OpenAI\"]\n" +
            "}";
        escribirConfigJson(json);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI cargada = gestor.cargarConfiguracion();

        assertEquals("Z.ai", cargada.obtenerProveedorAI());
        assertEquals("", cargada.obtenerApiKeyParaProveedor("-- Custom --"));
        assertNull(cargada.obtenerUrlBaseGuardadaParaProveedor("-- Custom --"));
        assertEquals("", cargada.obtenerModeloParaProveedor("-- Custom --"));
        assertEquals(4096, cargada.obtenerMaxTokensParaProveedor("-- Custom --"));
        assertNull(cargada.obtenerTiempoEsperaConfiguradoParaModelo("-- Custom --", "legacy-model"));
        assertEquals(1, cargada.obtenerProveedoresMultiConsulta().size());
        assertEquals("OpenAI", cargada.obtenerProveedoresMultiConsulta().get(0));

        assertTrue(gestor.guardarConfiguracion(cargada));
        String jsonMigrado = leerConfigJson();
        assertFalse(jsonMigrado.contains("\"-- Custom --\""));
    }
}
