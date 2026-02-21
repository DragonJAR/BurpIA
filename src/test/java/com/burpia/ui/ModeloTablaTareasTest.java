package com.burpia.ui;

import com.burpia.model.Tarea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
