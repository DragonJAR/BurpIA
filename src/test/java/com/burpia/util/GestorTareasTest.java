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

        assertEquals(1, modelo.obtenerNumeroTareas(), "assertEquals failed at GestorTareasTest.java:47");
        assertEquals(tarea.obtenerId(), modelo.obtenerIdTarea(0), "assertEquals failed at GestorTareasTest.java:48");
        assertEquals(Tarea.ESTADO_EN_COLA, modelo.getValueAt(0, 2), "assertEquals failed at GestorTareasTest.java:49");

        gestor.actualizarTarea(tarea.obtenerId(), Tarea.ESTADO_COMPLETADO, "ok");
        flushEdt();

        assertEquals(Tarea.ESTADO_COMPLETADO, modelo.getValueAt(0, 2), "assertEquals failed at GestorTareasTest.java:54");
    }

    @Test
    @DisplayName("Pausar, reanudar y cancelar reflejan estados correctos")
    void testPausarReanudarCancelar() throws Exception {
        Tarea tarea = gestor.crearTarea("Analisis HTTP", "https://example.com/a", Tarea.ESTADO_ANALIZANDO, "run");
        flushEdt();

        assertTrue(gestor.pausarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:63");
        flushEdt();
        assertTrue(gestor.estaTareaPausada(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:65");

        assertTrue(gestor.reanudarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:67");
        flushEdt();
        assertFalse(gestor.estaTareaPausada(tarea.obtenerId()), "assertFalse failed at GestorTareasTest.java:69");

        assertTrue(gestor.cancelarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:71");
        flushEdt();
        assertTrue(gestor.estaTareaCancelada(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:73");
    }

    @Test
    @DisplayName("Limpiar tarea por id elimina fila correcta")
    void testLimpiarTareaPorId() throws Exception {
        Tarea t1 = gestor.crearTarea("A", "https://example.com/1", Tarea.ESTADO_EN_COLA, "");
        Tarea t2 = gestor.crearTarea("B", "https://example.com/2", Tarea.ESTADO_EN_COLA, "");
        flushEdt();
        assertEquals(2, modelo.obtenerNumeroTareas(), "assertEquals failed at GestorTareasTest.java:82");

        assertTrue(gestor.limpiarTarea(t1.obtenerId()), "assertTrue failed at GestorTareasTest.java:84");
        flushEdt();

        assertEquals(1, modelo.obtenerNumeroTareas(), "assertEquals failed at GestorTareasTest.java:87");
        assertEquals(t2.obtenerId(), modelo.obtenerIdTarea(0), "assertEquals failed at GestorTareasTest.java:88");
    }

    @Test
    @DisplayName("Operaciones por id retornan éxito real según transición aplicada")
    void testOperacionesRetornanResultadoReal() throws Exception {
        Tarea tarea = gestor.crearTarea("A", "https://example.com/return", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        assertTrue(gestor.pausarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:97");
        assertFalse(gestor.pausarTarea(tarea.obtenerId()), "assertFalse failed at GestorTareasTest.java:98");

        assertTrue(gestor.reanudarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:100");
        assertFalse(gestor.reanudarTarea(tarea.obtenerId()), "assertFalse failed at GestorTareasTest.java:101");

        assertTrue(gestor.cancelarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:103");
        assertFalse(gestor.cancelarTarea(tarea.obtenerId()), "assertFalse failed at GestorTareasTest.java:104");

        assertTrue(gestor.limpiarTarea(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:106");
        assertFalse(gestor.limpiarTarea(tarea.obtenerId()), "assertFalse failed at GestorTareasTest.java:107");
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
        assertEquals(Tarea.ESTADO_EN_COLA, pausadaActualizada.obtenerEstado(), "assertEquals failed at GestorTareasTest.java:122");
        assertEquals(Tarea.ESTADO_ANALIZANDO, analizandoActualizada.obtenerEstado(), "assertEquals failed at GestorTareasTest.java:123");
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

        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:137");
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:138");
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:139");
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

        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada1.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:153");
        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada2.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:154");
        assertEquals(Tarea.ESTADO_ERROR, gestor.obtenerTarea(error.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:155");
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

        assertTrue(gestor.marcarTareaAnalizando(enCola.obtenerId(), "inicio"), "assertTrue failed at GestorTareasTest.java:178");
        assertFalse(gestor.marcarTareaAnalizando(pausada.obtenerId(), "inicio"), "assertFalse failed at GestorTareasTest.java:179");
        flushEdt();

        assertEquals(Tarea.ESTADO_ANALIZANDO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:182");
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado(), "assertEquals failed at GestorTareasTest.java:183");
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
            Thread.sleep(10);
            gestorRetencion.actualizarTarea(t2.obtenerId(), Tarea.ESTADO_ERROR, "err");
            Thread.sleep(10);
            gestorRetencion.actualizarTarea(t3.obtenerId(), Tarea.ESTADO_CANCELADO, "cancel");
            flushEdt();

            // Usa reflection para invocar metodo privado de verificacion y probar logica interna
            Method verificar = GestorTareas.class.getDeclaredMethod("verificarTareasAtascadas");
            verificar.setAccessible(true);
            verificar.invoke(gestorRetencion);
            flushEdt();

            assertNull(gestorRetencion.obtenerTarea(t1.obtenerId()), "assertNull failed at GestorTareasTest.java:209");
            assertNotNull(gestorRetencion.obtenerTarea(t2.obtenerId()), "assertNotNull failed at GestorTareasTest.java:210");
            assertNotNull(gestorRetencion.obtenerTarea(t3.obtenerId()), "assertNotNull failed at GestorTareasTest.java:211");
        } finally {
            gestorRetencion.detener();
        }
    }

    @Test
    @DisplayName("Purga visual del modelo mantiene sincronizado el mapa del gestor")
    void testPurgaModeloSincronizaMapaGestor() throws Exception {
        ModeloTablaTareas modeloLimitado = new ModeloTablaTareas(1);
        GestorTareas gestorLimitado = new GestorTareas(modeloLimitado, logs::add);
        try {
            Tarea antigua = gestorLimitado.crearTarea("A", "https://example.com/old", Tarea.ESTADO_COMPLETADO, "");
            Tarea reciente = gestorLimitado.crearTarea("B", "https://example.com/new", Tarea.ESTADO_ERROR, "");
            flushEdt();

            assertNull(gestorLimitado.obtenerTarea(antigua.obtenerId()), "assertNull failed at GestorTareasTest.java:227");
            assertNotNull(gestorLimitado.obtenerTarea(reciente.obtenerId()), "assertNotNull failed at GestorTareasTest.java:228");
            assertEquals(1, modeloLimitado.obtenerNumeroTareas(), "assertEquals failed at GestorTareasTest.java:229");
            assertEquals(reciente.obtenerId(), modeloLimitado.obtenerIdTarea(0), "assertEquals failed at GestorTareasTest.java:230");
        } finally {
            gestorLimitado.detener();
        }
    }

    @Test
    @DisplayName("Cambio de límite en el modelo también sincroniza el mapa del gestor")
    void testCambioLimiteModeloSincronizaMapaGestor() throws Exception {
        ModeloTablaTareas modeloAjustable = new ModeloTablaTareas(10);
        GestorTareas gestorAjustable = new GestorTareas(modeloAjustable, logs::add);
        try {
            Tarea antigua = gestorAjustable.crearTarea("A", "https://example.com/old-limit", Tarea.ESTADO_COMPLETADO, "");
            Tarea reciente = gestorAjustable.crearTarea("B", "https://example.com/new-limit", Tarea.ESTADO_ERROR, "");
            flushEdt();

            modeloAjustable.establecerLimiteFilas(1);
            flushEdt();

            assertNull(gestorAjustable.obtenerTarea(antigua.obtenerId()), "assertNull failed at GestorTareasTest.java:244");
            assertNotNull(gestorAjustable.obtenerTarea(reciente.obtenerId()), "assertNotNull failed at GestorTareasTest.java:245");
            assertEquals(1, modeloAjustable.obtenerNumeroTareas(), "assertEquals failed at GestorTareasTest.java:246");
            assertEquals(reciente.obtenerId(), modeloAjustable.obtenerIdTarea(0), "assertEquals failed at GestorTareasTest.java:247");
        } finally {
            gestorAjustable.detener();
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
        assertEquals(total, ids.size(), "assertEquals failed at GestorTareasTest.java:245");
    }

    @Test
    @DisplayName("Tareas atascadas se marcan error y disparan cancelación activa")
    void testTareasAtascadasDisparanCancelacion() throws Exception {
        List<String> cancelaciones = new ArrayList<>();
        gestor.establecerManejadorCancelacion(cancelaciones::add);

        Tarea tarea = gestor.crearTarea("A", "https://example.com/stuck", Tarea.ESTADO_ANALIZANDO, "");
        flushEdt();

        // Usa reflection para simular tarea atascada modificando timestamp interno
        Field tiempoUltimoInicioAnalisisField = Tarea.class.getDeclaredField("tiempoUltimoInicioAnalisis");
        tiempoUltimoInicioAnalisisField.setAccessible(true);
        tiempoUltimoInicioAnalisisField.setLong(tarea, System.currentTimeMillis() - 600_000L);

        // Invoca metodo privado de verificacion mediante reflection
        Method verificar = GestorTareas.class.getDeclaredMethod("verificarTareasAtascadas");
        verificar.setAccessible(true);
        verificar.invoke(gestor);
        flushEdt();

        Tarea actualizada = gestor.obtenerTarea(tarea.obtenerId());
        assertNotNull(actualizada, "assertNotNull failed at GestorTareasTest.java:269");
        assertEquals(Tarea.ESTADO_ERROR, actualizada.obtenerEstado(), "assertEquals failed at GestorTareasTest.java:270");
        assertTrue(cancelaciones.contains(tarea.obtenerId()), "assertTrue failed at GestorTareasTest.java:271");
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
