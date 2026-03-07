package com.burpia.ui;

import com.burpia.config.*;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controlador principal para el diálogo de configuración.
 * <p>
 * Extrae la lógica de manejo de eventos, coordinación y validación
 * desde DialogoConfiguracion siguiendo el principio DRY.
 * </p>
 * <p>
 * Responsabilidades:
 * - Manejo de eventos de UI
 * - Coordinación entre vista, persistencia, proveedores y validadores
 * - Gestión del ciclo de vida del diálogo (abrir/guardar/cancelar)
 * - Coordinación de operaciones asíncronas (pruebas de conexión, obtención de modelos)
 * </p>
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
public class ConfigDialogController {

    private static final String ORIGEN_LOG = "ConfigDialogController";
    private static final int TIMEOUT_CONEXION_MODELOS_SEG = 8;
    private static final int TIMEOUT_LECTURA_MODELOS_SEG = 12;
    private static final int TIMEOUT_CONEXION_ACTUALIZACIONES_SEG = 8;
    private static final int TIMEOUT_LECTURA_ACTUALIZACIONES_SEG = 12;

    private final DialogoConfiguracion dialogo;
    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final ConfigPersistenceManager persistenceManager;
    private final ProviderConfigManager providerManager;
    private final GestorLoggingUnificado gestorLogging;
    private final Map<String, String> rutasBinarioAgenteTemporal;
    private final AtomicLong secuenciaRefrescoModelos;

    private boolean guardandoConfiguracion = false;
    private boolean actualizandoRutaFlag = false;

    public ConfigDialogController(DialogoConfiguracion dialogo, 
                                ConfiguracionAPI config, 
                                GestorConfiguracion gestorConfig) {
        if (dialogo == null || config == null || gestorConfig == null) {
            throw new IllegalArgumentException("Dialogo, configuración y gestorConfig no pueden ser nulos");
        }

        this.dialogo = dialogo;
        this.config = config;
        this.gestorConfig = gestorConfig;
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(
            new java.io.PrintWriter(System.out, true), 
            new java.io.PrintWriter(System.err, true)
        );
        this.persistenceManager = new ConfigPersistenceManager(gestorConfig, gestorLogging);
        this.providerManager = new ProviderConfigManager(config, gestorLogging);
        this.rutasBinarioAgenteTemporal = new HashMap<>();
        this.secuenciaRefrescoModelos = new AtomicLong(0);

        gestorLogging.info(ORIGEN_LOG, "ConfigDialogController inicializado");
    }

    /**
     * Configura los componentes de UI en el ProviderConfigManager.
     * Esto es necesario para que el manager pueda extraer el estado actual de la UI.
     */
    private void configurarComponentesUIProvider() {
        providerManager.configurarComponentes(
            dialogo.obtenerComboProveedor(),
            dialogo.obtenerTxtUrl(),
            dialogo.obtenerTxtClave(),
            dialogo.obtenerComboModelo(),
            dialogo.obtenerTxtTimeoutModelo(),
            dialogo.obtenerTxtMaxTokens()
        );
    }

    public void inicializarEventHandlers() {
        gestorLogging.info(ORIGEN_LOG, "Inicializando manejadores de eventos");
        
        // Configurar componentes de UI primero
        configurarComponentesUIProvider();
        
        inicializarEventHandlersProvider();
        inicializarEventHandlersAgent();
        inicializarEventHandlersAcciones();
        inicializarEventHandlersDialog();
        inicializarEventHandlersDocumentos();
        
        gestorLogging.info(ORIGEN_LOG, "Manejadores de eventos inicializados");
    }

    private void inicializarEventHandlersProvider() {
        JComboBox<String> comboProveedor = dialogo.obtenerComboProveedor();
        if (comboProveedor != null) {
            comboProveedor.addActionListener(e -> manejarCambioProveedor());
        }

        JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
        if (comboModelo != null) {
            comboModelo.addActionListener(e -> manejarCambioModelo());
        }

        JButton btnRefrescarModelos = dialogo.obtenerBtnRefrescarModelos();
        if (btnRefrescarModelos != null) {
            btnRefrescarModelos.addActionListener(e -> manejarRefrescarModelos());
        }

        JButton btnProbarConexion = dialogo.obtenerBtnProbarConexion();
        if (btnProbarConexion != null) {
            btnProbarConexion.addActionListener(e -> manejarProbarConexion());
        }
    }

    private void inicializarEventHandlersAgent() {
        JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
        if (comboAgente != null) {
            comboAgente.addActionListener(e -> manejarCambioAgente());
        }

        JButton btnRestaurarPromptAgenteInicial = dialogo.obtenerBtnRestaurarPromptAgenteInicial();
        if (btnRestaurarPromptAgenteInicial != null) {
            btnRestaurarPromptAgenteInicial.addActionListener(e -> manejarRestaurarPromptAgenteInicial());
        }

        JButton btnRestaurarPromptAgente = dialogo.obtenerBtnRestaurarPromptAgente();
        if (btnRestaurarPromptAgente != null) {
            btnRestaurarPromptAgente.addActionListener(e -> manejarRestaurarPromptAgente());
        }
    }

