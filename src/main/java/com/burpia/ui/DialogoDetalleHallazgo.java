package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DialogoDetalleHallazgo extends JDialog {
    private final Hallazgo hallazgoOriginal;
    private final Consumer<Hallazgo> alGuardar;

    private JTextField txtUrl;
    private JTextField txtTitulo;
    private JTextArea txtDescripcion;
    private JComboBox<String> comboSeveridad;
    private JComboBox<String> comboConfianza;

    public DialogoDetalleHallazgo(Window padre, Hallazgo hallazgo, Consumer<Hallazgo> alGuardar) {
        super(padre, I18nUI.DetalleHallazgo.TITULO_DIALOGO(), Dialog.ModalityType.APPLICATION_MODAL);
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
        panelContenido.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.DetalleHallazgo.TITULO_PANEL(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblUrl = new JLabel(I18nUI.DetalleHallazgo.LABEL_URL());
        lblUrl.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.URL());
        panelContenido.add(lblUrl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtUrl = new JTextField();
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtUrl.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.URL());
        panelContenido.add(txtUrl, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblTitulo = new JLabel(I18nUI.DetalleHallazgo.LABEL_TITULO());
        panelContenido.add(lblTitulo, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtTitulo = new JTextField();
        txtTitulo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        panelContenido.add(txtTitulo, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblSeveridad = new JLabel(I18nUI.DetalleHallazgo.LABEL_SEVERIDAD());
        lblSeveridad.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.SEVERIDAD());
        panelContenido.add(lblSeveridad, gbc);

        JPanel panelClasificacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboSeveridad = new JComboBox<>(I18nUI.Hallazgos.OPCIONES_SEVERIDAD());
        comboSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboSeveridad.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.SEVERIDAD());
        panelClasificacion.add(comboSeveridad);

        JLabel lblEspacio = new JLabel(I18nUI.DetalleHallazgo.LABEL_CONFIANZA());
        lblEspacio.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblEspacio.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.CONFIANZA());
        panelClasificacion.add(lblEspacio);

        comboConfianza = new JComboBox<>(new String[]{
            I18nUI.Hallazgos.CONFIANZA_ALTA(),
            I18nUI.Hallazgos.CONFIANZA_MEDIA(),
            I18nUI.Hallazgos.CONFIANZA_BAJA()
        });
        comboConfianza.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboConfianza.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.CONFIANZA());
        panelClasificacion.add(comboConfianza);

        gbc.gridx = 1; gbc.weightx = 1;
        panelContenido.add(panelClasificacion, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblDescripcion = new JLabel(I18nUI.DetalleHallazgo.LABEL_DESCRIPCION());
        lblDescripcion.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.DESCRIPCION());
        panelContenido.add(lblDescripcion, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        txtDescripcion = new JTextArea();
        txtDescripcion.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        txtDescripcion.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.DESCRIPCION());

        JScrollPane scrollDescripcion = new JScrollPane(txtDescripcion);
        scrollDescripcion.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelContenido.add(scrollDescripcion, gbc);

        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panelPrincipal.add(panelContenido, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        JButton btnGuardar = new JButton(I18nUI.DetalleHallazgo.BOTON_GUARDAR());
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.GUARDAR());
        btnGuardar.addActionListener(e -> guardarYSalir());

        JButton btnCancelar = new JButton(I18nUI.DetalleHallazgo.BOTON_CANCELAR());
        btnCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnCancelar.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.CANCELAR());
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        add(panelPrincipal, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        if (hallazgoOriginal == null) {
            comboSeveridad.setSelectedItem(I18nUI.Hallazgos.SEVERIDAD_INFO());
            comboConfianza.setSelectedItem(I18nUI.Hallazgos.CONFIANZA_MEDIA());
            return;
        }
        txtUrl.setText(hallazgoOriginal.obtenerUrl());
        txtTitulo.setText(hallazgoOriginal.obtenerTitulo());
        txtDescripcion.setText(hallazgoOriginal.obtenerHallazgo());
        comboSeveridad.setSelectedItem(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(hallazgoOriginal.obtenerSeveridad()));
        comboConfianza.setSelectedItem(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(hallazgoOriginal.obtenerConfianza()));
    }

    private void guardarYSalir() {
        String nuevaUrl = txtUrl.getText().trim();
        String nuevoTitulo = txtTitulo.getText().trim();
        String nuevaDescripcion = txtDescripcion.getText().trim();
        String nuevaSeveridad = (String) comboSeveridad.getSelectedItem();
        String nuevaConfianza = (String) comboConfianza.getSelectedItem();

        if (nuevaUrl.isEmpty() || nuevaDescripcion.isEmpty() || nuevoTitulo.isEmpty()) {
            UIUtils.mostrarError(this, I18nUI.DetalleHallazgo.TITULO_ERROR_VALIDACION(), I18nUI.DetalleHallazgo.MSG_VALIDACION());
            return;
        }

        Hallazgo resultado;
        if (hallazgoOriginal != null) {
            resultado = hallazgoOriginal.editar(nuevaUrl, nuevoTitulo, nuevaDescripcion, nuevaSeveridad, nuevaConfianza);
        } else {
            resultado = new Hallazgo(nuevaUrl, nuevoTitulo, nuevaDescripcion,
                Hallazgo.normalizarSeveridad(nuevaSeveridad),
                Hallazgo.normalizarConfianza(nuevaConfianza));
        }

        if (alGuardar != null) {
            alGuardar.accept(resultado);
        }
        dispose();
    }
}
