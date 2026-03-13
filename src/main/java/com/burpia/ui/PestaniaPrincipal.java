package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.Timer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PestaniaPrincipal extends JPanel {
    private static final String ORIGEN_LOG = "PestaniaPrincipal";
    private static final int DELAY_GUARDADO_ANCHO_COLUMNAS_MS = 500;
    private static final String ID_TABLA_HALLAZGOS = "hallazgos";
    private static final String ID_TABLA_TAREAS = "tareas";

    private enum DestinoPestania {
        TAREAS("TAB_TAREAS", I18nUI.Pestanias::TAREAS, I18nUI.Tooltips.Pestanias::TAREAS),
        HALLAZGOS("TAB_HALLAZGOS", I18nUI.Pestanias::HALLAZGOS, I18nUI.Tooltips.Pestanias::HALLAZGOS),
        AGENTE("TAB_AGENTE", I18nUI.Pestanias::AGENTE, I18nUI.Tooltips.Pestanias::AGENTE),
        CONSOLA("TAB_CONSOLA", I18nUI.Pestanias::CONSOLA, I18nUI.Tooltips.Pestanias::CONSOLA);

        private final String identificadorPersistencia;
        private final Supplier<String> tituloProveedor;
        private final Supplier<String> tooltipProveedor;

        DestinoPestania(String identificadorPersistencia,
                        Supplier<String> tituloProveedor,
                        Supplier<String> tooltipProveedor) {
            this.identificadorPersistencia = identificadorPersistencia;
            this.tituloProveedor = tituloProveedor;
            this.tooltipProveedor = tooltipProveedor;
        }

        private String obtenerIdentificadorPersistencia() {
            return identificadorPersistencia;
        }

        private String obtenerTitulo() {
            return tituloProveedor.get();
        }

        private String obtenerTooltip() {
            return tooltipProveedor.get();
        }
    }

    private final PanelEstadisticas panelEstadisticas;
    private final PanelTareas panelTareas;
    private final PanelHallazgos panelHallazgos;
    private final PanelConsola panelConsola;
    private final PanelAgente panelAgente;
    private final JTabbedPane tabbedPane;
    private final ConfiguracionAPI config;
    private final UIStateManager uiStateManager;
    private final GestorLoggingUnificado gestorLogging;
    private final Map<DestinoPestania, Component> componentesPorDestino;
    private final PropertyChangeListener listenerLookAndFeel;
    private volatile Timer timerFocoAgente;

    @SuppressWarnings("this-escape")
    public PestaniaPrincipal(MontoyaApi api,
                            Estadisticas estadisticas,
                            GestorTareas gestorTareas,
                            GestorConsolaGUI gestorConsola,
                            ModeloTablaTareas modeloTareas,
                            ModeloTablaHallazgos modeloHallazgos,
                            boolean esBurpProfessional,
                            ConfiguracionAPI config,
                            GestorLoggingUnificado gestorLogging) {
        this.config = config;
        this.gestorLogging = gestorLogging;
        this.uiStateManager = new UIStateManager(config, gestorLogging);

        panelTareas = new PanelTareas(gestorTareas, modeloTareas);
        panelTareas.establecerConfiguracion(config);
        panelHallazgos = new PanelHallazgos(api, modeloHallazgos, esBurpProfessional);
        panelHallazgos.establecerConfiguracion(config);

        panelEstadisticas = new PanelEstadisticas(estadisticas, modeloHallazgos::obtenerLimiteFilas, panelHallazgos);
        panelHallazgos.establecerManejadorFiltrosAplicados(panelEstadisticas::actualizarForzado);
        this.panelConsola = new PanelConsola(gestorConsola);
        this.panelAgente = new PanelAgente(config, config.hayAlgunAgenteHabilitado());
        this.panelAgente.establecerManejadorFocoPestania(this::enfocarPestaniaAgenteDesdeManejador);
        this.componentesPorDestino = new EnumMap<>(DestinoPestania.class);
        registrarComponentePestania(DestinoPestania.TAREAS, panelTareas);
        registrarComponentePestania(DestinoPestania.HALLAZGOS, panelHallazgos);
        registrarComponentePestania(DestinoPestania.AGENTE, panelAgente);
        registrarComponentePestania(DestinoPestania.CONSOLA, panelConsola);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(EstilosUI.FUENTE_NEGRITA);
        agregarPestania(DestinoPestania.TAREAS);
        agregarPestania(DestinoPestania.HALLAZGOS);

        if (config.hayAlgunAgenteHabilitado()) {
            agregarPestania(DestinoPestania.AGENTE);
        }
        agregarPestania(DestinoPestania.CONSOLA);
        tabbedPane.setSelectedComponent(panelConsola);
        tabbedPane.addChangeListener(e -> manejarCambioPestania());
        aplicarIdioma();

        setLayout(new BorderLayout(10, 2));
        add(panelEstadisticas, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        listenerLookAndFeel = evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                ejecutarEnEdt(this::aplicarTema);
            }
        };
        UIManager.addPropertyChangeListener(listenerLookAndFeel);
        aplicarTema();
        
        // Restaurar estado UI después de inicializar componentes
        restaurarEstadoUI();
        
        // Configurar listeners para guardar cambios de estado
        configurarListenersEstadoUI();
    }

    /**
     * Restaura el estado UI guardado.
     */
    private void restaurarEstadoUI() {
        try {
            uiStateManager.restaurarUltimaPestaniaSeleccionada(
                tabbedPane,
                obtenerIndicePestania(DestinoPestania.CONSOLA)
            );
            uiStateManager.restaurarEstadoFiltrosHallazgos(panelHallazgos);
            restaurarAnchosColumnasActuales();
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al restaurar estado UI de pestaña principal"), e);
        }
    }

    /**
     * Configura listeners para guardar cambios de estado UI.
     */
    private void configurarListenersEstadoUI() {
        panelHallazgos.establecerUIStateManager(uiStateManager);
        registrarPersistenciaAnchosColumnas(panelHallazgos.obtenerTabla(), ID_TABLA_HALLAZGOS);
        registrarPersistenciaAnchosColumnas(panelTareas.obtenerTabla(), ID_TABLA_TAREAS);
    }

    /**
     * Agrega un hallazgo al panel de hallazgos.
     *
     * @param hallazgo El hallazgo a agregar
     */
    public void agregarHallazgo(Hallazgo hallazgo) {
        panelHallazgos.agregarHallazgo(hallazgo);
    }

    /**
     * Agrega múltiples hallazgos al panel de hallazgos.
     *
     * @param hallazgos La lista de hallazgos a agregar
     */
    public void agregarHallazgos(List<Hallazgo> hallazgos) {
        panelHallazgos.agregarHallazgos(hallazgos);
    }


    /**
     * Registra un mensaje informativo en la consola.
     *
     * @param mensaje El mensaje a registrar
     */
    public void registrar(String mensaje) {
        panelConsola.obtenerGestorConsola().registrarInfo(mensaje);
    }

    /**
     * Actualiza las estadísticas mostradas en el panel.
     */
    public void actualizarEstadisticas() {
        panelEstadisticas.actualizar();
    }

    /**
     * Obtiene el panel principal como JPanel.
     *
     * @return Esta instancia como JPanel
     */
    public JPanel obtenerPanel() {
        return this;
    }

    /**
     * Obtiene el panel de consola.
     *
     * @return El panel de consola
     */
    public PanelConsola obtenerPanelConsola() {
        return panelConsola;
    }

    /**
     * Obtiene el panel de agente.
     *
     * @return El panel de agente
     */
    public PanelAgente obtenerPanelAgente() {
        return panelAgente;
    }

    /**
     * Actualiza la visibilidad del panel de agente según la configuración.
     * Si el agente está habilitado y no está visible, lo agrega.
     * Si está deshabilitado y está visible, lo remueve.
     * Si está habilitado y visible, actualiza su título y tooltip.
     */
    public void actualizarVisibilidadAgentes() {
        boolean habilitado = config.hayAlgunAgenteHabilitado();
        int index = obtenerIndicePestania(DestinoPestania.AGENTE);

        if (habilitado && index == -1) {
            int indexConsola = obtenerIndicePestania(DestinoPestania.CONSOLA);
            if (indexConsola != -1) {
                insertarPestania(DestinoPestania.AGENTE, indexConsola);
            } else {
                agregarPestania(DestinoPestania.AGENTE);
            }
            panelAgente.asegurarConsolaIniciada();
            panelAgente.reinyectarPromptInicial();
        } else if (!habilitado && index != -1) {
            tabbedPane.removeTabAt(index);
            panelAgente.destruir();
        } else if (habilitado && index != -1) {
            actualizarMetadatosPestania(DestinoPestania.AGENTE);
            tabbedPane.revalidate();
            tabbedPane.repaint();

            panelAgente.asegurarConsolaIniciada();
            
            String idActual = config.obtenerTipoAgenteOperativo();
            if (!Objects.equals(idActual, panelAgente.obtenerUltimoAgenteIniciado())) {
                panelAgente.reiniciar();
            }
        }
    }

    /**
     * Selecciona y enfoca la pestaña del agente.
     */
    public void seleccionarPestaniaAgente() {
        enfocarPestania(DestinoPestania.AGENTE, true);
    }

    private void manejarCambioPestania() {
        guardarPestaniaSeleccionadaActual();
        DestinoPestania destino = obtenerDestinoSeleccionado();
        if (destino != DestinoPestania.AGENTE) {
            return;
        }
        enfocarPestania(destino, false);
    }

    private void enfocarPestaniaAgenteDesdeManejador() {
        enfocarPestania(DestinoPestania.AGENTE, true);
    }

    private void enfocarPestania(DestinoPestania destino, boolean traerVentanaAlFrente) {
        if (destino == null) {
            return;
        }
        Component componente = resolverComponentePestania(destino);
        if (componente == null || tabbedPane.indexOfComponent(componente) == -1) {
            return;
        }

        tabbedPane.setSelectedComponent(componente);

        if (destino == DestinoPestania.AGENTE) {
            panelAgente.asegurarConsolaIniciada();
            programarFocoTerminalAgente(traerVentanaAlFrente);
            return;
        }

        ejecutarEnEdt(() -> enfocarComponenteSeleccionado(componente, traerVentanaAlFrente));
    }

    private DestinoPestania obtenerDestinoSeleccionado() {
        return resolverDestinoPestania(tabbedPane.getSelectedComponent());
    }

    private DestinoPestania resolverDestinoPestania(Component componente) {
        for (Map.Entry<DestinoPestania, Component> entry : componentesPorDestino.entrySet()) {
            if (entry.getValue() == componente) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Component resolverComponentePestania(DestinoPestania destino) {
        return destino != null ? componentesPorDestino.get(destino) : null;
    }

    private void enfocarComponenteSeleccionado(Component componente, boolean traerVentanaAlFrente) {
        if (tabbedPane.getSelectedComponent() != componente) {
            return;
        }
        if (traerVentanaAlFrente) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.toFront();
            }
        }
        componente.requestFocusInWindow();
    }

    private void programarFocoTerminalAgente(boolean traerVentanaAlFrente) {
        if (timerFocoAgente != null && timerFocoAgente.isRunning()) {
            timerFocoAgente.stop();
        }

        timerFocoAgente = new Timer(150, e -> {
            ((Timer) e.getSource()).stop();
            ejecutarEnEdt(() -> {
                if (tabbedPane.getSelectedComponent() != panelAgente) {
                    return;
                }
                if (traerVentanaAlFrente) {
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window != null) {
                        window.toFront();
                    }
                }
                panelAgente.enfocarTerminal();
            });
        });
        timerFocoAgente.setRepeats(false);
        timerFocoAgente.start();
    }

    /**
     * Obtiene el panel de hallazgos.
     *
     * @return El panel de hallazgos
     */
    public PanelHallazgos obtenerPanelHallazgos() {
        return panelHallazgos;
    }

    /**
     * Establece el manejador para el botón de configuración.
     *
     * @param manejador El manejador a ejecutar cuando se presiona el botón
     */
    public void establecerManejadorConfiguracion(Runnable manejador) {
        panelEstadisticas.establecerManejadorConfiguracion(manejador);
    }

    /**
     * Establece el manejador para el toggle de captura.
     *
     * @param manejador El manejador a ejecutar cuando se cambia el estado de captura
     */
    public void establecerManejadorToggleCaptura(Runnable manejador) {
        panelEstadisticas.establecerManejadorToggleCaptura(manejador);
    }

    /**
     * Establece el manejador para cambios en la configuración del agente.
     *
     * @param manejador El manejador a ejecutar cuando cambia la configuración del agente
     */
    public void establecerManejadorCambioAgente(Runnable manejador) {
        panelAgente.establecerManejadorCambioConfiguracion(manejador);
    }

    /**
     * Establece el estado de captura de tráfico.
     *
     * @param activa true si la captura está activa, false en caso contrario
     */
    public void establecerEstadoCaptura(boolean activa) {
        panelEstadisticas.establecerEstadoCaptura(activa);
    }

    /**
     * Establece si el guardado automático de issues está activo.
     *
     * @param activo true si está activo, false en caso contrario
     */
    public void establecerGuardadoAutomaticoIssuesActivo(boolean activo) {
        panelHallazgos.establecerGuardadoAutomaticoIssuesActivo(activo);
    }

    /**
     * Establece el manejador para cambios en el guardado automático de issues.
     *
     * @param manejador El consumidor que recibe el nuevo estado
     */
    public void establecerManejadorAutoGuardadoIssues(Consumer<Boolean> manejador) {
        panelHallazgos.establecerManejadorCambioGuardadoIssues(manejador);
    }

    /**
     * Establece si el auto-scroll de la consola está activo.
     *
     * @param activo true si está activo, false en caso contrario
     */
    public void establecerAutoScrollConsolaActivo(boolean activo) {
        panelConsola.establecerAutoScrollActivo(activo);
    }

    /**
     * Establece el manejador para cambios en el auto-scroll de la consola.
     *
     * @param manejador El consumidor que recibe el nuevo estado
     */
    public void establecerManejadorAutoScrollConsola(Consumer<Boolean> manejador) {
        panelConsola.establecerManejadorCambioAutoScroll(manejador);
    }

    /**
     * Establece el manejador para reintentar tareas.
     *
     * @param manejador La función que recibe el ID de la tarea y devuelve true si se reintento exitosamente
     */
    public void establecerManejadorReintentoTareas(Function<String, Boolean> manejador) {
        panelTareas.establecerManejadorReintento(manejador);
    }

    /**
     * Establece el manejador para enviar hallazgos al agente.
     *
     * @param manejador El predicado que recibe el hallazgo y devuelve true si se envió exitosamente
     */
    public void establecerManejadorEnviarAAgente(Predicate<Hallazgo> manejador) {
        panelHallazgos.establecerManejadorEnviarAAgente(manejador);
    }

    /**
     * Establece el manejador para cambios en las alertas de envío.
     *
     * @param manejador El manejador a ejecutar cuando cambian las alertas
     */
    public void establecerManejadorAlertasEnviarA(Runnable manejador) {
        panelHallazgos.establecerManejadorCambioAlertasEnviarA(manejador);
    }

    /**
     * Establece el manejador para cambios en los filtros de hallazgos.
     *
     * @param manejador El manejador a ejecutar cuando cambian los filtros
     */
    public void establecerManejadorCambioFiltros(Runnable manejador) {
        panelHallazgos.establecerManejadorCambioFiltros(manejador);
    }

    /**
     * Aplica el idioma configurado a todos los componentes de la interfaz.
     */
    public void aplicarIdioma() {
        UIUtils.ejecutarEnEdtYEsperar(() -> {
            panelEstadisticas.aplicarIdioma();
            panelTareas.aplicarIdioma();
            panelHallazgos.aplicarIdioma();
            panelConsola.aplicarIdioma();
            panelAgente.aplicarIdioma();
            actualizarMetadatosPestaniasVisibles();
        });
    }

    /**
     * Aplica el tema visual a todos los componentes de la interfaz.
     */
    public void aplicarTema() {
        UIUtils.ejecutarEnEdtYEsperar(() -> {
            panelEstadisticas.aplicarTema();
            panelConsola.aplicarTema();
            panelAgente.aplicarTema();
            panelTareas.aplicarTema();
            panelHallazgos.aplicarTema();
            tabbedPane.repaint();
            repaint();
        });
    }

    /**
     * Libera todos los recursos asociados a este panel.
     * Debe llamarse cuando el panel ya no se va a usar.
     */
    public void destruir() {
        try {
            guardarPestaniaSeleccionadaActual();
            guardarAnchosColumnasActuales();
        } catch (Exception e) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.tr("Error al persistir estado UI de pestaña principal"));
        }

        UIManager.removePropertyChangeListener(listenerLookAndFeel);
        if (timerFocoAgente != null) {
            timerFocoAgente.stop();
            timerFocoAgente = null;
        }
        panelEstadisticas.destruir();
        panelConsola.destruir();
        panelTareas.destruir();
        panelHallazgos.destruir();
        panelAgente.destruir();
    }

    private void registrarComponentePestania(DestinoPestania destino, Component componente) {
        if (destino == null || componente == null) {
            return;
        }
        componente.setName(destino.obtenerIdentificadorPersistencia());
        componentesPorDestino.put(destino, componente);
    }

    private void agregarPestania(DestinoPestania destino) {
        insertarPestania(destino, tabbedPane.getTabCount());
    }

    private void insertarPestania(DestinoPestania destino, int index) {
        Component componente = resolverComponentePestania(destino);
        if (destino == null || componente == null || tabbedPane.indexOfComponent(componente) >= 0) {
            return;
        }
        int indiceSeguro = Math.max(0, Math.min(index, tabbedPane.getTabCount()));
        tabbedPane.insertTab(destino.obtenerTitulo(), null, componente, destino.obtenerTooltip(), indiceSeguro);
    }

    private int obtenerIndicePestania(DestinoPestania destino) {
        Component componente = resolverComponentePestania(destino);
        return componente != null ? tabbedPane.indexOfComponent(componente) : -1;
    }

    private void actualizarMetadatosPestaniasVisibles() {
        for (DestinoPestania destino : DestinoPestania.values()) {
            actualizarMetadatosPestania(destino);
        }
    }

    private void actualizarMetadatosPestania(DestinoPestania destino) {
        if (destino == null) {
            return;
        }
        int indice = obtenerIndicePestania(destino);
        if (indice < 0) {
            return;
        }
        tabbedPane.setTitleAt(indice, destino.obtenerTitulo());
        tabbedPane.setToolTipTextAt(indice, destino.obtenerTooltip());
    }

    private void restaurarAnchosColumnasActuales() {
        uiStateManager.restaurarAnchosColumnasTabla(panelHallazgos.obtenerTabla(), ID_TABLA_HALLAZGOS);
        uiStateManager.restaurarAnchosColumnasTabla(panelTareas.obtenerTabla(), ID_TABLA_TAREAS);
    }

    private void guardarAnchosColumnasActuales() {
        uiStateManager.guardarAnchosColumnasTabla(panelHallazgos.obtenerTabla(), ID_TABLA_HALLAZGOS);
        uiStateManager.guardarAnchosColumnasTabla(panelTareas.obtenerTabla(), ID_TABLA_TAREAS);
    }

    private void guardarPestaniaSeleccionadaActual() {
        DestinoPestania destinoSeleccionado = obtenerDestinoSeleccionado();
        if (destinoSeleccionado == null) {
            return;
        }
        uiStateManager.guardarUltimaPestaniaSeleccionada(
            tabbedPane,
            destinoSeleccionado.obtenerIdentificadorPersistencia()
        );
    }

    private void registrarPersistenciaAnchosColumnas(JTable tabla, String identificadorTabla) {
        if (tabla == null) {
            return;
        }
        tabla.getColumnModel().addColumnModelListener(crearListenerPersistenciaAnchos(tabla, identificadorTabla));
    }

    private TableColumnModelListener crearListenerPersistenciaAnchos(JTable tabla, String identificadorTabla) {
        Timer timerAnchos = new Timer(
            DELAY_GUARDADO_ANCHO_COLUMNAS_MS,
            e -> uiStateManager.guardarAnchosColumnasTabla(tabla, identificadorTabla)
        );
        timerAnchos.setRepeats(false);
        return new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
                timerAnchos.restart();
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        };
    }
}
