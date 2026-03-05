package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.font.TextAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("RenderizadorHallazgoBorrado Tests")
class RenderizadorHallazgoBorradoTest {

    private static final int COLUMNA_TITULO = 2;

    @Test
    @DisplayName("Aplica tachado y desactiva HTML para hallazgos ignorados")
    void testRenderFilaIgnorada() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "Hallazgo <html>", "Medium", "High"));
        flushEdt();

        modelo.marcarComoIgnorado(0);
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Hallazgo <html>", false, false, 0, COLUMNA_TITULO);
        JLabel label = (JLabel) c;

        assertEquals(Boolean.TRUE, label.getClientProperty("html.disable"));
        assertEquals(TextAttribute.STRIKETHROUGH_ON, label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
        assertEquals(EstilosUI.colorFondoIgnorado(tabla.getBackground()), label.getBackground());
        assertEquals(EstilosUI.colorTextoIgnorado(label.getBackground()), label.getForeground());
    }

    @Test
    @DisplayName("No aplica tachado para hallazgos NO ignorados")
    void testRenderFilaNoIgnorada() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "Hallazgo normal", "Medium", "High"));
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Hallazgo normal", false, false, 0, COLUMNA_TITULO);
        JLabel label = (JLabel) c;

        assertEquals(Boolean.TRUE, label.getClientProperty("html.disable"));
        assertNull(label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
        assertEquals(tabla.getFont(), label.getFont());
    }

    @Test
    @DisplayName("Maneja fila con índice negativo sin error")
    void testRenderFilaIndiceNegativo() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "Hallazgo", "Low", "Low"));
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Test", false, false, -1, COLUMNA_TITULO);
        JLabel label = (JLabel) c;

        assertNull(label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
    }

    @Test
    @DisplayName("Maneja fila fuera de rango sin error")
    void testRenderFilaFueraDeRango() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "Hallazgo", "Low", "Low"));
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Test", false, false, 100, COLUMNA_TITULO);
        JLabel label = (JLabel) c;

        assertNull(label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
    }

    @Test
    @DisplayName("Desactiva HTML siempre por seguridad")
    void testHtmlSiempreDesactivado() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "<b>HTML</b>", "Low", "Low"));
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "<b>HTML</b>", false, false, 0, COLUMNA_TITULO);
        JLabel label = (JLabel) c;

        assertEquals(Boolean.TRUE, label.getClientProperty("html.disable"));
    }

    @Test
    @DisplayName("Retorna el mismo componente decorado")
    void testRetornaComponenteDecorado() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "T", "Hallazgo", "Low", "Low"));
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Test", false, false, 0, COLUMNA_TITULO);

        assertNotNull(c);
        assertInstanceOf(JLabel.class, c);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
