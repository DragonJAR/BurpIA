package com.burpia.ui;
import com.burpia.i18n.I18nUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.awt.font.TextAttribute;
import java.util.concurrent.atomic.AtomicReference;

public class UIUtils {
    private static final int DIALOGO_COLUMNAS_MIN = 36;
    private static final int DIALOGO_COLUMNAS_MAX = 52;
    private static final int DIALOGO_FILAS_MIN = 2;
    private static final int DIALOGO_FILAS_MAX = 8;
    private static final int DIALOGO_ANCHO_MIN = 460;
    private static final int DIALOGO_ALTO_MIN_MENSAJE = 168;
    private static final int DIALOGO_ALTO_MIN_CONFIRMACION = 156;
    private static final int DIALOGO_ALTO_EXTRA_OPT_OUT = 16;

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
        Color fondoBase = UIManager.getColor("Panel.background");
        if (fondoBase == null) {
            fondoBase = EstilosUI.COLOR_FONDO_PANEL;
        }
        Color colorBorde = EstilosUI.colorSeparador(fondoBase);
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(colorBorde, 1),
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
        mostrarMensaje(parent, titulo, mensaje, JOptionPane.ERROR_MESSAGE);
    }

    public static void mostrarAdvertencia(Component parent, String titulo, String mensaje) {
        mostrarMensaje(parent, titulo, mensaje, JOptionPane.WARNING_MESSAGE);
    }

    public static void mostrarInfo(Component parent, String titulo, String mensaje) {
        mostrarMensaje(parent, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean debeMostrarAlertaConOptOut(boolean alertasHabilitadas) {
        return alertasHabilitadas;
    }

    public static boolean confirmarPregunta(Component parent, String titulo, String mensaje) {
        return confirmar(parent, titulo, mensaje, JOptionPane.QUESTION_MESSAGE);
    }

    public static boolean confirmarAdvertencia(Component parent, String titulo, String mensaje) {
        return confirmar(parent, titulo, mensaje, JOptionPane.WARNING_MESSAGE);
    }

    public static void mostrarInfoConOptOut(Component parent,
                                            String titulo,
                                            String mensaje,
                                            boolean alertasHabilitadas,
                                            Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE, alertasHabilitadas, onDeshabilitar);
    }

    public static void mostrarAdvertenciaConOptOut(Component parent,
                                                   String titulo,
                                                   String mensaje,
                                                   boolean alertasHabilitadas,
                                                   Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.WARNING_MESSAGE, alertasHabilitadas, onDeshabilitar);
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
            panel.add(crearAreaMensajeDialogo(mensajePrincipal), BorderLayout.NORTH);
            if (urlEnlace != null && !urlEnlace.trim().isEmpty()
                && textoEnlace != null && !textoEnlace.trim().isEmpty()) {
                String textoVisible = extraerTextoVisibleEnlace(textoEnlace);
                JButton enlace = new JButton(textoVisible);
                enlace.setFont(resolverFuenteUI("Label.font", EstilosUI.FUENTE_ESTANDAR));
                enlace.setCursor(new Cursor(Cursor.HAND_CURSOR));
                Color fondoReferencia = panel.getBackground();
                if (fondoReferencia == null) {
                    fondoReferencia = UIManager.getColor("Panel.background");
                }
                if (fondoReferencia == null) {
                    fondoReferencia = EstilosUI.COLOR_FONDO_PANEL;
                }
                enlace.setForeground(EstilosUI.colorEnlaceAccesible(fondoReferencia));
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
            mostrarDialogoConContenido(
                parent,
                titulo,
                new DialogoContenido(panel, null),
                JOptionPane.ERROR_MESSAGE,
                JOptionPane.DEFAULT_OPTION
            );
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

    private static void mostrarMensajeConOptOut(Component parent,
                                                String titulo,
                                                String mensaje,
                                                int tipoMensaje,
                                                boolean alertasHabilitadas,
                                                Runnable onDeshabilitar) {
        if (!debeMostrarAlertaConOptOut(alertasHabilitadas)) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ejecutarEnEDT(() -> {
            DialogoContenido contenido = crearContenidoDialogo(mensaje, true);
            mostrarDialogoConContenido(
                parent,
                titulo,
                contenido,
                tipoMensaje,
                JOptionPane.DEFAULT_OPTION
            );

            if (contenido.chkNoMostrar != null && contenido.chkNoMostrar.isSelected() && onDeshabilitar != null) {
                onDeshabilitar.run();
            }
        });
    }

    static JTextArea crearAreaMensajeDialogo(String mensaje) {
        JTextArea areaMensaje = new JTextArea(mensaje != null ? mensaje : "");
        areaMensaje.setEditable(false);
        areaMensaje.setLineWrap(true);
        areaMensaje.setWrapStyleWord(true);
        areaMensaje.setOpaque(false);
        areaMensaje.setBorder(null);
        int columnas = calcularColumnasSugeridas(mensaje);
        areaMensaje.setColumns(columnas);
        areaMensaje.setRows(calcularFilasSugeridas(mensaje, columnas));
        areaMensaje.setFont(resolverFuenteUI("Label.font", EstilosUI.FUENTE_ESTANDAR));
        Color fg = UIManager.getColor("Label.foreground");
        areaMensaje.setForeground(fg != null ? fg : EstilosUI.COLOR_TEXTO_NORMAL);
        areaMensaje.setFocusable(false);
        return areaMensaje;
    }

    static JCheckBox crearCheckNoMostrarDialogo() {
        JCheckBox chkNoMostrar = new JCheckBox(I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA());
        chkNoMostrar.setFont(resolverFuenteUI("CheckBox.font", EstilosUI.FUENTE_ESTANDAR));
        chkNoMostrar.setOpaque(false);
        chkNoMostrar.setToolTipText(I18nUI.General.TOOLTIP_NO_VOLVER_MOSTRAR_ALERTA());
        return chkNoMostrar;
    }

    private static boolean confirmar(Component parent, String titulo, String mensaje, int tipoMensaje) {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return mostrarConfirmacionEnEdt(parent, titulo, mensaje, tipoMensaje);
        }
        AtomicReference<Boolean> confirmado = new AtomicReference<>(false);
        try {
            SwingUtilities.invokeAndWait(() -> confirmado.set(
                mostrarConfirmacionEnEdt(parent, titulo, mensaje, tipoMensaje)
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
        return confirmado.get();
    }

    private static boolean mostrarConfirmacionEnEdt(Component parent, String titulo, String mensaje, int tipoMensaje) {
        DialogoContenido contenido = crearContenidoDialogo(mensaje, false);
        int confirmacion = mostrarDialogoConContenido(
            parent,
            titulo,
            contenido,
            tipoMensaje,
            JOptionPane.YES_NO_OPTION
        );
        return confirmacion == JOptionPane.YES_OPTION;
    }

    private static void mostrarMensaje(Component parent, String titulo, String mensaje, int tipoMensaje) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ejecutarEnEDT(() -> {
            DialogoContenido contenido = crearContenidoDialogo(mensaje, false);
            mostrarDialogoConContenido(
                parent,
                titulo,
                contenido,
                tipoMensaje,
                JOptionPane.DEFAULT_OPTION
            );
        });
    }

    private static Component resolverPadreDialogo(Component parent) {
        if (parent != null) {
            return parent;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component foco = kfm.getFocusOwner();
        if (foco != null) {
            Window window = SwingUtilities.getWindowAncestor(foco);
            if (window != null) {
                return window;
            }
            return foco;
        }
        Window activa = kfm.getActiveWindow();
        return activa;
    }

    private static DialogoContenido crearContenidoDialogo(String mensaje, boolean incluirOptOut) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JTextArea areaMensaje = crearAreaMensajeDialogo(mensaje);
        areaMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(areaMensaje);
        JCheckBox chkNoMostrar = null;
        if (incluirOptOut) {
            panel.add(Box.createVerticalStrut(10));
            chkNoMostrar = crearCheckNoMostrarDialogo();
            chkNoMostrar.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(chkNoMostrar);
        }
        return new DialogoContenido(panel, chkNoMostrar);
    }

    private static final class DialogoContenido {
        private final JPanel panel;
        private final JCheckBox chkNoMostrar;

        private DialogoContenido(JPanel panel, JCheckBox chkNoMostrar) {
            this.panel = panel;
            this.chkNoMostrar = chkNoMostrar;
        }
    }

    private static int mostrarDialogoConContenido(Component parent,
                                                  String titulo,
                                                  DialogoContenido contenido,
                                                  int tipoMensaje,
                                                  int tipoOpcion) {
        Component padreDialogo = resolverPadreDialogo(parent);
        JOptionPane optionPane = new JOptionPane(contenido.panel, tipoMensaje, tipoOpcion);
        JDialog dialogo = optionPane.createDialog(padreDialogo, titulo);
        dialogo.setResizable(false);
        aplicarFuentesDialogo(dialogo);
        dialogo.pack();
        ajustarTamanoDialogo(dialogo, tipoOpcion, contenido.chkNoMostrar != null);
        dialogo.setLocationRelativeTo(padreDialogo);
        dialogo.setVisible(true);

        Object valor = optionPane.getValue();
        if (valor instanceof Integer) {
            return (Integer) valor;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    private static void ajustarTamanoDialogo(JDialog dialogo, int tipoOpcion, boolean incluyeOptOut) {
        if (dialogo == null) {
            return;
        }
        Dimension actual = dialogo.getSize();
        int altoMinimo = tipoOpcion == JOptionPane.YES_NO_OPTION
            ? DIALOGO_ALTO_MIN_CONFIRMACION
            : DIALOGO_ALTO_MIN_MENSAJE;
        if (incluyeOptOut) {
            altoMinimo += DIALOGO_ALTO_EXTRA_OPT_OUT;
        }
        int ancho = Math.max(actual.width, DIALOGO_ANCHO_MIN);
        int alto = Math.max(actual.height, altoMinimo);
        Dimension dimensionObjetivo = new Dimension(ancho, alto);
        dialogo.setMinimumSize(dimensionObjetivo);
        if (ancho != actual.width || alto != actual.height) {
            dialogo.setSize(dimensionObjetivo);
        }
    }

    private static void aplicarFuentesDialogo(Component componente) {
        if (componente == null) {
            return;
        }
        if (componente instanceof JButton) {
            componente.setFont(resolverFuenteUI("Button.font", EstilosUI.FUENTE_BOTON_PRINCIPAL));
        } else if (componente instanceof JLabel || componente instanceof JCheckBox) {
            componente.setFont(resolverFuenteUI("Label.font", EstilosUI.FUENTE_ESTANDAR));
        } else if (componente instanceof JTextArea) {
            componente.setFont(resolverFuenteUI("Label.font", EstilosUI.FUENTE_ESTANDAR));
        }

        if (componente instanceof Container) {
            for (Component hijo : ((Container) componente).getComponents()) {
                aplicarFuentesDialogo(hijo);
            }
        }
    }

    private static int calcularFilasSugeridas(String mensaje, int columnas) {
        if (mensaje == null || mensaje.isBlank()) {
            return DIALOGO_FILAS_MIN;
        }
        int columnasSeguras = Math.max(16, columnas);
        int filas = 0;
        for (String linea : mensaje.split("\\R", -1)) {
            int largo = linea != null ? linea.length() : 0;
            filas += Math.max(1, (largo + columnasSeguras - 1) / columnasSeguras);
        }
        return Math.max(DIALOGO_FILAS_MIN, Math.min(DIALOGO_FILAS_MAX, filas));
    }

    private static int calcularColumnasSugeridas(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return DIALOGO_COLUMNAS_MIN;
        }
        int maxLinea = 0;
        for (String linea : mensaje.split("\\R", -1)) {
            int largo = linea != null ? linea.trim().length() : 0;
            if (largo > maxLinea) {
                maxLinea = largo;
            }
        }
        int sugerido = maxLinea + 2;
        return Math.max(DIALOGO_COLUMNAS_MIN, Math.min(DIALOGO_COLUMNAS_MAX, sugerido));
    }

    private static Font resolverFuenteUI(String clave, Font fallback) {
        Font fuenteUI = clave != null ? UIManager.getFont(clave) : null;
        return fuenteUI != null ? fuenteUI : fallback;
    }
}
