package com.burpia.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class UIUtils {

    private UIUtils() {
    }

    public static void actualizarTituloPanel(JPanel panel, String titulo) {
        Border borde = panel.getBorder();
        if (borde instanceof CompoundBorder) {
            Border externo = ((CompoundBorder) borde).getOutsideBorder();
            if (externo instanceof TitledBorder) {
                ((TitledBorder) externo).setTitle(titulo);
                panel.repaint();
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

    public static void mostrarErrorBinarioAgenteNoEncontrado(Component parent,
                                                             String titulo,
                                                             String mensajePrincipal,
                                                             String textoEnlace,
                                                             String urlEnlace) {
        ejecutarEnEDT(() -> {
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.add(new JLabel(mensajePrincipal), BorderLayout.NORTH);
            if (urlEnlace != null && !urlEnlace.trim().isEmpty()
                && textoEnlace != null && !textoEnlace.trim().isEmpty()) {
                JLabel enlace = new JLabel("<html><a href='" + urlEnlace + "'>" + textoEnlace + "</a></html>");
                enlace.setCursor(new Cursor(Cursor.HAND_CURSOR));
                enlace.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        abrirUrlEnNavegador(urlEnlace);
                    }
                });
                panel.add(enlace, BorderLayout.SOUTH);
            }
            JOptionPane.showMessageDialog(parent, panel, titulo, JOptionPane.ERROR_MESSAGE);
        });
    }

    public static boolean abrirUrlEnNavegador(String url) {
        if (url == null || url.trim().isEmpty() || !Desktop.isDesktopSupported()) {
            return false;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void ejecutarEnEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
