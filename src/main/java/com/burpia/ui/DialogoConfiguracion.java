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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DialogoConfiguracion extends JDialog {
    private static final int ANCHO_DIALOGO = 800;
    private static final int ALTO_DIALOGO = 720;
    private static final int FILAS_PROMPT_AGENTE = 6;
    private static final int ANCHO_SCROLL_PROMPT_AGENTE = 300;
    private static final int ALTO_SCROLL_PROMPT_AGENTE = 180;

    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final Runnable alGuardar;

    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtRetraso;
    private JTextField txtMaximoConcurrente;
    private JTextField txtMaximoHallazgosTabla;
    private JTextField txtMaximoTareas;
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

    // Multi-Proveedor Components
    private JCheckBox chkHabilitarMultiProveedor;
    private JList<String> listaProveedoresDisponibles;
    private JList<String> listaProveedoresSeleccionados;
    private DefaultListModel<String> modeloListaDisponibles;
    private DefaultListModel<String> modeloListaSeleccionados;
    private JButton btnAgregarProveedor;
    private JButton btnQuitarProveedor;
    private JButton btnSubirProveedor;
    private JButton btnBajarProveedor;
    private JLabel lblEstadoMultiProveedor;
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
    private EstadoEdicionDialogo estadoInicialDialogo;
    private String proveedorActualUi;
    private boolean actualizandoProveedorUi = false;
    private boolean actualizandoListaMultiProveedor = false;
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
        btnProbarConexion = new JButton(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
        btnProbarConexion.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnProbarConexion.addActionListener(e -> probarConexion());

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

        btnCerrar = new JButton(I18nUI.DetalleHallazgo.BOTON_CANCELAR());
        btnCerrar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnCerrar.addActionListener(e -> intentarCerrarDialogo());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCerrar);

        add(tabbedPane, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                intentarCerrarDialogo();
            }
        });

        pack();
        setSize(ANCHO_DIALOGO, ALTO_DIALOGO);
        setLocationRelativeTo(getParent());
    }

    private void inicializarCamposConfiguracion() {
        txtRetraso = new JTextField(10);
        txtRetraso.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtRetraso.setToolTipText(I18nUI.Tooltips.Configuracion.RETRASO());

        txtMaximoConcurrente = new JTextField(10);
        txtMaximoConcurrente.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoConcurrente.setToolTipText(I18nUI.Tooltips.Configuracion.MAXIMO_CONCURRENTE());

        txtUrl = new JTextField(30);
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtUrl.setToolTipText(I18nUI.Tooltips.Configuracion.URL_API());

        txtClave = new JPasswordField(30);
        txtClave.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtClave.setToolTipText(I18nUI.Tooltips.Configuracion.CLAVE_API());

        txtMaxTokens = new JTextField(10);
        txtMaxTokens.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());

        txtTimeoutModelo = new JTextField(10);
        txtTimeoutModelo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());

        txtMaximoHallazgosTabla = new JTextField(10);
        txtMaximoHallazgosTabla.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoHallazgosTabla.setToolTipText(
                I18nUI.Tooltips.Configuracion.MAXIMO_HALLAZGOS() + " (" +
                        ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA + "-" +
                        ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA + ")");

        txtMaximoTareas = new JTextField(10);
        txtMaximoTareas.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoTareas.setToolTipText(
                I18nUI.Tooltips.Configuracion.MAXIMO_TAREAS() + " (" +
                        ConfiguracionAPI.MINIMO_TAREAS_TABLA + "-" +
                        ConfiguracionAPI.MAXIMO_TAREAS_TABLA + ")");

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

        comboProveedor = new JComboBox<>(ProveedorAI.obtenerNombresProveedores().toArray(new String[0]));
        comboProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.PROVEEDOR());
        comboProveedor.addActionListener(e -> alCambiarProveedor());

        comboModelo = new JComboBox<>();
        comboModelo.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboModelo.setEditable(true);
        comboModelo.setToolTipText(I18nUI.Tooltips.Configuracion.MODELO());
        comboModelo.addActionListener(e -> actualizarTimeoutModeloSeleccionado());

        btnRefrescarModelos = new JButton(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
        btnRefrescarModelos.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRefrescarModelos.setToolTipText(I18nUI.Tooltips.Configuracion.CARGAR_MODELOS());
        btnRefrescarModelos.addActionListener(e -> refrescarModelosDesdeAPI());

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

    private JPanel crearPanelProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PROVEEDOR(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;

        // 1. Proveedor AI - Define el contexto
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDOR_AI()), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        panel.add(comboProveedor, gbc);

        fila++;

        // 2. Clave de API - Credencial principal (requerida)
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_CLAVE_API()), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        panel.add(txtClave, gbc);

        fila++;

        // 3. Modelo - Configuración específica del proveedor
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MODELO()), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        JPanel panelModelo = new JPanel(new BorderLayout(5, 0));
        panelModelo.setOpaque(false);
        panelModelo.add(comboModelo, BorderLayout.CENTER);
        JPanel panelAccionesModelo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panelAccionesModelo.setOpaque(false);
        panelAccionesModelo.add(btnRefrescarModelos);
        panelAccionesModelo.add(btnProbarConexion);
        panelModelo.add(panelAccionesModelo, BorderLayout.EAST);
        panel.add(panelModelo, gbc);

        fila++;

        // 4. URL de API - Configuración avanzada (opcional/custom)
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_URL_API()), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        panel.add(txtUrl, gbc);

        fila++;

        // 5. FILA COMBINADA: Timeout + Retraso - Configuración de rendimiento
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_TIMEOUT_MODELO()), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        panel.add(txtTimeoutModelo, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(8, 15, 8, 8);
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_RETRASO()), gbc);

        gbc.gridx = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 8, 8);
        panel.add(txtRetraso, gbc);

        fila++;

        // 6. FILA COMBINADA: Máximo Tokens + Máximo Concurrente - Configuración de rendimiento
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel lblMaxTokens = new JLabel(I18nUI.Configuracion.LABEL_MAX_TOKENS());
        lblMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());
        panel.add(lblMaxTokens, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        JPanel panelMaxTokens = new JPanel(new BorderLayout(5, 0));
        panelMaxTokens.setOpaque(false);
        panelMaxTokens.add(txtMaxTokens, BorderLayout.CENTER);
        JLabel lblInfoTokens = new JLabel("ℹ️");
        lblInfoTokens.setToolTipText(I18nUI.Tooltips.Configuracion.INFO_TOKENS());
        panelMaxTokens.add(lblInfoTokens, BorderLayout.EAST);
        panel.add(panelMaxTokens, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(8, 15, 8, 8);
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MAXIMO_CONCURRENTE()), gbc);

        gbc.gridx = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 8, 8);
        panel.add(txtMaximoConcurrente, gbc);

        return panel;
    }

    private JPanel crearTabAjustesUsuario() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        contenido.setOpaque(false);

        // EFICIENCIA: PANEL 1 - PREFERENCIAS DE USUARIO (lo que el usuario VE)
        JPanel panelPreferencias = new JPanel(new GridBagLayout());
        panelPreferencias.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPreferencias.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PREFERENCIAS_USUARIO(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Idioma - y=0
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panelPreferencias.add(new JLabel(I18nUI.Configuracion.LABEL_IDIOMA()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelPreferencias.add(comboIdioma, gbc);

        // Opciones de preferencias - y=1 (3 checkbox en misma línea armónica)
        JPanel panelOpcionesPreferencias = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelOpcionesPreferencias.setOpaque(false);
        panelOpcionesPreferencias.add(chkDetallado);
        panelOpcionesPreferencias.add(Box.createHorizontalStrut(25));
        panelOpcionesPreferencias.add(chkPersistirBusqueda);
        panelOpcionesPreferencias.add(Box.createHorizontalStrut(25));
        panelOpcionesPreferencias.add(chkPersistirSeveridad);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panelPreferencias.add(panelOpcionesPreferencias, gbc);

        panelPreferencias.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelPreferencias.getPreferredSize().height));
        contenido.add(panelPreferencias);
        contenido.add(Box.createVerticalStrut(15));

        // CONFIABILIDAD: PANEL 2 - LÍMITES Y SEGURIDAD (configuración técnica)
        JPanel panelLimitesSeguridad = new JPanel(new GridBagLayout());
        panelLimitesSeguridad.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelLimitesSeguridad.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_LIMITES_SEGURIDAD(), 12, 16));

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Máximo hallazgos en tabla - y=0
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panelLimitesSeguridad.add(new JLabel(I18nUI.Configuracion.LABEL_MAX_HALLAZGOS_TABLA()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelLimitesSeguridad.add(txtMaximoHallazgosTabla, gbc);

        // Máximo tareas - y=1
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panelLimitesSeguridad.add(new JLabel(I18nUI.Configuracion.LABEL_MAXIMO_TAREAS()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelLimitesSeguridad.add(txtMaximoTareas, gbc);

        // Alertas - y=2
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panelLimitesSeguridad.add(new JLabel(I18nUI.Configuracion.LABEL_ALERTAS()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelLimitesSeguridad.add(chkAlertasHabilitadas, gbc);

        // Seguridad SSL - y=3
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panelLimitesSeguridad.add(new JLabel(I18nUI.Configuracion.LABEL_SEGURIDAD_SSL()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelLimitesSeguridad.add(chkIgnorarSSL, gbc);

        // Filtro herramientas - y=4
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        panelLimitesSeguridad.add(new JLabel(I18nUI.Configuracion.LABEL_FILTRO_HERRAMIENTAS()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panelLimitesSeguridad.add(chkSoloProxy, gbc);

        panelLimitesSeguridad.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelLimitesSeguridad.getPreferredSize().height));
        contenido.add(panelLimitesSeguridad);
        contenido.add(Box.createVerticalStrut(15));

        // DRY: PANEL 3 - APARIENCIA (renombrado de panelFuentes)
        JPanel panelApariencia = crearPanelConfiguracionApariencia();
        panelApariencia.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelApariencia.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelApariencia.getPreferredSize().height));
        contenido.add(panelApariencia);

        root.add(new JScrollPane(contenido), BorderLayout.CENTER);
        return root;
    }

    // DRY: Panel de apariencia -renombrado de crearPanelConfiguracionFuentes()
    private JPanel crearPanelConfiguracionApariencia() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Configuracion.TITULO_APARIENCIA(), 12, 16));

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
            comboFuenteEstandar.setSelectedItem(ConfiguracionAPI.FUENTE_ESTANDAR_DEFECTO);
            spinnerTamanioEstandar.setValue(ConfiguracionAPI.TAMANIO_FUENTE_ESTANDAR_DEFECTO);
            comboFuenteMono.setSelectedItem(ConfiguracionAPI.FUENTE_MONO_DEFECTO);
            spinnerTamanioMono.setValue(ConfiguracionAPI.TAMANIO_FUENTE_MONO_DEFECTO);
        }
    }

    private JPanel crearPanelMultiProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_MULTI_PROVEEDOR(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 3;
        chkHabilitarMultiProveedor = new JCheckBox(I18nUI.Configuracion.LABEL_HABILITAR_MULTI_PROVEEDOR());
        chkHabilitarMultiProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkHabilitarMultiProveedor.addActionListener(e -> actualizarEstadoMultiProveedor());
        panel.add(chkHabilitarMultiProveedor, gbc);

        fila++;

        gbc.gridy = fila;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(16, 8, 8, 8);
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.insets = new Insets(8, 8, 8, 8);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDORES_DISPONIBLES()), gbc);

        fila++;

        gbc.gridy = fila;
        gbc.gridheight = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        modeloListaDisponibles = new DefaultListModel<>();
        listaProveedoresDisponibles = new JList<>(modeloListaDisponibles);
        listaProveedoresDisponibles.setFont(EstilosUI.FUENTE_ESTANDAR);
        listaProveedoresDisponibles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProveedoresDisponibles.setVisibleRowCount(5);
        JScrollPane scrollDisponibles = new JScrollPane(listaProveedoresDisponibles);
        scrollDisponibles.setPreferredSize(new Dimension(200, 120));
        panel.add(scrollDisponibles, gbc);

        gbc.gridx = 1;
        gbc.gridy = fila;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panelBotonesCentro = new JPanel(new GridLayout(4, 1, 5, 5));
        btnAgregarProveedor = new JButton("→");
        btnAgregarProveedor.setToolTipText("Agregar proveedor seleccionado");
        btnAgregarProveedor.setEnabled(false);
        btnAgregarProveedor.addActionListener(e -> agregarProveedorSeleccionado());
        panelBotonesCentro.add(btnAgregarProveedor);

        btnQuitarProveedor = new JButton("←");
        btnQuitarProveedor.setToolTipText("Quitar proveedor seleccionado");
        btnQuitarProveedor.setEnabled(false);
        btnQuitarProveedor.addActionListener(e -> quitarProveedorSeleccionado());
        panelBotonesCentro.add(btnQuitarProveedor);

        Box.createVerticalStrut(10);
        panelBotonesCentro.add(Box.createVerticalStrut(10));

        panel.add(panelBotonesCentro, gbc);

        gbc.gridx = 2;
        gbc.gridheight = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        modeloListaSeleccionados = new DefaultListModel<>();
        listaProveedoresSeleccionados = new JList<>(modeloListaSeleccionados);
        listaProveedoresSeleccionados.setFont(EstilosUI.FUENTE_ESTANDAR);
        listaProveedoresSeleccionados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProveedoresSeleccionados.setVisibleRowCount(5);

        ListSelectionModel seleccionDisponibles = listaProveedoresDisponibles.getSelectionModel();
        seleccionDisponibles.addListSelectionListener(e -> actualizarBotonesMultiProveedor());

        ListSelectionModel seleccionSeleccionados = listaProveedoresSeleccionados.getSelectionModel();
        seleccionSeleccionados.addListSelectionListener(e -> actualizarBotonesMultiProveedor());

        JScrollPane scrollSeleccionados = new JScrollPane(listaProveedoresSeleccionados);
        scrollSeleccionados.setPreferredSize(new Dimension(200, 120));
        panel.add(scrollSeleccionados, gbc);

        fila += 5;

        gbc.gridx = 2;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;

        JPanel panelBotonesReordenar = new JPanel(new GridLayout(1, 2, 5, 0));
        btnSubirProveedor = new JButton("▲");
        btnSubirProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnSubirProveedor.setToolTipText("Subir proveedor en el orden");
        btnSubirProveedor.setEnabled(false);
        btnSubirProveedor.addActionListener(e -> subirProveedor());
        panelBotonesReordenar.add(btnSubirProveedor);

        btnBajarProveedor = new JButton("▼");
        btnBajarProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnBajarProveedor.setToolTipText("Bajar proveedor en el orden");
        btnBajarProveedor.setEnabled(false);
        btnBajarProveedor.addActionListener(e -> bajarProveedor());
        panelBotonesReordenar.add(btnBajarProveedor);

        panel.add(panelBotonesReordenar, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lblEstadoMultiProveedor = new JLabel(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
        lblEstadoMultiProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblEstadoMultiProveedor.setForeground(EstilosUI.COLOR_INFO);
        panel.add(lblEstadoMultiProveedor, gbc);

        fila++;

        listaProveedoresSeleccionados.addListSelectionListener(e -> {
            actualizarBotonesReordenamiento();
        });

        return panel;
    }

    private void actualizarEstadoMultiProveedor() {
        boolean habilitado = chkHabilitarMultiProveedor.isSelected();

        listaProveedoresDisponibles.setEnabled(habilitado);
        listaProveedoresSeleccionados.setEnabled(habilitado);
        btnAgregarProveedor.setEnabled(habilitado);
        btnQuitarProveedor.setEnabled(habilitado);
        btnSubirProveedor.setEnabled(habilitado);
        btnBajarProveedor.setEnabled(habilitado);

        if (habilitado) {
            asegurarProveedorActualEnPrimeraPosicion();
            actualizarEtiquetaEstadoMultiProveedor();
        } else {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
        }
    }

    private void actualizarBotonesMultiProveedor() {
        boolean haySeleccionDisponible = !listaProveedoresDisponibles.isSelectionEmpty();
        boolean haySeleccionSeleccionados = !listaProveedoresSeleccionados.isSelectionEmpty();

        btnAgregarProveedor.setEnabled(haySeleccionDisponible && chkHabilitarMultiProveedor.isSelected());
        btnQuitarProveedor.setEnabled(haySeleccionSeleccionados && chkHabilitarMultiProveedor.isSelected());

        actualizarBotonesReordenamiento();
    }

    private void actualizarBotonesReordenamiento() {
        int indiceSeleccionado = listaProveedoresSeleccionados.getSelectedIndex();
        boolean habilitado = chkHabilitarMultiProveedor.isSelected();

        btnSubirProveedor.setEnabled(habilitado && indiceSeleccionado > 0);
        btnBajarProveedor.setEnabled(habilitado && indiceSeleccionado >= 0 &&
                indiceSeleccionado < modeloListaSeleccionados.getSize() - 1);
    }

    private void asegurarProveedorActualEnPrimeraPosicion() {
        String proveedorActual = (String) comboProveedor.getSelectedItem();
        if (Normalizador.esVacio(proveedorActual)) {
            return;
        }

        int indiceActual = modeloListaSeleccionados.indexOf(proveedorActual);

        if (indiceActual == 0) {
            return;
        }

        if (indiceActual > 0) {
            modeloListaSeleccionados.removeElementAt(indiceActual);
            modeloListaSeleccionados.insertElementAt(proveedorActual, 0);
        } else {
            if (modeloListaSeleccionados.getSize() >= 5) {
                modeloListaSeleccionados.removeElementAt(modeloListaSeleccionados.getSize() - 1);
            }
            modeloListaSeleccionados.insertElementAt(proveedorActual, 0);

            modeloListaDisponibles.removeElement(proveedorActual);
        }

        listaProveedoresSeleccionados.setSelectedIndex(0);
        actualizarBotonesMultiProveedor();
    }

    private void actualizarEtiquetaEstadoMultiProveedor() {
        int cantidad = modeloListaSeleccionados.getSize();
        if (cantidad == 0) {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
        } else if (cantidad == 1) {
            lblEstadoMultiProveedor.setText("⚠️ " + I18nUI.Configuracion.ERROR_MIN_PROVEEDORES());
        } else {
            lblEstadoMultiProveedor
                    .setText("✅ " + I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_HABILITADO(String.valueOf(cantidad)));
        }
    }

    private void agregarProveedorSeleccionado() {
        String proveedor = listaProveedoresDisponibles.getSelectedValue();
        if (Normalizador.esVacio(proveedor)) {
            return;
        }

        if (modeloListaSeleccionados.getSize() >= 5) {
            UIUtils.mostrarAdvertencia(this, "Límite alcanzado", I18nUI.Configuracion.ERROR_MAX_PROVEEDORES());
            return;
        }

        if (modeloListaSeleccionados.indexOf(proveedor) >= 0) {
            return;
        }

        modeloListaSeleccionados.addElement(proveedor);

        modeloListaDisponibles.removeElement(proveedor);

        actualizarEtiquetaEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
    }

    private void quitarProveedorSeleccionado() {
        String proveedor = listaProveedoresSeleccionados.getSelectedValue();
        if (Normalizador.esVacio(proveedor)) {
            return;
        }

        int indiceSeleccionado = listaProveedoresSeleccionados.getSelectedIndex();
        String proveedorActual = (String) comboProveedor.getSelectedItem();

        if (indiceSeleccionado == 0 && proveedor.equals(proveedorActual)) {
            UIUtils.mostrarAdvertencia(this,
                    I18nUI.Configuracion.TITULO_ADVERTENCIA_PROVEEDOR_PRINCIPAL(),
                    I18nUI.Configuracion.MSG_NO_QUITAR_PROVEEDOR_PRINCIPAL());
            return;
        }

        modeloListaSeleccionados.removeElement(proveedor);

        if (!proveedor.equals(proveedorActual)) {
            modeloListaDisponibles.addElement(proveedor);
        }

        actualizarEtiquetaEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
    }

    private void subirProveedor() {
        int indice = listaProveedoresSeleccionados.getSelectedIndex();
        if (indice <= 0) {
            return;
        }

        String proveedor = modeloListaSeleccionados.remove(indice);
        modeloListaSeleccionados.add(indice - 1, proveedor);
        listaProveedoresSeleccionados.setSelectedIndex(indice - 1);
        actualizarBotonesReordenamiento();

        if (indice - 1 == 0 && !actualizandoProveedorUi) {
            actualizandoListaMultiProveedor = true;
            try {
                comboProveedor.setSelectedItem(proveedor);
            } finally {
                actualizandoListaMultiProveedor = false;
            }
        }
    }

    private void bajarProveedor() {
        int indice = listaProveedoresSeleccionados.getSelectedIndex();
        if (indice < 0 || indice >= modeloListaSeleccionados.getSize() - 1) {
            return;
        }

        String proveedor = modeloListaSeleccionados.remove(indice);
        modeloListaSeleccionados.add(indice + 1, proveedor);
        listaProveedoresSeleccionados.setSelectedIndex(indice + 1);
        actualizarBotonesReordenamiento();

        if (indice == 0 && !actualizandoProveedorUi) {
            String nuevoProveedorPrincipal = modeloListaSeleccionados.getElementAt(0);
            actualizandoListaMultiProveedor = true;
            try {
                comboProveedor.setSelectedItem(nuevoProveedorPrincipal);
            } finally {
                actualizandoListaMultiProveedor = false;
            }
        }
    }

    private JPanel crearPanelPrompt() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PROMPT_ANALISIS(), 12, 16));

        txtPrompt = new JTextArea();
        txtPrompt.setFont(EstilosUI.FUENTE_TABLA);
        txtPrompt.setLineWrap(false);
        txtPrompt.setWrapStyleWord(false);
        txtPrompt.setTabSize(2);
        // Tooltip completo con instrucciones y ejemplo JSON
        txtPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_EDITOR());

        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelEditor.add(scrollPrompt, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 6));
        panelSur.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // ARMONÍA: Botón y contador agrupados a la izquierda (proximidad)
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        btnRestaurarPrompt = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPrompt.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT());
        btnRestaurarPrompt.addActionListener(e -> restaurarPromptPorDefecto());
        panelAcciones.add(btnRestaurarPrompt);

        lblContadorPrompt = new JLabel(I18nUI.Configuracion.CONTADOR_CARACTERES(0));
        lblContadorPrompt.setFont(EstilosUI.FUENTE_TABLA);
        lblContadorPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.CONTADOR_PROMPT());
        panelAcciones.add(lblContadorPrompt);

        panelSur.add(panelAcciones, BorderLayout.NORTH);

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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 10, 20));

        JPanel panelAgenteGeneral = new JPanel(new GridBagLayout());
        panelAgenteGeneral.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.Agentes.TITULO_EJECUCION_AGENTE(), 10, 14));

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

        txtAgenteBinario = new JTextField(30);
        txtAgenteBinario.getDocument()
                .addDocumentListener(UIUtils.crearDocumentListener(this::actualizarRutaEnMemoria));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panelAgenteGeneral.add(new JLabel(I18nUI.Configuracion.Agentes.LABEL_RUTA_BINARIO()), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelAgenteGeneral.add(txtAgenteBinario, gbc);

        chkAgenteHabilitado = new JCheckBox(I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE());
        chkAgenteHabilitado.setFont(EstilosUI.FUENTE_ESTANDAR);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        panelAgenteGeneral.add(chkAgenteHabilitado, gbc);

        JPanel panelPrompts = new JPanel(new GridLayout(1, 2, 10, 0));
        panelPrompts.setOpaque(false);

        txtAgentePromptInicial = new JTextArea();
        configurarAreaPromptAgente(txtAgentePromptInicial);

        btnRestaurarPromptAgenteInicial = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgenteInicial.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgenteInicial.addActionListener(
                e -> txtAgentePromptInicial.setText(ConfiguracionAPI.obtenerAgentePreflightPromptPorDefecto()));

        txtAgentePrompt = new JTextArea();
        configurarAreaPromptAgente(txtAgentePrompt);

        btnRestaurarPromptAgente = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgente.addActionListener(
                e -> txtAgentePrompt.setText(ConfiguracionAPI.obtenerAgentePromptPorDefecto()));

        panelPrompts.add(crearSeccionPromptAgente(
                I18nUI.Configuracion.Agentes.TITULO_PROMPT_INICIAL_AGENTE(),
                txtAgentePromptInicial,
                btnRestaurarPromptAgenteInicial,
                I18nUI.Configuracion.Agentes.DESCRIPCION_PROMPT_INICIAL_AGENTE(),
                FlowLayout.LEFT));
        panelPrompts.add(crearSeccionPromptAgente(
                I18nUI.Configuracion.Agentes.TITULO_PROMPT_AGENTE(),
                txtAgentePrompt,
                btnRestaurarPromptAgente,
                I18nUI.Configuracion.Agentes.DESCRIPCION_PROMPT_VALIDACION_AGENTE(),
                FlowLayout.RIGHT));

        GridBagConstraints gbcRoot = new GridBagConstraints();
        gbcRoot.gridx = 0;
        gbcRoot.gridy = 0;
        gbcRoot.weightx = 1.0;
        gbcRoot.weighty = 0;
        gbcRoot.fill = GridBagConstraints.HORIZONTAL;
        gbcRoot.anchor = GridBagConstraints.NORTHWEST;
        panel.add(panelAgenteGeneral, gbcRoot);

        gbcRoot.gridy = 1;
        gbcRoot.insets = new Insets(10, 0, 0, 0);
        gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH;
        panel.add(panelPrompts, gbcRoot);

        return panel;
    }

    private void configurarAreaPromptAgente(JTextArea area) {
        if (area == null) {
            return;
        }
        area.setFont(EstilosUI.FUENTE_MONO);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setTabSize(2);
        area.setRows(FILAS_PROMPT_AGENTE);
        area.setColumns(1);
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    }

    private JScrollPane crearScrollPromptAgente(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(ANCHO_SCROLL_PROMPT_AGENTE, ALTO_SCROLL_PROMPT_AGENTE));
        scroll.setMinimumSize(new Dimension(140, 120));
        return scroll;
    }

    private JPanel crearSeccionPromptAgente(String titulo,
            JTextArea area,
            JButton botonRestaurar,
            String descripcion,
            int alineacionBoton) {
        JPanel seccion = new JPanel(new BorderLayout(0, 6));
        seccion.setOpaque(false);
        seccion.setBorder(UIUtils.crearBordeTitulado(titulo, 8, 10));

        if (Normalizador.noEsVacio(descripcion)) {
            JLabel lblDescripcion = new JLabel(descripcion);
            lblDescripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
            lblDescripcion.setForeground(EstilosUI.colorTextoSecundario(EstilosUI.obtenerFondoPanel()));
            seccion.add(lblDescripcion, BorderLayout.NORTH);
        }

        seccion.add(crearScrollPromptAgente(area), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(alineacionBoton, 0, 0));
        acciones.setOpaque(false);
        acciones.add(botonRestaurar);
        seccion.add(acciones, BorderLayout.SOUTH);

        return seccion;
    }

    private JPanel crearPanelAcercaDe() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(EstilosUI.obtenerFondoPanel());

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

        JPanel contentScroll = new JPanel(new BorderLayout());
        contentScroll.setOpaque(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        content.setOpaque(false);

        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_RESUMEN(VersionBurpIA.obtenerVersionActual()),
                I18nUI.Configuracion.DESCRIPCION_APP(),
                null));
        content.add(Box.createVerticalStrut(20));

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
        txtMaximoTareas.setText(String.valueOf(config.obtenerMaximoTareasTabla()));
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

        // Cargar configuración multi-proveedor
        chkHabilitarMultiProveedor.setSelected(config.esMultiProveedorHabilitado());

        // Cargar proveedores seleccionados PRIMERO (para filtrar correctamente)
        modeloListaSeleccionados.clear();
        List<String> seleccionados = config.obtenerProveedoresMultiConsulta();
        for (String proveedor : seleccionados) {
            modeloListaSeleccionados.addElement(proveedor);
        }

        // Cargar proveedores disponibles DESPUÉS (filtrando contra seleccionados ya
        // cargados)
        modeloListaDisponibles.clear();
        List<String> disponibles = config.obtenerProveedoresDisponibles();
        for (String proveedor : disponibles) {
            if (modeloListaSeleccionados.indexOf(proveedor) == -1) {
                modeloListaDisponibles.addElement(proveedor);
            }
        }

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
        estadoInicialDialogo = capturarEstadoEdicionActual();
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
        final String maximoTareasTexto = txtMaximoTareas.getText();

        final String nombreFuenteEstandar = (String) comboFuenteEstandar.getSelectedItem();
        final int tamanioFuenteEstandar = (int) spinnerTamanioEstandar.getValue();
        final String nombreFuenteMono = (String) comboFuenteMono.getSelectedItem();
        final int tamanioFuenteMono = (int) spinnerTamanioMono.getValue();

        final boolean multiProveedorHabilitado = chkHabilitarMultiProveedor.isSelected();
        final List<String> proveedoresMultiConsulta = new ArrayList<>();
        for (int i = 0; i < modeloListaSeleccionados.getSize(); i++) {
            proveedoresMultiConsulta.add(modeloListaSeleccionados.getElementAt(i));
        }

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

                    // CONFIABILIDAD: Guardar estado actual del proveedor seleccionado antes de procesar todos
                    estadoProveedorTemporal.put(proveedorSeleccionado, extraerEstadoActualRapido());

                    // CONFIABILIDAD: Guardar TODOS los proveedores configurados, no solo el seleccionado
                    for (Map.Entry<String, EstadoProveedorUI> entry : estadoProveedorTemporal.entrySet()) {
                        String nombreProveedor = entry.getKey();
                        EstadoProveedorUI estadoProveedor = entry.getValue();

                        if (estadoProveedor != null && Normalizador.noEsVacio(nombreProveedor)) {
                            snapshot.establecerApiKeyParaProveedor(nombreProveedor, estadoProveedor.getApiKey());
                            snapshot.establecerModeloParaProveedor(nombreProveedor, estadoProveedor.getModelo());
                            snapshot.establecerUrlBaseParaProveedor(nombreProveedor, estadoProveedor.getBaseUrl());
                            snapshot.establecerMaxTokensParaProveedor(nombreProveedor, estadoProveedor.getMaxTokens());
                            snapshot.establecerTiempoEsperaParaModelo(nombreProveedor, estadoProveedor.getModelo(),
                                    estadoProveedor.getTimeout());
                        }
                    }

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
                    snapshot.establecerMaximoTareasTabla(parsearEntero(maximoTareasTexto));

                    snapshot.establecerNombreFuenteEstandar(nombreFuenteEstandar);
                    snapshot.establecerTamanioFuenteEstandar(tamanioFuenteEstandar);
                    snapshot.establecerNombreFuenteMono(nombreFuenteMono);
                    snapshot.establecerTamanioFuenteMono(tamanioFuenteMono);

                    snapshot.establecerMultiProveedorHabilitado(multiProveedorHabilitado);
                    snapshot.establecerProveedoresMultiConsulta(proveedoresMultiConsulta);

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
                        estadoInicialDialogo = capturarEstadoEdicionActual();
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

            if (chkHabilitarMultiProveedor.isSelected() && !actualizandoListaMultiProveedor) {
                SwingUtilities.invokeLater(this::asegurarProveedorActualEnPrimeraPosicion);
            }
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
        // Se conserva el idioma solo en la UI hasta que el usuario pulse Guardar.
    }

    private void intentarCerrarDialogo() {
        if (guardandoConfiguracion) {
            return;
        }

        if (!tieneCambiosSinGuardar()) {
            dispose();
            return;
        }

        boolean confirmarDescarte = UIUtils.confirmarAdvertencia(
                this,
                I18nUI.Configuracion.TITULO_CONFIRMAR_DESCARTE_CAMBIOS_AJUSTES(),
                I18nUI.Configuracion.MSG_CONFIRMAR_DESCARTE_CAMBIOS_AJUSTES());
        if (confirmarDescarte) {
            dispose();
        }
    }

    private boolean tieneCambiosSinGuardar() {
        if (estadoInicialDialogo == null) {
            return false;
        }
        EstadoEdicionDialogo estadoActual = capturarEstadoEdicionActual();
        return !estadoInicialDialogo.equals(estadoActual);
    }

    private EstadoEdicionDialogo capturarEstadoEdicionActual() {
        String proveedorSeleccionado = obtenerTextoSeleccionado(comboProveedor);
        String agenteSeleccionado = obtenerTextoSeleccionado(comboAgente);

        Map<String, String> rutasAgente = new HashMap<>(rutasBinarioAgenteTemporal);
        if (Normalizador.noEsVacio(agenteSeleccionado) && txtAgenteBinario != null) {
            rutasAgente.put(agenteSeleccionado, textoSeguro(txtAgenteBinario.getText()));
        }

        Map<String, EstadoProveedorUI> borradores = new HashMap<>(estadoProveedorTemporal);
        if (Normalizador.noEsVacio(proveedorSeleccionado)) {
            borradores.put(proveedorSeleccionado, extraerEstadoActualRapido());
        }

        Map<String, EstadoProveedorSnapshot> estadosProveedor = new HashMap<>();
        for (String proveedor : ProveedorAI.obtenerNombresProveedores()) {
            EstadoProveedorUI borrador = borradores.get(proveedor);
            estadosProveedor.put(proveedor, crearEstadoProveedorSnapshot(proveedor, borrador));
        }

        List<String> proveedoresSeleccionados = new ArrayList<>();
        if (modeloListaSeleccionados != null) {
            for (int i = 0; i < modeloListaSeleccionados.getSize(); i++) {
                proveedoresSeleccionados.add(modeloListaSeleccionados.getElementAt(i));
            }
        }

        return new EstadoEdicionDialogo(
                obtenerCodigoIdiomaSeleccionado(),
                proveedorSeleccionado,
                obtenerModeloSeleccionado(),
                textoSeguro(txtUrl != null ? txtUrl.getText() : ""),
                textoSeguro(txtClave != null ? new String(txtClave.getPassword()) : ""),
                textoSeguro(txtMaxTokens != null ? txtMaxTokens.getText() : ""),
                textoSeguro(txtTimeoutModelo != null ? txtTimeoutModelo.getText() : ""),
                textoSeguro(txtRetraso != null ? txtRetraso.getText() : ""),
                textoSeguro(txtMaximoConcurrente != null ? txtMaximoConcurrente.getText() : ""),
                textoSeguro(txtMaximoHallazgosTabla != null ? txtMaximoHallazgosTabla.getText() : ""),
                textoSeguro(txtMaximoTareas != null ? txtMaximoTareas.getText() : ""),
                chkDetallado != null && chkDetallado.isSelected(),
                chkIgnorarSSL != null && chkIgnorarSSL.isSelected(),
                chkSoloProxy != null && chkSoloProxy.isSelected(),
                chkAlertasHabilitadas != null && chkAlertasHabilitadas.isSelected(),
                chkPersistirBusqueda != null && chkPersistirBusqueda.isSelected(),
                chkPersistirSeveridad != null && chkPersistirSeveridad.isSelected(),
                textoSeguro(txtPrompt != null ? txtPrompt.getText() : ""),
                chkAgenteHabilitado != null && chkAgenteHabilitado.isSelected(),
                agenteSeleccionado,
                textoSeguro(txtAgentePromptInicial != null ? txtAgentePromptInicial.getText() : ""),
                textoSeguro(txtAgentePrompt != null ? txtAgentePrompt.getText() : ""),
                textoSeguro(comboFuenteEstandar != null ? (String) comboFuenteEstandar.getSelectedItem() : ""),
                spinnerTamanioEstandar != null ? (int) spinnerTamanioEstandar.getValue() : 0,
                textoSeguro(comboFuenteMono != null ? (String) comboFuenteMono.getSelectedItem() : ""),
                spinnerTamanioMono != null ? (int) spinnerTamanioMono.getValue() : 0,
                chkHabilitarMultiProveedor != null && chkHabilitarMultiProveedor.isSelected(),
                rutasAgente,
                proveedoresSeleccionados,
                estadosProveedor);
    }

    private EstadoProveedorSnapshot crearEstadoProveedorSnapshot(String proveedor, EstadoProveedorUI borrador) {
        if (borrador != null) {
            return new EstadoProveedorSnapshot(
                    textoSeguro(borrador.getApiKey()),
                    textoSeguro(borrador.getModelo()),
                    textoSeguro(borrador.getBaseUrl()),
                    borrador.getMaxTokens(),
                    borrador.getTimeout());
        }

        String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
        String modelo = config.obtenerModeloParaProveedor(proveedor);
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (Normalizador.esVacio(modelo) && configProveedor != null) {
            modelo = configProveedor.obtenerModeloPorDefecto();
        }

        Integer maxTokensConfigurado = config.obtenerMaxTokensConfiguradoParaProveedor(proveedor);
        int maxTokens = maxTokensConfigurado != null
                ? maxTokensConfigurado
                : (configProveedor != null ? configProveedor.obtenerMaxTokensPorDefecto() : EstadoProveedorUI.MAX_TOKENS_POR_DEFECTO);
        int timeout = config.obtenerTiempoEsperaParaModelo(proveedor, modelo);

        return new EstadoProveedorSnapshot(
                textoSeguro(apiKey),
                textoSeguro(modelo),
                textoSeguro(config.obtenerUrlBaseParaProveedor(proveedor)),
                maxTokens,
                timeout);
    }

    private String obtenerCodigoIdiomaSeleccionado() {
        IdiomaUI idioma = comboIdioma != null ? (IdiomaUI) comboIdioma.getSelectedItem() : null;
        return idioma != null ? idioma.codigo() : config.obtenerIdiomaUi();
    }

    private String obtenerTextoSeleccionado(JComboBox<String> combo) {
        if (combo == null) {
            return "";
        }
        Object seleccionado = combo.getSelectedItem();
        return seleccionado != null ? seleccionado.toString() : "";
    }

    private String textoSeguro(String valor) {
        return valor != null ? valor : "";
    }

    private static final class EstadoProveedorSnapshot {
        private final String apiKey;
        private final String modelo;
        private final String baseUrl;
        private final int maxTokens;
        private final int timeout;

        private EstadoProveedorSnapshot(String apiKey, String modelo, String baseUrl, int maxTokens, int timeout) {
            this.apiKey = apiKey;
            this.modelo = modelo;
            this.baseUrl = baseUrl;
            this.maxTokens = maxTokens;
            this.timeout = timeout;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EstadoProveedorSnapshot)) {
                return false;
            }
            EstadoProveedorSnapshot that = (EstadoProveedorSnapshot) o;
            return maxTokens == that.maxTokens
                    && timeout == that.timeout
                    && Objects.equals(apiKey, that.apiKey)
                    && Objects.equals(modelo, that.modelo)
                    && Objects.equals(baseUrl, that.baseUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiKey, modelo, baseUrl, maxTokens, timeout);
        }
    }

    private static final class EstadoEdicionDialogo {
        private final String idiomaUi;
        private final String proveedorSeleccionado;
        private final String modeloSeleccionado;
        private final String urlActual;
        private final String apiKeyActual;
        private final String maxTokensTexto;
        private final String timeoutTexto;
        private final String retrasoTexto;
        private final String maximoConcurrenteTexto;
        private final String maximoHallazgosTexto;
        private final String maximoTareasTexto;
        private final boolean detallado;
        private final boolean ignorarSsl;
        private final boolean soloProxy;
        private final boolean alertasHabilitadas;
        private final boolean persistirBusqueda;
        private final boolean persistirSeveridad;
        private final String prompt;
        private final boolean agenteHabilitado;
        private final String tipoAgente;
        private final String agentePromptInicial;
        private final String agentePrompt;
        private final String fuenteEstandar;
        private final int tamanioFuenteEstandar;
        private final String fuenteMono;
        private final int tamanioFuenteMono;
        private final boolean multiProveedorHabilitado;
        private final Map<String, String> rutasBinarioAgente;
        private final List<String> proveedoresMultiConsulta;
        private final Map<String, EstadoProveedorSnapshot> estadosProveedor;

        private EstadoEdicionDialogo(
                String idiomaUi,
                String proveedorSeleccionado,
                String modeloSeleccionado,
                String urlActual,
                String apiKeyActual,
                String maxTokensTexto,
                String timeoutTexto,
                String retrasoTexto,
                String maximoConcurrenteTexto,
                String maximoHallazgosTexto,
                String maximoTareasTexto,
                boolean detallado,
                boolean ignorarSsl,
                boolean soloProxy,
                boolean alertasHabilitadas,
                boolean persistirBusqueda,
                boolean persistirSeveridad,
                String prompt,
                boolean agenteHabilitado,
                String tipoAgente,
                String agentePromptInicial,
                String agentePrompt,
                String fuenteEstandar,
                int tamanioFuenteEstandar,
                String fuenteMono,
                int tamanioFuenteMono,
                boolean multiProveedorHabilitado,
                Map<String, String> rutasBinarioAgente,
                List<String> proveedoresMultiConsulta,
                Map<String, EstadoProveedorSnapshot> estadosProveedor) {
            this.idiomaUi = idiomaUi;
            this.proveedorSeleccionado = proveedorSeleccionado;
            this.modeloSeleccionado = modeloSeleccionado;
            this.urlActual = urlActual;
            this.apiKeyActual = apiKeyActual;
            this.maxTokensTexto = maxTokensTexto;
            this.timeoutTexto = timeoutTexto;
            this.retrasoTexto = retrasoTexto;
            this.maximoConcurrenteTexto = maximoConcurrenteTexto;
            this.maximoHallazgosTexto = maximoHallazgosTexto;
            this.maximoTareasTexto = maximoTareasTexto;
            this.detallado = detallado;
            this.ignorarSsl = ignorarSsl;
            this.soloProxy = soloProxy;
            this.alertasHabilitadas = alertasHabilitadas;
            this.persistirBusqueda = persistirBusqueda;
            this.persistirSeveridad = persistirSeveridad;
            this.prompt = prompt;
            this.agenteHabilitado = agenteHabilitado;
            this.tipoAgente = tipoAgente;
            this.agentePromptInicial = agentePromptInicial;
            this.agentePrompt = agentePrompt;
            this.fuenteEstandar = fuenteEstandar;
            this.tamanioFuenteEstandar = tamanioFuenteEstandar;
            this.fuenteMono = fuenteMono;
            this.tamanioFuenteMono = tamanioFuenteMono;
            this.multiProveedorHabilitado = multiProveedorHabilitado;
            this.rutasBinarioAgente = rutasBinarioAgente != null
                    ? Collections.unmodifiableMap(new HashMap<>(rutasBinarioAgente))
                    : Collections.emptyMap();
            this.proveedoresMultiConsulta = proveedoresMultiConsulta != null
                    ? Collections.unmodifiableList(new ArrayList<>(proveedoresMultiConsulta))
                    : Collections.emptyList();
            this.estadosProveedor = estadosProveedor != null
                    ? Collections.unmodifiableMap(new HashMap<>(estadosProveedor))
                    : Collections.emptyMap();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EstadoEdicionDialogo)) {
                return false;
            }
            EstadoEdicionDialogo that = (EstadoEdicionDialogo) o;
            return detallado == that.detallado
                    && ignorarSsl == that.ignorarSsl
                    && soloProxy == that.soloProxy
                    && alertasHabilitadas == that.alertasHabilitadas
                    && persistirBusqueda == that.persistirBusqueda
                    && persistirSeveridad == that.persistirSeveridad
                    && agenteHabilitado == that.agenteHabilitado
                    && tamanioFuenteEstandar == that.tamanioFuenteEstandar
                    && tamanioFuenteMono == that.tamanioFuenteMono
                    && multiProveedorHabilitado == that.multiProveedorHabilitado
                    && Objects.equals(idiomaUi, that.idiomaUi)
                    && Objects.equals(proveedorSeleccionado, that.proveedorSeleccionado)
                    && Objects.equals(modeloSeleccionado, that.modeloSeleccionado)
                    && Objects.equals(urlActual, that.urlActual)
                    && Objects.equals(apiKeyActual, that.apiKeyActual)
                    && Objects.equals(maxTokensTexto, that.maxTokensTexto)
                    && Objects.equals(timeoutTexto, that.timeoutTexto)
                    && Objects.equals(retrasoTexto, that.retrasoTexto)
                    && Objects.equals(maximoConcurrenteTexto, that.maximoConcurrenteTexto)
                    && Objects.equals(maximoHallazgosTexto, that.maximoHallazgosTexto)
                    && Objects.equals(maximoTareasTexto, that.maximoTareasTexto)
                    && Objects.equals(prompt, that.prompt)
                    && Objects.equals(tipoAgente, that.tipoAgente)
                    && Objects.equals(agentePromptInicial, that.agentePromptInicial)
                    && Objects.equals(agentePrompt, that.agentePrompt)
                    && Objects.equals(fuenteEstandar, that.fuenteEstandar)
                    && Objects.equals(fuenteMono, that.fuenteMono)
                    && Objects.equals(rutasBinarioAgente, that.rutasBinarioAgente)
                    && Objects.equals(proveedoresMultiConsulta, that.proveedoresMultiConsulta)
                    && Objects.equals(estadosProveedor, that.estadosProveedor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    idiomaUi,
                    proveedorSeleccionado,
                    modeloSeleccionado,
                    urlActual,
                    apiKeyActual,
                    maxTokensTexto,
                    timeoutTexto,
                    retrasoTexto,
                    maximoConcurrenteTexto,
                    maximoHallazgosTexto,
                    maximoTareasTexto,
                    detallado,
                    ignorarSsl,
                    soloProxy,
                    alertasHabilitadas,
                    persistirBusqueda,
                    persistirSeveridad,
                    prompt,
                    agenteHabilitado,
                    tipoAgente,
                    agentePromptInicial,
                    agentePrompt,
                    fuenteEstandar,
                    tamanioFuenteEstandar,
                    fuenteMono,
                    tamanioFuenteMono,
                    multiProveedorHabilitado,
                    rutasBinarioAgente,
                    proveedoresMultiConsulta,
                    estadosProveedor);
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
