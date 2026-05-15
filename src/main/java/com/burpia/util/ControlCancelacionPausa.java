package com.burpia.util;

import java.util.function.BooleanSupplier;

/**
 * Utilidad compartida para control de cancelación y pausa de tareas de análisis.
 * <p>
 * Centraliza la lógica de verificación de cancelación, espera durante pausa,
 * y espera con control de interrupción, eliminando duplicación entre
 * {@code AnalizadorAI} y {@code OrquestadorAnalisis}.
 * <p>
 * Thread-safe: los BooleanSupplier se invocan desde el hilo de la tarea.
 *
 * @see com.burpia.analyzer.AnalizadorAI
 * @see com.burpia.analyzer.OrquestadorAnalisis
 */
public final class ControlCancelacionPausa {

    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;

    public ControlCancelacionPausa(BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
    }

    /**
     * Verifica si la tarea fue cancelada.
     *
     * @return true si la tarea fue cancelada
     */
    public boolean esCancelada() {
        return tareaCancelada.getAsBoolean();
    }

    /**
     * Verifica si la tarea está pausada.
     *
     * @return true si la tarea está pausada
     */
    public boolean esPausada() {
        return tareaPausada.getAsBoolean();
    }

    /**
     * Lanza InterruptedException si la tarea fue cancelada.
     *
     * @throws InterruptedException si la tarea fue cancelada
     */
    public void verificarCancelacion() throws InterruptedException {
        if (tareaCancelada.getAsBoolean()) {
            throw new InterruptedException("Tarea cancelada");
        }
    }

    /**
     * Espera activamente mientras la tarea esté pausada, con verificación de cancelación.
     * <p>
     * Usa un polling interval de 250ms para balance entre responsividad y uso de CPU.
     *
     * @throws InterruptedException si la tarea fue cancelada mientras estaba pausada
     */
    public void esperarSiPausada() throws InterruptedException {
        while (tareaPausada.getAsBoolean() && !tareaCancelada.getAsBoolean()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    /**
     * Espera un tiempo determinado con verificaciones periódicas de cancelación y pausa.
     * <p>
     * El tiempo se divide en intervalos de 250ms, verificando estado entre cada uno.
     *
     * @param milisegundos tiempo total a esperar en milisegundos
     * @throws InterruptedException si la tarea fue cancelada durante la espera
     */
    public void esperarConControl(long milisegundos) throws InterruptedException {
        long restante = milisegundos;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
    }
}