package com.burpia.ui;

import com.burpia.model.Hallazgo;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RenderizadorConfianza extends DefaultTableCellRenderer {
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

            String confianza = (value != null) ? value.toString() : "";

            if (!isSelected) {
                Color colorFondo = Hallazgo.obtenerColorConfianza(confianza);
                etiqueta.setBackground(colorFondo);

                // Texto blanco o negro segun el fondo
                if (confianza.equals(Hallazgo.CONFIANZA_BAJA)) {
                    etiqueta.setForeground(Color.WHITE);
                } else {
                    etiqueta.setForeground(Color.BLACK);
                }
            }

            // Barra visual de confianza
            StringBuilder texto = new StringBuilder(confianza);
            if (confianza.equals(Hallazgo.CONFIANZA_ALTA)) {
                texto.append(" (███)");
            } else if (confianza.equals(Hallazgo.CONFIANZA_MEDIA)) {
                texto.append(" (█▀▀)");
            } else if (confianza.equals(Hallazgo.CONFIANZA_BAJA)) {
                texto.append(" (▀▀▀)");
            }

            etiqueta.setText(texto.toString());
        }

        return componente;
    }
}
