package com.burpia.ui;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("DialogoConfiguracion Timeout por Modelo Tests")
class DialogoConfiguracionTimeoutPorModeloTest {

    private String userHomeOriginal;

    @AfterEach
    void tearDown() {
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
            flushEdt();

            assertTrue(guardadoCallback.get());
            assertEquals(210, config.obtenerTiempoEsperaParaModelo("Z.ai", "glm-5"));

            Path configPath = tempDir.resolve(".burpia.json");
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"tiempoEsperaPorModelo\""));
            assertTrue(json.contains("\"Z.ai::glm-5\": 210"));
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

    private void destruirDialogo(DialogoConfiguracion dialogo) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            dialogo.setVisible(false);
            dialogo.dispose();
        });
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
