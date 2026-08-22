package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PanelHallazgos Limpiar Todo Tests")
class PanelHallazgosLimpiarTodoTest extends PanelTestBase {

    private static final String CAMPO_BOTON_LIMPIAR_TODO = "botonLimpiarTodo";
    private static final String CAMPO_MODELO = "modelo";
    // Array de estadísticas visibles: [total, critical, high, medium, low, info]
    private static final int NUMERO_CATEGORIAS_ESTADISTICAS = 6;

    @Test
    @DisplayName("Botón limpiar todo existe y es clickable")
    void testBotonLimpiarTodoExiste() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        JButton botonLimpiarTodo = obtenerCampo(panel, CAMPO_BOTON_LIMPIAR_TODO, JButton.class);

        assertNotNull(botonLimpiarTodo, "assertNotNull failed at PanelHallazgosLimpiarTodoTest.java:28");
        assertTrue(botonLimpiarTodo.isEnabled(), "assertTrue failed at PanelHallazgosLimpiarTodoTest.java:29");
        assertNotNull(botonLimpiarTodo.getText(), "assertNotNull failed at PanelHallazgosLimpiarTodoTest.java:30");
        assertFalse(botonLimpiarTodo.getText().isEmpty(), "assertFalse failed at PanelHallazgosLimpiarTodoTest.java:31");
    }

    @Test
    @DisplayName("Botón limpiar todo tiene action listener configurado")
    void testBotonLimpiarTodoTieneActionListener() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        JButton botonLimpiarTodo = obtenerCampo(panel, CAMPO_BOTON_LIMPIAR_TODO, JButton.class);

        ActionListener[] listeners = botonLimpiarTodo.getListeners(ActionListener.class);

        assertTrue(listeners.length > 0, "El botón debe tener al menos un ActionListener");
    }

    @Test
    @DisplayName("Modelo de tabla puede eliminar todos los hallazgos")
    void testModeloTablaPuedeEliminarTodos() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        ModeloTablaHallazgos modelo = obtenerCampo(panel, CAMPO_MODELO, ModeloTablaHallazgos.class);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://test1.com", "Test 1", "Desc 1", "High", "Medium"),
            new Hallazgo("https://test2.com", "Test 2", "Desc 2", "Medium", "Low"),
            new Hallazgo("https://test3.com", "Test 3", "Desc 3", "Low", "High")
        ));
        flushEdt();

        assertTrue(modelo.getRowCount() >= 3, "Debe haber al menos 3 hallazgos");

        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        assertEquals(0, modelo.getRowCount(), "Todos los hallazgos deben ser eliminados");
    }

    @Test
    @DisplayName("Limpiar todo actualiza estadísticas del modelo")
    void testLimpiarTodoActualizaEstadisticas() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        ModeloTablaHallazgos modelo = obtenerCampo(panel, CAMPO_MODELO, ModeloTablaHallazgos.class);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://crit.com", "Critical", "Desc", "Critical", "High"),
            new Hallazgo("https://high.com", "High", "Desc", "High", "Medium"),
            new Hallazgo("https://med.com", "Medium", "Desc", "Medium", "Low")
        ));
        flushEdt();

        int[] estadisticasAntes = modelo.obtenerEstadisticasVisibles();
        assertEquals(3, estadisticasAntes[0], "Debe haber 3 hallazgos totales antes de limpiar");

        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        int[] estadisticasDespues = modelo.obtenerEstadisticasVisibles();
        assertEquals(0, modelo.getRowCount(), "No debe haber hallazgos después de limpiar");
        assertArrayEquals(new int[NUMERO_CATEGORIAS_ESTADISTICAS], estadisticasDespues,
            "Todas las estadísticas deben ser cero");
    }

    @Test
    @DisplayName("Limpiar todo con tabla vacía no causa errores")
    void testLimpiarTodoConTablaVacia() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        ModeloTablaHallazgos modelo = obtenerCampo(panel, CAMPO_MODELO, ModeloTablaHallazgos.class);

        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        assertEquals(0, modelo.getRowCount(), "La tabla debe estar vacía");

        assertDoesNotThrow(() -> {
            SwingUtilities.invokeAndWait(modelo::limpiar);
            flushEdt();
        });

        assertEquals(0, modelo.getRowCount(), "La tabla debe seguir vacía");
    }
}
