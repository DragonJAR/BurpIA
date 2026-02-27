package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("PanelConsola Preferencias Tests")
class PanelConsolaPreferenciasTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Permite establecer auto-scroll programaticamente")
    void testSetterProgramaticoAutoScroll() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            assertTrue(panel.isAutoScrollActivo());

            SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(false));
            flushEdt();
            assertFalse(panel.isAutoScrollActivo());

            SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(true));
            flushEdt();
            assertTrue(panel.isAutoScrollActivo());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("aplicarIdioma refresca el resumen aunque no cambie la version")
    void testAplicarIdiomaRefrescaResumen() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelConsola panel = crearPanel();
        try {
            JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
            flushEdt();
            assertTrue(etiquetaResumen.getText().contains("Total LOGs"));

            SwingUtilities.invokeAndWait(() -> {
                I18nUI.establecerIdioma("en");
                panel.aplicarIdioma();
            });
            flushEdt();

            assertTrue(etiquetaResumen.getText().contains("Total Logs"));
        } finally {
            panel.destruir();
        }
    }

    private PanelConsola crearPanel() throws Exception {
        final PanelConsola[] holder = new PanelConsola[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelConsola(new GestorConsolaGUI()));
        return holder[0];
    }

    private JLabel obtenerEtiquetaResumen(PanelConsola panel) throws Exception {
        Field field = PanelConsola.class.getDeclaredField("etiquetaResumen");
        field.setAccessible(true);
        return (JLabel) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
