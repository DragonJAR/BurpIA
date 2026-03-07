package com.burpia.ui;

import com.burpia.config.*;
import com.burpia.i18n.I18nUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Diálogo de configuración principal de BurpIA.
 * <p>
 * Esta clase ahora actúa como un orquestador principal,
 * delegando la lógica específica a clases helper especializadas:
 * - ConfigDialogView: Creación y manejo de componentes UI
 * - ConfigDialogController: Manejo de eventos y coordinación
 * - ConfigPersistenceManager: Persistencia de configuración
 * - ProviderConfigManager: Gestión de proveedores AI
 * - ConfigValidator: Validación de datos
 * - ConnectionTester: Pruebas de conexión
 * - DialogStateManager: Gestión de estado del diálogo
 * </p>
 * 
 * @author BurpIA Team
 * @since 1.0.2
 */
public class DialogoConfiguracion extends JDialog {
    
    private static final int ANCHO_DIALOGO = 800;
    private static final int ALTO_DIALOGO = 720;
    
    // Componentes principales
    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final Runnable alGuardar;
    
    // Clases helper
    private ConfigDialogView view;
    private ConfigDialogController controller;
    
    // ===== CAMPOS DELEGADOS PARA COMPATIBILIDAD CON TESTS =====
    // Estos campos permiten que los tests accedan a los componentes por reflexión
    
    private JComboBox<String> comboProveedor;
    private JComboBox<String> comboModelo;
    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtMaxTokens;
    private JTextField txtTimeoutModelo;
    private JButton btnRefrescarModelos;
    private JButton btnProbarConexion;
    private JButton btnGuardar;
    private JButton btnCerrar;
    private JButton btnBuscarActualizaciones;
    private JButton btnSitioWeb;
    private JButton btnRestaurarPrompt;
    private JButton btnRestaurarFuentes;
    private JComboBox<String> comboAgente;
    private JCheckBox chkAgenteHabilitado;
    private JTextField txtAgenteBinario;
    private JTextArea txtAgentePromptInicial;
    private JTextArea txtAgentePrompt;
    private JButton btnRestaurarPromptAgenteInicial;
    private JButton btnRestaurarPromptAgente;
    private JComboBox<com.burpia.i18n.IdiomaUI> comboIdioma;
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
    private JLabel lblContadorPrompt;
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
    
    /**
     * Crea un nuevo diálogo de configuración.
     *
     * @param padre         Ventana padre
     * @param config        Configuración API actual
     * @param gestorConfig  Gestor de configuración
     * @param alGuardar     Callback a ejecutar al guardar
     */
    @SuppressWarnings("this-escape")
    public DialogoConfiguracion(Window padre, 
                                ConfiguracionAPI config, 
                                GestorConfiguracion gestorConfig,
                                Runnable alGuardar) {
        super(padre, I18nUI.Configuracion.TITULO_DIALOGO(), Dialog.ModalityType.APPLICATION_MODAL);
        
        if (config == null || gestorConfig == null) {
            throw new IllegalArgumentException("Configuración y gestorConfig no pueden ser nulos");
        }
        
        this.config = config;
        this.gestorConfig = gestorConfig;
        this.alGuardar = alGuardar;
        
        inicializarDialogo();
    }
    
