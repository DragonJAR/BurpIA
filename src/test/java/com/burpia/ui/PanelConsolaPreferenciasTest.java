package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PanelConsola Preferencias Tests")
class PanelConsolaPreferenciasTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Permite establecer auto-scroll programáticamente")
    void testSetterProgramaticoAutoScroll() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            assertTrue(panel.isAutoScrollActivo(), "assertTrue failed at PanelConsolaPreferenciasTest.java:33");

            SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(false));
            flushEdt();
            assertFalse(panel.isAutoScrollActivo(), "assertFalse failed at PanelConsolaPreferenciasTest.java:37");

            SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(true));
            flushEdt();
            assertTrue(panel.isAutoScrollActivo(), "assertTrue failed at PanelConsolaPreferenciasTest.java:41");
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
            assertTrue(etiquetaResumen.getText().contains("Total:"), "assertTrue failed at PanelConsolaPreferenciasTest.java:55");

            SwingUtilities.invokeAndWait(() -> {
                I18nUI.establecerIdioma("en");
                panel.aplicarIdioma();
            });
            flushEdt();

            assertTrue(etiquetaResumen.getText().contains("Total:"), "assertTrue failed at PanelConsolaPreferenciasTest.java:63");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("aplicarTema mantiene contraste legible en resumen y consola")
    void testAplicarTemaMantieneContraste() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
            JTextPane consola = obtenerConsola(panel);

            SwingUtilities.invokeAndWait(panel::aplicarTema);
            flushEdt();

            Color fondoPanel = panel.getBackground();
            assertTrue(EstilosUI.ratioContraste(etiquetaResumen.getForeground(), fondoPanel) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at PanelConsolaPreferenciasTest.java:81");
            assertTrue(EstilosUI.ratioContraste(consola.getForeground(), consola.getBackground()) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at PanelConsolaPreferenciasTest.java:82");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Limpiar consola refleja resumen en cero sin esperar nuevos logs")
    void testLimpiarConsolaActualizaResumenCero() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();

            SwingUtilities.invokeAndWait(() -> gestor.limpiarConsola());
            flushEdt();

            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            flushEdt();

            assertTrue(etiquetaResumen.getText().contains("Total: 0"), "assertTrue failed at PanelConsolaPreferenciasTest.java:102");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Setter de auto-scroll es seguro fuera del EDT")
    void testSetterAutoScrollFueraDelEdt() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread hilo = new Thread(() -> {
                try {
                    panel.establecerAutoScrollActivo(false);
                } catch (Throwable t) {
                    error.set(t);
                }
            }, "PanelConsola-AutoScroll-Test");
            hilo.start();
            hilo.join(2000);
            flushEdt();

            assertNull(error.get(), "assertNull failed at PanelConsolaPreferenciasTest.java:125");
            assertFalse(panel.isAutoScrollActivo(), "assertFalse failed at PanelConsolaPreferenciasTest.java:126");
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

    private JTextPane obtenerConsola(PanelConsola panel) throws Exception {
        Field field = PanelConsola.class.getDeclaredField("consola");
        field.setAccessible(true);
        return (JTextPane) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
