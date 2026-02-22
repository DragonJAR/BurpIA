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

        Path configPath = tempDir.resolve(".burpia.json");
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

        Path configPath = tempDir.resolve(".burpia.json");
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
        Files.writeString(configPath, json, StandardCharsets.UTF_8);

        GestorConfiguracion gestor = new GestorConfiguracion();
        ConfiguracionAPI config = gestor.cargarConfiguracion();
        assertFalse(config.esDetallado());
        assertEquals("en", config.obtenerIdiomaUi());
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla());
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
}
