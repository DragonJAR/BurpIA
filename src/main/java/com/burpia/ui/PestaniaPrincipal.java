package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class PestaniaPrincipal extends JPanel {
    private final PanelEstadisticas panelEstadisticas;
    private final PanelTareas panelTareas;
    private final PanelHallazgos panelHallazgos;
    private final PanelConsola panelConsola;
    private final PanelAgente panelAgente;
    private final JTabbedPane tabbedPane;
    private final ConfiguracionAPI config;

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
        this.panelAgente = new PanelAgente(config);
        this.panelAgente.establecerManejadorFocoPestania(this::seleccionarPestaniaAgente);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(EstilosUI.FUENTE_NEGRITA);
        tabbedPane.addTab(I18nUI.Pestanias.TAREAS(), panelTareas);
        tabbedPane.addTab(I18nUI.Pestanias.HALLAZGOS(), panelHallazgos);

        if (config.agenteHabilitado()) {
            tabbedPane.addTab(obtenerNombrePestaniaAgente(), panelAgente);
        }
        tabbedPane.addTab(I18nUI.Pestanias.CONSOLA(), panelConsola);
        tabbedPane.setSelectedComponent(panelConsola);
        aplicarTooltipsPestanias();
        aplicarIdioma();

        add(panelEstadisticas, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        panelHallazgos.agregarHallazgo(hallazgo);
    }
    
    private String obtenerNombrePestaniaAgente() {
        String nombreAgente = AgenteTipo.obtenerNombreVisible(
            config.obtenerTipoAgente(),
            I18nUI.General.AGENTE_GENERICO()
        );
        return I18nUI.Pestanias.AGENTE_DINAMICO(nombreAgente.toUpperCase(Locale.ROOT));
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
                tabbedPane.insertTab(obtenerNombrePestaniaAgente(), null, panelAgente, null, indexConsola);
            } else {
                tabbedPane.addTab(obtenerNombrePestaniaAgente(), panelAgente);
            }
            panelAgente.reinyectarPromptInicial();
        } else if (!habilitado && index != -1) {
            tabbedPane.removeTabAt(index);
        } else if (habilitado && index != -1) {
            tabbedPane.setTitleAt(index, obtenerNombrePestaniaAgente());
        }
    }

    public void seleccionarPestaniaAgente() {
        int index = tabbedPane.indexOfComponent(panelAgente);
        if (index != -1) {
            tabbedPane.setSelectedIndex(index);
            
            new Timer(150, e -> {
                ((Timer) e.getSource()).stop();
                SwingUtilities.invokeLater(() -> {
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window != null) {
                        window.toFront();
                    }
                    panelAgente.enfocarTerminal();
                });
            }).start();
        }
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

    public void establecerManejadorEnviarADroid(Consumer<Hallazgo> manejador) {
        panelHallazgos.establecerManejadorEnviarADroid(manejador);
    }

    public void aplicarIdioma() {
        panelEstadisticas.aplicarIdioma();
        panelTareas.aplicarIdioma();
        panelHallazgos.aplicarIdioma();
        panelConsola.aplicarIdioma();
        panelAgente.aplicarIdioma();

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c == panelTareas) tabbedPane.setTitleAt(i, I18nUI.Pestanias.TAREAS());
            else if (c == panelHallazgos) tabbedPane.setTitleAt(i, I18nUI.Pestanias.HALLAZGOS());
            else if (c == panelConsola) tabbedPane.setTitleAt(i, I18nUI.Pestanias.CONSOLA());
            else if (c == panelAgente) tabbedPane.setTitleAt(i, obtenerNombrePestaniaAgente());
        }
        aplicarTooltipsPestanias();
    }

    private void aplicarTooltipsPestanias() {
        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c == panelTareas) tabbedPane.setToolTipTextAt(i, I18nUI.Tooltips.Pestanias.TAREAS());
            else if (c == panelHallazgos) tabbedPane.setToolTipTextAt(i, I18nUI.Tooltips.Pestanias.HALLAZGOS());
            else if (c == panelConsola) tabbedPane.setToolTipTextAt(i, I18nUI.Tooltips.Pestanias.CONSOLA());
            else if (c == panelAgente) tabbedPane.setToolTipTextAt(i, "Agent Terminal");
        }
    }

    public void destruir() {
        panelEstadisticas.destruir();
        panelConsola.destruir();
        panelTareas.destruir();
        panelHallazgos.destruir();
    }
}
