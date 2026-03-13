package com.burpia.ui;

import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.awt.font.TextAttribute;
import java.util.concurrent.atomic.AtomicReference;

public final class UIUtils {
    private static final String ORIGEN_LOG = "UIUtils";
    private static final GestorLoggingUnificado GESTOR_LOGGING =
            GestorLoggingUnificado.crearMinimal(null, null);
    private static final String PROPIEDAD_TOOLTIPS_ENCABEZADO = "burpia.tooltips.encabezado";
    private static final String PROPIEDAD_LISTENER_ENCABEZADO = "burpia.listener.encabezado";
    private static final int DIALOGO_COLUMNAS_MIN = 36;
    private static final int DIALOGO_COLUMNAS_MAX = 52;
    private static final int DIALOGO_FILAS_MIN = 2;
    private static final int DIALOGO_FILAS_MAX = 8;
    private static final int DIALOGO_ANCHO_MIN = 460;
    private static final int DIALOGO_ALTO_MIN_MENSAJE = 168;
    private static final int DIALOGO_ALTO_MIN_CONFIRMACION = 156;
    private static final int DIALOGO_ALTO_EXTRA_OPT_OUT = 16;
    private static final int DIALOGO_DELAY_MENU_CONTEXTO_MS = 140;

    private UIUtils() {
    }

