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
        PanelConsola panel = crearPanel();
        try {
            JButton botonLimpiar = obtenerBotonLimpiar(panel);

            assertNotNull(botonLimpiar, "assertNotNull failed at PanelConsolaLimpiarTest.java:30");
            assertTrue(botonLimpiar.isEnabled(), "assertTrue failed at PanelConsolaLimpiarTest.java:31");
            assertEquals(I18nUI.Consola.BOTON_LIMPIAR(), botonLimpiar.getText(), "assertEquals failed at PanelConsolaLimpiarTest.java:32");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Botón limpiar tiene action listener configurado")
    void testBotonLimpiarTieneActionListener() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();

            gestor.registrarInfo("Log 1");
            gestor.registrarInfo("Log 2");
            gestor.registrarInfo("Log 3");

            int logsAntes = gestor.obtenerTotalLogs();
            assertTrue(logsAntes >= 3, "Debe haber al menos 3 logs");

            JButton botonLimpiar = obtenerBotonLimpiar(panel);
            ActionListener[] listeners = botonLimpiar.getListeners(ActionListener.class);

            assertTrue(listeners.length > 0, "El botón debe tener al menos un ActionListener");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Gestor de consola responde a limpiarConsola reseteando contadores")
    void testGestorConsolaRespondeALimpiar() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();

            gestor.registrarInfo("Log 1");
            gestor.registrarInfo("Log 2");
            gestor.registrarError("Error 1");
            assertTrue(gestor.obtenerTotalLogs() >= 3, "Debe haber al menos 3 logs");

            gestor.limpiarConsola();
            flushEdt();

            assertEquals(0, gestor.obtenerTotalLogs(), "Total de logs debe ser 0 después de limpiar");
            assertEquals(0, gestor.obtenerContadorInfo(), "Contador info debe ser 0 después de limpiar");
            assertEquals(0, gestor.obtenerContadorError(), "Contador error debe ser 0 después de limpiar");
            assertEquals(0, gestor.obtenerContadorVerbose(), "Contador verbose debe ser 0 después de limpiar");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Limpiar actualiza resumen a cero")
    void testLimpiarActualizaResumen() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();
            JLabel etiquetaResumen = obtenerEtiquetaResumen(panel);

            // Limpiar primero para eliminar el log de inicialización del constructor
            SwingUtilities.invokeAndWait(() -> gestor.limpiarConsola());
            flushEdt();

            // Agregar logs de prueba
            gestor.registrarInfo("Info");
            gestor.registrarError("Error");
            gestor.registrarVerbose("Verbose");

            // Forzar actualización del resumen
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            flushEdt();

            String resumenAntes = etiquetaResumen.getText();
            assertTrue(resumenAntes.contains("Total: 3"), "Resumen debe mostrar Total: 3 antes de limpiar");

            // Limpiar consola
            SwingUtilities.invokeAndWait(() -> gestor.limpiarConsola());
            flushEdt();

            // Forzar actualización del resumen después de limpiar
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            flushEdt();

            String resumenDespues = etiquetaResumen.getText();
            assertTrue(resumenDespues.contains("Total: 0"), "Resumen debe mostrar Total: 0 después de limpiar");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Limpiar consola vacía mantiene estado válido")
    void testLimpiarConsolaVaciaMantieneEstadoValido() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();

            gestor.limpiarConsola();
            flushEdt();

            assertEquals(0, gestor.obtenerTotalLogs(), "assertEquals failed at PanelConsolaLimpiarTest.java:134");

            gestor.limpiarConsola();
            flushEdt();

            assertEquals(0, gestor.obtenerTotalLogs(), "Limpiar consola vacía no debe causar errores");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Contadores se reinician correctamente tras limpiar")
    void testContadoresSeReinicianCorrectamente() throws Exception {
        PanelConsola panel = crearPanel();
        try {
            GestorConsolaGUI gestor = panel.obtenerGestorConsola();

            // Limpiar primero para eliminar el log de inicialización del constructor
            SwingUtilities.invokeAndWait(() -> gestor.limpiarConsola());
            flushEdt();

            // Agregar logs de prueba
            for (int i = 0; i < 5; i++) {
                gestor.registrarInfo("Info " + i);
            }
            for (int i = 0; i < 3; i++) {
                gestor.registrarError("Error " + i);
            }
            for (int i = 0; i < 2; i++) {
                gestor.registrarVerbose("Verbose " + i);
            }

            assertEquals(5, gestor.obtenerContadorInfo(), "assertEquals failed at PanelConsolaLimpiarTest.java:167");
            assertEquals(3, gestor.obtenerContadorError(), "assertEquals failed at PanelConsolaLimpiarTest.java:168");
            assertEquals(2, gestor.obtenerContadorVerbose(), "assertEquals failed at PanelConsolaLimpiarTest.java:169");
            assertEquals(10, gestor.obtenerTotalLogs(), "assertEquals failed at PanelConsolaLimpiarTest.java:170");

            gestor.limpiarConsola();
            flushEdt();

            assertEquals(0, gestor.obtenerContadorInfo(), "Contador info debe reiniciarse a 0");
            assertEquals(0, gestor.obtenerContadorError(), "Contador error debe reiniciarse a 0");
            assertEquals(0, gestor.obtenerContadorVerbose(), "Contador verbose debe reiniciarse a 0");
            assertEquals(0, gestor.obtenerTotalLogs(), "Total debe ser 0");
        } finally {
            panel.destruir();
        }
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
