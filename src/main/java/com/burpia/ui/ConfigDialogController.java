package com.burpia.ui;

import com.burpia.config.*;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
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
    private final DialogStateManager dialogStateManager;
    private final Map<String, String> rutasBinarioAgenteTemporal;
    private final AtomicLong secuenciaRefrescoModelos;
    private final String tooltipCargarModelosPorDefecto;

    private boolean guardandoConfiguracion = false;
    private boolean actualizandoRutaFlag = false;

    private static final class ValoresNumericosConfiguracion {
        private final int retrasoSegundos;
        private final int maximoConcurrente;
        private final int maximoHallazgosTabla;
        private final int maximoTareas;

        private ValoresNumericosConfiguracion(int retrasoSegundos,
                                              int maximoConcurrente,
                                              int maximoHallazgosTabla,
                                              int maximoTareas) {
            this.retrasoSegundos = retrasoSegundos;
            this.maximoConcurrente = maximoConcurrente;
            this.maximoHallazgosTabla = maximoHallazgosTabla;
            this.maximoTareas = maximoTareas;
        }
    }

    private static final class ResultadoValidacionEnteroDialogo {
        private final boolean valido;
        private final int valor;
        private final String mensajeError;
        private final String campo;

        private ResultadoValidacionEnteroDialogo(boolean valido, int valor, String mensajeError, String campo) {
            this.valido = valido;
            this.valor = valor;
            this.mensajeError = mensajeError;
            this.campo = campo;
        }

        private static ResultadoValidacionEnteroDialogo valido(int valor) {
            return new ResultadoValidacionEnteroDialogo(true, valor, null, null);
        }

        private static ResultadoValidacionEnteroDialogo invalido(String mensajeError, String campo) {
            return new ResultadoValidacionEnteroDialogo(false, 0, mensajeError, campo);
        }
    }

    public ConfigDialogController(DialogoConfiguracion dialogo, 
                                ConfiguracionAPI config, 
                                GestorConfiguracion gestorConfig) {
        if (dialogo == null || config == null || gestorConfig == null) {
            throw new IllegalArgumentException("Dialogo, configuración y gestorConfig no pueden ser nulos");
        }

        this.dialogo = dialogo;
        this.config = config;
        this.gestorConfig = gestorConfig;
        // Usar gestorLogging sin PrintWriter - usa Logger interno en lugar de System.out/System.err
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(null, null);
        this.persistenceManager = new ConfigPersistenceManager(gestorConfig, gestorLogging);
        this.providerManager = new ProviderConfigManager(config, gestorLogging);
        this.dialogStateManager = new DialogStateManager(gestorLogging);
        this.rutasBinarioAgenteTemporal = new HashMap<>();
        this.secuenciaRefrescoModelos = new AtomicLong(0);
        this.tooltipCargarModelosPorDefecto = I18nUI.Tooltips.Configuracion.CARGAR_MODELOS();

        gestorLogging.info(ORIGEN_LOG, "ConfigDialogController inicializado");
    }

    /**
     * Configura los componentes de UI en el ProviderConfigManager.
     * Esto es necesario para que el manager pueda extraer el estado actual de la UI.
     */
    private void configurarComponentesUIProvider() {
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
            dialogo.obtenerModeloListaSeleccionados(),
            dialogo.obtenerListaProveedoresDisponibles(),
            dialogo.obtenerListaProveedoresSeleccionados()
        );
    }

    public void inicializarEventHandlers() {
        gestorLogging.info(ORIGEN_LOG, "Inicializando manejadores de eventos");
        
        // Configurar componentes de UI primero
        configurarComponentesUIProvider();
        
        inicializarEventHandlersProvider();
        inicializarEventHandlersAgent();
        inicializarEventHandlersAcciones();
        inicializarEventHandlersDocumentos();
        
        gestorLogging.info(ORIGEN_LOG, "Manejadores de eventos inicializados");
    }

    private void inicializarEventHandlersProvider() {
        agregarListenerSiPresente(dialogo.obtenerComboProveedor(), e -> actualizarEstadoCargaModelosSegunProveedor());
        agregarListenerSiPresente(dialogo.obtenerComboModelo(), e -> manejarCambioModelo());
        agregarListenerSiPresente(dialogo.obtenerBtnRefrescarModelos(), e -> manejarRefrescarModelos());
        agregarListenerSiPresente(dialogo.obtenerBtnProbarConexion(), e -> manejarProbarConexion());
    }

    private void inicializarEventHandlersAgent() {
        agregarListenerSiPresente(dialogo.obtenerComboAgente(), e -> manejarCambioAgente());
        agregarListenerSiPresente(dialogo.obtenerBtnRestaurarPromptAgenteInicial(), e -> manejarRestaurarPromptAgenteInicial());
        agregarListenerSiPresente(dialogo.obtenerBtnRestaurarPromptAgente(), e -> manejarRestaurarPromptAgente());
    }

    private void inicializarEventHandlersAcciones() {
        agregarListenerSiPresente(dialogo.obtenerBtnGuardar(), e -> manejarGuardarConfiguracion());
        agregarListenerSiPresente(dialogo.obtenerBtnCerrar(), e -> manejarCerrarDialogo());
        agregarListenerSiPresente(dialogo.obtenerBtnBuscarActualizaciones(), e -> manejarVerificarActualizaciones());
        agregarListenerSiPresente(dialogo.obtenerBtnSitioWeb(), e -> manejarAbrirSitioWeb());
        agregarListenerSiPresente(dialogo.obtenerBtnRestaurarPrompt(), e -> manejarRestaurarPromptPorDefecto());
        agregarListenerSiPresente(dialogo.obtenerBtnRestaurarFuentes(), e -> manejarRestaurarFuentesPorDefecto());
    }

    private void inicializarEventHandlersDocumentos() {
        agregarDocumentListenerSiPresente(dialogo.obtenerTxtPrompt(), crearDocumentListenerPrompt());
        agregarDocumentListenerSiPresente(dialogo.obtenerTxtAgenteBinario(), crearDocumentListenerAgenteBinario());
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

    // ===== MÉTODOS DRY PARA EVENT HANDLERS =====

    /**
     * DRY: Agrega un ActionListener a un componente si no es null.
     */
    private void agregarListenerSiPresente(JButton boton, java.awt.event.ActionListener listener) {
        if (boton != null) {
            boton.addActionListener(listener);
        }
    }

    /**
     * DRY: Agrega un ActionListener a un combo box si no es null.
     */
    private void agregarListenerSiPresente(JComboBox<?> combo, java.awt.event.ActionListener listener) {
        if (combo != null) {
            combo.addActionListener(listener);
        }
    }

    /**
     * DRY: Agrega un DocumentListener a un JTextArea si no es null.
     */
    private void agregarDocumentListenerSiPresente(JTextArea textArea, javax.swing.event.DocumentListener listener) {
        if (textArea != null) {
            textArea.getDocument().addDocumentListener(listener);
        }
    }

    /**
     * DRY: Agrega un DocumentListener a un JTextField si no es null.
     */
    private void agregarDocumentListenerSiPresente(JTextField textField, javax.swing.event.DocumentListener listener) {
        if (textField != null) {
            textField.getDocument().addDocumentListener(listener);
        }
    }

    public void cargarConfiguracionInicial() {
        gestorLogging.info(ORIGEN_LOG, "Cargando configuración inicial");
        
        ConfiguracionAPI configCargada = persistenceManager.cargarConfiguracion();
        if (configCargada != null) {
            config.aplicarDesde(configCargada);
        }

        // Nota: providerManager.inicializarComponentesUI() ya se llamó en inicializarEventHandlers()
        // No duplicar la llamada aquí para evitar listeners duplicados
        
        providerManager.cargarConfiguracionInicial();
        
        rutasBinarioAgenteTemporal.clear();
        rutasBinarioAgenteTemporal.putAll(new HashMap<>(config.obtenerTodasLasRutasBinario()));

        cargarConfiguracionGeneral();
        cargarConfiguracionAgente();
        cargarConfiguracionFuentes();
        actualizarEstadoCargaModelosSegunProveedor();
        dialogStateManager.capturarEstadoInicial(new DialogoEstadoUIProvider());

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
            cargarAgentesConfigurables(comboAgente);
            
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
    
    private void cargarAgentesConfigurables(JComboBox<String> comboAgente) {
        comboAgente.removeAllItems();

        List<String> agentesDisponibles = new ArrayList<>();
        List<String> agentesPendientesConfig = new ArrayList<>();
        for (String codigoAgente : AgenteTipo.codigosDisponibles()) {
            if (esAgenteDisponible(codigoAgente)) {
                agentesDisponibles.add(codigoAgente);
            } else {
                agentesPendientesConfig.add(codigoAgente);
            }
        }

        agentesDisponibles.forEach(comboAgente::addItem);
        agentesPendientesConfig.forEach(comboAgente::addItem);
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

    private void manejarCambioModelo() {
        gestorLogging.info(ORIGEN_LOG, "Manejando cambio de modelo");
        SwingUtilities.invokeLater(providerManager::actualizarTimeoutModeloSeleccionadoEnUI);
    }

    private void manejarCambioAgente() {
        if (dialogo.obtenerComboAgente() == null) {
            return;
        }
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
        if (Normalizador.esVacio(proveedorSeleccionado)
                || !ProveedorAI.permiteCargaRemotaModelos(proveedorSeleccionado)) {
            actualizarEstadoCargaModelosSegunProveedor();
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
                    if (Normalizador.esVacia(modelos)) {
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
                        btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
                    }
                    actualizarEstadoCargaModelosSegunProveedor();
                }
            }
        };
        worker.execute();
    }

    private void actualizarEstadoCargaModelosSegunProveedor() {
        JButton btnRefrescarModelos = dialogo.obtenerBtnRefrescarModelos();
        if (btnRefrescarModelos == null) {
            return;
        }

        String proveedorSeleccionado = (String) dialogo.obtenerComboProveedor().getSelectedItem();
        boolean permiteCargaModelos = ProveedorAI.permiteCargaRemotaModelos(proveedorSeleccionado);
        btnRefrescarModelos.setEnabled(permiteCargaModelos);
        btnRefrescarModelos.setToolTipText(permiteCargaModelos
                ? tooltipCargarModelosPorDefecto
                : I18nUI.Tooltips.Configuracion.CARGAR_MODELOS_NO_DISPONIBLE_MINIMAX());
    }

    public void manejarProbarConexion() {
        EstadoProveedorUI estadoUI = obtenerEstadoProveedorValidado(true, true, false);
        if (estadoUI == null) {
            return;
        }

        String proveedorSeleccionado = providerManager.obtenerProveedorActual();
        String urlApi = estadoUI.getBaseUrl();
        String claveApi = estadoUI.getApiKey().trim();

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
        String textoPrompt = txtPrompt != null ? txtPrompt.getText() : "";
        if (Normalizador.noEsVacio(textoPrompt) && !textoPrompt.contains("{REQUEST}")) {
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

        EstadoProveedorUI estadoUI = obtenerEstadoProveedorValidado(false, false, true);
        if (estadoUI == null) {
            return;
        }

        ValoresNumericosConfiguracion valoresNumericos = obtenerValoresNumericosConfiguracionValidados();
        if (valoresNumericos == null) {
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
                    aplicarConfiguracionASnapshot(snapshot, estadoUI, proveedorSeleccionado, valoresNumericos);

                    Map<String, String> errores = snapshot.validar();
                    if (!errores.isEmpty()) {
                        errorBuilder.append(construirMensajeErroresValidacion(errores));
                        errorMsg = errorBuilder.toString();
                        return false;
                    }

                    Map<String, String> erroresMultiProveedor = validarMultiProveedor(snapshot);
                    if (!erroresMultiProveedor.isEmpty()) {
                        errorBuilder.append(construirMensajeErroresValidacion(erroresMultiProveedor));
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
                        dialogStateManager.capturarEstadoInicial(new DialogoEstadoUIProvider());
                        
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
                                               String proveedorSeleccionado,
                                               ValoresNumericosConfiguracion valoresNumericos) {
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

        snapshot.establecerRetrasoSegundos(valoresNumericos.retrasoSegundos);
        snapshot.establecerMaximoConcurrente(valoresNumericos.maximoConcurrente);
        snapshot.establecerMaximoHallazgosTabla(valoresNumericos.maximoHallazgosTabla);
        snapshot.establecerMaximoTareasTabla(valoresNumericos.maximoTareas);

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

            DefaultListModel<String> modeloListaSeleccionados = dialogo.obtenerModeloListaSeleccionados();
            if (modeloListaSeleccionados != null) {
                List<String> proveedoresSeleccionados = new ArrayList<>();
                for (int i = 0; i < modeloListaSeleccionados.getSize(); i++) {
                    proveedoresSeleccionados.add(modeloListaSeleccionados.getElementAt(i));
                }
                snapshot.establecerProveedoresMultiConsulta(proveedoresSeleccionados);
            }
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
        if (UIUtils.confirmarAdvertencia(
                dialogo,
                I18nUI.Configuracion.TITULO_CONFIRMAR_RESTAURACION(),
                I18nUI.Configuracion.MSG_CONFIRMAR_RESTAURAR_FUENTES())) {
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

        String textoPrompt = txtPrompt.getText();
        int longitud = textoPrompt.length();
        lblContadorPrompt.setText(I18nUI.Configuracion.CONTADOR_CARACTERES(longitud));

        if (Normalizador.noEsVacio(textoPrompt) && !textoPrompt.contains("{REQUEST}")) {
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
        providerManager.actualizarTimeoutModeloSeleccionadoEnUI();
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
        return providerManager.obtenerModeloSeleccionadoActual();
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
        providerManager.cargarModelosEnComboEnUI(modelos, preferido);
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
        if (Normalizador.esVacia(modelos)) {
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

    private EstadoProveedorUI obtenerEstadoProveedorValidado(boolean requiereUrlExplicita,
                                                             boolean validarApiKey,
                                                             boolean mostrarComoError) {
        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerManager.validarEstadoActual(requiereUrlExplicita, validarApiKey);
        if (resultado.esValido()) {
            return resultado.getEstado();
        }

        mostrarErrorValidacionProveedor(resultado, mostrarComoError);
        return null;
    }

    private ValoresNumericosConfiguracion obtenerValoresNumericosConfiguracionValidados() {
        ResultadoValidacionEnteroDialogo retraso = validarCampoEntero(
                dialogo.obtenerTxtRetraso(),
                "retrasoSegundos",
                limpiarEtiquetaCampo(I18nUI.Configuracion.LABEL_RETRASO()),
                ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS,
                ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS);
        if (!retraso.valido) {
            mostrarErrorValidacionGenerica(retraso.mensajeError, retraso.campo);
            return null;
        }

        ResultadoValidacionEnteroDialogo maximoConcurrente = validarCampoEntero(
                dialogo.obtenerTxtMaximoConcurrente(),
                "maximoConcurrente",
                limpiarEtiquetaCampo(I18nUI.Configuracion.LABEL_MAXIMO_CONCURRENTE()),
                ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE,
                ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE);
        if (!maximoConcurrente.valido) {
            mostrarErrorValidacionGenerica(maximoConcurrente.mensajeError, maximoConcurrente.campo);
            return null;
        }

        ResultadoValidacionEnteroDialogo maximoHallazgos = validarCampoEntero(
                dialogo.obtenerTxtMaximoHallazgosTabla(),
                "maximoHallazgos",
                limpiarEtiquetaCampo(I18nUI.Configuracion.LABEL_MAX_HALLAZGOS_TABLA()),
                ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA,
                ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA);
        if (!maximoHallazgos.valido) {
            mostrarErrorValidacionGenerica(maximoHallazgos.mensajeError, maximoHallazgos.campo);
            return null;
        }

        ResultadoValidacionEnteroDialogo maximoTareas = validarCampoEntero(
                dialogo.obtenerTxtMaximoTareas(),
                "maximoTareas",
                limpiarEtiquetaCampo(I18nUI.Configuracion.LABEL_MAXIMO_TAREAS()),
                ConfiguracionAPI.MINIMO_TAREAS_TABLA,
                ConfiguracionAPI.MAXIMO_TAREAS_TABLA);
        if (!maximoTareas.valido) {
            mostrarErrorValidacionGenerica(maximoTareas.mensajeError, maximoTareas.campo);
            return null;
        }

        return new ValoresNumericosConfiguracion(
                retraso.valor,
                maximoConcurrente.valor,
                maximoHallazgos.valor,
                maximoTareas.valor);
    }

    private ResultadoValidacionEnteroDialogo validarCampoEntero(JTextField campoTexto,
                                                                String campo,
                                                                String nombreVisible,
                                                                int minimo,
                                                                int maximo) {
        ConfigValidator.ValidationResultEntero resultado = ConfigValidator.validarEntero(
                obtenerTexto(campoTexto),
                nombreVisible,
                minimo,
                maximo);
        if (!resultado.esValido()) {
            return ResultadoValidacionEnteroDialogo.invalido(resultado.getMensajeError(), campo);
        }
        return ResultadoValidacionEnteroDialogo.valido(resultado.getValor());
    }

    private String limpiarEtiquetaCampo(String etiqueta) {
        if (Normalizador.esVacio(etiqueta)) {
            return "";
        }
        String limpia = etiqueta.trim();
        if (limpia.endsWith(":")) {
            limpia = limpia.substring(0, limpia.length() - 1).trim();
        }
        return limpia;
    }

    private void mostrarErrorValidacionProveedor(ProviderConfigManager.ValidationResultEstadoProveedor resultado,
                                                 boolean mostrarComoError) {
        if (resultado == null || Normalizador.esVacio(resultado.getMensajeError())) {
            return;
        }

        if (mostrarComoError) {
            UIUtils.mostrarError(dialogo,
                    I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    resultado.getMensajeError());
        } else {
            UIUtils.mostrarAdvertencia(dialogo,
                    I18nUI.Configuracion.TITULO_VALIDACION(),
                    resultado.getMensajeError());
        }
        enfocarCampoConfiguracion(resultado.getCampo());
    }

    private void mostrarErrorValidacionGenerica(String mensajeError, String campo) {
        if (Normalizador.esVacio(mensajeError)) {
            return;
        }
        UIUtils.mostrarError(dialogo,
                I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                mensajeError);
        enfocarCampoConfiguracion(campo);
    }

    private void enfocarCampoConfiguracion(String campo) {
        if (Normalizador.esVacio(campo)) {
            return;
        }

        switch (campo) {
            case "proveedor":
                enfocar(dialogo.obtenerComboProveedor());
                break;
            case "modelo":
                enfocar(dialogo.obtenerComboModelo());
                break;
            case "url":
                enfocar(dialogo.obtenerTxtUrl());
                break;
            case "apiKey":
                enfocar(dialogo.obtenerTxtClave());
                break;
            case "timeout":
                enfocar(dialogo.obtenerTxtTimeoutModelo());
                break;
            case "maxTokens":
                enfocar(dialogo.obtenerTxtMaxTokens());
                break;
            case "retrasoSegundos":
                enfocar(dialogo.obtenerTxtRetraso());
                break;
            case "maximoConcurrente":
                enfocar(dialogo.obtenerTxtMaximoConcurrente());
                break;
            case "maximoHallazgos":
                enfocar(dialogo.obtenerTxtMaximoHallazgosTabla());
                break;
            case "maximoTareas":
                enfocar(dialogo.obtenerTxtMaximoTareas());
                break;
            default:
                break;
        }
    }

    private void enfocar(Component componente) {
        if (componente != null) {
            componente.requestFocusInWindow();
        }
    }

    public boolean tieneCambiosSinGuardar() {
        return dialogStateManager.hayCambiosNoGuardados(new DialogoEstadoUIProvider());
    }

    private String construirMensajeErroresValidacion(java.util.Map<String, String> errores) {
        StringBuilder mensaje = new StringBuilder(I18nUI.Configuracion.MSG_CORRIGE_CAMPOS());
        errores.values().forEach(valor -> mensaje.append(" - ").append(valor).append("\n"));
        return mensaje.toString().trim();
    }

    private final class DialogoEstadoUIProvider implements DialogStateManager.EstadoUIProvider {
        @Override
        public String obtenerProveedorSeleccionado() {
            return obtenerTextoSeleccionado(dialogo.obtenerComboProveedor());
        }

        @Override
        public String obtenerAgenteSeleccionado() {
            return obtenerTextoSeleccionado(dialogo.obtenerComboAgente());
        }

        @Override
        public String obtenerRutaBinarioAgente() {
            JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
            return txtAgenteBinario != null ? txtAgenteBinario.getText() : "";
        }

        @Override
        public EstadoProveedorUI obtenerEstadoProveedorActual() {
            return providerManager.extraerEstadoActualRapido();
        }

        @Override
        public Map<String, String> obtenerRutasBinarioAgenteTemporales() {
            return new HashMap<>(rutasBinarioAgenteTemporal);
        }

        @Override
        public Map<String, EstadoProveedorUI> obtenerEstadosProveedorTemporales() {
            return providerManager.obtenerEstadosProveedorTemporales();
        }

        @Override
        public List<String> obtenerProveedoresMultiSeleccionados() {
            List<String> proveedores = new ArrayList<>();
            DefaultListModel<String> modeloListaSeleccionados = dialogo.obtenerModeloListaSeleccionados();
            if (modeloListaSeleccionados == null) {
                return proveedores;
            }
            for (int i = 0; i < modeloListaSeleccionados.size(); i++) {
                proveedores.add(modeloListaSeleccionados.getElementAt(i));
            }
            return proveedores;
        }

        @Override
        public String obtenerCodigoIdiomaSeleccionado() {
            IdiomaUI idioma = obtenerValorSeleccionado(dialogo.obtenerComboIdioma(), IdiomaUI.class);
            return idioma != null ? idioma.codigo() : config.obtenerIdiomaUi();
        }

        @Override
        public String obtenerModeloSeleccionado() {
            return ConfigDialogController.this.obtenerModeloSeleccionado();
        }

        @Override
        public String obtenerUrlActual() {
            return obtenerTexto(dialogo.obtenerTxtUrl());
        }

        @Override
        public String obtenerApiKeyActual() {
            JPasswordField txtClave = dialogo.obtenerTxtClave();
            return txtClave != null ? new String(txtClave.getPassword()) : "";
        }

        @Override
        public String obtenerMaxTokensTexto() {
            return obtenerTexto(dialogo.obtenerTxtMaxTokens());
        }

        @Override
        public String obtenerTimeoutTexto() {
            return obtenerTexto(dialogo.obtenerTxtTimeoutModelo());
        }

        @Override
        public String obtenerRetrasoTexto() {
            return obtenerTexto(dialogo.obtenerTxtRetraso());
        }

        @Override
        public String obtenerMaximoConcurrenteTexto() {
            return obtenerTexto(dialogo.obtenerTxtMaximoConcurrente());
        }

        @Override
        public String obtenerMaximoHallazgosTexto() {
            return obtenerTexto(dialogo.obtenerTxtMaximoHallazgosTabla());
        }

        @Override
        public String obtenerMaximoTareasTexto() {
            return obtenerTexto(dialogo.obtenerTxtMaximoTareas());
        }

        @Override
        public boolean esDetalladoSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkDetallado());
        }

        @Override
        public boolean esIgnorarSslSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkIgnorarSSL());
        }

        @Override
        public boolean esSoloProxySeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkSoloProxy());
        }

        @Override
        public boolean esAlertasHabilitadasSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkAlertasHabilitadas());
        }

        @Override
        public boolean esPersistirBusquedaSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkPersistirBusqueda());
        }

        @Override
        public boolean esPersistirSeveridadSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkPersistirSeveridad());
        }

        @Override
        public String obtenerPromptActual() {
            JTextArea txtPrompt = dialogo.obtenerTxtPrompt();
            return txtPrompt != null ? txtPrompt.getText() : "";
        }

        @Override
        public boolean esAgenteHabilitadoSeleccionado() {
            return estaSeleccionado(dialogo.obtenerChkAgenteHabilitado());
        }

        @Override
        public String obtenerAgentePromptInicial() {
            JTextArea txtAgentePromptInicial = dialogo.obtenerTxtAgentePromptInicial();
            return txtAgentePromptInicial != null ? txtAgentePromptInicial.getText() : "";
        }

        @Override
        public String obtenerAgentePrompt() {
            JTextArea txtAgentePrompt = dialogo.obtenerTxtAgentePrompt();
            return txtAgentePrompt != null ? txtAgentePrompt.getText() : "";
        }

        @Override
        public String obtenerFuenteEstandar() {
            return obtenerTextoSeleccionado(dialogo.obtenerComboFuenteEstandar());
        }

        @Override
        public int obtenerTamanioFuenteEstandar() {
            return obtenerValorSpinner(dialogo.obtenerSpinnerTamanioEstandar(), config.obtenerTamanioFuenteEstandar());
        }

        @Override
        public String obtenerFuenteMono() {
            return obtenerTextoSeleccionado(dialogo.obtenerComboFuenteMono());
        }

        @Override
        public int obtenerTamanioFuenteMono() {
            return obtenerValorSpinner(dialogo.obtenerSpinnerTamanioMono(), config.obtenerTamanioFuenteMono());
        }

        @Override
        public boolean esMultiProveedorHabilitado() {
            return estaSeleccionado(dialogo.obtenerChkHabilitarMultiProveedor());
        }

        @Override
        public ConfiguracionAPI obtenerConfiguracion() {
            return config;
        }

        @Override
        public void actualizarProveedorEnUI(String nuevoProveedor) {
            providerManager.cambiarProveedor(nuevoProveedor);
        }
    }

    private String obtenerTexto(JTextField campo) {
        return campo != null ? campo.getText() : "";
    }

    private boolean estaSeleccionado(JCheckBox checkBox) {
        return checkBox != null && checkBox.isSelected();
    }

    private int obtenerValorSpinner(JSpinner spinner, int valorPorDefecto) {
        if (spinner == null) {
            return valorPorDefecto;
        }
        Object valor = spinner.getValue();
        return valor instanceof Number ? ((Number) valor).intValue() : valorPorDefecto;
    }

    private String obtenerTextoSeleccionado(JComboBox<?> comboBox) {
        Object valor = comboBox != null ? comboBox.getSelectedItem() : null;
        return valor != null ? valor.toString() : "";
    }

    private <T> T obtenerValorSeleccionado(JComboBox<?> comboBox, Class<T> tipo) {
        Object valor = comboBox != null ? comboBox.getSelectedItem() : null;
        return tipo.isInstance(valor) ? tipo.cast(valor) : null;
    }

    private Map<String, String> validarMultiProveedor(ConfiguracionAPI snapshot) {
        Map<String, String> erroresMultiProveedor = new HashMap<>();

        if (!snapshot.esMultiProveedorHabilitado()) {
            return erroresMultiProveedor;
        }

        List<String> proveedores = snapshot.obtenerProveedoresMultiConsulta();
        if (Normalizador.esVacia(proveedores)) {
            erroresMultiProveedor.put("multiProveedor",
                    I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_SIN_PROVEEDORES());
            return erroresMultiProveedor;
        }

        if (proveedores.size() < 2) {
            erroresMultiProveedor.put("multiProveedor",
                    I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_MENOS_DOS());
        }

        for (String proveedor : proveedores) {
            if (Normalizador.esVacio(proveedor)) {
                erroresMultiProveedor.put("multiProveedor",
                        I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_PROVEEDOR_VACIO());
                continue;
            }

            if (!ProveedorAI.existeProveedor(proveedor)) {
                erroresMultiProveedor.put("multiProveedor",
                        I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_PROVEEDOR_INVALIDO(proveedor));
                continue;
            }

            ProveedorAI.ConfiguracionProveedor configProveedor = 
                    ProveedorAI.obtenerProveedor(proveedor);

            if (configProveedor != null && configProveedor.requiereClaveApi()) {
                String apiKey = snapshot.obtenerApiKeyParaProveedor(proveedor);
                if (Normalizador.esVacio(apiKey)) {
                    erroresMultiProveedor.put("multiProveedor",
                            I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_SIN_API_KEY(proveedor));
                }
            }

            String modelo = snapshot.obtenerModeloParaProveedor(proveedor);
            if (Normalizador.esVacio(modelo)) {
                erroresMultiProveedor.put("multiProveedor",
                        I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_SIN_MODELO(proveedor));
            }
        }

        return erroresMultiProveedor;
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
