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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        }, config, rr2 -> true, () -> {});

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
        }, config, rr2 -> true, () -> {});

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
        FabricaMenuContextual fabrica = new FabricaMenuContextual(api, (solicitud, forzar, solicitudRespuestaOriginal) -> {}, config, rr2 -> true, () -> {});
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
            rr2 -> {
                enviados.incrementAndGet();
                return true;
            },
            () -> {}
        );

        List<Component> items = fabrica.provideMenuItems(evento);
        assertEquals(2, items.size());

        JMenuItem itemAgente = (JMenuItem) items.get(1);
        itemAgente.doClick();
        assertEquals(1, enviados.get());
    }

    @Test
    @DisplayName("Analisis forzado funciona cuando alertas de contexto estan deshabilitadas")
    void testAnalisisForzadoConAlertasDeshabilitadas() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn("GET /force HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        when(config.agenteHabilitado()).thenReturn(false);
        when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(false);

        AtomicInteger analisis = new AtomicInteger(0);
        AtomicInteger guardados = new AtomicInteger(0);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {
                if (forzar && solicitud != null && solicitudRespuestaOriginal != null) {
                    analisis.incrementAndGet();
                }
            },
            config,
            rr2 -> true,
            guardados::incrementAndGet
        );

        JMenuItem itemAnalizar = (JMenuItem) fabrica.provideMenuItems(evento).get(0);
        itemAnalizar.doClick();

        assertEquals(1, analisis.get());
        assertEquals(0, guardados.get());
        verify(config, never()).establecerAlertasClickDerechoEnviarAHabilitadas(false);
    }

    @Test
    @DisplayName("Procesa múltiples solicitudes al enviar a agente desde menú contextual")
    void testEnvioAgenteProcesaSeleccionMultiple() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr1 = mock(HttpRequestResponse.class);
        HttpRequestResponse rr2 = mock(HttpRequestResponse.class);
        HttpRequest request1 = mock(HttpRequest.class);
        HttpRequest request2 = mock(HttpRequest.class);
        when(request1.toString()).thenReturn("GET /m1 HTTP/1.1");
        when(request2.toString()).thenReturn("GET /m2 HTTP/1.1");
        when(rr1.request()).thenReturn(request1);
        when(rr2.request()).thenReturn(request2);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr1, rr2));

        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        when(config.agenteHabilitado()).thenReturn(true);
        when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");
        when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(false);

        AtomicInteger enviados = new AtomicInteger(0);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {},
            config,
            rr -> {
                enviados.incrementAndGet();
                return true;
            },
            () -> {}
        );

        List<Component> items = fabrica.provideMenuItems(evento);
        JMenuItem itemAgente = (JMenuItem) items.get(1);
        itemAgente.doClick();

        assertEquals(2, enviados.get());
    }

    @Test
    @DisplayName("Config nula no rompe menú contextual y omite acción de agente")
    void testConfigNulaNoRompeMenuContextual() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn("GET /single HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        AtomicInteger analisis = new AtomicInteger(0);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> analisis.incrementAndGet(),
            null,
            rr2 -> true,
            () -> {}
        );

        List<Component> items = fabrica.provideMenuItems(evento);
        assertEquals(1, items.size());
        ((JMenuItem) items.get(0)).doClick();
        assertEquals(1, analisis.get());
    }
}
