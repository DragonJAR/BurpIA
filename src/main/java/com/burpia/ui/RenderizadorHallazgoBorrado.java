package com.burpia.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class RenderizadorHallazgoBorrado implements TableCellRenderer {
    private final TableCellRenderer rendererOriginal;
    private final JTable tabla;
    private final ModeloTablaHallazgos modelo;

    public RenderizadorHallazgoBorrado(TableCellRenderer rendererOriginal, JTable tabla, ModeloTablaHallazgos modelo) {
        this.rendererOriginal = rendererOriginal;
        this.tabla = tabla;
        this.modelo = modelo;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

        Component componente = rendererOriginal.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );

        int filaModelo = tabla.convertRowIndexToModel(row);

        if (modelo.estaIgnorado(filaModelo)) {
            if (componente instanceof JLabel) {
                JLabel etiqueta = (JLabel) componente;
                etiqueta.putClientProperty("html.disable", Boolean.TRUE);

                if (!isSelected) {
                    Color tableBg = table.getBackground();
                    boolean isDarkTheme = EstilosUI.esTemaOscuro(tableBg);
                    
                    if (isDarkTheme) {
                        etiqueta.setBackground(new Color(Math.max(0, tableBg.getRed() - 15), 
                                                         Math.max(0, tableBg.getGreen() - 15), 
                                                         Math.max(0, tableBg.getBlue() - 15)));
                        etiqueta.setForeground(new Color(130, 130, 130));
                    } else {
                        etiqueta.setBackground(new Color(245, 245, 245));
                        etiqueta.setForeground(new Color(150, 150, 150));
                    }
                }

                Font base = tabla.getFont();
                Map<TextAttribute, Object> atributos = new HashMap<>(base.getAttributes());
                atributos.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                etiqueta.setFont(base.deriveFont(atributos));
            }
        } else {
            if (componente instanceof JLabel) {
                JLabel etiqueta = (JLabel) componente;
                etiqueta.putClientProperty("html.disable", Boolean.TRUE);
                etiqueta.setFont(tabla.getFont());
            }
        }

        return componente;
    }
}
