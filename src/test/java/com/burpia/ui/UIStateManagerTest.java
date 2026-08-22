package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("UIStateManager Tests")
class UIStateManagerTest {

    private UIStateManager crearManager() {
        return new UIStateManager(
                new ConfiguracionAPI(),
                GestorLoggingUnificado.crearMinimal(null, null));
    }

    @Test
    @DisplayName("restaurarUltimaPestaniaSeleccionada con tabbedPane vacío no lanza IndexOutOfBounds")
    void testRestaurarPestaniaConTabbedPaneVacioNoLanza() throws Exception {
        UIStateManager manager = crearManager();

        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            assertDoesNotThrow(() -> manager.restaurarUltimaPestaniaSeleccionada(tabs, 0),
                "Con 0 pestañas debe retornar temprano sin llamar setSelectedIndex");
            assertEquals(-1, tabs.getSelectedIndex(),
                "Sin pestañas no debe quedar ninguna selección");
        });
    }

    @Test
    @DisplayName("restaurarUltimaPestaniaSeleccionada usa la pestaña por defecto cuando no hay guardada")
    void testRestaurarPestaniaUsaDefecto() throws Exception {
        UIStateManager manager = crearManager();

        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("A", new JPanel());
            tabs.addTab("B", new JPanel());
            manager.restaurarUltimaPestaniaSeleccionada(tabs, 1);
            assertEquals(1, tabs.getSelectedIndex(),
                "Sin estado guardado debe seleccionarse la pestaña por defecto");
        });
    }
}
