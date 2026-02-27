package com.burpia.model;
import java.util.concurrent.atomic.AtomicInteger;

public class Estadisticas {
    private final AtomicInteger totalSolicitudes;
    private final AtomicInteger analizados;
    private final AtomicInteger omitidosDuplicado;
    private final AtomicInteger omitidosRateLimit;
    private final AtomicInteger omitidosBajaConfianza;
    private final AtomicInteger hallazgosCreados;
    private final AtomicInteger errores;

    private final AtomicInteger hallazgosCritical;
    private final AtomicInteger hallazgosHigh;
    private final AtomicInteger hallazgosMedium;
    private final AtomicInteger hallazgosLow;
    private final AtomicInteger hallazgosInfo;
    private final AtomicInteger versionCambios;

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
        this.versionCambios = new AtomicInteger(0);

    }

    public void incrementarTotalSolicitudes() { totalSolicitudes.incrementAndGet(); versionCambios.incrementAndGet(); }
    public void incrementarAnalizados() { analizados.incrementAndGet(); versionCambios.incrementAndGet(); }
    public void incrementarOmitidosDuplicado() { omitidosDuplicado.incrementAndGet(); versionCambios.incrementAndGet(); }
    public void incrementarOmitidosBajaConfianza() { omitidosBajaConfianza.incrementAndGet(); versionCambios.incrementAndGet(); }
    public void incrementarErrores() { errores.incrementAndGet(); versionCambios.incrementAndGet(); }
    public int obtenerVersion() { return versionCambios.get(); }

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
        versionCambios.incrementAndGet();
    }

    public int obtenerTotalSolicitudes() { return totalSolicitudes.get(); }
    public int obtenerAnalizados() { return analizados.get(); }
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
