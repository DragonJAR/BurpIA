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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        PanelHallazgos panel = crearPanel(true);

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertTrue(panel.isGuardadoAutomaticoIssuesActivo(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:36");
        assertTrue(checkAutoIssues.isSelected(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:37");

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:41");
        assertFalse(checkAutoIssues.isSelected(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:42");

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:46");
        assertTrue(checkAutoIssues.isSelected(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:47");
    }

    @Test
    @DisplayName("Menu Issues se incluye dinamicamente cuando autoguardado desactivado en Pro")
    void testMenuIssuesVisibleEnMenuDinamicoCuandoAutoguardadoDesactivado() throws Exception {
        PanelHallazgos panel = crearPanel(true);

        Method metodo = PanelHallazgos.class.getDeclaredMethod("obtenerEtiquetaMenuIssues");
        metodo.setAccessible(true);

        String etiqueta = (String) metodo.invoke(panel);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), etiqueta, "assertEquals failed at PanelHallazgosIssuesMenuTest.java:59");

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:63");

        String etiquetaTrasDesactivar = (String) metodo.invoke(panel);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), etiquetaTrasDesactivar, "assertEquals failed at PanelHallazgosIssuesMenuTest.java:66");
    }

    @Test
    @DisplayName("Permite establecer autoguardado de Issues programáticamente")
    void testSetterProgramaticoAutoguardadoIssues() throws Exception {
        PanelHallazgos panel = crearPanel(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:77");
        assertFalse(checkAutoIssues.isSelected(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:78");

        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:82");
        assertTrue(checkAutoIssues.isSelected(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:83");
    }

    @Test
    @DisplayName("Community deshabilita integracion de Issues y muestra etiqueta solo Pro")
    void testCommunityDeshabilitaIntegracionIssues() throws Exception {
        PanelHallazgos panel = crearPanel(false);

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertFalse(checkAutoIssues.isEnabled(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:93");
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:94");

        Method metodoTooltip = PanelHallazgos.class.getDeclaredMethod("obtenerTooltipMenuIssues");
        metodoTooltip.setAccessible(true);
        String tooltip = (String) metodoTooltip.invoke(panel);
        assertEquals(I18nUI.Tooltips.Hallazgos.MENU_ISSUES_SOLO_PRO(), tooltip, "assertEquals failed at PanelHallazgosIssuesMenuTest.java:99");
    }

    @Test
    @DisplayName("Checkbox y etiquetas cambian idioma con aplicarIdioma")
    void testAplicarIdiomaActualizaEtiquetasIssues() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelHallazgos panel = crearPanel(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        String etiquetaInicial = checkAutoIssues.getText();
        assertTrue(etiquetaInicial.contains("Issues") || etiquetaInicial.contains("automáticamente"), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:110");

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        String etiquetaIngles = checkAutoIssues.getText();
        assertTrue(etiquetaIngles.contains("Issues") || etiquetaIngles.contains("automatically"), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:119");
    }

    @Test
    @DisplayName("Setter de autoguardado es seguro fuera del EDT")
    void testSetterAutoguardadoSeguroFueraDelEdt() throws Exception {
        PanelHallazgos panel = crearPanel(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread hilo = new Thread(() -> {
            try {
                panel.establecerGuardadoAutomaticoIssuesActivo(false);
            } catch (Throwable t) {
                error.set(t);
            }
        });
        hilo.start();
        hilo.join(1000);
        flushEdt();

        assertFalse(hilo.isAlive(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:140");
        assertNull(error.get(), "assertNull failed at PanelHallazgosIssuesMenuTest.java:141");
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:142");
        assertFalse(checkAutoIssues.isSelected(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:143");
    }

    /**
     * Crea una instancia de PanelHallazgos con configuración mock.
     *
     * @param esBurpProfessional true para simular Burp Professional, false para Community
     * @return instancia de PanelHallazgos creada en el EDT
     */
    private PanelHallazgos crearPanel(boolean esBurpProfessional) throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), esBurpProfessional));
        return holder[0];
    }

    /**
     * Obtiene el valor de un campo privado mediante reflexión.
     *
     * @param target objeto del que obtener el campo
     * @param nombre nombre del campo
     * @param tipo   clase del tipo esperado
     * @return valor del campo casteado al tipo especificado
     */
    @SuppressWarnings({"unchecked", "PMD.UnusedFormalParameter"})
    private <T> T obtenerCampo(Object target, String nombre, Class<T> tipo) throws Exception {
        Field field = target.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    /**
     * Espera a que todos los eventos pendientes en el EDT sean procesados.
     */
    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
