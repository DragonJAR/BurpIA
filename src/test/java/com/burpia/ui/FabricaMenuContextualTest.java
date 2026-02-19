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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FabricaMenuContextual Tests")
class FabricaMenuContextualTest {

    @Test
    @DisplayName("Crea item de menu y dispara analisis forzado")
    void testMenuYCallback() {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);

        when(request.toString()).thenReturn("GET /test HTTP/1.1");
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));

        AtomicInteger llamadas = new AtomicInteger(0);
        FabricaMenuContextual fabrica = new FabricaMenuContextual(api, (solicitud, forzar) -> {
            if (forzar && solicitud != null) {
                llamadas.incrementAndGet();
            }
        });

        List<Component> items = fabrica.provideMenuItems(evento);
        assertFalse(items.isEmpty());
        assertEquals(1, items.size());

        JMenuItem item = (JMenuItem) items.get(0);
        item.doClick();
        assertEquals(1, llamadas.get());
    }
}
