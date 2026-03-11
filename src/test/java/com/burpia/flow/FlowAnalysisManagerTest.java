package com.burpia.flow;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FlowAnalysisManager Tests")
class FlowAnalysisManagerTest {

    @Test
    @DisplayName("crearSolicitudFlujo tolera respuestas faltantes y mantiene requests")
    void testCrearSolicitudFlujoToleraRespuestasFaltantesYMantieneRequests() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ConfiguracionAPI config = new ConfiguracionAPI();
        GestorLoggingUnificado gestorLogging = mock(GestorLoggingUnificado.class);
        FlowAnalysisManager manager = new FlowAnalysisManager(api, config, gestorLogging, new LimitadorTasa(1), null, null);

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

        Method metodo = FlowAnalysisManager.class.getDeclaredMethod("crearSolicitudFlujo", List.class);
        metodo.setAccessible(true);
        Object resultado = assertDoesNotThrow(() -> metodo.invoke(manager, List.of(primera, segunda)));
        SolicitudAnalisis solicitudFlujo = (SolicitudAnalisis) resultado;

        assertNotNull(solicitudFlujo, "La solicitud de flujo no debe ser null");
        assertNotNull(solicitudFlujo.obtenerEncabezados(), "Los encabezados del flujo no deben ser null");
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("GET https://example.com/one"), solicitudFlujo.obtenerEncabezados());
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("POST https://example.com/two"), solicitudFlujo.obtenerEncabezados());
        assertFalse(solicitudFlujo.obtenerEncabezados().contains("RESPONSE:\nSTATUS: N/A\n[RESPONSE NOT AVAILABLE]"),
            "El prompt del flujo no debe inyectar respuestas faltantes fuera de su formato normal");
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
