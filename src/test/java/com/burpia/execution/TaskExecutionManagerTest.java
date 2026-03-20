package com.burpia.execution;

import com.burpia.analyzer.AnalizadorAI;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.config.ConfiguracionAPI;
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

        String tareaId = manager.programarAnalisis(null, null, "Analisis HTTP");

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
            String tareaId = manager.programarAnalisis(solicitud, null, "Analisis HTTP");
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
            String tareaId = manager.programarAnalisis(solicitud, null, "Analisis HTTP");
            flushEdt();
            establecerEjecucionActiva(tareaId);

            boolean reencolada = manager.reencolarTarea(tareaId);

            assertFalse(reencolada, "assertFalse failed at TaskExecutionManagerTest.java:reencolar:duplicada");
            assertEquals(1, construccion.constructed().size(),
                "No debe construirse un segundo AnalizadorAI mientras el primero sigue activo");
        }
    }

    @Test
    @DisplayName("Hallazgos completados heredan evidenciaId de la tarea")
    @SuppressWarnings("unchecked")
    void testHallazgosCompletadosHeredanEvidenciaId() throws Exception {
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
            HttpRequestResponse evidenciaHttp = mock(HttpRequestResponse.class);
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);
            ByteArray requestBytes = mock(ByteArray.class);
            ByteArray responseBytes = mock(ByteArray.class);

            when(evidenciaHttp.request()).thenReturn(request);
            when(evidenciaHttp.response()).thenReturn(response);
            when(request.toByteArray()).thenReturn(requestBytes);
            when(response.toByteArray()).thenReturn(responseBytes);
            when(requestBytes.getBytes()).thenReturn("request".getBytes());
            when(responseBytes.getBytes()).thenReturn("response".getBytes());

            String tareaId = manager.programarAnalisis(
                solicitud,
                evidenciaHttp,
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
            assertNotNull(hallazgosEnviados.get(0).obtenerEvidenciaId(),
                "assertNotNull failed at TaskExecutionManagerTest.java:evidencia:evidenciaId");
        }
    }

    private TaskExecutionManager crearManager() {
        return crearManager(null);
    }

    private TaskExecutionManager crearManager(PestaniaPrincipal pestaniaPrincipal) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoConcurrente(1);
        gestorTareas = new GestorTareas(new ModeloTablaTareas(), mensaje -> { });
        return new TaskExecutionManager(
            config,
            gestorTareas,
            null,
            pestaniaPrincipal,
            new PrintWriter(new StringWriter(), true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            null
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
