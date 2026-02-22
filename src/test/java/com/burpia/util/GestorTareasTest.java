package com.burpia.util;

import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GestorTareas Integration Tests")
class GestorTareasTest {

    private ModeloTablaTareas modelo;
    private GestorTareas gestor;
    private List<String> logs;

    @BeforeEach
    void setUp() {
        modelo = new ModeloTablaTareas();
        logs = new ArrayList<>();
        gestor = new GestorTareas(modelo, logs::add);
    }

    @AfterEach
    void tearDown() {
        gestor.detener();
    }

    @Test
    @DisplayName("Crear y actualizar tarea sincroniza con tabla")
    void testCrearYActualizarTarea() throws Exception {
        Tarea tarea = gestor.crearTarea("Analisis HTTP", "https://example.com", Tarea.ESTADO_EN_COLA, "en cola");
        flushEdt();

        assertEquals(1, modelo.obtenerNumeroTareas());
        assertEquals(tarea.obtenerId(), modelo.obtenerIdTarea(0));
        assertEquals(Tarea.ESTADO_EN_COLA, modelo.getValueAt(0, 2));

        gestor.actualizarTarea(tarea.obtenerId(), Tarea.ESTADO_COMPLETADO, "ok");
        flushEdt();

        assertEquals(Tarea.ESTADO_COMPLETADO, modelo.getValueAt(0, 2));
    }

    @Test
    @DisplayName("Pausar, reanudar y cancelar reflejan estados correctos")
    void testPausarReanudarCancelar() throws Exception {
        Tarea tarea = gestor.crearTarea("Analisis HTTP", "https://example.com/a", Tarea.ESTADO_ANALIZANDO, "run");
        flushEdt();

        gestor.pausarTarea(tarea.obtenerId());
        flushEdt();
        assertTrue(gestor.estaTareaPausada(tarea.obtenerId()));

        gestor.reanudarTarea(tarea.obtenerId());
        flushEdt();
        assertFalse(gestor.estaTareaPausada(tarea.obtenerId()));

        gestor.cancelarTarea(tarea.obtenerId());
        flushEdt();
        assertTrue(gestor.estaTareaCancelada(tarea.obtenerId()));
    }

    @Test
    @DisplayName("Limpiar tarea por id elimina fila correcta")
    void testLimpiarTareaPorId() throws Exception {
        Tarea t1 = gestor.crearTarea("A", "https://example.com/1", Tarea.ESTADO_EN_COLA, "");
        Tarea t2 = gestor.crearTarea("B", "https://example.com/2", Tarea.ESTADO_EN_COLA, "");
        flushEdt();
        assertEquals(2, modelo.obtenerNumeroTareas());

        gestor.limpiarTarea(t1.obtenerId());
        flushEdt();

        assertEquals(1, modelo.obtenerNumeroTareas());
        assertEquals(t2.obtenerId(), modelo.obtenerIdTarea(0));
    }

    @Test
    @DisplayName("PausarReanudarTodas en estado mixto solo reanuda pausadas")
    void testPausarReanudarTodasEstadoMixto() throws Exception {
        Tarea pausada = gestor.crearTarea("A", "https://example.com/pausa", Tarea.ESTADO_PAUSADO, "");
        Tarea analizando = gestor.crearTarea("B", "https://example.com/run", Tarea.ESTADO_ANALIZANDO, "");
        flushEdt();

        gestor.pausarReanudarTodas();
        flushEdt();

        Tarea pausadaActualizada = gestor.obtenerTarea(pausada.obtenerId());
        Tarea analizandoActualizada = gestor.obtenerTarea(analizando.obtenerId());
        assertEquals(Tarea.ESTADO_EN_COLA, pausadaActualizada.obtenerEstado());
        assertEquals(Tarea.ESTADO_ANALIZANDO, analizandoActualizada.obtenerEstado());
    }

    @Test
    @DisplayName("Pausar todas activas no afecta tareas ya pausadas")
    void testPausarTodasActivas() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        Tarea analizando = gestor.crearTarea("B", "https://example.com/run", Tarea.ESTADO_ANALIZANDO, "");
        Tarea pausada = gestor.crearTarea("C", "https://example.com/pause", Tarea.ESTADO_PAUSADO, "");
        flushEdt();

        gestor.pausarTodasActivas();
        flushEdt();

        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Reanudar todas pausadas mueve a cola")
    void testReanudarTodasPausadas() throws Exception {
        Tarea pausada1 = gestor.crearTarea("A", "https://example.com/p1", Tarea.ESTADO_PAUSADO, "");
        Tarea pausada2 = gestor.crearTarea("B", "https://example.com/p2", Tarea.ESTADO_PAUSADO, "");
        Tarea error = gestor.crearTarea("C", "https://example.com/e", Tarea.ESTADO_ERROR, "");
        flushEdt();

        gestor.reanudarTodasPausadas();
        flushEdt();

        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada1.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada2.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_ERROR, gestor.obtenerTarea(error.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Logger nulo no rompe operaciones")
    void testLoggerNuloNoRompe() throws Exception {
        GestorTareas gestorSinLogger = new GestorTareas(new ModeloTablaTareas(), null);
        try {
            assertDoesNotThrow(() -> gestorSinLogger.crearTarea("A", "https://example.com/null-logger", Tarea.ESTADO_EN_COLA, ""));
            assertDoesNotThrow(gestorSinLogger::cancelarTodas);
            flushEdt();
        } finally {
            gestorSinLogger.detener();
        }
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
