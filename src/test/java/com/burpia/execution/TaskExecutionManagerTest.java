package com.burpia.execution;

import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.swing.SwingUtilities;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TaskExecutionManager Tests")
class TaskExecutionManagerTest {

    private GestorTareas gestorTareas;
    private TaskExecutionManager manager;
    private Estadisticas estadisticas;

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdown();
        }
        if (gestorTareas != null) {
            gestorTareas.detener();
        }
    }

    @Test
    @DisplayName("programarAnalisis retorna null si la solicitud es null")
    void testProgramarAnalisisRetornaNullSiLaSolicitudEsNull() {
        manager = crearManager();

        String tareaId = manager.programarAnalisis(null, "Analisis HTTP");

        assertNull(tareaId, "assertNull failed at TaskExecutionManagerTest.java:47");
    }

    @Test
    @DisplayName("programarAnalisis crea tarea y contexto de reintento")
    @SuppressWarnings("unchecked")
    void testProgramarAnalisisCreaTareaYContextoDeReintento() throws Exception {
        manager = crearManager();
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api",
            "GET",
            "GET /api HTTP/1.1\nHost: example.com",
            "",
            "hash-task"
        );

        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(AnalizadorAI.class)) {
            String tareaId = manager.programarAnalisis(solicitud, "Analisis HTTP");
            flushEdt();

            Tarea tarea = gestorTareas.obtenerTarea(tareaId);
            Field campoContextos = TaskExecutionManager.class.getDeclaredField("contextosReintento");
            campoContextos.setAccessible(true);
            Map<String, ?> contextos = (Map<String, ?>) campoContextos.get(manager);

            assertNotNull(tareaId, "assertNotNull failed at TaskExecutionManagerTest.java:70");
            assertNotNull(tarea, "assertNotNull failed at TaskExecutionManagerTest.java:71");
            assertEquals(Tarea.ESTADO_EN_COLA, tarea.obtenerEstado(),
                "assertEquals failed at TaskExecutionManagerTest.java:73");
            assertTrue(contextos.containsKey(tareaId), "assertTrue failed at TaskExecutionManagerTest.java:75");
            assertEquals(1, construccion.constructed().size(),
                "Debe construirse un AnalizadorAI por tarea programada");
        }
    }

    @Test
    @DisplayName("reencolarTarea falla si no existe contexto")
    void testReencolarTareaFallaSiNoExisteContexto() {
        manager = crearManager();

        boolean reencolada = manager.reencolarTarea("inexistente");

        assertFalse(reencolada, "assertFalse failed at TaskExecutionManagerTest.java:88");
    }

    @Test
    @DisplayName("reencolarTarea ignora duplicados si la ejecución sigue activa")
    void testReencolarTareaIgnoraDuplicadosConEjecucionActiva() throws Exception {
        manager = crearManager();
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api/retry",
            "GET",
            "GET /api/retry HTTP/1.1\nHost: example.com",
            "",
            "hash-task-retry"
        );

        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(AnalizadorAI.class)) {
            String tareaId = manager.programarAnalisis(solicitud, "Analisis HTTP");
            flushEdt();
            establecerEjecucionActiva(tareaId);

            boolean reencolada = manager.reencolarTarea(tareaId);

            assertFalse(reencolada, "assertFalse failed at TaskExecutionManagerTest.java:reencolar:duplicada");
            assertEquals(1, construccion.constructed().size(),
                "No debe construirse un segundo AnalizadorAI mientras el primero sigue activo");
        }
    }

    @Test
    @DisplayName("Hallazgos completados se reenvian conservando la solicitud HTTP")
    @SuppressWarnings("unchecked")
    void testHallazgosCompletadosConservanSolicitud() throws Exception {
        PestaniaPrincipal pestaniaPrincipal = mock(PestaniaPrincipal.class);
        manager = crearManager(pestaniaPrincipal);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api/evidence",
            "POST",
            "POST /api/evidence HTTP/1.1\nHost: example.com",
            "{\"a\":1}",
            "hash-task-evidence"
        );

        AtomicReference<AnalizadorAI.Callback> callbackRef = new AtomicReference<>();
        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(
                AnalizadorAI.class,
                (mock, context) -> callbackRef.set((AnalizadorAI.Callback) context.arguments().get(5)))) {
            String tareaId = manager.programarAnalisis(
                solicitud,
                "Analisis HTTP"
            );
            flushEdt();

            assertNotNull(tareaId, "assertNotNull failed at TaskExecutionManagerTest.java:evidencia:tareaId");
            assertNotNull(callbackRef.get(), "assertNotNull failed at TaskExecutionManagerTest.java:evidencia:callback");
            assertEquals(1, construccion.constructed().size(),
                "Debe construirse un AnalizadorAI para propagar el callback");

            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/api/evidence",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            callbackRef.get().alCompletarAnalisis(
                new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    List.of(hallazgo),
                    solicitud.obtenerSolicitudHttp(),
                    List.of()
                )
            );
            flushEdt();

            org.mockito.ArgumentCaptor<List<Hallazgo>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(pestaniaPrincipal).agregarHallazgos(captor.capture());
            List<Hallazgo> hallazgosEnviados = captor.getValue();

            assertEquals(1, hallazgosEnviados.size(),
                "assertEquals failed at TaskExecutionManagerTest.java:evidencia:hallazgos");
        }
    }

    @Test
    @DisplayName("Completar con tarea pausada conserva hallazgos pero no sobrescribe PAUSADO")
    @SuppressWarnings("unchecked")
    void testAlCompletarConTareaPausadaNoSobrescribeEstado() throws Exception {
        PestaniaPrincipal pestaniaPrincipal = mock(PestaniaPrincipal.class);
        manager = crearManager(pestaniaPrincipal);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api/pausada",
            "GET",
            "GET /api/pausada HTTP/1.1\nHost: example.com",
            "",
            "hash-task-pausada"
        );

        AtomicReference<AnalizadorAI.Callback> callbackRef = new AtomicReference<>();
        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(
                AnalizadorAI.class,
                (mock, context) -> callbackRef.set((AnalizadorAI.Callback) context.arguments().get(5)))) {
            String tareaId = manager.programarAnalisis(solicitud, "Analisis HTTP");
            flushEdt();

            assertNotNull(tareaId, "assertNotNull failed at TaskExecutionManagerTest.java:pausa:tareaId");
            assertNotNull(callbackRef.get(), "assertNotNull failed at TaskExecutionManagerTest.java:pausa:callback");

            // El usuario pausa la tarea en la ventana final del análisis
            gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_PAUSADO, "Pausada por el usuario");

            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/api/pausada",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            callbackRef.get().alCompletarAnalisis(
                new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    List.of(hallazgo),
                    solicitud.obtenerSolicitudHttp(),
                    List.of()
                )
            );
            flushEdt();

            assertEquals(Tarea.ESTADO_PAUSADO, gestorTareas.obtenerTarea(tareaId).obtenerEstado(),
                "La completación tardía no debe sobrescribir PAUSADO→COMPLETADO");
            assertEquals(1, construccion.constructed().size(),
                "Debe construirse un único AnalizadorAI para la tarea pausada");
            verify(pestaniaPrincipal).agregarHallazgos(org.mockito.ArgumentMatchers.anyList());
        }
    }

    @Test
    @DisplayName("Completar con hallazgos incrementa el contador de hallazgos creados")
    void testContadorHallazgosIncrementaTrasCompletarConHallazgos() throws Exception {
        PestaniaPrincipal pestaniaPrincipal = mock(PestaniaPrincipal.class);
        manager = crearManager(pestaniaPrincipal);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api/stats",
            "GET",
            "GET /api/stats HTTP/1.1\nHost: example.com",
            "",
            "hash-task-stats"
        );

        AtomicReference<AnalizadorAI.Callback> callbackRef = new AtomicReference<>();
        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(
                AnalizadorAI.class,
                (mock, context) -> callbackRef.set((AnalizadorAI.Callback) context.arguments().get(5)))) {
            String tareaId = manager.programarAnalisis(solicitud, "Analisis HTTP");
            flushEdt();

            assertNotNull(tareaId, "assertNotNull failed at TaskExecutionManagerTest.java:stats:tareaId");
            assertNotNull(callbackRef.get(), "assertNotNull failed at TaskExecutionManagerTest.java:stats:callback");
            assertEquals(1, construccion.constructed().size(),
                "Debe construirse un único AnalizadorAI para la tarea");

            Hallazgo hallazgo1 = new Hallazgo(
                "https://example.com/api/stats", "Titulo 1", "Descripcion 1",
                Hallazgo.SEVERIDAD_HIGH, Hallazgo.CONFIANZA_ALTA);
            Hallazgo hallazgo2 = new Hallazgo(
                "https://example.com/api/stats", "Titulo 2", "Descripcion 2",
                Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.CONFIANZA_MEDIA);

            callbackRef.get().alCompletarAnalisis(
                new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    List.of(hallazgo1, hallazgo2),
                    solicitud.obtenerSolicitudHttp(),
                    List.of()
                )
            );
            flushEdt();

            assertEquals(2, estadisticas.obtenerHallazgosCreados(),
                "El contador debe reflejar cada hallazgo agregado tras completar el análisis");
        }
    }

    @Test
    @DisplayName("Resultado de tarea cancelada no incrementa el contador de hallazgos")
    void testContadorHallazgosNoIncrementaSiTareaCancelada() throws Exception {
        PestaniaPrincipal pestaniaPrincipal = mock(PestaniaPrincipal.class);
        manager = crearManager(pestaniaPrincipal);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api/cancelada",
            "GET",
            "GET /api/cancelada HTTP/1.1\nHost: example.com",
            "",
            "hash-task-cancelada"
        );

        AtomicReference<AnalizadorAI.Callback> callbackRef = new AtomicReference<>();
        try (MockedConstruction<AnalizadorAI> construccion = mockConstruction(
                AnalizadorAI.class,
                (mock, context) -> callbackRef.set((AnalizadorAI.Callback) context.arguments().get(5)))) {
            String tareaId = manager.programarAnalisis(solicitud, "Analisis HTTP");
            flushEdt();

            assertNotNull(callbackRef.get(), "assertNotNull failed at TaskExecutionManagerTest.java:cancel:callback");
            assertEquals(1, construccion.constructed().size(),
                "Debe construirse un único AnalizadorAI para la tarea cancelada");

            // El usuario cancela en la ventana final del análisis: el resultado
            // tardío debe descartarse completo (sin hallazgos fantasma ni conteo).
            gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_CANCELADO, "Cancelada por el usuario");

            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/api/cancelada", "Titulo", "Descripcion",
                Hallazgo.SEVERIDAD_HIGH, Hallazgo.CONFIANZA_ALTA);

            callbackRef.get().alCompletarAnalisis(
                new ResultadoAnalisisMultiple(
                    solicitud.obtenerUrl(),
                    List.of(hallazgo),
                    solicitud.obtenerSolicitudHttp(),
                    List.of()
                )
            );
            flushEdt();

            assertEquals(0, estadisticas.obtenerHallazgosCreados(),
                "Un resultado descartado por cancelación no debe contar hallazgos");
        }
    }

    private TaskExecutionManager crearManager() {
        return crearManager(null);
    }

    private TaskExecutionManager crearManager(PestaniaPrincipal pestaniaPrincipal) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoConcurrente(1);
        gestorTareas = new GestorTareas(new ModeloTablaTareas(), mensaje -> { });
        estadisticas = new Estadisticas();
        return new TaskExecutionManager(
            config,
            gestorTareas,
            null,
            pestaniaPrincipal,
            new PrintWriter(new StringWriter(), true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            estadisticas
        );
    }

    @SuppressWarnings("unchecked")
    private void establecerEjecucionActiva(String tareaId) throws Exception {
        Field campoEjecuciones = TaskExecutionManager.class.getDeclaredField("ejecucionesActivas");
        campoEjecuciones.setAccessible(true);
        Map<String, Future<?>> ejecuciones = (Map<String, Future<?>>) campoEjecuciones.get(manager);

        Future<?> future = mock(Future.class);
        when(future.isDone()).thenReturn(false);
        when(future.isCancelled()).thenReturn(false);
        ejecuciones.put(tareaId, future);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
