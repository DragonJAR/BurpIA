package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.util.Normalizador;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Diálogo para ver y editar los detalles de un hallazgo de seguridad.
 * Permite modificar URL, título, descripción, severidad y confianza.
 */
public class DialogoDetalleHallazgo extends JDialog {
    private final Hallazgo hallazgoOriginal;
    private final Consumer<Hallazgo> alGuardar;

    private JTextField txtUrl;
    private JTextField txtTitulo;
    private JTextArea txtDescripcion;
    private JComboBox<String> comboSeveridad;
    private JComboBox<String> comboConfianza;

    /**
     * Crea un nuevo diálogo para editar un hallazgo.
     *
     * @param padre   Ventana padre del diálogo
     * @param hallazgo Hallazgo a editar, puede ser null para crear uno nuevo
     * @param alGuardar Callback que se ejecuta al guardar los cambios
     */
    @SuppressWarnings("this-escape")
    public DialogoDetalleHallazgo(Window padre, Hallazgo hallazgo, Consumer<Hallazgo> alGuardar) {
        super(padre, I18nUI.DetalleHallazgo.TITULO_DIALOGO(), Dialog.ModalityType.APPLICATION_MODAL);
        this.hallazgoOriginal = hallazgo;
        this.alGuardar = alGuardar;

        inicializarComponentes();
        cargarDatos();
        instalarIndicadorSucio();
    }

    private static final String SUFIJO_SUCIO = " •";

    /**
     * Marca el título con un sufijo cuando el usuario edita algún campo, para señalar
     * visualmente que hay cambios sin guardar antes de cerrar/cancelar.
     */
    private void instalarIndicadorSucio() {
        javax.swing.event.DocumentListener marcador = UIUtils.crearDocumentListener(() -> marcarTituloSucio(true));
        txtUrl.getDocument().addDocumentListener(marcador);
        txtTitulo.getDocument().addDocumentListener(marcador);
        txtDescripcion.getDocument().addDocumentListener(marcador);
        comboSeveridad.addActionListener(e -> marcarTituloSucio(true));
        comboConfianza.addActionListener(e -> marcarTituloSucio(true));
    }

    private void marcarTituloSucio(boolean sucio) {
        String base = I18nUI.DetalleHallazgo.TITULO_DIALOGO();
        String actual = getTitle();
        boolean yaSucio = actual != null && actual.endsWith(SUFIJO_SUCIO);
        if (sucio && !yaSucio) {
            setTitle(base + SUFIJO_SUCIO);
        } else if (!sucio && yaSucio) {
            setTitle(base);
        }
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setSize(700, 500);
        setMinimumSize(new Dimension(500, 360));
        setLocationRelativeTo(getParent());
        // Evitar descarte silencioso: la X y el botón Cancelar pasan por el mismo confirmar.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cerrarConConfirmacionSiSucio();
            }
        });

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
        lblUrl.setLabelFor(txtUrl);
        panelContenido.add(lblUrl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtUrl = new JTextField(30);
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtUrl.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.URL());
        txtUrl.setInputVerifier(UIUtils.crearInputVerifierUrl());
        panelContenido.add(txtUrl, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblTitulo = new JLabel(I18nUI.DetalleHallazgo.LABEL_TITULO());
        lblTitulo.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.TITULO());
        lblTitulo.setLabelFor(txtTitulo);
        panelContenido.add(lblTitulo, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtTitulo = new JTextField(30);
        txtTitulo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtTitulo.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.TITULO());
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

        JLabel lblConfianza = new JLabel(I18nUI.DetalleHallazgo.LABEL_CONFIANZA());
        lblConfianza.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblConfianza.setToolTipText(I18nUI.Tooltips.DetalleHallazgo.CONFIANZA());
        panelClasificacion.add(lblConfianza);

        comboConfianza = new JComboBox<>(I18nUI.Hallazgos.OPCIONES_CONFIANZA());
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
        btnCancelar.addActionListener(e -> cerrarConConfirmacionSiSucio());

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

        // Validación por campo: indica exactamente qué campo falló, no un mensaje genérico.
        String errorCampo = validarCampos(nuevaUrl, nuevoTitulo, nuevaDescripcion);
        if (errorCampo != null) {
            UIUtils.mostrarError(this, I18nUI.DetalleHallazgo.TITULO_ERROR_VALIDACION(), errorCampo);
            return;
        }

        Hallazgo resultado;
        if (hallazgoOriginal != null) {
            resultado = hallazgoOriginal.editar(nuevaUrl, nuevoTitulo, nuevaDescripcion, nuevaSeveridad, nuevaConfianza);
        } else {
            resultado = new Hallazgo(nuevaUrl, nuevoTitulo, nuevaDescripcion, nuevaSeveridad, nuevaConfianza);
        }

        if (alGuardar != null) {
            alGuardar.accept(resultado);
        }
        UIUtils.mostrarInfo(this, I18nUI.DetalleHallazgo.TITULO_DIALOGO(), I18nUI.DetalleHallazgo.MSG_GUARDADO_OK());
        dispose();
    }

    /**
     * Valida los campos obligatorios y devuelve el mensaje de error del primer campo inválido,
     * o null si todos son válidos.
     */
    private static String validarCampos(String url, String titulo, String descripcion) {
        if (Normalizador.esVacio(url)) {
            return I18nUI.DetalleHallazgo.MSG_VALIDACION_URL();
        }
        if (Normalizador.esVacio(titulo)) {
            return I18nUI.DetalleHallazgo.MSG_VALIDACION_TITULO();
        }
        if (Normalizador.esVacio(descripcion)) {
            return I18nUI.DetalleHallazgo.MSG_VALIDACION_DESCRIPCION();
        }
        return null;
    }

    /**
     * Cierra el diálogo descartando cambios, pero solo tras confirmar si el usuario editó algo.
     */
    private void cerrarConConfirmacionSiSucio() {
        if (tieneCambiosSinGuardar() && !UIUtils.confirmarAdvertencia(
                this,
                I18nUI.DetalleHallazgo.TITULO_CONFIRMAR_DESCARTE(),
                I18nUI.DetalleHallazgo.MSG_CONFIRMAR_DESCARTE())) {
            return;
        }
        dispose();
    }

    private boolean tieneCambiosSinGuardar() {
        if (hallazgoOriginal == null) {
            // Hallazgo nuevo: cualquier contenido cuenta como cambio.
            return Normalizador.noEsVacio(txtUrl.getText())
                    || Normalizador.noEsVacio(txtTitulo.getText())
                    || txtDescripcion.getDocument().getLength() > 0;
        }
        return !java.util.Objects.equals(txtUrl.getText(), valorONulo(hallazgoOriginal.obtenerUrl()))
                || !java.util.Objects.equals(txtTitulo.getText(), valorONulo(hallazgoOriginal.obtenerTitulo()))
                || !java.util.Objects.equals(txtDescripcion.getText(), valorONulo(hallazgoOriginal.obtenerHallazgo()));
    }

    private static String valorONulo(String valor) {
        return valor != null ? valor : "";
    }
}
