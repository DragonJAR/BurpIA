package com.burpia.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estadísticas thread-safe para el seguimiento de métricas de análisis de BurpIA.
 * <p>
 * Esta clase utiliza {@link AtomicInteger} para garantizar operaciones atómicas
 * y thread-safe en entornos concurrentes.
 * </p>
 * <p>
 * Implementa un sistema de versionado para detectar cambios y sincronizar
 * actualizaciones de UI de manera eficiente.
 * </p>
 *
 * @see Hallazgo
 */
public class Estadisticas {

    private final AtomicInteger totalSolicitudes;
    private final AtomicInteger analizados;
    private final AtomicInteger omitidosDuplicado;
    private final AtomicInteger omitidosBajaConfianza;
    private final AtomicInteger hallazgosCreados;
    private final AtomicInteger errores;

    private final AtomicInteger hallazgosCritical;
    private final AtomicInteger hallazgosHigh;
    private final AtomicInteger hallazgosMedium;
    private final AtomicInteger hallazgosLow;
    private final AtomicInteger hallazgosInfo;
    private final AtomicInteger hallazgosDesconocidos;
    private final AtomicInteger versionCambios;

    /**
     * Crea una nueva instancia de estadísticas con todos los contadores en cero.
     */
    public Estadisticas() {
        this.totalSolicitudes = new AtomicInteger(0);
        this.analizados = new AtomicInteger(0);
        this.omitidosDuplicado = new AtomicInteger(0);
        this.omitidosBajaConfianza = new AtomicInteger(0);
        this.hallazgosCreados = new AtomicInteger(0);
        this.errores = new AtomicInteger(0);

        this.hallazgosCritical = new AtomicInteger(0);
        this.hallazgosHigh = new AtomicInteger(0);
        this.hallazgosMedium = new AtomicInteger(0);
        this.hallazgosLow = new AtomicInteger(0);
        this.hallazgosInfo = new AtomicInteger(0);
        this.hallazgosDesconocidos = new AtomicInteger(0);
        this.versionCambios = new AtomicInteger(0);
    }

    /**
     * Incrementa el contador de solicitudes totales procesadas.
     */
    public void incrementarTotalSolicitudes() {
        totalSolicitudes.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Incrementa el contador de solicitudes analizadas exitosamente.
     */
    public void incrementarAnalizados() {
        analizados.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Incrementa el contador de hallazgos omitidos por ser duplicados.
     */
    public void incrementarOmitidosDuplicado() {
        omitidosDuplicado.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Incrementa el contador de hallazgos omitidos por baja confianza.
     */
    public void incrementarOmitidosBajaConfianza() {
        omitidosBajaConfianza.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Incrementa el contador de errores de análisis.
     */
    public void incrementarErrores() {
        errores.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Obtiene la versión actual de cambios para detectar modificaciones.
     *
     * @return número de versión incremental
     */
    public int obtenerVersion() {
        return versionCambios.get();
    }

    /**
     * Incrementa el contador de hallazgos según su severidad.
     * <p>
     * Si la severidad no es reconocida (no es Critical, High, Medium, Low o Info),
     * se incrementa el contador de hallazgos desconocidos.
     * </p>
     *
     * @param severidad la severidad del hallazgo (debe ser una de las constantes de {@link Hallazgo})
     */
    public void incrementarHallazgoSeveridad(String severidad) {
        if (Hallazgo.esSeveridadValida(severidad)) {
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
                default:
                    hallazgosDesconocidos.incrementAndGet();
                    break;
            }
        } else {
            hallazgosDesconocidos.incrementAndGet();
        }
        hallazgosCreados.incrementAndGet();
        versionCambios.incrementAndGet();
    }

    /**
     * Obtiene el total de solicitudes procesadas.
     *
     * @return número de solicitudes totales
     */
    public int obtenerTotalSolicitudes() {
        return totalSolicitudes.get();
    }

    /**
     * Obtiene el total de solicitudes analizadas exitosamente.
     *
     * @return número de solicitudes analizadas
     */
    public int obtenerAnalizados() {
        return analizados.get();
    }

    /**
     * Obtiene el total de hallazgos creados.
     *
     * @return número de hallazgos creados (incluye todos los niveles de severidad)
     */
    public int obtenerHallazgosCreados() {
        return hallazgosCreados.get();
    }

    /**
     * Obtiene el total de errores de análisis.
     *
     * @return número de errores
     */
    public int obtenerErrores() {
        return errores.get();
    }

    /**
     * Obtiene el total de hallazgos omitidos (duplicados + baja confianza).
     *
     * @return número total de hallazgos omitidos
     */
    public int obtenerTotalOmitidos() {
        return omitidosDuplicado.get() + omitidosBajaConfianza.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad Critical.
     *
     * @return número de hallazgos Critical
     */
    public int obtenerHallazgosCritical() {
        return hallazgosCritical.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad High.
     *
     * @return número de hallazgos High
     */
    public int obtenerHallazgosHigh() {
        return hallazgosHigh.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad Medium.
     *
     * @return número de hallazgos Medium
     */
    public int obtenerHallazgosMedium() {
        return hallazgosMedium.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad Low.
     *
     * @return número de hallazgos Low
     */
    public int obtenerHallazgosLow() {
        return hallazgosLow.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad Info.
     *
     * @return número de hallazgos Info
     */
    public int obtenerHallazgosInfo() {
        return hallazgosInfo.get();
    }

    /**
     * Obtiene el número de hallazgos con severidad desconocida o no reconocida.
     *
     * @return número de hallazgos con severidad desconocida
     */
    public int obtenerHallazgosDesconocidos() {
        return hallazgosDesconocidos.get();
    }

    /**
     * Genera un resumen textual de las estadísticas actuales.
     * <p>
     * Formato: "Solicitudes: N | Analizados: N | Omitidos: N | Hallazgos: N | Errores: N"
     * </p>
     *
     * @return cadena con el resumen de estadísticas
     */
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

    /**
     * Reinicia todos los contadores de estadísticas a cero.
     * <p>
     * Este método es thread-safe y puede ser llamado en cualquier momento.
     * </p>
     */
    public void reiniciar() {
        totalSolicitudes.set(0);
        analizados.set(0);
        omitidosDuplicado.set(0);
        omitidosBajaConfianza.set(0);
        hallazgosCreados.set(0);
        errores.set(0);
        hallazgosCritical.set(0);
        hallazgosHigh.set(0);
        hallazgosMedium.set(0);
        hallazgosLow.set(0);
        hallazgosInfo.set(0);
        hallazgosDesconocidos.set(0);
        versionCambios.incrementAndGet();
    }
}
