package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.ui.EstadoProveedorUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor centralizado para la configuración de proveedores AI.
 * <p>
 * Extrae la lógica de manejo de proveedores desde DialogoConfiguracion
 * para seguir el principio DRY y mejorar la mantenibilidad.
 * </p>
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
public final class ProviderConfigManager {

    private static final int MAX_PROVEEDORES_SELECCIONADOS = 5;
    private static final String ORIGEN_LOG = "ProviderConfigManager";

    private final ConfiguracionAPI config;
    private final GestorLoggingUnificado gestorLogging;

    private final Map<String, EstadoProveedorUI> estadoProveedorTemporal;
    private String proveedorActualUi;
    private boolean actualizandoProveedorUi = false;
    private boolean actualizandoListaMultiProveedor = false;

    private DefaultListModel<String> modeloListaDisponibles;
    private DefaultListModel<String> modeloListaSeleccionados;
    private JList<String> listaProveedoresDisponibles;
    private JList<String> listaProveedoresSeleccionados;
    private JLabel lblEstadoMultiProveedor;

    private JComboBox<String> comboProveedor;
    private JComboBox<String> comboModelo;
    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtMaxTokens;
    private JTextField txtTimeoutModelo;
    private JButton btnAgregarProveedor;
    private JButton btnQuitarProveedor;
    private JButton btnSubirProveedor;
    private JButton btnBajarProveedor;
    private JCheckBox chkHabilitarMultiProveedor;

    /**
     * Crea un nuevo gestor de configuración de proveedores.
     *
     * @param config        Configuración API principal
     * @param gestorLogging Gestor de logging unificado
     */
    public ProviderConfigManager(ConfiguracionAPI config, GestorLoggingUnificado gestorLogging) {
        if (config == null || gestorLogging == null) {
            throw new IllegalArgumentException("Configuración y gestorLogging no pueden ser nulos");
        }

        this.config = config;
        this.gestorLogging = gestorLogging;
        this.estadoProveedorTemporal = new HashMap<>();
        this.proveedorActualUi = null;

        gestorLogging.info(ORIGEN_LOG, "ProviderConfigManager inicializado");
    }

    /**
     * Configura los componentes básicos de UI necesarios para extraer el estado.
     * Versión simplificada de inicializarComponentesUI para casos donde no se necesita multi-proveedor.
     *
     * @param comboProveedor  ComboBox de selección de proveedor
     * @param txtUrl           TextField de URL base
     * @param txtClave         PasswordField de API key
     * @param comboModelo      ComboBox de selección de modelo
     * @param txtTimeoutModelo TextField de timeout
     * @param txtMaxTokens     TextField de max tokens
     */
    public void configurarComponentes(JComboBox<String> comboProveedor,
                                      JTextField txtUrl,
                                      JPasswordField txtClave,
                                      JComboBox<String> comboModelo,
                                      JTextField txtTimeoutModelo,
                                      JTextField txtMaxTokens) {
        this.comboProveedor = comboProveedor;
        this.txtUrl = txtUrl;
        this.txtClave = txtClave;
        this.comboModelo = comboModelo;
        this.txtTimeoutModelo = txtTimeoutModelo;
        this.txtMaxTokens = txtMaxTokens;
        
        gestorLogging.info(ORIGEN_LOG, "Componentes básicos de UI configurados");
    }

