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
    private final PanelEstadisticas panelEstadisticas;
    private final PanelTareas panelTareas;
    private final PanelHallazgos panelHallazgos;
    private final PanelConsola panelConsola;
    private final PanelFactoryDroid panelFactoryDroid;
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
        this.panelFactoryDroid = new PanelFactoryDroid(config);
        this.panelFactoryDroid.establecerManejadorFocoPestania(this::seleccionarPestaniaFactoryDroid);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(EstilosUI.FUENTE_NEGRITA);
        tabbedPane.addTab(I18nUI.Pestanias.TAREAS(), panelTareas);
        tabbedPane.addTab(I18nUI.Pestanias.HALLAZGOS(), panelHallazgos);

        if (config.agenteFactoryDroidHabilitado()) {
            tabbedPane.addTab(I18nUI.Pestanias.AGENTE_FACTORY_DROID(), panelFactoryDroid);
        }
        tabbedPane.addTab(I18nUI.Pestanias.CONSOLA(), panelConsola);
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

    public PanelFactoryDroid obtenerPanelFactoryDroid() {
        return panelFactoryDroid;
    }

    public void actualizarVisibilidadAgentes() {
        boolean habilitado = config.agenteFactoryDroidHabilitado();
        int index = tabbedPane.indexOfComponent(panelFactoryDroid);

        if (habilitado && index == -1) {
            int indexConsola = tabbedPane.indexOfComponent(panelConsola);
            if (indexConsola != -1) {
                tabbedPane.insertTab(I18nUI.Pestanias.AGENTE_FACTORY_DROID(), null, panelFactoryDroid, null, indexConsola);
            } else {
                tabbedPane.addTab(I18nUI.Pestanias.AGENTE_FACTORY_DROID(), panelFactoryDroid);
            }
        } else if (!habilitado && index != -1) {
            tabbedPane.removeTabAt(index);
        }
    }

    public void seleccionarPestaniaFactoryDroid() {
        int index = tabbedPane.indexOfComponent(panelFactoryDroid);
        if (index != -1) {
            tabbedPane.setSelectedIndex(index);
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
        panelFactoryDroid.aplicarIdioma();

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c == panelTareas) tabbedPane.setTitleAt(i, I18nUI.Pestanias.TAREAS());
            else if (c == panelHallazgos) tabbedPane.setTitleAt(i, I18nUI.Pestanias.HALLAZGOS());
            else if (c == panelConsola) tabbedPane.setTitleAt(i, I18nUI.Pestanias.CONSOLA());
            else if (c == panelFactoryDroid) tabbedPane.setTitleAt(i, I18nUI.Pestanias.AGENTE_FACTORY_DROID());
        }
    }

    public void destruir() {
        panelEstadisticas.destruir();
        panelConsola.destruir();
        panelTareas.destruir();
        panelHallazgos.destruir();
    }
}
