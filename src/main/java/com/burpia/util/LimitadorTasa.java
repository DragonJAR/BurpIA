package com.burpia.util;

import java.util.concurrent.Semaphore;

/**
 * Limitador de tasa basado en semáforos para controlar la concurrencia máxima
 * de operaciones paralelas.
 * <p>
 * Esta clase proporciona un mecanismo simple y eficiente para limitar el número
 * de operaciones concurrentes, utilizando un {@link Semaphore} subyacente.
 * </p>
 *
 * <h2>Características:</h2>
 * <ul>
 *   <li>Thread-safe: Puede ser usado de forma segura desde múltiples hilos</li>
 *   <li>Normalización automática: Valores inválidos (≤0) se normalizan a {@value #MINIMO_CONCURRENTE}</li>
 *   <li>Operaciones bloqueantes: {@link #adquirir()} bloquea hasta que haya permiso disponible</li>
 *   <li>Liberación manual: Es responsabilidad del llamador liberar permisos con {@link #liberar()}</li>
 * </ul>
 *
 * <h2>Ejemplo de uso:</h2>
 * <pre>{@code
 * // Crear limitador con máximo 5 operaciones concurrentes
 * LimitadorTasa limitador = new LimitadorTasa(5);
 *
 * // Antes de ejecutar operación
 * limitador.adquirir();
 * try {
 *     // Realizar operación
 * } finally {
 *     // SIEMPRE liberar el permiso
 *     limitador.liberar();
 * }
 * }</pre>
 *
 * <h2>Patrón recomendado:</h2>
 * <p>
 * Siempre usar en bloques try-finally para garantizar la liberación de permisos:
 * </p>
 * <pre>{@code
 * limitador.adquirir();
 * try {
 *     // Operación protegida
 * } finally {
 *     limitador.liberar();
 * }
 * }</pre>
 *
 * @see Semaphore
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public class LimitadorTasa {

    /**
     * Valor mínimo de concurrencia permitido.
     * Valores menores o iguales a cero se normalizan a este valor.
     */
    private static final int MINIMO_CONCURRENTE = 1;

    /**
     * Semáforo subyacente que controla los permisos de concurrencia.
     */
    private final Semaphore semaforo;

    /**
     * Crea un nuevo limitador de tasa con el máximo de operaciones concurrentes especificado.
     * <p>
     * Si el valor proporcionado es menor o igual a cero, se normaliza automáticamente
     * a {@value #MINIMO_CONCURRENTE}.
     * </p>
     *
     * @param maximoConcurrente el número máximo de operaciones concurrentes permitidas.
     *                          Si es ≤ 0, se normaliza a {@value #MINIMO_CONCURRENTE}
     */
    public LimitadorTasa(int maximoConcurrente) {
        int maximoNormalizado = Math.max(MINIMO_CONCURRENTE, maximoConcurrente);
        this.semaforo = new Semaphore(maximoNormalizado);
    }

    /**
     * Adquiere un permiso del limitador, bloqueando hasta que uno esté disponible.
     * <p>
     * Si no hay permisos disponibles, el hilo actual se bloquea hasta que otro hilo
     * libere un permiso mediante {@link #liberar()}.
     * </p>
     *
     * @throws InterruptedException si el hilo es interrumpido mientras espera
     * @see #liberar()
     */
    public void adquirir() throws InterruptedException {
        semaforo.acquire();
    }

    /**
     * Libera un permiso, permitiendo que otro hilo adquiera el recurso.
     * <p>
     * <strong>Importante:</strong> Este método debe llamarse en un bloque finally
     * para garantizar la liberación incluso si ocurre una excepción.
     * </p>
     *
     * @see #adquirir()
     */
    public void liberar() {
        semaforo.release();
    }

    /**
     * Retorna el número de permisos disponibles actualmente.
     * <p>
     * Este valor es aproximado y puede cambiar inmediatamente después de ser consultado
     * en entornos multi-hilo.
     * </p>
     *
     * @return el número de permisos disponibles, puede ser negativo si hay hilos esperando
     */
    public int permisosDisponibles() {
        return semaforo.availablePermits();
    }
}
