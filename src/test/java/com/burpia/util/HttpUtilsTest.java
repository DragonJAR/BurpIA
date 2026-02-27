package com.burpia.util;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class HttpUtilsTest {

    @Test
    void generarHash_nullOrEmptyInput() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HttpUtils.generarHash(null));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HttpUtils.generarHash(new byte[0]));
    }

    @Test
    void generarHash_validInput() {
        String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertEquals(expectedHash, HttpUtils.generarHash("hello".getBytes()));
    }

    @Test
    void generarHashPartes_nullOrEmpty() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HttpUtils.generarHashPartes((String[]) null));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HttpUtils.generarHashPartes());
    }

    @Test
    void generarHashPartes_validInput() {
        assertNotNull(HttpUtils.generarHashPartes("part1", "part2", null, ""));
    }

    @Test
    void extraerEncabezadosRequest_null() {
        assertEquals("[SOLICITUD NULL]", HttpUtils.extraerEncabezados((HttpRequest) null));
    }

    @Test
    void extraerEncabezadosRequest_valid() {
        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.method()).thenReturn("GET");
        when(request.url()).thenReturn("http://example.com");

        HttpHeader header1 = Mockito.mock(HttpHeader.class);
        when(header1.name()).thenReturn("Host");
        when(header1.value()).thenReturn("example.com");

        HttpHeader header2 = Mockito.mock(HttpHeader.class);
        when(header2.name()).thenReturn(null);
        when(header2.value()).thenReturn(null);

        when(request.headers()).thenReturn(Arrays.asList(header1, header2, null));

        String headers = HttpUtils.extraerEncabezados(request);
        assertTrue(headers.contains("GET http://example.com"));
        assertTrue(headers.contains("Host: example.com"));
        assertTrue(headers.contains("[NAME NULL]: [VALUE NULL]"));
    }

    @Test
    void extraerEncabezadosRequest_nullFields() {
        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.method()).thenReturn(null);
        when(request.url()).thenReturn(null);
        when(request.headers()).thenReturn(null);
        String headers = HttpUtils.extraerEncabezados(request);
        assertTrue(headers.contains("[METHOD NULL] [URL NULL]"));
        assertTrue(headers.contains("[HEADERS NULL]"));
    }

    @Test
    void extraerEncabezadosResponse_null() {
        assertEquals("[RESPUESTA NULL]", HttpUtils.extraerEncabezados((HttpResponse) null));
    }

    @Test
    void extraerEncabezadosResponse_valid() {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        when(response.httpVersion()).thenReturn("HTTP/2");
        when(response.statusCode()).thenReturn((short) 200);
        when(response.reasonPhrase()).thenReturn("OK");

        HttpHeader header = Mockito.mock(HttpHeader.class);
        when(header.name()).thenReturn("Content-Type");
        when(header.value()).thenReturn("text/html");

        when(response.headers()).thenReturn(Collections.singletonList(header));

        String headers = HttpUtils.extraerEncabezados(response);
        assertTrue(headers.contains("HTTP/2 200 OK"));
        assertTrue(headers.contains("Content-Type: text/html"));
    }

    @Test
    void extraerEncabezadosResponse_nullFields() {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        when(response.httpVersion()).thenReturn(null);
        when(response.statusCode()).thenReturn((short) 500);
        when(response.reasonPhrase()).thenReturn(" ");
        when(response.headers()).thenReturn(null);

        String headers = HttpUtils.extraerEncabezados(response);
        assertTrue(headers.contains("HTTP/1.1 500"));
        assertTrue(headers.contains("[HEADERS NULL]"));
    }

    @Test
    void extraerCuerpoRequest_null() {
        assertEquals("", HttpUtils.extraerCuerpo((HttpRequest) null));
    }

    @Test
    void extraerCuerpoRequest_valid() {
        HttpRequest request = Mockito.mock(HttpRequest.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.body().length()).thenReturn("body content".length());
        when(request.body().toString()).thenReturn("body content");
        assertEquals("body content", HttpUtils.extraerCuerpo(request));
    }

    @Test
    void extraerCuerpoRequest_exception() {
        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.body()).thenThrow(new RuntimeException("Error"));
        assertEquals("", HttpUtils.extraerCuerpo(request));
    }

    @Test
    void extraerCuerpoResponse_null() {
        assertEquals("", HttpUtils.extraerCuerpo((HttpResponse) null));
    }

    @Test
    void extraerCuerpoResponse_valid() {
        HttpResponse response = Mockito.mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(response.body().length()).thenReturn("response body".length());
        when(response.body().toString()).thenReturn("response body");
        assertEquals("response body", HttpUtils.extraerCuerpo(response));
    }

    @Test
    void extraerCuerpoResponse_exception() {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        when(response.body()).thenThrow(new RuntimeException("Error"));
        assertEquals("", HttpUtils.extraerCuerpo(response));
    }

    @Test
    void esRecursoEstatico_validInput() {
        assertTrue(HttpUtils.esRecursoEstatico("http://example.com/style.css"));
        assertTrue(HttpUtils.esRecursoEstatico("http://example.com/image.PNG"));
        assertTrue(HttpUtils.esRecursoEstatico("http://example.com/script.js?v=1.2"));
        assertTrue(HttpUtils.esRecursoEstatico("http://example.com/font.woff2#anchor"));

        assertFalse(HttpUtils.esRecursoEstatico("http://example.com/"));
        assertFalse(HttpUtils.esRecursoEstatico("http://example.com/api/data"));
        assertFalse(HttpUtils.esRecursoEstatico("http://example.com/page.html"));
        assertFalse(HttpUtils.esRecursoEstatico(null));
        assertFalse(HttpUtils.esRecursoEstatico(""));
    }
}
