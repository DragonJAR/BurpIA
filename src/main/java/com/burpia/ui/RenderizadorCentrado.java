package com.burpia.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renderizador de celdas que centra horizontalmente el contenido.
 * Optimizado evitando verificaciones redundantes de tipo.
 */
public class RenderizadorCentrado extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    /**
     * Configura el renderizador de celda con alineación centrada.
     * DefaultTableCellRenderer extiende JLabel, por lo que siempre es seguro
     * establecer la alineación directamente sin verificación de tipo.
     */
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        return this;
    }
}
