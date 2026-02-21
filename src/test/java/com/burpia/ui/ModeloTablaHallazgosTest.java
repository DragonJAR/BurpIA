package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    @DisplayName("Ignorar y eliminar con indice invalido no rompe la tabla")
    void testOperacionesIndiceInvalido() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "Hallazgo X", "Low", "Low"));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());

        assertDoesNotThrow(() -> modelo.marcarComoIgnorado(-1));
        assertDoesNotThrow(() -> modelo.eliminarHallazgo(-1));
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, modelo.getRowCount());
        assertEquals(0, modelo.obtenerNumeroIgnorados());
    }

    @Test
    @DisplayName("Actualizar con hallazgo null no altera datos")
    void testActualizarHallazgoNullNoRompe() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "Hallazgo X", "Low", "Low"));
        SwingUtilities.invokeAndWait(() -> {});

        assertDoesNotThrow(() -> modelo.actualizarHallazgo(0, null));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());
    }
}
