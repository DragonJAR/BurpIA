package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("LimitadorTasa Tests")
class LimitadorTasaTest {

    @Test
    @DisplayName("Normaliza concurrencia invalida a un permiso")
    void testNormalizaConcurrenciaInvalida() {
        LimitadorTasa cero = new LimitadorTasa(0);
        LimitadorTasa negativo = new LimitadorTasa(-5);

        assertEquals(1, cero.permisosDisponibles());
        assertEquals(1, negativo.permisosDisponibles());
    }

    @Test
    @DisplayName("Mantiene concurrencia valida configurada")
    void testMantieneConcurrenciaValida() {
        LimitadorTasa limitador = new LimitadorTasa(3);
        assertEquals(3, limitador.permisosDisponibles());
    }
}
