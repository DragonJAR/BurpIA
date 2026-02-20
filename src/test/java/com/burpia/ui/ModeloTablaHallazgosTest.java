package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ModeloTablaHallazgos Tests")
class ModeloTablaHallazgosTest {

    @Test
    @DisplayName("Permite actualizar limite de filas en runtime")
    void testActualizarLimiteFilas() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

        for (int i = 0; i < 6; i++) {
            modelo.agregarHallazgo(new Hallazgo(
                "https://example.com/" + i,
                "Hallazgo " + i,
                "Low",
                "Low"
            ));
        }
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(5, modelo.getRowCount());

        modelo.establecerLimiteFilas(3);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(3, modelo.getRowCount());
        assertEquals(3, modelo.obtenerLimiteFilas());
    }
}
