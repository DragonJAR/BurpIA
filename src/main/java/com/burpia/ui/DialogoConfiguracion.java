package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.Normalizador;
import com.burpia.util.ProbadorConexionAI;
import com.burpia.util.OSUtils;
import com.burpia.util.VersionBurpIA;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DialogoConfiguracion extends JDialog {
    private static final int ANCHO_DIALOGO = 800;
    private static final int ALTO_DIALOGO = 720;

    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final Runnable alGuardar;

    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtRetraso;
    private JTextField txtMaximoConcurrente;
    private JTextField txtMaximoHallazgosTabla;
    private JCheckBox chkDetallado;
    private JCheckBox chkIgnorarSSL;
    private JCheckBox chkSoloProxy;
    private JCheckBox chkAlertasHabilitadas;
    private JCheckBox chkPersistirBusqueda;
    private JCheckBox chkPersistirSeveridad;
    private JTextArea txtPrompt;
    private JButton btnRestaurarPrompt;
    private JLabel lblContadorPrompt;

    private JComboBox<String> comboAgente;
    private JCheckBox chkAgenteHabilitado;
    private JTextField txtAgenteBinario;
    private JTextArea txtAgentePromptInicial;
    private JTextArea txtAgentePrompt;
    private JButton btnRestaurarPromptAgenteInicial;
    private JButton btnRestaurarPromptAgente;

    private JComboBox<String> comboProveedor;
    private JComboBox<String> comboModelo;
    private JComboBox<IdiomaUI> comboIdioma;
    private JButton btnRefrescarModelos;
    private JButton btnProbarConexion;
    private JButton btnBuscarActualizaciones;
    private JTextField txtMaxTokens;
    private JTextField txtTimeoutModelo;

    private JComboBox<String> comboFuenteEstandar;
    private JSpinner spinnerTamanioEstandar;
    private JComboBox<String> comboFuenteMono;
    private JSpinner spinnerTamanioMono;
    private JButton btnRestaurarFuentes;

    private JButton btnGuardar;
    private JButton btnCerrar;
    private JButton btnSitioWeb;
    private final Map<String, String> rutasBinarioAgenteTemporal = new HashMap<>();
    private final Map<String, EstadoProveedorUI> estadoProveedorTemporal = new HashMap<>();
    private final Map<String, List<String>> modelosProveedorTemporal = new HashMap<>();
    private String proveedorActualUi;
    private String codigoIdiomaUiActual;
    private boolean actualizandoProveedorUi = false;
    private boolean guardandoConfiguracion = false;
    private boolean actualizandoRutaFlag = false;
    private long secuenciaRefrescoModelos = 0L;

    private static final int TIMEOUT_CONEXION_MODELOS_SEG = 8;
    private static final int TIMEOUT_LECTURA_MODELOS_SEG = 12;
    private static final int TIMEOUT_CONEXION_ACTUALIZACIONES_SEG = 8;
    private static final int TIMEOUT_LECTURA_ACTUALIZACIONES_SEG = 12;

    @SuppressWarnings("this-escape")
    public DialogoConfiguracion(Window padre, ConfiguracionAPI config, GestorConfiguracion gestorConfig,
            Runnable alGuardar) {
        super(padre, I18nUI.Configuracion.TITULO_DIALOGO(), Dialog.ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.gestorConfig = gestorConfig;
        this.alGuardar = alGuardar;

        inicializarComponentes();
        aplicarTooltips();
        cargarConfiguracionActual();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));
        setPreferredSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));

        JTabbedPane tabbedPane = new JTabbedPane();

        inicializarCamposConfiguracion();

        JPanel tabAjustesUsuario = crearTabAjustesUsuario();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_AJUSTES_USUARIO(), tabAjustesUsuario);

        JPanel tabProveedor = crearTabProveedor();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROVEEDOR(), tabProveedor);

        JPanel panelPrompt = crearPanelPrompt();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROMPT(), panelPrompt);

        JPanel panelAgentes = crearPanelAgentes();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_AGENTES(), panelAgentes);

        JPanel panelAcercaDe = crearPanelAcercaDe();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_ACERCA(), panelAcercaDe);

        btnGuardar = new JButton(I18nUI.Configuracion.BOTON_GUARDAR());
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.addActionListener(e -> guardarConfiguracion());

        btnProbarConexion = new JButton(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
        btnProbarConexion.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnProbarConexion.addActionListener(e -> probarConexion());

        btnCerrar = new JButton(I18nUI.DetalleHallazgo.BOTON_CANCELAR());
        btnCerrar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnCerrar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        panelBotones.add(btnProbarConexion);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCerrar);

        add(tabbedPane, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        pack();
        setSize(Math.max(getWidth(), ANCHO_DIALOGO), Math.max(getHeight(), ALTO_DIALOGO));
        setLocationRelativeTo(getParent());
    }

    private void inicializarCamposConfiguracion() {
        txtRetraso = new JTextField(10);
        txtRetraso.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtRetraso.setToolTipText(I18nUI.Tooltips.Configuracion.RETRASO());
        txtMaximoConcurrente = new JTextField(10);
        txtMaximoConcurrente.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoConcurrente.setToolTipText(I18nUI.Tooltips.Configuracion.MAXIMO_CONCURRENTE());

        txtMaximoHallazgosTabla = new JTextField(10);
        txtMaximoHallazgosTabla.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoHallazgosTabla.setToolTipText(
                I18nUI.Tooltips.Configuracion.MAXIMO_HALLAZGOS() + " (" +
                        ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA + "-" +
                        ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA + ")");
        chkDetallado = new JCheckBox(I18nUI.Configuracion.CHECK_DETALLADO());
        chkDetallado.setToolTipText(I18nUI.Tooltips.Configuracion.DETALLADO());
        chkIgnorarSSL = new JCheckBox(I18nUI.Configuracion.LABEL_IGNORAR_SSL());
        chkIgnorarSSL.setToolTipText(I18nUI.Tooltips.Configuracion.IGNORAR_SSL());
        chkSoloProxy = new JCheckBox(I18nUI.Configuracion.LABEL_SOLO_PROXY());
        chkSoloProxy.setToolTipText(I18nUI.Tooltips.Configuracion.SOLO_PROXY());
        chkAlertasHabilitadas = new JCheckBox(I18nUI.Configuracion.LABEL_HABILITAR_ALERTAS());
        chkAlertasHabilitadas.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_ALERTAS());

        chkPersistirBusqueda = new JCheckBox(I18nUI.Configuracion.CHECK_PERSISTIR_FILTRO_BUSQUEDA());
        chkPersistirBusqueda.setToolTipText(I18nUI.Tooltips.Configuracion.PERSISTIR_FILTRO_BUSQUEDA());
        chkPersistirSeveridad = new JCheckBox(I18nUI.Configuracion.CHECK_PERSISTIR_FILTRO_SEVERIDAD());
        chkPersistirSeveridad.setToolTipText(I18nUI.Tooltips.Configuracion.PERSISTIR_FILTRO_SEVERIDAD());

        comboIdioma = new JComboBox<>(IdiomaUI.values());
        comboIdioma.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboIdioma.setToolTipText(I18nUI.Tooltips.Configuracion.IDIOMA());
        comboIdioma.addActionListener(e -> alCambiarIdiomaUi());

        String[] fuentes = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        comboFuenteEstandar = new JComboBox<>(fuentes);
        comboFuenteEstandar.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboFuenteMono = new JComboBox<>(fuentes);
        comboFuenteMono.setFont(EstilosUI.FUENTE_ESTANDAR);

        spinnerTamanioEstandar = new JSpinner(new SpinnerNumberModel(11, 6, 72, 1));
        spinnerTamanioMono = new JSpinner(new SpinnerNumberModel(12, 6, 72, 1));
    }

    private JPanel crearTabProveedor() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        contenido.setOpaque(false);

        JPanel panelProveedor = crearPanelProveedor();
        panelProveedor.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelProveedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelProveedor.getPreferredSize().height));
        contenido.add(panelProveedor);
        contenido.add(Box.createVerticalStrut(12));

        JPanel panelMulti = crearPanelMultiProveedor();
        panelMulti.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelMulti.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelMulti.getPreferredSize().height));
        contenido.add(panelMulti);

        root.add(contenido, BorderLayout.CENTER);
        return root;
    }

    private JPanel crearTabAjustesUsuario() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        contenido.setOpaque(false);

        JPanel panelAjustes = new JPanel(new GridBagLayout());
        panelAjustes.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelAjustes.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_AJUSTES_USUARIO(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_IDIOMA()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(comboIdioma, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_MAX_HALLAZGOS_TABLA()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(txtMaximoHallazgosTabla, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_MODO_DETALLADO()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(chkDetallado, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_SEGURIDAD_SSL()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(chkIgnorarSSL, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_FILTRO_HERRAMIENTAS()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(chkSoloProxy, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        panelAjustes.add(new JLabel(I18nUI.Configuracion.LABEL_ALERTAS()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelAjustes.add(chkAlertasHabilitadas, gbc);

        panelAjustes.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelAjustes.getPreferredSize().height));
        contenido.add(panelAjustes);
        contenido.add(Box.createVerticalStrut(15));

        JPanel panelFuentes = crearPanelConfiguracionFuentes();
        panelFuentes.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelFuentes.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelFuentes.getPreferredSize().height));
        contenido.add(panelFuentes);
        contenido.add(Box.createVerticalStrut(15));

        JPanel panelPersistenciaUI = crearPanelPersistenciaUI();
        panelPersistenciaUI.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPersistenciaUI.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelPersistenciaUI.getPreferredSize().height));
        contenido.add(panelPersistenciaUI);

        root.add(new JScrollPane(contenido), BorderLayout.CENTER);
        return root;
    }

    private JPanel crearPanelConfiguracionFuentes() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Configuracion.TITULO_FUENTES(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fuente Estándar
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_FUENTE_ESTANDAR()), gbc);

        JPanel panelFuenteEstandar = new JPanel(new BorderLayout(8, 0));
        panelFuenteEstandar.setOpaque(false);
        panelFuenteEstandar.add(comboFuenteEstandar, BorderLayout.CENTER);
        panelFuenteEstandar.add(spinnerTamanioEstandar, BorderLayout.EAST);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(panelFuenteEstandar, gbc);

        // Fuente Mono
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_FUENTE_MONO()), gbc);

        JPanel panelFuenteMono = new JPanel(new BorderLayout(8, 0));
        panelFuenteMono.setOpaque(false);
        panelFuenteMono.add(comboFuenteMono, BorderLayout.CENTER);
        panelFuenteMono.add(spinnerTamanioMono, BorderLayout.EAST);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(panelFuenteMono, gbc);

        // Botón Restaurar
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 12, 8, 12);

        btnRestaurarFuentes = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_FUENTES());
        btnRestaurarFuentes.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarFuentes.addActionListener(e -> restaurarFuentesPorDefecto());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRestaurarFuentes);
        panel.add(panelBoton, gbc);

        return panel;
    }

    private JPanel crearPanelPersistenciaUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PERSISTENCIA_UI(), 12, 16));

        JPanel panelChecks = new JPanel(new GridLayout(0, 2, 12, 8));
        panelChecks.setOpaque(false);

        chkPersistirBusqueda.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkPersistirSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);

        panelChecks.add(chkPersistirBusqueda);
        panelChecks.add(chkPersistirSeveridad);

        panelChecks.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(panelChecks);

        return panel;
    }

    private void restaurarFuentesPorDefecto() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_FUENTES(),
                I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            config.restaurarFuentesPorDefecto();
            actualizarCamposFuentes();
        }
    }

    private void actualizarCamposFuentes() {
        comboFuenteEstandar.setSelectedItem(config.obtenerNombreFuenteEstandar());
        spinnerTamanioEstandar.setValue(config.obtenerTamanioFuenteEstandar());
        comboFuenteMono.setSelectedItem(config.obtenerNombreFuenteMono());
        spinnerTamanioMono.setValue(config.obtenerTamanioFuenteMono());
    }

    private JPanel crearPanelMultiProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_MULTI_PROVEEDOR(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 16, 12, 16);
        gbc.anchor = GridBagConstraints.CENTER;

        JButton btnHabilitar = new JButton(I18nUI.Configuracion.BOTON_HABILITAR());
        btnHabilitar.setFont(EstilosUI.FUENTE_NEGRITA);
        btnHabilitar.setEnabled(false);

        JLabel lblPronto = new JLabel("✨ " + I18nUI.Configuracion.LABEL_PRONTO().toUpperCase() + " ✨");
        lblPronto.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        lblPronto.setForeground(EstilosUI.COLOR_INFO);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnHabilitar, gbc);

        gbc.gridx = 1;
        panel.add(Box.createHorizontalStrut(30), gbc);

        gbc.gridx = 2;
        panel.add(lblPronto, gbc);

        return panel;
    }

    private JPanel crearPanelProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PROVEEDOR(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDOR_AI()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        comboProveedor = new JComboBox<>(ProveedorAI.obtenerNombresProveedores().toArray(new String[0]));
        comboProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.PROVEEDOR());
        comboProveedor.addActionListener(e -> alCambiarProveedor());
        panel.add(comboProveedor, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_URL_API()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtUrl = new JTextField(30);
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtUrl.setToolTipText(I18nUI.Tooltips.Configuracion.URL_API());
        panel.add(txtUrl, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_CLAVE_API()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtClave = new JPasswordField(30);
        txtClave.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtClave.setToolTipText(I18nUI.Tooltips.Configuracion.CLAVE_API());
        panel.add(txtClave, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MODELO()), gbc);

        JPanel panelModelo = new JPanel(new BorderLayout(5, 0));
        comboModelo = new JComboBox<>();
        comboModelo.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboModelo.setEditable(true);
        comboModelo.setToolTipText(I18nUI.Tooltips.Configuracion.MODELO());
        comboModelo.addActionListener(e -> actualizarTimeoutModeloSeleccionado());
        panelModelo.add(comboModelo, BorderLayout.CENTER);

        btnRefrescarModelos = new JButton(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
        btnRefrescarModelos.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRefrescarModelos.setToolTipText(I18nUI.Tooltips.Configuracion.CARGAR_MODELOS());
        btnRefrescarModelos.addActionListener(e -> refrescarModelosDesdeAPI());
        panelModelo.add(btnRefrescarModelos, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(panelModelo, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        JLabel lblMaxTokens = new JLabel(I18nUI.Configuracion.LABEL_MAX_TOKENS());
        lblMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());
        panel.add(lblMaxTokens, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panelMaxTokens = new JPanel(new BorderLayout(5, 0));
        txtMaxTokens = new JTextField(30);
        txtMaxTokens.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());
        panelMaxTokens.add(txtMaxTokens, BorderLayout.CENTER);

        JLabel lblInfoTokens = new JLabel("ℹ️");
        lblInfoTokens.setToolTipText(I18nUI.Tooltips.Configuracion.INFO_TOKENS());
        panelMaxTokens.add(lblInfoTokens, BorderLayout.EAST);

        panel.add(panelMaxTokens, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_TIMEOUT_MODELO()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtTimeoutModelo = new JTextField(30);
        txtTimeoutModelo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());
        panel.add(txtTimeoutModelo, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_RETRASO()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtRetraso, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MAXIMO_CONCURRENTE()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtMaximoConcurrente, gbc);

        return panel;
    }

    private JPanel crearPanelPrompt() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel panelInstrucciones = new JPanel(new BorderLayout(0, 8));
        panelInstrucciones.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_INSTRUCCIONES(), 12, 16));
        panelInstrucciones.setBackground(EstilosUI.colorFondoSecundario(EstilosUI.obtenerFondoPanel()));

        JTextArea txtInstrucciones = new JTextArea();
        txtInstrucciones.setEditable(false);
        txtInstrucciones.setOpaque(false);
        txtInstrucciones.setWrapStyleWord(true);
        txtInstrucciones.setLineWrap(true);
        txtInstrucciones.setFont(EstilosUI.FUENTE_ESTANDAR);
        txtInstrucciones.setForeground(EstilosUI.colorTextoPrimario(panelInstrucciones.getBackground()));
        txtInstrucciones.setText(I18nUI.Configuracion.TEXTO_INSTRUCCIONES());
        txtInstrucciones.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));

        JTextArea ejemploJson = new JTextArea();
        ejemploJson.setEditable(false);
        ejemploJson.setFont(EstilosUI.FUENTE_TABLA);
        ejemploJson.setBackground(
                UIManager.getColor("TextPane.background") != null ? UIManager.getColor("TextPane.background")
                        : Color.WHITE);
        ejemploJson.setForeground(EstilosUI.colorTextoPrimario(ejemploJson.getBackground()));
        ejemploJson.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstilosUI.colorSeparador(ejemploJson.getBackground()), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        ejemploJson.setText(
                "{\"hallazgos\":[{\"titulo\":\"string\",\"descripcion\":\"string\",\"severidad\":\"Critical|High|Medium|Low|Info\",\"confianza\":\"High|Medium|Low\",\"evidencia\":\"string\"}]}");

        JPanel bloqueInstrucciones = new JPanel(new BorderLayout(0, 8));
        bloqueInstrucciones.setOpaque(false);
        bloqueInstrucciones.add(txtInstrucciones, BorderLayout.NORTH);
        bloqueInstrucciones.add(ejemploJson, BorderLayout.CENTER);
        bloqueInstrucciones.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panelInstrucciones.add(bloqueInstrucciones, BorderLayout.CENTER);
        panel.add(panelInstrucciones, BorderLayout.NORTH);

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PROMPT_ANALISIS(), 12, 16));

        txtPrompt = new JTextArea();
        txtPrompt.setFont(EstilosUI.FUENTE_TABLA);
        txtPrompt.setLineWrap(false);
        txtPrompt.setWrapStyleWord(false);
        txtPrompt.setTabSize(2);
        txtPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_EDITOR());

        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelEditor.add(scrollPrompt, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 6));
        panelSur.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel panelBotonesPrompt = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        btnRestaurarPrompt = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPrompt.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT());
        btnRestaurarPrompt.addActionListener(e -> restaurarPromptPorDefecto());
        panelBotonesPrompt.add(btnRestaurarPrompt);

        JPanel panelContador = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lblContadorPrompt = new JLabel(I18nUI.Configuracion.CONTADOR_CARACTERES(0));
        lblContadorPrompt.setFont(EstilosUI.FUENTE_TABLA);
        lblContadorPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.CONTADOR_PROMPT());
        panelContador.add(lblContadorPrompt);

        JPanel barraAcciones = new JPanel(new BorderLayout());
        barraAcciones.add(panelBotonesPrompt, BorderLayout.WEST);
        barraAcciones.add(panelContador, BorderLayout.EAST);
        panelSur.add(barraAcciones, BorderLayout.NORTH);

        JLabel etiquetaAdvertencia = new JLabel(I18nUI.Configuracion.ADVERTENCIA_PROMPT());
        etiquetaAdvertencia.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaAdvertencia.setForeground(EstilosUI.colorErrorAccesible(EstilosUI.obtenerFondoPanel()));
        panelSur.add(etiquetaAdvertencia, BorderLayout.SOUTH);

        panelEditor.add(panelSur, BorderLayout.SOUTH);

        panel.add(panelEditor, BorderLayout.CENTER);

        txtPrompt.getDocument().addDocumentListener(UIUtils.crearDocumentListener(this::actualizarContador));

        return panel;
    }

    private JPanel crearPanelAgentes() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel panelAgenteGeneral = new JPanel(new GridBagLayout());
        panelAgenteGeneral.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.Agentes.TITULO_EJECUCION_AGENTE(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panelAgenteGeneral.add(new JLabel(I18nUI.Configuracion.LABEL_SELECCIONAR_AGENTE()), gbc);

        comboAgente = new JComboBox<>(AgenteTipo.codigosDisponibles());
        comboAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboAgente.addActionListener(e -> alCambiarAgente());

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelAgenteGeneral.add(comboAgente, gbc);

        chkAgenteHabilitado = new JCheckBox(I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE());
        chkAgenteHabilitado.setFont(EstilosUI.FUENTE_ESTANDAR);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panelAgenteGeneral.add(chkAgenteHabilitado, gbc);

        txtAgenteBinario = new JTextField(30);
        txtAgenteBinario.getDocument()
                .addDocumentListener(UIUtils.crearDocumentListener(this::actualizarRutaEnMemoria));

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panelAgenteGeneral.add(new JLabel(I18nUI.Configuracion.Agentes.LABEL_RUTA_BINARIO()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelAgenteGeneral.add(txtAgenteBinario, gbc);

        JPanel panelPrompts = new JPanel(new GridLayout(2, 1, 0, 10));
        panelPrompts.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.Agentes.TITULO_PROMPTS_AGENTE(), 12, 16));

        txtAgentePromptInicial = new JTextArea(8, 40);
        txtAgentePromptInicial.setFont(EstilosUI.FUENTE_MONO);
        txtAgentePromptInicial.setLineWrap(true);
        txtAgentePromptInicial.setWrapStyleWord(false);
        txtAgentePromptInicial.setTabSize(2);

        btnRestaurarPromptAgenteInicial = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgenteInicial.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgenteInicial.addActionListener(
                e -> txtAgentePromptInicial.setText(ConfiguracionAPI.obtenerAgentePreflightPromptPorDefecto()));

        txtAgentePrompt = new JTextArea(10, 40);
        txtAgentePrompt.setFont(EstilosUI.FUENTE_MONO);
        txtAgentePrompt.setLineWrap(true);
        txtAgentePrompt.setWrapStyleWord(false);
        txtAgentePrompt.setTabSize(2);

        btnRestaurarPromptAgente = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgente.addActionListener(
                e -> txtAgentePrompt.setText(ConfiguracionAPI.obtenerAgentePromptPorDefecto()));

        panelPrompts.add(crearSeccionPromptAgente(
                I18nUI.Configuracion.Agentes.TITULO_PROMPT_INICIAL_AGENTE(),
                txtAgentePromptInicial,
                btnRestaurarPromptAgenteInicial,
                I18nUI.Configuracion.Agentes.DESCRIPCION_PROMPT_INICIAL_AGENTE()));
        panelPrompts.add(crearSeccionPromptAgente(
                I18nUI.Configuracion.Agentes.TITULO_PROMPT_AGENTE(),
                txtAgentePrompt,
                btnRestaurarPromptAgente,
                I18nUI.Configuracion.Agentes.DESCRIPCION_PROMPT_VALIDACION_AGENTE()));

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.add(panelAgenteGeneral, BorderLayout.NORTH);
        contenido.add(panelPrompts, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearSeccionPromptAgente(String titulo,
            JTextArea area,
            JButton botonRestaurar,
            String descripcion) {
        JPanel seccion = new JPanel(new BorderLayout(0, 6));
        seccion.setBorder(UIUtils.crearBordeTitulado(titulo, 8, 10));

        if (Normalizador.noEsVacio(descripcion)) {
            JLabel lblDescripcion = new JLabel(descripcion);
            lblDescripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
            lblDescripcion.setForeground(EstilosUI.colorTextoSecundario(EstilosUI.obtenerFondoPanel()));
            seccion.add(lblDescripcion, BorderLayout.NORTH);
        }

        JScrollPane scroll = new JScrollPane(area);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        seccion.add(scroll, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.add(botonRestaurar);
        seccion.add(acciones, BorderLayout.SOUTH);

        return seccion;
    }

    private JPanel crearPanelAcercaDe() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(EstilosUI.obtenerFondoPanel());

        // --- HERO SECTION (HORIZONTAL) ---
        JPanel heroPanel = new JPanel(new GridBagLayout());
        heroPanel.setBackground(EstilosUI.colorFondoSecundario(main.getBackground()));
        heroPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbcHero = new GridBagConstraints();
        gbcHero.gridx = 0;
        gbcHero.gridy = 0;
        gbcHero.gridheight = 2;
        gbcHero.anchor = GridBagConstraints.WEST;
        gbcHero.insets = new Insets(0, 0, 0, 25);

        JLabel lblIcono = new JLabel(I18nUI.Configuracion.ICONO_APP());
        lblIcono.setFont(EstilosUI.FUENTE_TITULO_BANNER);
        try {
            var resourceUrl = getClass().getResource("/logo.png");
            if (resourceUrl != null) {
                ImageIcon icon = new ImageIcon(resourceUrl);
                Image img = icon.getImage();
                Image newimg = img.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH);
                lblIcono = new JLabel(new ImageIcon(newimg));
            }
        } catch (Exception ignored) {
        }
        heroPanel.add(lblIcono, gbcHero);

        // Text Panel for Title/Subtitle
        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setOpaque(false);
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 0;
        gbcText.gridy = 0;
        gbcText.anchor = GridBagConstraints.WEST;

        JLabel lblTitulo = new JLabel(I18nUI.Configuracion.TITULO_APP());
        lblTitulo.setFont(EstilosUI.FUENTE_TITULO_BANNER.deriveFont(28f));
        lblTitulo.setForeground(EstilosUI.COLOR_INFO);
        textPanel.add(lblTitulo, gbcText);

        gbcText.gridy = 1;
        gbcText.insets = new Insets(2, 0, 0, 0);
        JLabel lblSubtitulo = new JLabel(I18nUI.Configuracion.SUBTITULO_APP());
        lblSubtitulo.setFont(EstilosUI.FUENTE_ESTANDAR.deriveFont(Font.ITALIC, 13f));
        lblSubtitulo.setForeground(EstilosUI.colorTextoSecundario(heroPanel.getBackground()));
        textPanel.add(lblSubtitulo, gbcText);

        gbcHero.gridx = 1;
        gbcHero.gridy = 0;
        gbcHero.gridheight = 2;
        gbcHero.insets = new Insets(0, 0, 0, 0);
        gbcHero.weightx = 1.0;
        heroPanel.add(textPanel, gbcHero);

        main.add(heroPanel, BorderLayout.NORTH);

        // --- CONTENT SECTION ---
        JPanel contentScroll = new JPanel(new BorderLayout());
        contentScroll.setOpaque(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        content.setOpaque(false);

        // Summary
        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_RESUMEN(VersionBurpIA.obtenerVersionActual()),
                I18nUI.Configuracion.DESCRIPCION_APP(),
                null));
        content.add(Box.createVerticalStrut(20));

        // Developer
        JPanel panelBotonWeb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panelBotonWeb.setOpaque(false);
        btnSitioWeb = new JButton(I18nUI.Configuracion.BOTON_SITIO_WEB());
        btnSitioWeb.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnSitioWeb.addActionListener(e -> {
            String url = I18nUI.Configuracion.URL_SITIO_WEB();
            UIUtils.abrirUrlConFallbackInfo(this, I18nUI.Configuracion.TITULO_ENLACE(), url,
                    I18nUI.Configuracion.MSG_URL(url));
        });
        panelBotonWeb.add(btnSitioWeb);

        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_DESARROLLADO_POR(),
                "",
                panelBotonWeb));
        content.add(Box.createVerticalStrut(20));

        // Updates
        JPanel panelBotonUpdate = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panelBotonUpdate.setOpaque(false);
        btnBuscarActualizaciones = new JButton(I18nUI.Configuracion.BOTON_BUSCAR_ACTUALIZACIONES());
        btnBuscarActualizaciones.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnBuscarActualizaciones.addActionListener(e -> verificarActualizaciones());
        panelBotonUpdate.add(btnBuscarActualizaciones);

        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                I18nUI.Configuracion.DESCRIPCION_ACTUALIZACIONES(),
                panelBotonUpdate));

        main.add(content, BorderLayout.CENTER);

        return main;
    }

    private JPanel crearSeccionAcerca(String titulo, String contenido, JComponent extra) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(EstilosUI.FUENTE_NEGRITA);
        lblTitulo.setForeground(EstilosUI.COLOR_INFO);
        p.add(lblTitulo, BorderLayout.NORTH);

        JPanel cPanel = new JPanel(new BorderLayout(0, 8));
        cPanel.setOpaque(false);
        cPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        if (Normalizador.noEsVacio(contenido)) {
            JTextArea txt = new JTextArea(contenido);
            txt.setEditable(false);
            txt.setLineWrap(true);
            txt.setWrapStyleWord(true);
            txt.setOpaque(false);
            txt.setFont(EstilosUI.FUENTE_ESTANDAR);
            txt.setForeground(EstilosUI.colorTextoPrimario(EstilosUI.obtenerFondoPanel()));
            cPanel.add(txt, BorderLayout.CENTER);
        }

        if (extra != null) {
            cPanel.add(extra, BorderLayout.SOUTH);
        }

        p.add(cPanel, BorderLayout.CENTER);
        return p;
    }

    private void actualizarContador() {
        int longitud = txtPrompt.getText().length();
        lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_CARACTERES(longitud));

        if (!txtPrompt.getText().contains("{REQUEST}")) {
            lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_FALTA_REQUEST(longitud));
            lblContadorPrompt.setForeground(EstilosUI.colorErrorAccesible(EstilosUI.obtenerFondoPanel()));
        } else {
            lblContadorPrompt.setForeground(EstilosUI.colorTextoPrimario(EstilosUI.obtenerFondoPanel()));
        }
    }

    private void restaurarPromptPorDefecto() {
        boolean confirmacion = UIUtils.confirmarAdvertencia(
                this,
                I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
                I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_PROMPT());

        if (confirmacion) {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
            actualizarContador();
        }
    }

    private void cargarConfiguracionActual() {
        comboIdioma.setSelectedItem(IdiomaUI.desdeCodigo(config.obtenerIdiomaUi()));
        rutasBinarioAgenteTemporal.clear();
        rutasBinarioAgenteTemporal.putAll(new HashMap<>(config.obtenerTodasLasRutasBinario()));

        String proveedorActual = config.obtenerProveedorAI();
        if (proveedorActual != null && ProveedorAI.existeProveedor(proveedorActual)) {
            comboProveedor.setSelectedItem(proveedorActual);
        }

        txtRetraso.setText(String.valueOf(config.obtenerRetrasoSegundos()));
        txtMaximoConcurrente.setText(String.valueOf(config.obtenerMaximoConcurrente()));
        txtMaximoHallazgosTabla.setText(String.valueOf(config.obtenerMaximoHallazgosTabla()));
        chkDetallado.setSelected(config.esDetallado());
        chkIgnorarSSL.setSelected(config.ignorarErroresSSL());
        chkSoloProxy.setSelected(config.soloProxy());
        chkAlertasHabilitadas.setSelected(config.alertasHabilitadas());
        chkPersistirBusqueda.setSelected(config.persistirFiltroBusquedaHallazgos());
        chkPersistirSeveridad.setSelected(config.persistirFiltroSeveridadHallazgos());

        String tipoAgente = config.obtenerTipoAgente();
        if (Normalizador.noEsVacio(tipoAgente)) {
            boolean existeOpcion = false;
            for (int i = 0; i < comboAgente.getItemCount(); i++) {
                if (comboAgente.getItemAt(i).equals(tipoAgente)) {
                    existeOpcion = true;
                    break;
                }
            }
            if (!existeOpcion) {
                comboAgente.addItem(tipoAgente);
            }
            comboAgente.setSelectedItem(tipoAgente);
        }

        chkAgenteHabilitado.setSelected(config.agenteHabilitado());
        txtAgentePromptInicial.setText(config.obtenerAgentePreflightPrompt());
        txtAgentePrompt.setText(config.obtenerAgentePrompt());

        alCambiarAgente();

        if (config.esPromptModificado()) {
            txtPrompt.setText(config.obtenerPromptConfigurable());
        } else {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
        }
        actualizarContador();

        comboFuenteEstandar.setSelectedItem(config.obtenerNombreFuenteEstandar());
        spinnerTamanioEstandar.setValue(config.obtenerTamanioFuenteEstandar());
        comboFuenteMono.setSelectedItem(config.obtenerNombreFuenteMono());
        spinnerTamanioMono.setValue(config.obtenerTamanioFuenteMono());

        proveedorActualUi = (String) comboProveedor.getSelectedItem();
        cargarEstadoProveedor(proveedorActualUi);
    }

    private void guardarConfiguracion() {
        if (guardandoConfiguracion)
            return;

        if (!txtPrompt.getText().contains("{REQUEST}")) {
            UIUtils.mostrarError(this, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    I18nUI.Configuracion.MSG_ERROR_PROMPT_SIN_REQUEST());
            return;
        }

        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (Normalizador.esVacio(proveedorSeleccionado)) {
            UIUtils.mostrarError(this, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR());
            return;
        }

        EstadoProveedorUI estadoUI = extraerEstadoActual();
        if (estadoUI == null)
            return;

        // Extraer todos los valores de la UI en el EDT
        final String promptActual = txtPrompt.getText();
        final IdiomaUI idiomaSeleccionado = (IdiomaUI) comboIdioma.getSelectedItem();
        final boolean detallado = chkDetallado.isSelected();
        final boolean ignorarSSL = chkIgnorarSSL.isSelected();
        final boolean soloProxy = chkSoloProxy.isSelected();
        final boolean alertasHabilitadas = chkAlertasHabilitadas.isSelected();
        final boolean persistirBusqueda = chkPersistirBusqueda.isSelected();
        final boolean persistirSeveridad = chkPersistirSeveridad.isSelected();
        final boolean agenteHabilitado = chkAgenteHabilitado.isSelected();
        final String tipoAgente = (String) comboAgente.getSelectedItem();
        final String agentePromptInicial = txtAgentePromptInicial.getText();
        final String agentePrompt = txtAgentePrompt.getText();
        final String retrasoTexto = txtRetraso.getText();
        final String maximoConcurrenteTexto = txtMaximoConcurrente.getText();
        final String maximoHallazgosTexto = txtMaximoHallazgosTabla.getText();

        final String nombreFuenteEstandar = (String) comboFuenteEstandar.getSelectedItem();
        final int tamanioFuenteEstandar = (int) spinnerTamanioEstandar.getValue();
        final String nombreFuenteMono = (String) comboFuenteMono.getSelectedItem();
        final int tamanioFuenteMono = (int) spinnerTamanioMono.getValue();

        final Map<String, String> rutasTemporalesCopy = new HashMap<>(rutasBinarioAgenteTemporal);
        final String promptPorDefecto = ConfiguracionAPI.obtenerPromptPorDefecto();

        // Validar binario del agente si está habilitado
        if (agenteHabilitado) {
            String rutaAgente = rutasTemporalesCopy.get(tipoAgente);
            if (!OSUtils.existeBinario(rutaAgente)) {
                AgenteTipo enumAgente = AgenteTipo.desdeCodigo(tipoAgente, AgenteTipo.porDefecto());
                String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(enumAgente.getNombreVisible(),
                        rutaAgente);
                String urlDoc = enumAgente.getUrlDocPorIdioma(config.obtenerIdiomaUi());

                UIUtils.mostrarErrorBinarioAgenteNoEncontrado(
                        this,
                        I18nUI.Configuracion.Agentes.TITULO_VALIDACION_AGENTE(),
                        mensaje,
                        I18nUI.Configuracion.Agentes.ENLACE_INSTALAR_AGENTE(enumAgente.getNombreVisible()),
                        urlDoc);
                return;
            }
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText(I18nUI.Configuracion.BOTON_GUARDANDO());
        guardandoConfiguracion = true;

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private final StringBuilder errorMsg = new StringBuilder();
            private ConfiguracionAPI snapshot;

            @Override
            protected Boolean doInBackground() {
                try {
                    snapshot = config.crearSnapshot();

                    snapshot.establecerIdiomaUi(
                            idiomaSeleccionado != null ? idiomaSeleccionado.codigo() : IdiomaUI.porDefecto().codigo());
                    snapshot.establecerProveedorAI(proveedorSeleccionado);

                    snapshot.establecerApiKeyParaProveedor(proveedorSeleccionado, estadoUI.getApiKey());
                    snapshot.establecerModeloParaProveedor(proveedorSeleccionado, estadoUI.getModelo());
                    snapshot.establecerUrlBaseParaProveedor(proveedorSeleccionado, estadoUI.getBaseUrl());
                    snapshot.establecerMaxTokensParaProveedor(proveedorSeleccionado, estadoUI.getMaxTokens());
                    snapshot.establecerTiempoEsperaParaModelo(proveedorSeleccionado, estadoUI.getModelo(),
                            estadoUI.getTimeout());

                    snapshot.establecerDetallado(detallado);
                    snapshot.establecerIgnorarErroresSSL(ignorarSSL);
                    snapshot.establecerSoloProxy(soloProxy);
                    snapshot.establecerAlertasHabilitadas(alertasHabilitadas);
                    snapshot.establecerPersistirFiltroBusquedaHallazgos(persistirBusqueda);
                    snapshot.establecerPersistirFiltroSeveridadHallazgos(persistirSeveridad);
                    snapshot.establecerAgenteHabilitado(agenteHabilitado);
                    snapshot.establecerTipoAgente(tipoAgente);
                    snapshot.establecerAgentePreflightPrompt(agentePromptInicial);
                    snapshot.establecerAgentePrompt(agentePrompt);

                    for (Map.Entry<String, String> entry : rutasTemporalesCopy.entrySet()) {
                        snapshot.establecerRutaBinarioAgente(entry.getKey(), entry.getValue());
                    }

                    snapshot.establecerPromptConfigurable(promptActual);
                    snapshot.establecerPromptModificado(!promptActual.equals(promptPorDefecto));

                    snapshot.establecerRetrasoSegundos(parsearEntero(retrasoTexto));
                    snapshot.establecerMaximoConcurrente(parsearEntero(maximoConcurrenteTexto));
                    snapshot.establecerMaximoHallazgosTabla(parsearEntero(maximoHallazgosTexto));

                    snapshot.establecerNombreFuenteEstandar(nombreFuenteEstandar);
                    snapshot.establecerTamanioFuenteEstandar(tamanioFuenteEstandar);
                    snapshot.establecerNombreFuenteMono(nombreFuenteMono);
                    snapshot.establecerTamanioFuenteMono(tamanioFuenteMono);

                    java.util.Map<String, String> errores = snapshot.validar();
                    if (!errores.isEmpty()) {
                        errorMsg.append(construirMensajeErroresValidacion(errores));
                        return false;
                    }

                    return gestorConfig.guardarConfiguracion(snapshot, errorMsg);
                } catch (Exception e) {
                    errorMsg.append(e.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        config.aplicarDesde(snapshot);
                        EstilosUI.actualizarFuentes(config);
                        if (alGuardar != null)
                            alGuardar.run();
                        dispose();
                    } else {
                        UIUtils.mostrarError(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_ERROR_GUARDAR(),
                                errorMsg.toString());
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_ERROR_GUARDAR(),
                            e.getMessage());
                } finally {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText(I18nUI.Configuracion.BOTON_GUARDAR());
                    guardandoConfiguracion = false;
                }
            }
        };
        worker.execute();
    }

    private EstadoProveedorUI extraerEstadoActual() {
        String modelo = obtenerModeloSeleccionado();
        if (Normalizador.esVacio(modelo)) {
            UIUtils.mostrarError(this, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    I18nUI.Configuracion.MSG_SELECCIONA_MODELO());
            return null;
        }

        Integer timeout = parsearEntero(txtTimeoutModelo.getText());
        if (timeout == null) {
            UIUtils.mostrarError(this, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    I18nUI.Configuracion.MSG_ERROR_FORMATO_NUMERO());
            return null;
        }

        Integer maxTokens = parsearEntero(txtMaxTokens.getText());
        if (maxTokens == null)
            maxTokens = 4096;

        return new EstadoProveedorUI(
                new String(txtClave.getPassword()),
                modelo,
                txtUrl.getText().trim(),
                maxTokens,
                timeout);
    }

    private String construirMensajeErroresValidacion(java.util.Map<String, String> errores) {
        StringBuilder mensaje = new StringBuilder(I18nUI.Configuracion.MSG_CORRIGE_CAMPOS());
        errores.values().forEach(valor -> mensaje.append(" - ").append(valor).append("\n"));
        return mensaje.toString().trim();
    }

    private Integer parsearEntero(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolverRutaBinarioAgente(String agenteSeleccionado) {
        if (agenteSeleccionado == null) {
            return "";
        }
        String rutaTemporal = rutasBinarioAgenteTemporal.get(agenteSeleccionado);
        if (Normalizador.noEsVacio(rutaTemporal)) {
            return rutaTemporal;
        }
        String rutaGuardada = config.obtenerRutaBinarioAgente(agenteSeleccionado);
        return rutaGuardada != null ? rutaGuardada : "";
    }

    private void alCambiarAgente() {
        String agenteSeleccionado = (String) comboAgente.getSelectedItem();
        if (agenteSeleccionado == null)
            return;

        actualizandoRutaFlag = true;
        try {
            AgenteTipo enumAgente = AgenteTipo.desdeCodigo(agenteSeleccionado, null);
            if (enumAgente != null) {
                chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(enumAgente.getNombreVisible()));

                String rutaSeleccionada = resolverRutaBinarioAgente(agenteSeleccionado);
                if (Normalizador.noEsVacio(rutaSeleccionada)) {
                    txtAgenteBinario.setText(rutaSeleccionada);
                } else {
                    txtAgenteBinario.setText(enumAgente.getRutaPorDefecto());
                }

                // Asegurar que la ruta actual (defecto o cargada) esté en el mapa temporal
                rutasBinarioAgenteTemporal.put(agenteSeleccionado, txtAgenteBinario.getText().trim());
            } else {
                chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(agenteSeleccionado));
                txtAgenteBinario.setText("");
            }
        } finally {
            actualizandoRutaFlag = false;
        }
    }

    private void actualizarRutaEnMemoria() {
        if (actualizandoRutaFlag)
            return;
        String agenteSeleccionado = (String) comboAgente.getSelectedItem();
        if (agenteSeleccionado != null) {
            rutasBinarioAgenteTemporal.put(agenteSeleccionado, txtAgenteBinario.getText().trim());
        }
    }

    private void alCambiarProveedor() {
        if (actualizandoProveedorUi)
            return;
        actualizandoProveedorUi = true;

        try {
            if (proveedorActualUi != null) {
                estadoProveedorTemporal.put(proveedorActualUi, extraerEstadoActualRapido());
            }

            String nuevoProveedor = (String) comboProveedor.getSelectedItem();
            if (nuevoProveedor == null)
                return;

            proveedorActualUi = nuevoProveedor;
            cargarEstadoProveedor(nuevoProveedor);
        } finally {
            actualizandoProveedorUi = false;
        }
    }

    private void cargarEstadoProveedor(String proveedor) {
        if (proveedor == null)
            return;

        EstadoProveedorUI borrador = estadoProveedorTemporal.get(proveedor);
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (configProveedor == null)
            return;

        if (borrador != null) {
            txtUrl.setText(borrador.getBaseUrl());
            txtClave.setText(borrador.getApiKey());
            txtMaxTokens.setText(String.valueOf(borrador.getMaxTokens()));
            cargarModelosEnCombo(configProveedor.obtenerModelosDisponibles(), borrador.getModelo());
            txtTimeoutModelo.setText(String.valueOf(borrador.getTimeout()));
        } else {
            String urlGuardada = config.obtenerUrlBaseGuardadaParaProveedor(proveedor);
            boolean tieneUrlGuardada = Normalizador.noEsVacio(urlGuardada);
            if (!tieneUrlGuardada && ProveedorAI.PROVEEDOR_CUSTOM.equals(proveedor)) {
                IdiomaUI idioma = (IdiomaUI) comboIdioma.getSelectedItem();
                String codigo = idioma != null ? idioma.codigo() : config.obtenerIdiomaUi();
                txtUrl.setText(ProveedorAI.obtenerUrlApiPorDefecto(proveedor, codigo));
            } else {
                String urlBase = tieneUrlGuardada ? urlGuardada : config.obtenerUrlBaseParaProveedor(proveedor);
                txtUrl.setText(urlBase != null ? urlBase : configProveedor.obtenerUrlApi());
            }

            String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
            txtClave.setText(apiKey != null ? apiKey : "");

            Integer maxTokens = config.obtenerMaxTokensConfiguradoParaProveedor(proveedor);
            txtMaxTokens.setText(
                    String.valueOf(maxTokens != null ? maxTokens : configProveedor.obtenerMaxTokensPorDefecto()));

            String modelo = config.obtenerModeloParaProveedor(proveedor);
            if (Normalizador.esVacio(modelo))
                modelo = configProveedor.obtenerModeloPorDefecto();
            cargarModelosEnCombo(configProveedor.obtenerModelosDisponibles(), modelo);

            actualizarTimeoutModeloSeleccionado();
        }
    }

    private EstadoProveedorUI extraerEstadoActualRapido() {
        Integer timeout = parsearEntero(txtTimeoutModelo.getText());
        Integer maxTokens = parsearEntero(txtMaxTokens.getText());
        return new EstadoProveedorUI(
                new String(txtClave.getPassword()),
                obtenerModeloSeleccionado(),
                txtUrl.getText().trim(),
                maxTokens != null ? maxTokens : 4096,
                timeout != null ? timeout : 120);
    }

    private String obtenerModeloSeleccionado() {
        String modeloActual = (String) comboModelo.getSelectedItem();
        if (modeloActual == null)
            return "";
        if (!esOpcionModeloCustom(modeloActual))
            return normalizarModeloSeleccionado(modeloActual);
        Object editorValue = comboModelo.getEditor().getItem();
        return editorValue != null ? normalizarModeloSeleccionado(editorValue.toString()) : "";
    }

    private void refrescarModelosDesdeAPI() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null)
            return;

        final long seq = ++secuenciaRefrescoModelos;
        btnRefrescarModelos.setEnabled(false);
        btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_ACTUALIZANDO_MODELOS());

        SwingWorker<java.util.List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<String> doInBackground() {
                return obtenerModelosDesdeAPI(proveedorSeleccionado);
            }

            @Override
            protected void done() {
                if (seq != secuenciaRefrescoModelos)
                    return;

                try {
                    java.util.List<String> modelos = get();
                    if (modelos == null || modelos.isEmpty()) {
                        throw new IllegalStateException(
                                I18nUI.Configuracion.ERROR_API_SIN_MODELOS(proveedorSeleccionado));
                    }
                    cargarModelosEnCombo(modelos, modelos.get(0));
                    UIUtils.mostrarInfo(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_MODELOS_ACTUALIZADOS(),
                            I18nUI.Configuracion.MSG_MODELOS_ACTUALIZADOS(modelos.size(), proveedorSeleccionado));
                } catch (Exception e) {
                    UIUtils.mostrarError(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_ERROR(),
                            I18nUI.Configuracion.MSG_ERROR_PROCESAR_MODELOS(extraerMensajeError(e)));
                } finally {
                    btnRefrescarModelos.setEnabled(true);
                    btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
                }
            }
        };
        worker.execute();
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
        comboModelo.removeAllItems();
        comboModelo.addItem(obtenerEtiquetaModeloCustom());

        List<String> modelosNormalizados = normalizarModelos(modelos);
        for (String modelo : modelosNormalizados) {
            comboModelo.addItem(modelo);
        }

        String preferidoNormalizado = normalizarModeloSeleccionado(preferido);
        if (preferidoNormalizado.isEmpty() && !modelosNormalizados.isEmpty()) {
            preferidoNormalizado = modelosNormalizados.get(0);
        }

        for (int i = 0; i < comboModelo.getItemCount(); i++) {
            if (preferidoNormalizado.equals(comboModelo.getItemAt(i))) {
                comboModelo.setSelectedIndex(i);
                return;
            }
        }

        comboModelo.setSelectedItem(obtenerEtiquetaModeloCustom());
        comboModelo.getEditor().setItem(preferidoNormalizado);
        actualizarTimeoutModeloSeleccionado();
    }

    private String obtenerEtiquetaModeloCustom() {
        return I18nUI.Configuracion.OPCION_MODELO_CUSTOM();
    }

    private boolean esOpcionModeloCustom(String valor) {
        return I18nUI.Configuracion.OPCION_MODELO_CUSTOM().equals(valor)
                || "-- Custom --".equals(valor)
                || "-- Personalizado --".equals(valor);
    }

    private void actualizarTimeoutModeloSeleccionado() {
        if (txtTimeoutModelo == null) {
            return;
        }
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (Normalizador.esVacio(proveedorSeleccionado)) {
            txtTimeoutModelo.setText(String.valueOf(config.obtenerTiempoEsperaAI()));
            return;
        }

        String modelo = obtenerModeloSeleccionado();
        if (Normalizador.esVacio(modelo)) {
            ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorSeleccionado);
            if (configProveedor != null) {
                modelo = configProveedor.obtenerModeloPorDefecto();
            }
        }

        int timeout = config.obtenerTiempoEsperaParaModelo(proveedorSeleccionado, modelo);
        txtTimeoutModelo.setText(String.valueOf(timeout));
    }

    private List<String> normalizarModelos(List<String> modelos) {
        Set<String> unicos = new LinkedHashSet<>();
        if (modelos != null) {
            for (String modelo : modelos) {
                String limpio = normalizarModeloSeleccionado(modelo);
                if (!limpio.isEmpty()) {
                    unicos.add(limpio);
                }
            }
        }
        return new ArrayList<>(unicos);
    }

    private String normalizarModeloSeleccionado(String modelo) {
        if (modelo == null) {
            return "";
        }
        String limpio = modelo.trim();
        return ":".equals(limpio) ? "" : limpio;
    }

    private java.util.List<String> obtenerModelosDesdeAPI(String proveedor) {
        okhttp3.OkHttpClient cliente = crearClienteModelos();
        if ("Gemini".equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosGemini(
                        txtUrl.getText().trim(),
                        new String(txtClave.getPassword()).trim(),
                        cliente);
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_GEMINI() + e.getMessage(), e);
            }
        }
        if ("Ollama".equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOllama(
                        txtUrl.getText().trim(),
                        cliente);
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_OLLAMA() + e.getMessage(), e);
            }
        }
        if ("OpenAI".equals(proveedor) || "Z.ai".equals(proveedor) || "minimax".equals(proveedor)
                || ProveedorAI.PROVEEDOR_CUSTOM.equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOpenAI(
                        txtUrl.getText().trim(),
                        new String(txtClave.getPassword()).trim(),
                        cliente);
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_API() + e.getMessage(), e);
            }
        }
        List<String> modelos = ProveedorAI.obtenerModelosDisponibles(proveedor);
        if (modelos == null || modelos.isEmpty()) {
            throw new RuntimeException(I18nUI.Configuracion.ERROR_PROVEEDOR_SIN_LISTA());
        }
        return modelos;
    }

    private okhttp3.OkHttpClient crearClienteModelos() {
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_CONEXION_MODELOS_SEG, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_LECTURA_MODELOS_SEG, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private String extraerMensajeError(Exception error) {
        Throwable causa = error;
        if (error instanceof java.util.concurrent.ExecutionException && error.getCause() != null) {
            causa = error.getCause();
        }
        String mensaje = causa != null ? causa.getMessage() : null;
        if (Normalizador.esVacio(mensaje)) {
            return I18nUI.Configuracion.SIN_DETALLE_ERROR();
        }
        return mensaje.trim();
    }

    private void verificarActualizaciones() {
        if (btnBuscarActualizaciones == null) {
            return;
        }

        btnBuscarActualizaciones.setEnabled(false);
        btnBuscarActualizaciones.setText(I18nUI.Configuracion.BOTON_BUSCANDO_ACTUALIZACIONES());

        SwingWorker<ResultadoActualizacion, Void> worker = new SwingWorker<>() {
            @Override
            protected ResultadoActualizacion doInBackground() throws Exception {
                String versionRemota = obtenerVersionRemota();
                String versionActual = VersionBurpIA.obtenerVersionActual();
                boolean hayActualizacion = VersionBurpIA.sonVersionesDiferentes(versionActual, versionRemota);
                return new ResultadoActualizacion(versionActual, versionRemota, hayActualizacion);
            }

            @Override
            protected void done() {
                try {
                    ResultadoActualizacion resultado = get();
                    if (resultado.hayActualizacion) {
                        UIUtils.mostrarInfo(
                                DialogoConfiguracion.this,
                                I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                                I18nUI.Configuracion.MSG_ACTUALIZACION_DISPONIBLE(
                                        resultado.versionActual,
                                        resultado.versionRemota,
                                        VersionBurpIA.URL_DESCARGA));
                    } else {
                        UIUtils.mostrarInfo(
                                DialogoConfiguracion.this,
                                I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                                I18nUI.Configuracion.MSG_VERSION_AL_DIA(resultado.versionActual));
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(
                            DialogoConfiguracion.this,
                            I18nUI.Configuracion.TITULO_ERROR(),
                            I18nUI.Configuracion.MSG_ERROR_VERIFICAR_ACTUALIZACIONES(extraerMensajeError(e)));
                } finally {
                    btnBuscarActualizaciones.setEnabled(true);
                    btnBuscarActualizaciones.setText(I18nUI.Configuracion.BOTON_BUSCAR_ACTUALIZACIONES());
                }
            }
        };

        worker.execute();
    }

    private String obtenerVersionRemota() throws Exception {
        okhttp3.OkHttpClient cliente = crearClienteActualizaciones();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(VersionBurpIA.URL_VERSION_REMOTA)
                .get()
                .build();

        try (okhttp3.Response response = cliente.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException(I18nUI.Configuracion.MSG_ERROR_HTTP_VERSION_REMOTA(response.code()));
            }
            okhttp3.ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException(I18nUI.Configuracion.MSG_VERSION_REMOTA_VACIA());
            }
            String versionRemota = VersionBurpIA.normalizarVersion(body.string());
            if (versionRemota.isEmpty()) {
                throw new IllegalStateException(I18nUI.Configuracion.MSG_VERSION_REMOTA_VACIA());
            }
            return versionRemota;
        }
    }

    private okhttp3.OkHttpClient crearClienteActualizaciones() {
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_CONEXION_ACTUALIZACIONES_SEG, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_LECTURA_ACTUALIZACIONES_SEG, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private static final class ResultadoActualizacion {
        /*
         * - **Horizontal Hero Layout**: The logo is now smaller (80x80) and positioned
         * to the left of the title for a more compact and modern look.
         * - **Scroll-Free Experience**: Optimized vertical spacing (reduced from 20px
         * to 10-15px gaps) to ensure all content, including "Updates", fits without
         * scrollbars.
         * - **Preserved Functionality**: The "Search Updates" button is kept and fully
         * visible.
         * - **Dynamic Version Header**: The summary section title includes the version
         * `BurpIA (VERSION)`.
         */
        private final String versionActual;
        private final String versionRemota;
        private final boolean hayActualizacion;

        private ResultadoActualizacion(String versionActual, String versionRemota, boolean hayActualizacion) {
            this.versionActual = versionActual;
            this.versionRemota = versionRemota;
            this.hayActualizacion = hayActualizacion;
        }
    }

    private void probarConexion() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) {
            UIUtils.mostrarAdvertencia(this, I18nUI.Configuracion.TITULO_VALIDACION(),
                    I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR());
            return;
        }

        EstadoProveedorUI estadoUI = extraerEstadoActual();
        if (estadoUI == null)
            return;

        String urlApi = estadoUI.getBaseUrl();
        if (urlApi.isEmpty()) {
            UIUtils.mostrarAdvertencia(this, I18nUI.Configuracion.TITULO_VALIDACION(),
                    I18nUI.Configuracion.MSG_URL_VACIA());
            txtUrl.requestFocus();
            return;
        }

        String claveApi = estadoUI.getApiKey().trim();
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorSeleccionado);
        if (configProveedor != null && configProveedor.requiereClaveApi()) {
            if (claveApi.isEmpty()) {
                UIUtils.mostrarAdvertencia(this, I18nUI.Configuracion.TITULO_VALIDACION(),
                        I18nUI.Configuracion.MSG_API_KEY_VACIA(proveedorSeleccionado));
                txtClave.requestFocus();
                return;
            }
        }

        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBANDO());

        final String modeloFinal = estadoUI.getModelo();
        final int timeoutModeloFinal = estadoUI.getTimeout();
        SwingWorker<ProbadorConexionAI.ResultadoPrueba, Void> worker = new SwingWorker<>() {
            @Override
            protected ProbadorConexionAI.ResultadoPrueba doInBackground() {
                try {
                    ConfiguracionAPI configTemp = new ConfiguracionAPI();
                    configTemp.establecerProveedorAI(proveedorSeleccionado);
                    configTemp.establecerClaveApi(claveApi);
                    configTemp.establecerModelo(modeloFinal);
                    configTemp.establecerUrlApi(
                            ConfiguracionAPI.construirUrlApiProveedor(
                                    proveedorSeleccionado,
                                    urlApi,
                                    modeloFinal));
                    configTemp.establecerTiempoEsperaParaModelo(proveedorSeleccionado, modeloFinal, timeoutModeloFinal);

                    ProbadorConexionAI probador = new ProbadorConexionAI(configTemp);
                    return probador.probarConexion();
                } catch (Exception e) {
                    return new ProbadorConexionAI.ResultadoPrueba(
                            false,
                            I18nUI.Conexion.ERROR_PRUEBA_CONEXION(e.getMessage()),
                            null);
                }
            }

            @Override
            protected void done() {
                try {
                    ProbadorConexionAI.ResultadoPrueba resultado = get();
                    if (resultado.exito) {
                        UIUtils.mostrarInfo(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_CONEXION_EXITOSA(),
                                resultado.mensaje);
                    } else {
                        UIUtils.mostrarError(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_ERROR_CONEXION(),
                                resultado.mensaje);
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(DialogoConfiguracion.this, I18nUI.Configuracion.TITULO_ERROR(),
                            I18nUI.Configuracion.MSG_ERROR_PRUEBA_INESPERADO(e.getMessage()));
                } finally {
                    btnProbarConexion.setEnabled(true);
                    btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
                }
            }
        };

        worker.execute();
    }

    private void alCambiarIdiomaUi() {
        IdiomaUI idioma = (IdiomaUI) comboIdioma.getSelectedItem();
        if (idioma != null) {
            config.establecerIdiomaUi(idioma.codigo());
        }
    }

    private void aplicarTooltips() {
        // Tab Proveedor
        if (comboProveedor != null)
            comboProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.PROVEEDOR());
        if (txtUrl != null)
            txtUrl.setToolTipText(I18nUI.Tooltips.Configuracion.URL_API());
        if (txtClave != null)
            txtClave.setToolTipText(I18nUI.Tooltips.Configuracion.CLAVE_API());
        if (comboModelo != null)
            comboModelo.setToolTipText(I18nUI.Tooltips.Configuracion.MODELO());
        if (btnRefrescarModelos != null)
            btnRefrescarModelos.setToolTipText(I18nUI.Tooltips.Configuracion.CARGAR_MODELOS());
        if (txtMaxTokens != null)
            txtMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());
        if (txtTimeoutModelo != null)
            txtTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());
        if (txtPrompt != null)
            txtPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_EDITOR());
        if (btnRestaurarPrompt != null)
            btnRestaurarPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT());
        if (lblContadorPrompt != null)
            lblContadorPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.CONTADOR_PROMPT());

        // Tab Ajustes Usuario
        if (txtRetraso != null)
            txtRetraso.setToolTipText(I18nUI.Tooltips.Configuracion.RETRASO());
        if (txtMaximoConcurrente != null)
            txtMaximoConcurrente.setToolTipText(I18nUI.Tooltips.Configuracion.MAXIMO_CONCURRENTE());
        if (txtMaximoHallazgosTabla != null)
            txtMaximoHallazgosTabla.setToolTipText(I18nUI.Tooltips.Configuracion.MAXIMO_HALLAZGOS());
        if (chkDetallado != null)
            chkDetallado.setToolTipText(I18nUI.Tooltips.Configuracion.DETALLADO());
        if (chkIgnorarSSL != null)
            chkIgnorarSSL.setToolTipText(I18nUI.Tooltips.Configuracion.IGNORAR_SSL());
        if (chkSoloProxy != null)
            chkSoloProxy.setToolTipText(I18nUI.Tooltips.Configuracion.SOLO_PROXY());
        if (chkAlertasHabilitadas != null)
            chkAlertasHabilitadas.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_ALERTAS());
        if (comboIdioma != null)
            comboIdioma.setToolTipText(I18nUI.Tooltips.Configuracion.IDIOMA());

        // Fuentes
        if (comboFuenteEstandar != null)
            comboFuenteEstandar.setToolTipText(I18nUI.Tooltips.Configuracion.FUENTE_ESTANDAR());
        if (spinnerTamanioEstandar != null)
            spinnerTamanioEstandar.setToolTipText(I18nUI.Tooltips.Configuracion.TAMANIO_ESTANDAR());
        if (comboFuenteMono != null)
            comboFuenteMono.setToolTipText(I18nUI.Tooltips.Configuracion.FUENTE_MONO());
        if (spinnerTamanioMono != null)
            spinnerTamanioMono.setToolTipText(I18nUI.Tooltips.Configuracion.TAMANIO_MONO());

        // Tab Agentes
        if (comboAgente != null)
            comboAgente.setToolTipText(I18nUI.Tooltips.Configuracion.SELECCIONAR_AGENTE());
        if (chkAgenteHabilitado != null)
            chkAgenteHabilitado.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_AGENTE());
        if (txtAgenteBinario != null)
            txtAgenteBinario.setToolTipText(I18nUI.Tooltips.Configuracion.BINARIO_AGENTE());
        if (txtAgentePromptInicial != null)
            txtAgentePromptInicial.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_INICIAL_AGENTE());
        if (txtAgentePrompt != null)
            txtAgentePrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_AGENTE());
        if (btnRestaurarPromptAgenteInicial != null)
            btnRestaurarPromptAgenteInicial
                    .setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT_INICIAL_AGENTE());
        if (btnRestaurarPromptAgente != null)
            btnRestaurarPromptAgente.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT_AGENTE());

        // Tab Acerca De
        if (btnBuscarActualizaciones != null)
            btnBuscarActualizaciones.setToolTipText(I18nUI.Tooltips.Configuracion.CHECK_ACTUALIZACIONES());
        if (btnSitioWeb != null)
            btnSitioWeb.setToolTipText(I18nUI.Tooltips.Configuracion.SITIO_AUTOR());

        // Botones Principales
        if (btnGuardar != null)
            btnGuardar.setToolTipText(I18nUI.Tooltips.Configuracion.GUARDAR());
        if (btnProbarConexion != null)
            btnProbarConexion.setToolTipText(I18nUI.Tooltips.Configuracion.PROBAR_CONEXION());
        if (btnRestaurarFuentes != null)
            btnRestaurarFuentes.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_FUENTES());
    }
}
