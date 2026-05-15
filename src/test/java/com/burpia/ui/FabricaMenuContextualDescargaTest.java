package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.InvocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FabricaMenuContextual Descarga Tests")
class FabricaMenuContextualDescargaTest {

    private MontoyaApi api;
    private ConfiguracionAPI config;

    @BeforeEach
    void setUp() {
        api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        config = mock(ConfiguracionAPI.class);
        when(config.agenteHabilitado()).thenReturn(false);
        when(config.hayAlgunAgenteHabilitado()).thenReturn(false);
        when(config.alertasHabilitadas()).thenReturn(true);
        when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(false);
        when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");
        when(config.obtenerTipoAgenteOperativo()).thenReturn("FACTORY_DROID");
    }

    @Test
    @DisplayName("marcarDescargado establece el flag descargado a true")
    void testMarcarDescargadoEstableceFlag() throws Exception {
        FabricaMenuContextual fabrica = crearFabricaBasica();

        Field flag = FabricaMenuContextual.class.getDeclaredField("descargado");
        flag.setAccessible(true);
        assertFalse((Boolean) flag.get(fabrica), "assertFalse failed at FabricaMenuContextualDescargaTest.java:43");

        fabrica.marcarDescargado();

        assertTrue((Boolean) flag.get(fabrica), "assertTrue failed at FabricaMenuContextualDescargaTest.java:45");
    }

    @Test
    @DisplayName("provideMenuItems retorna lista vacia despues de marcarDescargado")
    void testProvideMenuItemsRetornaListaVaciaDespuesDeDescargado() {
        FabricaMenuContextual fabrica = crearFabricaBasica();
        ContextMenuEvent evento = crearEventoConSolicitud("GET /test HTTP/1.1");

        // Before: should return items
        List<Component> itemsBefore = fabrica.provideMenuItems(evento);
        assertNotNull(itemsBefore, "assertNotNull failed at FabricaMenuContextualDescargaTest.java:59");
        assertEquals(1, itemsBefore.size(), "assertEquals failed at FabricaMenuContextualDescargaTest.java:60");

        // Mark as downloaded
        fabrica.marcarDescargado();

        // After: should return empty list
        List<Component> itemsAfter = fabrica.provideMenuItems(evento);
        assertTrue(itemsAfter.isEmpty(), "assertTrue failed at FabricaMenuContextualDescargaTest.java:64");
    }

    @Test
    @DisplayName("provideMenuItems funciona normalmente antes de marcarDescargado")
    void testProvideMenuItemsFuncionaNormalAntesDeDescargado() {
        FabricaMenuContextual fabrica = crearFabricaBasica();
        ContextMenuEvent evento = crearEventoConSolicitud("GET /normal HTTP/1.1");

        List<Component> items = fabrica.provideMenuItems(evento);

        assertNotNull(items, "assertNotNull failed at FabricaMenuContextualDescargaTest.java:75");
        assertEquals(1, items.size(), "assertEquals failed at FabricaMenuContextualDescargaTest.java:76");
        Component item = items.get(0);
        assertTrue(item instanceof JMenuItem, "assertTrue failed at FabricaMenuContextualDescargaTest.java:77");
        assertFalse(((JMenuItem) item).getText().isEmpty(),
            "assertFalse failed at FabricaMenuContextualDescargaTest.java:78");
    }

    @Test
    @DisplayName("marcarDescargado es idempotente")
    void testMarcarDescargadoEsIdempotente() {
        FabricaMenuContextual fabrica = crearFabricaBasica();
        ContextMenuEvent evento = crearEventoConSolicitud("GET /idempotente HTTP/1.1");

        // Multiple calls should not throw
        fabrica.marcarDescargado();
        fabrica.marcarDescargado();
        fabrica.marcarDescargado();

        List<Component> items = fabrica.provideMenuItems(evento);
        assertTrue(items.isEmpty(), "assertTrue failed at FabricaMenuContextualDescargaTest.java:92");

        // Also verify it was called many times without issue
        assertTrue(true, "No exception thrown after multiple marcarDescargado calls");
    }

    @Test
    @DisplayName("descargado previene creacion de menu incluso con eventos validos")
    void testDescargadoPrevieneCreacionMenuInclusoConEventosValidos() {
        FabricaMenuContextual fabrica = crearFabricaBasicaConAgentes();

        // Create a fully valid event with multiple selections (would normally produce flow items)
        HttpRequestResponse rr1 = mock(HttpRequestResponse.class);
        HttpRequestResponse rr2 = mock(HttpRequestResponse.class);
        HttpRequest request1 = mock(HttpRequest.class);
        HttpRequest request2 = mock(HttpRequest.class);
        when(request1.toString()).thenReturn("GET /first HTTP/1.1");
        when(request2.toString()).thenReturn("GET /second HTTP/1.1");
        when(rr1.request()).thenReturn(request1);
        when(rr2.request()).thenReturn(request2);

        ContextMenuEvent eventoValido = mock(ContextMenuEvent.class);
        when(eventoValido.selectedRequestResponses()).thenReturn(List.of(rr1, rr2));
        when(eventoValido.invocationType()).thenReturn(InvocationType.PROXY_HISTORY);
        when(eventoValido.toolType()).thenReturn(ToolType.PROXY);

        // Before marking downloaded: should return menu items
        List<Component> itemsBefore = fabrica.provideMenuItems(eventoValido);
        assertEquals(2, itemsBefore.size(), "assertEquals failed at FabricaMenuContextualDescargaTest.java:114");

        // Mark as downloaded
        fabrica.marcarDescargado();

        // After: even with valid event, should return empty
        List<Component> itemsAfter = fabrica.provideMenuItems(eventoValido);
        assertTrue(itemsAfter.isEmpty(), "assertTrue failed at FabricaMenuContextualDescargaTest.java:118");
    }

    // Helper methods

    private FabricaMenuContextual crearFabricaBasica() {
        return new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {},
            null,
            config,
            rr -> true,
            null,
            () -> {},
            null);
    }

    private FabricaMenuContextual crearFabricaBasicaConAgentes() {
        when(config.agenteHabilitado()).thenReturn(true);
        when(config.hayAlgunAgenteHabilitado()).thenReturn(true);
        return new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {},
            null,
            config,
            rr -> true,
            rr -> true,
            () -> {},
            null);
    }

    private ContextMenuEvent crearEventoConSolicitud(String solicitudHttp) {
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn(solicitudHttp);
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));
        when(evento.invocationType()).thenReturn(InvocationType.PROXY_HISTORY);
        when(evento.toolType()).thenReturn(ToolType.PROXY);
        return evento;
    }
}
