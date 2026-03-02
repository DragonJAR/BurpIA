package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.burpia.config.ConfiguracionAPI;





@DisplayName("FabricaMenuContextual Tests")
class FabricaMenuContextualTest {

    @Test
    @DisplayName("Crea item de menu y dispara análisis forzado")
    void testMenuYCallback() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);

        when(request.toString()).thenReturn("GET /test HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        AtomicInteger llamadas = new AtomicInteger(0);
        AtomicReference<HttpRequestResponse> evidencia = new AtomicReference<>();
        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(api, (solicitud, forzar, solicitudRespuestaOriginal) -> {
            if (forzar && solicitud != null && solicitudRespuestaOriginal != null) {
                llamadas.incrementAndGet();
                evidencia.set(solicitudRespuestaOriginal);
            }
        }, config, (rr2) -> {}, () -> {});

        List<Component> items = fabrica.provideMenuItems(evento);
        assertFalse(items.isEmpty());
        assertEquals(1, items.size());

        JMenuItem item = (JMenuItem) items.get(0);
        item.doClick();
        assertEquals(1, llamadas.get());
        assertEquals(rr, evidencia.get());
    }

    @Test
    @DisplayName("Debounce evita doble análisis sobre la misma solicitud")
    void testDebounceMismaSolicitud() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);

        when(request.toString()).thenReturn("GET /debounce HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        AtomicInteger llamadas = new AtomicInteger(0);
        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(api, (solicitud, forzar, solicitudRespuestaOriginal) -> {
            if (forzar) {
                llamadas.incrementAndGet();
            }
        }, config, (rr2) -> {}, () -> {});

        JMenuItem item = (JMenuItem) fabrica.provideMenuItems(evento).get(0);
        item.doClick();
        item.doClick();

        assertEquals(1, llamadas.get());
    }

    @Test
    @DisplayName("No falla cuando el evento llega sin selección")
    void testEventoSinSeleccion() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        when(evento.selectedRequestResponses()).thenReturn(null);

        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(api, (solicitud, forzar, solicitudRespuestaOriginal) -> {}, config, (rr2) -> {}, () -> {});
        List<Component> items = fabrica.provideMenuItems(evento);

        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("Incluye acción enviar a agente cuando está habilitado")
    void testIncluyeEnviarAgenteYEjecutaCallback() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn("GET /agent HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        when(config.agenteHabilitado()).thenReturn(true);
        when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");
        when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(false);

        AtomicInteger enviados = new AtomicInteger(0);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {},
            config,
            rr2 -> enviados.incrementAndGet(),
            () -> {}
        );

        List<Component> items = fabrica.provideMenuItems(evento);
        assertEquals(2, items.size());

        JMenuItem itemAgente = (JMenuItem) items.get(1);
        itemAgente.doClick();
        assertEquals(1, enviados.get());
    }
}
