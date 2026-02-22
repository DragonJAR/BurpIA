package com.burpia.ui;

import com.burpia.model.Tarea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModeloTablaTareas Tests")
class ModeloTablaTareasTest {

    @Test
    @DisplayName("Normaliza limite invalido y conserva al menos una fila")
    void testNormalizaLimiteInvalido() throws Exception {
        ModeloTablaTareas modelo = new ModeloTablaTareas(0);
        modelo.agregarTarea(new Tarea("1", "A", "https://example.com/1", Tarea.ESTADO_EN_COLA));
        modelo.agregarTarea(new Tarea("2", "B", "https://example.com/2", Tarea.ESTADO_EN_COLA));

        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());
    }

    @Test
    @DisplayName("Eliminar con indice invalido no lanza excepcion ni modifica filas")
    void testEliminarIndiceInvalido() throws Exception {
        ModeloTablaTareas modelo = new ModeloTablaTareas(10);
        modelo.agregarTarea(new Tarea("1", "A", "https://example.com/1", Tarea.ESTADO_EN_COLA));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());

        assertDoesNotThrow(() -> modelo.eliminarTarea(-1));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());
    }

    @Test
    @DisplayName("Buscar y eliminar por id null es seguro")
    void testOperacionesIdNull() {
        ModeloTablaTareas modelo = new ModeloTablaTareas(10);
        assertEquals(-1, modelo.buscarIndicePorId(null));
        assertDoesNotThrow(() -> modelo.eliminarTareaPorId(null));
    }

    @Test
    @DisplayName("Contadores y limpieza por estado toleran parametros null")
    void testOperacionesEstadoNull() throws Exception {
        ModeloTablaTareas modelo = new ModeloTablaTareas(10);
        modelo.agregarTarea(new Tarea("1", "A", "https://example.com/1", Tarea.ESTADO_EN_COLA));
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(0, modelo.contarPorEstado(null));
        assertDoesNotThrow(() -> modelo.eliminarPorEstado((String[]) null));
        assertDoesNotThrow(() -> modelo.eliminarPorEstado());
        assertDoesNotThrow(() -> modelo.eliminarPorEstado((String) null));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());
    }

    @Test
    @DisplayName("Limite visual elimina primero finalizadas y preserva activas")
    void testLimiteVisualPriorizaFinalizadas() throws Exception {
        ModeloTablaTareas modelo = new ModeloTablaTareas(2);
        Tarea activa = new Tarea("activa", "A", "https://example.com/activa", Tarea.ESTADO_EN_COLA);
        Tarea finalizadaAntigua = new Tarea("old", "B", "https://example.com/old", Tarea.ESTADO_COMPLETADO);
        Tarea finalizadaNueva = new Tarea("new", "C", "https://example.com/new", Tarea.ESTADO_ERROR);

        modelo.agregarTarea(activa);
        modelo.agregarTarea(finalizadaAntigua);
        modelo.agregarTarea(finalizadaNueva);
        SwingUtilities.invokeAndWait(() -> {});

        Set<String> idsRestantes = new HashSet<>();
        for (Tarea tarea : modelo.obtenerTodasLasTareas()) {
            idsRestantes.add(tarea.obtenerId());
        }

        assertEquals(2, modelo.obtenerNumeroTareas());
        assertTrue(idsRestantes.contains("activa"));
        assertTrue(idsRestantes.contains("new"));
    }
}
