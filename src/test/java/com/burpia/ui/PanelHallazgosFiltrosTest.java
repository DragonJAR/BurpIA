package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;





@DisplayName("PanelHallazgos Filtros Tests")
class PanelHallazgosFiltrosTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Volver a 'Todas las Criticidades' no deja filtro residual")
    void testFiltroTodasLasCriticidades() throws Exception {
        I18nUI.establecerIdioma("es");
        
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);
        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);

        assertEquals(2, tabla.getRowCount());

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem(I18nUI.Hallazgos.SEVERIDAD_HIGH()));
        flushEdt();
        assertEquals(1, tabla.getRowCount());

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem(I18nUI.Hallazgos.OPCION_TODAS_CRITICIDADES()));
        flushEdt();
        assertEquals(2, tabla.getRowCount());
    }

    @Test
    @DisplayName("Estado de guardado automático en Issues se sincroniza con el checkbox")
    void testEstadoGuardadoAutomaticoIssues() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        JCheckBox checkbox = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());

        SwingUtilities.invokeAndWait(checkbox::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo());

        SwingUtilities.invokeAndWait(checkbox::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo());
    }

    @Test
    @DisplayName("Modelo marca y consulta hallazgos ignorados correctamente")
    void testModeloIgnorados() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroHallazgos());
        assertFalse(modelo.estaIgnorado(0));
        assertFalse(modelo.estaIgnorado(1));
        assertEquals(0, modelo.obtenerNumeroIgnorados());

        modelo.marcarComoIgnorado(0);
        flushEdt();

        assertTrue(modelo.estaIgnorado(0));
        assertFalse(modelo.estaIgnorado(1));
        assertEquals(1, modelo.obtenerNumeroIgnorados());

        List<Hallazgo> noIgnorados = modelo.obtenerHallazgosNoIgnorados();
        assertEquals(1, noIgnorados.size());
        assertEquals("TB", noIgnorados.get(0).obtenerTitulo());

        modelo.marcarComoIgnorado(1);
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroIgnorados());
        assertTrue(modelo.obtenerHallazgosNoIgnorados().isEmpty());
    }

    @Test
    @DisplayName("Eliminar hallazgo actualiza indices de ignorados")
    void testEliminarActualizaIgnorados() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium"),
            new Hallazgo("https://example.com/c", "TC", "Hallazgo C", "Medium", "Low")
        ));
        flushEdt();

        modelo.marcarComoIgnorado(1);
        assertTrue(modelo.estaIgnorado(1));

        modelo.eliminarHallazgo(0);
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroHallazgos());
        assertTrue(modelo.estaIgnorado(0));
        assertFalse(modelo.estaIgnorado(1));

        assertEquals(1, modelo.obtenerNumeroIgnorados());
    }

    @Test
    @DisplayName("aplicarIdioma actualiza todas las opciones del combo de severidad")
    void testAplicarIdiomaActualizaOpcionesSeveridad() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);
        SwingUtilities.invokeAndWait(() -> {
            comboSeveridad.setSelectedIndex(2);
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals("All Severities", comboSeveridad.getItemAt(0));
        assertEquals("Critical", comboSeveridad.getItemAt(1));
        assertEquals("High", comboSeveridad.getItemAt(2));
        assertEquals("Medium", comboSeveridad.getItemAt(3));
        assertEquals("Low", comboSeveridad.getItemAt(4));
        assertEquals("Info", comboSeveridad.getItemAt(5));
        assertEquals(2, comboSeveridad.getSelectedIndex());
    }

    @Test
    @DisplayName("Captura de acciones tolera URL invalida sin request")
    void testCapturaEntradasAccionToleraUrlInvalida() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgo(new Hallazgo("://url-invalida", "T", "H", "Low", "Low"));
        flushEdt();

        Method metodo = PanelHallazgos.class.getDeclaredMethod("capturarEntradasAccion", int[].class);
        metodo.setAccessible(true);

        assertDoesNotThrow(() -> metodo.invoke(panel, (Object) new int[]{0}));
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
