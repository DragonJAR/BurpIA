package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;





@DisplayName("PanelHallazgos Issues Menu Tests")
class PanelHallazgosIssuesMenuTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Toggle de guardado automatico sincroniza estado interno y checkbox")
    void testToggleGuardadoAutomaticoSincronizaEstado() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel(true);

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertTrue(checkAutoIssues.isSelected());

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());
        assertFalse(checkAutoIssues.isSelected());

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertTrue(checkAutoIssues.isSelected());
    }

    @Test
    @DisplayName("Menu Issues se incluye dinamicamente cuando autoguardado desactivado en Pro")
    void testMenuIssuesVisibleEnMenuDinamicoCuandoAutoguardadoDesactivado() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel(true);

        Method metodo = PanelHallazgos.class.getDeclaredMethod("obtenerEtiquetaMenuIssues");
        metodo.setAccessible(true);

        String etiqueta = (String) metodo.invoke(panel);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), etiqueta);

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());

        String etiquetaTrasDesactivar = (String) metodo.invoke(panel);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), etiquetaTrasDesactivar);
    }

    @Test
    @DisplayName("Permite establecer autoguardado de Issues programaticamente")
    void testSetterProgramaticoAutoguardadoIssues() throws Exception {
        PanelHallazgos panel = crearPanel(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());
        assertFalse(checkAutoIssues.isSelected());

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
        assertTrue(checkAutoIssues.isSelected());
    }

    @Test
    @DisplayName("Community deshabilita integracion de Issues y muestra etiqueta solo Pro")
    void testCommunityDeshabilitaIntegracionIssues() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel(false);

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertFalse(checkAutoIssues.isEnabled());
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());

        Method metodoTooltip = PanelHallazgos.class.getDeclaredMethod("obtenerTooltipMenuIssues");
        metodoTooltip.setAccessible(true);
        String tooltip = (String) metodoTooltip.invoke(panel);
        assertEquals(I18nUI.Tooltips.Hallazgos.MENU_ISSUES_SOLO_PRO(), tooltip);
    }

    @Test
    @DisplayName("Checkbox y etiquetas cambian idioma con aplicarIdioma")
    void testAplicarIdiomaActualizaEtiquetasIssues() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        String etiquetaInicial = checkAutoIssues.getText();
        assertTrue(etiquetaInicial.contains("Issues") || etiquetaInicial.contains("automáticamente"));

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        String etiquetaIngles = checkAutoIssues.getText();
        assertTrue(etiquetaIngles.contains("Issues") || etiquetaIngles.contains("automatically"));
    }

    private PanelHallazgos crearPanel(boolean esBurpProfessional) throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), esBurpProfessional));
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
