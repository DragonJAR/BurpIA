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
 * - ProbadorConexionAI / ConnectionTester: Pruebas de conexión y operaciones remotas auxiliares
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
            throw new IllegalArgumentException(I18nUI.General.ERROR_CONFIG_Y_GESTOR_CONFIG_NULOS());
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

    @Override
    public void dispose() {
        if (controller != null) {
            controller.cerrar();
        }
        super.dispose();
    }
    
    /**
     * Inicializa los campos delegados para compatibilidad con tests.
     */
    private void inicializarCamposDelegados() {
        // Delegar todos los campos a la vista
        this.comboProveedor = view.obtenerComboProveedor();
        this.comboModelo = view.obtenerComboModelo();
        this.txtUrl = view.obtenerTxtUrl();
        this.txtClave = view.obtenerTxtClave();
        this.txtMaxTokens = view.obtenerTxtMaxTokens();
        this.txtTimeoutModelo = view.obtenerTxtTimeoutModelo();
        this.btnRefrescarModelos = view.obtenerBtnRefrescarModelos();
        this.btnProbarConexion = view.obtenerBtnProbarConexion();
        this.btnGuardar = view.obtenerBtnGuardar();
        this.btnCerrar = view.obtenerBtnCerrar();
        this.btnBuscarActualizaciones = view.obtenerBtnBuscarActualizaciones();
        this.btnSitioWeb = view.obtenerBtnSitioWeb();
        this.btnRestaurarPrompt = view.obtenerBtnRestaurarPrompt();
        this.btnRestaurarFuentes = view.obtenerBtnRestaurarFuentes();
        this.comboAgente = view.obtenerComboAgente();
        this.chkAgenteHabilitado = view.obtenerChkAgenteHabilitado();
        this.txtAgenteBinario = view.obtenerTxtAgenteBinario();
        this.txtAgentePromptInicial = view.obtenerTxtAgentePromptInicial();
        this.txtAgentePrompt = view.obtenerTxtAgentePrompt();
        this.btnRestaurarPromptAgenteInicial = view.obtenerBtnRestaurarPromptAgenteInicial();
        this.btnRestaurarPromptAgente = view.obtenerBtnRestaurarPromptAgente();
        this.comboIdioma = view.obtenerComboIdioma();
        this.txtRetraso = view.obtenerTxtRetraso();
        this.txtMaximoConcurrente = view.obtenerTxtMaximoConcurrente();
        this.txtMaximoHallazgosTabla = view.obtenerTxtMaximoHallazgosTabla();
        this.txtMaximoTareas = view.obtenerTxtMaximoTareas();
        this.chkDetallado = view.obtenerChkDetallado();
        this.chkIgnorarSSL = view.obtenerChkIgnorarSSL();
        this.chkSoloProxy = view.obtenerChkSoloProxy();
        this.chkAlertasHabilitadas = view.obtenerChkAlertasHabilitadas();
        this.chkPersistirBusqueda = view.obtenerChkPersistirBusqueda();
        this.chkPersistirSeveridad = view.obtenerChkPersistirSeveridad();
        this.txtPrompt = view.obtenerTxtPrompt();
        this.lblContadorPrompt = view.obtenerLblContadorPrompt();
        this.chkHabilitarMultiProveedor = view.obtenerChkHabilitarMultiProveedor();
        this.listaProveedoresDisponibles = view.obtenerListaProveedoresDisponibles();
        this.listaProveedoresSeleccionados = view.obtenerListaProveedoresSeleccionados();
        this.modeloListaDisponibles = view.obtenerModeloListaDisponibles();
        this.modeloListaSeleccionados = view.obtenerModeloListaSeleccionados();
        this.btnAgregarProveedor = view.obtenerBtnAgregarProveedor();
        this.btnQuitarProveedor = view.obtenerBtnQuitarProveedor();
        this.btnSubirProveedor = view.obtenerBtnSubirProveedor();
        this.btnBajarProveedor = view.obtenerBtnBajarProveedor();
        this.lblEstadoMultiProveedor = view.obtenerLblEstadoMultiProveedor();
        this.comboFuenteEstandar = view.obtenerComboFuenteEstandar();
        this.spinnerTamanioEstandar = view.obtenerSpinnerTamanioEstandar();
        this.comboFuenteMono = view.obtenerComboFuenteMono();
        this.spinnerTamanioMono = view.obtenerSpinnerTamanioMono();
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
        return obtenerComponente(ConfigDialogView::obtenerComboProveedor);
    }
    
    public JComboBox<String> obtenerComboModelo() {
        return obtenerComponente(ConfigDialogView::obtenerComboModelo);
    }
    
    public JTextField obtenerTxtUrl() {
        return obtenerComponente(ConfigDialogView::obtenerTxtUrl);
    }
    
    public JPasswordField obtenerTxtClave() {
        return obtenerComponente(ConfigDialogView::obtenerTxtClave);
    }
    
    public JTextField obtenerTxtMaxTokens() {
        return obtenerComponente(ConfigDialogView::obtenerTxtMaxTokens);
    }
    
    public JTextField obtenerTxtTimeoutModelo() {
        return obtenerComponente(ConfigDialogView::obtenerTxtTimeoutModelo);
    }
    
    public JButton obtenerBtnRefrescarModelos() {
        return obtenerComponente(ConfigDialogView::obtenerBtnRefrescarModelos);
    }
    
    public JButton obtenerBtnProbarConexion() {
        return obtenerComponente(ConfigDialogView::obtenerBtnProbarConexion);
    }
    
    public JButton obtenerBtnGuardar() {
        return obtenerComponente(ConfigDialogView::obtenerBtnGuardar);
    }
    
    public JButton obtenerBtnCerrar() {
        return obtenerComponente(ConfigDialogView::obtenerBtnCerrar);
    }
    
    public JButton obtenerBtnBuscarActualizaciones() {
        return obtenerComponente(ConfigDialogView::obtenerBtnBuscarActualizaciones);
    }
    
    public JButton obtenerBtnSitioWeb() {
        return obtenerComponente(ConfigDialogView::obtenerBtnSitioWeb);
    }
    
    public JButton obtenerBtnRestaurarPrompt() {
        return obtenerComponente(ConfigDialogView::obtenerBtnRestaurarPrompt);
    }
    
    public JButton obtenerBtnRestaurarFuentes() {
        return obtenerComponente(ConfigDialogView::obtenerBtnRestaurarFuentes);
    }
    
    public JComboBox<String> obtenerComboAgente() {
        return obtenerComponente(ConfigDialogView::obtenerComboAgente);
    }
    
    public JCheckBox obtenerChkAgenteHabilitado() {
        return obtenerComponente(ConfigDialogView::obtenerChkAgenteHabilitado);
    }
    
    public JTextField obtenerTxtAgenteBinario() {
        return obtenerComponente(ConfigDialogView::obtenerTxtAgenteBinario);
    }
    
    public JTextArea obtenerTxtAgentePromptInicial() {
        return obtenerComponente(ConfigDialogView::obtenerTxtAgentePromptInicial);
    }
    
    public JTextArea obtenerTxtAgentePrompt() {
        return obtenerComponente(ConfigDialogView::obtenerTxtAgentePrompt);
    }
    
    public JButton obtenerBtnRestaurarPromptAgenteInicial() {
        return obtenerComponente(ConfigDialogView::obtenerBtnRestaurarPromptAgenteInicial);
    }
    
    public JButton obtenerBtnRestaurarPromptAgente() {
        return obtenerComponente(ConfigDialogView::obtenerBtnRestaurarPromptAgente);
    }
    
    public JComboBox<com.burpia.i18n.IdiomaUI> obtenerComboIdioma() {
        return obtenerComponente(ConfigDialogView::obtenerComboIdioma);
    }
    
    public JTextField obtenerTxtRetraso() {
        return obtenerComponente(ConfigDialogView::obtenerTxtRetraso);
    }
    
    public JTextField obtenerTxtMaximoConcurrente() {
        return obtenerComponente(ConfigDialogView::obtenerTxtMaximoConcurrente);
    }
    
    public JTextField obtenerTxtMaximoHallazgosTabla() {
        return obtenerComponente(ConfigDialogView::obtenerTxtMaximoHallazgosTabla);
    }
    
    public JTextField obtenerTxtMaximoTareas() {
        return obtenerComponente(ConfigDialogView::obtenerTxtMaximoTareas);
    }
    
    public JCheckBox obtenerChkDetallado() {
        return obtenerComponente(ConfigDialogView::obtenerChkDetallado);
    }
    
    public JCheckBox obtenerChkIgnorarSSL() {
        return obtenerComponente(ConfigDialogView::obtenerChkIgnorarSSL);
    }
    
    public JCheckBox obtenerChkSoloProxy() {
        return obtenerComponente(ConfigDialogView::obtenerChkSoloProxy);
    }
    
    public JCheckBox obtenerChkAlertasHabilitadas() {
        return obtenerComponente(ConfigDialogView::obtenerChkAlertasHabilitadas);
    }
    
    public JCheckBox obtenerChkPersistirBusqueda() {
        return obtenerComponente(ConfigDialogView::obtenerChkPersistirBusqueda);
    }
    
    public JCheckBox obtenerChkPersistirSeveridad() {
        return obtenerComponente(ConfigDialogView::obtenerChkPersistirSeveridad);
    }
    
    public JTextArea obtenerTxtPrompt() {
        return obtenerComponente(ConfigDialogView::obtenerTxtPrompt);
    }
    
    public JLabel obtenerLblContadorPrompt() {
        return obtenerComponente(ConfigDialogView::obtenerLblContadorPrompt);
    }
    
    public JCheckBox obtenerChkHabilitarMultiProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerChkHabilitarMultiProveedor);
    }
    
    public JList<String> obtenerListaProveedoresDisponibles() {
        return obtenerComponente(ConfigDialogView::obtenerListaProveedoresDisponibles);
    }
    
    public JList<String> obtenerListaProveedoresSeleccionados() {
        return obtenerComponente(ConfigDialogView::obtenerListaProveedoresSeleccionados);
    }
    
    public DefaultListModel<String> obtenerModeloListaDisponibles() {
        return obtenerComponente(ConfigDialogView::obtenerModeloListaDisponibles);
    }
    
    public DefaultListModel<String> obtenerModeloListaSeleccionados() {
        return obtenerComponente(ConfigDialogView::obtenerModeloListaSeleccionados);
    }
    
    public JButton obtenerBtnAgregarProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerBtnAgregarProveedor);
    }
    
    public JButton obtenerBtnQuitarProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerBtnQuitarProveedor);
    }
    
    public JButton obtenerBtnSubirProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerBtnSubirProveedor);
    }
    
    public JButton obtenerBtnBajarProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerBtnBajarProveedor);
    }
    
    public JLabel obtenerLblEstadoMultiProveedor() {
        return obtenerComponente(ConfigDialogView::obtenerLblEstadoMultiProveedor);
    }
    
    public JComboBox<String> obtenerComboFuenteEstandar() {
        return obtenerComponente(ConfigDialogView::obtenerComboFuenteEstandar);
    }
    
    public JSpinner obtenerSpinnerTamanioEstandar() {
        return obtenerComponente(ConfigDialogView::obtenerSpinnerTamanioEstandar);
    }
    
    public JComboBox<String> obtenerComboFuenteMono() {
        return obtenerComponente(ConfigDialogView::obtenerComboFuenteMono);
    }
    
    public JSpinner obtenerSpinnerTamanioMono() {
        return obtenerComponente(ConfigDialogView::obtenerSpinnerTamanioMono);
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
