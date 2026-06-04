package com.burpia.config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Contenedor thread-safe para la configuración compartida de BurpIA.
 * <p>
 * Resuelve el race condition entre el hilo de Burp (que lee la configuración
 * al procesar cada request) y el EDT (que la modifica al guardar cambios).
 * <p>
 * En lugar de mutar una instancia de {@link ConfiguracionAPI} in-place con
 * {@code aplicarDesde()}, se reemplaza la referencia completa de forma atómica.
 * Cada hilo obtiene un snapshot consistente llamando {@link #obtener()}, sin
 * necesidad de sincronización adicional.
 * <p>
 * <b>Patrón de uso:</b>
 * <pre>{@code
 * // Escritura (desde EDT):
 * ConfiguracionAPI nueva = configRef.obtener().crearSnapshot();
 * nueva.establecerMaximoConcurrente(5);
 * configRef.reemplazar(nueva);
 *
 * // Lectura (desde cualquier hilo):
 * ConfiguracionAPI snapshot = configRef.obtener();
 * if (snapshot.esDetallado()) { ... }
 * }</pre>
 *
 * @see ConfiguracionAPI
 * @see ConfiguracionAPI#crearSnapshot()
 */
public final class ConfiguracionAPIRef {

    private final AtomicReference<ConfiguracionAPI> ref;

    /**
     * Crea una referencia con la configuración inicial.
     *
     * @param initial configuración inicial; si es null, se usa una configuración por defecto
     */
    public ConfiguracionAPIRef(ConfiguracionAPI initial) {
        this.ref = new AtomicReference<>(initial != null ? initial : new ConfiguracionAPI());
    }

    /**
     * Obtiene el snapshot actual de la configuración.
     * <p>
     * La referencia devuelta es inmutable en práctica: el llamador puede leerla
     * de forma segura sin preocuparse por cambios concurrentes, ya que cualquier
     * actualización posterior reemplazará la referencia atómicamente.
     *
     * @return la configuración actual, nunca null
     */
    public ConfiguracionAPI obtener() {
        return ref.get();
    }

    /**
     * Reemplaza la configuración actual de forma atómica.
     * <p>
     * Garantiza happens-before: todos los reads subsiguientes a {@link #obtener()}
     * verán la nueva configuración.
     *
     * @param nueva la nueva configuración; si es null, la llamada se ignora
     */
    public void reemplazar(ConfiguracionAPI nueva) {
        if (nueva != null) {
            ref.set(nueva);
        }
    }

    // compararYReemplazar(esperada, nueva) — CAS removed (orphan): no había
    // callers en producción, solo en tests que verificaban el método mismo.
    // El flujo real de actualización de config usa establecer(nueva) sin
    // semántica optimista. Si en el futuro se necesita compare-and-set para
    // resolver conflictos concurrentes, se vuelve a introducir con su caller.
}