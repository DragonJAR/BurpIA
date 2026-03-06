package com.burpia.ui;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
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
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PanelTareas Acciones Tests")
class PanelTareasAccionesTest {

    private ModeloTablaTareas modelo;
    private GestorTareas gestor;
    private PanelTareas panel;
    private Method metodoReencolarTarea;
    private Method metodoCapturarSeleccion;

    @BeforeEach
    void setUp() throws Exception {
        I18nUI.establecerIdioma("es");
        modelo = new ModeloTablaTareas();
        List<String> logs = new ArrayList<>();
        gestor = new GestorTareas(modelo, logs::add);
        SwingUtilities.invokeAndWait(() -> panel = new PanelTareas(gestor, modelo));
        
        // DRY: Inicializar métodos de reflexión una sola vez
        metodoReencolarTarea = PanelTareas.class.getDeclaredMethod("reencolarTarea", String.class);
        metodoReencolarTarea.setAccessible(true);
        metodoCapturarSeleccion = PanelTareas.class.getDeclaredMethod("capturarSeleccion", int[].class);
        metodoCapturarSeleccion.setAccessible(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (panel != null) {
                panel.destruir();
            }
        });
        gestor.detener();
        I18nUI.establecerIdioma("es");
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
    @DisplayName("Captura de seleccion ignora índices inválidos y duplicados")
    void testCapturarSeleccionRobusta() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        Object resultado = assertDoesNotThrow(() -> metodoCapturarSeleccion.invoke(panel, (Object) new int[]{0, -1, 999, 0}));
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

    @Test
    @DisplayName("Captura de seleccion retorna lista vacia con array vacio")
    void testCapturarSeleccionVacia() throws Exception {
        flushEdt();

        Object resultado = metodoCapturarSeleccion.invoke(panel, (Object) new int[]{});
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertNotNull(seleccion);
        assertTrue(seleccion.isEmpty());
    }

    @Test
    @DisplayName("Captura de seleccion con todos indices invalidos retorna lista vacia")
    void testCapturarSeleccionSoloInvalidos() throws Exception {
        gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        Object resultado = metodoCapturarSeleccion.invoke(panel, (Object) new int[]{-1, -5, 999, -100});
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertNotNull(seleccion);
        assertTrue(seleccion.isEmpty());
    }

    @Test
    @DisplayName("Reintentar usa manejador real de reencolado cuando existe")
    void testReintentarUsaManejadorReencolado() throws Exception {
        Tarea error = gestor.crearTarea("A", "https://example.com/error", Tarea.ESTADO_ERROR, "");
        flushEdt();

        AtomicReference<String> tareaReintentada = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panel.establecerManejadorReintento(tareaId -> {
            tareaReintentada.set(tareaId);
            return true;
        }));

        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, error.obtenerId());

        assertTrue(resultado);
        assertEquals(error.obtenerId(), tareaReintentada.get());
        assertEquals(Tarea.ESTADO_ERROR, gestor.obtenerTarea(error.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Reintentar sin manejador solo cuenta éxito si el gestor realmente reencola")
    void testReintentarSinManejadorRetornaEstadoReal() throws Exception {
        Tarea completada = gestor.crearTarea("A", "https://example.com/done", Tarea.ESTADO_COMPLETADO, "");
        flushEdt();

        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, completada.obtenerId());

        assertFalse(resultado);
        assertEquals(Tarea.ESTADO_COMPLETADO, gestor.obtenerTarea(completada.obtenerId()).obtenerEstado());
    }

    @Test
    @DisplayName("Reencolar tarea con id null retorna false")
    void testReencolarTareaIdNull() throws Exception {
        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, (String) null);
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Reencolar tarea con id vacio retorna false")
    void testReencolarTareaIdVacio() throws Exception {
        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, "");
        assertFalse(resultado);
    }

    @Test
    @DisplayName("aplicarIdioma actualiza boton principal aun sin cambios de version")
    void testAplicarIdiomaRefrescaBotonPrincipal() throws Exception {
        gestor.crearTarea("A", "https://example.com/pausada", Tarea.ESTADO_PAUSADO, "");
        flushEdt();

        JButton boton = obtenerBotonPrincipal();
        Method actualizar = obtenerMetodoActualizarEstadisticas();
        actualizar.invoke(panel);
        flushEdt();

        assertEquals("▶️ Reanudar Todo", boton.getText());

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals("▶️ Resume All", boton.getText());
    }

    @Test
    @DisplayName("establecerConfiguracion aplica limite de filas de tareas desde ajustes")
    void testEstablecerConfiguracionAplicaLimiteFilas() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoTareasTabla(321);

        SwingUtilities.invokeAndWait(() -> panel.establecerConfiguracion(config));

        assertEquals(321, modelo.obtenerLimiteFilas());
    }

    private JButton obtenerBotonPrincipal() throws Exception {
        Field field = PanelTareas.class.getDeclaredField("botonPausarReanudar");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private Method obtenerMetodoActualizarEstadisticas() throws Exception {
        Method metodo = PanelTareas.class.getDeclaredMethod("actualizarEstadisticas");
        metodo.setAccessible(true);
        return metodo;
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
