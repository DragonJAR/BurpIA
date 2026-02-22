package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.function.Consumer;

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

    public PanelConsola(GestorConsolaGUI gestorConsola) {
        this.gestorConsola = gestorConsola;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Consola.TITULO_CONTROLES(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));

        checkboxAutoScroll = new JCheckBox(I18nUI.Consola.CHECK_AUTO_SCROLL(), true);
        checkboxAutoScroll.setFont(EstilosUI.FUENTE_ESTANDAR);
        checkboxAutoScroll.setToolTipText(TooltipsUI.Consola.AUTOSCROLL());
        panelBotones.add(checkboxAutoScroll);

        botonLimpiar = new JButton(I18nUI.Consola.BOTON_LIMPIAR());
        botonLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiar.setToolTipText(TooltipsUI.Consola.LIMPIAR());
        panelBotones.add(botonLimpiar);

        etiquetaResumen = new JLabel(I18nUI.Consola.RESUMEN(0, 0, 0, 0));
        etiquetaResumen.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumen.setToolTipText(TooltipsUI.Consola.RESUMEN());
        panelBotones.add(etiquetaResumen);

        panelControles.add(panelBotones);

        panelConsolaWrapper = new JPanel(new BorderLayout());
        panelConsolaWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Consola.TITULO_LOGS(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        consola = new JTextPane();
        consola.setEditable(false);
        consola.setFont(EstilosUI.FUENTE_TABLA);
        consola.setToolTipText(TooltipsUI.Consola.AREA_LOGS());
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
                JOptionPane.showMessageDialog(
                    this,
                    I18nUI.Consola.MSG_CONSOLA_VACIA(),
                    I18nUI.Consola.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                I18nUI.Consola.MSG_CONFIRMAR_LIMPIEZA(total),
                I18nUI.Consola.TITULO_CONFIRMAR_LIMPIEZA(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorConsola.limpiarConsola();
            }
        });

        timerActualizacion = new Timer(1000, e -> actualizarResumen());
        timerActualizacion.start();

        panelConsolaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        add(panelControles, BorderLayout.NORTH);
        add(panelConsolaWrapper, BorderLayout.CENTER);

        gestorConsola.registrarInfo(I18nUI.Consola.LOG_INICIALIZADA());
    }

    private void actualizarResumen() {
        SwingUtilities.invokeLater(() -> {
            etiquetaResumen.setText(I18nUI.Consola.RESUMEN(
                gestorConsola.obtenerTotalLogs(),
                gestorConsola.obtenerContadorInfo(),
                gestorConsola.obtenerContadorVerbose(),
                gestorConsola.obtenerContadorError()
            ));
        });
    }

    public void aplicarIdioma() {
        checkboxAutoScroll.setText(I18nUI.Consola.CHECK_AUTO_SCROLL());
        botonLimpiar.setText(I18nUI.Consola.BOTON_LIMPIAR());
        actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        actualizarTituloPanel(panelConsolaWrapper, I18nUI.Consola.TITULO_LOGS());
        checkboxAutoScroll.setToolTipText(TooltipsUI.Consola.AUTOSCROLL());
        botonLimpiar.setToolTipText(TooltipsUI.Consola.LIMPIAR());
        etiquetaResumen.setToolTipText(TooltipsUI.Consola.RESUMEN());
        consola.setToolTipText(TooltipsUI.Consola.AREA_LOGS());
        actualizarResumen();
        revalidate();
        repaint();
    }

    private void actualizarTituloPanel(JPanel panel, String titulo) {
        Border borde = panel.getBorder();
        if (borde instanceof CompoundBorder) {
            Border externo = ((CompoundBorder) borde).getOutsideBorder();
            if (externo instanceof TitledBorder) {
                ((TitledBorder) externo).setTitle(titulo);
            }
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
        aplicarAutoScroll(activo, false);
    }

    public boolean isAutoScrollActivo() {
        return checkboxAutoScroll.isSelected();
    }

    private void aplicarAutoScroll(boolean activo, boolean notificarCambio) {
        if (checkboxAutoScroll.isSelected() != activo) {
            actualizandoAutoScroll = true;
            try {
                checkboxAutoScroll.setSelected(activo);
            } finally {
                actualizandoAutoScroll = false;
            }
        }
        gestorConsola.establecerAutoScroll(activo);
        if (notificarCambio && manejadorCambioAutoScroll != null) {
            manejadorCambioAutoScroll.accept(activo);
        }
    }
}
