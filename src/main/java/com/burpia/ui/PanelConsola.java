package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelConsola extends JPanel {
    private final JTextPane consola;
    private final JCheckBox checkboxAutoScroll;
    private final JButton botonLimpiar;
    private final JLabel etiquetaResumen;
    private final GestorConsolaGUI gestorConsola;
    private final Timer timerActualizacion;
    private final JPanel panelControles;
    private final JPanel panelConsolaWrapper;
    private Consumer<Boolean> manejadorCambioAutoScroll;
    private boolean actualizandoAutoScroll = false;
    private volatile int ultimaVersionConsola = -1;

    @SuppressWarnings("this-escape")
    public PanelConsola(GestorConsolaGUI gestorConsola) {
        this.gestorConsola = gestorConsola;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Consola.TITULO_CONTROLES(), 12, 16));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));

        checkboxAutoScroll = new JCheckBox(I18nUI.Consola.CHECK_AUTO_SCROLL(), true);
        checkboxAutoScroll.setFont(EstilosUI.FUENTE_ESTANDAR);
        checkboxAutoScroll.setToolTipText(I18nUI.Tooltips.Consola.AUTOSCROLL());
        panelBotones.add(checkboxAutoScroll);

        botonLimpiar = new JButton(I18nUI.Consola.BOTON_LIMPIAR());
        botonLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiar.setToolTipText(I18nUI.Tooltips.Consola.LIMPIAR());
        panelBotones.add(botonLimpiar);

        etiquetaResumen = new JLabel(I18nUI.Consola.RESUMEN(0, 0, 0, 0));
        etiquetaResumen.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumen.setToolTipText(I18nUI.Tooltips.Consola.RESUMEN());
        panelBotones.add(etiquetaResumen);

        panelControles.add(panelBotones);

        panelConsolaWrapper = new JPanel(new BorderLayout());
        panelConsolaWrapper.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Consola.TITULO_LOGS(), 12, 16));

        consola = new JTextPane();
        consola.setEditable(false);
        consola.setFont(EstilosUI.FUENTE_TABLA);
        consola.setToolTipText(I18nUI.Tooltips.Consola.AREA_LOGS());
        JScrollPane panelDesplazable = new JScrollPane(consola);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gestorConsola.establecerConsola(consola);

        checkboxAutoScroll.addActionListener(e -> {
            if (actualizandoAutoScroll) {
                return;
            }
            aplicarAutoScroll(checkboxAutoScroll.isSelected(), true);
        });

        botonLimpiar.addActionListener(e -> {
            int total = gestorConsola.obtenerTotalLogs();
            if (total == 0) {
                UIUtils.mostrarInfo(this, I18nUI.Consola.TITULO_INFORMACION(), I18nUI.Consola.MSG_CONSOLA_VACIA());
                return;
            }

            boolean confirmacion = UIUtils.confirmarPregunta(
                this,
                I18nUI.Consola.TITULO_CONFIRMAR_LIMPIEZA(),
                I18nUI.Consola.MSG_CONFIRMAR_LIMPIEZA(total)
            );
            if (confirmacion) {
                gestorConsola.limpiarConsola();
            }
        });

        timerActualizacion = new Timer(1000, e -> actualizarResumen());
        timerActualizacion.start();

        panelConsolaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        add(panelControles, BorderLayout.NORTH);
        add(panelConsolaWrapper, BorderLayout.CENTER);

        aplicarTema();
        gestorConsola.registrarInfo(I18nUI.Consola.LOG_INICIALIZADA());
    }

    private void actualizarResumen() {
        actualizarResumen(false);
    }

    private void actualizarResumen(boolean forzar) {
        int versionActual = gestorConsola.obtenerVersion();
        if (!forzar && versionActual == ultimaVersionConsola) {
            return;
        }
        ultimaVersionConsola = versionActual;
        Runnable actualizarUi = () -> etiquetaResumen.setText(I18nUI.Consola.RESUMEN(
            gestorConsola.obtenerTotalLogs(),
            gestorConsola.obtenerContadorInfo(),
            gestorConsola.obtenerContadorVerbose(),
            gestorConsola.obtenerContadorError()
        ));
        if (SwingUtilities.isEventDispatchThread()) {
            actualizarUi.run();
        } else {
            ejecutarEnEdt(actualizarUi);
        }
    }

    public void aplicarIdioma() {
        checkboxAutoScroll.setText(I18nUI.Consola.CHECK_AUTO_SCROLL());
        botonLimpiar.setText(I18nUI.Consola.BOTON_LIMPIAR());
        UIUtils.actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        UIUtils.actualizarTituloPanel(panelConsolaWrapper, I18nUI.Consola.TITULO_LOGS());
        checkboxAutoScroll.setToolTipText(I18nUI.Tooltips.Consola.AUTOSCROLL());
        botonLimpiar.setToolTipText(I18nUI.Tooltips.Consola.LIMPIAR());
        etiquetaResumen.setToolTipText(I18nUI.Tooltips.Consola.RESUMEN());
        consola.setToolTipText(I18nUI.Tooltips.Consola.AREA_LOGS());
        aplicarTema();
        actualizarResumen(true);
        revalidate();
        repaint();
    }

    public void aplicarTema() {
        Runnable aplicar = () -> {
            Color fondoPanel = EstilosUI.obtenerFondoPanel();
            Color fondoConsola = UIManager.getColor("TextPane.background");
            if (fondoConsola == null) {
                fondoConsola = EstilosUI.colorFondoSecundario(fondoPanel);
            }

            setBackground(fondoPanel);
            panelControles.setBackground(fondoPanel);
            panelConsolaWrapper.setBackground(fondoPanel);
            checkboxAutoScroll.setOpaque(false);

            consola.setBackground(fondoConsola);
            consola.setForeground(EstilosUI.colorTextoPrimario(fondoConsola));
            consola.setCaretColor(EstilosUI.colorTextoPrimario(fondoConsola));

            etiquetaResumen.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));

            panelControles.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), 12, 16));
            panelConsolaWrapper.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_LOGS(), 12, 16));
            gestorConsola.aplicarTemaConsola(consola);

            revalidate();
            repaint();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            aplicar.run();
        } else {
            ejecutarEnEdt(aplicar);
        }
    }

    public void destruir() {
        timerActualizacion.stop();
    }

    public GestorConsolaGUI obtenerGestorConsola() {
        return gestorConsola;
    }

    public void establecerManejadorCambioAutoScroll(Consumer<Boolean> manejadorCambioAutoScroll) {
        this.manejadorCambioAutoScroll = manejadorCambioAutoScroll;
    }

    public void establecerAutoScrollActivo(boolean activo) {
        UIUtils.ejecutarEnEdtYEsperar(() -> aplicarAutoScrollEnEdt(activo, false));
    }

    public boolean isAutoScrollActivo() {
        return checkboxAutoScroll.isSelected();
    }

    private void aplicarAutoScroll(boolean activo, boolean notificarCambio) {
        if (!SwingUtilities.isEventDispatchThread()) {
            ejecutarEnEdt(() -> aplicarAutoScroll(activo, notificarCambio));
            return;
        }
        aplicarAutoScrollEnEdt(activo, notificarCambio);
    }

    private void aplicarAutoScrollEnEdt(boolean activo, boolean notificarCambio) {
        boolean estadoActual = checkboxAutoScroll.isSelected();

        if (estadoActual != activo) {
            actualizandoAutoScroll = true;
            try {
                checkboxAutoScroll.setSelected(activo);
            } finally {
                actualizandoAutoScroll = false;
            }
        }

        gestorConsola.establecerAutoScroll(activo);

        if (notificarCambio && manejadorCambioAutoScroll != null && estadoActual != activo) {
            manejadorCambioAutoScroll.accept(activo);
        }
    }

}
