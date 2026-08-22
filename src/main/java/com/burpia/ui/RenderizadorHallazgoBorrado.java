package com.burpia.ui;

import com.burpia.i18n.I18nUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Renderizador de celdas que aplica estilo visual de "ignorado/borrado" a hallazgos.
 * Decora el renderer original con tachado y colores atenuados cuando el hallazgo está marcado como ignorado.
 */
public class RenderizadorHallazgoBorrado implements TableCellRenderer {
    private final TableCellRenderer rendererOriginal;
    private final ModeloTablaHallazgos modelo;

    // Cache de la fuente tachada para evitar reasignar HashMap + deriveFont
    // por cada cell paint (60 fps × N filas ignoradas → GC pressure).
    // Re-derivada solo cuando cambia la identidad de la fuente base.
    private Font fuenteBaseCacheada;
    private Font fuenteTachadaCacheada;

    /**
     * Crea un nuevo renderizador decorador para hallazgos ignorados.
     *
     * @param rendererOriginal El renderer base a decorar (no puede ser null)
     * @param tabla            La tabla asociada para obtener índices y fuentes (no puede ser null)
     * @param modelo           El modelo de tabla para verificar estado de ignorado (no puede ser null)
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public RenderizadorHallazgoBorrado(TableCellRenderer rendererOriginal, JTable tabla, ModeloTablaHallazgos modelo) {
        this.rendererOriginal = Objects.requireNonNull(rendererOriginal, I18nUI.General.ERROR_ARGUMENTO_NULO("renderer"));
        Objects.requireNonNull(tabla, I18nUI.General.ERROR_ARGUMENTO_NULO("tabla"));
        this.modelo = Objects.requireNonNull(modelo, I18nUI.General.ERROR_ARGUMENTO_NULO("modelo"));
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

        // Usar consistentemente el parámetro `table` (no un campo guardado): el
        // renderer se instancia una vez por columna y Swing siempre pasa la tabla
        // correcta; mezclar ambos podía leer estado de una tabla distinta.
        if (row < 0 || row >= table.getRowCount()) {
            // Reset defensivo: la fuente puede estar tachada de una fila
            // anterior (Swing reusa la misma instancia de renderer).
            componente.setFont(table.getFont());
            return componente;
        }

        int filaModelo = table.convertRowIndexToModel(row);

        // Aplicar estilos según estado de ignorado
        if (componente instanceof JLabel) {
            JLabel etiqueta = (JLabel) componente;

            // Desactivar HTML siempre, por seguridad: el contenido proviene del LLM y podría
            // contener marcado. Es una protección de defensa, no un detalle estético.
            etiqueta.putClientProperty("html.disable", Boolean.TRUE);

            if (modelo.estaIgnorado(filaModelo)) {
                aplicarEstiloIgnorado(etiqueta, isSelected, table);
            } else {
                etiqueta.setFont(table.getFont());
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
        Color tableBg = table.getBackground();
        if (!isSelected) {
            // Sin selección: fondo atenuado + texto ignorado para diferenciar la fila.
            Color fondoIgnorado = EstilosUI.colorFondoIgnorado(tableBg);
            etiqueta.setBackground(fondoIgnorado);
            etiqueta.setForeground(EstilosUI.colorTextoIgnorado(fondoIgnorado));
        } else {
            // Con selección: el fondo nativo prevalece, pero atenuamos el texto para
            // que una fila ignorada seleccionada siga distinguiéndose de una normal.
            etiqueta.setForeground(EstilosUI.colorTextoIgnorado(tableBg));
        }

        // Aplicar fuente con tachado (cache invalidación por identidad de base).
        etiqueta.setFont(obtenerFuenteTachada(table.getFont()));
    }

    /**
     * Devuelve la versión tachada de {@code base}, cacheando el resultado.
     * Re-deriva solo cuando la identidad de la fuente base cambia (cambio
     * de tema o fontsize por el usuario), evitando HashMap + deriveFont
     * en cada paint.
     */
    private Font obtenerFuenteTachada(Font base) {
        if (base == fuenteBaseCacheada && fuenteTachadaCacheada != null) {
            return fuenteTachadaCacheada;
        }
        Map<TextAttribute, Object> atributos = new HashMap<>(base.getAttributes());
        atributos.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        fuenteTachadaCacheada = base.deriveFont(atributos);
        fuenteBaseCacheada = base;
        return fuenteTachadaCacheada;
    }
}
