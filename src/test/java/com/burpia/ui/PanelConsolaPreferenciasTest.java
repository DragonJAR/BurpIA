package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PanelConsola Preferencias Tests")
class PanelConsolaPreferenciasTest extends PanelTestBase {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Permite establecer auto-scroll programáticamente")
    void testSetterProgramaticoAutoScroll() throws Exception {
        PanelConsola panel = crearPanelConsola();
        assertFalse(panel.isAutoScrollActivo(), "assertFalse failed at PanelConsolaPreferenciasTest.java:33");

        SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(false));
        flushEdt();
        assertFalse(panel.isAutoScrollActivo(), "assertFalse failed at PanelConsolaPreferenciasTest.java:37");

        SwingUtilities.invokeAndWait(() -> panel.establecerAutoScrollActivo(true));
        flushEdt();
        assertTrue(panel.isAutoScrollActivo(), "assertTrue failed at PanelConsolaPreferenciasTest.java:41");
    }

    @Test
    @DisplayName("aplicarIdioma refresca el resumen aunque no cambie la version")
    void testAplicarIdiomaRefrescaResumen() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelConsola panel = crearPanelConsola();
        JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
        flushEdt();
        assertTrue(etiquetaResumen.getText().contains("Total:"), "assertTrue failed at PanelConsolaPreferenciasTest.java:55");

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertTrue(etiquetaResumen.getText().contains("Total:"), "assertTrue failed at PanelConsolaPreferenciasTest.java:63");
    }

    @Test
    @DisplayName("aplicarTema mantiene contraste legible en resumen y consola")
    void testAplicarTemaMantieneContraste() throws Exception {
        PanelConsola panel = crearPanelConsola();
        JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
        JTextPane consola = obtenerConsola(panel);

        SwingUtilities.invokeAndWait(panel::aplicarTema);
        flushEdt();

        Color fondoPanel = panel.getBackground();
        assertTrue(EstilosUI.ratioContraste(etiquetaResumen.getForeground(), fondoPanel) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at PanelConsolaPreferenciasTest.java:81");
        assertTrue(EstilosUI.ratioContraste(consola.getForeground(), consola.getBackground()) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at PanelConsolaPreferenciasTest.java:82");
    }

    @Test
    @DisplayName("Limpiar consola refleja resumen en cero sin esperar nuevos logs")
    void testLimpiarConsolaActualizaResumenCero() throws Exception {
        PanelConsola panel = crearPanelConsola();
        JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();

        SwingUtilities.invokeAndWait(() -> gestor.limpiarConsola());
        flushEdt();

        SwingUtilities.invokeAndWait(panel::aplicarIdioma);
        flushEdt();

        assertTrue(etiquetaResumen.getText().contains("Total: 0"), "assertTrue failed at PanelConsolaPreferenciasTest.java:102");
    }

    @Test
    @DisplayName("Setter de auto-scroll es seguro fuera del EDT")
    void testSetterAutoScrollFueraDelEdt() throws Exception {
        PanelConsola panel = crearPanelConsola();
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
    }

    @Test
    @DisplayName("aplicarIdioma refresca controles de búsqueda y tooltips")
    void testAplicarIdiomaRefrescaControlesBusqueda() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelConsola panel = crearPanelConsola();
        JLabel etiquetaBuscar = obtenerEtiquetaBuscar(panel);
        JButton botonBuscar = obtenerBotonBuscar(panel);
        JButton botonAnterior = obtenerBotonAnterior(panel);
        JButton botonSiguiente = obtenerBotonSiguiente(panel);

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals(I18nUI.Consola.ETIQUETA_BUSCAR(), etiquetaBuscar.getText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:154");
        assertEquals(I18nUI.Consola.BOTON_BUSCAR(), botonBuscar.getText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:155");
        assertEquals(I18nUI.Consola.BOTON_BUSCAR_ANTERIOR(), botonAnterior.getText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:156");
        assertEquals(I18nUI.Consola.BOTON_BUSCAR_SIGUIENTE(), botonSiguiente.getText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:157");
        assertEquals(I18nUI.Tooltips.Consola.ANTERIOR(), botonAnterior.getToolTipText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:156");
        assertEquals(I18nUI.Tooltips.Consola.SIGUIENTE(), botonSiguiente.getToolTipText(), "assertEquals failed at PanelConsolaPreferenciasTest.java:157");
    }

    @Test
    @DisplayName("aplicarIdioma es seguro fuera del EDT")
    void testAplicarIdiomaFueraDelEdt() throws Exception {
        PanelConsola panel = crearPanelConsola();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread hilo = new Thread(() -> {
            try {
                I18nUI.establecerIdioma("en");
                panel.aplicarIdioma();
            } catch (Throwable t) {
                error.set(t);
            }
        }, "PanelConsola-AplicarIdioma-Test");
        hilo.start();
        hilo.join(2000);
        flushEdt();

        assertNull(error.get(), "assertNull failed at PanelConsolaPreferenciasTest.java:179");
        assertEquals(I18nUI.Consola.BOTON_BUSCAR(), obtenerBotonBuscar(panel).getText(),
            "assertEquals failed at PanelConsolaPreferenciasTest.java:180");
        assertEquals(I18nUI.Consola.BOTON_BUSCAR_SIGUIENTE(), obtenerBotonSiguiente(panel).getText(),
            "assertEquals failed at PanelConsolaPreferenciasTest.java:181");
    }

    @Test
    @DisplayName("Busqueda muestra el total real de coincidencias")
    void testBusquedaMuestraTotalCoincidencias() throws Exception {
        PanelConsola panel = crearPanelConsola();
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();
        JTextField campoBusqueda = obtenerCampoBusqueda(panel);
        JLabel etiquetaResultados = obtenerEtiquetaResultadosBusqueda(panel);
        JButton botonBuscar = obtenerBotonBuscar(panel);

        SwingUtilities.invokeAndWait(gestor::limpiarConsola);
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            gestor.registrarInfo("token alpha");
            gestor.registrarInfo("alpha beta");
            gestor.registrarError("alpha");
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            campoBusqueda.setText("alpha");
            botonBuscar.doClick();
        });
        flushEdt();

        assertTrue(etiquetaResultados.getText().contains("3"), "assertTrue failed at PanelConsolaPreferenciasTest.java:188");
    }

    @Test
    @DisplayName("Busqueda activa se refresca cuando cambian los logs")
    void testBusquedaActivaSeRefrescaConNuevosLogs() throws Exception {
        PanelConsola panel = crearPanelConsola();
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();
        JTextField campoBusqueda = obtenerCampoBusqueda(panel);
        JLabel etiquetaResultados = obtenerEtiquetaResultadosBusqueda(panel);
        JButton botonBuscar = obtenerBotonBuscar(panel);

        SwingUtilities.invokeAndWait(gestor::limpiarConsola);
        flushEdt();

        SwingUtilities.invokeAndWait(() -> gestor.registrarInfo("alpha inicial"));
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            campoBusqueda.setText("alpha");
            botonBuscar.doClick();
        });
        flushEdt();

        assertEquals(
            I18nUI.Consola.MSG_BUSQUEDA_ENCONTRADA(1, "alpha"),
            etiquetaResultados.getText(),
            "assertEquals failed at PanelConsolaPreferenciasTest.java:214"
        );

        SwingUtilities.invokeAndWait(() -> {
            gestor.registrarInfo("alpha nuevo");
            gestor.registrarVerbose("sin coincidencia");
        });
        flushEdt();
        invocarActualizacionResumen(panel);
        flushEdt();

        assertEquals(
            I18nUI.Consola.MSG_BUSQUEDA_ENCONTRADA(2, "alpha"),
            etiquetaResultados.getText(),
            "assertEquals failed at PanelConsolaPreferenciasTest.java:229"
        );
    }

    @Test
    @DisplayName("Modo angosto agrupa controles de consola en filas lógicas")
    void testModoAngostoAgrupaControlesEnFilasLogicas() throws Exception {
        PanelConsola panel = crearPanelConsola();
        JPanel panelControles = obtenerPanelControles(panel);
        JPanel panelResponsive = (JPanel) panelControles.getComponent(0);

        SwingUtilities.invokeAndWait(() -> {
            panelControles.setSize(500, 160);
            panelResponsive.setSize(460, 120);
            panelResponsive.doLayout();
        });
        flushEdt();

        assertEquals(2, panelResponsive.getComponentCount(), "assertEquals failed at PanelConsolaPreferenciasTest.java:207");
        assertTrue(panelResponsive.getComponent(0) instanceof JPanel, "assertTrue failed at PanelConsolaPreferenciasTest.java:208");
        assertTrue(panelResponsive.getComponent(1) instanceof JPanel, "assertTrue failed at PanelConsolaPreferenciasTest.java:209");
    }

    private JLabel obtenerEtiquetaResumen(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "etiquetaResumen", JLabel.class);
    }

    private JLabel obtenerEtiquetaBuscar(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "etiquetaBuscar", JLabel.class);
    }

    private JLabel obtenerEtiquetaResultadosBusqueda(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "etiquetaResultadosBusqueda", JLabel.class);
    }

    private JTextField obtenerCampoBusqueda(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "campoBusqueda", JTextField.class);
    }

    private JButton obtenerBotonBuscar(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "botonBuscar", JButton.class);
    }

    private JButton obtenerBotonAnterior(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "botonAnterior", JButton.class);
    }

    private JButton obtenerBotonSiguiente(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "botonSiguiente", JButton.class);
    }

    private JPanel obtenerPanelControles(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "panelControles", JPanel.class);
    }

    private JTextPane obtenerConsola(PanelConsola panel) throws Exception {
        return obtenerCampo(panel, "consola", JTextPane.class);
    }

    private void invocarActualizacionResumen(PanelConsola panel) throws Exception {
        java.lang.reflect.Method method = PanelConsola.class.getDeclaredMethod("actualizarResumen", boolean.class);
        method.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                method.invoke(panel, false);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
