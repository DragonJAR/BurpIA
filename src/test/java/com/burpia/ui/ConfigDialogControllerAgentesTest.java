package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComboBox;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigDialogController - Validación de Agentes")
class ConfigDialogControllerAgentesTest {

    @TempDir
    Path tempDir;

    private Path burpiaDir;
    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        RutasBurpIA.limpiarCacheParaTests();
        TestDialogUtils.registrarCapturaDialogos();
        
        originalUserHome = System.getProperty("user.home");
        burpiaDir = tempDir.resolve(".burpia");
        Files.createDirectories(burpiaDir);
        System.setProperty("user.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.desregistrarCapturaDialogos();
        RutasBurpIA.limpiarCacheParaTests();
        
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    @DisplayName("Muestra todos los agentes para permitir configuración y reparación")
    void testMuestraTodosLosAgentesParaConfiguracion() throws IOException {
        Path binarioValido = burpiaDir.resolve("droid");
        Files.writeString(binarioValido, "#!/bin/bash\necho 'test'");
        binarioValido.toFile().setExecutable(true);

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerRutaBinarioAgente("FACTORY_DROID", binarioValido.toString());
        config.establecerRutaBinarioAgente("CLAUDE_CODE", "/ruta/inexistente/claude");
        config.establecerRutaBinarioAgente("GEMINI_CLI", "");

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});
        
        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            assertNotNull(comboAgente);
            
            assertEquals(AgenteTipo.values().length, comboAgente.getItemCount(),
                "El combo debe exponer todos los agentes para permitir su configuración");
            assertEquals("FACTORY_DROID", comboAgente.getItemAt(0),
                "Los agentes disponibles deben priorizarse al inicio");

            boolean tieneClaudeCode = false;
            boolean tieneGeminiCli = false;
            for (int i = 0; i < comboAgente.getItemCount(); i++) {
                String item = comboAgente.getItemAt(i);
                if ("CLAUDE_CODE".equals(item)) {
                    tieneClaudeCode = true;
                }
                if ("GEMINI_CLI".equals(item)) {
                    tieneGeminiCli = true;
                }
            }

            assertTrue(tieneClaudeCode, "Debe mostrar CLAUDE_CODE aunque requiera reparación de ruta");
            assertTrue(tieneGeminiCli, "Debe mostrar GEMINI_CLI aunque aún no tenga binario configurado");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Combo no queda vacío si ningún agente tiene binario válido")
    void testComboMantieneOpcionesSiNingunAgenteTieneBinario() throws IOException {
        ConfiguracionAPI config = new ConfiguracionAPI();
        for (AgenteTipo tipo : AgenteTipo.values()) {
            config.establecerRutaBinarioAgente(tipo.name(), "/ruta/falsa/que/no/existe");
        }

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});
        
        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            assertNotNull(comboAgente);
            
            assertEquals(AgenteTipo.values().length, comboAgente.getItemCount(),
                "El combo debe seguir mostrando agentes para poder configurar rutas desde cero");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Mantiene seleccionado el agente configurado aunque su binario requiera corrección")
    void testMantieneAgenteConfiguradoParaPermitirReparacion() throws IOException {
        Path binarioClaude = burpiaDir.resolve("claude");
        Files.writeString(binarioClaude, "#!/bin/bash\necho 'claude'");
        binarioClaude.toFile().setExecutable(true);

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente("FACTORY_DROID");
        config.establecerRutaBinarioAgente("FACTORY_DROID", "/no/existe/droid");
        config.establecerRutaBinarioAgente("CLAUDE_CODE", binarioClaude.toString());

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});
        
        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            assertNotNull(comboAgente);
            
            assertTrue(comboAgente.getItemCount() > 0, "Debería haber al menos un agente disponible");
            
            String seleccionado = (String) comboAgente.getSelectedItem();
            assertNotNull(seleccionado, "Debería haber un agente seleccionado");
            assertEquals("FACTORY_DROID", seleccionado,
                "Debe conservar FACTORY_DROID seleccionado para permitir reparar su ruta");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Mantiene agente configurado si tiene binario válido")
    void testMantieneAgenteConfiguradoValido() throws IOException {
        Path binarioDroid = burpiaDir.resolve("droid");
        Files.writeString(binarioDroid, "#!/bin/bash\necho 'droid'");
        binarioDroid.toFile().setExecutable(true);

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente("FACTORY_DROID");
        config.establecerRutaBinarioAgente("FACTORY_DROID", binarioDroid.toString());

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});
        
        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            assertNotNull(comboAgente);
            
            String seleccionado = (String) comboAgente.getSelectedItem();
            assertEquals("FACTORY_DROID", seleccionado, 
                "Debería mantener FACTORY_DROID seleccionado ya que tiene binario válido");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Conserva rutas de agentes con argumentos en la UI")
    void testConservaRutasConArgumentosEnLaUI() throws IOException {
        Path binarioClaude = burpiaDir.resolve("claude");
        Files.writeString(binarioClaude, "#!/bin/bash\necho 'claude'");
        binarioClaude.toFile().setExecutable(true);

        String rutaConArgumentos = binarioClaude.toString() + " --dangerously-skip-permissions";
        
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente("CLAUDE_CODE");
        config.establecerRutaBinarioAgente("CLAUDE_CODE", rutaConArgumentos);

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});
        
        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            javax.swing.JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
            assertNotNull(comboAgente);
            assertNotNull(txtAgenteBinario);
            
            assertEquals("CLAUDE_CODE", comboAgente.getSelectedItem(),
                "Debe conservar seleccionado el agente configurado");
            assertEquals(rutaConArgumentos, txtAgenteBinario.getText(),
                "La ruta con argumentos debe mostrarse íntegra para poder editarla");
        } finally {
            dialogo.dispose();
        }
    }

    private void crearArchivoConfiguracion(ConfiguracionAPI config) throws IOException {
        Path configFile = burpiaDir.resolve("config.json");
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"rutasBinarioPorAgente\": {\n");
        
        Map<String, String> rutas = config.obtenerTodasLasRutasBinario();
        int count = 0;
        for (Map.Entry<String, String> entry : rutas.entrySet()) {
            if (count > 0) json.append(",\n");
            json.append("    \"").append(entry.getKey()).append("\": \"")
                .append(entry.getValue().replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\"");
            count++;
        }
        
        json.append("\n  },\n");
        json.append("  \"tipoAgente\": \"").append(config.obtenerTipoAgente()).append("\"\n");
        json.append("}");
        
        Files.writeString(configFile, json.toString(), StandardCharsets.UTF_8);
    }
}
