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
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        assertEquals(Tarea.ESTADO_EN_COLA, gestor.obtenerTarea(pausada.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:69");
        assertEquals(Tarea.ESTADO_ANALIZANDO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:70");
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

        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(enCola.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:84");
        assertEquals(Tarea.ESTADO_PAUSADO, gestor.obtenerTarea(analizando.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:85");
    }

    @Test
    @DisplayName("Captura de seleccion ignora índices inválidos y duplicados")
    void testCapturarSeleccionRobusta() throws Exception {
        Tarea enCola = gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        Object resultado = assertDoesNotThrow(() -> metodoCapturarSeleccion.invoke(panel, (Object) new int[]{0, -1, 999, 0}));
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertEquals(1, seleccion.size(), "assertEquals failed at PanelTareasAccionesTest.java:97");

        Object item = seleccion.get(0);
        Field tareaId = item.getClass().getDeclaredField("tareaId");
        Field estado = item.getClass().getDeclaredField("estado");
        tareaId.setAccessible(true);
        estado.setAccessible(true);
        assertEquals(enCola.obtenerId(), tareaId.get(item), "assertEquals failed at PanelTareasAccionesTest.java:104");
        assertEquals(Tarea.ESTADO_EN_COLA, estado.get(item), "assertEquals failed at PanelTareasAccionesTest.java:105");
    }

    @Test
    @DisplayName("Captura de seleccion retorna lista vacia con array vacio")
    void testCapturarSeleccionVacia() throws Exception {
        flushEdt();

        Object resultado = metodoCapturarSeleccion.invoke(panel, (Object) new int[]{});
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertNotNull(seleccion, "assertNotNull failed at PanelTareasAccionesTest.java:116");
        assertTrue(seleccion.isEmpty(), "assertTrue failed at PanelTareasAccionesTest.java:117");
    }

    @Test
    @DisplayName("Captura de seleccion con todos indices invalidos retorna lista vacia")
    void testCapturarSeleccionSoloInvalidos() throws Exception {
        gestor.crearTarea("A", "https://example.com/cola", Tarea.ESTADO_EN_COLA, "");
        flushEdt();

        Object resultado = metodoCapturarSeleccion.invoke(panel, (Object) new int[]{-1, -5, 999, -100});
        @SuppressWarnings("unchecked")
        List<Object> seleccion = (List<Object>) resultado;
        assertNotNull(seleccion, "assertNotNull failed at PanelTareasAccionesTest.java:129");
        assertTrue(seleccion.isEmpty(), "assertTrue failed at PanelTareasAccionesTest.java:130");
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

        assertTrue(resultado, "assertTrue failed at PanelTareasAccionesTest.java:147");
        assertEquals(error.obtenerId(), tareaReintentada.get(), "assertEquals failed at PanelTareasAccionesTest.java:148");
        assertEquals(Tarea.ESTADO_ERROR, gestor.obtenerTarea(error.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:149");
    }

    @Test
    @DisplayName("Reintentar sin manejador solo cuenta éxito si el gestor realmente reencola")
    void testReintentarSinManejadorRetornaEstadoReal() throws Exception {
        Tarea completada = gestor.crearTarea("A", "https://example.com/done", Tarea.ESTADO_COMPLETADO, "");
        flushEdt();

        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, completada.obtenerId());

        assertFalse(resultado, "assertFalse failed at PanelTareasAccionesTest.java:160");
        assertEquals(Tarea.ESTADO_COMPLETADO, gestor.obtenerTarea(completada.obtenerId()).obtenerEstado(), "assertEquals failed at PanelTareasAccionesTest.java:161");
    }

    @Test
    @DisplayName("Reencolar tarea con id null retorna false")
    void testReencolarTareaIdNull() throws Exception {
        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, (String) null);
        assertFalse(resultado, "assertFalse failed at PanelTareasAccionesTest.java:168");
    }

    @Test
    @DisplayName("Reencolar tarea con id vacio retorna false")
    void testReencolarTareaIdVacio() throws Exception {
        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, "");
        assertFalse(resultado, "assertFalse failed at PanelTareasAccionesTest.java:175");
    }

    @Test
    @DisplayName("Reintentar con manejador que falla retorna false sin propagar excepción")
    void testReintentarConManejadorQueLanzaRetornaFalse() throws Exception {
        Tarea error = gestor.crearTarea("A", "https://example.com/error", Tarea.ESTADO_ERROR, "");
        flushEdt();

        SwingUtilities.invokeAndWait(() -> panel.establecerManejadorReintento(tareaId -> {
            throw new IllegalStateException("fallo-controlado");
        }));

        boolean resultado = (boolean) metodoReencolarTarea.invoke(panel, error.obtenerId());

        assertFalse(resultado, "assertFalse failed at PanelTareasAccionesTest.java:187");
        assertEquals(Tarea.ESTADO_ERROR, gestor.obtenerTarea(error.obtenerId()).obtenerEstado(),
            "assertEquals failed at PanelTareasAccionesTest.java:189");
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

        assertEquals("▶️ Reanudar Todo", boton.getText(), "assertEquals failed at PanelTareasAccionesTest.java:189");

        SwingUtilities.invokeAndWait(() -> {
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals("▶️ Resume All", boton.getText(), "assertEquals failed at PanelTareasAccionesTest.java:197");
    }

    @Test
    @DisplayName("Encabezados de tabla muestran tooltips traducidos por columna")
    void testTooltipsEncabezadosTabla() throws Exception {
        JTable tabla = panel.obtenerTabla();
        JTableHeader encabezado = tabla.getTableHeader();

        assertTooltipEncabezado(encabezado, 0, I18nUI.Tooltips.Tareas.COLUMNA_TIPO());
        assertTooltipEncabezado(encabezado, 1, I18nUI.Tooltips.Tareas.COLUMNA_URL());
        assertTooltipEncabezado(encabezado, 2, I18nUI.Tooltips.Tareas.COLUMNA_ESTADO());
        assertTooltipEncabezado(encabezado, 3, I18nUI.Tooltips.Tareas.COLUMNA_DURACION());
    }

    @Test
    @DisplayName("establecerConfiguracion aplica limite de filas de tareas desde ajustes")
    void testEstablecerConfiguracionAplicaLimiteFilas() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoTareasTabla(321);

        SwingUtilities.invokeAndWait(() -> panel.establecerConfiguracion(config));

        assertEquals(321, modelo.obtenerLimiteFilas(), "assertEquals failed at PanelTareasAccionesTest.java:208");
    }

    @Test
    @DisplayName("establecerConfiguracion purga finalizadas y sincroniza el gestor")
    void testEstablecerConfiguracionPurgaFinalizadasYSincronizaGestor() throws Exception {
        Tarea antigua = null;
        Tarea reciente = null;
        for (int i = 0; i < 101; i++) {
            Tarea tarea = gestor.crearTarea(
                "A",
                "https://example.com/task-" + i,
                i == 100 ? Tarea.ESTADO_ERROR : Tarea.ESTADO_COMPLETADO,
                ""
            );
            if (i == 0) {
                antigua = tarea;
            }
            if (i == 100) {
                reciente = tarea;
            }
        }
        flushEdt();

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoTareasTabla(100);

        SwingUtilities.invokeAndWait(() -> panel.establecerConfiguracion(config));
        flushEdt();

        assertNotNull(antigua, "assertNotNull failed at PanelTareasAccionesTest.java:233");
        assertNotNull(reciente, "assertNotNull failed at PanelTareasAccionesTest.java:234");
        assertEquals(100, modelo.obtenerNumeroTareas(), "assertEquals failed at PanelTareasAccionesTest.java:235");
        assertNull(gestor.obtenerTarea(antigua.obtenerId()), "assertNull failed at PanelTareasAccionesTest.java:236");
        assertNotNull(gestor.obtenerTarea(reciente.obtenerId()), "assertNotNull failed at PanelTareasAccionesTest.java:237");
    }

    @Test
    @DisplayName("Modo angosto agrupa controles de tareas en filas lógicas")
    void testModoAngostoAgrupaControlesEnFilasLogicas() throws Exception {
        JPanel panelControles = obtenerPanelControles();
        JPanel panelResponsive = (JPanel) panelControles.getComponent(0);

        SwingUtilities.invokeAndWait(() -> {
            panel.setSize(500, 220);
            panelControles.setSize(500, 120);
            panelResponsive.setSize(460, 100);
            panelResponsive.doLayout();
        });
        flushEdt();

        assertEquals(2, panelResponsive.getComponentCount(), "assertEquals failed at PanelTareasAccionesTest.java:253");
        assertTrue(panelResponsive.getComponent(0) instanceof JPanel, "assertTrue failed at PanelTareasAccionesTest.java:254");
        assertTrue(panelResponsive.getComponent(1) instanceof JPanel, "assertTrue failed at PanelTareasAccionesTest.java:255");

        JPanel filaResumen = (JPanel) panelResponsive.getComponent(0);
        JPanel filaAcciones = (JPanel) panelResponsive.getComponent(1);
        assertEquals(1, filaResumen.getComponentCount(), "assertEquals failed at PanelTareasAccionesTest.java:259");
        assertEquals(3, filaAcciones.getComponentCount(), "assertEquals failed at PanelTareasAccionesTest.java:260");
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

    private JPanel obtenerPanelControles() throws Exception {
        Field field = PanelTareas.class.getDeclaredField("panelControles");
        field.setAccessible(true);
        return (JPanel) field.get(panel);
    }

    private void assertTooltipEncabezado(JTableHeader encabezado, int columnaVista, String esperado) throws Exception {
        Rectangle rect = encabezado.getHeaderRect(columnaVista);
        MouseEvent evento = new MouseEvent(
                encabezado,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                rect.x + Math.max(1, rect.width / 2),
                rect.y + Math.max(1, rect.height / 2),
                0,
                false);
        SwingUtilities.invokeAndWait(() -> {
            for (var listener : encabezado.getMouseMotionListeners()) {
                listener.mouseMoved(evento);
            }
        });
        assertEquals(esperado, encabezado.getToolTipText(), "assertEquals failed at PanelTareasAccionesTest.java:286");
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
