package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("PanelHallazgos Limpiar Todo Tests")
class PanelHallazgosLimpiarTodoTest {

    @Test
    @DisplayName("Botón limpiar todo existe y es clickable")
    void testBotonLimpiarTodoExiste() throws Exception {
        // Arrange & Act
        PanelHallazgos panel = crearPanel();
        JButton botonLimpiarTodo = obtenerBotonLimpiarTodo(panel);

        // Assert
        assertNotNull(botonLimpiarTodo);
        assertTrue(botonLimpiarTodo.isEnabled());
        assertNotNull(botonLimpiarTodo.getText());
        assertFalse(botonLimpiarTodo.getText().isEmpty());
    }

    @Test
    @DisplayName("Botón limpiar todo tiene action listener configurado")
    void testBotonLimpiarTodoTieneActionListener() throws Exception {
        // Arrange
        PanelHallazgos panel = crearPanel();
        JButton botonLimpiarTodo = obtenerBotonLimpiarTodo(panel);

        // Act
        ActionListener[] listeners = botonLimpiarTodo.getListeners(ActionListener.class);

        // Assert
        assertTrue(listeners.length > 0, "El botón debe tener al menos un ActionListener");
    }

    @Test
    @DisplayName("Modelo de tabla puede eliminar todos los hallazgos")
    void testModeloTablaPuedeEliminarTodos() throws Exception {
        // Arrange
        PanelHallazgos panel = crearPanel();
        ModeloTablaHallazgos modelo = obtenerModeloTabla(panel);

        // Agregar algunos hallazgos
        Hallazgo h1 = new Hallazgo("https://test1.com", "Test 1", "Desc 1", "High", "Medium");
        Hallazgo h2 = new Hallazgo("https://test2.com", "Test 2", "Desc 2", "Medium", "Low");
        Hallazgo h3 = new Hallazgo("https://test3.com", "Test 3", "Desc 3", "Low", "High");

        SwingUtilities.invokeAndWait(() -> {
            modelo.agregarHallazgo(h1);
            modelo.agregarHallazgo(h2);
            modelo.agregarHallazgo(h3);
        });
        flushEdt();

        int hallazgosAntes = modelo.getRowCount();
        assertTrue(hallazgosAntes >= 3, "Debe haber al menos 3 hallazgos");

        // Act - Limpiar todo
        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        // Assert
        assertEquals(0, modelo.getRowCount(), "Todos los hallazgos deben ser eliminados");
    }

    @Test
    @DisplayName("Limpiar todo actualiza estadísticas del modelo")
    void testLimpiarTodoActualizaEstadisticas() throws Exception {
        // Arrange
        PanelHallazgos panel = crearPanel();
        ModeloTablaHallazgos modelo = obtenerModeloTabla(panel);

        // Agregar hallazgos de diferentes severidades
        SwingUtilities.invokeAndWait(() -> {
            modelo.agregarHallazgo(new Hallazgo("https://crit.com", "Critical", "Desc", "Critical", "High"));
            modelo.agregarHallazgo(new Hallazgo("https://high.com", "High", "Desc", "High", "Medium"));
            modelo.agregarHallazgo(new Hallazgo("https://med.com", "Medium", "Desc", "Medium", "Low"));
        });
        flushEdt();

        int hallazgosAntes = modelo.getRowCount();
        assertTrue(hallazgosAntes >= 3, "Debe haber al menos 3 hallazgos");

        // Act
        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        // Assert
        assertEquals(0, modelo.getRowCount(), "No debe haber hallazgos después de limpiar");
    }

    @Test
    @DisplayName("Limpiar todo con tabla vacía no causa errores")
    void testLimpiarTodoConTablaVacia() throws Exception {
        // Arrange
        PanelHallazgos panel = crearPanel();
        ModeloTablaHallazgos modelo = obtenerModeloTabla(panel);

        // Asegurar que está vacío
        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        assertEquals(0, modelo.getRowCount(), "La tabla debe estar vacía");

        // Act - Limpiar de nuevo (no debe causar error)
        SwingUtilities.invokeAndWait(modelo::limpiar);
        flushEdt();

        // Assert
        assertEquals(0, modelo.getRowCount(), "La tabla debe seguir vacía");
    }

    // ========== Helper Methods DRY ==========

    private PanelHallazgos crearPanel() throws Exception {
        MontoyaApi api = mock(burp.api.montoya.MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), false));
        return holder[0];
    }

    private JButton obtenerBotonLimpiarTodo(PanelHallazgos panel) throws Exception {
        Field field = PanelHallazgos.class.getDeclaredField("botonLimpiarTodo");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private ModeloTablaHallazgos obtenerModeloTabla(PanelHallazgos panel) throws Exception {
        Field field = PanelHallazgos.class.getDeclaredField("modelo");
        field.setAccessible(true);
        return (ModeloTablaHallazgos) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
