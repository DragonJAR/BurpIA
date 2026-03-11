package com.burpia.model;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SolicitudAnalisis Tests")
class SolicitudAnalisisTest {

    @Test
    @DisplayName("Constructor con strings normaliza null y conserva prompt preconstruido")
    void testConstructorConStringsNormalizaNullYConservaPromptPreconstruido() {
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api",
            "POST",
            null,
            null,
            "hash-1",
            null,
            201,
            null,
            null,
            "prompt-flujo"
        );

        assertEquals("", solicitud.obtenerEncabezados(), "assertEquals failed at SolicitudAnalisisTest.java:34");
        assertEquals("", solicitud.obtenerCuerpo(), "assertEquals failed at SolicitudAnalisisTest.java:35");
        assertEquals("", solicitud.obtenerEncabezadosRespuesta(), "assertEquals failed at SolicitudAnalisisTest.java:36");
        assertEquals("", solicitud.obtenerCuerpoRespuesta(), "assertEquals failed at SolicitudAnalisisTest.java:37");
        assertEquals("prompt-flujo", solicitud.obtenerPromptPreconstruido(), "assertEquals failed at SolicitudAnalisisTest.java:38");
    }

    @Test
    @DisplayName("Extrae request y response en lazy desde objetos Montoya")
    void testExtraeRequestYResponseEnLazyDesdeObjetosMontoya() {
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);
        ByteArray requestBody = mock(ByteArray.class);
        ByteArray responseBody = mock(ByteArray.class);
        HttpHeader contentType = mock(HttpHeader.class);
        HttpHeader server = mock(HttpHeader.class);

        when(contentType.name()).thenReturn("Content-Type");
        when(contentType.value()).thenReturn("application/json");
        when(server.name()).thenReturn("Server");
        when(server.value()).thenReturn("nginx");

        when(request.method()).thenReturn("POST");
        when(request.url()).thenReturn("https://example.com/login");
        when(request.headers()).thenReturn(List.of(contentType));
        when(request.body()).thenReturn(requestBody);
        when(requestBody.length()).thenReturn(18);
        when(requestBody.toString()).thenReturn("{\"user\":\"admin\"}");

        when(response.httpVersion()).thenReturn("HTTP/1.1");
        when(response.statusCode()).thenReturn((short) 200);
        when(response.reasonPhrase()).thenReturn("OK");
        when(response.headers()).thenReturn(List.of(server));
        when(response.body()).thenReturn(responseBody);
        when(responseBody.length()).thenReturn(11);
        when(responseBody.toString()).thenReturn("{\"ok\":true}");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/login",
            "POST",
            "hash-2",
            request,
            response,
            200
        );

        String encabezados = solicitud.obtenerEncabezados();
        String cuerpo = solicitud.obtenerCuerpo();
        String encabezadosRespuesta = solicitud.obtenerEncabezadosRespuesta();
        String cuerpoRespuesta = solicitud.obtenerCuerpoRespuesta();

        assertTrue(encabezados.contains("POST https://example.com/login"),
            "assertTrue failed at SolicitudAnalisisTest.java:81");
        assertTrue(encabezados.contains("Content-Type: application/json"),
            "assertTrue failed at SolicitudAnalisisTest.java:83");
        assertEquals("{\"user\":\"admin\"}", cuerpo, "assertEquals failed at SolicitudAnalisisTest.java:85");
        assertTrue(encabezadosRespuesta.contains("HTTP/1.1 200 OK"),
            "assertTrue failed at SolicitudAnalisisTest.java:87");
        assertTrue(encabezadosRespuesta.contains("Server: nginx"),
            "assertTrue failed at SolicitudAnalisisTest.java:89");
        assertEquals("{\"ok\":true}", cuerpoRespuesta, "assertEquals failed at SolicitudAnalisisTest.java:91");
        assertNotNull(solicitud.obtenerSolicitudHttp(), "assertNotNull failed at SolicitudAnalisisTest.java:92");
    }
}
