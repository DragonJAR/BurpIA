package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("PanelHallazgos Issues Menu Tests")
class PanelHallazgosIssuesMenuTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Menu de Issues solo visible cuando autoguardado esta desactivado")
    void testMenuIssuesVisibleSoloConAutoguardadoDesactivado() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel();

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);
        JMenuItem menuItemIssues = obtenerCampo(panel, "menuItemIssues", JMenuItem.class);

        assertNotNull(menuItemIssues);
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertFalse(menuItemIssues.isVisible());

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());
        assertTrue(menuItemIssues.isVisible());

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertFalse(menuItemIssues.isVisible());
    }

    @Test
    @DisplayName("Menu de Issues cambia idioma con aplicarIdioma")
    void testMenuIssuesRespetaIdiomaConfigurado() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel();

        JMenuItem menuItemIssues = obtenerCampo(panel, "menuItemIssues", JMenuItem.class);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), menuItemIssues.getText());

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), menuItemIssues.getText());
    }

    @Test
    @DisplayName("Permite establecer autoguardado de Issues programaticamente")
    void testSetterProgramaticoAutoguardadoIssues() throws Exception {
        PanelHallazgos panel = crearPanel();
        JMenuItem menuItemIssues = obtenerCampo(panel, "menuItemIssues", JMenuItem.class);

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());
        assertTrue(menuItemIssues.isVisible());

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertFalse(menuItemIssues.isVisible());
    }

    private PanelHallazgos crearPanel() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), false));
        return holder[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T obtenerCampo(Object target, String nombre, Class<T> tipo) throws Exception {
        Field field = target.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
