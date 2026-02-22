package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
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
    private final JTabbedPane tabbedPane;

    public PestaniaPrincipal(MontoyaApi api,
                            Estadisticas estadisticas,
                            GestorTareas gestorTareas,
                            GestorConsolaGUI gestorConsola,
                            ModeloTablaTareas modeloTareas,
                            ModeloTablaHallazgos modeloHallazgos,
                            boolean esBurpProfessional) {
        setLayout(new BorderLayout(10, 2));

        panelEstadisticas = new PanelEstadisticas(estadisticas, modeloHallazgos::obtenerLimiteFilas);

        panelTareas = new PanelTareas(gestorTareas, modeloTareas);
        panelHallazgos = new PanelHallazgos(api, modeloHallazgos, esBurpProfessional);
        panelConsola = new PanelConsola(gestorConsola);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(EstilosUI.FUENTE_NEGRITA);
        tabbedPane.addTab("", panelTareas);
        tabbedPane.addTab("", panelHallazgos);
        tabbedPane.addTab("", panelConsola);
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

    public void limpiarResultados() {
        panelHallazgos.limpiar();
    }

    public void actualizarEstadisticas() {
        panelEstadisticas.actualizar();
    }

    public JPanel obtenerPanel() {
        return this;
    }

    public PanelHallazgos obtenerPanelHallazgos() {
        return panelHallazgos;
    }

    public PanelTareas obtenerPanelTareas() {
        return panelTareas;
    }

    public PanelConsola obtenerPanelConsola() {
        return panelConsola;
    }

    public PanelEstadisticas obtenerPanelEstadisticas() {
        return panelEstadisticas;
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

    public void aplicarIdioma() {
        tabbedPane.setTitleAt(0, I18nUI.Pestanias.TAREAS());
        tabbedPane.setTitleAt(1, I18nUI.Pestanias.HALLAZGOS());
        tabbedPane.setTitleAt(2, I18nUI.Pestanias.CONSOLA());
        tabbedPane.setToolTipTextAt(0, TooltipsUI.Pestanias.TAREAS());
        tabbedPane.setToolTipTextAt(1, TooltipsUI.Pestanias.HALLAZGOS());
        tabbedPane.setToolTipTextAt(2, TooltipsUI.Pestanias.CONSOLA());
        panelEstadisticas.aplicarIdioma();
        panelTareas.aplicarIdioma();
        panelHallazgos.aplicarIdioma();
        panelConsola.aplicarIdioma();
    }

    public void destruir() {
        panelEstadisticas.destruir();
        panelConsola.destruir();
        panelTareas.destruir();
        panelHallazgos.destruir();
    }
}
