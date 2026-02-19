package com.burpia.util;

import java.util.concurrent.Semaphore;

public class LimitadorTasa {
    private final Semaphore semaforo;

    public LimitadorTasa(int maximoConcurrente) {
        this.semaforo = new Semaphore(maximoConcurrente);
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