    private void inicializarEventHandlersAcciones() {
        JButton btnGuardar = dialogo.obtenerBtnGuardar();
        if (btnGuardar != null) {
            btnGuardar.addActionListener(e -> manejarGuardarConfiguracion());
        }

        JButton btnCerrar = dialogo.obtenerBtnCerrar();
        if (btnCerrar != null) {
            btnCerrar.addActionListener(e -> manejarCerrarDialogo());
        }

        JButton btnBuscarActualizaciones = dialogo.obtenerBtnBuscarActualizaciones();
        if (btnBuscarActualizaciones != null) {
            btnBuscarActualizaciones.addActionListener(e -> manejarVerificarActualizaciones());
        }

        JButton btnSitioWeb = dialogo.obtenerBtnSitioWeb();
        if (btnSitioWeb != null) {
            btnSitioWeb.addActionListener(e -> manejarAbrirSitioWeb());
        }

        JButton btnRestaurarPrompt = dialogo.obtenerBtnRestaurarPrompt();
        if (btnRestaurarPrompt != null) {
            btnRestaurarPrompt.addActionListener(e -> manejarRestaurarPromptPorDefecto());
        }

        JButton btnRestaurarFuentes = dialogo.obtenerBtnRestaurarFuentes();
        if (btnRestaurarFuentes != null) {
            btnRestaurarFuentes.addActionListener(e -> manejarRestaurarFuentesPorDefecto());
        }
    }

