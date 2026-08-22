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
    @DisplayName("crea solicitud de flujo usando el prompt configurable del usuario")
    void testCreaSolicitudFlujoUsandoPromptConfigurableUsuario() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerPromptConfigurable("PROMPT USUARIO\nREQ={REQUEST}\nRES={RESPONSE}\nLANG={OUTPUT_LANGUAGE}");
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
        assertTrue(solicitudFlujo.obtenerPromptPreconstruido().startsWith("PROMPT USUARIO"),
            solicitudFlujo.obtenerPromptPreconstruido());
        assertTrue(solicitudFlujo.obtenerPromptPreconstruido().contains("=== REQUEST 1 ==="),
            solicitudFlujo.obtenerPromptPreconstruido());
        assertTrue(solicitudFlujo.obtenerPromptPreconstruido().contains("=== REQUEST 2 ==="),
            solicitudFlujo.obtenerPromptPreconstruido());
        assertTrue(solicitudFlujo.obtenerPromptPreconstruido().contains("=== RESPONSE 1 ==="),
            solicitudFlujo.obtenerPromptPreconstruido());
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("GET https://example.com/one"), solicitudFlujo.obtenerEncabezados());
        assertTrue(solicitudFlujo.obtenerEncabezados().contains("POST https://example.com/two"), solicitudFlujo.obtenerEncabezados());
        assertFalse(solicitudFlujo.obtenerPromptPreconstruido().contains("SECUENCIA DE PETICIONES HTTP"),
            "No debe reconstruir un prompt de flujo separado");
        assertFalse(solicitudFlujo.obtenerPromptPreconstruido().contains("RESPONSE:\nSTATUS: N/A\n[RESPONSE NOT AVAILABLE]"),
            "El prompt de flujo no debe forzar respuestas inexistentes");
        assertFalse(solicitudFlujo.obtenerPromptPreconstruido().contains("=== RESPONSE 2 ==="),
            "El prompt de flujo no debe inventar respuestas faltantes");
    }

    @Test
    @DisplayName("La numeracion de peticiones no deja huecos cuando hay nulls en la lista")
    void testNumeracionSinHuecosConNulls() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        SolicitudAnalisis primera = crearSolicitud(
            "https://example.com/one", "GET", "GET /one HTTP/1.1\nHost: example.com", "", 200, "HTTP/1.1 200 OK", "r1");
        SolicitudAnalisis tercera = crearSolicitud(
            "https://example.com/three", "POST", "POST /three HTTP/1.1\nHost: example.com", "{\"b\":2}", -1, "", "");

        SolicitudAnalisis solicitudFlujo = FlowAnalysisRequestBuilder.crearSolicitudFlujo(
            config, java.util.Arrays.asList(primera, null, tercera));

        assertNotNull(solicitudFlujo, "La solicitud de flujo no debe ser null con nulls intercalados");
        String encabezados = solicitudFlujo.obtenerEncabezados();
        assertTrue(encabezados.contains("[PETICIÓN 1]"), "La primera petición válida debe numerarse 1");
        assertTrue(encabezados.contains("[PETICIÓN 2]"), "La segunda petición válida debe numerarse 2 (sin hueco por el null)");
        assertFalse(encabezados.contains("[PETICIÓN 3]"), "Solo hay 2 peticiones válidas: no debe existir [PETICIÓN 3]");
        assertTrue(solicitudFlujo.obtenerCuerpo().contains("[BODY PETICIÓN 2]"),
            "El body de la segunda petición válida debe usar la misma numeración sin huecos");
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
