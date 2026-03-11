package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ConfigDialogView {
    private static final int FILAS_PROMPT_AGENTE = 6;
    private static final int ANCHO_SCROLL_PROMPT_AGENTE = 300;
    private static final int ALTO_SCROLL_PROMPT_AGENTE = 180;

    // Constantes de dimensiones del diálogo (compartidas con DialogoConfiguracion)
    static final int ANCHO_DIALOGO = 800;
    static final int ALTO_DIALOGO = 720;

    private JTabbedPane tabbedPane;
    private JPanel panelBotones;

    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtRetraso;
    private JTextField txtMaximoConcurrente;
    private JTextField txtMaximoHallazgosTabla;
    private JTextField txtMaximoTareas;
    private JTextField txtMaxTokens;
    private JTextField txtTimeoutModelo;
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
    private JComboBox<String> comboFuenteEstandar;
    private JSpinner spinnerTamanioEstandar;
    private JComboBox<String> comboFuenteMono;
    private JSpinner spinnerTamanioMono;
    private JButton btnRestaurarFuentes;
    private JButton btnGuardar;
    private JButton btnCerrar;
    private JButton btnSitioWeb;

    public ConfigDialogView() {
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        tabbedPane = new JTabbedPane();

        inicializarCamposConfiguracion();

        panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        btnGuardar = new JButton(I18nUI.Configuracion.BOTON_GUARDAR());
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.setToolTipText(I18nUI.Tooltips.Configuracion.GUARDAR());

        btnCerrar = new JButton(I18nUI.Configuracion.BOTON_CERRAR());
        btnCerrar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnCerrar.setToolTipText(I18nUI.Tooltips.Configuracion.CERRAR());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCerrar);
    }

    private void inicializarCamposConfiguracion() {
        txtRetraso = crearCampoTexto();
        txtRetraso.setToolTipText(I18nUI.Tooltips.Configuracion.RETRASO());

        txtMaximoConcurrente = crearCampoTexto();
        txtMaximoConcurrente.setToolTipText(I18nUI.Tooltips.Configuracion.MAXIMO_CONCURRENTE());

        txtUrl = crearCampoTexto(30);
        txtUrl.setToolTipText(I18nUI.Tooltips.Configuracion.URL_API());

        txtClave = new JPasswordField(30);
        txtClave.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtClave.setToolTipText(I18nUI.Tooltips.Configuracion.CLAVE_API());

        txtMaxTokens = crearCampoTexto();
        txtMaxTokens.setToolTipText(I18nUI.Tooltips.Configuracion.MAX_TOKENS());

        txtTimeoutModelo = crearCampoTexto();
        txtTimeoutModelo.setToolTipText(I18nUI.Tooltips.Configuracion.TIMEOUT_MODELO());

        txtMaximoHallazgosTabla = crearCampoTexto();
        txtMaximoHallazgosTabla.setToolTipText(
                I18nUI.Tooltips.Configuracion.MAXIMO_HALLAZGOS() + " (" +
                        ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA + "-" +
                        ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA + ")");

        txtMaximoTareas = crearCampoTexto();
        txtMaximoTareas.setToolTipText(
                I18nUI.Tooltips.Configuracion.MAXIMO_TAREAS() + " (" +
                        ConfiguracionAPI.MINIMO_TAREAS_TABLA + "-" +
                        ConfiguracionAPI.MAXIMO_TAREAS_TABLA + ")");

        chkDetallado = crearCheckBox(I18nUI.Configuracion.CHECK_DETALLADO());
        chkDetallado.setToolTipText(I18nUI.Tooltips.Configuracion.DETALLADO());

        chkIgnorarSSL = crearCheckBox(I18nUI.Configuracion.LABEL_IGNORAR_SSL());
        chkIgnorarSSL.setToolTipText(I18nUI.Tooltips.Configuracion.IGNORAR_SSL());

        chkSoloProxy = crearCheckBox(I18nUI.Configuracion.LABEL_SOLO_PROXY());
        chkSoloProxy.setToolTipText(I18nUI.Tooltips.Configuracion.SOLO_PROXY());

        chkAlertasHabilitadas = crearCheckBox(I18nUI.Configuracion.LABEL_HABILITAR_ALERTAS());
        chkAlertasHabilitadas.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_ALERTAS());

        chkPersistirBusqueda = crearCheckBox(I18nUI.Configuracion.CHECK_PERSISTIR_FILTRO_BUSQUEDA());
        chkPersistirBusqueda.setToolTipText(I18nUI.Tooltips.Configuracion.PERSISTIR_FILTRO_BUSQUEDA());

        chkPersistirSeveridad = crearCheckBox(I18nUI.Configuracion.CHECK_PERSISTIR_FILTRO_SEVERIDAD());
        chkPersistirSeveridad.setToolTipText(I18nUI.Tooltips.Configuracion.PERSISTIR_FILTRO_SEVERIDAD());

        comboIdioma = new JComboBox<>(IdiomaUI.values());
        comboIdioma.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboIdioma.setToolTipText(I18nUI.Tooltips.Configuracion.IDIOMA());

        comboProveedor = new JComboBox<>(ProveedorAI.obtenerNombresProveedores().toArray(new String[0]));
        comboProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.PROVEEDOR());

        comboModelo = new JComboBox<>();
        comboModelo.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboModelo.setEditable(true);
        comboModelo.setToolTipText(I18nUI.Tooltips.Configuracion.MODELO());

        btnRefrescarModelos = new JButton(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
        btnRefrescarModelos.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRefrescarModelos.setToolTipText(I18nUI.Tooltips.Configuracion.CARGAR_MODELOS());

        btnProbarConexion = new JButton(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
        btnProbarConexion.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnProbarConexion.setToolTipText(I18nUI.Tooltips.Configuracion.PROBAR_CONEXION());

        String[] fuentes = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        comboFuenteEstandar = new JComboBox<>(fuentes);
        comboFuenteEstandar.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboFuenteEstandar.setToolTipText(I18nUI.Tooltips.Configuracion.FUENTE_ESTANDAR());
        comboFuenteMono = new JComboBox<>(fuentes);
        comboFuenteMono.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboFuenteMono.setToolTipText(I18nUI.Tooltips.Configuracion.FUENTE_MONO());

        spinnerTamanioEstandar = new JSpinner(new SpinnerNumberModel(11, 6, 72, 1));
        spinnerTamanioEstandar.setToolTipText(I18nUI.Tooltips.Configuracion.TAMANIO_ESTANDAR());
        spinnerTamanioMono = new JSpinner(new SpinnerNumberModel(12, 6, 72, 1));
        spinnerTamanioMono.setToolTipText(I18nUI.Tooltips.Configuracion.TAMANIO_MONO());

        comboAgente = new JComboBox<>(AgenteTipo.codigosDisponibles());
        comboAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboAgente.setToolTipText(I18nUI.Tooltips.Configuracion.SELECCIONAR_AGENTE());

        txtAgenteBinario = crearCampoTexto(30);
        txtAgenteBinario.setToolTipText(I18nUI.Tooltips.Configuracion.BINARIO_AGENTE());

        chkAgenteHabilitado = crearCheckBox(I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE());
        chkAgenteHabilitado.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkAgenteHabilitado.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_AGENTE());

        btnBuscarActualizaciones = new JButton(I18nUI.Configuracion.BOTON_BUSCAR_ACTUALIZACIONES());
        btnBuscarActualizaciones.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnBuscarActualizaciones.setToolTipText(I18nUI.Tooltips.Configuracion.CHECK_ACTUALIZACIONES());

        btnSitioWeb = new JButton(I18nUI.Configuracion.BOTON_SITIO_WEB());
        btnSitioWeb.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnSitioWeb.setToolTipText(I18nUI.Tooltips.Configuracion.SITIO_AUTOR());

        modeloListaDisponibles = new DefaultListModel<>();
        listaProveedoresDisponibles = new JList<>(modeloListaDisponibles);
        listaProveedoresDisponibles.setFont(EstilosUI.FUENTE_ESTANDAR);
        listaProveedoresDisponibles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProveedoresDisponibles.setVisibleRowCount(5);
        listaProveedoresDisponibles.setToolTipText(I18nUI.Tooltips.Configuracion.LISTA_PROVEEDORES_DISPONIBLES());

        modeloListaSeleccionados = new DefaultListModel<>();
        listaProveedoresSeleccionados = new JList<>(modeloListaSeleccionados);
        listaProveedoresSeleccionados.setFont(EstilosUI.FUENTE_ESTANDAR);
        listaProveedoresSeleccionados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProveedoresSeleccionados.setVisibleRowCount(5);
        listaProveedoresSeleccionados.setToolTipText(I18nUI.Tooltips.Configuracion.LISTA_PROVEEDORES_SELECCIONADOS());

        chkHabilitarMultiProveedor = new JCheckBox(I18nUI.Configuracion.LABEL_HABILITAR_MULTI_PROVEEDOR());
        chkHabilitarMultiProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkHabilitarMultiProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.HABILITAR_MULTI_PROVEEDOR());

        btnAgregarProveedor = new JButton("→");
        btnAgregarProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.MULTI_PROVEEDOR_AGREGAR());

        btnQuitarProveedor = new JButton("←");
        btnQuitarProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.MULTI_PROVEEDOR_QUITAR());

        btnSubirProveedor = new JButton("↑");
        btnSubirProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnSubirProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.MULTI_PROVEEDOR_SUBIR());

        btnBajarProveedor = new JButton("↓");
        btnBajarProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnBajarProveedor.setToolTipText(I18nUI.Tooltips.Configuracion.MULTI_PROVEEDOR_BAJAR());

        lblEstadoMultiProveedor = new JLabel(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
        lblEstadoMultiProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblEstadoMultiProveedor.setForeground(EstilosUI.COLOR_INFO);

        btnRestaurarFuentes = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_FUENTES());
        btnRestaurarFuentes.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarFuentes.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_FUENTES());
    }

    private JTextField crearCampoTexto() {
        return crearCampoTexto(10);
    }

    private JTextField crearCampoTexto(int columnas) {
        JTextField campo = new JTextField(columnas);
        campo.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        return campo;
    }

    private JCheckBox crearCheckBox(String texto) {
        JCheckBox checkBox = new JCheckBox(texto);
        checkBox.setFont(EstilosUI.FUENTE_ESTANDAR);
        return checkBox;
    }

    public JPanel crearTabProveedor() {
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
        JLabel lblInfoTokens = new JLabel(I18nUI.Configuracion.ICONO_INFO_TOKENS());
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
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(chkHabilitarMultiProveedor, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(16, 8, 8, 8);
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.insets = new Insets(8, 8, 8, 8);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDORES_DISPONIBLES()), gbc);

        gbc.gridx = 2;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_PROVEEDORES_SELECCIONADOS()), gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollDisponibles = new JScrollPane(listaProveedoresDisponibles);
        scrollDisponibles.setPreferredSize(new Dimension(200, 120));
        panel.add(scrollDisponibles, gbc);

        gbc.gridx = 1;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panelBotonesCentro = new JPanel(new GridLayout(4, 1, 5, 5));
        panelBotonesCentro.add(btnAgregarProveedor);
        panelBotonesCentro.add(btnQuitarProveedor);
        panelBotonesCentro.add(Box.createVerticalStrut(10));
        panel.add(panelBotonesCentro, gbc);

        gbc.gridx = 2;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.gridheight = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

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
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panelBotonesReordenar = new JPanel(new GridLayout(1, 2, 5, 0));
        panelBotonesReordenar.add(btnSubirProveedor);
        panelBotonesReordenar.add(btnBajarProveedor);
        panel.add(panelBotonesReordenar, gbc);

        fila++;

        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(lblEstadoMultiProveedor, gbc);

        return panel;
    }

    public JPanel crearTabAjustesUsuario() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        contenido.setOpaque(false);

        JPanel panelPreferencias = crearPanelPreferenciasUsuario();
        panelPreferencias.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPreferencias.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelPreferencias.getPreferredSize().height));
        contenido.add(panelPreferencias);
        contenido.add(Box.createVerticalStrut(15));

        JPanel panelLimitesSeguridad = crearPanelLimitesSeguridad();
        panelLimitesSeguridad.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelLimitesSeguridad
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, panelLimitesSeguridad.getPreferredSize().height));
        contenido.add(panelLimitesSeguridad);
        contenido.add(Box.createVerticalStrut(15));

        JPanel panelApariencia = crearPanelConfiguracionApariencia();
        panelApariencia.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelApariencia.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelApariencia.getPreferredSize().height));
        contenido.add(panelApariencia);

        root.add(contenido, BorderLayout.CENTER);
        return root;
    }

    private JPanel crearPanelPreferenciasUsuario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_PREFERENCIAS_USUARIO(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_IDIOMA()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(comboIdioma, gbc);

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
        panel.add(panelOpcionesPreferencias, gbc);

        return panel;
    }

    private JPanel crearPanelLimitesSeguridad() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Configuracion.TITULO_LIMITES_SEGURIDAD(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MAX_HALLAZGOS_TABLA()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtMaximoHallazgosTabla, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_MAXIMO_TAREAS()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(txtMaximoTareas, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_ALERTAS()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(chkAlertasHabilitadas, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_SEGURIDAD_SSL()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(chkIgnorarSSL, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUI.Configuracion.LABEL_FILTRO_HERRAMIENTAS()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(chkSoloProxy, gbc);

        return panel;
    }

    private JPanel crearPanelConfiguracionApariencia() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Configuracion.TITULO_APARIENCIA(), 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

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

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 12, 8, 12);

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRestaurarFuentes);
        panel.add(panelBoton, gbc);

        return panel;
    }

    public JPanel crearPanelPrompt() {
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
        txtPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_EDITOR());

        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelEditor.add(scrollPrompt, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 6));
        panelSur.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panelAcciones.setOpaque(false);

        btnRestaurarPrompt = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPrompt.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPrompt.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT());
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

        return panel;
    }

    public JPanel crearPanelAgentes() {
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

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelAgenteGeneral.add(comboAgente, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panelAgenteGeneral.add(new JLabel(I18nUI.Configuracion.Agentes.LABEL_RUTA_BINARIO()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelAgenteGeneral.add(txtAgenteBinario, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        panelAgenteGeneral.add(chkAgenteHabilitado, gbc);

        JPanel panelPrompts = new JPanel(new GridLayout(1, 2, 10, 0));
        panelPrompts.setOpaque(false);

        txtAgentePromptInicial = new JTextArea();
        configurarAreaPromptAgente(txtAgentePromptInicial);
        txtAgentePromptInicial.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_INICIAL_AGENTE());

        btnRestaurarPromptAgenteInicial = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgenteInicial.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgenteInicial.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT_INICIAL_AGENTE());

        txtAgentePrompt = new JTextArea();
        configurarAreaPromptAgente(txtAgentePrompt);
        txtAgentePrompt.setToolTipText(I18nUI.Tooltips.Configuracion.PROMPT_AGENTE());

        btnRestaurarPromptAgente = new JButton(I18nUI.Configuracion.BOTON_RESTAURAR_PROMPT());
        btnRestaurarPromptAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPromptAgente.setToolTipText(I18nUI.Tooltips.Configuracion.RESTAURAR_PROMPT_AGENTE());

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

        if (com.burpia.util.Normalizador.noEsVacio(descripcion)) {
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

    public JPanel crearPanelAcercaDe() {
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
        var resourceUrl = getClass().getResource("/logo.png");
        if (resourceUrl != null) {
            ImageIcon icon = new ImageIcon(resourceUrl);
            Image img = icon.getImage();
            Image newimg = img.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH);
            lblIcono = new JLabel(new ImageIcon(newimg));
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

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        content.setOpaque(false);

        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_RESUMEN(com.burpia.util.VersionBurpIA.obtenerVersionActual()),
                I18nUI.Configuracion.DESCRIPCION_APP(),
                null));
        content.add(Box.createVerticalStrut(20));

        JPanel panelBotonWeb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panelBotonWeb.setOpaque(false);
        panelBotonWeb.add(btnSitioWeb);

        content.add(crearSeccionAcerca(
                I18nUI.Configuracion.TITULO_DESARROLLADO_POR(),
                "",
                panelBotonWeb));
        content.add(Box.createVerticalStrut(20));

        JPanel panelBotonUpdate = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panelBotonUpdate.setOpaque(false);
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

        if (com.burpia.util.Normalizador.noEsVacio(contenido)) {
            JTextArea txt = new JTextArea(contenido);
            txt.setEditable(false);
            txt.setLineWrap(true);
            txt.setWrapStyleWord(true);
            txt.setOpaque(false);
            txt.setFont(EstilosUI.FUENTE_ESTANDAR);
            txt.setForeground(EstilosUI.colorTextoSecundario(p.getBackground()));
            cPanel.add(txt, BorderLayout.CENTER);
        }

        if (extra != null) {
            cPanel.add(extra, BorderLayout.SOUTH);
        }

        p.add(cPanel, BorderLayout.CENTER);
        return p;
    }

    public JComponent crearDialogoPrincipal() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setPreferredSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));
        panelPrincipal.setMinimumSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));

        tabbedPane.addTab(I18nUI.Configuracion.TAB_AJUSTES_USUARIO(), crearTabAjustesUsuario());
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROVEEDOR(), crearTabProveedor());
        tabbedPane.addTab(I18nUI.Configuracion.TAB_PROMPT(), crearPanelPrompt());
        tabbedPane.addTab(I18nUI.Configuracion.TAB_AGENTES(), crearPanelAgentes());
        tabbedPane.addTab(I18nUI.Configuracion.TAB_ACERCA(), crearPanelAcercaDe());
        aplicarTooltipsPestanias();

        panelPrincipal.add(tabbedPane, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        return panelPrincipal;
    }

    private void aplicarTooltipsPestanias() {
        if (tabbedPane.getTabCount() < 5) {
            return;
        }
        tabbedPane.setToolTipTextAt(0, I18nUI.Tooltips.Configuracion.TAB_AJUSTES_USUARIO());
        tabbedPane.setToolTipTextAt(1, I18nUI.Tooltips.Configuracion.TAB_PROVEEDOR());
        tabbedPane.setToolTipTextAt(2, I18nUI.Tooltips.Configuracion.TAB_PROMPT());
        tabbedPane.setToolTipTextAt(3, I18nUI.Tooltips.Configuracion.TAB_AGENTES());
        tabbedPane.setToolTipTextAt(4, I18nUI.Tooltips.Configuracion.TAB_ACERCA());
    }

    public JTabbedPane obtenerTabbedPane() {
        return tabbedPane;
    }

    public JPanel obtenerPanelBotones() {
        return panelBotones;
    }

    public JTextField obtenerTxtUrl() {
        return txtUrl;
    }

    public JPasswordField obtenerTxtClave() {
        return txtClave;
    }

    public JTextField obtenerTxtRetraso() {
        return txtRetraso;
    }

    public JTextField obtenerTxtMaximoConcurrente() {
        return txtMaximoConcurrente;
    }

    public JTextField obtenerTxtMaximoHallazgosTabla() {
        return txtMaximoHallazgosTabla;
    }

    public JTextField obtenerTxtMaximoTareas() {
        return txtMaximoTareas;
    }

    public JCheckBox obtenerChkDetallado() {
        return chkDetallado;
    }

    public JCheckBox obtenerChkIgnorarSSL() {
        return chkIgnorarSSL;
    }

    public JCheckBox obtenerChkSoloProxy() {
        return chkSoloProxy;
    }

    public JCheckBox obtenerChkAlertasHabilitadas() {
        return chkAlertasHabilitadas;
    }

    public JCheckBox obtenerChkPersistirBusqueda() {
        return chkPersistirBusqueda;
    }

    public JCheckBox obtenerChkPersistirSeveridad() {
        return chkPersistirSeveridad;
    }

    public JTextArea obtenerTxtPrompt() {
        return txtPrompt;
    }

    public JButton obtenerBtnRestaurarPrompt() {
        return btnRestaurarPrompt;
    }

    public JLabel obtenerLblContadorPrompt() {
        return lblContadorPrompt;
    }

    public JComboBox<String> obtenerComboAgente() {
        return comboAgente;
    }

    public JCheckBox obtenerChkAgenteHabilitado() {
        return chkAgenteHabilitado;
    }

    public JTextField obtenerTxtAgenteBinario() {
        return txtAgenteBinario;
    }

    public JTextArea obtenerTxtAgentePromptInicial() {
        return txtAgentePromptInicial;
    }

    public JTextArea obtenerTxtAgentePrompt() {
        return txtAgentePrompt;
    }

    public JButton obtenerBtnRestaurarPromptAgenteInicial() {
        return btnRestaurarPromptAgenteInicial;
    }

    public JButton obtenerBtnRestaurarPromptAgente() {
        return btnRestaurarPromptAgente;
    }

    public JComboBox<String> obtenerComboProveedor() {
        return comboProveedor;
    }

    public JComboBox<String> obtenerComboModelo() {
        return comboModelo;
    }

    public JComboBox<IdiomaUI> obtenerComboIdioma() {
        return comboIdioma;
    }

    public JButton obtenerBtnRefrescarModelos() {
        return btnRefrescarModelos;
    }

    public JButton obtenerBtnProbarConexion() {
        return btnProbarConexion;
    }

    public JButton obtenerBtnBuscarActualizaciones() {
        return btnBuscarActualizaciones;
    }

    public JCheckBox obtenerChkHabilitarMultiProveedor() {
        return chkHabilitarMultiProveedor;
    }

    public JList<String> obtenerListaProveedoresDisponibles() {
        return listaProveedoresDisponibles;
    }

    public JList<String> obtenerListaProveedoresSeleccionados() {
        return listaProveedoresSeleccionados;
    }

    public DefaultListModel<String> obtenerModeloListaDisponibles() {
        return modeloListaDisponibles;
    }

    public DefaultListModel<String> obtenerModeloListaSeleccionados() {
        return modeloListaSeleccionados;
    }

    public JButton obtenerBtnAgregarProveedor() {
        return btnAgregarProveedor;
    }

    public JButton obtenerBtnQuitarProveedor() {
        return btnQuitarProveedor;
    }

    public JButton obtenerBtnSubirProveedor() {
        return btnSubirProveedor;
    }

    public JButton obtenerBtnBajarProveedor() {
        return btnBajarProveedor;
    }

    public JLabel obtenerLblEstadoMultiProveedor() {
        return lblEstadoMultiProveedor;
    }

    public JTextField obtenerTxtMaxTokens() {
        return txtMaxTokens;
    }

    public JTextField obtenerTxtTimeoutModelo() {
        return txtTimeoutModelo;
    }

    public JComboBox<String> obtenerComboFuenteEstandar() {
        return comboFuenteEstandar;
    }

    public JSpinner obtenerSpinnerTamanioEstandar() {
        return spinnerTamanioEstandar;
    }

    public JComboBox<String> obtenerComboFuenteMono() {
        return comboFuenteMono;
    }

    public JSpinner obtenerSpinnerTamanioMono() {
        return spinnerTamanioMono;
    }

    public JButton obtenerBtnRestaurarFuentes() {
        return btnRestaurarFuentes;
    }

    public JButton obtenerBtnGuardar() {
        return btnGuardar;
    }

    public JButton obtenerBtnCerrar() {
        return btnCerrar;
    }

    public JButton obtenerBtnSitioWeb() {
        return btnSitioWeb;
    }
}
