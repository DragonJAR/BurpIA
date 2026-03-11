package com.burpia.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

final class PanelFilasResponsive extends JPanel {
    private static final int AJUSTE_MARGEN_ANCHO = 40;
    private static final int VGAP_HORIZONTAL = 4;

    private final int umbralResponsive;
    private final int espaciadoHorizontal;
    private final int espaciadoVertical;
    private final List<List<JComponent>> filas;
    private boolean ultimoLayoutHorizontal = true;

    PanelFilasResponsive(
        int umbralResponsive,
        int espaciadoHorizontal,
        int espaciadoVertical,
        List<List<JComponent>> filas
    ) {
        this.umbralResponsive = umbralResponsive;
        this.espaciadoHorizontal = espaciadoHorizontal;
        this.espaciadoVertical = espaciadoVertical;
        this.filas = copiarFilas(filas);
        setOpaque(false);
        reconstruirContenido(true);
    }

    @Override
    public void doLayout() {
        boolean layoutHorizontal = calcularLayoutHorizontal();
        if (layoutHorizontal != ultimoLayoutHorizontal) {
            reconstruirContenido(layoutHorizontal);
            ultimoLayoutHorizontal = layoutHorizontal;
        }
        super.doLayout();
    }

    private boolean calcularLayoutHorizontal() {
        int anchoBase = getParent() != null ? getParent().getWidth() : getWidth();
        int anchoDisponible = Math.max(0, anchoBase - AJUSTE_MARGEN_ANCHO);
        return anchoDisponible >= umbralResponsive;
    }

    private void reconstruirContenido(boolean layoutHorizontal) {
        removeAll();
        if (layoutHorizontal) {
            reconstruirHorizontal();
        } else {
            reconstruirVertical();
        }
        revalidate();
        repaint();
    }

    private void reconstruirHorizontal() {
        setLayout(new FlowLayout(FlowLayout.LEFT, espaciadoHorizontal, VGAP_HORIZONTAL));
        for (int i = 0; i < filas.size(); i++) {
            agregarComponentesFila(this, filas.get(i));
            if (i + 1 < filas.size()) {
                add(new JSeparator(SwingConstants.VERTICAL));
            }
        }
    }

    private void reconstruirVertical() {
        int totalFilas = Math.max(1, filas.size());
        setLayout(new GridLayout(totalFilas, 1, 0, espaciadoVertical));
        for (List<JComponent> fila : filas) {
            JPanel panelFila = new JPanel(new FlowLayout(FlowLayout.LEFT, espaciadoHorizontal, VGAP_HORIZONTAL));
            panelFila.setOpaque(false);
            agregarComponentesFila(panelFila, fila);
            add(panelFila);
        }
    }

    private void agregarComponentesFila(JPanel contenedor, List<JComponent> componentes) {
        for (JComponent componente : componentes) {
            if (componente != null) {
                contenedor.add(componente);
            }
        }
    }

    private static List<List<JComponent>> copiarFilas(List<List<JComponent>> filasOriginales) {
        List<List<JComponent>> copia = new ArrayList<>();
        if (filasOriginales == null) {
            return copia;
        }
        for (List<JComponent> fila : filasOriginales) {
            copia.add(fila == null ? List.of() : List.copyOf(fila));
        }
        return List.copyOf(copia);
    }
}
