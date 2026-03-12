package com.burpia.ui;

import com.burpia.config.*;
import com.burpia.i18n.I18nLogs;
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

    private final DialogoConfiguracion dialogo;
    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final ConfigPersistenceManager persistenceManager;
    private final ProviderConfigManager providerManager;
    private final GestorLoggingUnificado gestorLogging;
    private final DialogStateManager dialogStateManager;
    private final ConnectionTester connectionTester;
    private final Map<String, String> rutasBinarioAgenteTemporal;
    private final Map<String, Boolean> estadosHabilitacionAgenteTemporal;
    private final AtomicLong secuenciaRefrescoModelos;
    private final String tooltipCargarModelosPorDefecto;

    private boolean guardandoConfiguracion = false;
    private boolean actualizandoRutaFlag = false;
    private String ultimoAgenteSeleccionado = null;

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
            throw new IllegalArgumentException(I18nUI.General.ERROR_DIALOGO_CONFIG_Y_GESTOR_NULOS());
        }

        this.dialogo = dialogo;
        this.config = config;
        this.gestorConfig = gestorConfig;
        // Usar gestorLogging sin PrintWriter - usa Logger interno en lugar de System.out/System.err
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(null, null);
        this.persistenceManager = new ConfigPersistenceManager(gestorConfig, gestorLogging);
        this.providerManager = new ProviderConfigManager(config, gestorLogging);
        this.dialogStateManager = new DialogStateManager(gestorLogging);
        this.connectionTester = new ConnectionTester();
        this.rutasBinarioAgenteTemporal = new HashMap<>();
        this.estadosHabilitacionAgenteTemporal = new HashMap<>();
        this.secuenciaRefrescoModelos = new AtomicLong(0);
        this.tooltipCargarModelosPorDefecto = I18nUI.Tooltips.Configuracion.CARGAR_MODELOS();

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("ConfigDialogController inicializado"));
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
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Inicializando manejadores de eventos"));
        
        // Configurar componentes de UI primero
        configurarComponentesUIProvider();
        
        inicializarEventHandlersProvider();
        inicializarEventHandlersAgent();
        inicializarEventHandlersAcciones();
        inicializarEventHandlersDocumentos();
        
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Manejadores de eventos inicializados"));
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
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Cargando configuración inicial"));
        
        ConfiguracionAPI configCargada = persistenceManager.cargarConfiguracion();
        if (configCargada != null) {
            config.aplicarDesde(configCargada);
        }

        // Nota: providerManager.inicializarComponentesUI() ya se llamó en inicializarEventHandlers()
        // No duplicar la llamada aquí para evitar listeners duplicados
        
        providerManager.cargarConfiguracionInicial();
        
        rutasBinarioAgenteTemporal.clear();
        rutasBinarioAgenteTemporal.putAll(new HashMap<>(config.obtenerTodasLasRutasBinario()));
        estadosHabilitacionAgenteTemporal.clear();
        estadosHabilitacionAgenteTemporal.putAll(new HashMap<>(config.obtenerEstadosHabilitacionAgentes()));
        ultimoAgenteSeleccionado = null;

        cargarConfiguracionGeneral();
        cargarConfiguracionAgente();
        cargarConfiguracionFuentes();
        actualizarEstadoCargaModelosSegunProveedor();
        dialogStateManager.capturarEstadoInicial(new DialogoEstadoUIProvider());

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Configuración inicial cargada"));
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
            chkAgenteHabilitado.setSelected(resolverEstadoHabilitacionAgente(config.obtenerTipoAgente()));
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
        return config.tieneBinarioAgenteDisponible(codigoAgente);
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
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Manejando cambio de modelo"));
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
            guardarEstadoHabilitacionAgenteActual(ultimoAgenteSeleccionado);
            AgenteTipo enumAgente = AgenteTipo.desdeCodigo(agenteSeleccionado, null);
            if (enumAgente != null) {
                JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
                if (chkAgenteHabilitado != null) {
                    chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(enumAgente.obtenerNombreVisible()));
                    chkAgenteHabilitado.setSelected(resolverEstadoHabilitacionAgente(agenteSeleccionado));
                }

                String rutaSeleccionada = resolverRutaBinarioAgente(agenteSeleccionado);
                JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
                if (txtAgenteBinario != null) {
                    if (Normalizador.noEsVacio(rutaSeleccionada)) {
                        txtAgenteBinario.setText(rutaSeleccionada);
                    } else {
                        txtAgenteBinario.setText(enumAgente.obtenerRutaPorDefecto());
                    }
                }

                rutasBinarioAgenteTemporal.put(agenteSeleccionado, 
                    txtAgenteBinario != null ? txtAgenteBinario.getText().trim() : "");
            } else {
                JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
                if (chkAgenteHabilitado != null) {
                    chkAgenteHabilitado.setText(
                        I18nUI.Configuracion.Agentes.CHECK_HABILITAR_AGENTE(agenteSeleccionado));
                    chkAgenteHabilitado.setSelected(false);
                }
                JTextField txtAgenteBinario = dialogo.obtenerTxtAgenteBinario();
                if (txtAgenteBinario != null) {
                    txtAgenteBinario.setText("");
                }
            }
            ultimoAgenteSeleccionado = agenteSeleccionado;
        } finally {
            actualizandoRutaFlag = false;
        }
    }

    private boolean resolverEstadoHabilitacionAgente(String agenteSeleccionado) {
        if (Normalizador.esVacio(agenteSeleccionado)) {
            return false;
        }
        if (estadosHabilitacionAgenteTemporal.containsKey(agenteSeleccionado)) {
            return Boolean.TRUE.equals(estadosHabilitacionAgenteTemporal.get(agenteSeleccionado));
        }
        return config.agenteHabilitado(agenteSeleccionado);
    }

    private void guardarEstadoHabilitacionAgenteActual(String agente) {
        if (Normalizador.esVacio(agente)) {
            return;
        }
        JCheckBox chkAgenteHabilitado = dialogo.obtenerChkAgenteHabilitado();
        if (chkAgenteHabilitado != null) {
            estadosHabilitacionAgenteTemporal.put(agente, chkAgenteHabilitado.isSelected());
        }
    }

    private void manejarRefrescarModelos() {
        String proveedorSeleccionado = (String) dialogo.obtenerComboProveedor().getSelectedItem();
        if (Normalizador.esVacio(proveedorSeleccionado)
                || !ProveedorAI.permiteCargaRemotaModelos(proveedorSeleccionado)) {
            actualizarEstadoCargaModelosSegunProveedor();
            return;
        }

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerManager.validarEstadoActualParaCargaModelos(true);
        if (!resultado.esValido()) {
            mostrarErrorValidacionProveedor(resultado, true);
            return;
        }

        EstadoProveedorUI estadoUI = resultado.obtenerEstado();
        ConfiguracionAPI configTemporal = crearConfiguracionTemporalProveedor(proveedorSeleccionado, estadoUI);

        final long seq = secuenciaRefrescoModelos.incrementAndGet();
        JButton btnRefrescarModelos = dialogo.obtenerBtnRefrescarModelos();
        if (btnRefrescarModelos != null) {
            btnRefrescarModelos.setEnabled(false);
            btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_ACTUALIZANDO_MODELOS());
        }

        connectionTester.obtenerModelosDisponibles(configTemporal, new ConnectionTester.CallbackModelos() {
            @Override
            public void alExito(List<String> modelos) {
                SwingUtilities.invokeLater(() -> {
                    if (seq != secuenciaRefrescoModelos.get()) {
                        return;
                    }

                    try {
                        if (Normalizador.esVacia(modelos)) {
                            throw new IllegalStateException(
                                    I18nUI.Configuracion.ERROR_API_SIN_MODELOS(proveedorSeleccionado));
                        }

                        JComboBox<String> comboModelo = dialogo.obtenerComboModelo();
                        if (comboModelo != null) {
                            String preferido = estadoUI.tieneModelo() ? estadoUI.obtenerModelo() : modelos.get(0);
                            cargarModelosEnCombo(modelos, preferido);
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
                });
            }

            @Override
            public void alError(String error) {
                SwingUtilities.invokeLater(() -> {
                    if (seq != secuenciaRefrescoModelos.get()) {
                        return;
                    }

                    UIUtils.mostrarError(dialogo, I18nUI.Configuracion.TITULO_ERROR(),
                            I18nUI.Configuracion.MSG_ERROR_PROCESAR_MODELOS(error));
                    if (btnRefrescarModelos != null) {
                        btnRefrescarModelos.setText(I18nUI.Configuracion.BOTON_CARGAR_MODELOS());
                    }
                    actualizarEstadoCargaModelosSegunProveedor();
                });
            }
        });
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
                : obtenerTooltipCargaModelosNoDisponible(proveedorSeleccionado));
    }

    private String obtenerTooltipCargaModelosNoDisponible(String proveedorSeleccionado) {
        if ("Z.ai".equals(proveedorSeleccionado)) {
            return I18nUI.Tooltips.Configuracion.CARGAR_MODELOS_NO_DISPONIBLE_ZAI();
        }
        return I18nUI.Tooltips.Configuracion.CARGAR_MODELOS_NO_DISPONIBLE_MINIMAX();
    }

    public void manejarProbarConexion() {
        EstadoProveedorUI estadoUI = obtenerEstadoProveedorValidado(true, true, false);
        if (estadoUI == null) {
            return;
        }

        String proveedorSeleccionado = providerManager.obtenerProveedorActual();
        ConfiguracionAPI configTemporal = crearConfiguracionTemporalProveedor(proveedorSeleccionado, estadoUI);

        JButton btnProbarConexion = dialogo.obtenerBtnProbarConexion();
        if (btnProbarConexion != null) {
            btnProbarConexion.setEnabled(false);
            btnProbarConexion.setText(I18nUI.Configuracion.BOTON_PROBANDO());
        }

        SwingWorker<ProbadorConexionAI.ResultadoPrueba, Void> worker = new SwingWorker<>() {
            @Override
            protected ProbadorConexionAI.ResultadoPrueba doInBackground() {
                try {
                    ProbadorConexionAI probador = new ProbadorConexionAI(configTemporal);
                    return probador.probarConexion();
                } catch (Exception e) {
                    return new ProbadorConexionAI.ResultadoPrueba(
                        false,
                        I18nUI.Conexion.ERROR_PRUEBA_CONEXION(extraerMensajeError(e)),
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
                        I18nUI.Configuracion.MSG_ERROR_PRUEBA_INESPERADO(extraerMensajeError(e)));
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
                    errorBuilder.append(extraerMensajeError(e));
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
                        extraerMensajeError(e));
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
                snapshot.establecerApiKeyParaProveedor(nombreProveedor, estadoProveedor.obtenerApiKey());
                snapshot.establecerModeloParaProveedor(nombreProveedor, estadoProveedor.obtenerModelo());
                snapshot.establecerUrlBaseParaProveedor(nombreProveedor, estadoProveedor.obtenerBaseUrl());
                snapshot.establecerMaxTokensParaProveedor(nombreProveedor, estadoProveedor.obtenerMaxTokens());
                snapshot.establecerTiempoEsperaParaModelo(nombreProveedor, 
                    estadoProveedor.obtenerModelo(), estadoProveedor.obtenerTimeout());
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
        if (chkAgenteHabilitado != null && comboAgente != null) {
            guardarEstadoHabilitacionAgenteActual((String) comboAgente.getSelectedItem());
        }
        snapshot.establecerEstadosHabilitacionAgentes(estadosHabilitacionAgenteTemporal);
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

        connectionTester.verificarActualizaciones(
                VersionBurpIA.obtenerVersionActual(),
                new ConnectionTester.CallbackActualizacion() {
            @Override
            public void alExito(ConnectionTester.InfoActualizacion info) {
                SwingUtilities.invokeLater(() -> {
                    String versionActual = VersionBurpIA.obtenerVersionActual();
                    String urlDescarga = Normalizador.noEsVacio(info.obtenerUrlDescarga())
                            ? info.obtenerUrlDescarga()
                            : VersionBurpIA.URL_DESCARGA;

                    if (info.hayActualizacion()) {
                        UIUtils.mostrarInfo(
                                dialogo,
                                I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                                I18nUI.Configuracion.MSG_ACTUALIZACION_DISPONIBLE(
                                        versionActual,
                                        info.obtenerVersion(),
                                        urlDescarga));
                    } else {
                        UIUtils.mostrarInfo(
                                dialogo,
                                I18nUI.Configuracion.TITULO_ACTUALIZACIONES(),
                                I18nUI.Configuracion.MSG_VERSION_AL_DIA(versionActual));
                    }

                    btnBuscarActualizaciones.setEnabled(true);
                    btnBuscarActualizaciones.setText(I18nUI.Configuracion.BOTON_BUSCAR_ACTUALIZACIONES());
                });
            }

            @Override
            public void alError(String error) {
                SwingUtilities.invokeLater(() -> {
                    UIUtils.mostrarError(
                            dialogo,
                            I18nUI.Configuracion.TITULO_ERROR(),
                            I18nUI.Configuracion.MSG_ERROR_VERIFICAR_ACTUALIZACIONES(error));
                    btnBuscarActualizaciones.setEnabled(true);
                    btnBuscarActualizaciones.setText(I18nUI.Configuracion.BOTON_BUSCAR_ACTUALIZACIONES());
                });
            }
        });
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
        if (Normalizador.noEsVacio(rutaGuardada)) {
            return rutaGuardada;
        }
        AgenteTipo tipoAgente = AgenteTipo.desdeCodigo(agenteSeleccionado, null);
        return tipoAgente != null ? tipoAgente.obtenerRutaPorDefecto() : "";
    }

    private String obtenerModeloSeleccionado() {
        return providerManager.obtenerModeloSeleccionadoActual();
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
        providerManager.cargarModelosEnComboEnUI(modelos, preferido);
    }

    private ConfiguracionAPI crearConfiguracionTemporalProveedor(String proveedor, EstadoProveedorUI estadoUI) {
        ConfiguracionAPI configTemporal = new ConfiguracionAPI();
        configTemporal.establecerProveedorAI(proveedor);
        configTemporal.establecerApiKeyParaProveedor(proveedor, estadoUI.obtenerApiKey().trim());
        configTemporal.establecerUrlBaseParaProveedor(proveedor, estadoUI.obtenerBaseUrl().trim());
        configTemporal.establecerMaxTokensParaProveedor(proveedor, estadoUI.obtenerMaxTokens());
        configTemporal.establecerIgnorarErroresSSL(
                dialogo.obtenerChkIgnorarSSL() != null && dialogo.obtenerChkIgnorarSSL().isSelected());

        if (Normalizador.noEsVacio(estadoUI.obtenerModelo())) {
            configTemporal.establecerModeloParaProveedor(proveedor, estadoUI.obtenerModelo());
            configTemporal.establecerTiempoEsperaParaModelo(
                    proveedor,
                    estadoUI.obtenerModelo(),
                    estadoUI.obtenerTimeout());
        }

        return configTemporal;
    }

    public void cerrar() {
        connectionTester.cerrar();
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
            return resultado.obtenerEstado();
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
            return ResultadoValidacionEnteroDialogo.invalido(resultado.obtenerMensajeError(), campo);
        }
        return ResultadoValidacionEnteroDialogo.valido(resultado.obtenerValor());
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
        if (resultado == null || Normalizador.esVacio(resultado.obtenerMensajeError())) {
            return;
        }

        if (mostrarComoError) {
            UIUtils.mostrarError(dialogo,
                    I18nUI.Configuracion.TITULO_ERROR_VALIDACION(),
                    resultado.obtenerMensajeError());
        } else {
            UIUtils.mostrarAdvertencia(dialogo,
                    I18nUI.Configuracion.TITULO_VALIDACION(),
                    resultado.obtenerMensajeError());
        }
        enfocarCampoConfiguracion(resultado.obtenerCampo());
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
        public Map<String, Boolean> obtenerEstadosHabilitacionAgentesTemporales() {
            return new HashMap<>(estadosHabilitacionAgenteTemporal);
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
}
