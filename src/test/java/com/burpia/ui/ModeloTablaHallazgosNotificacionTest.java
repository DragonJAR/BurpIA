package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de notificación de cambios en ModeloTablaHallazgos.
 *
 * Estos tests verifican que el modelo notifica a sus suscriptores cuando:
 * - Se agrega un hallazgo
 * - Se eliminan hallazgos
 * - Se limpia la tabla
 * - Se actualiza un hallazgo
 */
@DisplayName("ModeloTablaHallazgos Notificación Tests")
class ModeloTablaHallazgosNotificacionTest {

    private ModeloTablaHallazgos modelo;

    @BeforeEach
    void setUp() {
        modelo = new ModeloTablaHallazgos(1000);
    }

    @Test
    @DisplayName("Agregar escucha recibe notificación al agregar hallazgo")
    void agregarEscuchaRecibeNotificacionAlAgregarHallazgo() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        Hallazgo hallazgo = crearHallazgoPrueba();
        modelo.agregarHallazgo(hallazgo);

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(), "El escucha debió ser notificado una vez");
    }

    @Test
    @DisplayName("Agregar escucha recibe notificación al limpiar tabla")
    void agregarEscuchaRecibeNotificacionAlLimpiarTabla() throws Exception {
        // Agregar primero los hallazgos para que las notificaciones de agregar se procesen
        modelo.agregarHallazgo(crearHallazgoPrueba());
        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {}); // Esperar notificaciones de agregar

        // Ahora agregar el escucha y limpiar
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        modelo.limpiar();

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(), "El escucha debió ser notificado al limpiar");
    }

    @Test
    @DisplayName("Agregar escucha recibe notificación al eliminar hallazgo")
    void agregarEscuchaRecibeNotificacionAlEliminarHallazgo() throws Exception {
        modelo.agregarHallazgo(crearHallazgoPrueba());
        modelo.agregarHallazgo(crearHallazgoPrueba());

        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        modelo.eliminarHallazgo(0);

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(), "El escucha debió ser notificado al eliminar");
    }

    @Test
    @DisplayName("Agregar escucha recibe notificación al actualizar hallazgo")
    void agregarEscuchaRecibeNotificacionAlActualizarHallazgo() throws Exception {
        Hallazgo original = crearHallazgoPrueba();
        modelo.agregarHallazgo(original);

        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        Hallazgo actualizado = crearHallazgoPrueba();
        modelo.actualizarHallazgo(original, actualizado);

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(), "El escucha debió ser notificado al actualizar");
    }

    @Test
    @DisplayName("Múltiples escuchas reciben notificación")
    void multiplesEscuchasRecibenNotificacion() throws Exception {
        AtomicInteger contador1 = new AtomicInteger(0);
        AtomicInteger contador2 = new AtomicInteger(0);

        modelo.agregarEscucha(() -> contador1.incrementAndGet());
        modelo.agregarEscucha(() -> contador2.incrementAndGet());

        modelo.agregarHallazgo(crearHallazgoPrueba());

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador1.get(), "El primer escucha debió ser notificado");
        assertEquals(1, contador2.get(), "El segundo escucha debió ser notificado");
    }

    @Test
    @DisplayName("Eliminar escucha deja de recibir notificaciones")
    void eliminarEscuchaDejaDeRecibirNotificaciones() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        EscuchaCambiosHallazgos escucha = () -> contador.incrementAndGet();

        modelo.agregarEscucha(escucha);
        modelo.agregarHallazgo(crearHallazgoPrueba()); // Primera notificación
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, contador.get(), "Debió recibir primera notificación");

        modelo.eliminarEscucha(escucha);
        modelo.agregarHallazgo(crearHallazgoPrueba()); // Segunda notificación
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(), "No debió recibir segunda notificación");
    }

    @Test
    @DisplayName("Notificación se ejecuta en EDT para agregar hallazgo")
    void notificacionSeEjecutaEnEDTParaAgregarHallazgo() throws Exception {
        Boolean[] seEjecutoEnEDT = {null};
        modelo.agregarEscucha(() -> {
            seEjecutoEnEDT[0] = javax.swing.SwingUtilities.isEventDispatchThread();
        });

        modelo.agregarHallazgo(crearHallazgoPrueba());

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(Boolean.TRUE, seEjecutoEnEDT[0],
            "La notificación debió ejecutarse en el EDT");
    }

    private Hallazgo crearHallazgoPrueba() {
        return new Hallazgo(
            "https://example.com",
            "Título prueba",
            "Descripción prueba",
            "Medium",
            "Certain"
        );
    }
}
