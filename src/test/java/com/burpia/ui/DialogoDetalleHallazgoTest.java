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
        TestDialogUtils.desregistrarCapturaDialogos();
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
            assertNotNull(guardado.get(), "assertNotNull failed at DialogoDetalleHallazgoTest.java:71");
            assertEquals("https://modificado.com", guardado.get().obtenerUrl(), "assertEquals failed at DialogoDetalleHallazgoTest.java:72");
            assertEquals("Título Modificado", guardado.get().obtenerTitulo(), "assertEquals failed at DialogoDetalleHallazgoTest.java:73");
            assertEquals("Descripción modificada", guardado.get().obtenerHallazgo(), "assertEquals failed at DialogoDetalleHallazgoTest.java:74");
            assertEquals("Critical", guardado.get().obtenerSeveridad(), "assertEquals failed at DialogoDetalleHallazgoTest.java:75"); // Internal value is normalized
            assertEquals("High", guardado.get().obtenerConfianza(), "assertEquals failed at DialogoDetalleHallazgoTest.java:76");
        } finally {
            destruirDialogo(dialogo);
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
            assertFalse(dialogo.isVisible(), "El diálogo debe cerrarse al cancelar");
        } finally {
            destruirDialogo(dialogo);
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
            destruirDialogo(dialogo);
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
                assertEquals("https://original.com", txtUrl.getText(), "assertEquals failed at DialogoDetalleHallazgoTest.java:171");
                assertEquals("Título Original", txtTitulo.getText(), "assertEquals failed at DialogoDetalleHallazgoTest.java:172");
                assertEquals("Descripción original", txtDescripcion.getText(), "assertEquals failed at DialogoDetalleHallazgoTest.java:173");
                assertEquals("Alta", comboSeveridad.getSelectedItem(), "assertEquals failed at DialogoDetalleHallazgoTest.java:174");
                assertEquals("Media", comboConfianza.getSelectedItem(), "assertEquals failed at DialogoDetalleHallazgoTest.java:175");
            });
        } finally {
            destruirDialogo(dialogo);
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
                assertEquals("", txtUrl.getText(), "assertEquals failed at DialogoDetalleHallazgoTest.java:197");
                assertEquals("", txtTitulo.getText(), "assertEquals failed at DialogoDetalleHallazgoTest.java:198");
                assertEquals("Informativa", comboSeveridad.getSelectedItem(), "assertEquals failed at DialogoDetalleHallazgoTest.java:199");
                assertEquals("Media", comboConfianza.getSelectedItem(), "assertEquals failed at DialogoDetalleHallazgoTest.java:200");
            });
        } finally {
            destruirDialogo(dialogo);
        }
    }

    @Test
    @DisplayName("Campos editables exponen tooltips coherentes")
    void testCamposEditablesTienenTooltips() throws Exception {
        Consumer<Hallazgo> alGuardar = h -> {};
        DialogoDetalleHallazgo dialogo = crearDialogo(null, alGuardar);
        try {
            JTextField txtTitulo = obtenerCampo(dialogo, "txtTitulo");

            SwingUtilities.invokeAndWait(() -> assertEquals(
                    I18nUI.Tooltips.DetalleHallazgo.TITULO(),
                    txtTitulo.getToolTipText(),
                    "assertEquals failed at DialogoDetalleHallazgoTest.java:222"));
        } finally {
            destruirDialogo(dialogo);
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
                assertTrue(btnGuardar.getText().contains("Guardar"), "assertTrue failed at DialogoDetalleHallazgoTest.java:228");
                assertTrue(btnCancelar.getText().contains("Cancelar"), "assertTrue failed at DialogoDetalleHallazgoTest.java:229");
            });
        } finally {
            destruirDialogo(dialogo);
        }

        // Arrange & Act - Test con idioma inglés
        I18nUI.establecerIdioma("en");
        DialogoDetalleHallazgo dialogoEn = crearDialogo(original, alGuardar);
        try {
            JButton btnGuardarEn = obtenerBotonGuardar(dialogoEn);
            JButton btnCancelarEn = obtenerBotonCancelar(dialogoEn);

            // Assert - Verificar idioma inglés
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(btnGuardarEn.getText().contains("Save"), "assertTrue failed at DialogoDetalleHallazgoTest.java:244");
                assertTrue(btnCancelarEn.getText().contains("Cancel"), "assertTrue failed at DialogoDetalleHallazgoTest.java:245");
            });
        } finally {
            destruirDialogo(dialogoEn);
        }
    }

    // ========== Helper Methods DRY ==========

    /**
     * Crea un diálogo sin ventana padre (null) para evitar fugas de recursos.
     * El diálogo debe ser destruido con {@link #destruirDialogo(DialogoDetalleHallazgo)}
     * en el bloque finally de cada test.
     */
    private DialogoDetalleHallazgo crearDialogo(Hallazgo hallazgo, Consumer<Hallazgo> alGuardar) throws Exception {
        final DialogoDetalleHallazgo[] holder = new DialogoDetalleHallazgo[1];
        SwingUtilities.invokeAndWait(() ->
            holder[0] = new DialogoDetalleHallazgo(null, hallazgo, alGuardar)
        );
        return holder[0];
    }

    /**
     * Destruye el diálogo de forma segura en el EDT.
     */
    private void destruirDialogo(DialogoDetalleHallazgo dialogo) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            dialogo.setVisible(false);
            dialogo.dispose();
        });
    }

    /**
     * Obtiene un campo privado del diálogo usando reflexión.
     * Uso legítimo en tests donde el caller controla el tipo esperado.
     *
     * @param <T> Tipo del componente (extiende Component)
     * @param dialogo Diálogo del cual extraer el campo
     * @param nombreCampo Nombre del campo a obtener
     * @return Campo castado al tipo T
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> T obtenerCampo(DialogoDetalleHallazgo dialogo, String nombreCampo) throws Exception {
        Field field = DialogoDetalleHallazgo.class.getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (T) field.get(dialogo);
    }

    private JButton obtenerBotonGuardar(DialogoDetalleHallazgo dialogo) throws Exception {
        return obtenerBotonPorTexto(dialogo, "Guardar", "Save");
    }

    private JButton obtenerBotonCancelar(DialogoDetalleHallazgo dialogo) throws Exception {
        return obtenerBotonPorTexto(dialogo, "Cancelar", "Cancel");
    }

    /**
     * Busca un botón por su texto (soporta múltiples idiomas).
     */
    private JButton obtenerBotonPorTexto(DialogoDetalleHallazgo dialogo, String... textosEsperados) throws Exception {
        Component[] components = dialogo.getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                for (Component child : ((JPanel) comp).getComponents()) {
                    if (child instanceof JButton) {
                        String texto = ((JButton) child).getText();
                        for (String esperado : textosEsperados) {
                            if (texto != null && texto.contains(esperado)) {
                                return (JButton) child;
                            }
                        }
                    }
                }
            }
        }
        throw new RuntimeException("No se encontró botón con textos: " + String.join(", ", textosEsperados));
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
