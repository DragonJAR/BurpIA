package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("PanelHallazgos Filtros Tests")
class PanelHallazgosFiltrosTest {

    @Test
    @DisplayName("Volver a 'Todas las Criticidades' no deja filtro residual")
    void testFiltroTodasLasCriticidades() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, false));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);
        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);

        assertEquals(2, tabla.getRowCount());

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem("High"));
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
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, false));
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
