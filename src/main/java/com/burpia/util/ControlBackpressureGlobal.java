package com.burpia.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Control de backpressure global basado en cooldown.
 * 
 * <p>Permite activar periodos de cooldown que bloquean nuevas operaciones
 * hasta que expiren. Los cooldowns se extienden automáticamente si se
 * activa uno nuevo mientras ya existe uno activo (se mantiene el mayor).
 * 
 * <p>Thread-safe: usa AtomicLong para garantizar consistencia en entornos
 * concurrentes.
 */
public class ControlBackpressureGlobal {
    
    /** Valor que indica sin cooldown activo */
    private static final long SIN_COOLDOWN = 0L;
    
    /** Timestamp de expiración del cooldown en milisegundos */
    private final AtomicLong cooldownHastaMs = new AtomicLong(SIN_COOLDOWN);

    /**
     * Activa un cooldown por la duración especificada.
     * 
     * <p>Si ya existe un cooldown activo más largo, se mantiene el mayor.
     * Si el nuevo cooldown es más largo, se extiende.
     * 
     * @param milisegundos duración del cooldown en milisegundos; 
     *                     valores &lt;= 0 son ignorados
     */
    public void activarCooldown(long milisegundos) {
        if (milisegundos <= SIN_COOLDOWN) {
            return;
        }
        
        long ahora = System.currentTimeMillis();
        long nuevoHasta;
        
        // Prevenir overflow aritmético
        if (Long.MAX_VALUE - ahora < milisegundos) {
            nuevoHasta = Long.MAX_VALUE;
        } else {
            nuevoHasta = ahora + milisegundos;
        }
        
        cooldownHastaMs.updateAndGet(actual -> Math.max(actual, nuevoHasta));
    }

    /**
     * Verifica si hay un cooldown activo.
     * 
     * @return true si el cooldown está activo, false en caso contrario
     */
    public boolean estaEnCooldown() {
        return milisegundosRestantes() > SIN_COOLDOWN;
    }

    /**
     * Obtiene los milisegundos restantes del cooldown actual.
     * 
     * @return milisegundos restantes, o 0 si no hay cooldown activo
     */
    public long milisegundosRestantes() {
        long restante = cooldownHastaMs.get() - System.currentTimeMillis();
        return Math.max(SIN_COOLDOWN, restante);
    }

    /**
     * Elimina el cooldown actual, permitiendo operaciones inmediatas.
     */
    public void limpiar() {
        cooldownHastaMs.set(SIN_COOLDOWN);
    }
    
    /**
     * Obtiene el timestamp de expiración del cooldown.
     * 
     * <p>Útil para debugging y monitoreo.
     * 
     * @return timestamp en milisegundos desde epoch, o 0 si no hay cooldown
     */
    public long obtenerExpiracionCooldown() {
        return cooldownHastaMs.get();
    }
    
    @Override
    public String toString() {
        long restantes = milisegundosRestantes();
        if (restantes <= SIN_COOLDOWN) {
            return "ControlBackpressureGlobal[sin cooldown activo]";
        }
        return String.format("ControlBackpressureGlobal[cooldown: %d ms restantes]", restantes);
    }
}
