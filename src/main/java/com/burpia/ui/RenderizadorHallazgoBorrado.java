package com.burpia.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Renderizador de celdas que aplica estilo visual de "ignorado/borrado" a hallazgos.
 * Decora el renderer original con tachado y colores atenuados cuando el hallazgo está marcado como ignorado.
 */
public class RenderizadorHallazgoBorrado implements TableCellRenderer {
    private final TableCellRenderer rendererOriginal;
    private final JTable tabla;
    private final ModeloTablaHallazgos modelo;

    /**
     * Crea un nuevo renderizador decorador para hallazgos ignorados.
     *
     * @param rendererOriginal El renderer base a decorar (no puede ser null)
     * @param tabla            La tabla asociada para obtener índices y fuentes (no puede ser null)
     * @param modelo           El modelo de tabla para verificar estado de ignorado (no puede ser null)
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public RenderizadorHallazgoBorrado(TableCellRenderer rendererOriginal, JTable tabla, ModeloTablaHallazgos modelo) {
        this.rendererOriginal = Objects.requireNonNull(rendererOriginal, "El renderer original no puede ser null");
        this.tabla = Objects.requireNonNull(tabla, "La tabla no puede ser null");
        this.modelo = Objects.requireNonNull(modelo, "El modelo no puede ser null");
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

        // Validar índice de fila antes de convertir
        if (row < 0 || row >= tabla.getRowCount()) {
            return componente;
        }

        int filaModelo = tabla.convertRowIndexToModel(row);

        // Aplicar estilos según estado de ignorado
        if (componente instanceof JLabel) {
            JLabel etiqueta = (JLabel) componente;

            // Desactivar HTML por seguridad (DRY: una sola vez)
            etiqueta.putClientProperty("html.disable", Boolean.TRUE);

            if (modelo.estaIgnorado(filaModelo)) {
                aplicarEstiloIgnorado(etiqueta, isSelected, table);
            } else {
                // Restaurar fuente normal
                etiqueta.setFont(tabla.getFont());
            }
        }

        return componente;
    }

    /**
     * Aplica el estilo visual de hallazgo ignorado: colores atenuados y fuente tachada.
     *
     * @param etiqueta   La etiqueta a estilizar
     * @param isSelected Si la fila está seleccionada
     * @param table      La tabla para obtener colores base
     */
    private void aplicarEstiloIgnorado(JLabel etiqueta, boolean isSelected, JTable table) {
        if (!isSelected) {
            Color tableBg = table.getBackground();
            Color fondoIgnorado = EstilosUI.colorFondoIgnorado(tableBg);
            etiqueta.setBackground(fondoIgnorado);
            etiqueta.setForeground(EstilosUI.colorTextoIgnorado(fondoIgnorado));
        }

        // Aplicar fuente con tachado
        Font base = tabla.getFont();
        Map<TextAttribute, Object> atributos = new HashMap<>(base.getAttributes());
        atributos.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        etiqueta.setFont(base.deriveFont(atributos));
    }
}
