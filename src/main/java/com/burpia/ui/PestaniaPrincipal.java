package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;

import javax.swing.*;
import java.awt.*;

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
                            ModeloTablaHallazgos modeloHallazgos) {
        setLayout(new BorderLayout(10, 10));

        panelEstadisticas = new PanelEstadisticas(estadisticas);

        panelTareas = new PanelTareas(gestorTareas, modeloTareas);
        panelHallazgos = new PanelHallazgos(api, modeloHallazgos);
        panelConsola = new PanelConsola(gestorConsola);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addTab("📋 Tareas", panelTareas);
        tabbedPane.addTab("🔍 Hallazgos", panelHallazgos);
        tabbedPane.addTab("📝 Consola", panelConsola);

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

    public void destruir() {
        panelEstadisticas.destruir();
        panelConsola.destruir();
        panelTareas.destruir();
    }
}
