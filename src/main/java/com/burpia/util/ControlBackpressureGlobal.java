package com.burpia.util;

import java.util.concurrent.atomic.AtomicLong;

public class ControlBackpressureGlobal {
    private final AtomicLong cooldownHastaMs = new AtomicLong(0L);

    public void activarCooldown(long milisegundos) {
        if (milisegundos <= 0L) {
            return;
        }
        long nuevoHasta = System.currentTimeMillis() + milisegundos;
        cooldownHastaMs.updateAndGet(actual -> Math.max(actual, nuevoHasta));
    }

    public boolean estaEnCooldown() {
        return milisegundosRestantes() > 0L;
    }

    public long milisegundosRestantes() {
        long restante = cooldownHastaMs.get() - System.currentTimeMillis();
        return Math.max(0L, restante);
    }

    public void limpiar() {
        cooldownHastaMs.set(0L);
    }
}
