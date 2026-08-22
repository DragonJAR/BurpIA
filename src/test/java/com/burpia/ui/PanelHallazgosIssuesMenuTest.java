package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PanelHallazgos Issues Menu Tests")
class PanelHallazgosIssuesMenuTest extends PanelTestBase {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Toggle de guardado automatico sincroniza estado interno y checkbox")
    void testToggleGuardadoAutomaticoSincronizaEstado() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(true);

        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:36");
        assertFalse(checkAutoIssues.isSelected(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:37");

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:41");
        assertTrue(checkAutoIssues.isSelected(), "assertTrue failed at PanelHallazgosIssuesMenuTest.java:42");

        SwingUtilities.invokeAndWait(checkAutoIssues::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:46");
        assertFalse(checkAutoIssues.isSelected(), "assertFalse failed at PanelHallazgosIssuesMenuTest.java:47");
    }

    @Test
    @DisplayName("Etiqueta del menu Issues es la estandar en Pro y 'solo Pro' en Community")
    void testEtiquetaMenuIssuesDependeDeEdicionBurp() throws Exception {
        Method metodo = PanelHallazgos.class.getDeclaredMethod("obtenerEtiquetaMenuIssues");
        metodo.setAccessible(true);

        PanelHallazgos panelPro = crearPanelHallazgos(true);
        String etiquetaPro = (String) metodo.invoke(panelPro);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES(), etiquetaPro,
            "En Burp Professional la etiqueta debe ser la estándar de envío a Issues");

        PanelHallazgos panelCommunity = crearPanelHallazgos(false);
        String etiquetaCommunity = (String) metodo.invoke(panelCommunity);
        assertEquals(I18nUI.Hallazgos.MENU_ENVIAR_ISSUES_SOLO_PRO(), etiquetaCommunity,
            "En Community la etiqueta debe indicar que requiere Burp Professional");
    }

    @Test
    @DisplayName("Permite establecer autoguardado de Issues programáticamente")
    void testSetterProgramaticoAutoguardadoIssues() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(true);
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
        PanelHallazgos panel = crearPanelHallazgos(false);

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
        PanelHallazgos panel = crearPanelHallazgos(true);
        JCheckBox checkAutoIssues = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        String etiquetaEspaniol = checkAutoIssues.getText();
        assertEquals(I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES(), etiquetaEspaniol,
            "La etiqueta inicial debe ser la versión en español de CHECK_GUARDAR_ISSUES");

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals(I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES(), checkAutoIssues.getText(),
            "Tras aplicarIdioma la etiqueta debe ser la versión en inglés de CHECK_GUARDAR_ISSUES");
        assertNotEquals(etiquetaEspaniol, checkAutoIssues.getText(),
            "La etiqueta debe cambiar efectivamente al cambiar de idioma");
    }

    @Test
    @DisplayName("Setter de autoguardado es seguro fuera del EDT")
    void testSetterAutoguardadoSeguroFueraDelEdt() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(true);
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
}
