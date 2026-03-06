package com.burpia.ui;

import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de notificación de cambios en ModeloTablaHallazgos.
 *
 * Estos tests verifican que el modelo notifica a sus suscriptores cuando:
 * - Se agrega un hallazgo (individual y en lote)
 * - Se eliminan hallazgos
 * - Se limpia la tabla
 * - Se actualiza un hallazgo
 * - Las notificaciones se ejecutan en EDT
 * - Excepciones en escuchas no afectan a otros escuchas
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
        // Agregar hallazgos primero y esperar a que se procesen las notificaciones
        modelo.agregarHallazgo(crearHallazgoPrueba());
        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {}); // Esperar notificaciones de agregar

        // Ahora agregar el escucha y eliminar
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

        // Esperar a que se procese la notificación de agregar antes de registrar el escucha
        SwingUtilities.invokeAndWait(() -> {});

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

    @Test
    @DisplayName("Notificación se ejecuta en EDT para limpiar tabla")
    void notificacionSeEjecutaEnEDTParaLimpiarTabla() throws Exception {
        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});

        Boolean[] seEjecutoEnEDT = {null};
        modelo.agregarEscucha(() -> {
            seEjecutoEnEDT[0] = javax.swing.SwingUtilities.isEventDispatchThread();
        });

        modelo.limpiar();
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(Boolean.TRUE, seEjecutoEnEDT[0],
            "La notificación de limpiar debió ejecutarse en el EDT");
    }

    @Test
    @DisplayName("Notificación se ejecuta en EDT para eliminar hallazgo")
    void notificacionSeEjecutaEnEDTParaEliminarHallazgo() throws Exception {
        modelo.agregarHallazgo(crearHallazgoPrueba());
        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});

        Boolean[] seEjecutoEnEDT = {null};
        modelo.agregarEscucha(() -> {
            seEjecutoEnEDT[0] = javax.swing.SwingUtilities.isEventDispatchThread();
        });

        modelo.eliminarHallazgo(0);
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(Boolean.TRUE, seEjecutoEnEDT[0],
            "La notificación de eliminar debió ejecutarse en el EDT");
    }

    @Test
    @DisplayName("Notificación se ejecuta en EDT para actualizar hallazgo")
    void notificacionSeEjecutaEnEDTParaActualizarHallazgo() throws Exception {
        Hallazgo original = crearHallazgoPrueba();
        modelo.agregarHallazgo(original);
        SwingUtilities.invokeAndWait(() -> {});

        Boolean[] seEjecutoEnEDT = {null};
        modelo.agregarEscucha(() -> {
            seEjecutoEnEDT[0] = javax.swing.SwingUtilities.isEventDispatchThread();
        });

        Hallazgo actualizado = crearHallazgoPrueba();
        modelo.actualizarHallazgo(original, actualizado);
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(Boolean.TRUE, seEjecutoEnEDT[0],
            "La notificación de actualizar debió ejecutarse en el EDT");
    }

    @Test
    @DisplayName("Agregar hallazgos en lote notifica una vez")
    void agregarHallazgosEnLoteNotificaUnaVez() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        List<Hallazgo> hallazgos = new ArrayList<>();
        hallazgos.add(crearHallazgoPrueba());
        hallazgos.add(crearHallazgoPrueba());
        hallazgos.add(crearHallazgoPrueba());

        modelo.agregarHallazgos(hallazgos);

        // Esperar a que se procesen eventos pendientes en EDT
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(),
            "El escucha debió ser notificado una sola vez por el lote");
        assertEquals(3, modelo.obtenerNumeroHallazgos(),
            "Deben haber 3 hallazgos en el modelo");
    }

    @Test
    @DisplayName("Agregar lista vacía no notifica")
    void agregarListaVaciaNoNotifica() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        modelo.agregarHallazgos(Collections.emptyList());

        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(0, contador.get(),
            "No debe haber notificación al agregar lista vacía");
    }

    @Test
    @DisplayName("Agregar lista null no notifica")
    void agregarListaNullNoNotifica() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        // Suprime advertencia intencional para probar caso null
        // noinspection DataFlowIssue
        modelo.agregarHallazgos(null);

        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(0, contador.get(),
            "No debe haber notificación al agregar lista null");
    }

    @Test
    @DisplayName("Excepción en un escucha no afecta a otros escuchas")
    void excepcionEnEscuchaNoAfectaOtrosEscuchas() throws Exception {
        AtomicInteger contadorEscuchaOk = new AtomicInteger(0);

        // Escucha que lanza excepción
        modelo.agregarEscucha(() -> {
            throw new RuntimeException("Error simulado en escucha");
        });

        // Escucha que debería seguir funcionando
        modelo.agregarEscucha(() -> contadorEscuchaOk.incrementAndGet());

        modelo.agregarHallazgo(crearHallazgoPrueba());

        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contadorEscuchaOk.get(),
            "El segundo escucha debe recibir notificación aunque el primero falle");
    }

    @Test
    @DisplayName("Agregar escucha null no causa error")
    void agregarEscuchaNullNoCausaError() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        // Agregar escucha null no debería causar error
        assertDoesNotThrow(() -> modelo.agregarEscucha(null),
            "Agregar escucha null no debe lanzar excepción");

        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(),
            "El escucha válido debe seguir recibiendo notificaciones");
    }

    @Test
    @DisplayName("Eliminar escucha null no causa error")
    void eliminarEscuchaNullNoCausaError() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        EscuchaCambiosHallazgos escucha = () -> contador.incrementAndGet();
        modelo.agregarEscucha(escucha);

        // Eliminar escucha null no debería causar error
        assertDoesNotThrow(() -> modelo.eliminarEscucha(null),
            "Eliminar escucha null no debe lanzar excepción");

        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(),
            "El escucha válido debe seguir recibiendo notificaciones");
    }

    @Test
    @DisplayName("Agregar hallazgos en lote ignora elementos null")
    void agregarHallizgosIgnoraElementosNull() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        List<Hallazgo> hallazgos = new ArrayList<>();
        hallazgos.add(crearHallazgoPrueba());
        hallazgos.add(null);
        hallazgos.add(crearHallazgoPrueba());

        modelo.agregarHallazgos(hallazgos);

        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(),
            "El escucha debió ser notificado una vez");
        assertEquals(2, modelo.obtenerNumeroHallazgos(),
            "Solo deben agregarse los hallazgos no null");
    }

    @Test
    @DisplayName("Notificación con múltiples operaciones secuenciales")
    void notificacionConMultiplesOperacionesSecuenciales() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});
        int conteoDespuesAgregar = contador.get();

        modelo.limpiar();
        SwingUtilities.invokeAndWait(() -> {});
        int conteoDespuesLimpiar = contador.get();

        modelo.agregarHallazgos(List.of(crearHallazgoPrueba(), crearHallazgoPrueba()));
        SwingUtilities.invokeAndWait(() -> {});
        int conteoFinal = contador.get();

        assertEquals(1, conteoDespuesAgregar, "Debe haber 1 notificación después de agregar");
        assertEquals(2, conteoDespuesLimpiar, "Debe haber 2 notificaciones después de limpiar");
        assertEquals(3, conteoFinal, "Debe haber 3 notificaciones al final");
    }

    @Test
    @DisplayName("Actualizar hallazgo por índice notifica cambios")
    void actualizarHallazgoPorIndiceNotificaCambios() throws Exception {
        modelo.agregarHallazgo(crearHallazgoPrueba());
        SwingUtilities.invokeAndWait(() -> {});

        AtomicInteger contador = new AtomicInteger(0);
        modelo.agregarEscucha(() -> contador.incrementAndGet());

        Hallazgo actualizado = crearHallazgoPrueba();
        modelo.actualizarHallazgo(0, actualizado);

        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, contador.get(),
            "El escucha debió ser notificado al actualizar por índice");
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
