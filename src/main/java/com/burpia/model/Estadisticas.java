package com.burpia.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Estadisticas {
    private final AtomicInteger totalSolicitudes;
    private final AtomicInteger analizados;
    private final AtomicInteger omitidosDuplicado;
    private final AtomicInteger omitidosRateLimit;
    private final AtomicInteger omitidosBajaConfianza;
    private final AtomicInteger hallazgosCreados;
    private final AtomicInteger errores;

    // Contadores de severidad de hallazgos
    private final AtomicInteger hallazgosCritical;
    private final AtomicInteger hallazgosHigh;
    private final AtomicInteger hallazgosMedium;
    private final AtomicInteger hallazgosLow;
    private final AtomicInteger hallazgosInfo;

    private final ReentrantLock candado;

    public Estadisticas() {
        this.totalSolicitudes = new AtomicInteger(0);
        this.analizados = new AtomicInteger(0);
        this.omitidosDuplicado = new AtomicInteger(0);
        this.omitidosRateLimit = new AtomicInteger(0);
        this.omitidosBajaConfianza = new AtomicInteger(0);
        this.hallazgosCreados = new AtomicInteger(0);
        this.errores = new AtomicInteger(0);

        this.hallazgosCritical = new AtomicInteger(0);
        this.hallazgosHigh = new AtomicInteger(0);
        this.hallazgosMedium = new AtomicInteger(0);
        this.hallazgosLow = new AtomicInteger(0);
        this.hallazgosInfo = new AtomicInteger(0);

        this.candado = new ReentrantLock();
    }

    // Metodos de incremento
    public void incrementarTotalSolicitudes() { totalSolicitudes.incrementAndGet(); }
    public void incrementarAnalizados() { analizados.incrementAndGet(); }
    public void incrementarOmitidosDuplicado() { omitidosDuplicado.incrementAndGet(); }
    public void incrementarOmitidosRateLimit() { omitidosRateLimit.incrementAndGet(); }
    public void incrementarOmitidosBajaConfianza() { omitidosBajaConfianza.incrementAndGet(); }
    public void incrementarHallazgosCreados() { hallazgosCreados.incrementAndGet(); }
    public void incrementarErrores() { errores.incrementAndGet(); }

    public void incrementarHallazgoSeveridad(String severidad) {
        switch (severidad) {
            case Hallazgo.SEVERIDAD_CRITICAL:
                hallazgosCritical.incrementAndGet();
                break;
            case Hallazgo.SEVERIDAD_HIGH:
                hallazgosHigh.incrementAndGet();
                break;
            case Hallazgo.SEVERIDAD_MEDIUM:
                hallazgosMedium.incrementAndGet();
                break;
            case Hallazgo.SEVERIDAD_LOW:
                hallazgosLow.incrementAndGet();
                break;
            case Hallazgo.SEVERIDAD_INFO:
                hallazgosInfo.incrementAndGet();
                break;
        }
        hallazgosCreados.incrementAndGet();
    }

    // Getters
    public int obtenerTotalSolicitudes() { return totalSolicitudes.get(); }
    public int obtenerAnalizados() { return analizados.get(); }
    public int obtenerOmitidosDuplicado() { return omitidosDuplicado.get(); }
    public int obtenerOmitidosRateLimit() { return omitidosRateLimit.get(); }
    public int obtenerOmitidosBajaConfianza() { return omitidosBajaConfianza.get(); }
    public int obtenerHallazgosCreados() { return hallazgosCreados.get(); }
    public int obtenerErrores() { return errores.get(); }

    public int obtenerTotalOmitidos() {
        return omitidosDuplicado.get() +
               omitidosRateLimit.get() +
               omitidosBajaConfianza.get();
    }

    public int obtenerHallazgosCritical() { return hallazgosCritical.get(); }
    public int obtenerHallazgosHigh() { return hallazgosHigh.get(); }
    public int obtenerHallazgosMedium() { return hallazgosMedium.get(); }
    public int obtenerHallazgosLow() { return hallazgosLow.get(); }
    public int obtenerHallazgosInfo() { return hallazgosInfo.get(); }

    public void reiniciar() {
        candado.lock();
        try {
            totalSolicitudes.set(0);
            analizados.set(0);
            omitidosDuplicado.set(0);
            omitidosRateLimit.set(0);
            omitidosBajaConfianza.set(0);
            hallazgosCreados.set(0);
            errores.set(0);

            hallazgosCritical.set(0);
            hallazgosHigh.set(0);
            hallazgosMedium.set(0);
            hallazgosLow.set(0);
            hallazgosInfo.set(0);
        } finally {
            candado.unlock();
        }
    }

    public String generarResumen() {
        return String.format(
            "Solicitudes: %d | Analizados: %d | Omitidos: %d | Hallazgos: %d | Errores: %d",
            totalSolicitudes.get(),
            analizados.get(),
            obtenerTotalOmitidos(),
            hallazgosCreados.get(),
            errores.get()
        );
    }
}
