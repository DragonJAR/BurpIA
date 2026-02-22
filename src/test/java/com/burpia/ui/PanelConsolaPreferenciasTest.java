package com.burpia.ui;

import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PanelConsola Preferencias Tests")
class PanelConsolaPreferenciasTest {

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

    private PanelConsola crearPanel() throws Exception {
        final PanelConsola[] holder = new PanelConsola[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelConsola(new GestorConsolaGUI()));
        return holder[0];
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
