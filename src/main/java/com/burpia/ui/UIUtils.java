package com.burpia.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.awt.font.TextAttribute;

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

    public static DocumentListener crearDocumentListener(Runnable onChange) {
        Runnable accionSegura = onChange != null ? onChange : () -> { };
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accionSegura.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accionSegura.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accionSegura.run();
            }
        };
    }

    public static void mostrarErrorBinarioAgenteNoEncontrado(Component parent,
                                                             String titulo,
                                                             String mensajePrincipal,
                                                             String textoEnlace,
                                                             String urlEnlace) {
        ejecutarEnEDT(() -> {
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.setOpaque(false);
            panel.add(new JLabel(mensajePrincipal), BorderLayout.NORTH);
            if (urlEnlace != null && !urlEnlace.trim().isEmpty()
                && textoEnlace != null && !textoEnlace.trim().isEmpty()) {
                String textoVisible = extraerTextoVisibleEnlace(textoEnlace);
                JButton enlace = new JButton(textoVisible);
                enlace.setCursor(new Cursor(Cursor.HAND_CURSOR));
                enlace.setForeground(new Color(0, 102, 204));
                enlace.setBorderPainted(false);
                enlace.setContentAreaFilled(false);
                enlace.setFocusPainted(false);
                enlace.setOpaque(false);
                enlace.setHorizontalAlignment(SwingConstants.LEFT);
                enlace.addActionListener(e -> abrirUrlEnNavegador(urlEnlace));
                Font fuenteNormal = enlace.getFont();
                Font fuenteSubrayada = crearFuenteSubrayada(fuenteNormal);
                enlace.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        enlace.setFont(fuenteSubrayada);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        enlace.setFont(fuenteNormal);
                    }
                });

                JPanel panelEnlace = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                panelEnlace.setOpaque(false);
                panelEnlace.add(enlace);
                panel.add(panelEnlace, BorderLayout.SOUTH);
            }
            JOptionPane.showMessageDialog(parent, panel, titulo, JOptionPane.ERROR_MESSAGE);
        });
    }

    static String extraerTextoVisibleEnlace(String textoEnlace) {
        if (textoEnlace == null) {
            return "";
        }
        String texto = textoEnlace.trim();
        if (texto.isEmpty()) {
            return "";
        }
        String sinAnchor = texto.replaceAll("(?is)<a\\b[^>]*>(.*?)</a>", "$1");
        String sinHtml = sinAnchor.replaceAll("(?is)<[^>]+>", "").trim();
        return sinHtml.isEmpty() ? texto : sinHtml;
    }

    private static Font crearFuenteSubrayada(Font fuenteBase) {
        if (fuenteBase == null) {
            return null;
        }
        Map<TextAttribute, Object> atributos = new HashMap<>();
        for (Map.Entry<?, ?> entry : fuenteBase.getAttributes().entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof TextAttribute) {
                atributos.put((TextAttribute) key, value);
            }
        }
        atributos.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        return fuenteBase.deriveFont(atributos);
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