    private void inicializarEventHandlersDialog() {
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                manejarCerrarDialogo();
            }
        });
    }

    private void inicializarEventHandlersDocumentos() {
        JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
        if (txtPrompt != null) {
            txtPrompt.getDocument().addDocumentListener(crearDocumentListenerPrompt());
        }

        JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
        if (txtAgenteBinario != null) {
            txtAgenteBinario.getDocument().addDocumentListener(crearDocumentListenerAgenteBinario());
        }
    }

    private DocumentListener crearDocumentListenerPrompt() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContadorPrompt();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContadorPrompt();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContadorPrompt();
            }
        };
    }

    private DocumentListener crearDocumentListenerAgenteBinario() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarRutaEnMemoria();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarRutaEnMemoria();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarRutaEnMemoria();
            }
        };
    }

    public void cargarConfiguracionInicial() {
        gestorLogging.info(ORIGEN_LOG, "Cargando configuración inicial");
        
        ConfiguracionAPI configCargada = persistenceManager.cargarConfiguracion();
        if (configCargada != null) {
            config.aplicarDesde(configCargada);
        }

        providerManager.inicializarComponentesUI(
            dialogo.obtenerComboProveedor(),
            dialogo.obtenerComboModelo(),
            dialogo.obtenerTxtUrl(),
            dialogo.obtenerTxtClave(),
            dialogo.obtenerTxtMaxTokens(),
            dialogo.obtenerTxtTimeoutModelo(),
            dialogo.obtenerBtnAgregarProveedor(),
            dialogo.obtenerBtnQuitarProveedor(),
            dialogo.obtenerBtnSubirProveedor(),
            dialogo.obtenerBtnBajarProveedor(),
            dialogo.obtenerChkHabilitarMultiProveedor(),
            dialogo.obtenerLblEstadoMultiProveedor(),
            dialogo.obtenerModeloListaDisponibles(),
            dialogo.obtenerModeloListaSeleccionados()
        );

        providerManager.setListasProveedores(
            dialogo.obtenerListaProveedoresDisponibles(),
            dialogo.obtenerListaProveedoresSeleccionados()
        );

        providerManager.cargarConfiguracionInicial();
        
        rutasBinarioAgenteTemporal.clear();
        rutasBinarioAgenteTemporal.putAll(new HashMap<>(config.obtenerTodasLasRutasBinario()));

        cargarConfiguracionGeneral();
        cargarConfiguracionAgente();
        cargarConfiguracionFuentes();

        gestorLogging.info(ORIGEN_LOG, "Configuración inicial cargada");
    }

    private void cargarConfiguracionGeneral() {
        JComboBox<IdiomaUI> comboIdioma = dialogo.obtenerComboIdioma();
        if (comboIdioma != null) {
            comboIdioma.setSelectedItem(IdiomaUI.desdeCodigo(config.obtenerIdiomaUi()));
        }

        JTextField txtRetraso = dialogo.obtenerTxtRetraso();
        if (txtRetraso != null) {
            txtRetraso.setText(String.valueOf(config.obtenerRetrasoSegundos()));
        }

        JTextField txtMaximoConcurrente = dialogo.obtenerTxtMaximoConcurrente();
        if (txtMaximoConcurrente != null) {
            txtMaximoConcurrente.setText(String.valueOf(config.obtenerMaximoConcurrente()));
        }

        JTextField txtMaximoHallazgosTabla = dialogo.obtenerTxtMaximoHallazgosTabla();
        if (txtMaximoHallazgosTabla != null) {
            txtMaximoHallazgosTabla.setText(String.valueOf(config.obtenerMaximoHallazgosTabla()));
        }

        JTextField txtMaximoTareas = dialogo.obtenerTxtMaximoTareas();
        if (txtMaximoTareas != null) {
            txtMaximoTareas.setText(String.valueOf(config.obtenerMaximoTareasTabla()));
        }

        JCheckBox chkDetallado = dialogo.obtenerChkDetallado();
        if (chkDetallado != null) {
            chkDetallado.setSelected(config.esDetallado());
        }

        JCheckBox chkIgnorarSSL = dialogo.obtenerChkIgnorarSSL();
        if (chkIgnorarSSL != null) {
            chkIgnorarSSL.setSelected(config.ignorarErroresSSL());
        }

        JCheckBox chkSoloProxy = dialogo.obtenerChkSoloProxy();
        if (chkSoloProxy != null) {
            chkSoloProxy.setSelected(config.soloProxy());
        }

        JCheckBox chkAlertasHabilitadas = dialogo.obtenerChkAlertasHabilitadas();
        if (chkAlertasHabilitadas != null) {
            chkAlertasHabilitadas.setSelected(config.alertasHabilitadas());
        }

        JCheckBox chkPersistirBusqueda = dialogo.obtenerChkPersistirBusqueda();
        if (chkPersistirBusqueda != null) {
            chkPersistirBusqueda.setSelected(config.persistirFiltroBusquedaHallazgos());
        }

        JCheckBox chkPersistirSeveridad = dialogo.obtenerChkPersistirSeveridad();
        if (chkPersistirSeveridad != null) {
            chkPersistirSeveridad.setSelected(config.persistirFiltroSeveridadHallazgos());
        }

        JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
        if (txtPrompt != null) {
            if (config.esPromptModificado()) {
                txtPrompt.setText(config.obtenerPromptConfigurable());
            } else {
                txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
            }
            actualizarContadorPrompt();
        }
    }

    private void cargarConfiguracionAgente() {
        JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
        if (comboAgente != null) {
            filtrarAgentesDisponibles(comboAgente);
            
            String tipoAgente = config.obtenerTipoAgente();
            if (Normalizador.noEsVacio(tipoAgente)) {
                boolean existeOpcion = false;
                for (int i = 0; i < comboAgente.getItemCount(); i++) {
                    if (comboAgente.getItemAt(i).equals(tipoAgente)) {
                        existeOpcion = true;
                        break;
                    }
                }
                if (existeOpcion) {
                    comboAgente.setSelectedItem(tipoAgente);
                } else if (comboAgente.getItemCount() > 0) {
                    comboAgente.setSelectedIndex(0);
                }
            } else if (comboAgente.getItemCount() > 0) {
                comboAgente.setSelectedIndex(0);
            }
        }

        JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
        if (chkAgenteHabilitado != null) {
            chkAgenteHabilitado.setSelected(config.agenteHabilitado());
        }

        JTextArea txtAgentePromptInicial = dialogo.obtenerTxtAgentePromptInicial();
        if (txtAgentePromptInicial != null) {
            txtAgentePromptInicial.setText(config.obtenerAgentePreflightPrompt());
        }

        JTextArea txtAgentePrompt = dialogo.obtenerTxtAgentePrompt();
        if (txtAgentePrompt != null) {
            txtAgentePrompt.setText(config.obtenerAgentePrompt());
        }

        manejarCambioAgente();
    }
    
    private void filtrarAgentesDisponibles(JComboBox<String> comboAgente) {
        comboAgente.removeAllItems();
        
        for (String codigoAgente : AgenteTipo.codigosDisponibles()) {
            if (esAgenteDisponible(codigoAgente)) {
                comboAgente.addItem(codigoAgente);
            }
        }
    }
    
    private boolean esAgenteDisponible(String codigoAgente) {
        if (Normalizador.esVacio(codigoAgente)) {
            return false;
        }
        
        AgenteTipo tipoAgente = AgenteTipo.desdeCodigo(codigoAgente, null);
        if (tipoAgente == null) {
            return false;
        }
        
        String rutaBinario = config.obtenerRutaBinarioAgente(codigoAgente);
        if (Normalizador.esVacio(rutaBinario)) {
            return false;
        }
        
        return OSUtils.existeBinario(rutaBinario);
    }

    private void cargarConfiguracionFuentes() {
        JComboBox<String> comboFuenteEstandar = dialogo.obtenerComboFuenteEstandar();
        if (comboFuenteEstandar != null) {
            comboFuenteEstandar.setSelectedItem(config.obtenerNombreFuenteEstandar());
        }

        JSpinner spinnerTamanioEstandar = dialogo.obtenerSpinnerTamanioEstandar();
        if (spinnerTamanioEstandar != null) {
            spinnerTamanioEstandar.setValue(config.obtenerTamanioFuenteEstandar());
        }

        JComboBox<String> comboFuenteMono = dialogo.obtenerComboFuenteMono();
        if (comboFuenteMono != null) {
            comboFuenteMono.setSelectedItem(config.obtenerNombreFuenteMono());
        }

        JSpinner spinnerTamanioMono = dialogo.obtenerSpinnerTamanioMono();
        if (spinnerTamanioMono != null) {
            spinnerTamanioMono.setValue(config.obtenerTamanioFuenteMono());
        }
    }

    private void manejarCambioProveedor() {
        gestorLogging.info(ORIGEN_LOG, "Manejando cambio de proveedor");
        String nuevoProveedor = (String) dialogo.obtenerComboProveedor().getSelectedItem();
        providerManager.cambiarProveedor(nuevoProveedor);
    }

    private void manejarCambioModelo() {
        gestorLogging.info(ORIGEN_LOG, "Manejando cambio de modelo");
        SwingUtilities.invokeLater(this::actualizarTimeoutModeloSeleccionado);
    }

    private void manejarCambioAgente() {
        String agenteSeleccionado = (String) dialogo.obtenerComboAgente().getSelectedItem();
        if (Normalizador.esVacio(agenteSeleccionado)) {
            return;
        }

        actualizandoRutaFlag = true;
        try {
            AgenteTipo enumAgente = AgenteTipo.desdeCodigo(agenteSeleccionado, null);
            if (enumAgente != null) {
                JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
                if (chkAgenteHabilitado != null) {
                    chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(enumAgente.getNombreVisible()));
                }

                String rutaSeleccionada = resolverRutaBinarioAgente(agenteSeleccionado);
                JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
                if (txtAgenteBinario != null) {
                    if (Normalizador.noEsVacio(rutaSeleccionada)) {
                        txtAgenteBinario.setText(rutaSeleccionada);
                    } else {
                        txtAgenteBinario.setText(enumAgente.getRutaPorDefecto());
                    }
                }

                rutasBinarioAgenteTemporal.put(agenteSeleccionado, 
                    txtAgenteBinario != null ? txtAgenteBinario.getText().trim() : "");
            } else {
                JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
                if (chkAgenteHabilitado != null) {
                    chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(agenteSeleccionado));
                }
                JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
                if (txtAgenteBinario != null) {
                    txtAgenteBinario.setText("");
                }
            }
        } finally {
            actualizandoRutaFlag = false;
        }
    }

    private void manejarRefrescarModelos() {
        String proveedorSeleccionado = (String) dialogo.obtenerComboProveedor().getSelectedItem();
        if (Normalizador.esVacio(proveedorSeleccionado)) {
            return;
        }

        final long seq = secuenciaRefrescoModelos.incrementAndGet();
        JButton btnRefrescarModelos = dialogo.obtenerBtnRefrescarModelos();
        if (btnRefrescarModelos != null) {
            btnRefrescarModelos.setEnabled(false);
            btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_ACTUALIZANDO_MODELOS());
        }

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return obtenerModelosDesdeAPI(proveedorSeleccionado);
            }

            @Override
            protected void done() {
                if (seq != secuenciaRefrescoModelos.get()) {
                    return;
                }

                try {
                    List<String> modelos = get();
                    if (modelos == null || modelos.isEmpty()) {
                        throw new IllegalStateException(
                            I18nUI.Configuracion.ERROR_API_SIN_MODELOS(proveedorSeleccionado));
                    }
                    
                    JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
                    if (comboModelo != null) {
                        cargarModelosEnCombo(modelos, modelos.get(0));
                    }
                    
                    UIUtils.mostrarInfo(dialogo, I18nUI.Configuracion.TITULO_MODELOS_ACTUALIZADOS(),
                        I18nUI.Configuracion.MSG_MODELOS_ACTUALIZADOS(modelos.size(), proveedorSeleccionado));
                } catch (Exception e) {
                    UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR(),
                        I18nUI.Configuracion.MSG_ERROR_PROCESAR_MODELOS(extraerMensajeError(e)));
                } finally {
                    if (btnRefrescarModelos != null) {
                        btnRefrescarModelos.setEnabled(true);
                        btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
                    }
                }
            }
        };
        worker.execute();
    }

    public void manejarProbarConexion() {
        EstadoProveedorUI estadoUI = providerManager.extraerEstadoActual();
        if (estadoUI == null) {
            UIUtils.mostrarAdvertencia(dialogo, I18nUI.Configuracion.TITULO_VALIDACION(),
                I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR());
            return;
        }

        String urlApi = estadoUI.getBaseUrl();
        if (Normalizador.esVacio(urlApi)) {
            UIUtils.mostrarAdvertencia(dialogo, I18nUI.Configuracion.TITULO_VALIDACION(),
                I18nUI.Configuracion.MSG_URL_VACIA());
            JTextField txtUrl = dialogo.obtenerTxtUrl();
            if (txtUrl != null) {
                txtUrl.requestFocus();
            }
            return;
        }

        String claveApi = estadoUI.getApiKey().trim();
        String proveedorSeleccionado = providerManager.obtenerProveedorActual();
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorSeleccionado);
        if (configProveedor != null && configProveedor.requiereClaveApi()) {
            if (Normalizador.esVacio(claveApi)) {
                UIUtils.mostrarAdvertencia(dialogo, I18nUI.Configuracion.TITULO_VALIDACION(),
                    I18nUI.Configuracion.MSG_API_KEY_VACIA(proveedorSeleccionado));
                JPasswordField txtClave = dialogo.obtenerTxtClave();
                if (txtClave != null) {
                    txtClave.requestFocus();
                }
                return;
            }
        }

        JButton btnProbarConexion = dialogo.obtenerBtnProbarConexion();
        if (btnProbarConexion != null) {
            btnProbarConexion.setEnabled(false);
            btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBANDO());
        }

        SwingWorker<ProbadorConexionAI.ResultadoPrueba, Void> worker = new SwingWorker<>() {
            @Override
            protected ProbadorConexionAI.ResultadoPrueba doInBackground() {
                try {
                    ConfiguracionAPI configTemp = new ConfiguracionAPI();
                    configTemp.establecerProveedorAI(proveedorSeleccionado);
                    configTemp.establecerClaveApi(claveApi);
                    configTemp.establecerModelo(estadoUI.getModelo());
                    configTemp.establecerUrlApi(
                        ConfiguracionAPI.construirUrlApiProveedor(
                            proveedorSeleccionado,
                            urlApi,
                            estadoUI.getModelo()));
                    configTemp.establecerTiempoEsperaParaModelo(proveedorSeleccionado, 
                        estadoUI.getModelo(), estadoUI.getTimeout());

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
                        UIUtils.mostrarInfo(dialogo, I18nUI.Configuracion.TITULO_CONEXION_EXITOSA(),
                            resultado.mensaje);
                    } else {
                        UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR_CONEXION(),
                            resultado.mensaje);
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR(),
                        I18nUI.Configuracion.MSG_ERROR_PRUEBA_INESPERADO(e.getMessage()));
                } finally {
                    if (btnProbarConexion != null) {
                        btnProbarConexion.setEnabled(true);
                        btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBAR_CONEXION());
                    }
                }
            }
        };

        worker.execute();
    }

    public void manejarGuardarConfiguracion() {
        if (guardandoConfiguracion) {
            return;
        }

        JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
        if (txtPrompt != null && !txtPrompt.getText().contains("{REQUEST}")) {
            UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                I18nUI.Configuracion.MSG_ERROR_PROMPT_SIN_REQUEST());
            return;
        }

        String proveedorSeleccionado = (String) dialogo.obtenerComboProveedor().getSelectedItem();
        if (Normalizador.esVacio(proveedorSeleccionado)) {
            UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                I18nUI.Configuracion.MSG_SELECCIONA_PROVEEDOR());
            return;
        }

        EstadoProveedorUI estadoUI = providerManager.extraerEstadoActual();
        if (estadoUI == null) {
            return;
        }

        JButton btnGuardar = dialogo.obtenerBtnGuardar();
        if (btnGuardar != null) {
            btnGuardar.setEnabled(false);
            btnGuardar.setText(I18nUI.Configuracion.BOTON_GUARDANDO());
        }
        guardandoConfiguracion = true;

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMsg = "";
            private ConfiguracionAPI snapshot;

            @Override
            protected Boolean doInBackground() {
                StringBuilder errorBuilder = new StringBuilder();
                try {
                    snapshot = config.crearSnapshot();
                    aplicarConfiguracionASnapshot(snapshot, estadoUI, proveedorSeleccionado);

                    Map<String, String> errores = snapshot.validar();
                    if (!errores.isEmpty()) {
                        errorBuilder.append(construirMensajeErroresValidacion(errores));
                        errorMsg = errorBuilder.toString();
                        return false;
                    }

                    boolean guardado = persistenceManager.guardarConfiguracion(snapshot, errorBuilder);
                    errorMsg = errorBuilder.toString();
                    return guardado;
                } catch (Exception e) {
                    errorBuilder.append(e.getMessage());
                    errorMsg = errorBuilder.toString();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        config.aplicarDesde(snapshot);
                        EstilosUI.actualizarFuentes(config);
                        persistenceManager.actualizarEstadoReferencia();
                        
                        java.lang.Runnable alGuardar = dialogo.obtenerAlGuardar();
                        if (alGuardar != null) {
                            alGuardar.run();
                        }
                        dialogo.dispose();
                    } else {
                        UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR_GUARDAR(),
                            errorMsg);
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR_GUARDAR(),
                        e.getMessage());
                } finally {
                    if (btnGuardar != null) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText(I18nUI.Configuracion.BOTON_GUARDAR());
                    }
                    guardandoConfiguracion = false;
                }
            }
        };
        worker.execute();
    }

    private void aplicarConfiguracionASnapshot(ConfiguracionAPI snapshot, 
                                            EstadoProveedorUI estadoUI, 
                                            String proveedorSeleccionado) {
        JComboBox<IdiomaUI> comboIdioma = dialogo.obtenerComboIdioma();
        if (comboIdioma != null) {
            IdiomaUI idiomaSeleccionado = (IdiomaUI) comboIdioma.getSelectedItem();
            snapshot.establecerIdiomaUi(
                idiomaSeleccionado != null ? idiomaSeleccionado.codigo() : IdiomaUI.porDefecto().codigo());
        }

        snapshot.establecerProveedorAI(proveedorSeleccionado);

        Map<String, EstadoProveedorUI> estadosProveedorGuardar = providerManager.obtenerConfiguracionParaGuardar();
        estadosProveedorGuardar.put(proveedorSeleccionado, estadoUI);

        for (Map.Entry<String, EstadoProveedorUI> entry : estadosProveedorGuardar.entrySet()) {
            String nombreProveedor = entry.getKey();
            EstadoProveedorUI estadoProveedor = entry.getValue();

            if (estadoProveedor != null && Normalizador.noEsVacio(nombreProveedor)) {
                snapshot.establecerApiKeyParaProveedor(nombreProveedor, estadoProveedor.getApiKey());
                snapshot.establecerModeloParaProveedor(nombreProveedor, estadoProveedor.getModelo());
                snapshot.establecerUrlBaseParaProveedor(nombreProveedor, estadoProveedor.getBaseUrl());
                snapshot.establecerMaxTokensParaProveedor(nombreProveedor, estadoProveedor.getMaxTokens());
                snapshot.establecerTiempoEsperaParaModelo(nombreProveedor, 
                    estadoProveedor.getModelo(), estadoProveedor.getTimeout());
            }
        }

        JCheckBox chkDetallado = dialogo.obtenerChkDetallado();
        if (chkDetallado != null) {
            snapshot.establecerDetallado(chkDetallado.isSelected());
        }

        JCheckBox chkIgnorarSSL = dialogo.obtenerChkIgnorarSSL();
        if (chkIgnorarSSL != null) {
            snapshot.establecerIgnorarErroresSSL(chkIgnorarSSL.isSelected());
        }

        JCheckBox chkSoloProxy = dialogo.obtenerChkSoloProxy();
        if (chkSoloProxy != null) {
            snapshot.establecerSoloProxy(chkSoloProxy.isSelected());
        }

        JCheckBox chkAlertasHabilitadas = dialogo.obtenerChkAlertasHabilitadas();
        if (chkAlertasHabilitadas != null) {
            snapshot.establecerAlertasHabilitadas(chkAlertasHabilitadas.isSelected());
        }

        JCheckBox chkPersistirBusqueda = dialogo.obtenerChkPersistirBusqueda();
        if (chkPersistirBusqueda != null) {
            snapshot.establecerPersistirFiltroBusquedaHallazgos(chkPersistirBusqueda.isSelected());
        }

        JCheckBox chkPersistirSeveridad = dialogo.obtenerChkPersistirSeveridad();
        if (chkPersistirSeveridad != null) {
            snapshot.establecerPersistirFiltroSeveridadHallazgos(chkPersistirSeveridad.isSelected());
        }

        JTextField txtRetraso = dialogo.obtenerTxtRetraso();
        if (txtRetraso != null) {
            ConfigValidator.ValidationResultEntero resultadoRetraso = 
                ConfigValidator.validarEntero(txtRetraso.getText(), "retrasoSegundos", 
                    ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS, ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS);
            if (resultadoRetraso.esValido()) {
                snapshot.establecerRetrasoSegundos(resultadoRetraso.getValor());
            }
        }

        JTextField txtMaximoConcurrente = dialogo.obtenerTxtMaximoConcurrente();
        if (txtMaximoConcurrente != null) {
            ConfigValidator.ValidationResultEntero resultadoConcurrente = 
                ConfigValidator.validarEntero(txtMaximoConcurrente.getText(), "maximoConcurrente",
                    ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE, ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE);
            if (resultadoConcurrente.esValido()) {
                snapshot.establecerMaximoConcurrente(resultadoConcurrente.getValor());
            }
        }

        JTextField txtMaximoHallazgosTabla = dialogo.obtenerTxtMaximoHallazgosTabla();
        if (txtMaximoHallazgosTabla != null) {
            ConfigValidator.ValidationResultEntero resultadoHallazgos = 
                ConfigValidator.validarEntero(txtMaximoHallazgosTabla.getText(), "maximoHallazgos",
                    ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA, ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA);
            if (resultadoHallazgos.esValido()) {
                snapshot.establecerMaximoHallazgosTabla(resultadoHallazgos.getValor());
            }
        }

        JTextField txtMaximoTareas = dialogo.obtenerTxtMaximoTareas();
        if (txtMaximoTareas != null) {
            ConfigValidator.ValidationResultEntero resultadoTareas = 
                ConfigValidator.validarEntero(txtMaximoTareas.getText(), "maximoTareas",
                    ConfiguracionAPI.MINIMO_TAREAS_TABLA, ConfiguracionAPI.MAXIMO_TAREAS_TABLA);
            if (resultadoTareas.esValido()) {
                snapshot.establecerMaximoTareasTabla(resultadoTareas.getValor());
            }
        }

        JComboBox<String> comboAgente = dialogo.obtenerComboAgente();
        JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
        JTextArea txtAgentePromptInicial = dialogo.obtenerTxtAgentePromptInicial();
        JTextArea txtAgentePrompt = dialogo.obtenerTxtAgentePrompt();

        if (comboAgente != null) {
            snapshot.establecerTipoAgente((String) comboAgente.getSelectedItem());
        }
        if (chkAgenteHabilitado != null) {
            snapshot.establecerAgenteHabilitado(chkAgenteHabilitado.isSelected());
        }
        if (txtAgentePromptInicial != null) {
            snapshot.establecerAgentePreflightPrompt(txtAgentePromptInicial.getText());
        }
        if (txtAgentePrompt != null) {
            snapshot.establecerAgentePrompt(txtAgentePrompt.getText());
        }

        for (Map.Entry<String, String> entry : rutasBinarioAgenteTemporal.entrySet()) {
            snapshot.establecerRutaBinarioAgente(entry.getKey(), entry.getValue());
        }

        JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
        if (txtPrompt != null) {
            String promptActual = txtPrompt.getText();
            String promptPorDefecto = ConfiguracionAPI.obtenerPromptPorDefecto();
            snapshot.establecerPromptConfigurable(promptActual);
            snapshot.establecerPromptModificado(!promptActual.equals(promptPorDefecto));
        }

        JComboBox<String> comboFuenteEstandar = dialogo.obtenerComboFuenteEstandar();
        JSpinner spinnerTamanioEstandar = dialogo.obtenerSpinnerTamanioEstandar();
        JComboBox<String> comboFuenteMono = dialogo.obtenerComboFuenteMono();
        JSpinner spinnerTamanioMono = dialogo.obtenerSpinnerTamanioMono();

        if (comboFuenteEstandar != null) {
            snapshot.establecerNombreFuenteEstandar((String) comboFuenteEstandar.getSelectedItem());
        }
        if (spinnerTamanioEstandar != null) {
            snapshot.establecerTamanioFuenteEstandar((int) spinnerTamanioEstandar.getValue());
        }
        if (comboFuenteMono != null) {
            snapshot.establecerNombreFuenteMono((String) comboFuenteMono.getSelectedItem());
        }
        if (spinnerTamanioMono != null) {
            snapshot.establecerTamanioFuenteMono((int) spinnerTamanioMono.getValue());
        }

        JCheckBox chkHabilitarMultiProveedor = dialogo.obtenerChkHabilitarMultiProveedor();
        if (chkHabilitarMultiProveedor != null) {
            snapshot.establecerMultiProveedorHabilitado(chkHabilitarMultiProveedor.isSelected());
        }
    }

    public void manejarCerrarDialogo() {
        if (guardandoConfiguracion) {
            return;
        }

        if (!tieneCambiosSinGuardar()) {
            dialogo.dispose();
            return;
        }

        boolean confirmarDescarte = UIUtils.confirmarAdvertencia(
            dialogo,
            I18nUI.Configuracion.TITULO_CONFIRMAR_DESCARTE_CAMBIOS_AJUSTES(),
            I18nUI.Configuracion.MSG_CONFIRMAR_DESCARTE_CAMBIOS_AJUSTES());
        if (confirmarDescarte) {
            dialogo.dispose();
        }
    }

    private void manejarVerificarActualizaciones() {
        JButton btnBuscarActualizaciones = dialogo.obtenerBtnBuscarActualizaciones();
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
                            dialogo,
                            I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                            I18nUI.Configuracion.MSG_ACTUALIZACION_DISPONIBLE(
                                resultado.versionActual,
                                resultado.versionRemota,
                                VersionBurpIA.URL_DESCARGA));
                    } else {
                        UIUtils.mostrarInfo(
                            dialogo,
                            I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                            I18nUI.Configuracion.MSG_VERSION_AL_DIA(resultado.versionActual));
                    }
                } catch (Exception e) {
                    UIUtils.mostrarError(
                        dialogo,
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

    private void manejarAbrirSitioWeb() {
        String url = I18nUI.Configuracion.URL_SITIO_WEB();
        UIUtils.abrirUrlConFallbackInfo(dialogo, I18nUI.Configuracion.TITULO_ENLACE(), url,
            I18nUI.Configuracion.MSG_URL(url));
    }

    private void manejarRestaurarPromptPorDefecto() {
        boolean confirmacion = UIUtils.confirmarAdvertencia(
            dialogo,
            I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
            I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_PROMPT());

        if (confirmacion) {
            JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
            if (txtPrompt != null) {
                txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
                actualizarContadorPrompt();
            }
        }
    }

    private void manejarRestaurarPromptAgenteInicial() {
        JTextArea txtAgentePromptInicial = dialogo.obtenerTxtAgentePromptInicial();
        if (txtAgentePromptInicial != null) {
            txtAgentePromptInicial.setText(ConfiguracionAPI.obtenerAgentePreflightPromptPorDefecto());
        }
    }

    private void manejarRestaurarPromptAgente() {
        JTextArea txtAgentePrompt = dialogo.obtenerTxtAgentePrompt();
        if (txtAgentePrompt != null) {
            txtAgentePrompt.setText(ConfiguracionAPI.obtenerAgentePromptPorDefecto());
        }
    }

    private void manejarRestaurarFuentesPorDefecto() {
        int confirm = JOptionPane.showConfirmDialog(
            dialogo,
            I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_FUENTES(),
            I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            JComboBox<String> comboFuenteEstandar = dialogo.obtenerComboFuenteEstandar();
            JSpinner spinnerTamanioEstandar = dialogo.obtenerSpinnerTamanioEstandar();
            JComboBox<String> comboFuenteMono = dialogo.obtenerComboFuenteMono();
            JSpinner spinnerTamanioMono = dialogo.obtenerSpinnerTamanioMono();

            if (comboFuenteEstandar != null) {
                comboFuenteEstandar.setSelectedItem(ConfiguracionAPI.FUENTE_ESTANDAR_DEFECTO);
            }
            if (spinnerTamanioEstandar != null) {
                spinnerTamanioEstandar.setValue(ConfiguracionAPI.TAMANIO_FUENTE_ESTANDAR_DEFECTO);
            }
            if (comboFuenteMono != null) {
                comboFuenteMono.setSelectedItem(ConfiguracionAPI.FUENTE_MONO_DEFECTO);
            }
            if (spinnerTamanioMono != null) {
                spinnerTamanioMono.setValue(ConfiguracionAPI.TAMANIO_FUENTE_MONO_DEFECTO);
            }
        }
    }

    private void actualizarContadorPrompt() {
        JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
        JLabel lblContadorPrompt = dialogo.obtenerLblContadorPrompt();
        
        if (txtPrompt == null || lblContadorPrompt == null) {
            return;
        }

        int longitud = txtPrompt.getText().length();
        lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_CARACTERES(longitud));

        if (!txtPrompt.getText().contains("{REQUEST}")) {
            lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_FALTA_REQUEST(longitud));
            lblContadorPrompt.setForeground(EstilosUI.colorErrorAccesible(EstilosUI.obtenerFondoPanel()));
        } else {
            lblContadorPrompt.setForeground(EstilosUI.colorTextoPrimario(EstilosUI.obtenerFondoPanel()));
        }
    }

    private void actualizarRutaEnMemoria() {
        if (actualizandoRutaFlag) {
            return;
        }
        String agenteSeleccionado = (String) dialogo.obtenerComboAgente().getSelectedItem();
        if (agenteSeleccionado != null) {
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
            if (txtAgenteBinario != null) {
                rutasBinarioAgenteTemporal.put(agenteSeleccionado, txtAgenteBinario.getText().trim());
            }
        }
    }

    public void actualizarTimeoutModeloSeleccionado() {
        JTextField txtTimeoutModelo = dialogo.obtenerTxtTimeoutModelo();
        if (txtTimeoutModelo == null) {
            return;
        }

        String proveedorSeleccionado = providerManager.obtenerProveedorActual();
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

    private String resolverRutaBinarioAgente(String agenteSeleccionado) {
        if (Normalizador.esVacio(agenteSeleccionado)) {
            return "";
        }
        String rutaTemporal = rutasBinarioAgenteTemporal.get(agenteSeleccionado);
        if (Normalizador.noEsVacio(rutaTemporal)) {
            return rutaTemporal;
        }
        String rutaGuardada = config.obtenerRutaBinarioAgente(agenteSeleccionado);
        return rutaGuardada != null ? rutaGuardada : "";
    }

    private String obtenerModeloSeleccionado() {
        JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
        if (comboModelo == null) {
            return "";
        }

        String modeloActual = (String) comboModelo.getSelectedItem();
        if (modeloActual == null) {
            return "";
        }
        if (!esOpcionModeloCustom(modeloActual)) {
            return normalizarModeloSeleccionado(modeloActual);
        }
        Object editorValue = comboModelo.getEditor().getItem();
        return editorValue != null ? normalizarModeloSeleccionado(editorValue.toString()) : "";
    }

    private boolean esOpcionModeloCustom(String valor) {
        return I18nUI.Configuracion.OPCION_MODELO_CUSTOM().equals(valor);
    }

    private String normalizarModeloSeleccionado(String modelo) {
        if (Normalizador.esVacio(modelo)) {
            return "";
        }
        String limpio = modelo.trim();
        return ":".equals(limpio) ? "" : limpio;
    }

    private List<String> normalizarModelos(List<String> modelos) {
        List<String> unicos = new ArrayList<>();
        if (modelos != null) {
            for (String modelo : modelos) {
                String limpio = normalizarModeloSeleccionado(modelo);
                if (!limpio.isEmpty() && !unicos.contains(limpio)) {
                    unicos.add(limpio);
                }
            }
        }
        return unicos;
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
        JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
        if (comboModelo == null) {
            return;
        }

        comboModelo.removeAllItems();
        comboModelo.addItem(I18nUI.Configuracion.OPCION_MODELO_CUSTOM());

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

        comboModelo.setSelectedItem(I18nUI.Configuracion.OPCION_MODELO_CUSTOM());
        comboModelo.getEditor().setItem(preferidoNormalizado);
        actualizarTimeoutModeloSeleccionado();
    }

    private List<String> obtenerModelosDesdeAPI(String proveedor) {
        okhttp3.OkHttpClient cliente = crearClienteModelos();
        if ("Gemini".equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosGemini(
                    dialogo.obtenerTxtUrl().getText().trim(),
                    new String(dialogo.obtenerTxtClave().getPassword()).trim(),
                    cliente);
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_GEMINI() + e.getMessage(), e);
            }
        }
        if ("Ollama".equals(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOllama(
                    dialogo.obtenerTxtUrl().getText().trim(),
                    cliente);
            } catch (Exception e) {
                throw new RuntimeException(I18nUI.Configuracion.ERROR_OBTENER_OLLAMA() + e.getMessage(), e);
            }
        }
        if ("OpenAI".equals(proveedor) || "Z.ai".equals(proveedor) || "minimax".equals(proveedor)
                || ProveedorAI.esProveedorCustom(proveedor)) {
            try {
                return ConstructorSolicitudesProveedor.listarModelosOpenAI(
                    dialogo.obtenerTxtUrl().getText().trim(),
                    new String(dialogo.obtenerTxtClave().getPassword()).trim(),
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

    public boolean tieneCambiosSinGuardar() {
        return persistenceManager.hayCambiosPendientes();
    }

    private String construirMensajeErroresValidacion(java.util.Map<String, String> errores) {
        StringBuilder mensaje = new StringBuilder(I18nUI.Configuracion.MSG_CORRIGE_CAMPOS());
        errores.values().forEach(valor -> mensaje.append(" - ").append(valor).append("\n"));
        return mensaje.toString().trim();
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
}