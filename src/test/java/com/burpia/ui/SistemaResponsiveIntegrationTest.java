package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nUI;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sistema Responsive - Cadena reactiva del diálogo de configuración")
class SistemaResponsiveIntegrationTest {

    @TempDir
    Path tempDir;

    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        RutasBurpIA.limpiarCacheParaTests();
        TestDialogUtils.registrarCapturaDialogos();
        originalUserHome = System.getProperty("user.home");
        Path homeAislado = Files.createTempDirectory(tempDir, "home-");
        Files.createDirectories(homeAislado.resolve(".burpia"));
        System.setProperty("user.home", homeAislado.toString());
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
    @DisplayName("Seleccionar CUSTOM_AGENT actualiza el checkbox con su nombre visible")
    void seleccionarCustomAgentActualizaCheckbox() throws Exception {
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();

            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
            });

            String textoCheckbox = chkAgenteHabilitado.getText();
            assertTrue(textoCheckbox.contains("Custom Agent"),
                "El checkbox debe mostrar 'Custom Agent', fue: " + textoCheckbox);
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("CUSTOM_AGENT aparece en el combo con ruta vacía por defecto")
    void customAgentApareceConRutaVaciaPorDefecto() throws Exception {
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();

            boolean encontrado = false;
            for (int i = 0; i < comboAgente.getItemCount(); i++) {
                if (AgenteTipo.CUSTOM_AGENT.name().equals(comboAgente.getItemAt(i))) {
                    encontrado = true;
                    break;
                }
            }
            assertTrue(encontrado, "CUSTOM_AGENT debe estar en el combo");

            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
            });

            assertEquals("", txtAgenteBinario.getText(),
                "CUSTOM_AGENT debe mostrar ruta vacía (sin ruta por defecto)");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Cambiar entre agentes preserva la ruta de cada uno independientemente")
    void cambiarEntreAgentesPreservaRutasIndependientes() throws Exception {
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();

            // 1. Seleccionar CUSTOM_AGENT y escribir una ruta
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
                txtAgenteBinario.setText("/usr/local/bin/mi-agente --flag");
            });

            // 2. Cambiar a CLAUDE_CODE (debe mostrar su propia ruta)
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
            });
            String rutaClaude = txtAgenteBinario.getText();
            assertTrue(rutaClaude.contains("claude"),
                "CLAUDE_CODE debe mostrar su ruta por defecto con 'claude', fue: " + rutaClaude);

            // 3. Volver a CUSTOM_AGENT (debe preservar la ruta que escribimos)
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
            });
            assertEquals("/usr/local/bin/mi-agente --flag", txtAgenteBinario.getText(),
                "Al volver a CUSTOM_AGENT debe restaurar la ruta escrita por el usuario");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Cambiar entre agentes preserva el estado de habilitación de cada uno")
    void cambiarEntreAgentesPreservaHabilitacionIndependiente() throws Exception {
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();

            // 1. CLAUDE_CODE: habilitar
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
                chkAgenteHabilitado.setSelected(true);
            });

            // 2. CUSTOM_AGENT: deshabilitar explícitamente
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
                chkAgenteHabilitado.setSelected(false);
            });

            // 3. Volver a CLAUDE_CODE → debe seguir habilitado
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
            });
            assertTrue(chkAgenteHabilitado.isSelected(),
                "CLAUDE_CODE debe conservar su estado habilitado tras cambiar de agente");

            // 4. Volver a CUSTOM_AGENT → debe seguir deshabilitado
            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CUSTOM_AGENT.name());
            });
            assertFalse(chkAgenteHabilitado.isSelected(),
                "CUSTOM_AGENT debe conservar su estado deshabilitado tras cambiar de agente");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Todos los agentes del combo pueden seleccionarse sin excepciones")
    void todosLosAgentesSeleccionablesSinExcepciones() throws Exception {
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
            JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();

            for (AgenteTipo tipo : AgenteTipo.values()) {
                assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
                    comboAgente.setSelectedItem(tipo.name());
                }), "Seleccionar " + tipo.name() + " no debe lanzar excepción");

                // El checkbox debe contener el nombre visible del agente
                assertTrue(chkAgenteHabilitado.getText().contains(tipo.obtenerNombreVisible()),
                    "El checkbox debe mostrar el nombre visible de " + tipo.name()
                        + ", fue: " + chkAgenteHabilitado.getText());

                // La ruta debe ser editable (no null)
                assertNotNull(txtAgenteBinario.getText(),
                    "La ruta de " + tipo.name() + " no debe ser null");
            }
        } finally {
            dialogo.dispose();
        }
    }
}
