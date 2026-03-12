package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PestaniaPrincipal Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PestaniaPrincipalTest {

    private static final int TIMEOUT_ESPERA_EDT_MS = 2000;
    private static final int TIMEOUT_ESPERA_FOCO_MS = 1500;
    private static final int TIMER_FOCO_AGENTE_MS = 150;

    @Mock private MontoyaApi montoyaApi;
    @Mock private Estadisticas estadisticas;
    @Mock private GestorTareas gestorTareas;
    @Mock private GestorConsolaGUI gestorConsolaGUI;
    @Mock private ModeloTablaTareas modeloTablaTareas;
    @Mock private ModeloTablaHallazgos modeloTablaHallazgos;
    @Mock private ConfiguracionAPI configuracionAPI;
    @Mock private GestorLoggingUnificado gestorLogging;

    private PestaniaPrincipal pestaniaPrincipal;
    private final AtomicBoolean hayAgentesHabilitados = new AtomicBoolean(true);
    private final AtomicBoolean agenteSeleccionadoHabilitado = new AtomicBoolean(true);
    private final AtomicReference<String> tipoAgenteOperativo = new AtomicReference<>("FACTORY_DROID");
    private Map<String, String> estadoUIPersistido;

    @BeforeEach
    void setUp() throws Exception {
        hayAgentesHabilitados.set(true);
        agenteSeleccionadoHabilitado.set(true);
        tipoAgenteOperativo.set("FACTORY_DROID");
        estadoUIPersistido = new HashMap<>();
        I18nUI.establecerIdioma("es");

        when(configuracionAPI.hayAlgunAgenteHabilitado()).thenAnswer(invocation -> hayAgentesHabilitados.get());
        when(configuracionAPI.agenteHabilitado()).thenAnswer(invocation -> agenteSeleccionadoHabilitado.get());
        when(configuracionAPI.obtenerTipoAgenteOperativo()).thenAnswer(invocation ->
            hayAgentesHabilitados.get() ? tipoAgenteOperativo.get() : null);
        when(configuracionAPI.obtenerTipoAgente()).thenAnswer(invocation -> tipoAgenteOperativo.get());
        when(configuracionAPI.obtenerAgenteDelay()).thenReturn(1500);
        when(configuracionAPI.obtenerEstadoUI()).thenAnswer(invocation -> new HashMap<>(estadoUIPersistido));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, String> nuevoEstado = (Map<String, String>) invocation.getArgument(0);
            estadoUIPersistido = new HashMap<>(nuevoEstado);
            return null;
        }).when(configuracionAPI).establecerEstadoUI(anyMap());

        when(modeloTablaHallazgos.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaHallazgos).getColumnClass(anyInt());
        when(modeloTablaHallazgos.getColumnName(anyInt())).thenReturn("MockCol");
        when(modeloTablaHallazgos.obtenerEstadisticasVisibles()).thenReturn(new int[6]);

        when(modeloTablaTareas.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaTareas).getColumnClass(anyInt());
        when(modeloTablaTareas.getColumnName(anyInt())).thenReturn("MockCol");

        pestaniaPrincipal = crearPestaniaPrincipal();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (pestaniaPrincipal != null) {
            SwingUtilities.invokeAndWait(() -> pestaniaPrincipal.destruir());
        }
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Seleccionar pestaña agente enfoca terminal correctamente")
    void testSeleccionarPestaniaAgenteEnfocaTerminal() throws Exception {
        CountDownLatch latchSeleccion = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            pestaniaPrincipal.seleccionarPestaniaAgente();
            latchSeleccion.countDown();
        });

        assertTrue(latchSeleccion.await(TIMEOUT_ESPERA_EDT_MS, TimeUnit.MILLISECONDS),
            "Timeout esperando seleccion de pestaña");

        esperarTimerFocoAgente();

        SwingUtilities.invokeAndWait(() -> {
            PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
            assertNotNull(panelAgente, "El panel de Agente no debe ser nulo");
            JTabbedPane tabs = obtenerTabbedPane(pestaniaPrincipal);
            assertEquals(panelAgente, tabs.getSelectedComponent(),
                "La pestaña de agente debe quedar seleccionada al solicitar foco");
        });
    }

    @Test
    @DisplayName("Alternar visibilidad del agente no rompe el flujo básico")
    void testAlternarVisibilidadAgenteNoRompeFlujoBasico() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = obtenerTabbedPane(pestaniaPrincipal);
            PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
            assertNotNull(panelAgente, "El panel de Agente no debe ser nulo inicialmente");
            assertTrue(tabs.indexOfComponent(panelAgente) >= 0, "El agente debe estar visible inicialmente");

            hayAgentesHabilitados.set(false);
            agenteSeleccionadoHabilitado.set(false);
            pestaniaPrincipal.actualizarVisibilidadAgentes();
            assertEquals(-1, tabs.indexOfComponent(panelAgente), "El agente debe estar oculto tras deshabilitar");

            hayAgentesHabilitados.set(true);
            agenteSeleccionadoHabilitado.set(true);
            pestaniaPrincipal.actualizarVisibilidadAgentes();
            assertTrue(tabs.indexOfComponent(panelAgente) >= 0, "El agente debe estar visible tras re-habilitar");

            assertDoesNotThrow(() -> panelAgente.escribirComandoCrudo("echo lifecycle"),
                "Escribir comando no debe lanzar excepcion tras ciclo de visibilidad");
        });
    }

    @Test
    @DisplayName("Visibilidad del agente usa cualquier agente habilitado y no solo el seleccionado")
    void testVisibilidadAgenteUsaAlgunAgenteHabilitado() throws Exception {
        SwingUtilities.invokeAndWait(() -> pestaniaPrincipal.destruir());
        pestaniaPrincipal = null;

        hayAgentesHabilitados.set(true);
        agenteSeleccionadoHabilitado.set(false);
        tipoAgenteOperativo.set("OPEN_CODE");
        pestaniaPrincipal = crearPestaniaPrincipal();

        SwingUtilities.invokeAndWait(() -> {
            PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
            JTabbedPane tabs = obtenerTabbedPane(pestaniaPrincipal);
            assertTrue(tabs.indexOfComponent(panelAgente) >= 0,
                "assertTrue failed at PestaniaPrincipalTest.java:149");
        });
    }

    @Test
    @DisplayName("obtenerPanelAgente devuelve instancia consistente")
    void testObtenerPanelAgenteConsistente() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PanelAgente panel1 = pestaniaPrincipal.obtenerPanelAgente();
            PanelAgente panel2 = pestaniaPrincipal.obtenerPanelAgente();

            assertNotNull(panel1, "El panel de agente no debe ser nulo");
            assertSame(panel1, panel2, "obtenerPanelAgente debe devolver siempre la misma instancia");
        });
    }

    @Test
    @DisplayName("obtenerPanelConsola devuelve instancia valida")
    void testObtenerPanelConsola() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PanelConsola panel = pestaniaPrincipal.obtenerPanelConsola();
            assertNotNull(panel, "El panel de consola no debe ser nulo");
        });
    }

    @Test
    @DisplayName("Persistencia de pestaña resiste cambio de idioma usando identificador estable")
    void testPersistenciaPestaniaResisteCambioDeIdioma() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = obtenerTabbedPane(pestaniaPrincipal);
            tabs.setSelectedComponent(pestaniaPrincipal.obtenerPanelHallazgos());
        });

        SwingUtilities.invokeAndWait(() -> pestaniaPrincipal.destruir());
        pestaniaPrincipal = null;

        I18nUI.establecerIdioma("en");
        pestaniaPrincipal = crearPestaniaPrincipal();

        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = obtenerTabbedPane(pestaniaPrincipal);
            assertSame(
                pestaniaPrincipal.obtenerPanelHallazgos(),
                tabs.getSelectedComponent(),
                "La pestaña de hallazgos debe restaurarse aunque el idioma cambie"
            );
        });
    }

    private void esperarTimerFocoAgente() throws InterruptedException {
        Thread.sleep(TIMER_FOCO_AGENTE_MS + 100);
    }

    private PestaniaPrincipal crearPestaniaPrincipal() throws Exception {
        final PestaniaPrincipal[] holder = new PestaniaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PestaniaPrincipal(
            montoyaApi, estadisticas, gestorTareas, gestorConsolaGUI,
            modeloTablaTareas, modeloTablaHallazgos, true, configuracionAPI, gestorLogging
        ));
        return holder[0];
    }

    private JTabbedPane obtenerTabbedPane(PestaniaPrincipal pestania) {
        try {
            Field field = PestaniaPrincipal.class.getDeclaredField("tabbedPane");
            field.setAccessible(true);
            return (JTabbedPane) field.get(pestania);
        } catch (ReflectiveOperationException e) {
            fail("No se pudo acceder a tabbedPane para validar seleccion de foco: " + e.getMessage());
            return null;
        }
    }
}
