package com.burpia.flow;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FlowAnalysisConstraints Tests")
class FlowAnalysisConstraintsTest {

    @Test
    @DisplayName("valida rango permitido de 2 a 4 requests válidas")
    void testValidaRangoPermitido() {
        HttpRequestResponse valida1 = crearValida();
        HttpRequestResponse valida2 = crearValida();
        HttpRequestResponse valida3 = crearValida();
        HttpRequestResponse valida4 = crearValida();
        HttpRequestResponse valida5 = crearValida();

        assertFalse(FlowAnalysisConstraints.tieneMinimoValido(List.of(valida1)), "Debe rechazar menos de 2");
        assertTrue(FlowAnalysisConstraints.tieneMinimoValido(List.of(valida1, valida2)), "Debe aceptar 2");
        assertFalse(FlowAnalysisConstraints.excedeMaximoValido(List.of(valida1, valida2, valida3, valida4)), "Debe aceptar 4");
        assertTrue(FlowAnalysisConstraints.excedeMaximoValido(List.of(valida1, valida2, valida3, valida4, valida5)), "Debe rechazar más de 4");
        assertEquals(4, FlowAnalysisConstraints.contarSolicitudesValidas(List.of(valida1, valida2, valida3, valida4)),
            "Debe contar requests válidas");
    }

    private HttpRequestResponse crearValida() {
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        when(rr.request()).thenReturn(mock(HttpRequest.class));
        return rr;
    }
}
