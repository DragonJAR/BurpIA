package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import com.burpia.ui.TestDialogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DialogoDetalleHallazgo Tests")
class DialogoDetalleHallazgoTest {

    @BeforeEach
    void setUp() {
        TestDialogUtils.registrarCapturaDialogos();
    }

    @AfterEach
    void resetIdioma() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.deregistrarCapturaDialogos();
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Guardar modifica todos los campos del hallazgo")
    void testGuardarModificaTodosLosCampos() throws Exception {
        // Arrange
        Hallazgo original = new Hallazgo(
            "https://original.com",
            "Título Original",
            "Descripción original",
            "High",
            "Medium"
        );

        AtomicReference<Hallazgo> guardado = new AtomicReference<>();
        Consumer<Hallazgo> alGuardar = guardado::set;

        DialogoDetalleHallazgo dialogo = crearDialogo(original, alGuardar);
        try {
            // Act
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl");
            JTextField txtTitulo = obtenerCampo(dialogo, "txtTitulo");
            JTextArea txtDescripcion = obtenerCampo(dialogo, "txtDescripcion");
            JComboBox<String> comboSeveridad = obtenerCampo(dialogo, "comboSeveridad");
            JComboBox<String> comboConfianza = obtenerCampo(dialogo, "comboConfianza");

            SwingUtilities.invokeAndWait(() -> {
                txtUrl.setText("https://modificado.com");
                txtTitulo.setText("Título Modificado");
                txtDescripcion.setText("Descripción modificada");
                comboSeveridad.setSelectedItem("Crítica");
                comboConfianza.setSelectedItem("Alta");
            });

            JButton btnGuardar = obtenerBotonGuardar(dialogo);
            SwingUtilities.invokeAndWait(btnGuardar::doClick);
            flushEdt();

            // Assert
            assertNotNull(guardado.get());
            assertEquals("https://modificado.com", guardado.get().obtenerUrl());
            assertEquals("Título Modificado", guardado.get().obtenerTitulo());
            assertEquals("Descripción modificada", guardado.get().obtenerHallazgo());
            assertEquals("Critical", guardado.get().obtenerSeveridad()); // Internal value is normalized
            assertEquals("High", guardado.get().obtenerConfianza());
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Cancelar descarta cambios sin llamar alGuardar")
    void testCancelarDescartaCambios() throws Exception {
        // Arrange
        Hallazgo original = new Hallazgo(
            "https://original.com",
            "Título Original",
            "Descripción original",
            "High",
            "Medium"
        );

        AtomicBoolean guardadoLlamado = new AtomicBoolean(false);
        Consumer<Hallazgo> alGuardar = h -> guardadoLlamado.set(true);

        DialogoDetalleHallazgo dialogo = crearDialogo(original, alGuardar);
        try {
            // Act
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl");
            SwingUtilities.invokeAndWait(() -> txtUrl.setText("https://cambiado.com"));

            JButton btnCancelar = obtenerBotonCancelar(dialogo);
            SwingUtilities.invokeAndWait(btnCancelar::doClick);
            flushEdt();

            // Assert
            assertFalse(guardadoLlamado.get(), "alGuardar no debe ser llamado al cancelar");
            assertTrue(dialogo.isVisible() == false || dialogo.isActive() == false,
                "El diálogo debe cerrarse al cancelar");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Validación rechaza campos vacíos")
    void testValidacionRechacaCamposVacios() throws Exception {
        // Arrange
        Hallazgo original = new Hallazgo(
            "https://original.com",
            "Título Original",
            "Descripción original",
            "High",
            "Medium"
        );

        AtomicReference<Hallazgo> guardado = new AtomicReference<>();
        Consumer<Hallazgo> alGuardar = guardado::set;

        DialogoDetalleHallazgo dialogo = crearDialogo(original, alGuardar);
        try {
            // Act
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl");
            JButton btnGuardar = obtenerBotonGuardar(dialogo);

            SwingUtilities.invokeAndWait(() -> txtUrl.setText(""));
            SwingUtilities.invokeAndWait(btnGuardar::doClick);
            flushEdt();

            // Assert
            assertNull(guardado.get(), "alGuardar no debe ser llamado con campos vacíos");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Campos se cargan correctamente desde hallazgo existente")
    void testCargarDatosDesdeHallazgoExistente() throws Exception {
        // Arrange
        Hallazgo original = new Hallazgo(
            "https://original.com",
            "Título Original",
            "Descripción original",
            "High",
            "Medium"
        );

        Consumer<Hallazgo> alGuardar = h -> {};
        DialogoDetalleHallazgo dialogo = crearDialogo(original, alGuardar);
        try {
            // Act
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl");
            JTextField txtTitulo = obtenerCampo(dialogo, "txtTitulo");
            JTextArea txtDescripcion = obtenerCampo(dialogo, "txtDescripcion");
            JComboBox<String> comboSeveridad = obtenerCampo(dialogo, "comboSeveridad");
            JComboBox<String> comboConfianza = obtenerCampo(dialogo, "comboConfianza");

            // Assert
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("https://original.com", txtUrl.getText());
                assertEquals("Título Original", txtTitulo.getText());
                assertEquals("Descripción original", txtDescripcion.getText());
                assertEquals("Alta", comboSeveridad.getSelectedItem());
                assertEquals("Media", comboConfianza.getSelectedItem());
            });
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Diálogo sin hallazgo carga valores por defecto")
    void testDialogoSinHallazgoCargaValoresPorDefecto() throws Exception {
        // Arrange
        Consumer<Hallazgo> alGuardar = h -> {};
        DialogoDetalleHallazgo dialogo = crearDialogo(null, alGuardar);
        try {
            // Act
            JTextField txtUrl = obtenerCampo(dialogo, "txtUrl");
            JTextField txtTitulo = obtenerCampo(dialogo, "txtTitulo");
            JComboBox<String> comboSeveridad = obtenerCampo(dialogo, "comboSeveridad");
            JComboBox<String> comboConfianza = obtenerCampo(dialogo, "comboConfianza");

            // Assert
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("", txtUrl.getText());
                assertEquals("", txtTitulo.getText());
                assertEquals("Informativa", comboSeveridad.getSelectedItem());
                assertEquals("Media", comboConfianza.getSelectedItem());
            });
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Diálogo usa idioma configurado al crearse")
    void testInternacionalizacion() throws Exception {
        // Arrange & Act - Test con idioma español
        I18nUI.establecerIdioma("es");
        Hallazgo original = new Hallazgo(
            "https://test.com",
            "Test",
            "Test",
            "High",
            "Medium"
        );

        Consumer<Hallazgo> alGuardar = h -> {};
        DialogoDetalleHallazgo dialogo = crearDialogo(original, alGuardar);
        try {
            JButton btnGuardar = obtenerBotonGuardar(dialogo);
            JButton btnCancelar = obtenerBotonCancelar(dialogo);

            // Assert - Verificar idioma español
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(btnGuardar.getText().contains("Guardar"));
                assertTrue(btnCancelar.getText().contains("Cancelar"));
            });
        } finally {
            dialogo.dispose();
        }

        // Arrange & Act - Test con idioma inglés
        I18nUI.establecerIdioma("en");
        DialogoDetalleHallazgo dialogoEn = crearDialogo(original, alGuardar);
        try {
            JButton btnGuardarEn = obtenerBotonGuardar(dialogoEn);
            JButton btnCancelarEn = obtenerBotonCancelar(dialogoEn);

            // Assert - Verificar idioma inglés
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(btnGuardarEn.getText().contains("Save"));
                assertTrue(btnCancelarEn.getText().contains("Cancel"));
            });
        } finally {
            dialogoEn.dispose();
        }
    }

    // ========== Helper Methods DRY ==========

    private DialogoDetalleHallazgo crearDialogo(Hallazgo hallazgo, Consumer<Hallazgo> alGuardar) throws Exception {
        final DialogoDetalleHallazgo[] holder = new DialogoDetalleHallazgo[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame ventanaPadre = new JFrame();
            ventanaPadre.setVisible(true);
            holder[0] = new DialogoDetalleHallazgo(ventanaPadre, hallazgo, alGuardar);
        });
        return holder[0];
    }

    private <T extends Component> T obtenerCampo(DialogoDetalleHallazgo dialogo, String nombreCampo) throws Exception {
        Field field = DialogoDetalleHallazgo.class.getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (T) field.get(dialogo);
    }

    private JButton obtenerBotonGuardar(DialogoDetalleHallazgo dialogo) throws Exception {
        JButton[] botones = obtenerBotonesDialogo(dialogo);
        return botones[0]; // Guardar es el primer botón
    }

    private JButton obtenerBotonCancelar(DialogoDetalleHallazgo dialogo) throws Exception {
        JButton[] botones = obtenerBotonesDialogo(dialogo);
        return botones[1]; // Cancelar es el segundo botón
    }

    private JButton[] obtenerBotonesDialogo(DialogoDetalleHallazgo dialogo) throws Exception {
        // Buscar botones en el panel sur (BorderLayout.SOUTH)
        Component[] components = dialogo.getContentPane().getComponents();
        JPanel panelBotones = null;
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                // Verificar si es el panel de botones (FlowLayout con botones)
                for (Component child : panel.getComponents()) {
                    if (child instanceof JButton) {
                        panelBotones = panel;
                        break;
                    }
                }
            }
        }

        if (panelBotones == null) {
            throw new RuntimeException("No se encontró el panel de botones");
        }

        JButton guardar = null;
        JButton cancelar = null;
        for (Component comp : panelBotones.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (guardar == null) {
                    guardar = btn;
                } else {
                    cancelar = btn;
                    break;
                }
            }
        }

        return new JButton[]{guardar, cancelar};
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
