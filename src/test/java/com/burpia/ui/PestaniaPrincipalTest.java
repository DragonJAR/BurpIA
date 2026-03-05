package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Estadisticas;
import com.burpia.util.GestorConsolaGUI;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private PestaniaPrincipal pestaniaPrincipal;
    private final AtomicBoolean agenteHabilitado = new AtomicBoolean(true);

    @BeforeEach
    void setUp() throws Exception {
        agenteHabilitado.set(true);

        when(configuracionAPI.agenteHabilitado()).thenAnswer(invocation -> agenteHabilitado.get());
        when(configuracionAPI.obtenerAgenteDelay()).thenReturn(1500);

        when(modeloTablaHallazgos.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaHallazgos).getColumnClass(anyInt());
        when(modeloTablaHallazgos.getColumnName(anyInt())).thenReturn("MockCol");
        when(modeloTablaHallazgos.obtenerEstadisticasVisibles()).thenReturn(new int[6]);

        when(modeloTablaTareas.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaTareas).getColumnClass(anyInt());
        when(modeloTablaTareas.getColumnName(anyInt())).thenReturn("MockCol");

        final PestaniaPrincipal[] holder = new PestaniaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PestaniaPrincipal(
            montoyaApi, estadisticas, gestorTareas, gestorConsolaGUI,
            modeloTablaTareas, modeloTablaHallazgos, true, configuracionAPI
        ));
        pestaniaPrincipal = holder[0];
    }

    @AfterEach
    void tearDown() throws Exception {
        if (pestaniaPrincipal != null) {
            SwingUtilities.invokeAndWait(() -> pestaniaPrincipal.destruir());
        }
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

            agenteHabilitado.set(false);
            pestaniaPrincipal.actualizarVisibilidadAgentes();
            assertEquals(-1, tabs.indexOfComponent(panelAgente), "El agente debe estar oculto tras deshabilitar");

            agenteHabilitado.set(true);
            pestaniaPrincipal.actualizarVisibilidadAgentes();
            assertTrue(tabs.indexOfComponent(panelAgente) >= 0, "El agente debe estar visible tras re-habilitar");

            assertDoesNotThrow(() -> panelAgente.escribirComandoCrudo("echo lifecycle"),
                "Escribir comando no debe lanzar excepcion tras ciclo de visibilidad");
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

    private void esperarTimerFocoAgente() throws InterruptedException {
        Thread.sleep(TIMER_FOCO_AGENTE_MS + 100);
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
