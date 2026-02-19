package com.burpia.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renderizador que muestra las celdas de hallazgos ignorados en gris y tachado.
 * Envuelve a otros renderizadores y aplica el estilo si la fila está marcada como ignorada.
 */
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

        // Verificar si el hallazgo está ignorado
        if (modelo.estaIgnorado(filaModelo)) {
            if (componente instanceof JLabel) {
                JLabel etiqueta = (JLabel) componente;

                if (!isSelected) {
                    etiqueta.setBackground(new Color(240, 240, 240));
                    etiqueta.setForeground(Color.GRAY);
                }

                String textoOriginal = etiqueta.getText();
                if (!textoOriginal.startsWith("<html>")) {
                    etiqueta.setText("<html><s>" + textoOriginal + "</s></html>");
                }
            }
        }

        return componente;
    }
}
