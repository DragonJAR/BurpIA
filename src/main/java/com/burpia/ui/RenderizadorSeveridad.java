package com.burpia.ui;

import com.burpia.model.Hallazgo;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RenderizadorSeveridad extends DefaultTableCellRenderer {
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

            String severidad = (value != null) ? value.toString() : "";

            if (!isSelected) {
                Color colorFondo = Hallazgo.obtenerColorSeveridad(severidad);
                etiqueta.setBackground(colorFondo);

                // Texto blanco para fondos oscuros
                if (severidad.equals(Hallazgo.SEVERIDAD_CRITICAL) ||
                    severidad.equals(Hallazgo.SEVERIDAD_HIGH)) {
                    etiqueta.setForeground(Color.WHITE);
                } else {
                    etiqueta.setForeground(Color.BLACK);
                }
            }

            // Icono o indicador opcional
            StringBuilder texto = new StringBuilder(severidad);
            if (severidad.equals(Hallazgo.SEVERIDAD_CRITICAL)) {
                texto.insert(0, "⚠ ");
            } else if (severidad.equals(Hallazgo.SEVERIDAD_HIGH)) {
                texto.insert(0, "▲ ");
            } else if (severidad.equals(Hallazgo.SEVERIDAD_MEDIUM)) {
                texto.insert(0, "◆ ");
            } else if (severidad.equals(Hallazgo.SEVERIDAD_LOW)) {
                texto.insert(0, "▽ ");
            } else if (severidad.equals(Hallazgo.SEVERIDAD_INFO)) {
                texto.insert(0, "ℹ ");
            }

            etiqueta.setText(texto.toString());
        }

        return componente;
    }
}
