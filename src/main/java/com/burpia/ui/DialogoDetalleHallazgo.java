package com.burpia.ui;

import com.burpia.model.Hallazgo;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DialogoDetalleHallazgo extends JDialog {
    private final Hallazgo hallazgoOriginal;
    private final Consumer<Hallazgo> alGuardar;

    private JTextField txtUrl;
    private JTextArea txtDescripcion;
    private JComboBox<String> comboSeveridad;
    private JComboBox<String> comboConfianza;

    public DialogoDetalleHallazgo(Window padre, Hallazgo hallazgo, Consumer<Hallazgo> alGuardar) {
        super(padre, "💎 Detalle de Hallazgo", Dialog.ModalityType.APPLICATION_MODAL);
        this.hallazgoOriginal = hallazgo;
        this.alGuardar = alGuardar;

        inicializarComponentes();
        cargarDatos();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setSize(700, 500);
        setLocationRelativeTo(getParent());

        JPanel panelContenido = new JPanel(new GridBagLayout());
        panelContenido.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "✏️ EDITAR HALLAZGO",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;

        // URL
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelContenido.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtUrl = new JTextField();
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        panelContenido.add(txtUrl, gbc);

        fila++;

        // Severidad y Confianza en la misma fila
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelContenido.add(new JLabel("Severidad:"), gbc);

        JPanel panelClasificacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboSeveridad = new JComboBox<>(new String[]{
            Hallazgo.SEVERIDAD_CRITICAL,
            Hallazgo.SEVERIDAD_HIGH,
            Hallazgo.SEVERIDAD_MEDIUM,
            Hallazgo.SEVERIDAD_LOW,
            Hallazgo.SEVERIDAD_INFO
        });
        comboSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelClasificacion.add(comboSeveridad);

        JLabel lblEspacio = new JLabel("       Confianza: ");
        lblEspacio.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelClasificacion.add(lblEspacio);

        comboConfianza = new JComboBox<>(new String[]{
            Hallazgo.CONFIANZA_ALTA,
            Hallazgo.CONFIANZA_MEDIA,
            Hallazgo.CONFIANZA_BAJA
        });
        comboConfianza.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelClasificacion.add(comboConfianza);

        gbc.gridx = 1; gbc.weightx = 1;
        panelContenido.add(panelClasificacion, gbc);

        fila++;

        // Descripción (Hallazgo)
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelContenido.add(new JLabel("Descripción:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        txtDescripcion = new JTextArea();
        txtDescripcion.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        
        JScrollPane scrollDescripcion = new JScrollPane(txtDescripcion);
        scrollDescripcion.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelContenido.add(scrollDescripcion, gbc);

        // Panel Principal con espaciado global
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panelPrincipal.add(panelContenido, BorderLayout.CENTER);

        // Botones de acción
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        JButton btnGuardar = new JButton("💾 Guardar Cambios");
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.addActionListener(e -> guardarYSalir());

        JButton btnCancelar = new JButton("❌ Cancelar");
        btnCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        add(panelPrincipal, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        if (hallazgoOriginal != null) {
            txtUrl.setText(hallazgoOriginal.obtenerUrl());
            txtDescripcion.setText(hallazgoOriginal.obtenerHallazgo());
            comboSeveridad.setSelectedItem(hallazgoOriginal.obtenerSeveridad());
            comboConfianza.setSelectedItem(hallazgoOriginal.obtenerConfianza());
        }
    }

    private void guardarYSalir() {
        String nuevaUrl = txtUrl.getText().trim();
        String nuevaDescripcion = txtDescripcion.getText().trim();
        String nuevaSeveridad = (String) comboSeveridad.getSelectedItem();
        String nuevaConfianza = (String) comboConfianza.getSelectedItem();

        if (nuevaUrl.isEmpty() || nuevaDescripcion.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "La URL y la descripción no pueden estar vacías.",
                "Error de validación",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Hallazgo hallazgoEditado = new Hallazgo(
            hallazgoOriginal.obtenerHoraDescubrimiento(),
            nuevaUrl,
            nuevaDescripcion,
            nuevaSeveridad,
            nuevaConfianza,
            hallazgoOriginal.obtenerSolicitudHttp()
        );

        alGuardar.accept(hallazgoEditado);
        dispose();
    }
}
