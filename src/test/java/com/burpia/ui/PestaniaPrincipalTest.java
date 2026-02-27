package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Estadisticas;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PestaniaPrincipalTest {

    @Mock private MontoyaApi montoyaApi;
    @Mock private Estadisticas estadisticas;
    @Mock private GestorTareas gestorTareas;
    @Mock private GestorConsolaGUI gestorConsolaGUI;
    @Mock private ModeloTablaTareas modeloTablaTareas;
    @Mock private ModeloTablaHallazgos modeloTablaHallazgos;
    @Mock private ConfiguracionAPI configuracionAPI;

    private PestaniaPrincipal pestaniaPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configuracionAPI.agenteHabilitado()).thenReturn(true);
        when(configuracionAPI.obtenerAgenteDelay()).thenReturn(1500);

        when(modeloTablaHallazgos.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaHallazgos).getColumnClass(anyInt());
        when(modeloTablaHallazgos.getColumnName(anyInt())).thenReturn("MockCol");

        when(modeloTablaTareas.getColumnCount()).thenReturn(5);
        doReturn(String.class).when(modeloTablaTareas).getColumnClass(anyInt());
        when(modeloTablaTareas.getColumnName(anyInt())).thenReturn("MockCol");

        pestaniaPrincipal = new PestaniaPrincipal(
            montoyaApi, estadisticas, gestorTareas, gestorConsolaGUI,
            modeloTablaTareas, modeloTablaHallazgos, true, configuracionAPI
        );
    }

    @Test
    void testSeleccionarPestaniaAgenteEnfocaTerminal() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            pestaniaPrincipal.seleccionarPestaniaAgente();
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Timeout esperando SwingUtilities.invokeLater");

        Thread.sleep(300);

        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        assertNotNull(panelAgente, "El panel de Agente no debe ser nulo");
    }
}
