package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.Timer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PestaniaPrincipal extends JPanel {
    private enum DestinoPestania {
        TAREAS,
        HALLAZGOS,
        AGENTE,
        CONSOLA
    }

    private final PanelEstadisticas panelEstadisticas;
    private final PanelTareas panelTareas;
    private final PanelHallazgos panelHallazgos;
    private final PanelConsola panelConsola;
    private final PanelAgente panelAgente;
    private final JTabbedPane tabbedPane;
    private final ConfiguracionAPI config;
    private final UIStateManager uiStateManager;
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
        this.uiStateManager = new UIStateManager(config, gestorLogging);

        panelTareas = new PanelTareas(gestorTareas, modeloTareas);
        panelTareas.establecerConfiguracion(config);
        panelHallazgos = new PanelHallazgos(api, modeloHallazgos, esBurpProfessional);
        panelHallazgos.establecerConfiguracion(config);

        panelEstadisticas = new PanelEstadisticas(estadisticas, modeloHallazgos::obtenerLimiteFilas, panelHallazgos);
        this.panelConsola = new PanelConsola(gestorConsola);
        this.panelAgente = new PanelAgente(config, config.agenteHabilitado());
        this.panelAgente.establecerManejadorFocoPestania(this::enfocarPestaniaAgenteDesdeManejador);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(EstilosUI.FUENTE_NEGRITA);
        tabbedPane.addTab(I18nUI.Pestanias.TAREAS(), panelTareas);
        tabbedPane.addTab(I18nUI.Pestanias.HALLAZGOS(), panelHallazgos);

        if (config.agenteHabilitado()) {
            tabbedPane.addTab(I18nUI.Pestanias.AGENTE(), panelAgente);
        }
        tabbedPane.addTab(I18nUI.Pestanias.CONSOLA(), panelConsola);
        tabbedPane.setSelectedComponent(panelConsola);
        tabbedPane.addChangeListener(e -> manejarCambioPestania());
        aplicarTooltipsPestanias();
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
            // Restaurar última pestaña seleccionada
            uiStateManager.restaurarUltimaPestaniaSeleccionada(tabbedPane, 3); // Consola por defecto
            
            // Restaurar filtros de hallazgos
            uiStateManager.restaurarEstadoFiltrosHallazgos(panelHallazgos);
            
            // Restaurar anchos de columna
            uiStateManager.restaurarAnchosColumnasTabla(panelHallazgos.obtenerTabla(), "hallazgos");
            uiStateManager.restaurarAnchosColumnasTabla(panelTareas.obtenerTabla(), "tareas");
            
        } catch (Exception e) {
            // Silencioso para no interrumpir inicialización
        }
    }

    /**
     * Configura listeners para guardar cambios de estado UI.
     */
    private void configurarListenersEstadoUI() {
        // Establecer UIStateManager en PanelHallazgos
        panelHallazgos.establecerUIStateManager(uiStateManager);
        
        // Listener para cambios de anchos de columna en hallazgos
        panelHallazgos.obtenerTabla().getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            private final Timer timerAnchos = new Timer(500, e -> {
                uiStateManager.guardarAnchosColumnasTabla(panelHallazgos.obtenerTabla(), "hallazgos");
            });
            
            {
                timerAnchos.setRepeats(false);
            }
            
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                timerAnchos.restart();
            }
            
            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
        
        // Listener para cambios de anchos de columna en tareas
        panelTareas.obtenerTabla().getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            private final Timer timerAnchos = new Timer(500, e -> {
                uiStateManager.guardarAnchosColumnasTabla(panelTareas.obtenerTabla(), "tareas");
            });
            
            {
                timerAnchos.setRepeats(false);
            }
            
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            
            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                timerAnchos.restart();
            }
            
            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
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
        boolean habilitado = config.agenteHabilitado();
        int index = tabbedPane.indexOfComponent(panelAgente);

        if (habilitado && index == -1) {
            int indexConsola = tabbedPane.indexOfComponent(panelConsola);
            if (indexConsola != -1) {
                tabbedPane.insertTab(I18nUI.Pestanias.AGENTE(), null, panelAgente, null, indexConsola);
            } else {
                tabbedPane.addTab(I18nUI.Pestanias.AGENTE(), panelAgente);
            }
            panelAgente.asegurarConsolaIniciada();
            panelAgente.reinyectarPromptInicial();
        } else if (!habilitado && index != -1) {
            tabbedPane.removeTabAt(index);
            panelAgente.destruir();
        } else if (habilitado && index != -1) {
            DestinoPestania destinoAgente = DestinoPestania.AGENTE;
            tabbedPane.setTitleAt(index, resolverTituloPestania(destinoAgente));
            tabbedPane.setToolTipTextAt(index, resolverTooltipPestania(destinoAgente));
            
            tabbedPane.revalidate();
            tabbedPane.repaint();

            panelAgente.asegurarConsolaIniciada();
            
            String idActual = config.obtenerTipoAgente();
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
        // Guardar última pestaña seleccionada
        int indiceSeleccionado = tabbedPane.getSelectedIndex();
        if (indiceSeleccionado >= 0) {
            String tituloPestania = tabbedPane.getTitleAt(indiceSeleccionado);
            uiStateManager.guardarUltimaPestaniaSeleccionada(tabbedPane, tituloPestania);
        }
        
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
        if (componente == panelTareas) {
            return DestinoPestania.TAREAS;
        }
        if (componente == panelHallazgos) {
            return DestinoPestania.HALLAZGOS;
        }
        if (componente == panelAgente) {
            return DestinoPestania.AGENTE;
        }
        if (componente == panelConsola) {
            return DestinoPestania.CONSOLA;
        }
        return null;
    }

    private Component resolverComponentePestania(DestinoPestania destino) {
        if (destino == null) {
            return null;
        }
        switch (destino) {
            case TAREAS:
                return panelTareas;
            case HALLAZGOS:
                return panelHallazgos;
            case AGENTE:
                return panelAgente;
            case CONSOLA:
                return panelConsola;
        }
        return null;
    }

    private String resolverTituloPestania(DestinoPestania destino) {
        if (destino == null) {
            return null;
        }
        switch (destino) {
            case TAREAS:
                return I18nUI.Pestanias.TAREAS();
            case HALLAZGOS:
                return I18nUI.Pestanias.HALLAZGOS();
            case AGENTE:
                return I18nUI.Pestanias.AGENTE();
            case CONSOLA:
                return I18nUI.Pestanias.CONSOLA();
        }
        return null;
    }

    private String resolverTooltipPestania(DestinoPestania destino) {
        if (destino == null) {
            return null;
        }
        switch (destino) {
            case TAREAS:
                return I18nUI.Tooltips.Pestanias.TAREAS();
            case HALLAZGOS:
                return I18nUI.Tooltips.Pestanias.HALLAZGOS();
            case AGENTE:
                return I18nUI.Tooltips.Pestanias.AGENTE();
            case CONSOLA:
                return I18nUI.Tooltips.Pestanias.CONSOLA();
        }
        return null;
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
        panelEstadisticas.aplicarIdioma();
        panelTareas.aplicarIdioma();
        panelHallazgos.aplicarIdioma();
        panelConsola.aplicarIdioma();
        panelAgente.aplicarIdioma();

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            DestinoPestania destino = resolverDestinoPestania(tabbedPane.getComponentAt(i));
            String titulo = resolverTituloPestania(destino);
            if (titulo != null) {
                tabbedPane.setTitleAt(i, titulo);
            }
        }
        aplicarTooltipsPestanias();
    }

    /**
     * Aplica el tema visual a todos los componentes de la interfaz.
     */
    public void aplicarTema() {
        panelEstadisticas.aplicarTema();
        panelConsola.aplicarTema();
        panelAgente.aplicarTema();
        panelTareas.aplicarTema();
        panelHallazgos.repaint();
        tabbedPane.repaint();
        repaint();
    }

    private void aplicarTooltipsPestanias() {
        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            DestinoPestania destino = resolverDestinoPestania(tabbedPane.getComponentAt(i));
            String tooltip = resolverTooltipPestania(destino);
            if (tooltip != null) {
                tabbedPane.setToolTipTextAt(i, tooltip);
            }
        }
    }

    /**
     * Libera todos los recursos asociados a este panel.
     * Debe llamarse cuando el panel ya no se va a usar.
     */
    public void destruir() {
        // Guardar estado final antes de destruir
        try {
            if (uiStateManager != null) {
                // Guardar última pestaña seleccionada
                int indiceSeleccionado = tabbedPane.getSelectedIndex();
                if (indiceSeleccionado >= 0) {
                    String tituloPestania = tabbedPane.getTitleAt(indiceSeleccionado);
                    uiStateManager.guardarUltimaPestaniaSeleccionada(tabbedPane, tituloPestania);
                }
                
                // Guardar anchos de columna finales
                uiStateManager.guardarAnchosColumnasTabla(panelHallazgos.obtenerTabla(), "hallazgos");
                uiStateManager.guardarAnchosColumnasTabla(panelTareas.obtenerTabla(), "tareas");
            }
        } catch (Exception e) {
            // Silencioso en el shutdown
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
}