    /**
     * Inicializa los componentes UI necesarios para el manejo de proveedores.
     *
     * @param comboProveedor        ComboBox de selección de proveedor
     * @param comboModelo           ComboBox de selección de modelo
     * @param txtUrl                TextField de URL base
     * @param txtClave              PasswordField de API key
     * @param txtMaxTokens          TextField de max tokens
     * @param txtTimeoutModelo      TextField de timeout
     * @param btnAgregarProveedor   Botón para agregar proveedor
     * @param btnQuitarProveedor    Botón para quitar proveedor
     * @param btnSubirProveedor     Botón para subir proveedor en lista
     * @param btnBajarProveedor     Botón para bajar proveedor en lista
     * @param chkHabilitarMultiProveedor CheckBox para habilitar multi-proveedor
     * @param lblEstadoMultiProveedor Label de estado multi-proveedor
     * @param modeloListaDisponibles Modelo de lista de proveedores disponibles
     * @param modeloListaSeleccionados Modelo de lista de proveedores seleccionados
     * @param listaProveedoresDisponibles JList de proveedores disponibles
     * @param listaProveedoresSeleccionados JList de proveedores seleccionados
     */
    public void inicializarComponentesUI(JComboBox<String> comboProveedor,
                                       JComboBox<String> comboModelo,
                                       JTextField txtUrl,
                                       JPasswordField txtClave,
                                       JTextField txtMaxTokens,
                                       JTextField txtTimeoutModelo,
                                       JButton btnAgregarProveedor,
                                       JButton btnQuitarProveedor,
                                       JButton btnSubirProveedor,
                                       JButton btnBajarProveedor,
                                       JCheckBox chkHabilitarMultiProveedor,
                                       JLabel lblEstadoMultiProveedor,
                                       DefaultListModel<String> modeloListaDisponibles,
                                       DefaultListModel<String> modeloListaSeleccionados,
                                       JList<String> listaProveedoresDisponibles,
                                       JList<String> listaProveedoresSeleccionados) {
        this.comboProveedor = comboProveedor;
        this.comboModelo = comboModelo;
        this.txtUrl = txtUrl;
        this.txtClave = txtClave;
        this.txtMaxTokens = txtMaxTokens;
        this.txtTimeoutModelo = txtTimeoutModelo;
        this.btnAgregarProveedor = btnAgregarProveedor;
        this.btnQuitarProveedor = btnQuitarProveedor;
        this.btnSubirProveedor = btnSubirProveedor;
        this.btnBajarProveedor = btnBajarProveedor;
        this.chkHabilitarMultiProveedor = chkHabilitarMultiProveedor;
        this.lblEstadoMultiProveedor = lblEstadoMultiProveedor;
        this.modeloListaDisponibles = modeloListaDisponibles;
        this.modeloListaSeleccionados = modeloListaSeleccionados;
        this.listaProveedoresDisponibles = listaProveedoresDisponibles;
        this.listaProveedoresSeleccionados = listaProveedoresSeleccionados;

        if (comboProveedor != null) {
            comboProveedor.addActionListener(e -> alCambiarProveedor());
        }
        if (btnAgregarProveedor != null) {
            btnAgregarProveedor.addActionListener(e -> agregarProveedorSeleccionado());
        }
        if (btnQuitarProveedor != null) {
            btnQuitarProveedor.addActionListener(e -> quitarProveedorSeleccionado());
        }
        if (btnSubirProveedor != null) {
            btnSubirProveedor.addActionListener(e -> subirProveedorSeleccionado());
        }
        if (btnBajarProveedor != null) {
            btnBajarProveedor.addActionListener(e -> bajarProveedorSeleccionado());
        }
        ListSelectionListener listenerSeleccionMultiProveedor = e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotonesMultiProveedor();
            }
        };
        if (listaProveedoresDisponibles != null) {
            listaProveedoresDisponibles.addListSelectionListener(listenerSeleccionMultiProveedor);
        }
        if (listaProveedoresSeleccionados != null) {
            listaProveedoresSeleccionados.addListSelectionListener(listenerSeleccionMultiProveedor);
        }
        if (chkHabilitarMultiProveedor != null) {
            chkHabilitarMultiProveedor.addActionListener(e -> {
                actualizarEstadoMultiProveedor();
                actualizarBotonesMultiProveedor();

                // Si se acaba de marcar, sincronizar el proveedor actual
                if (chkHabilitarMultiProveedor.isSelected()) {
                    sincronizarProveedorPrincipalConMultiProveedor();
                }
            });
        }

        gestorLogging.info(ORIGEN_LOG, "Componentes UI inicializados");
    }

    /**
     * Carga la configuración inicial de proveedores en los componentes UI.
     */
    public void cargarConfiguracionInicial() {
        if (comboProveedor == null || modeloListaDisponibles == null || modeloListaSeleccionados == null) {
            gestorLogging.error(ORIGEN_LOG, "Componentes UI no inicializados");
            return;
        }

        // Obtener el proveedor GUARDADO en configuración, NO el primero del combo
        String proveedorGuardado = config.obtenerProveedorAI();
        if (Normalizador.noEsVacio(proveedorGuardado)) {
            comboProveedor.setSelectedItem(proveedorGuardado);
        }
        proveedorActualUi = (String) comboProveedor.getSelectedItem();
        cargarEstadoProveedor(proveedorActualUi);

        chkHabilitarMultiProveedor.setSelected(config.esMultiProveedorHabilitado());

        modeloListaSeleccionados.clear();
        List<String> seleccionados = config.obtenerProveedoresMultiConsulta();
        for (String proveedor : seleccionados) {
            modeloListaSeleccionados.addElement(proveedor);
        }

        modeloListaDisponibles.clear();
        List<String> todosProveedores = ProveedorAI.obtenerNombresProveedores();
        for (String proveedor : todosProveedores) {
            if (modeloListaSeleccionados.indexOf(proveedor) == -1) {
                modeloListaDisponibles.addElement(proveedor);
            }
        }

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();

        gestorLogging.info(ORIGEN_LOG, "Configuración inicial cargada");
    }

    /**
     * Cambia al proveedor especificado, guardando el estado actual.
     *
     * @param nuevoProveedor Nombre del nuevo proveedor
     */
    public void cambiarProveedor(String nuevoProveedor) {
        if (actualizandoProveedorUi || Normalizador.esVacio(nuevoProveedor)) {
            return;
        }

        actualizandoProveedorUi = true;
        try {
            if (proveedorActualUi != null) {
                EstadoProveedorUI estadoActual = extraerEstadoActualRapido();
                if (estadoActual != null) {
                    estadoProveedorTemporal.put(proveedorActualUi, estadoActual);
                }
            }

            cargarEstadoProveedor(nuevoProveedor);
            
            proveedorActualUi = nuevoProveedor;
            if (comboProveedor != null) {
                comboProveedor.setSelectedItem(nuevoProveedor);
            }

            gestorLogging.info(ORIGEN_LOG, "Proveedor cambiado a: " + nuevoProveedor);

            // Sincronizar proveedor principal con lista multi-proveedor
            sincronizarProveedorPrincipalConMultiProveedor();

        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Error al cambiar proveedor: " + e.getMessage(), e);
        } finally {
            actualizandoProveedorUi = false;
        }
    }

    /**
     * Obtiene el estado actual de la UI para el proveedor seleccionado.
     *
     * @return EstadoProveedorUI con los datos actuales, o null si hay error
     */
    public EstadoProveedorUI extraerEstadoActual() {
        String modelo = obtenerModeloSeleccionado();
        if (Normalizador.esVacio(modelo)) {
            gestorLogging.error(ORIGEN_LOG, "No se ha seleccionado un modelo");
            return null;
        }

        Integer timeout = parsearEntero(txtTimeoutModelo != null ? txtTimeoutModelo.getText() : null);
        if (timeout == null) {
            gestorLogging.error(ORIGEN_LOG, "Formato de timeout inválido");
            return null;
        }

        Integer maxTokens = parsearEntero(txtMaxTokens != null ? txtMaxTokens.getText() : null);
        if (maxTokens == null) {
            maxTokens = 4096;
        }

        String apiKey = txtClave != null ? new String(txtClave.getPassword()) : "";
        String baseUrl = txtUrl != null ? txtUrl.getText().trim() : "";

        return new EstadoProveedorUI(apiKey, modelo, baseUrl, maxTokens, timeout);
    }

    /**
     * Extrae el estado actual de forma rápida (sin validaciones estrictas).
     *
     * @return EstadoProveedorUI con datos actuales
     */
    public EstadoProveedorUI extraerEstadoActualRapido() {
        Integer timeout = parsearEntero(txtTimeoutModelo != null ? txtTimeoutModelo.getText() : null);
        Integer maxTokens = parsearEntero(txtMaxTokens != null ? txtMaxTokens.getText() : null);
        
        String apiKey = txtClave != null ? new String(txtClave.getPassword()) : "";
        String modelo = obtenerModeloSeleccionado();
        String baseUrl = txtUrl != null ? txtUrl.getText().trim() : "";

        return new EstadoProveedorUI(
                apiKey,
                modelo,
                baseUrl,
                maxTokens != null ? maxTokens : 4096,
                timeout != null ? timeout : 120);
    }

    /**
     * Obtiene la configuración extraída para ser guardada.
     *
     * @return Mapa con la configuración de proveedores temporales
     */
    public Map<String, EstadoProveedorUI> obtenerConfiguracionParaGuardar() {
        Map<String, EstadoProveedorUI> configGuardar = new HashMap<>(estadoProveedorTemporal);
        
        if (proveedorActualUi != null) {
            EstadoProveedorUI estadoActual = extraerEstadoActual();
            if (estadoActual != null) {
                configGuardar.put(proveedorActualUi, estadoActual);
            }
        }

        return configGuardar;
    }

    /**
     * Expone una copia del estado temporal de proveedores para detección de cambios no guardados.
     */
    public Map<String, EstadoProveedorUI> obtenerEstadosProveedorTemporales() {
        return new HashMap<>(estadoProveedorTemporal);
    }

    /**
     * Verifica si un proveedor está actualmente seleccionado en la UI.
     *
     * @param proveedor Nombre del proveedor a verificar
     * @return true si está seleccionado
     */
    public boolean esProveedorSeleccionado(String proveedor) {
        if (Normalizador.esVacio(proveedor) || comboProveedor == null) {
            return false;
        }
        return proveedor.equals(comboProveedor.getSelectedItem());
    }

    /**
     * Obtiene el proveedor actualmente seleccionado en la UI.
     *
     * @return Nombre del proveedor actual, o null si no hay selección
     */
    public String obtenerProveedorActual() {
        return comboProveedor != null ? (String) comboProveedor.getSelectedItem() : null;
    }

    /**
     * Establece el proveedor actual sin disparar eventos.
     *
     * @param proveedor Nombre del proveedor
     */
    public void establecerProveedorActual(String proveedor) {
        if (comboProveedor != null) {
            actualizandoProveedorUi = true;
            try {
                comboProveedor.setSelectedItem(proveedor);
                proveedorActualUi = proveedor;
            } finally {
                actualizandoProveedorUi = false;
            }
        }
    }

    private void alCambiarProveedor() {
        cambiarProveedor((String) comboProveedor.getSelectedItem());
    }

    private void cargarEstadoProveedor(String proveedor) {
        if (Normalizador.esVacio(proveedor)) {
            return;
        }

        EstadoProveedorUI borrador = estadoProveedorTemporal.get(proveedor);
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (configProveedor == null) {
            gestorLogging.error(ORIGEN_LOG, "No se encontró configuración para proveedor: " + proveedor);
            return;
        }

        if (borrador != null) {
            cargarEstadoDesdeBorrador(borrador, configProveedor);
        } else {
            cargarEstadoDesdeConfiguracion(proveedor, configProveedor);
        }
    }

    private void cargarEstadoDesdeBorrador(EstadoProveedorUI borrador, ProveedorAI.ConfiguracionProveedor configProveedor) {
        if (txtUrl != null) {
            txtUrl.setText(borrador.getBaseUrl());
        }
        if (txtClave != null) {
            txtClave.setText(borrador.getApiKey());
        }
        if (txtMaxTokens != null) {
            txtMaxTokens.setText(String.valueOf(borrador.getMaxTokens()));
        }
        cargarModelosEnCombo(configProveedor.obtenerModelosDisponibles(), borrador.getModelo());
        if (txtTimeoutModelo != null) {
            txtTimeoutModelo.setText(String.valueOf(borrador.getTimeout()));
        }
    }

    private void cargarEstadoDesdeConfiguracion(String proveedor, ProveedorAI.ConfiguracionProveedor configProveedor) {
        String urlGuardada = config.obtenerUrlBaseGuardadaParaProveedor(proveedor);
        boolean tieneUrlGuardada = Normalizador.noEsVacio(urlGuardada);
        
        if (txtUrl != null) {
            if (!tieneUrlGuardada && ProveedorAI.esProveedorCustom(proveedor)) {
                String codigo = config.obtenerIdiomaUi();
                txtUrl.setText(ProveedorAI.obtenerUrlApiPorDefecto(proveedor, codigo));
            } else {
                String urlBase = tieneUrlGuardada ? urlGuardada : config.obtenerUrlBaseParaProveedor(proveedor);
                txtUrl.setText(urlBase != null ? urlBase : configProveedor.obtenerUrlApi());
            }
        }

        if (txtClave != null) {
            String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
            txtClave.setText(apiKey != null ? apiKey : "");
        }

        if (txtMaxTokens != null) {
            Integer maxTokens = config.obtenerMaxTokensConfiguradoParaProveedor(proveedor);
            txtMaxTokens.setText(
                    String.valueOf(maxTokens != null ? maxTokens : configProveedor.obtenerMaxTokensPorDefecto()));
        }

        String modelo = config.obtenerModeloParaProveedor(proveedor);
        if (Normalizador.esVacio(modelo)) {
            modelo = configProveedor.obtenerModeloPorDefecto();
        }
        cargarModelosEnCombo(configProveedor.obtenerModelosDisponibles(), modelo);
        actualizarTimeoutModeloSeleccionado();
    }

    private void agregarProveedorSeleccionado() {
        if (listaProveedoresDisponibles == null || modeloListaSeleccionados == null) {
            return;
        }

        String proveedor = listaProveedoresDisponibles.getSelectedValue();
        if (Normalizador.esVacio(proveedor)) {
            return;
        }

        if (modeloListaSeleccionados.getSize() >= MAX_PROVEEDORES_SELECCIONADOS) {
            gestorLogging.error(ORIGEN_LOG, "Límite máximo de proveedores alcanzado: " + MAX_PROVEEDORES_SELECCIONADOS);
            return;
        }

        if (modeloListaSeleccionados.indexOf(proveedor) >= 0) {
            return;
        }

        modeloListaSeleccionados.addElement(proveedor);

        // Verificar si modeloListaDisponibles está inicializado
        if (modeloListaDisponibles != null) {
            modeloListaDisponibles.removeElement(proveedor);
        }

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
        gestorLogging.info(ORIGEN_LOG, "Proveedor agregado a lista: " + proveedor);
    }

    private void quitarProveedorSeleccionado() {
        if (listaProveedoresSeleccionados == null || modeloListaSeleccionados == null || modeloListaDisponibles == null) {
            return;
        }

        String proveedor = listaProveedoresSeleccionados.getSelectedValue();
        if (Normalizador.esVacio(proveedor)) {
            return;
        }

        String proveedorActual = obtenerProveedorActual();

        if (proveedor.equals(proveedorActual)) {
            gestorLogging.error(ORIGEN_LOG, "No se puede quitar el proveedor principal: " + proveedor);
            return;
        }

        modeloListaSeleccionados.removeElement(proveedor);
        modeloListaDisponibles.addElement(proveedor);

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
        gestorLogging.info(ORIGEN_LOG, "Proveedor quitado de lista: " + proveedor);
    }

    private void subirProveedorSeleccionado() {
        if (listaProveedoresSeleccionados == null || modeloListaSeleccionados == null) {
            return;
        }

        int indice = listaProveedoresSeleccionados.getSelectedIndex();
        if (indice <= 0) {
            return;
        }

        String proveedor = modeloListaSeleccionados.getElementAt(indice);

        // SRP: Protección del proveedor principal
        if (Normalizador.noEsVacio(proveedorActualUi) && proveedor.equals(proveedorActualUi)) {
            gestorLogging.error(ORIGEN_LOG, "No se puede subir el proveedor principal: " + proveedor);
            return;
        }

        String proveedorMovido = modeloListaSeleccionados.remove(indice);
        modeloListaSeleccionados.add(indice - 1, proveedorMovido);
        listaProveedoresSeleccionados.setSelectedIndex(indice - 1);

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
        gestorLogging.info(ORIGEN_LOG, "Proveedor subido en lista: " + proveedor);
    }

    private void bajarProveedorSeleccionado() {
        if (listaProveedoresSeleccionados == null || modeloListaSeleccionados == null) {
            return;
        }

        int indice = listaProveedoresSeleccionados.getSelectedIndex();
        if (indice < 0 || indice >= modeloListaSeleccionados.getSize() - 1) {
            return;
        }

        String proveedor = modeloListaSeleccionados.getElementAt(indice);

        // SRP: Protección del proveedor principal
        if (Normalizador.noEsVacio(proveedorActualUi) && proveedor.equals(proveedorActualUi)) {
            gestorLogging.error(ORIGEN_LOG, "No se puede bajar el proveedor principal: " + proveedor);
            return;
        }

        String proveedorMovido = modeloListaSeleccionados.remove(indice);
        modeloListaSeleccionados.add(indice + 1, proveedorMovido);
        listaProveedoresSeleccionados.setSelectedIndex(indice + 1);

        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
        gestorLogging.info(ORIGEN_LOG, "Proveedor bajado en lista: " + proveedor);
    }

    private void sincronizarProveedorPrincipalConMultiProveedor() {
        // 1. Precondiciones (SRP: Validación separada)
        if (chkHabilitarMultiProveedor == null || !chkHabilitarMultiProveedor.isSelected()) {
            return;
        }
        if (Normalizador.esVacio(proveedorActualUi)) {
            return;
        }
        if (modeloListaSeleccionados == null || modeloListaDisponibles == null) {
            return;
        }

        // 2. Buscar proveedor actual en lista seleccionada
        int indiceActual = modeloListaSeleccionados.indexOf(proveedorActualUi);

        // 3. Caso A: El proveedor YA está en la lista seleccionada
        if (indiceActual >= 0) {
            if (indiceActual != 0) {
                // Mover al inicio solo si no es el primero
                String proveedor = modeloListaSeleccionados.remove(indiceActual);
                modeloListaSeleccionados.add(0, proveedor);
                gestorLogging.info(ORIGEN_LOG, "Proveedor '" + proveedorActualUi +
                        "' movido a la primera posición en lista multi-proveedor");
            }
            // Si está en 0, no hacer nada (ya sincronizado)
            actualizarEstadoMultiProveedor();
            actualizarBotonesMultiProveedor();
            return;
        }

        // 4. Caso B: El proveedor NO está en la lista, agregarlo
        modeloListaSeleccionados.add(0, proveedorActualUi);

        // 5. Eliminar de lista disponibles si estaba ahí
        int indiceDisponibles = modeloListaDisponibles.indexOf(proveedorActualUi);
        if (indiceDisponibles >= 0) {
            modeloListaDisponibles.removeElement(proveedorActualUi);
        }

        gestorLogging.info(ORIGEN_LOG, "Proveedor '" + proveedorActualUi +
                "' agregado a la primera posición en lista multi-proveedor");

        // 6. Actualizar UI
        actualizarEstadoMultiProveedor();
        actualizarBotonesMultiProveedor();
    }

    private void actualizarEstadoMultiProveedor() {
        if (lblEstadoMultiProveedor == null || modeloListaSeleccionados == null 
                || chkHabilitarMultiProveedor == null) {
            return;
        }

        boolean habilitado = chkHabilitarMultiProveedor.isSelected();
        int cantidad = modeloListaSeleccionados.getSize();

        if (!habilitado) {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
        } else if (cantidad == 0) {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_SIN_PROVEEDORES());
        } else if (cantidad == 1) {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_MENOS_DOS());
        } else {
            lblEstadoMultiProveedor.setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_HABILITADO(String.valueOf(cantidad)));
        }
    }

    private void actualizarBotonesMultiProveedor() {
        if (btnAgregarProveedor == null || btnQuitarProveedor == null
                || btnSubirProveedor == null || btnBajarProveedor == null
                || chkHabilitarMultiProveedor == null) {
            return;
        }

        boolean habilitado = chkHabilitarMultiProveedor.isSelected();
        boolean haySeleccionDisponible = listaProveedoresDisponibles != null
            && listaProveedoresDisponibles.getSelectedValue() != null;
        boolean haySeleccionSeleccionados = listaProveedoresSeleccionados != null
            && listaProveedoresSeleccionados.getSelectedValue() != null;

        btnAgregarProveedor.setEnabled(habilitado && haySeleccionDisponible);
        btnQuitarProveedor.setEnabled(habilitado && haySeleccionSeleccionados);

        // FIX: Habilitar/Deshabilitar botones Subir y Bajar verificando posición
        boolean puedeSubir = false;
        boolean puedeBajar = false;

        if (habilitado && listaProveedoresSeleccionados != null) {
            int indiceSeleccionado = listaProveedoresSeleccionados.getSelectedIndex();
            int cantidadElementos = modeloListaSeleccionados != null ? modeloListaSeleccionados.getSize() : 0;

            // Solo puede subir si hay selección y NO es el primero (indice 0)
            puedeSubir = haySeleccionSeleccionados && indiceSeleccionado > 0;

            // Solo puede bajar si hay selección y NO es el último (indice >= cantidad - 1)
            puedeBajar = haySeleccionSeleccionados && indiceSeleccionado >= 0 && indiceSeleccionado < cantidadElementos - 1;
        }

        btnSubirProveedor.setEnabled(puedeSubir);
        btnBajarProveedor.setEnabled(puedeBajar);
    }

    private void actualizarTimeoutModeloSeleccionado() {
        if (txtTimeoutModelo == null || comboModelo == null || proveedorActualUi == null) {
            return;
        }

        String modelo = obtenerModeloSeleccionado();
        int timeout = config.obtenerTiempoEsperaParaModelo(proveedorActualUi, modelo);
        txtTimeoutModelo.setText(String.valueOf(timeout));
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
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

    private String obtenerModeloSeleccionado() {
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
        if (comboModelo.getEditor() == null) {
            return "";
        }
        Object editorValue = comboModelo.getEditor().getItem();
        return editorValue != null ? normalizarModeloSeleccionado(editorValue.toString()) : "";
    }

    private boolean esOpcionModeloCustom(String modelo) {
        return I18nUI.Configuracion.OPCION_MODELO_CUSTOM().equals(modelo);
    }

    private List<String> normalizarModelos(List<String> modelos) {
        List<String> normalizados = new ArrayList<>();
        for (String modelo : modelos) {
            String normalizado = normalizarModeloSeleccionado(modelo);
            if (Normalizador.noEsVacio(normalizado) && !normalizados.contains(normalizado)) {
                normalizados.add(normalizado);
            }
        }
        return normalizados;
    }

    private String normalizarModeloSeleccionado(String modelo) {
        if (Normalizador.esVacio(modelo)) {
            return "";
        }
        return modelo.trim();
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
}
