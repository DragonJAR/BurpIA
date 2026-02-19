package com.burpia.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RenderizadorCentrado extends DefaultTableCellRenderer {
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
        }

        return componente;
    }
}
