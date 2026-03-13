package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.ui.EstadoProveedorUI;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DialogStateManagerTest {

    @Mock
    private GestorLoggingUnificado gestorLogging;
    
    @Mock
    private ConfiguracionAPI config;
    
    @Mock
    private DialogStateManager.EstadoUIProvider uiProvider;
    
    private DialogStateManager stateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        I18nUI.establecerIdioma("es");
        stateManager = new DialogStateManager(gestorLogging);
        
        // Setup default UI provider behavior
        when(uiProvider.obtenerProveedorSeleccionado()).thenReturn("OpenAI");
        when(uiProvider.obtenerAgenteSeleccionado()).thenReturn("gemini");
        when(uiProvider.obtenerRutaBinarioAgente()).thenReturn("/path/to/binary");
        when(uiProvider.obtenerEstadoProveedorActual()).thenReturn(
            new EstadoProveedorUI("sk-key", "gpt-4", "https://api.openai.com", 4096, 120));
        when(uiProvider.obtenerProveedoresMultiSeleccionados()).thenReturn(Arrays.asList("OpenAI", "Claude"));
        when(uiProvider.obtenerCodigoIdiomaSeleccionado()).thenReturn("es");
        when(uiProvider.obtenerModeloSeleccionado()).thenReturn("gpt-4");
        when(uiProvider.obtenerUrlActual()).thenReturn("https://api.openai.com");
        when(uiProvider.obtenerApiKeyActual()).thenReturn("sk-key");
        when(uiProvider.obtenerMaxTokensTexto()).thenReturn("4096");
        when(uiProvider.obtenerTimeoutTexto()).thenReturn("120");
        when(uiProvider.obtenerRetrasoTexto()).thenReturn("0");
        when(uiProvider.obtenerMaximoConcurrenteTexto()).thenReturn("5");
        when(uiProvider.obtenerMaximoHallazgosTexto()).thenReturn("1000");
        when(uiProvider.obtenerMaximoTareasTexto()).thenReturn("100");
        when(uiProvider.esDetalladoSeleccionado()).thenReturn(true);
        when(uiProvider.esIgnorarSslSeleccionado()).thenReturn(false);
        when(uiProvider.esSoloProxySeleccionado()).thenReturn(false);
        when(uiProvider.esAlertasHabilitadasSeleccionado()).thenReturn(true);
        when(uiProvider.esPersistirBusquedaSeleccionado()).thenReturn(true);
        when(uiProvider.esPersistirSeveridadSeleccionado()).thenReturn(false);
        when(uiProvider.obtenerPromptActual()).thenReturn("Analyze {REQUEST}");
        when(uiProvider.esAgenteHabilitadoSeleccionado()).thenReturn(true);
        when(uiProvider.obtenerEstadosHabilitacionAgentesTemporales()).thenReturn(new HashMap<>());
        when(uiProvider.obtenerAgentePromptInicial()).thenReturn("Initial prompt");
        when(uiProvider.obtenerAgentePrompt()).thenReturn("Agent prompt");
        when(uiProvider.obtenerFuenteEstandar()).thenReturn("Arial");
        when(uiProvider.obtenerTamanioFuenteEstandar()).thenReturn(12);
        when(uiProvider.obtenerFuenteMono()).thenReturn("Monaco");
        when(uiProvider.obtenerTamanioFuenteMono()).thenReturn(10);
        when(uiProvider.esMultiProveedorHabilitado()).thenReturn(true);
        when(uiProvider.obtenerConfiguracion()).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    void testCapturarEstadoInicial() {
        stateManager.capturarEstadoInicial(uiProvider);
        
        verify(gestorLogging).info(eq("DialogStateManager"), contains("Estado inicial capturado"));
        assertEquals("OpenAI", stateManager.obtenerProveedorActual());
    }

    @Test
    void testCapturarEstadoActual() {
        DialogStateManager.EstadoEdicionDialogo estado = stateManager.capturarEstadoActual(uiProvider);
        
        assertNotNull(estado);
        assertEquals("OpenAI", estado.proveedorSeleccionado());
        assertEquals("gpt-4", estado.modeloSeleccionado());
        assertEquals("https://api.openai.com", estado.urlActual());
        assertEquals("sk-key", estado.apiKeyActual());
        assertEquals("4096", estado.maxTokensTexto());
        assertEquals("120", estado.timeoutTexto());
        assertTrue(estado.detallado());
        assertTrue(estado.multiProveedorHabilitado());
    }

    @Test
    void testCapturarEstadoInicialConUiProviderNuloNoLanza() {
        assertDoesNotThrow(() -> stateManager.capturarEstadoInicial(null));
        assertNull(stateManager.obtenerProveedorActual());
        verify(gestorLogging).error(eq("DialogStateManager"), eq("UIProvider es nulo"));
    }

    @Test
    void testCapturarEstadoActualConConfiguracionNula() {
        when(uiProvider.obtenerConfiguracion()).thenReturn(null);

        assertNull(stateManager.capturarEstadoActual(uiProvider));
        verify(gestorLogging).error(eq("DialogStateManager"), eq("Configuracion es nula"));
    }

    @Test
    void testHayCambiosNoGuardados_SinEstadoInicial() {
        assertFalse(stateManager.hayCambiosNoGuardados(uiProvider));
    }

    @Test
    void testHayCambiosNoGuardados_SinCambios() {
        stateManager.capturarEstadoInicial(uiProvider);
        assertFalse(stateManager.hayCambiosNoGuardados(uiProvider));
    }

    @Test
    void testHayCambiosNoGuardados_ConCambios() {
        stateManager.capturarEstadoInicial(uiProvider);
        
        when(uiProvider.obtenerModeloSeleccionado()).thenReturn("gpt-3.5-turbo");
        
        assertTrue(stateManager.hayCambiosNoGuardados(uiProvider));
    }

    @Test
    void testHayCambiosNoGuardados_CuandoCambiaHabilitacionAgenteSeleccionado() {
        Map<String, Boolean> estadosIniciales = new HashMap<>();
        estadosIniciales.put("gemini", true);
        when(uiProvider.obtenerEstadosHabilitacionAgentesTemporales()).thenReturn(estadosIniciales);
        stateManager.capturarEstadoInicial(uiProvider);

        Map<String, Boolean> estadosActualizados = new HashMap<>();
        estadosActualizados.put("gemini", false);
        when(uiProvider.obtenerEstadosHabilitacionAgentesTemporales()).thenReturn(estadosActualizados);
        when(uiProvider.esAgenteHabilitadoSeleccionado()).thenReturn(false);

        assertTrue(stateManager.hayCambiosNoGuardados(uiProvider),
            "assertTrue failed at DialogStateManagerTest.java:110");
    }

    @Test
    void testObtenerCambiosDetectados() {
        stateManager.capturarEstadoInicial(uiProvider);
        
        when(uiProvider.obtenerModeloSeleccionado()).thenReturn("gpt-3.5-turbo");
        when(uiProvider.esDetalladoSeleccionado()).thenReturn(false);
        
        List<String> cambios = stateManager.obtenerCambiosDetectados(uiProvider);
        
        assertEquals(2, cambios.size());
        assertTrue(cambios.contains(I18nUI.Configuracion.CAMBIO_MODELO()));
        assertTrue(cambios.contains(I18nUI.Configuracion.CAMBIO_MODO_DETALLADO()));
    }

    @Test
    void testObtenerCambiosDetectadosRespetaIdiomaActual() {
        I18nUI.establecerIdioma("en");
        stateManager.capturarEstadoInicial(uiProvider);

        when(uiProvider.obtenerModeloSeleccionado()).thenReturn("gpt-3.5-turbo");
        when(uiProvider.esDetalladoSeleccionado()).thenReturn(false);

        List<String> cambios = stateManager.obtenerCambiosDetectados(uiProvider);

        assertTrue(cambios.contains("Model"));
        assertTrue(cambios.contains("Verbose mode"));
    }

    @Test
    void testGestionarCambioProveedor() {
        stateManager.gestionarCambioProveedor("Claude", uiProvider);
        
        verify(uiProvider).actualizarProveedorEnUI("Claude");
        verify(gestorLogging).info(eq("DialogStateManager"), contains("Cambiando proveedor"));
    }

    @Test
    void testGestionarCambioProveedor_MismoProveedor() {
        stateManager.capturarEstadoInicial(uiProvider);
        stateManager.gestionarCambioProveedor("OpenAI", uiProvider);
        
        verify(uiProvider, never()).actualizarProveedorEnUI(anyString());
    }

    @Test
    void testGuardarEstadoTemporalProveedor() {
        EstadoProveedorUI estado = new EstadoProveedorUI("sk-key", "claude-3", "https://api.anthropic.com", 8192, 180);
        
        stateManager.guardarEstadoTemporalProveedor("Claude", estado);
        
        Map<String, EstadoProveedorUI> estadosTemporales = stateManager.obtenerEstadoProveedorTemporal();
        assertTrue(estadosTemporales.containsKey("Claude"));
        assertEquals(estado, estadosTemporales.get("Claude"));
        
        verify(gestorLogging).info(eq("DialogStateManager"), contains("Estado temporal guardado"));
    }

    @Test
    void testGuardarRutaBinarioAgente() {
        stateManager.guardarRutaBinarioAgente("gemini", "/usr/local/bin/gemini");
        
        Map<String, String> rutas = stateManager.obtenerRutasBinarioAgenteTemporal();
        assertTrue(rutas.containsKey("gemini"));
        assertEquals("/usr/local/bin/gemini", rutas.get("gemini"));
        
        verify(gestorLogging).info(eq("DialogStateManager"), contains("Ruta binaria guardada"));
    }

    @Test
    void testLimpiarEstadoTemporal() {
        stateManager.guardarEstadoTemporalProveedor("Claude", new EstadoProveedorUI("sk-key", "claude-3", "https://api.anthropic.com", 8192, 180));
        stateManager.guardarRutaBinarioAgente("gemini", "/usr/local/bin/gemini");
        
        assertFalse(stateManager.obtenerEstadoProveedorTemporal().isEmpty());
        assertFalse(stateManager.obtenerRutasBinarioAgenteTemporal().isEmpty());
        
        stateManager.limpiarEstadoTemporal();
        
        assertTrue(stateManager.obtenerEstadoProveedorTemporal().isEmpty());
        assertTrue(stateManager.obtenerRutasBinarioAgenteTemporal().isEmpty());
        
        verify(gestorLogging).info(eq("DialogStateManager"), contains("Estado temporal limpiado"));
    }

    @Test
    void testEstadoEdicionDialogo_EqualsAndHashCode() {
        DialogStateManager.EstadoEdicionDialogo estado1 = new DialogStateManager.EstadoEdicionDialogo(
            "es", "OpenAI", "gpt-4", "https://api.openai.com", "sk-key",
            "4096", "120", "0", "5", "1000", "100",
            true, false, false, true, true, false,
            "Analyze {REQUEST}", true, "gemini", "Initial", "Agent",
            "Arial", 12, "Monaco", 10, true, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()
        );
        
        DialogStateManager.EstadoEdicionDialogo estado2 = new DialogStateManager.EstadoEdicionDialogo(
            "es", "OpenAI", "gpt-4", "https://api.openai.com", "sk-key",
            "4096", "120", "0", "5", "1000", "100",
            true, false, false, true, true, false,
            "Analyze {REQUEST}", true, "gemini", "Initial", "Agent",
            "Arial", 12, "Monaco", 10, true, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()
        );
        
        DialogStateManager.EstadoEdicionDialogo estado3 = new DialogStateManager.EstadoEdicionDialogo(
            "en", "OpenAI", "gpt-4", "https://api.openai.com", "sk-key",
            "4096", "120", "0", "5", "1000", "100",
            true, false, false, true, true, false,
            "Analyze {REQUEST}", true, "gemini", "Initial", "Agent",
            "Arial", 12, "Monaco", 10, true, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()
        );
        
        assertEquals(estado1, estado2);
        assertEquals(estado1.hashCode(), estado2.hashCode());
        assertNotEquals(estado1, estado3);
    }

    @Test
    void testEstadoProveedorSnapshot_EqualsAndHashCode() {
        DialogStateManager.EstadoProveedorSnapshot snapshot1 = new DialogStateManager.EstadoProveedorSnapshot(
            "sk-key", "gpt-4", "https://api.openai.com", 4096, 120
        );
        
        DialogStateManager.EstadoProveedorSnapshot snapshot2 = new DialogStateManager.EstadoProveedorSnapshot(
            "sk-key", "gpt-4", "https://api.openai.com", 4096, 120
        );
        
        DialogStateManager.EstadoProveedorSnapshot snapshot3 = new DialogStateManager.EstadoProveedorSnapshot(
            "sk-key", "gpt-3.5-turbo", "https://api.openai.com", 4096, 120
        );
        
        assertEquals(snapshot1, snapshot2);
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode());
        assertNotEquals(snapshot1, snapshot3);
    }

    @Test
    void testUIProviderNullHandling() {
        assertNull(stateManager.capturarEstadoActual(null));
        assertFalse(stateManager.hayCambiosNoGuardados(null));
        assertTrue(stateManager.obtenerCambiosDetectados(null).isEmpty());
        
        verify(gestorLogging).error(eq("DialogStateManager"), eq("UIProvider es nulo"));
    }
}
