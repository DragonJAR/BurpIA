package com.burpia.ui;
import com.burpia.model.Tarea;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RenderizadorEstado extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

        Component componente = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );

        if (componente instanceof JLabel) {
            JLabel etiqueta = (JLabel) componente;
            etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
            etiqueta.setOpaque(true);

            String estado = (value != null) ? value.toString() : "";

            if (!isSelected) {
                Color colorFondo = Tarea.obtenerColorEstado(estado);
                etiqueta.setBackground(colorFondo);
                Color colorTexto = EstilosUI.obtenerColorTextoContraste(colorFondo);
                etiqueta.setForeground(EstilosUI.ajustarParaContrasteMinimo(
                    colorTexto,
                    colorFondo,
                    EstilosUI.CONTRASTE_AA_NORMAL
                ));
            }

            etiqueta.setText(com.burpia.i18n.I18nUI.Tareas.TRADUCIR_ESTADO(estado));
        }

        return componente;
    }
}
