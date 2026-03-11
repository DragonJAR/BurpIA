package com.burpia.util;

import com.burpia.model.Tarea;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Utilidad centralizada para contar tareas por estado.
 * <p>
 * Implementa el principio DRY centralizando la lógica de conteo de estados
 * que se usa en múltiples lugares del sistema de tareas.
 * </p>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">AGENTS.md - Principio DRY</a>
 */
public final class ContadorEstadosTareas {

    private final int enCola;
    private final int analizando;
    private final int pausadas;
    private final int completadas;
    private final int errores;
    private final int canceladas;

    /**
     * Constructor para uso interno y testing.
     * <p>
     * Package-private para permitir instanciación en tests unitarios.
     * El uso preferido es mediante los métodos estáticos {@link #contar(Collection)}
     * y {@link #vacio()}.
     * </p>
     *
     * @param enCola cantidad de tareas en cola
     * @param analizando cantidad de tareas analizándose
     * @param pausadas cantidad de tareas pausadas
     * @param completadas cantidad de tareas completadas
     * @param errores cantidad de tareas con error
     * @param canceladas cantidad de tareas canceladas
     */
    ContadorEstadosTareas(int enCola, int analizando, int pausadas,
                          int completadas, int errores, int canceladas) {
        this.enCola = enCola;
        this.analizando = analizando;
        this.pausadas = pausadas;
        this.completadas = completadas;
        this.errores = errores;
        this.canceladas = canceladas;
    }

    /**
     * Cuenta los estados de una colección de tareas.
     *
     * @param tareas colección de tareas a contar
     * @return ContadorEstadosTareas con los conteos por estado
     */
    public static ContadorEstadosTareas contar(Collection<Tarea> tareas) {
        if (Normalizador.esVacia(tareas)) {
            return vacio();
        }

        int enCola = 0;
        int analizando = 0;
        int pausadas = 0;
        int completadas = 0;
        int errores = 0;
        int canceladas = 0;

        for (Tarea tarea : tareas) {
            if (tarea == null) {
                continue;
            }
            String estado = tarea.obtenerEstado();
            if (Tarea.ESTADO_EN_COLA.equals(estado)) {
                enCola++;
            } else if (Tarea.ESTADO_ANALIZANDO.equals(estado)) {
                analizando++;
            } else if (Tarea.ESTADO_PAUSADO.equals(estado)) {
                pausadas++;
            } else if (Tarea.ESTADO_COMPLETADO.equals(estado)) {
                completadas++;
            } else if (Tarea.ESTADO_ERROR.equals(estado)) {
                errores++;
            } else if (Tarea.ESTADO_CANCELADO.equals(estado)) {
                canceladas++;
            }
        }

        return new ContadorEstadosTareas(enCola, analizando, pausadas,
                                         completadas, errores, canceladas);
    }

    /**
     * Crea un contador vacío con todos los valores en cero.
     *
     * @return ContadorEstadosTareas con todos los conteos en cero
     */
    public static ContadorEstadosTareas vacio() {
        return new ContadorEstadosTareas(0, 0, 0, 0, 0, 0);
    }

    /**
     * Cuenta cuántos estados cumplen con un predicado dado.
     *
     * @param tareas colección de tareas
     * @param predicado condición a evaluar sobre el estado
     * @return cantidad de tareas que cumplen la condición
     */
    public static int contarPorPredicado(Collection<Tarea> tareas, Predicate<String> predicado) {
        if (Normalizador.esVacia(tareas) || predicado == null) {
            return 0;
        }
        int contador = 0;
        for (Tarea tarea : tareas) {
            if (tarea != null && predicado.test(tarea.obtenerEstado())) {
                contador++;
            }
        }
        return contador;
    }

    // Getters

    public int getEnCola() {
        return enCola;
    }

    public int getAnalizando() {
        return analizando;
    }

    public int getPausadas() {
        return pausadas;
    }

    public int getCompletadas() {
        return completadas;
    }

    public int getErrores() {
        return errores;
    }

    public int getCanceladas() {
        return canceladas;
    }

    /**
     * Total de tareas activas sin pausadas.
     *
     * @return enCola + analizando
     */
    public int getActivasSinPausadas() {
        return enCola + analizando;
    }

    /**
     * Total de tareas activas incluyendo pausadas.
     *
     * @return enCola + analizando + pausadas
     */
    public int getActivas() {
        return enCola + analizando + pausadas;
    }

    /**
     * Total de tareas finalizadas (completadas + errores + canceladas).
     *
     * @return completadas + errores + canceladas
     */
    public int getFinalizadas() {
        return completadas + errores + canceladas;
    }

    /**
     * Total de tareas con error o canceladas (reintentables).
     *
     * @return errores + canceladas
     */
    public int getErroresYCanceladas() {
        return errores + canceladas;
    }

    /**
     * Total de tareas en la colección.
     *
     * @return suma de todos los estados
     */
    public int getTotal() {
        return enCola + analizando + pausadas + completadas + errores + canceladas;
    }
}