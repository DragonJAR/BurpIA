package com.burpia.util;

import java.util.concurrent.Semaphore;

public class LimitadorTasa {
    private final Semaphore semaforo;

    public LimitadorTasa(int maximoConcurrente) {
        int maximoNormalizado = Math.max(1, maximoConcurrente);
        this.semaforo = new Semaphore(maximoNormalizado);
    }

    public void adquirir() throws InterruptedException {
        semaforo.acquire();
    }

    public void liberar() {
        semaforo.release();
    }

    public int permisosDisponibles() {
        return semaforo.availablePermits();
    }
}
