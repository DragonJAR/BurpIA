package com.burpia.ui;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.ProbadorConexionAI;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private JTextArea txtPrompt;
    private JButton btnRestaurarPrompt;
    private JLabel lblContadorPrompt;

    private JCheckBox chkAgenteFactoryDroidHabilitado;
    private JTextField txtAgenteFactoryDroidBinario;
    private JTextArea txtAgenteFactoryDroidPrompt;
    private JButton btnRestaurarPromptAgente;
    private JLabel lblContadorPromptAgente;

    private JComboBox<String> comboProveedor;
    private JComboBox<String> comboModelo;
    private JComboBox<IdiomaUI> comboIdioma;
    private JButton btnRefrescarModelos;
    private JButton btnProbarConexion;
    private JTextField txtMaxTokens;
    private JTextField txtTimeoutModelo;

    private static final String MODELO_CUSTOM = "-- Custom --";
    private static final int TIMEOUT_CONEXION_MODELOS_SEG = 8;
    private static final int TIMEOUT_LECTURA_MODELOS_SEG = 12;

    public DialogoConfiguracion(Window padre, ConfiguracionAPI config, GestorConfiguracion gestorConfig, Runnable alGuardar) {
        super(padre, I18nUI.Configuracion.TITULO_DIALOGO(), Dialog.ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.gestorConfig = gestorConfig;
        this.alGuardar = alGuardar;

        inicializarComponentes();
        cargarConfiguracionActual();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));
        setPreferredSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel panelGeneral = crearPanelGeneral();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROVEEDOR(), panelGeneral);

        JPanel panelPrompt = crearPanelPrompt();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROMPT(), panelPrompt);

        JPanel panelAgentes = crearPanelAgentes();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_AGENTES(), panelAgentes);

        JPanel panelAcercaDe = crearPanelAcercaDe();
        tabbedPane.addTab(I18nUI.Configuracion.TAB_ACERCA(), panelAcercaDe);

        JButton btnGuardar = new JButton(I18nUI.Configuracion.BOTON_GUARDAR());
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.setToolTipText(I18nUI.Tooltips.Configuracion.GUARDAR());
        btnGuardar.addActionListener(e -> guardarConfiguracion());

        btnProbarConexion = new JButton(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
        btnProbarConexion.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnProbarConexion.setToolTipText(I18nUI.Tooltips.Configuracion.PROBAR_CONEXION());
        btnProbarConexion.addActionListener(e -> probarConexion());

        JButton btnCerrar = new JButton(I18nUI.DetalleHallazgo.BOTON_CANCELAR());
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

    private JPanel crearPanelGeneral() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel contenidoGeneral = new JPanel();
        contenidoGeneral.setLayout(new BoxLayout(contenidoGeneral, BoxLayout.Y_AXIS));
        contenidoGeneral.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        contenidoGeneral.setOpaque(false);

        JPanel panelProveedor = crearPanelProveedor();
        panelProveedor.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelProveedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelProveedor.getPreferredSize().height));
        contenidoGeneral.add(panelProveedor);
        contenidoGeneral.add(Box.createVerticalStrut(8));

        JPanel panelEjecucion = new JPanel(new GridBagLayout());
        panelEjecucion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelEjecucion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.TITULO_AJUSTES_USUARIO(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

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
                ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA + ")"
        );
        chkDetallado = new JCheckBox(I18nUI.Configuracion.CHECK_DETALLADO());
        chkDetallado.setToolTipText(I18nUI.Tooltips.Configuracion.DETALLADO());
        chkIgnorarSSL = new JCheckBox(I18nUI.Configuracion.LABEL_IGNORAR_SSL());
        chkIgnorarSSL.setToolTipText(I18nUI.Configuracion.TOOLTIP_IGNORAR_SSL());
        chkSoloProxy = new JCheckBox(I18nUI.Configuracion.LABEL_SOLO_PROXY());
        chkSoloProxy.setToolTipText(I18nUI.Configuracion.TOOLTIP_SOLO_PROXY());

        comboIdioma = new JComboBox<>(IdiomaUI.values());
        comboIdioma.setFont(EstilosUI.FUENTE_ESTANDAR);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_IDIOMA()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(comboIdioma, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_RETRASO()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtRetraso, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_MAXIMO_CONCURRENTE()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtMaximoConcurrente, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_MAX_HALLAZGOS_TABLA()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtMaximoHallazgosTabla, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_MODO_DETALLADO()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(chkDetallado, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0; gbc.gridwidth = 1;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_SEGURIDAD_SSL()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(chkIgnorarSSL, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        panelEjecucion.add(new JLabel(I18nUI.Configuracion.LABEL_FILTRO_HERRAMIENTAS()), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(chkSoloProxy, gbc);

        panelEjecucion.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelEjecucion.getPreferredSize().height));

        contenidoGeneral.add(panelEjecucion);
        root.add(contenidoGeneral, BorderLayout.CENTER);
        return root;
    }

    private JPanel crearPanelProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.TITULO_PROVEEDOR(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDOR_AI()), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        comboProveedor = new JComboBox<>(ProveedorAI.obtenerNombresProveedores().toArray(new String[0]));
        comboProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.PROVEEDOR());
        comboProveedor.addActionListener(e -> alCambiarProveedor());
        panel.add(comboProveedor, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_URL_API()), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtUrl = new JTextField(30);
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtUrl.setToolTipText(I18nUI.Tooltips.Configuracion.URL_API());
        panel.add(txtUrl, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_CLAVE_API()), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtClave = new JPasswordField(30);
        txtClave.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtClave.setToolTipText(I18nUI.Tooltips.Configuracion.CLAVE_API());
        panel.add(txtClave, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
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

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(panelModelo, gbc);

        fila++;

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblMaxTokens = new JLabel(I18nUI.Configuracion.LABEL_MAX_TOKENS());
        lblMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());
        panel.add(lblMaxTokens, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
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

        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblTimeoutModelo = new JLabel(I18nUI.Configuracion.LABEL_TIMEOUT_MODELO());
        lblTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());
        panel.add(lblTimeoutModelo, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtTimeoutModelo = new JTextField(30);
        txtTimeoutModelo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());
        panel.add(txtTimeoutModelo, gbc);

        return panel;
    }

    private JPanel crearPanelPrompt() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel panelInstrucciones = new JPanel(new BorderLayout(0, 8));
        panelInstrucciones.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120, 140, 255), 1),
                I18nUI.Configuracion.TITULO_INSTRUCCIONES(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        panelInstrucciones.setBackground(new Color(244, 248, 255));

        JTextArea txtInstrucciones = new JTextArea();
        txtInstrucciones.setEditable(false);
        txtInstrucciones.setOpaque(false);
        txtInstrucciones.setWrapStyleWord(true);
        txtInstrucciones.setLineWrap(true);
        txtInstrucciones.setFont(EstilosUI.FUENTE_ESTANDAR);
        txtInstrucciones.setText(I18nUI.Configuracion.TEXTO_INSTRUCCIONES());
        txtInstrucciones.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));

        JTextArea ejemploJson = new JTextArea();
        ejemploJson.setEditable(false);
        ejemploJson.setFont(EstilosUI.FUENTE_TABLA);
        ejemploJson.setBackground(new Color(255, 255, 255));
        ejemploJson.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        ejemploJson.setText("{\"hallazgos\":[{\"titulo\":\"string\",\"descripcion\":\"string\",\"severidad\":\"Critical|High|Medium|Low|Info\",\"confianza\":\"High|Medium|Low\",\"evidencia\":\"string\"}]}");

        JPanel bloqueInstrucciones = new JPanel(new BorderLayout(0, 8));
        bloqueInstrucciones.setOpaque(false);
        bloqueInstrucciones.add(txtInstrucciones, BorderLayout.NORTH);
        bloqueInstrucciones.add(ejemploJson, BorderLayout.CENTER);
        bloqueInstrucciones.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panelInstrucciones.add(bloqueInstrucciones, BorderLayout.CENTER);
        panel.add(panelInstrucciones, BorderLayout.NORTH);

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.TITULO_PROMPT_ANALISIS(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

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
        etiquetaAdvertencia.setForeground(new Color(139, 0, 0));
        panelSur.add(etiquetaAdvertencia, BorderLayout.SOUTH);

        panelEditor.add(panelSur, BorderLayout.SOUTH);

        panel.add(panelEditor, BorderLayout.CENTER);

        txtPrompt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContador();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContador();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContador();
            }
        });

        return panel;
    }

    private JPanel crearPanelAgentes() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel contenedor = new JPanel();
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setOpaque(false);

        JPanel panelFactoryDroid = new JPanel(new GridBagLayout());
        panelFactoryDroid.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelFactoryDroid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.Agentes.TITULO_FACTORY_DROID(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        chkAgenteFactoryDroidHabilitado = new JCheckBox(I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE());
        chkAgenteFactoryDroidHabilitado.setFont(EstilosUI.FUENTE_ESTANDAR);
        txtAgenteFactoryDroidBinario = new JTextField(30);

        txtAgenteFactoryDroidPrompt = new JTextArea(6, 40);
        txtAgenteFactoryDroidPrompt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtAgenteFactoryDroidPrompt.setLineWrap(true);
        txtAgenteFactoryDroidPrompt.setWrapStyleWord(true);
        JScrollPane scrollPromptAgente = new JScrollPane(txtAgenteFactoryDroidPrompt);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panelFactoryDroid.add(chkAgenteFactoryDroidHabilitado, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelFactoryDroid.add(new JLabel(I18nUI.Configuracion.Agentes.LABEL_RUTA_BINARIO()), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelFactoryDroid.add(txtAgenteFactoryDroidBinario, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panelFactoryDroid.add(new JLabel(I18nUI.Configuracion.Agentes.TITULO_PROMPT_AGENTE()), gbc);

        gbc.gridy = 3; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panelFactoryDroid.add(scrollPromptAgente, gbc);

        btnRestaurarPromptAgente = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgente.setToolTipText(I18nUI.Configuracion.TOOLTIP_RESTAURAR_PROMPT());
        btnRestaurarPromptAgente.addActionListener(e -> txtAgenteFactoryDroidPrompt.setText(ConfiguracionAPI.obtenerAgenteFactoryDroidPromptPorDefecto()));

        JPanel panelSurAgente = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelSurAgente.add(btnRestaurarPromptAgente);

        gbc.gridy = 4; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panelFactoryDroid.add(panelSurAgente, gbc);

        contenedor.add(panelFactoryDroid);
        panel.add(new JScrollPane(contenedor), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelAcercaDe() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel contenedor = new JPanel();
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setOpaque(false);

        JLabel titulo = new JLabel(I18nUI.Configuracion.TITULO_APP());
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(titulo);

        contenedor.add(Box.createVerticalStrut(6));

        JLabel subtitulo = new JLabel(I18nUI.Configuracion.SUBTITULO_APP());
        subtitulo.setFont(EstilosUI.FUENTE_NEGRITA);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(subtitulo);

        contenedor.add(Box.createVerticalStrut(14));

        JPanel panelDescripcion = new JPanel(new BorderLayout());
        panelDescripcion.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelDescripcion.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));
        panelDescripcion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.TITULO_RESUMEN(),
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JTextArea descripcion = new JTextArea();
        descripcion.setEditable(false);
        descripcion.setWrapStyleWord(true);
        descripcion.setLineWrap(true);
        descripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
        descripcion.setBackground(new Color(245, 250, 255));
        descripcion.setText(I18nUI.Configuracion.DESCRIPCION_APP());
        descripcion.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panelDescripcion.add(descripcion, BorderLayout.CENTER);
        contenedor.add(panelDescripcion);

        contenedor.add(Box.createVerticalStrut(12));

        JPanel panelAutor = new JPanel(new BorderLayout(0, 8));
        panelAutor.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelAutor.setMaximumSize(new Dimension(760, 140));
        panelAutor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Configuracion.TITULO_DESARROLLADO_POR(),
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel nombreAutor = new JLabel("Jaime Andrés Restrepo", SwingConstants.CENTER);
        nombreAutor.setFont(EstilosUI.FUENTE_NEGRITA);
        panelAutor.add(nombreAutor, BorderLayout.NORTH);

        JButton btnSitioWeb = new JButton(I18nUI.Configuracion.BOTON_SITIO_WEB());
        btnSitioWeb.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnSitioWeb.setToolTipText(I18nUI.Tooltips.Configuracion.SITIO_AUTOR());
        btnSitioWeb.addActionListener(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica")
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        I18nUI.Configuracion.MSG_URL(),
                        I18nUI.Configuracion.TITULO_ENLACE(),
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    DialogoConfiguracion.this,
                    I18nUI.Configuracion.MSG_ERROR_ABRIR_NAVEGADOR(ex.getMessage()),
                    I18nUI.Configuracion.TITULO_ERROR(),
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(btnSitioWeb);
        panelAutor.add(panelBoton, BorderLayout.SOUTH);
        contenedor.add(panelAutor);

        contenedor.add(Box.createVerticalStrut(10));

        JLabel lblVersion = new JLabel(I18nUI.Configuracion.VERSION());
        lblVersion.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblVersion.setForeground(new Color(128, 128, 128));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(lblVersion);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(contenedor, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        wrapper.add(Box.createGlue(), gbc);

        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }

    private void actualizarContador() {
        int longitud = txtPrompt.getText().length();
        lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_CARACTERES(longitud));

        if (!txtPrompt.getText().contains("{REQUEST}")) {
            lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_FALTA_REQUEST(longitud));
            lblContadorPrompt.setForeground(Color.RED);
        } else {
            lblContadorPrompt.setForeground(Color.BLACK);
        }
    }

    private void restaurarPromptPorDefecto() {
        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_PROMPT(),
            I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
            actualizarContador();
        }
    }

    private void cargarConfiguracionActual() {
        comboIdioma.setSelectedItem(IdiomaUI.desdeCodigo(config.obtenerIdiomaUi()));

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

        chkAgenteFactoryDroidHabilitado.setSelected(config.agenteFactoryDroidHabilitado());
        txtAgenteFactoryDroidBinario.setText(config.obtenerAgenteFactoryDroidBinario());
        txtAgenteFactoryDroidPrompt.setText(config.obtenerAgenteFactoryDroidPrompt());

        if (config.esPromptModificado()) {
            txtPrompt.setText(config.obtenerPromptConfigurable());
        } else {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
        }
        actualizarContador();
        alCambiarProveedor();
    }

    private void guardarConfiguracion() {
        if (!txtPrompt.getText().contains("{REQUEST}")) {
            JOptionPane.showMessageDialog(this,
                    I18nUI.Configuracion.MSG_ERROR_PROMPT_SIN_REQUEST(),
                    I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null || proveedorSeleccionado.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR(),
                I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        ConfiguracionAPI configTemporal = config.crearSnapshot();
        IdiomaUI idiomaSeleccionado = (IdiomaUI) comboIdioma.getSelectedItem();
        configTemporal.establecerIdiomaUi(idiomaSeleccionado != null ? idiomaSeleccionado.codigo() : IdiomaUI.porDefecto().codigo());
        configTemporal.establecerProveedorAI(proveedorSeleccionado);
        String proveedorConfigurado = configTemporal.obtenerProveedorAI();

        String claveApi = new String(txtClave.getPassword());
        configTemporal.establecerApiKeyParaProveedor(proveedorConfigurado, claveApi);

        String modeloSeleccionado = obtenerModeloSeleccionado();
        configTemporal.establecerModeloParaProveedor(proveedorConfigurado, modeloSeleccionado);

        String urlBase = txtUrl.getText().trim();
        configTemporal.establecerUrlBaseParaProveedor(proveedorConfigurado, urlBase);

        configTemporal.establecerDetallado(chkDetallado.isSelected());
        configTemporal.establecerIgnorarErroresSSL(chkIgnorarSSL.isSelected());
        configTemporal.establecerSoloProxy(chkSoloProxy.isSelected());

        configTemporal.establecerAgenteFactoryDroidHabilitado(chkAgenteFactoryDroidHabilitado.isSelected());
        configTemporal.establecerAgenteFactoryDroidBinario(txtAgenteFactoryDroidBinario.getText().trim());
        configTemporal.establecerAgenteFactoryDroidPrompt(txtAgenteFactoryDroidPrompt.getText());

        String promptActual = txtPrompt.getText();
        String promptPorDefecto = ConfiguracionAPI.obtenerPromptPorDefecto();

        if (promptActual.equals(promptPorDefecto)) {
            configTemporal.establecerPromptModificado(false);
            configTemporal.establecerPromptConfigurable(promptPorDefecto);
        } else {
            configTemporal.establecerPromptModificado(true);
            configTemporal.establecerPromptConfigurable(promptActual);
        }

        Integer retraso = parsearEntero(txtRetraso.getText());
        Integer maximoConcurrente = parsearEntero(txtMaximoConcurrente.getText());
        Integer maximoHallazgos = parsearEntero(txtMaximoHallazgosTabla.getText());
        Integer timeoutModelo = parsearEntero(txtTimeoutModelo.getText());
        if (retraso == null || maximoConcurrente == null || maximoHallazgos == null || timeoutModelo == null) {
            JOptionPane.showMessageDialog(this,
                I18nUI.Configuracion.MSG_ERROR_FORMATO_NUMERO(),
                I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        configTemporal.establecerRetrasoSegundos(retraso);
        configTemporal.establecerMaximoConcurrente(maximoConcurrente);
        configTemporal.establecerMaximoHallazgosTabla(maximoHallazgos);
        configTemporal.establecerTiempoEsperaParaModelo(proveedorConfigurado, modeloSeleccionado, timeoutModelo);

        Integer maxTokens = parsearEntero(txtMaxTokens.getText());
        if (maxTokens != null) {
            configTemporal.establecerMaxTokensParaProveedor(proveedorConfigurado, maxTokens);
        } else {
            ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorConfigurado);
            if (configProveedor != null) {
                configTemporal.establecerMaxTokensParaProveedor(
                    proveedorConfigurado,
                    configProveedor.obtenerMaxTokensPorDefecto()
                );
            }
        }

        java.util.Map<String, String> erroresValidacion = configTemporal.validar();
        if (!erroresValidacion.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                construirMensajeErroresValidacion(erroresValidacion),
                I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder errorMsg = new StringBuilder();
        boolean guardado = gestorConfig.guardarConfiguracion(configTemporal, errorMsg);

        if (!guardado) {
            JOptionPane.showMessageDialog(this,
                I18nUI.Configuracion.MSG_ERROR_GUARDAR_CONFIG(
                    errorMsg.toString(),
                    gestorConfig.obtenerRutaConfiguracion()
                ),
                I18nUI.Configuracion.TITULO_ERROR_GUARDAR(),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        config.aplicarDesde(configTemporal);
        if (alGuardar != null) {
            alGuardar.run();
        }
        setVisible(false);
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

    private void alCambiarProveedor() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;
        ProveedorAI.ConfiguracionProveedor configProveedor =
            ProveedorAI.obtenerProveedor(proveedorSeleccionado);

        if (configProveedor == null) {
            return;
        }

        String urlGuardada = config.obtenerUrlBaseGuardadaParaProveedor(proveedorSeleccionado);
        boolean tieneUrlGuardada = urlGuardada != null && !urlGuardada.trim().isEmpty();
        if (!tieneUrlGuardada && ProveedorAI.PROVEEDOR_CUSTOM.equals(proveedorSeleccionado)) {
            IdiomaUI idiomaSeleccionado = (IdiomaUI) comboIdioma.getSelectedItem();
            String codigoIdioma = idiomaSeleccionado != null ? idiomaSeleccionado.codigo() : config.obtenerIdiomaUi();
            txtUrl.setText(ProveedorAI.obtenerUrlApiPorDefecto(proveedorSeleccionado, codigoIdioma));
        } else {
            String urlBase = tieneUrlGuardada
                ? urlGuardada
                : config.obtenerUrlBaseParaProveedor(proveedorSeleccionado);
            txtUrl.setText(urlBase != null ? urlBase : configProveedor.obtenerUrlApi());
        }

        String apiKeyGuardada = config.obtenerApiKeyParaProveedor(proveedorSeleccionado);
        txtClave.setText(apiKeyGuardada != null ? apiKeyGuardada : "");

        Integer maxTokensGuardado = config.obtenerMaxTokensConfiguradoParaProveedor(proveedorSeleccionado);
        int maxTokensMostrar = (maxTokensGuardado != null && maxTokensGuardado > 0)
            ? maxTokensGuardado
            : configProveedor.obtenerMaxTokensPorDefecto();
        txtMaxTokens.setText(String.valueOf(maxTokensMostrar));

        List<String> modelosProveedor = configProveedor.obtenerModelosDisponibles();

        String modeloGuardado = config.obtenerModeloParaProveedor(proveedorSeleccionado);
        if (modeloGuardado == null || modeloGuardado.trim().isEmpty()) {
            modeloGuardado = configProveedor.obtenerModeloPorDefecto();
        }
        cargarModelosEnCombo(modelosProveedor, modeloGuardado);
        actualizarTimeoutModeloSeleccionado();
    }

    private String obtenerModeloSeleccionado() {
        String modeloActual = (String) comboModelo.getSelectedItem();
        if (modeloActual == null) {
            return "";
        }
        if (!MODELO_CUSTOM.equals(modeloActual)) {
            return normalizarModeloSeleccionado(modeloActual);
        }
        Object editorValue = comboModelo.getEditor().getItem();
        return editorValue != null ? normalizarModeloSeleccionado(editorValue.toString()) : "";
    }

    private void refrescarModelosDesdeAPI() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;

        btnRefrescarModelos.setEnabled(false);
        btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_ACTUALIZANDO_MODELOS());

        SwingWorker<java.util.List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<String> doInBackground() {
                return obtenerModelosDesdeAPI(proveedorSeleccionado);
            }

            @Override
            protected void done() {
                try {
                    java.util.List<String> modelos = get();
                    if (modelos == null || modelos.isEmpty()) {
                        throw new IllegalStateException(I18nUI.Configuracion.ERROR_API_SIN_MODELOS(proveedorSeleccionado));
                    }
                    cargarModelosEnCombo(modelos, modelos.get(0));
                    JOptionPane.showMessageDialog(DialogoConfiguracion.this,
                        I18nUI.Configuracion.MSG_MODELOS_ACTUALIZADOS(modelos.size(), proveedorSeleccionado),
                        I18nUI.Configuracion.TITULO_MODELOS_ACTUALIZADOS(),
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    String detalleError = extraerMensajeError(e);
                    JOptionPane.showMessageDialog(DialogoConfiguracion.this,
                        I18nUI.Configuracion.MSG_ERROR_PROCESAR_MODELOS(detalleError),
                        I18nUI.Configuracion.TITULO_ERROR(),
                        JOptionPane.ERROR_MESSAGE);
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
        comboModelo.addItem(MODELO_CUSTOM);

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

        comboModelo.setSelectedItem(MODELO_CUSTOM);
        comboModelo.getEditor().setItem(preferidoNormalizado);
        actualizarTimeoutModeloSeleccionado();
    }

    private void actualizarTimeoutModeloSeleccionado() {
        if (txtTimeoutModelo == null) {
            return;
        }
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null || proveedorSeleccionado.trim().isEmpty()) {
            txtTimeoutModelo.setText(String.valueOf(config.obtenerTiempoEsperaAI()));
            return;
        }

        String modelo = obtenerModeloSeleccionado();
        if (modelo == null || modelo.trim().isEmpty()) {
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
                    cliente
                );
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_GEMINI() + e.getMessage(), e);
            }
        }
        if ("Ollama".equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOllama(
                    txtUrl.getText().trim(),
                    cliente
                );
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_OLLAMA() + e.getMessage(), e);
            }
        }
        if ("OpenAI".equals(proveedor) || "Z.ai".equals(proveedor) || "minimax".equals(proveedor) || ProveedorAI.PROVEEDOR_CUSTOM.equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOpenAI(
                    txtUrl.getText().trim(),
                    new String(txtClave.getPassword()).trim(),
                    cliente
                );
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
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return I18nUI.Configuracion.SIN_DETALLE_ERROR();
        }
        return mensaje.trim();
    }

    private void probarConexion() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) {
            JOptionPane.showMessageDialog(
                this,
                I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR(),
                I18nUI.Configuracion.TITULO_VALIDACION(),
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String urlApi = txtUrl.getText().trim();
        if (urlApi.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                I18nUI.Configuracion.MSG_URL_VACIA(),
                I18nUI.Configuracion.TITULO_VALIDACION(),
                JOptionPane.WARNING_MESSAGE
            );
            txtUrl.requestFocus();
            return;
        }

        String claveApi = new String(txtClave.getPassword()).trim();

        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorSeleccionado);
        if (configProveedor != null && configProveedor.requiereClaveApi()) {
            if (claveApi.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    I18nUI.Configuracion.MSG_API_KEY_VACIA(proveedorSeleccionado),
                    I18nUI.Configuracion.TITULO_VALIDACION(),
                    JOptionPane.WARNING_MESSAGE
                );
                txtClave.requestFocus();
                return;
            }
        }

        String modeloAUsar = obtenerModeloSeleccionado();
        if (modeloAUsar.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                I18nUI.Configuracion.MSG_SELECCIONA_MODELO(),
                I18nUI.Configuracion.TITULO_VALIDACION(),
                JOptionPane.WARNING_MESSAGE
            );
            comboModelo.requestFocus();
            return;
        }

        Integer timeoutModelo = parsearEntero(txtTimeoutModelo.getText());
        if (timeoutModelo == null) {
            JOptionPane.showMessageDialog(
                this,
                I18nUI.Configuracion.MSG_ERROR_FORMATO_NUMERO(),
                I18nUI.Configuracion.TITULO_VALIDACION(),
                JOptionPane.WARNING_MESSAGE
            );
            txtTimeoutModelo.requestFocus();
            return;
        }

        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBANDO());

        final String modeloFinal = modeloAUsar;
        final int timeoutModeloFinal = timeoutModelo;
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
                            modeloFinal
                        )
                    );
                    configTemp.establecerTiempoEsperaParaModelo(proveedorSeleccionado, modeloFinal, timeoutModeloFinal);

                    ProbadorConexionAI probador = new ProbadorConexionAI(configTemp);
                    return probador.probarConexion();
                } catch (Exception e) {
                    return new ProbadorConexionAI.ResultadoPrueba(
                        false,
                        I18nUI.Conexion.ERROR_PRUEBA_CONEXION(e.getMessage()),
                        null
                    );
                }
            }

            @Override
            protected void done() {
                try {
                    ProbadorConexionAI.ResultadoPrueba resultado = get();
                    int tipoMensaje = resultado.exito ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        resultado.mensaje,
                        resultado.exito ? I18nUI.Configuracion.TITULO_CONEXION_EXITOSA() : I18nUI.Configuracion.TITULO_ERROR_CONEXION(),
                        tipoMensaje
                    );
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        I18nUI.Configuracion.MSG_ERROR_PRUEBA_INESPERADO(e.getMessage()),
                        I18nUI.Configuracion.TITULO_ERROR(),
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    btnProbarConexion.setEnabled(true);
                    btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
                }
            }
        };

        worker.execute();
    }
}
