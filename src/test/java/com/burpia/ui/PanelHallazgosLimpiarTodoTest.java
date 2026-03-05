package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("PanelHallazgos Limpiar Todo Tests")
class PanelHallazgosLimpiarTodoTest {

    private static final String CAMPO_BOTON_LIMPIAR_TODO = "botonLimpiarTodo";
    private static final String CAMPO_MODELO = "modelo";

    @Test
    @DisplayName("Botón limpiar todo existe y es clickable")
    void testBotonLimpiarTodoExiste() throws Exception {
        PanelHallazgos panel = crearPanel();
        JButton botonLimpiarTodo = obtenerCampo(panel, CAMPO_BOTON_LIMPIAR_TODO, JButton.class);

        assertNotNull(botonLimpiarTodo);
        assertTrue(botonLimpiarTodo.isEnabled());
        assertNotNull(botonLimpiarTodo.getText());
        assertFalse(botonLimpiarTodo.getText().isEmpty());
    }

    @Test
    @DisplayName("Botón limpiar todo tiene action listener configurado")
    void testBotonLimpiarTodoTieneActionListener() throws Exception {
        PanelHallazgos panel = crearPanel();
        JButton botonLimpiarTodo = obtenerCampo(panel, CAMPO_BOTON_LIMPIAR_TODO, JButton.class);

        ActionListener[] listeners = botonLimpiarTodo.getListeners(ActionListener.class);

        assertTrue(listeners.length > 0, "El botón debe tener al menos un ActionListener");
    }

    @Test
    @DisplayName("Modelo de tabla puede eliminar todos los hallazgos")
    void testModeloTablaPuedeEliminarTodos() throws Exception {
        PanelHallazgos panel = crearPanel();
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
        PanelHallazgos panel = crearPanel();
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
        assertArrayEquals(new int[6], estadisticasDespues, "Todas las estadísticas deben ser cero");
    }

    @Test
    @DisplayName("Limpiar todo con tabla vacía no causa errores")
    void testLimpiarTodoConTablaVacia() throws Exception {
        PanelHallazgos panel = crearPanel();
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

    // Métodos auxiliares

    private PanelHallazgos crearPanel() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), false));
        PanelHallazgos panel = holder[0];
        assertNotNull(panel, "El panel debe haberse creado correctamente");
        return panel;
    }

    @SuppressWarnings("unchecked")
    private <T> T obtenerCampo(Object target, String nombreCampo, Class<T> tipo) throws Exception {
        Field field = target.getClass().getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
