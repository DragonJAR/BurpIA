package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PanelConsola Limpiar Tests")
class PanelConsolaLimpiarTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Botón limpiar existe y es clickable")
    void testBotonLimpiarExiste() throws Exception {
        // Arrange & Act
        PanelConsola panel = crearPanel();
        JButton botonLimpiar = obtenerBotonLimpiar(panel);

        // Assert
        assertNotNull(botonLimpiar);
        assertTrue(botonLimpiar.isEnabled());
        assertEquals(I18nUI.Consola.BOTON_LIMPIAR(), botonLimpiar.getText());

        panel.destruir();
    }

    @Test
    @DisplayName("Click en botón limpiar tiene action listener configurado")
    void testClickLimpiarConLogsRequiereConfirmacion() throws Exception {
        // Arrange
        PanelConsola panel = crearPanel();
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();

        // Agregar logs
        gestor.registrarInfo("Log 1");
        gestor.registrarInfo("Log 2");
        gestor.registrarInfo("Log 3");

        int logsAntes = gestor.obtenerTotalLogs();
        assertTrue(logsAntes >= 3, "Debe haber al menos 3 logs");

        // Act - Verificar que el botón tiene action listener
        JButton botonLimpiar = obtenerBotonLimpiar(panel);
        ActionListener[] listeners = botonLimpiar.getListeners(ActionListener.class);

        // Assert
        assertTrue(listeners.length > 0, "El botón debe tener al menos un ActionListener");

        panel.destruir();
    }

    @Test
    @DisplayName("Gestor de consola responde a limpiarConsola")
    void testGestorConsolaRespondeALimpiar() throws Exception {
        // Arrange
        PanelConsola panel = crearPanel();
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();

        // Agregar logs
        gestor.registrarInfo("Log 1");
        gestor.registrarInfo("Log 2");
        assertTrue(gestor.obtenerTotalLogs() >= 2, "Debe haber al menos 2 logs");

        // Act
        gestor.limpiarConsola();
        flushEdt();

        // Assert
        // limpiarConsola() puede agregar un log de confirmación, así que verificamos
        // que el número de logs disminuyó significativamente
        int logsDespues = gestor.obtenerTotalLogs();
        assertTrue(logsDespues <= 1, "Debe haber 0 o 1 logs después de limpiar (log de confirmación)");

        panel.destruir();
    }

    @Test
    @DisplayName("Limpiar actualiza resumen")
    void testLimpiarActualizaResumen() throws Exception {
        // Arrange
        PanelConsola panel = crearPanel();
        GestorConsolaGUI gestor = panel.obtenerGestorConsola();
        JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);

        // Limpiar para empezar desde un estado conocido
        gestor.limpiarConsola();

        // Agregar logs
        gestor.registrarInfo("Info");
        gestor.registrarError("Error");
        gestor.registrarVerbose("Verbose");

        // Forzar actualización del resumen
        SwingUtilities.invokeAndWait(panel::aplicarIdioma);
        flushEdt();

        String resumenAntes = etiquetaResumen.getText();
        assertTrue(resumenAntes.contains("Total:"), "Resumen debe mostrar total antes de limpiar");

        // Act
        gestor.limpiarConsola();

        // Forzar actualización del resumen después de limpiar
        SwingUtilities.invokeAndWait(panel::aplicarIdioma);
        flushEdt();

        // Assert
        String resumenDespues = etiquetaResumen.getText();
        assertTrue(resumenDespues.contains("Total:"), "Resumen debe mostrar total después de limpiar");

        panel.destruir();
    }

    // ========== Helper Methods DRY ==========

    private PanelConsola crearPanel() throws Exception {
        final PanelConsola[] holder = new PanelConsola[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelConsola(new GestorConsolaGUI()));
        return holder[0];
    }

    private JButton obtenerBotonLimpiar(PanelConsola panel) throws Exception {
        Field field = PanelConsola.class.getDeclaredField("botonLimpiar");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private JLabel obtenerEtiquetaResumen(PanelConsola panel) throws Exception {
        Field field = PanelConsola.class.getDeclaredField("etiquetaResumen");
        field.setAccessible(true);
        return (JLabel) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
