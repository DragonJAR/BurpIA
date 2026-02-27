package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private Timer timerFocoAgente;

    public PestaniaPrincipal(MontoyaApi api,
                            Estadisticas estadisticas,
                            GestorTareas gestorTareas,
                            GestorConsolaGUI gestorConsola,
                            ModeloTablaTareas modeloTareas,
                            ModeloTablaHallazgos modeloHallazgos,
                            boolean esBurpProfessional,
                            ConfiguracionAPI config) {
        setLayout(new BorderLayout(10, 2));
        this.config = config;

        panelEstadisticas = new PanelEstadisticas(estadisticas, modeloHallazgos::obtenerLimiteFilas);

        panelTareas = new PanelTareas(gestorTareas, modeloTareas);
        panelHallazgos = new PanelHallazgos(api, modeloHallazgos, esBurpProfessional);
        panelHallazgos.establecerConfiguracion(config);
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

        add(panelEstadisticas, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        panelHallazgos.agregarHallazgo(hallazgo);
    }


    public void registrar(String mensaje) {
        panelConsola.obtenerGestorConsola().registrarInfo(mensaje);
    }

    public void actualizarEstadisticas() {
        panelEstadisticas.actualizar();
    }

    public JPanel obtenerPanel() {
        return this;
    }

    public PanelConsola obtenerPanelConsola() {
        return panelConsola;
    }

    public PanelAgente obtenerPanelAgente() {
        return panelAgente;
    }

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
            if (!java.util.Objects.equals(idActual, panelAgente.obtenerUltimoAgenteIniciado())) {
                panelAgente.reiniciar();
            }
        }
    }

    public void seleccionarPestaniaAgente() {
        enfocarPestania(DestinoPestania.AGENTE, true);
    }

    private void manejarCambioPestania() {
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

        SwingUtilities.invokeLater(() -> enfocarComponenteSeleccionado(componente, traerVentanaAlFrente));
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
            default:
                return null;
        }
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
            default:
                return null;
        }
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
            default:
                return null;
        }
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
            SwingUtilities.invokeLater(() -> {
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

    public PanelHallazgos obtenerPanelHallazgos() {
        return panelHallazgos;
    }

    public void establecerManejadorConfiguracion(Runnable manejador) {
        panelEstadisticas.establecerManejadorConfiguracion(manejador);
    }

    public void establecerManejadorToggleCaptura(Runnable manejador) {
        panelEstadisticas.establecerManejadorToggleCaptura(manejador);
    }

    public void establecerManejadorCambioAgente(Runnable manejador) {
        panelAgente.establecerManejadorCambioConfiguracion(manejador);
    }

    public void establecerEstadoCaptura(boolean activa) {
        panelEstadisticas.establecerEstadoCaptura(activa);
    }

    public void establecerGuardadoAutomaticoIssuesActivo(boolean activo) {
        panelHallazgos.establecerGuardadoAutomaticoIssuesActivo(activo);
    }

    public void establecerManejadorAutoGuardadoIssues(Consumer<Boolean> manejador) {
        panelHallazgos.establecerManejadorCambioGuardadoIssues(manejador);
    }

    public void establecerAutoScrollConsolaActivo(boolean activo) {
        panelConsola.establecerAutoScrollActivo(activo);
    }

    public void establecerManejadorAutoScrollConsola(Consumer<Boolean> manejador) {
        panelConsola.establecerManejadorCambioAutoScroll(manejador);
    }

    public void establecerManejadorReintentoTareas(Function<String, Boolean> manejador) {
        panelTareas.establecerManejadorReintento(manejador);
    }

    public void establecerManejadorEnviarAAgente(java.util.function.Consumer<Hallazgo> manejador) {
        panelHallazgos.establecerManejadorEnviarAAgente(manejador);
    }

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

    public void destruir() {
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
