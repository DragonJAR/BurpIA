package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nUI;
import com.burpia.util.OSUtils;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Path homeAislado = Files.createTempDirectory(tempDir, "home-");
        burpiaDir = homeAislado.resolve(".burpia");
        Files.createDirectories(burpiaDir);
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
    @DisplayName("Cada agente conserva su check independiente al cambiar el combo")
    void testCadaAgenteConservaCheckIndependienteAlCambiarCombo() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
        config.establecerAgenteHabilitado(AgenteTipo.CLAUDE_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.GEMINI_CLI.name(), false);

        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
            assertNotNull(comboAgente, "assertNotNull failed at ConfigDialogControllerAgentesTest.java:201");
            assertNotNull(chkAgenteHabilitado, "assertNotNull failed at ConfigDialogControllerAgentesTest.java:202");

            SwingUtilities.invokeAndWait(() -> {
                comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
                assertTrue(chkAgenteHabilitado.isSelected(),
                    "assertTrue failed at ConfigDialogControllerAgentesTest.java:206");

                comboAgente.setSelectedItem(AgenteTipo.GEMINI_CLI.name());
                assertFalse(chkAgenteHabilitado.isSelected(),
                    "assertFalse failed at ConfigDialogControllerAgentesTest.java:210");
            });
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

    @Test
    @DisplayName("Prioriza agente con binario resoluble por la ruta por defecto")
    void testPriorizaAgenteDisponiblePorRutaPorDefecto() throws IOException {
        Path rutaDefault = Path.of(OSUtils.resolverEjecutableComando(AgenteTipo.CLAUDE_CODE.obtenerRutaPorDefecto()));
        Files.createDirectories(rutaDefault.getParent());
        Files.writeString(rutaDefault, "#!/bin/bash\necho 'claude-default'");
        rutaDefault.toFile().setExecutable(true);

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
        crearArchivoConfiguracion(config);

        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = new DialogoConfiguracion(
            null, new ConfiguracionAPI(), gestor, () -> {});

        try {
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            assertNotNull(comboAgente);

            assertEquals(AgenteTipo.CLAUDE_CODE.name(), comboAgente.getItemAt(0),
                "Los agentes disponibles por ruta por defecto deben priorizarse");
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), comboAgente.getSelectedItem(),
                "El agente configurado debe conservarse seleccionado");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Guardar acepta agente habilitado con ruta expandible por tilde")
    void testGuardarAceptaBinarioAgenteConTildeYArgumentos() throws Exception {
        Path binarioClaude = crearBinarioClaudeEnHomeActual();

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);
        DialogoConfiguracion dialogo = new DialogoConfiguracion(null, config, gestor, () -> guardadoCallback.set(true));

        try {
            completarFormularioMinimoGuardado(
                dialogo,
                "~/.local/bin/claude --dangerously-skip-permissions"
            );

            ejecutarGuardado(dialogo, guardadoCallback);

            assertTrue(guardadoCallback.get(), "assertTrue failed at ConfigDialogControllerAgentesTest.java:270");
            assertFalse(dialogo.isDisplayable(), "assertFalse failed at ConfigDialogControllerAgentesTest.java:271");
            assertTrue(config.agenteHabilitado(), "assertTrue failed at ConfigDialogControllerAgentesTest.java:272");
            assertEquals(
                "~/.local/bin/claude --dangerously-skip-permissions",
                config.obtenerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name()),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:273"
            );
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Guardar bloquea agente habilitado cuando la ruta expandida del binario no existe")
    void testGuardarBloqueaBinarioAgenteInexistente() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);
        DialogoConfiguracion dialogo = new DialogoConfiguracion(null, config, gestor, () -> guardadoCallback.set(true));

        try {
            completarFormularioMinimoGuardado(
                dialogo,
                "~/.local/bin/binario-claude-no-existe --dangerously-skip-permissions"
            );

            TestDialogUtils.reiniciarDialogosMensajeCapturados();
            ejecutarGuardado(dialogo);

            TestDialogUtils.DialogoMensajeCapturado dialogoCapturado =
                TestDialogUtils.obtenerUltimoDialogoMensajeCapturado();
            assertFalse(guardadoCallback.get(), "assertFalse failed at ConfigDialogControllerAgentesTest.java:293");
            assertTrue(dialogo.isDisplayable(), "assertTrue failed at ConfigDialogControllerAgentesTest.java:294");
            assertFalse(config.agenteHabilitado(), "assertFalse failed at ConfigDialogControllerAgentesTest.java:295");
            assertNotNull(dialogoCapturado, "assertNotNull failed at ConfigDialogControllerAgentesTest.java:296");
            assertEquals(I18nUI.Configuracion.TITULO_ERROR_GUARDAR(), dialogoCapturado.obtenerTitulo(),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:297");
            assertTrue(dialogoCapturado.obtenerMensaje().contains("binario-claude-no-existe"),
                "assertTrue failed at ConfigDialogControllerAgentesTest.java:299");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Guardar permite agente invalido deshabilitado sin mostrar mensaje cuando todos quedan deshabilitados")
    void testGuardarPermiteAgenteInvalidoDeshabilitadoSinMostrarMensaje() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);
        DialogoConfiguracion dialogo = new DialogoConfiguracion(null, config, gestor, () -> guardadoCallback.set(true));
        String comandoInvalido = "~/.local/bin/binario-claude-no-existe --dangerously-skip-permissions";

        try {
            completarFormularioMinimoGuardado(
                dialogo,
                comandoInvalido,
                false
            );

            TestDialogUtils.reiniciarDialogosMensajeCapturados();
            ejecutarGuardado(dialogo, guardadoCallback);

            assertTrue(guardadoCallback.get(), "assertTrue failed at ConfigDialogControllerAgentesTest.java:360");
            assertFalse(dialogo.isDisplayable(), "assertFalse failed at ConfigDialogControllerAgentesTest.java:361");
            assertFalse(TestDialogUtils.seCapturoDialogoMensaje(),
                "assertFalse failed at ConfigDialogControllerAgentesTest.java:362");
            for (AgenteTipo tipo : AgenteTipo.values()) {
                assertFalse(config.agenteHabilitado(tipo.name()),
                    "assertFalse failed at ConfigDialogControllerAgentesTest.java:365");
            }
            assertFalse(config.agenteHabilitado(), "assertFalse failed at ConfigDialogControllerAgentesTest.java:367");
            assertEquals(comandoInvalido, config.obtenerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name()),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:369");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Guardar no muestra error si el agente seleccionado es invalido pero queda deshabilitado")
    void testGuardarNoMuestraErrorSiAgenteSeleccionadoInvalidoQuedaDeshabilitado() throws Exception {
        Path binarioClaude = crearBinarioClaudeEnHomeActual();
        String comandoGeminiInvalido = "~/.local/bin/binario-gemini-no-existe --sandbox";

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);
        DialogoConfiguracion dialogo = new DialogoConfiguracion(null, config, gestor, () -> guardadoCallback.set(true));

        try {
            JComboBox<String> comboProveedor = dialogo.obtenerComboProveedor();
            JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
            JTextField txtTimeoutModelo = dialogo.obtenerTxtTimeoutModelo();
            JPasswordField txtClave = dialogo.obtenerTxtClave();
            JTextField txtRetraso = dialogo.obtenerTxtRetraso();
            JTextField txtMaximoConcurrente = dialogo.obtenerTxtMaximoConcurrente();
            JTextField txtMaximoHallazgosTabla = dialogo.obtenerTxtMaximoHallazgosTabla();
            JTextField txtMaximoTareas = dialogo.obtenerTxtMaximoTareas();
            JTextField txtMaxTokens = dialogo.obtenerTxtMaxTokens();
            JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
            JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();

            SwingUtilities.invokeAndWait(() -> {
                comboProveedor.setSelectedItem("Z.ai");
                comboModelo.setSelectedItem("glm-5");
                comboModelo.getEditor().setItem("glm-5");
                txtClave.setText("test-key");
                txtRetraso.setText("5");
                txtMaximoConcurrente.setText("3");
                txtMaximoHallazgosTabla.setText("1000");
                txtMaximoTareas.setText("500");
                txtMaxTokens.setText("4096");
                txtTimeoutModelo.setText("180");

                comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
                chkAgenteHabilitado.setSelected(true);
                txtAgenteBinario.setText(binarioClaude.toString());

                comboAgente.setSelectedItem(AgenteTipo.GEMINI_CLI.name());
                chkAgenteHabilitado.setSelected(false);
                txtAgenteBinario.setText(comandoGeminiInvalido);
            });
            flushEdt();

            TestDialogUtils.reiniciarDialogosMensajeCapturados();
            ejecutarGuardado(dialogo, guardadoCallback);

            assertTrue(guardadoCallback.get(), "assertTrue failed at ConfigDialogControllerAgentesTest.java:421");
            assertFalse(TestDialogUtils.seCapturoDialogoMensaje(),
                "assertFalse failed at ConfigDialogControllerAgentesTest.java:422");
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), config.obtenerTipoAgente(),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:424");
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), config.obtenerTipoAgenteOperativo(),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:426");
            assertTrue(config.agenteHabilitado(AgenteTipo.CLAUDE_CODE.name()),
                "assertTrue failed at ConfigDialogControllerAgentesTest.java:428");
            assertFalse(config.agenteHabilitado(AgenteTipo.GEMINI_CLI.name()),
                "assertFalse failed at ConfigDialogControllerAgentesTest.java:430");
            assertEquals(comandoGeminiInvalido, config.obtenerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name()),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:432");

            ConfiguracionAPI recargada = gestor.cargarConfiguracion();
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), recargada.obtenerTipoAgente(),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:435");
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), recargada.obtenerTipoAgenteOperativo(),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:437");
            assertTrue(recargada.agenteHabilitado(AgenteTipo.CLAUDE_CODE.name()),
                "assertTrue failed at ConfigDialogControllerAgentesTest.java:439");
            assertFalse(recargada.agenteHabilitado(AgenteTipo.GEMINI_CLI.name()),
                "assertFalse failed at ConfigDialogControllerAgentesTest.java:441");
            assertEquals(comandoGeminiInvalido, recargada.obtenerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name()),
                "assertEquals failed at ConfigDialogControllerAgentesTest.java:443");
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
        json.append("  \"agentesHabilitadosPorTipo\": {\n");

        Map<String, Boolean> estadosAgentes = config.obtenerEstadosHabilitacionAgentes();
        count = 0;
        for (Map.Entry<String, Boolean> entry : estadosAgentes.entrySet()) {
            if (count > 0) json.append(",\n");
            json.append("    \"").append(entry.getKey()).append("\": ")
                .append(Boolean.TRUE.equals(entry.getValue()));
            count++;
        }

        json.append("\n  },\n");
        json.append("  \"tipoAgente\": \"").append(config.obtenerTipoAgente()).append("\"\n");
        json.append("}");
        
        Files.writeString(configFile, json.toString(), StandardCharsets.UTF_8);
    }

    private void completarFormularioMinimoGuardado(DialogoConfiguracion dialogo, String comandoBinario) throws Exception {
        completarFormularioMinimoGuardado(dialogo, comandoBinario, true);
    }

    private void completarFormularioMinimoGuardado(DialogoConfiguracion dialogo,
                                                   String comandoBinario,
                                                   boolean agenteHabilitado) throws Exception {
        JComboBox<String> comboProveedor = dialogo.obtenerComboProveedor();
        JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
        JTextField txtTimeoutModelo = dialogo.obtenerTxtTimeoutModelo();
        JPasswordField txtClave = dialogo.obtenerTxtClave();
        JTextField txtRetraso = dialogo.obtenerTxtRetraso();
        JTextField txtMaximoConcurrente = dialogo.obtenerTxtMaximoConcurrente();
        JTextField txtMaximoHallazgosTabla = dialogo.obtenerTxtMaximoHallazgosTabla();
        JTextField txtMaximoTareas = dialogo.obtenerTxtMaximoTareas();
        JTextField txtMaxTokens = dialogo.obtenerTxtMaxTokens();
        JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
        JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
        JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();

        SwingUtilities.invokeAndWait(() -> {
            comboProveedor.setSelectedItem("Z.ai");
            comboModelo.setSelectedItem("glm-5");
            comboModelo.getEditor().setItem("glm-5");
            txtClave.setText("test-key");
            txtRetraso.setText("5");
            txtMaximoConcurrente.setText("3");
            txtMaximoHallazgosTabla.setText("1000");
            txtMaximoTareas.setText("500");
            txtMaxTokens.setText("4096");
            txtTimeoutModelo.setText("180");
            comboAgente.setSelectedItem(AgenteTipo.CLAUDE_CODE.name());
            chkAgenteHabilitado.setSelected(agenteHabilitado);
            txtAgenteBinario.setText(comandoBinario);
        });
        flushEdt();
    }

    private void ejecutarGuardado(DialogoConfiguracion dialogo) throws Exception {
        SwingUtilities.invokeAndWait(dialogo::guardarConfiguracion);
        long inicio = System.currentTimeMillis();
        long timeoutMs = 5000;
        int dialogosIniciales = TestDialogUtils.contarDialogosMensajeCapturados();
        while (dialogo.isDisplayable()
                && TestDialogUtils.contarDialogosMensajeCapturados() == dialogosIniciales
                && (System.currentTimeMillis() - inicio) < timeoutMs) {
            Thread.sleep(50);
            flushEdt();
        }
        flushEdt();
    }

    private void ejecutarGuardado(DialogoConfiguracion dialogo,
                                  AtomicBoolean guardadoCallback) throws Exception {
        SwingUtilities.invokeAndWait(dialogo::guardarConfiguracion);
        long inicio = System.currentTimeMillis();
        long timeoutMs = 5000;
        while (!guardadoCallback.get() && (System.currentTimeMillis() - inicio) < timeoutMs) {
            Thread.sleep(50);
            flushEdt();
        }
        flushEdt();
    }

    private Path crearBinarioClaudeEnHomeActual() throws IOException {
        Path homeActual = Path.of(System.getProperty("user.home"));
        Path binarioClaude = homeActual.resolve(".local").resolve("bin").resolve("claude");
        Files.createDirectories(binarioClaude.getParent());
        Files.writeString(binarioClaude, "#!/bin/bash\necho 'claude'");
        binarioClaude.toFile().setExecutable(true);
        return binarioClaude;
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
