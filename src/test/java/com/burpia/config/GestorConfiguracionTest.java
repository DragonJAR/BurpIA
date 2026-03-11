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
 * IMPORTANTE: Este test usa directorio temporal para evitar modificar
 * el archivo de configuración real del usuario en ~/.burpia/config.json
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
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
        RutasBurpIA.limpiarCacheParaTests();
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

        assertTrue(gestor.guardarConfiguracion(config), "assertTrue failed at GestorConfiguracionTest.java:106");

        String json = leerConfigJson();

        assertFalse(json.contains("\"urlApi\""), "assertFalse failed at GestorConfiguracionTest.java:110");
        assertFalse(json.contains("\"claveApi\""), "assertFalse failed at GestorConfiguracionTest.java:111");
        assertFalse(json.contains("\"modelo\""), "assertFalse failed at GestorConfiguracionTest.java:112");
        assertTrue(json.contains("\"apiKeysPorProveedor\""), "assertTrue failed at GestorConfiguracionTest.java:113");
        assertTrue(json.contains("\"modelosPorProveedor\""), "assertTrue failed at GestorConfiguracionTest.java:114");
        assertTrue(json.contains("\"idiomaUi\": \"en\""), "assertTrue failed at GestorConfiguracionTest.java:115");
        assertTrue(json.contains("\"maximoHallazgosTabla\": 2500"), "assertTrue failed at GestorConfiguracionTest.java:116");
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
        assertFalse(config.esDetallado(), "assertFalse failed at GestorConfiguracionTest.java:138");
        assertEquals("en", config.obtenerIdiomaUi(), "assertEquals failed at GestorConfiguracionTest.java:139");
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla(), "assertEquals failed at GestorConfiguracionTest.java:140");
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

        assertTrue(gestor.guardarConfiguracion(config), "assertTrue failed at GestorConfiguracionTest.java:155");

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertFalse(cargada.escaneoPasivoHabilitado(), "assertFalse failed at GestorConfiguracionTest.java:158");
        assertFalse(cargada.autoGuardadoIssuesHabilitado(), "assertFalse failed at GestorConfiguracionTest.java:159");
        assertFalse(cargada.autoScrollConsolaHabilitado(), "assertFalse failed at GestorConfiguracionTest.java:160");
        assertFalse(cargada.alertasClickDerechoEnviarAHabilitadas(), "assertFalse failed at GestorConfiguracionTest.java:161");

        String json = leerConfigJson();
        assertTrue(json.contains("\"escaneoPasivoHabilitado\": false"), "assertTrue failed at GestorConfiguracionTest.java:164");
        assertTrue(json.contains("\"autoGuardadoIssuesHabilitado\": false"), "assertTrue failed at GestorConfiguracionTest.java:165");
        assertTrue(json.contains("\"autoScrollConsolaHabilitado\": false"), "assertTrue failed at GestorConfiguracionTest.java:166");
        assertTrue(json.contains("\"alertasClickDerechoEnviarAHabilitadas\": false"), "assertTrue failed at GestorConfiguracionTest.java:167");
    }

    @Test
    @DisplayName("Guarda y carga prompts de agente inicial y validacion")
    void testPersistenciaPromptsAgente() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgentePreflightPrompt("PRE_FLIGHT_CUSTOM");
        config.establecerAgentePrompt("VALIDACION_CUSTOM");

        assertTrue(gestor.guardarConfiguracion(config), "assertTrue failed at GestorConfiguracionTest.java:180");

        String json = leerConfigJson();
        assertTrue(json.contains("\"agentePreflightPrompt\": \"PRE_FLIGHT_CUSTOM\""), "assertTrue failed at GestorConfiguracionTest.java:183");
        assertTrue(json.contains("\"agentePrompt\": \"VALIDACION_CUSTOM\""), "assertTrue failed at GestorConfiguracionTest.java:184");

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals("PRE_FLIGHT_CUSTOM", cargada.obtenerAgentePreflightPrompt(), "assertEquals failed at GestorConfiguracionTest.java:187");
        assertEquals("VALIDACION_CUSTOM", cargada.obtenerAgentePrompt(), "assertEquals failed at GestorConfiguracionTest.java:188");
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

        assertEquals(ConfiguracionAPI.AGENTE_DELAY_DEFECTO_MS, cargada.obtenerAgenteDelay(), "assertEquals failed at GestorConfiguracionTest.java:208");
    }

    @Test
    @DisplayName("agenteDelay en JSON se normaliza al rango permitido")
    void testAgenteDelayJsonSeNormalizaAlRangoPermitido() throws Exception {
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

        assertEquals(ConfiguracionAPI.AGENTE_DELAY_MAXIMO_MS, cargada.obtenerAgenteDelay(),
            "assertEquals failed at GestorConfiguracionTest.java:229");
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

        assertTrue(gestor.guardarConfiguracion(config), "assertTrue failed at GestorConfiguracionTest.java:243");

        String json = leerConfigJson();
        assertTrue(json.contains("\"tiempoEsperaPorModelo\""), "assertTrue failed at GestorConfiguracionTest.java:246");
        assertTrue(json.contains("\"Z.ai::glm-5\": 180"), "assertTrue failed at GestorConfiguracionTest.java:247");
        assertTrue(json.contains("\"OpenAI::gpt-5-mini\": 240"), "assertTrue failed at GestorConfiguracionTest.java:248");

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals(180, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"), "assertEquals failed at GestorConfiguracionTest.java:251");
        assertEquals(240, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"), "assertEquals failed at GestorConfiguracionTest.java:252");
        assertEquals(90, cargada.obtenerTiempoEsperaParaModelo("Z.ai", "glm-4-air"), "assertEquals failed at GestorConfiguracionTest.java:253");
    }

    @Test
    @DisplayName("Guarda y carga máximo de tareas en tabla")
    void testPersistenciaMaximoTareasTabla() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoTareasTabla(876);

        assertTrue(gestor.guardarConfiguracion(config), "assertTrue failed at GestorConfiguracionTest.java:maximoTareas:guardar");

        String json = leerConfigJson();
        assertTrue(json.contains("\"maximoTareasTabla\": 876"),
                "assertTrue failed at GestorConfiguracionTest.java:maximoTareas:json");

        ConfiguracionAPI cargada = gestor.cargarConfiguracion();
        assertEquals(876, cargada.obtenerMaximoTareasTabla(),
                "assertEquals failed at GestorConfiguracionTest.java:maximoTareas:carga");
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

        assertTrue(cargada.obtenerTiempoEsperaPorModelo().isEmpty(), "assertTrue failed at GestorConfiguracionTest.java:274");
        assertEquals(120, cargada.obtenerTiempoEsperaParaModelo("OpenAI", "gpt-5-mini"), "assertEquals failed at GestorConfiguracionTest.java:275");
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
        assertTrue(log.contains("[Configuration]"), "assertTrue failed at GestorConfiguracionTest.java:289");
        assertTrue(log.contains("Configuration path"), "assertTrue failed at GestorConfiguracionTest.java:290");
    }

    @Test
    @DisplayName("Guardar configuración nula devuelve error controlado")
    void testGuardarConfiguracionNula() throws Exception {
        configurarDirectorioTemporalComoHome();

        GestorConfiguracion gestor = new GestorConfiguracion();
        StringBuilder mensajeError = new StringBuilder();

        assertFalse(gestor.guardarConfiguracion(null, mensajeError), "assertFalse failed at GestorConfiguracionTest.java:301");
        assertNotNull(mensajeError.toString(), "assertNotNull failed at GestorConfiguracionTest.java:302");
        assertTrue(mensajeError.toString().toLowerCase().contains("null")
            || mensajeError.toString().toLowerCase().contains("nula"), "assertTrue failed at GestorConfiguracionTest.java:303");
        assertFalse(Files.exists(configDir.resolve("config.json")), "assertFalse failed at GestorConfiguracionTest.java:305");
        assertFalse(Files.exists(configDir.resolve("config.json.tmp")), "assertFalse failed at GestorConfiguracionTest.java:306");
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

        assertFalse(config.esPromptModificado(), "assertFalse failed at GestorConfiguracionTest.java:328");
        assertEquals(ConfiguracionAPI.obtenerPromptPorDefecto(), config.obtenerPromptConfigurable(), "assertEquals failed at GestorConfiguracionTest.java:329");
    }

    @Test
    @DisplayName("Carga configuración con proveedor desconocido, conserva defaults vigentes y descarta valores inválidos")
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

        assertEquals("Ollama", cargada.obtenerProveedorAI(), "assertEquals failed at GestorConfiguracionTest.java:352");
        assertEquals("", cargada.obtenerApiKeyParaProveedor("-- Custom --"), "assertEquals failed at GestorConfiguracionTest.java:353");
        assertNull(cargada.obtenerUrlBaseGuardadaParaProveedor("-- Custom --"), "assertNull failed at GestorConfiguracionTest.java:354");
        assertEquals("", cargada.obtenerModeloParaProveedor("-- Custom --"), "assertEquals failed at GestorConfiguracionTest.java:355");
        assertEquals(4096, cargada.obtenerMaxTokensParaProveedor("-- Custom --"), "assertEquals failed at GestorConfiguracionTest.java:356");
        assertNull(cargada.obtenerTiempoEsperaConfiguradoParaModelo("-- Custom --", "legacy-model"), "assertNull failed at GestorConfiguracionTest.java:357");
        assertEquals(1, cargada.obtenerProveedoresMultiConsulta().size(), "assertEquals failed at GestorConfiguracionTest.java:358");
        assertEquals("OpenAI", cargada.obtenerProveedoresMultiConsulta().get(0), "assertEquals failed at GestorConfiguracionTest.java:359");

        assertTrue(gestor.guardarConfiguracion(cargada), "assertTrue failed at GestorConfiguracionTest.java:361");
        String jsonMigrado = leerConfigJson();
        assertFalse(jsonMigrado.contains("\"-- Custom --\""), "assertFalse failed at GestorConfiguracionTest.java:363");
    }
}
