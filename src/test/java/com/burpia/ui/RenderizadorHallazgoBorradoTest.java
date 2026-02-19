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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RenderizadorHallazgoBorrado Tests")
class RenderizadorHallazgoBorradoTest {

    @Test
    @DisplayName("Aplica tachado y desactiva HTML para hallazgos ignorados")
    void testRenderFilaIgnorada() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos();
        JTable tabla = new JTable(modelo);
        modelo.agregarHallazgo(new Hallazgo("https://example.com", "Hallazgo <html>", "Medium", "High"));
        flushEdt();

        modelo.marcarComoIgnorado(0);
        flushEdt();

        RenderizadorHallazgoBorrado render = new RenderizadorHallazgoBorrado(
            new DefaultTableCellRenderer(),
            tabla,
            modelo
        );

        Component c = render.getTableCellRendererComponent(tabla, "Hallazgo <html>", false, false, 0, 2);
        JLabel label = (JLabel) c;

        assertEquals(Boolean.TRUE, label.getClientProperty("html.disable"));
        assertNotNull(label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
        assertTrue(label.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH).equals(TextAttribute.STRIKETHROUGH_ON));
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
