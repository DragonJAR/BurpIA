package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para mostrar estados de tareas con estilo visual (color de fondo).
 * Implementa accesibilidad WCAG 2.0 con contraste mínimo AA para texto sobre fondos coloreados.
 */
public class RenderizadorEstado extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("this-escape")
    public RenderizadorEstado() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(true);
    }

    /**
     * Configura el renderizador de celda con el color de fondo correspondiente al estado.
     * DefaultTableCellRenderer extiende JLabel, por lo que siempre es seguro
     * establecer propiedades directamente sin verificación de tipo.
     *
     * @param table      Tabla que solicita el renderizador
     * @param value      Valor de la celda (estado de la tarea)
     * @param isSelected Si la celda está seleccionada
     * @param hasFocus   Si la celda tiene el foco
     * @param row        Fila de la celda
     * @param column     Columna de la celda
     * @return Componente configurado para renderizar la celda
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

        String estado = (value != null) ? value.toString() : "";

        if (!isSelected) {
            Color colorFondo = Tarea.obtenerColorEstado(estado);
            setBackground(colorFondo);
            Color colorTexto = EstilosUI.obtenerColorTextoContraste(colorFondo);
            setForeground(EstilosUI.ajustarParaContrasteMinimo(
                colorTexto,
                colorFondo,
                EstilosUI.CONTRASTE_AA_NORMAL
            ));
        }

        setText(I18nUI.Tareas.TRADUCIR_ESTADO(estado));

        return this;
    }
}
