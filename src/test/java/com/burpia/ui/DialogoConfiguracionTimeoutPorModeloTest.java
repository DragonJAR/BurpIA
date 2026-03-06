package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nUI;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("DialogoConfiguracion Timeout por Modelo Tests")
class DialogoConfiguracionTimeoutPorModeloTest {

    private String userHomeOriginal;

    @BeforeEach
    void setUp() {
        RutasBurpIA.limpiarCacheParaTests();
        TestDialogUtils.registrarCapturaDialogos();
    }

    @AfterEach
    void tearDown() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.desregistrarCapturaDialogos();
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
    }

    @Test
    @DisplayName("Al cambiar proveedor/modelo se muestra timeout correspondiente")
    void testCargaTimeoutPorModeloEnCampoUI() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-dialogo-timeout");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");
        config.establecerApiKeyParaProveedor("Z.ai", "test-key");
        config.establecerTiempoEsperaAI(60);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-5", 180);
        config.establecerTiempoEsperaParaModelo("Z.ai", "glm-4-air", 120);

        GestorConfiguracion gestor = new GestorConfiguracion();

        DialogoConfiguracion dialogo = crearDialogo(config, gestor, () -> {});
        try {
            JComboBox<String> comboModelo = obtenerComboString(dialogo, "comboModelo");
            JTextField txtTimeoutModelo = obtenerCampo(dialogo, "txtTimeoutModelo", JTextField.class);

            SwingUtilities.invokeAndWait(() -> comboModelo.setSelectedItem("glm-5"));
            flushEdt();
            assertEquals("180", txtTimeoutModelo.getText());

            SwingUtilities.invokeAndWait(() -> comboModelo.setSelectedItem("glm-4-air"));
            flushEdt();
            assertEquals("120", txtTimeoutModelo.getText());
        } finally {
            destruirDialogo(dialogo);
        }
    }

    @Test
    @DisplayName("Guardar ajustes persiste timeout del modelo seleccionado")
    void testGuardarPersisteTimeoutPorModelo() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-dialogo-timeout-save");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);

        DialogoConfiguracion dialogo = crearDialogo(config, gestor, () -> guardadoCallback.set(true));
        try {
            JComboBox<String> comboProveedor = obtenerComboString(dialogo, "comboProveedor");
            JComboBox<String> comboModelo = obtenerComboString(dialogo, "comboModelo");
            JTextField txtTimeoutModelo = obtenerCampo(dialogo, "txtTimeoutModelo", JTextField.class);
            JPasswordField txtClave = obtenerCampo(dialogo, "txtClave", JPasswordField.class);
            JTextField txtRetraso = obtenerCampo(dialogo, "txtRetraso", JTextField.class);
            JTextField txtMaximoConcurrente = obtenerCampo(dialogo, "txtMaximoConcurrente", JTextField.class);
            JTextField txtMaximoHallazgosTabla = obtenerCampo(dialogo, "txtMaximoHallazgosTabla", JTextField.class);
            JTextField txtMaxTokens = obtenerCampo(dialogo, "txtMaxTokens", JTextField.class);

            SwingUtilities.invokeAndWait(() -> {
                comboProveedor.setSelectedItem("Z.ai");
                comboModelo.setSelectedItem("glm-5");
                comboModelo.getEditor().setItem("glm-5");
                txtClave.setText("test-key");
                txtRetraso.setText("5");
                txtMaximoConcurrente.setText("3");
                txtMaximoHallazgosTabla.setText("1000");
                txtMaxTokens.setText("4096");
                txtTimeoutModelo.setText("210");
            });
            flushEdt();

            Method guardar = DialogoConfiguracion.class.getDeclaredMethod("guardarConfiguracion");
            guardar.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    guardar.invoke(dialogo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            esperarAlGuardado(guardadoCallback);
            assertTrue(guardadoCallback.get(), "El callback de guardado debería haberse ejecutado");
            assertEquals(210, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));

            Path configPath = tempDir.resolve(".burpia/config.json");
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"tiempoEsperaPorModelo\""));
            assertTrue(json.contains("\"Z.ai::glm-5\": 210"));
        } finally {
            destruirDialogo(dialogo);
        }
    }

    @Test
    @DisplayName("Pestaña agentes permite editar y guardar prompt inicial y prompt de validación")
    void testGuardarPromptsAgenteDesdeUI() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-dialogo-prompts-agente");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");
        config.establecerModeloParaProveedor("Z.ai", "glm-5");

        GestorConfiguracion gestor = new GestorConfiguracion();
        AtomicBoolean guardadoCallback = new AtomicBoolean(false);

        DialogoConfiguracion dialogo = crearDialogo(config, gestor, () -> guardadoCallback.set(true));
        try {
            JComboBox<String> comboProveedor = obtenerComboString(dialogo, "comboProveedor");
            JComboBox<String> comboModelo = obtenerComboString(dialogo, "comboModelo");
            JTextField txtTimeoutModelo = obtenerCampo(dialogo, "txtTimeoutModelo", JTextField.class);
            JPasswordField txtClave = obtenerCampo(dialogo, "txtClave", JPasswordField.class);
            JTextField txtRetraso = obtenerCampo(dialogo, "txtRetraso", JTextField.class);
            JTextField txtMaximoConcurrente = obtenerCampo(dialogo, "txtMaximoConcurrente", JTextField.class);
            JTextField txtMaximoHallazgosTabla = obtenerCampo(dialogo, "txtMaximoHallazgosTabla", JTextField.class);
            JTextField txtMaxTokens = obtenerCampo(dialogo, "txtMaxTokens", JTextField.class);
            JTextArea txtAgentePromptInicial = obtenerCampo(dialogo, "txtAgentePromptInicial", JTextArea.class);
            JTextArea txtAgentePrompt = obtenerCampo(dialogo, "txtAgentePrompt", JTextArea.class);

            SwingUtilities.invokeAndWait(() -> {
                comboProveedor.setSelectedItem("Z.ai");
                comboModelo.setSelectedItem("glm-5");
                comboModelo.getEditor().setItem("glm-5");
                txtClave.setText("test-key");
                txtRetraso.setText("5");
                txtMaximoConcurrente.setText("3");
                txtMaximoHallazgosTabla.setText("1000");
                txtMaxTokens.setText("4096");
                txtTimeoutModelo.setText("180");
                txtAgentePromptInicial.setText("PRE_FLIGHT_EDITADO");
                txtAgentePrompt.setText("PROMPT_VALIDACION_EDITADO");
            });
            flushEdt();

            Method guardar = DialogoConfiguracion.class.getDeclaredMethod("guardarConfiguracion");
            guardar.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    guardar.invoke(dialogo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            esperarAlGuardado(guardadoCallback);
            assertTrue(guardadoCallback.get(), "El callback de guardado debería haberse ejecutado");
            assertEquals("PRE_FLIGHT_EDITADO", config.obtenerAgentePreflightPrompt());
            assertEquals("PROMPT_VALIDACION_EDITADO", config.obtenerAgentePrompt());
        } finally {
            destruirDialogo(dialogo);
        }
    }

    @Test
    @DisplayName("Pestaña Agentes usa layout compacto sin scroll global")
    void testPestaniaAgentesSinScrollGlobal() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-dialogo-agentes-layout");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ConfiguracionAPI config = new ConfiguracionAPI();
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = crearDialogo(config, gestor, () -> {});

        try {
            final Component[] panelAgentesHolder = new Component[1];
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabsPrincipal = buscarPrimerComponente(dialogo.getContentPane(), JTabbedPane.class);
                assertNotNull(tabsPrincipal, "El diálogo debe contener pestañas principales");
                int indiceAgentes = tabsPrincipal.indexOfTab(I18nUI.Configuracion.TAB_AGENTES());
                assertTrue(indiceAgentes >= 0, "La pestaña Agentes debe existir");
                panelAgentesHolder[0] = tabsPrincipal.getComponentAt(indiceAgentes);
            });

            Component panelAgentes = panelAgentesHolder[0];
            assertNotNull(panelAgentes, "La vista de Agentes no puede ser nula");
            assertFalse(contieneComponente(panelAgentes, JScrollPane.class),
                    "La pestaña Agentes no debe usar scroll global");
            assertTrue(contieneComponente(panelAgentes, JTabbedPane.class),
                    "Los prompts de agente deben estar organizados en subpestañas");
        } finally {
            destruirDialogo(dialogo);
        }
    }

    private DialogoConfiguracion crearDialogo(ConfiguracionAPI config,
                                              GestorConfiguracion gestor,
                                              Runnable alGuardar) throws Exception {
        final DialogoConfiguracion[] holder = new DialogoConfiguracion[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new DialogoConfiguracion(null, config, gestor, alGuardar));
        return holder[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T obtenerCampo(Object target, String nombreCampo, Class<T> tipo) throws Exception {
        Field field = target.getClass().getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> obtenerComboString(Object target, String nombreCampo) throws Exception {
        return (JComboBox<String>) obtenerCampo(target, nombreCampo, JComboBox.class);
    }

    @Test
    @DisplayName("Los cambios no guardados se mantienen como borrador al cambiar de proveedor")
    void testBorradorPersisteAlCambiarProveedor() throws Exception {
        Path tempDir = Files.createTempDirectory("burpia-borrador-test");
        userHomeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com");
        
        GestorConfiguracion gestor = new GestorConfiguracion();
        DialogoConfiguracion dialogo = crearDialogo(config, gestor, () -> {});

        try {
            JComboBox<String> comboProveedor = obtenerComboString(dialogo, "comboProveedor");
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl", JTextField.class);

            // 1. Modificar OpenAI
            SwingUtilities.invokeAndWait(() -> {
                comboProveedor.setSelectedItem("OpenAI");
                txtUrl.setText("https://mi-proxy.com");
            });
            flushEdt();

            // 2. Cambiar a Gemini (debería disparar el guardado del borrador de OpenAI)
            SwingUtilities.invokeAndWait(() -> comboProveedor.setSelectedItem("Gemini"));
            flushEdt();

            // 3. Volver a OpenAI
            SwingUtilities.invokeAndWait(() -> comboProveedor.setSelectedItem("OpenAI"));
            flushEdt();

            // 4. Verificar que se mantuvo el cambio
            assertEquals("https://mi-proxy.com", txtUrl.getText(), "El URL modificado de OpenAI debería persistir como borrador");

        } finally {
            destruirDialogo(dialogo);
        }
    }

    /**
     * Espera a que el callback de guardado se ejecute, con timeout y procesamiento del EDT.
     * Utiliza un bucle con espera activa corta para evitar condiciones de carrera.
     *
     * @param guardadoCallback Bandera atómica que indica si el guardado se completó
     * @throws Exception si ocurre un error durante la espera
     */
    private void esperarAlGuardado(AtomicBoolean guardadoCallback) throws Exception {
        long inicio = System.currentTimeMillis();
        long timeoutMs = 5000;
        while (!guardadoCallback.get() && (System.currentTimeMillis() - inicio) < timeoutMs) {
            Thread.sleep(50);
            flushEdt();
        }
    }

    private void destruirDialogo(DialogoConfiguracion dialogo) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            dialogo.setVisible(false);
            dialogo.dispose();
        });
    }

    private <T extends JComponent> T buscarPrimerComponente(Container raiz, Class<T> tipo) {
        for (Component componente : raiz.getComponents()) {
            if (tipo.isInstance(componente)) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container) {
                T encontrado = buscarPrimerComponente((Container) componente, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private boolean contieneComponente(Component raiz, Class<? extends Component> tipo) {
        if (tipo.isInstance(raiz)) {
            return true;
        }
        if (raiz instanceof Container) {
            for (Component hijo : ((Container) raiz).getComponents()) {
                if (contieneComponente(hijo, tipo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fuerza el procesamiento de todos los eventos pendientes en el EDT.
     * El cuerpo vacío del Runnable es intencional: invokeAndWait bloquea hasta
     * que todos los eventos pendientes se procesen, sincronizando el estado de la UI.
     */
    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Cuerpo vacío intencional: fuerza el procesamiento de eventos pendientes
        });
    }
}