    /**
     * Inicializa el diálogo y sus componentes.
     */
    private void inicializarDialogo() {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));
        setPreferredSize(new Dimension(ANCHO_DIALOGO, ALTO_DIALOGO));
        
        // Crear vista
        view = new ConfigDialogView();
        
        // Crear controlador
        controller = new ConfigDialogController(this, config, gestorConfig);
        
        // Construir UI
        JComponent panelPrincipal = view.crearDialogoPrincipal();
        add(panelPrincipal, BorderLayout.CENTER);
        
        // Inicializar campos delegados para compatibilidad con tests
        inicializarCamposDelegados();
        
        // Configurar manejadores de eventos
        controller.inicializarEventHandlers();
        
        // Cargar configuración inicial
        controller.cargarConfiguracionInicial();
        
        // Configurar cierre del diálogo
        configurarManejadorCierre();
        
        // Aplicar tooltips
        view.aplicarTooltips();
        
        pack();
        setSize(ANCHO_DIALOGO, ALTO_DIALOGO);
        setLocationRelativeTo(getParent());
    }
    
    /**
     * Configura el manejador de cierre del diálogo.
     */
    private void configurarManejadorCierre() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.manejarCerrarDialogo();
            }
        });
    }
    
    /**
     * Inicializa los campos delegados para compatibilidad con tests.
     */
    private void inicializarCamposDelegados() {
        // Delegar todos los campos a la vista
        this.comboProveedor = view.getComboProveedor();
        this.comboModelo = view.getComboModelo();
        this.txtUrl = view.getTxtUrl();
        this.txtClave = view.getTxtClave();
        this.txtMaxTokens = view.getTxtMaxTokens();
        this.txtTimeoutModelo = view.getTxtTimeoutModelo();
        this.btnRefrescarModelos = view.getBtnRefrescarModelos();
        this.btnProbarConexion = view.getBtnProbarConexion();
        this.btnGuardar = view.getBtnGuardar();
        this.btnCerrar = view.getBtnCerrar();
        this.btnBuscarActualizaciones = view.getBtnBuscarActualizaciones();
        this.btnSitioWeb = view.getBtnSitioWeb();
        this.btnRestaurarPrompt = view.getBtnRestaurarPrompt();
        this.btnRestaurarFuentes = view.getBtnRestaurarFuentes();
        this.comboAgente = view.getComboAgente();
        this.chkAgenteHabilitado = view.getChkAgenteHabilitado();
        this.txtAgenteBinario = view.getTxtAgenteBinario();
        this.txtAgentePromptInicial = view.getTxtAgentePromptInicial();
        this.txtAgentePrompt = view.getTxtAgentePrompt();
        this.btnRestaurarPromptAgenteInicial = view.getBtnRestaurarPromptAgenteInicial();
        this.btnRestaurarPromptAgente = view.getBtnRestaurarPromptAgente();
        this.comboIdioma = view.getComboIdioma();
        this.txtRetraso = view.getTxtRetraso();
        this.txtMaximoConcurrente = view.getTxtMaximoConcurrente();
        this.txtMaximoHallazgosTabla = view.getTxtMaximoHallazgosTabla();
        this.txtMaximoTareas = view.getTxtMaximoTareas();
        this.chkDetallado = view.getChkDetallado();
        this.chkIgnorarSSL = view.getChkIgnorarSSL();
        this.chkSoloProxy = view.getChkSoloProxy();
        this.chkAlertasHabilitadas = view.getChkAlertasHabilitadas();
        this.chkPersistirBusqueda = view.getChkPersistirBusqueda();
        this.chkPersistirSeveridad = view.getChkPersistirSeveridad();
        this.txtPrompt = view.getTxtPrompt();
        this.lblContadorPrompt = view.getLblContadorPrompt();
        this.chkHabilitarMultiProveedor = view.getChkHabilitarMultiProveedor();
        this.listaProveedoresDisponibles = view.getListaProveedoresDisponibles();
        this.listaProveedoresSeleccionados = view.getListaProveedoresSeleccionados();
        this.modeloListaDisponibles = view.getModeloListaDisponibles();
        this.modeloListaSeleccionados = view.getModeloListaSeleccionados();
        this.btnAgregarProveedor = view.getBtnAgregarProveedor();
        this.btnQuitarProveedor = view.getBtnQuitarProveedor();
        this.btnSubirProveedor = view.getBtnSubirProveedor();
        this.btnBajarProveedor = view.getBtnBajarProveedor();
        this.lblEstadoMultiProveedor = view.getLblEstadoMultiProveedor();
        this.comboFuenteEstandar = view.getComboFuenteEstandar();
        this.spinnerTamanioEstandar = view.getSpinnerTamanioEstandar();
        this.comboFuenteMono = view.getComboFuenteMono();
        this.spinnerTamanioMono = view.getSpinnerTamanioMono();
    }
    
    // ===== MÉTODOS DE ACCESO PARA EL CONTROLADOR =====
    // Estos métodos permiten que el controlador acceda a los componentes UI
    
    public JComboBox<String> obtenerComboProveedor() {
        return view != null ? view.getComboProveedor() : null;
    }
    
    public JComboBox<String> obtenerComboModelo() {
        return view != null ? view.getComboModelo() : null;
    }
    
    public JTextField obtenerTxtUrl() {
        return view != null ? view.getTxtUrl() : null;
    }
    
    public JPasswordField obtenerTxtClave() {
        return view != null ? view.getTxtClave() : null;
    }
    
    public JTextField obtenerTxtMaxTokens() {
        return view != null ? view.getTxtMaxTokens() : null;
    }
    
    public JTextField obtenerTxtTimeoutModelo() {
        return view != null ? view.getTxtTimeoutModelo() : null;
    }
    
    public JButton obtenerBtnRefrescarModelos() {
        return view != null ? view.getBtnRefrescarModelos() : null;
    }
    
    public JButton obtenerBtnProbarConexion() {
        return view != null ? view.getBtnProbarConexion() : null;
    }
    
    public JButton obtenerBtnGuardar() {
        return view != null ? view.getBtnGuardar() : null;
    }
    
    public JButton obtenerBtnCerrar() {
        return view != null ? view.getBtnCerrar() : null;
    }
    
    public JButton obtenerBtnBuscarActualizaciones() {
        return view != null ? view.getBtnBuscarActualizaciones() : null;
    }
    
    public JButton obtenerBtnSitioWeb() {
        return view != null ? view.getBtnSitioWeb() : null;
    }
    
    public JButton obtenerBtnRestaurarPrompt() {
        return view != null ? view.getBtnRestaurarPrompt() : null;
    }
    
    public JButton obtenerBtnRestaurarFuentes() {
        return view != null ? view.getBtnRestaurarFuentes() : null;
    }
    
    public JComboBox<String> obtenerComboAgente() {
        return view != null ? view.getComboAgente() : null;
    }
    
    public JCheckBox obtenerChkAgenteHabilitado() {
        return view != null ? view.getChkAgenteHabilitado() : null;
    }
    
    public JTextField obtenerTxtAgenteBinario() {
        return view != null ? view.getTxtAgenteBinario() : null;
    }
    
    public JTextArea obtenerTxtAgentePromptInicial() {
        return view != null ? view.getTxtAgentePromptInicial() : null;
    }
    
    public JTextArea obtenerTxtAgentePrompt() {
        return view != null ? view.getTxtAgentePrompt() : null;
    }
    
    public JButton obtenerBtnRestaurarPromptAgenteInicial() {
        return view != null ? view.getBtnRestaurarPromptAgenteInicial() : null;
    }
    
    public JButton obtenerBtnRestaurarPromptAgente() {
        return view != null ? view.getBtnRestaurarPromptAgente() : null;
    }
    
    public JComboBox<com.burpia.i18n.IdiomaUI> obtenerComboIdioma() {
        return view != null ? view.getComboIdioma() : null;
    }
    
    public JTextField obtenerTxtRetraso() {
        return view != null ? view.getTxtRetraso() : null;
    }
    
    public JTextField obtenerTxtMaximoConcurrente() {
        return view != null ? view.getTxtMaximoConcurrente() : null;
    }
    
    public JTextField obtenerTxtMaximoHallazgosTabla() {
        return view != null ? view.getTxtMaximoHallazgosTabla() : null;
    }
    
    public JTextField obtenerTxtMaximoTareas() {
        return view != null ? view.getTxtMaximoTareas() : null;
    }
    
    public JCheckBox obtenerChkDetallado() {
        return view != null ? view.getChkDetallado() : null;
    }
    
    public JCheckBox obtenerChkIgnorarSSL() {
        return view != null ? view.getChkIgnorarSSL() : null;
    }
    
    public JCheckBox obtenerChkSoloProxy() {
        return view != null ? view.getChkSoloProxy() : null;
    }
    
    public JCheckBox obtenerChkAlertasHabilitadas() {
        return view != null ? view.getChkAlertasHabilitadas() : null;
    }
    
    public JCheckBox obtenerChkPersistirBusqueda() {
        return view != null ? view.getChkPersistirBusqueda() : null;
    }
    
    public JCheckBox obtenerChkPersistirSeveridad() {
        return view != null ? view.getChkPersistirSeveridad() : null;
    }
    
    public JTextArea obtenerTxtPrompt() {
        return view != null ? view.getTxtPrompt() : null;
    }
    
    public JLabel obtenerLblContadorPrompt() {
        return view != null ? view.getLblContadorPrompt() : null;
    }
    
    public JCheckBox obtenerChkHabilitarMultiProveedor() {
        return view != null ? view.getChkHabilitarMultiProveedor() : null;
    }
    
    public JList<String> obtenerListaProveedoresDisponibles() {
        return view != null ? view.getListaProveedoresDisponibles() : null;
    }
    
    public JList<String> obtenerListaProveedoresSeleccionados() {
        return view != null ? view.getListaProveedoresSeleccionados() : null;
    }
    
    public DefaultListModel<String> obtenerModeloListaDisponibles() {
        return view != null ? view.getModeloListaDisponibles() : null;
    }
    
    public DefaultListModel<String> obtenerModeloListaSeleccionados() {
        return view != null ? view.getModeloListaSeleccionados() : null;
    }
    
    public JButton obtenerBtnAgregarProveedor() {
        return view != null ? view.getBtnAgregarProveedor() : null;
    }
    
    public JButton obtenerBtnQuitarProveedor() {
        return view != null ? view.getBtnQuitarProveedor() : null;
    }
    
    public JButton obtenerBtnSubirProveedor() {
        return view != null ? view.getBtnSubirProveedor() : null;
    }
    
    public JButton obtenerBtnBajarProveedor() {
        return view != null ? view.getBtnBajarProveedor() : null;
    }
    
    public JLabel obtenerLblEstadoMultiProveedor() {
        return view != null ? view.getLblEstadoMultiProveedor() : null;
    }
    
    public JComboBox<String> obtenerComboFuenteEstandar() {
        return view != null ? view.getComboFuenteEstandar() : null;
    }
    
    public JSpinner obtenerSpinnerTamanioEstandar() {
        return view != null ? view.getSpinnerTamanioEstandar() : null;
    }
    
    public JComboBox<String> obtenerComboFuenteMono() {
        return view != null ? view.getComboFuenteMono() : null;
    }
    
    public JSpinner obtenerSpinnerTamanioMono() {
        return view != null ? view.getSpinnerTamanioMono() : null;
    }
    
    /**
     * Obtiene el callback a ejecutar al guardar la configuración.
     * 
     * @return Callback de guardado
     */
    public Runnable obtenerAlGuardar() {
        return alGuardar;
    }
    
    /**
     * Muestra el diálogo y espera a que se cierre.
     * 
     * @return true si se guardó la configuración, false en caso contrario
     */
    public boolean mostrarDialogo() {
        setVisible(true);
        return true; // El controlador manejará el guardado
    }
    
    // ===== MÉTODOS DE DELEGACIÓN PARA MANTENER COMPATIBILIDAD CON TESTS =====
    // Estos métodos delegan al controlador para mantener la compatibilidad
    
    /**
     * Delegación al método del controlador para compatibilidad con tests.
     */
    public void guardarConfiguracion() {
        if (controller != null) {
            controller.manejarGuardarConfiguracion();
        }
    }
    
    /**
     * Delegación al método del controlador para compatibilidad con tests.
     */
    public void probarConexion() {
        if (controller != null) {
            controller.manejarProbarConexion();
        }
    }
    
    /**
     * Delegación al método del controlador para compatibilidad con tests.
     */
    public void actualizarTimeoutModeloSeleccionado() {
        if (controller != null) {
            controller.actualizarTimeoutModeloSeleccionado();
        }
    }
    
    /**
     * Delegación al método del controlador para compatibilidad con tests.
     */
    public boolean tieneCambiosSinGuardar() {
        return controller != null ? controller.tieneCambiosSinGuardar() : false;
    }
}