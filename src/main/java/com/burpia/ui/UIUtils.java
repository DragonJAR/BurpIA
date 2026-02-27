package com.burpia.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.Component;

public class UIUtils {

    private UIUtils() {
    }

    public static void actualizarTituloPanel(JPanel panel, String titulo) {
        Border borde = panel.getBorder();
        if (borde instanceof CompoundBorder) {
            Border externo = ((CompoundBorder) borde).getOutsideBorder();
            if (externo instanceof TitledBorder) {
                ((TitledBorder) externo).setTitle(titulo);
            }
        }
    }

    public static Border crearBordeTitulado(String titulo, int pV, int pH) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(pV, pH, pV, pH)
        );
    }

    public static Border crearBordeTitulado(String titulo) {
        return crearBordeTitulado(titulo, EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL);
    }

    public static void mostrarError(Component parent, String titulo, String mensaje) {
        ejecutarEnEDT(() -> JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.ERROR_MESSAGE));
    }

    public static void mostrarAdvertencia(Component parent, String titulo, String mensaje) {
        ejecutarEnEDT(() -> JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.WARNING_MESSAGE));
    }

    public static void mostrarInfo(Component parent, String titulo, String mensaje) {
        ejecutarEnEDT(() -> JOptionPane.showMessageDialog(parent, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE));
    }

    private static void ejecutarEnEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
