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
    
    // Dimensiones del diálogo (definidas en ConfigDialogView para centralizar)
    private static final int ANCHO_DIALOGO = ConfigDialogView.ANCHO_DIALOGO;
    private static final int ALTO_DIALOGO = ConfigDialogView.ALTO_DIALOGO;
    
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
    
    /**
     * DRY: Método genérico para obtener componentes de la vista de forma segura.
     */
    private <T> T obtenerComponente(java.util.function.Function<ConfigDialogView, T> getter) {
        return view != null ? getter.apply(view) : null;
    }
    
    public JComboBox<String> obtenerComboProveedor() {
        return obtenerComponente(ConfigDialogView::getComboProveedor);
    }
    
    public JComboBox<String> obtenerComboModelo() {
        return obtenerComponente(ConfigDialogView::getComboModelo);
    }
    
    public JTextField obtenerTxtUrl() {
        return obtenerComponente(ConfigDialogView::getTxtUrl);
    }
    
    public JPasswordField obtenerTxtClave() {
        return obtenerComponente(ConfigDialogView::getTxtClave);
    }
    
    public JTextField obtenerTxtMaxTokens() {
        return obtenerComponente(ConfigDialogView::getTxtMaxTokens);
    }
    
    public JTextField obtenerTxtTimeoutModelo() {
        return obtenerComponente(ConfigDialogView::getTxtTimeoutModelo);
    }
    
    public JButton obtenerBtnRefrescarModelos() {
        return obtenerComponente(ConfigDialogView::getBtnRefrescarModelos);
    }
    
    public JButton obtenerBtnProbarConexion() {
        return obtenerComponente(ConfigDialogView::getBtnProbarConexion);
    }
    
    public JButton obtenerBtnGuardar() {
        return obtenerComponente(ConfigDialogView::getBtnGuardar);
    }
    
    public JButton obtenerBtnCerrar() {
        return obtenerComponente(ConfigDialogView::getBtnCerrar);
    }
    
    public JButton obtenerBtnBuscarActualizaciones() {
        return obtenerComponente(ConfigDialogView::getBtnBuscarActualizaciones);
    }
    
    public JButton obtenerBtnSitioWeb() {
        return obtenerComponente(ConfigDialogView::getBtnSitioWeb);
    }
    
    public JButton obtenerBtnRestaurarPrompt() {
        return obtenerComponente(ConfigDialogView::getBtnRestaurarPrompt);
    }
    
    public JButton obtenerBtnRestaurarFuentes() {
        return obtenerComponente(ConfigDialogView::getBtnRestaurarFuentes);
    }
    
    public JComboBox<String> obtenerComboAgente() {
        return obtenerComponente(ConfigDialogView::getComboAgente);
    }
    
    public JCheckBox obtenerChkAgenteHabilitado() {
        return obtenerComponente(ConfigDialogView::getChkAgenteHabilitado);
    }
    
    public JTextField obtenerTxtAgenteBinario() {
        return obtenerComponente(ConfigDialogView::getTxtAgenteBinario);
    }
    
    public JTextArea obtenerTxtAgentePromptInicial() {
        return obtenerComponente(ConfigDialogView::getTxtAgentePromptInicial);
    }
    
    public JTextArea obtenerTxtAgentePrompt() {
        return obtenerComponente(ConfigDialogView::getTxtAgentePrompt);
    }
    
    public JButton obtenerBtnRestaurarPromptAgenteInicial() {
        return obtenerComponente(ConfigDialogView::getBtnRestaurarPromptAgenteInicial);
    }
    
    public JButton obtenerBtnRestaurarPromptAgente() {
        return obtenerComponente(ConfigDialogView::getBtnRestaurarPromptAgente);
    }
    
    public JComboBox<com.burpia.i18n.IdiomaUI> obtenerComboIdioma() {
        return obtenerComponente(ConfigDialogView::getComboIdioma);
    }
    
    public JTextField obtenerTxtRetraso() {
        return obtenerComponente(ConfigDialogView::getTxtRetraso);
    }
    
    public JTextField obtenerTxtMaximoConcurrente() {
        return obtenerComponente(ConfigDialogView::getTxtMaximoConcurrente);
    }
    
    public JTextField obtenerTxtMaximoHallazgosTabla() {
        return obtenerComponente(ConfigDialogView::getTxtMaximoHallazgosTabla);
    }
    
    public JTextField obtenerTxtMaximoTareas() {
        return obtenerComponente(ConfigDialogView::getTxtMaximoTareas);
    }
    
    public JCheckBox obtenerChkDetallado() {
        return obtenerComponente(ConfigDialogView::getChkDetallado);
    }
    
    public JCheckBox obtenerChkIgnorarSSL() {
        return obtenerComponente(ConfigDialogView::getChkIgnorarSSL);
    }
    
    public JCheckBox obtenerChkSoloProxy() {
        return obtenerComponente(ConfigDialogView::getChkSoloProxy);
    }
    
    public JCheckBox obtenerChkAlertasHabilitadas() {
        return obtenerComponente(ConfigDialogView::getChkAlertasHabilitadas);
    }
    
    public JCheckBox obtenerChkPersistirBusqueda() {
        return obtenerComponente(ConfigDialogView::getChkPersistirBusqueda);
    }
    
    public JCheckBox obtenerChkPersistirSeveridad() {
        return obtenerComponente(ConfigDialogView::getChkPersistirSeveridad);
    }
    
    public JTextArea obtenerTxtPrompt() {
        return obtenerComponente(ConfigDialogView::getTxtPrompt);
    }
    
    public JLabel obtenerLblContadorPrompt() {
        return obtenerComponente(ConfigDialogView::getLblContadorPrompt);
    }
    
    public JCheckBox obtenerChkHabilitarMultiProveedor() {
        return obtenerComponente(ConfigDialogView::getChkHabilitarMultiProveedor);
    }
    
    public JList<String> obtenerListaProveedoresDisponibles() {
        return obtenerComponente(ConfigDialogView::getListaProveedoresDisponibles);
    }
    
    public JList<String> obtenerListaProveedoresSeleccionados() {
        return obtenerComponente(ConfigDialogView::getListaProveedoresSeleccionados);
    }
    
    public DefaultListModel<String> obtenerModeloListaDisponibles() {
        return obtenerComponente(ConfigDialogView::getModeloListaDisponibles);
    }
    
    public DefaultListModel<String> obtenerModeloListaSeleccionados() {
        return obtenerComponente(ConfigDialogView::getModeloListaSeleccionados);
    }
    
    public JButton obtenerBtnAgregarProveedor() {
        return obtenerComponente(ConfigDialogView::getBtnAgregarProveedor);
    }
    
    public JButton obtenerBtnQuitarProveedor() {
        return obtenerComponente(ConfigDialogView::getBtnQuitarProveedor);
    }
    
    public JButton obtenerBtnSubirProveedor() {
        return obtenerComponente(ConfigDialogView::getBtnSubirProveedor);
    }
    
    public JButton obtenerBtnBajarProveedor() {
        return obtenerComponente(ConfigDialogView::getBtnBajarProveedor);
    }
    
    public JLabel obtenerLblEstadoMultiProveedor() {
        return obtenerComponente(ConfigDialogView::getLblEstadoMultiProveedor);
    }
    
    public JComboBox<String> obtenerComboFuenteEstandar() {
        return obtenerComponente(ConfigDialogView::getComboFuenteEstandar);
    }
    
    public JSpinner obtenerSpinnerTamanioEstandar() {
        return obtenerComponente(ConfigDialogView::getSpinnerTamanioEstandar);
    }
    
    public JComboBox<String> obtenerComboFuenteMono() {
        return obtenerComponente(ConfigDialogView::getComboFuenteMono);
    }
    
    public JSpinner obtenerSpinnerTamanioMono() {
        return obtenerComponente(ConfigDialogView::getSpinnerTamanioMono);
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
