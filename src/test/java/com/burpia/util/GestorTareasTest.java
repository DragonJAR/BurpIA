package com.burpia.util;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    @DisplayName("Marca una tarea como analizando solo si estaba en cola")
    void testMarcarTareaAnalizando() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        Tarea pausada = gestor.crearTarea("B", "https://example.com/pause", Tarea.ESTADO_PAUSADO, "");
        flushEdt();

        assertTrue(gestor.marcarTareaAnalizando(enCola.obtenerId(), "inicio"));
        assertFalse(gestor.marcarTareaAnalizando(pausada.obtenerId(), "inicio"));
        flushEdt();

        assertEquals(Tarea.ESTADO_ANALIZANDO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Retencion elimina primero finalizadas mas antiguas")
    void testRetencionFinalizadas() throws Exception {
        GestorTareas gestorRetencion = new GestorTareas(new ModeloTablaTareas(100), logs::add, 2);
        try {
            Tarea t1 = gestorRetencion.crearTarea("A", "https://example.com/1", Tarea.ESTADO_EN_COLA, "");
            Tarea t2 = gestorRetencion.crearTarea("B", "https://example.com/2", Tarea.ESTADO_EN_COLA, "");
            Tarea t3 = gestorRetencion.crearTarea("C", "https://example.com/3", Tarea.ESTADO_EN_COLA, "");
            flushEdt();

            gestorRetencion.actualizarTarea(t1.obtenerId(), Tarea.ESTADO_COMPLETADO, "ok");
            Thread.sleep(5);
            gestorRetencion.actualizarTarea(t2.obtenerId(), Tarea.ESTADO_ERROR, "err");
            Thread.sleep(5);
            gestorRetencion.actualizarTarea(t3.obtenerId(), Tarea.ESTADO_CANCELADO, "cancel");
            flushEdt();

            Method verificar = GestorTareas.class.getDeclaredMethod("verificarTareasAtascadas");
            verificar.setAccessible(true);
            verificar.invoke(gestorRetencion);
            flushEdt();

            assertNull(gestorRetencion.obtenerTarea(t1.obtenerId()));
            assertNotNull(gestorRetencion.obtenerTarea(t2.obtenerId()));
            assertNotNull(gestorRetencion.obtenerTarea(t3.obtenerId()));
        } finally {
            gestorRetencion.detener();
        }
    }

    @Test
    @DisplayName("Id de tarea se mantiene unico bajo creacion masiva")
    void testIdUnicoEnAltaConcurrencia() {
        int total = 1000;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < total; i++) {
            Tarea tarea = gestor.crearTarea("A", "https://example.com/" + i, Tarea.ESTADO_EN_COLA, "");
            ids.add(tarea.obtenerId());
        }
        assertEquals(total, ids.size());
    }

    @Test
    @DisplayName("Tareas atascadas se marcan error y disparan cancelacion activa")
    void testTareasAtascadasDisparanCancelacion() throws Exception {
        List<String> cancelaciones = new ArrayList<>();
        gestor.establecerManejadorCancelacion(cancelaciones::add);

        Tarea tarea = gestor.crearTarea("A", "https://example.com/stuck", Tarea.ESTADO_ANALIZANDO, "");
        flushEdt();

        Field tiempoUltimoInicioAnalisisField = Tarea.class.getDeclaredField("tiempoUltimoInicioAnalisis");
        tiempoUltimoInicioAnalisisField.setAccessible(true);
        tiempoUltimoInicioAnalisisField.setLong(tarea, System.currentTimeMillis() - 600_000L);

        Method verificar = GestorTareas.class.getDeclaredMethod("verificarTareasAtascadas");
        verificar.setAccessible(true);
        verificar.invoke(gestor);
        flushEdt();

        Tarea actualizada = gestor.obtenerTarea(tarea.obtenerId());
        assertNotNull(actualizada);
        assertEquals(Tarea.ESTADO_ERROR, actualizada.obtenerEstado());
        assertTrue(cancelaciones.contains(tarea.obtenerId()));
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