    public static void ejecutarEnEdtYEsperar(Runnable accion) {
        if (accion == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(accion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(null, e);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(null, causa);
        }
    }

    public static void ejecutarEnEdt(Runnable accion) {
        if (accion == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
        } else {
            SwingUtilities.invokeLater(accion);
        }
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

    public static void actualizarTextoYTooltip(JComponent componente, String texto, String tooltip) {
        if (componente == null) {
            return;
        }
        if (componente instanceof JLabel) {
            ((JLabel) componente).setText(texto);
        } else if (componente instanceof AbstractButton) {
            ((AbstractButton) componente).setText(texto);
        }
        componente.setToolTipText(tooltip);
    }

    public static JMenuItem crearMenuItemContextual(String texto,
            String tooltip,
            java.awt.event.ActionListener accion) {
        JMenuItem menuItem = new JMenuItem(texto);
        menuItem.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItem.setToolTipText(tooltip);
        if (accion != null) {
            menuItem.addActionListener(accion);
        }
        return menuItem;
    }

    public static void instalarTooltipsEncabezadoTabla(JTable tabla, String... tooltipsPorModelo) {
        if (tabla == null) {
            return;
        }
        JTableHeader encabezado = tabla.getTableHeader();
        if (encabezado == null) {
            return;
        }

        encabezado.putClientProperty(PROPIEDAD_TOOLTIPS_ENCABEZADO,
                tooltipsPorModelo != null ? tooltipsPorModelo.clone() : null);

        if (encabezado.getClientProperty(PROPIEDAD_LISTENER_ENCABEZADO) instanceof MouseAdapter) {
            return;
        }

        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                actualizarTooltipEncabezado(encabezado, e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encabezado.setToolTipText(null);
            }
        };
        encabezado.addMouseMotionListener(listener);
        encabezado.addMouseListener(listener);
        encabezado.putClientProperty(PROPIEDAD_LISTENER_ENCABEZADO, listener);
    }

    public static int[] capturarAnchosColumnasTabla(JTable tabla) {
        if (tabla == null || tabla.getColumnModel() == null) {
            return new int[0];
        }

        int totalColumnas = tabla.getColumnModel().getColumnCount();
        int[] anchos = new int[totalColumnas];
        for (int i = 0; i < totalColumnas; i++) {
            TableColumn columna = tabla.getColumnModel().getColumn(i);
            int anchoPreferido = columna.getPreferredWidth();
            anchos[i] = anchoPreferido > 0 ? anchoPreferido : columna.getWidth();
        }
        return anchos;
    }

    public static void restaurarAnchosColumnasTabla(JTable tabla, int... anchos) {
        if (tabla == null || anchos == null || tabla.getColumnModel() == null) {
            return;
        }

        int totalColumnas = Math.min(tabla.getColumnModel().getColumnCount(), anchos.length);
        for (int i = 0; i < totalColumnas; i++) {
            int ancho = anchos[i];
            if (ancho <= 0) {
                continue;
            }
            tabla.getColumnModel().getColumn(i).setPreferredWidth(ancho);
            tabla.getColumnModel().getColumn(i).setWidth(ancho);
        }
    }

    private static void actualizarTooltipEncabezado(JTableHeader encabezado, MouseEvent evento) {
        if (encabezado == null || evento == null) {
            return;
        }
        Object valor = encabezado.getClientProperty(PROPIEDAD_TOOLTIPS_ENCABEZADO);
        if (!(valor instanceof String[])) {
            encabezado.setToolTipText(null);
            return;
        }

        String[] tooltips = (String[]) valor;
        int indiceVista = encabezado.columnAtPoint(evento.getPoint());
        if (indiceVista < 0 || indiceVista >= encabezado.getColumnModel().getColumnCount()) {
            encabezado.setToolTipText(null);
            return;
        }

        int indiceModelo = encabezado.getColumnModel().getColumn(indiceVista).getModelIndex();
        String tooltip = indiceModelo >= 0 && indiceModelo < tooltips.length ? tooltips[indiceModelo] : null;
        encabezado.setToolTipText(tooltip);
    }

    public static Border crearBordeTitulado(String titulo, int pV, int pH) {
        Color fondoBase = EstilosUI.obtenerFondoPanel();
        Color colorBorde = EstilosUI.colorSeparador(fondoBase);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(colorBorde, 1),
                        titulo,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        EstilosUI.FUENTE_NEGRITA),
                BorderFactory.createEmptyBorder(pV, pH, pV, pH));
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
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE, alertasHabilitadas,
                onDeshabilitar);
    }

    public static void mostrarAdvertenciaConOptOut(Component parent,
            String titulo,
            String mensaje,
            boolean alertasHabilitadas,
            Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.WARNING_MESSAGE, alertasHabilitadas,
                onDeshabilitar);
    }

    public static void mostrarInfoConOptOutMenuContextual(Component parent,
            String titulo,
            String mensaje,
            boolean alertasHabilitadas,
            Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE,
                alertasHabilitadas, onDeshabilitar, DIALOGO_DELAY_MENU_CONTEXTO_MS);
    }

    public static void mostrarAdvertenciaConOptOutMenuContextual(Component parent,
            String titulo,
            String mensaje,
            boolean alertasHabilitadas,
            Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, JOptionPane.WARNING_MESSAGE,
                alertasHabilitadas, onDeshabilitar, DIALOGO_DELAY_MENU_CONTEXTO_MS);
    }

    public static DocumentListener crearDocumentListener(Runnable onChange) {
        Runnable accionSegura = onChange != null ? onChange : () -> {
        };
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
        ejecutarEnEdt(() -> {
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.setOpaque(false);
            panel.add(crearAreaMensajeDialogo(mensajePrincipal), BorderLayout.NORTH);
            if (Normalizador.noEsVacio(urlEnlace)
                    && Normalizador.noEsVacio(textoEnlace)) {
                String textoVisible = extraerTextoVisibleEnlace(textoEnlace);
                JButton enlace = new JButton(textoVisible);
                enlace.setFont(resolverFuenteUI("Label.font", EstilosUI.FUENTE_ESTANDAR));
                enlace.setCursor(new Cursor(Cursor.HAND_CURSOR));
                Color fondoReferencia = panel.getBackground();
                if (fondoReferencia == null) {
                    fondoReferencia = EstilosUI.obtenerFondoPanel();
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
                    JOptionPane.DEFAULT_OPTION);
        });
    }

    public static String construirMensajeBinarioAgenteNoEncontrado(String nombreAgente, String comandoConfigurado) {
        String ejecutableDetectado = OSUtils.resolverEjecutableComando(comandoConfigurado);
        String rutaMensaje = Normalizador.esVacio(ejecutableDetectado) ? comandoConfigurado : ejecutableDetectado;
        String mensaje = I18nUI.Configuracion.Agentes.MSG_BINARIO_NO_EXISTE(nombreAgente, rutaMensaje);

        if (!Normalizador.esVacio(comandoConfigurado)
                && !Normalizador.esVacio(rutaMensaje)
                && !comandoConfigurado.trim().equals(rutaMensaje)) {
            mensaje = mensaje + "\n" + I18nUI.Configuracion.Agentes.MSG_COMANDO_CONFIGURADO(comandoConfigurado);
        }
        return mensaje;
    }

    static String extraerTextoVisibleEnlace(String textoEnlace) {
        if (Normalizador.esVacio(textoEnlace)) {
            return "";
        }
        String texto = textoEnlace.trim();
        String sinAnchor = texto.replaceAll("(?is)<a\\b[^>]*>(.*?)</a>", "$1");
        String sinHtml = sinAnchor.replaceAll("(?is)<[^>]+>", "").trim();
        return Normalizador.esVacio(sinHtml) ? texto : sinHtml;
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
        if (Normalizador.esVacio(url)) {
            return false;
        }
        String urlLimpia = url.trim();
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(new URI(urlLimpia));
            return true;
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.tr("No se pudo abrir URL en el navegador"), e);
            return false;
        }
    }

    public static boolean abrirUrlConFallbackInfo(Component parent,
            String titulo,
            String url,
            String mensajeFallback) {
        boolean abierto = abrirUrlEnNavegador(url);
        if (!abierto && Normalizador.noEsVacio(mensajeFallback)) {
            mostrarInfo(parent, titulo, mensajeFallback);
        }
        return abierto;
    }

    private static void mostrarMensajeConOptOut(Component parent,
            String titulo,
            String mensaje,
            int tipoMensaje,
            boolean alertasHabilitadas,
            Runnable onDeshabilitar) {
        mostrarMensajeConOptOut(parent, titulo, mensaje, tipoMensaje, alertasHabilitadas, onDeshabilitar, 0);
    }

    private static void mostrarMensajeConOptOut(Component parent,
            String titulo,
            String mensaje,
            int tipoMensaje,
            boolean alertasHabilitadas,
            Runnable onDeshabilitar,
            int delayMs) {
        if (!alertasHabilitadas) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ejecutarEnEDTConRetraso(() -> {
            DialogoContenido contenido = crearContenidoDialogo(mensaje, true);
            mostrarDialogoConContenido(
                    parent,
                    titulo,
                    contenido,
                    tipoMensaje,
                    JOptionPane.DEFAULT_OPTION);

            if (contenido.chkNoMostrar != null && contenido.chkNoMostrar.isSelected() && onDeshabilitar != null) {
                onDeshabilitar.run();
            }
        }, delayMs);
    }

    static void ejecutarEnEDTConRetraso(Runnable runnable, int delayMs) {
        if (runnable == null) {
            return;
        }
        int delaySeguro = normalizarDelayMs(delayMs);
        ejecutarEnEdt(() -> {
            if (delaySeguro <= 0) {
                runnable.run();
                return;
            }
            Timer temporizador = new Timer(delaySeguro, e -> {
                ((Timer) e.getSource()).stop();
                runnable.run();
            });
            temporizador.setRepeats(false);
            temporizador.start();
        });
    }

    static int normalizarDelayMs(int delayMs) {
        return Math.max(0, delayMs);
    }

    static JTextArea crearAreaMensajeDialogo(String mensaje) {
        JTextArea areaMensaje = new JTextArea(mensaje != null ? mensaje : "");
        areaMensaje.setEditable(false);
        areaMensaje.setLineWrap(true);
        areaMensaje.setWrapStyleWord(true);
        areaMensaje.setOpaque(false);
        areaMensaje.setBorder(null);
        areaMensaje.setMargin(new Insets(0, 2, 0, 2));
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
                    mostrarConfirmacionEnEdt(parent, titulo, mensaje, tipoMensaje)));
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
                JOptionPane.YES_NO_OPTION);
        return confirmacion == JOptionPane.YES_OPTION;
    }

    private static void mostrarMensaje(Component parent, String titulo, String mensaje, int tipoMensaje) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ejecutarEnEdt(() -> {
            DialogoContenido contenido = crearContenidoDialogo(mensaje, false);
            mostrarDialogoConContenido(
                    parent,
                    titulo,
                    contenido,
                    tipoMensaje,
                    JOptionPane.DEFAULT_OPTION);
        });
    }

    private static Component resolverPadreDialogo(Component parent) {
        Component padre = normalizarPadreDialogo(parent);
        if (padre != null) {
            return padre;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        padre = normalizarPadreDialogo(kfm.getFocusOwner());
        if (padre != null) {
            return padre;
        }
        return normalizarPadreDialogo(kfm.getActiveWindow());
    }

    static Component normalizarPadreDialogo(Component candidato) {
        if (candidato == null) {
            return null;
        }
        Component base = candidato;
        if (base instanceof JPopupMenu) {
            base = ((JPopupMenu) base).getInvoker();
            if (base == null) {
                return null;
            }
        }
        Window ventana = base instanceof Window ? (Window) base : SwingUtilities.getWindowAncestor(base);
        Window estable = resolverVentanaDialogoEstable(ventana);
        return estable != null ? estable : base;
    }

    static Window resolverVentanaDialogoEstable(Window ventana) {
        Window actual = ventana;
        while (actual != null) {
            if ((actual instanceof Frame || actual instanceof Dialog) && actual.isDisplayable()) {
                return actual;
            }
            actual = actual.getOwner();
        }
        return null;
    }

    private static DialogoContenido crearContenidoDialogo(String mensaje, boolean incluirOptOut) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
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
        Component mensajeConIcono = envolverContenidoConIcono(contenido.panel, tipoMensaje);
        JOptionPane optionPane = new JOptionPane(mensajeConIcono, JOptionPane.PLAIN_MESSAGE, tipoOpcion);
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

    static JPanel envolverContenidoConIcono(Component contenido, int tipoMensaje) {
        JPanel contenedor = new JPanel(new BorderLayout(12, 0));
        contenedor.setOpaque(false);
        JLabel icono = crearEtiquetaIconoDialogo(tipoMensaje);
        if (icono != null) {
            contenedor.add(icono, BorderLayout.WEST);
        }
        contenedor.add(contenido, BorderLayout.CENTER);
        return contenedor;
    }

    static JLabel crearEtiquetaIconoDialogo(int tipoMensaje) {
        Icon icono = resolverIconoDialogo(tipoMensaje);
        if (icono == null) {
            return null;
        }
        JLabel etiqueta = new JLabel(icono);
        etiqueta.setVerticalAlignment(SwingConstants.TOP);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        return etiqueta;
    }

    static Icon resolverIconoDialogo(int tipoMensaje) {
        switch (tipoMensaje) {
            case JOptionPane.ERROR_MESSAGE:
                return UIManager.getIcon("OptionPane.errorIcon");
            case JOptionPane.WARNING_MESSAGE:
                return UIManager.getIcon("OptionPane.warningIcon");
            case JOptionPane.QUESTION_MESSAGE:
                return UIManager.getIcon("OptionPane.questionIcon");
            case JOptionPane.INFORMATION_MESSAGE:
                return UIManager.getIcon("OptionPane.informationIcon");
            default:
                return null;
        }
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

    public static void copiarAlPortapapeles(String texto) {
        if (Normalizador.esVacio(texto))
            return;
        try {
            StringSelection selection = new StringSelection(texto);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.tr("No se pudo copiar contenido al portapapeles"), e);
        }
    }

    public static void mostrarDetallesErrorAvanzado(Component parent, String url, String error) {
        ejecutarEnEdt(() -> {
            JDialog dialogo = new JDialog(resolverVentanaDialogoEstable(
                    parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent)),
                    I18nUI.Tareas.TITULO_DETALLES_ERROR(), Dialog.ModalityType.APPLICATION_MODAL);

            dialogo.setLayout(new BorderLayout(0, 0));
            dialogo.setResizable(true);
            dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
            panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panelPrincipal.setBackground(EstilosUI.obtenerFondoPanel());

            JPanel panelCabecera = new JPanel(new BorderLayout(5, 5));
            panelCabecera.setOpaque(false);

            JLabel lblTitulo = new JLabel(
                    I18nUI.Tareas.ENCABEZADO_DETALLES_ERROR());
            lblTitulo.setFont(EstilosUI.FUENTE_NEGRITA);
            lblTitulo.setForeground(EstilosUI.COLOR_TEXTO_NORMAL);

            JTextField txtUrl = new JTextField(url);
            txtUrl.setEditable(false);
            txtUrl.setBorder(null);
            txtUrl.setOpaque(false);
            txtUrl.setFont(EstilosUI.FUENTE_ESTANDAR);
            txtUrl.setForeground(EstilosUI.colorTextoSecundario(panelPrincipal.getBackground()));

            panelCabecera.add(lblTitulo, BorderLayout.NORTH);
            panelCabecera.add(txtUrl, BorderLayout.CENTER);
            panelPrincipal.add(panelCabecera, BorderLayout.NORTH);

            JTextArea areaError = new JTextArea(error != null ? error : "");
            areaError.setEditable(false);
            areaError.setLineWrap(true);
            areaError.setWrapStyleWord(true);
            areaError.setFont(EstilosUI.FUENTE_MONO);
            areaError.setBackground(EstilosUI.colorFondoSecundario(panelPrincipal.getBackground()));
            areaError.setForeground(EstilosUI.COLOR_TEXTO_NORMAL);
            areaError.setCaretPosition(0);

            JScrollPane scroll = new JScrollPane(areaError);
            scroll.setBorder(BorderFactory.createLineBorder(EstilosUI.colorSeparador(panelPrincipal.getBackground())));
            panelPrincipal.add(scroll, BorderLayout.CENTER);

            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            panelBotones.setOpaque(false);

            JButton btnCopiar = new JButton(I18nUI.General.BOTON_COPIAR());
            btnCopiar.setToolTipText(I18nUI.Tooltips.General.COPIAR_PORTAPAPELES());
            btnCopiar.addActionListener(e -> copiarAlPortapapeles(error));

            JButton btnCerrar = new JButton(I18nUI.General.BOTON_CERRAR());
            btnCerrar.setToolTipText(I18nUI.Tooltips.General.CERRAR_DIALOGO());
            btnCerrar.addActionListener(e -> dialogo.dispose());
            btnCerrar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);

            panelBotones.add(btnCopiar);
            panelBotones.add(btnCerrar);
            panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

            dialogo.add(panelPrincipal, BorderLayout.CENTER);
            dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            dialogo.pack();
            if (dialogo.getWidth() < 500)
                dialogo.setSize(500, dialogo.getHeight());
            if (dialogo.getHeight() < 300)
                dialogo.setSize(dialogo.getWidth(), 300);
            dialogo.setLocationRelativeTo(parent);
            dialogo.setVisible(true);
        });
    }
}
