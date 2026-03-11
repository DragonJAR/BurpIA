package com.burpia.flow;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FlowAnalysisRequestBuilder Tests")
class FlowAnalysisRequestBuilderTest {

    @Test
    @DisplayName("crea solicitud de flujo con prompt preconstruido y requests agregadas")
    void testCreaSolicitudFlujoConPromptPreconstruidoYRequestsAgregadas() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        SolicitudAnalisis primera = crearSolicitud(
            "https://example.com/one",
            "GET",
            "GET /one HTTP/1.1\nHost: example.com",
            "",
            200,
            "HTTP/1.1 200 OK",
            "respuesta uno"
        );
        SolicitudAnalisis segunda = crearSolicitud(
            "https://example.com/two",
            "POST",
            "POST /two HTTP/1.1\nHost: example.com",
            "{\"a\":1}",
            -1,
            "",
            ""
        );

        SolicitudAnalisis solicitudFlujo = FlowAnalysisRequestBuilder.crearSolicitudFlujo(config, List.of(primera, segunda));

        assertNotNull(solicitudFlujo, "La solicitud de flujo no debe ser null");
        assertNotNull(solicitudFlujo.obtenerPromptPreconstruido(), "El prompt preconstruido no debe ser null");
        assertTrue(solicitudFlujo.obtenerPromptPreconstruido().contains("SECUENCIA DE PETICIONES HTTP"),
            solicitudFlujo.obtenerPromptPreconstruido());
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("GET https://example.com/one"), solicitudFlujo.obtenerEncabezados());
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("POST https://example.com/two"), solicitudFlujo.obtenerEncabezados());
        assertFalse(solicitudFlujo.obtenerPromptPreconstruido().contains("RESPONSE:\nSTATUS: N/A\n[RESPONSE NOT AVAILABLE]"),
            "El prompt de flujo no debe forzar respuestas inexistentes");
    }

    private SolicitudAnalisis crearSolicitud(String url,
                                             String metodo,
                                             String encabezados,
                                             String cuerpo,
                                             int codigoRespuesta,
                                             String encabezadosRespuesta,
                                             String cuerpoRespuesta) {
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn(encabezados);
        return new SolicitudAnalisis(
            url,
            metodo,
            encabezados,
            cuerpo,
            "id-" + metodo,
            request,
            codigoRespuesta,
            encabezadosRespuesta,
            cuerpoRespuesta
        );
    }
}
