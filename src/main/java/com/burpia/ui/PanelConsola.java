package com.burpia.ui;

import com.burpia.util.GestorConsolaGUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class PanelConsola extends JPanel {
    private final JTextPane consola;
    private final JCheckBox checkboxAutoScroll;
    private final JButton botonLimpiar;
    private final JLabel etiquetaResumen;
    private final GestorConsolaGUI gestorConsola;
    private final Timer timerActualizacion;

    public PanelConsola(GestorConsolaGUI gestorConsola) {
        this.gestorConsola = gestorConsola;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "⚙️ CONTROLES DE CONSOLA",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));

        checkboxAutoScroll = new JCheckBox("📜 Auto-scroll", true);
        checkboxAutoScroll.setFont(EstilosUI.FUENTE_ESTANDAR);
        checkboxAutoScroll.setToolTipText("Desplazamiento automático al recibir nuevos logs");
        panelBotones.add(checkboxAutoScroll);

        botonLimpiar = new JButton("🧹 Limpiar Consola");
        botonLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiar.setToolTipText("Eliminar todos los logs de la consola");
        panelBotones.add(botonLimpiar);

        etiquetaResumen = new JLabel("📊 Total LOGs: 0 | ℹ️ Informativos: 0 | 🔍 Detallados: 0 | ❌ Errores: 0");
        etiquetaResumen.setFont(EstilosUI.FUENTE_MONO);
        panelBotones.add(etiquetaResumen);

        panelControles.add(panelBotones);

        JPanel panelConsolaWrapper = new JPanel(new BorderLayout());
        panelConsolaWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "📝 LOGS DEL SISTEMA",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Consola con colores
        consola = new JTextPane();
        consola.setEditable(false);
        consola.setFont(EstilosUI.FUENTE_TABLA);
        JScrollPane panelDesplazable = new JScrollPane(consola);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gestorConsola.establecerConsola(consola);

        checkboxAutoScroll.addActionListener(e -> {
            gestorConsola.establecerAutoScroll(checkboxAutoScroll.isSelected());
        });

        botonLimpiar.addActionListener(e -> {
            int total = gestorConsola.obtenerTotalLogs();
            if (total == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "La consola ya está vacía.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Limpiar " + total + " log(s) de la consola?",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorConsola.limpiarConsola();
            }
        });

        // Timer para actualizar resumen cada segundo
        timerActualizacion = new Timer(1000, e -> actualizarResumen());
        timerActualizacion.start();

        panelConsolaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        add(panelControles, BorderLayout.NORTH);
        add(panelConsolaWrapper, BorderLayout.CENTER);

        // Log inicial
        gestorConsola.registrarInfo("Consola GUI inicializada");
    }

    private void actualizarResumen() {
        SwingUtilities.invokeLater(() -> {
            etiquetaResumen.setText(String.format(
                "📊 Total LOGs: %d | ℹ️ Informativos: %d | 🔍 Detallados: %d | ❌ Errores: %d",
                gestorConsola.obtenerTotalLogs(),
                gestorConsola.obtenerContadorInfo(),
                gestorConsola.obtenerContadorVerbose(),
                gestorConsola.obtenerContadorError()
            ));
        });
    }

    public void destruir() {
        timerActualizacion.stop();
    }

    public GestorConsolaGUI obtenerGestorConsola() {
        return gestorConsola;
    }
}
