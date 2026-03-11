package com.burpia.execution;

import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

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

    private TaskExecutionManager crearManager() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoConcurrente(1);
        gestorTareas = new GestorTareas(new ModeloTablaTareas(), mensaje -> { });
        return new TaskExecutionManager(
            config,
            gestorTareas,
            null,
            null,
            new PrintWriter(new StringWriter(), true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            null
        );
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
