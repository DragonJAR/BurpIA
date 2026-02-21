package com.burpia.ui;

import com.burpia.model.Tarea;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PanelTareas Actions Tests")
class PanelTareasActionsTest {

    private ModeloTablaTareas modelo;
    private GestorTareas gestor;
    private PanelTareas panel;

    @BeforeEach
    void setUp() throws Exception {
        modelo = new ModeloTablaTareas();
        List<String> logs = new ArrayList<>();
        gestor = new GestorTareas(modelo, logs::add);
        SwingUtilities.invokeAndWait(() -> panel = new PanelTareas(gestor, modelo));
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (panel != null) {
                panel.destruir();
            }
        });
        gestor.detener();
    }

    @Test
    @DisplayName("Boton principal reanuda pausadas cuando existen")
    void testBotonPrincipalReanudaPausadas() throws Exception {
        Tarea pausada = gestor.crearTarea("A", "https://example.com/pausa", Tarea.ESTADO_PAUSADO, "");
        Tarea analizando = gestor.crearTarea("B", "https://example.com/run", Tarea.ESTADO_ANALIZANDO, "");
        flushEdt();

        JButton boton = obtenerBotonPrincipal();
        SwingUtilities.invokeAndWait(boton::doClick);
        flushEdt();

        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_ANALIZANDO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Boton principal pausa activas cuando no hay pausadas")
    void testBotonPrincipalPausaActivas() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        Tarea analizando = gestor.crearTarea("B", "https://example.com/run", Tarea.ESTADO_ANALIZANDO, "");
        flushEdt();

        JButton boton = obtenerBotonPrincipal();
        SwingUtilities.invokeAndWait(boton::doClick);
        flushEdt();

        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado());
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Captura de seleccion ignora indices invalidos y duplicados")
    void testCapturarSeleccionRobusta() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        Method metodo = PanelTareas.class.getDeclaredMethod("capturarSeleccion", int[].class);
        metodo.setAccessible(true);

        Object resultado = assertDoesNotThrow(() -> metodo.invoke(panel, (Object) new int[]{0, -1, 999, 0}));
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertEquals(1, seleccion.size());

        Object item = seleccion.get(0);
        Field tareaId = item.getClass().getDeclaredField("tareaId");
        Field estado = item.getClass().getDeclaredField("estado");
        tareaId.setAccessible(true);
        estado.setAccessible(true);
        assertEquals(enCola.obtenerId(), tareaId.get(item));
        assertEquals(Tarea.ESTADO_EN_COLA, estado.get(item));
    }

    private JButton obtenerBotonPrincipal() throws Exception {
        Field field = PanelTareas.class.getDeclaredField("botonPausarReanudar");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // sync EDT
        });
    }
}
