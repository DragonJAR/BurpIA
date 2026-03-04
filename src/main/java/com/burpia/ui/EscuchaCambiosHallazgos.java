package com.burpia.ui;

/**
 * Interfaz funcional para escuchar cambios en la tabla de hallazgos.
 *
 * <p>Las implementaciones de esta interfaz pueden registrarse en
 * {@link ModeloTablaHallazgos} para recibir notificaciones cuando
 * los datos de la tabla cambían.</p>
 *
 * @see ModeloTablaHallazgos#agregarEscucha(EscuchaCambiosHallazgos)
 * @see ModeloTablaHallazgos#eliminarEscucha(EscuchaCambiosHallazgos)
 */
@FunctionalInterface
public interface EscuchaCambiosHallazgos {
    /**
     * Notifica que los hallazgos en la tabla han cambiado.
     *
     * <p>Este método se invoca en el Event Dispatch Thread (EDT)
     * para garantizar la seguridad de la actualización de componentes UI.</p>
     */
    void enHallazgosCambiados();
}
