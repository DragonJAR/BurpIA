package com.burpia.util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;




@DisplayName("ControlBackpressureGlobal Tests")
class ControlBackpressureGlobalTest {

    @Test
    @DisplayName("Activar cooldown marca estado activo")
    void activarCooldown_marcaEstadoActivo() {
        ControlBackpressureGlobal control = new ControlBackpressureGlobal();
        control.activarCooldown(200L);
        assertTrue(control.estaEnCooldown());
        assertTrue(control.milisegundosRestantes() > 0L);
    }

    @Test
    @DisplayName("Cooldown expira despues del tiempo configurado")
    void cooldown_expira() throws Exception {
        ControlBackpressureGlobal control = new ControlBackpressureGlobal();
        control.activarCooldown(80L);
        Thread.sleep(150L);
        assertFalse(control.estaEnCooldown());
    }

    @Test
    @DisplayName("Limpiar cooldown resetea estado")
    void limpiarCooldown_reseteaEstado() {
        ControlBackpressureGlobal control = new ControlBackpressureGlobal();
        control.activarCooldown(5000L);
        control.limpiar();
        assertFalse(control.estaEnCooldown());
        assertFalse(control.milisegundosRestantes() > 0L);
    }
}
