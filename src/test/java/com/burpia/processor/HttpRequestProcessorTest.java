package com.burpia.processor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.scope.Scope;
import burp.api.montoya.core.ToolSource;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpRequestProcessorTest {

    @Mock
    private MontoyaApi api;

    @Mock
    private ConfiguracionAPI config;

    @Mock
    private GestorLoggingUnificado gestorLogging;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private HttpResponseReceived responseReceived;

    @Mock
    private HttpHeader header;

    @Mock
    private Scope scope;

    @Mock
    private ToolSource toolSource;

    private HttpRequestProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new HttpRequestProcessor(api, config, gestorLogging);
    }

    @Test
    void testEstaEnScope_ConRequestNull_ReturnsFalse() {
        boolean resultado = processor.estaEnScope(null);
        assertFalse(resultado);
    }

    @Test
    void testEstaEnScope_ConUrlVacia_ReturnsFalse() {
        when(request.url()).thenReturn("");
        boolean resultado = processor.estaEnScope(request);
        assertFalse(resultado);
    }

    @Test
    void testEstaEnScope_ConApiNull_ReturnsFalse() {
        when(request.url()).thenReturn("https://example.com");
        boolean resultado = processor.estaEnScope(request);
        assertFalse(resultado);
    }

    @Test
    void testEstaEnScope_ConScopeNull_ReturnsFalse() {
        when(request.url()).thenReturn("https://example.com");
        when(api.scope()).thenReturn(null);
        boolean resultado = processor.estaEnScope(request);
        assertFalse(resultado);
    }

    @Test
    void testEstaEnScope_ConUrlInScope_ReturnsTrue() {
        when(request.url()).thenReturn("https://example.com");
        when(api.scope()).thenReturn(scope);
        when(scope.isInScope(anyString())).thenReturn(true);
        boolean resultado = processor.estaEnScope(request);
        assertTrue(resultado);
    }

    @Test
    void testEstaEnScope_ConUrlOutOfScope_ReturnsFalse() {
        when(request.url()).thenReturn("https://example.com");
        when(api.scope()).thenReturn(scope);
        when(scope.isInScope(anyString())).thenReturn(false);
        boolean resultado = processor.estaEnScope(request);
        assertFalse(resultado);
    }

    @Test
    void testEstaEnScope_ConException_ReturnsFalse() {
        when(request.url()).thenReturn("https://example.com");
        when(api.scope()).thenReturn(scope);
        when(scope.isInScope(anyString())).thenThrow(new RuntimeException("Test exception"));
        boolean resultado = processor.estaEnScope(request);
        assertFalse(resultado);
        verify(gestorLogging).error(eq("HttpRequestProcessor"), anyString(), any(Exception.class));
    }

    @Test
    void testEsRecursoEstatico_ConUrlVacia_ReturnsFalse() {
        boolean resultado = processor.esRecursoEstatico("");
        assertFalse(resultado);
    }

    @Test
    void testEsRecursoEstatico_ConUrlNull_ReturnsFalse() {
        boolean resultado = processor.esRecursoEstatico(null);
        assertFalse(resultado);
    }

    @Test
    void testEsRecursoEstatico_ConExtensionEstatica_ReturnsTrue() {
        boolean resultado = processor.esRecursoEstatico("https://example.com/style.css");
        assertTrue(resultado);
    }

    @Test
    void testEsRecursoEstatico_ConExtensionNoEstatica_ReturnsFalse() {
        boolean resultado = processor.esRecursoEstatico("https://example.com/api/data");
        assertFalse(resultado);
    }

    @Test
    void testEsSoloProxy_ConResponseReceivedNull_ReturnsFalse() {
        boolean resultado = processor.esSoloProxy(null);
        assertFalse(resultado);
    }

    @Test
    void testEsSoloProxy_ConSoloProxyDesactivado_ReturnsFalse() {
        when(config.soloProxy()).thenReturn(false);
        boolean resultado = processor.esSoloProxy(responseReceived);
        assertFalse(resultado);
    }

    @Test
    void testEsSoloProxy_ConToolSourceProxy_ReturnsFalse() {
        when(config.soloProxy()).thenReturn(true);
        when(responseReceived.toolSource()).thenReturn(toolSource);
        when(toolSource.isFromTool(ToolType.PROXY)).thenReturn(true);
        boolean resultado = processor.esSoloProxy(responseReceived);
        assertFalse(resultado);
    }

    @Test
    void testEsSoloProxy_ConToolSourceNoProxy_ReturnsTrue() {
        when(config.soloProxy()).thenReturn(true);
        when(responseReceived.toolSource()).thenReturn(toolSource);
        when(toolSource.isFromTool(ToolType.PROXY)).thenReturn(false);
        boolean resultado = processor.esSoloProxy(responseReceived);
        assertTrue(resultado);
    }

    @Test
    void testEsContenidoAnalizable_ConContentTypeJson_ReturnsTrue() {
        boolean resultado = processor.esContenidoAnalizable("application/json", "GET", 200);
        assertTrue(resultado);
    }

    @Test
    void testEsContenidoAnalizable_ConContentTypeImage_ReturnsFalse() {
        boolean resultado = processor.esContenidoAnalizable("image/png", "GET", 200);
        assertFalse(resultado);
    }

    @Test
    void testEsContenidoAnalizable_ConMetodoHead_ReturnsFalse() {
        boolean resultado = processor.esContenidoAnalizable("application/json", "HEAD", 200);
        assertFalse(resultado);
    }

    @Test
    void testEsContenidoAnalizable_ConCodigo204_ReturnsFalse() {
        boolean resultado = processor.esContenidoAnalizable("application/json", "GET", 204);
        assertFalse(resultado);
    }

    @Test
    void testObtenerContentTypeRespuesta_ConResponseReceivedNull_ReturnsVacio() {
        String resultado = processor.obtenerContentTypeRespuesta(null);
        assertEquals("", resultado);
    }

    @Test
    void testObtenerContentTypeRespuesta_ConHeadersNull_ReturnsVacio() {
        when(responseReceived.headers()).thenReturn(null);
        String resultado = processor.obtenerContentTypeRespuesta(responseReceived);
        assertEquals("", resultado);
    }

    @Test
    void testObtenerContentTypeRespuesta_ConContentTypeHeader_ReturnsContentType() {
        when(header.name()).thenReturn("Content-Type");
        when(header.value()).thenReturn("application/json");
        
        List<HttpHeader> headers = Arrays.asList(header);
        when(responseReceived.headers()).thenReturn(headers);
        
        String resultado = processor.obtenerContentTypeRespuesta(responseReceived);
        assertEquals("application/json", resultado);
    }

    @Test
    void testObtenerContentTypeRespuesta_ConMultipleHeaders_ReturnsContentType() {
        HttpHeader contentTypeHeader = mock(HttpHeader.class);
        when(contentTypeHeader.name()).thenReturn("Content-Type");
        when(contentTypeHeader.value()).thenReturn("application/json");
        
        HttpHeader otherHeader = mock(HttpHeader.class);
        when(otherHeader.name()).thenReturn("Authorization");
        
        List<HttpHeader> headers = Arrays.asList(otherHeader, contentTypeHeader);
        when(responseReceived.headers()).thenReturn(headers);
        
        String resultado = processor.obtenerContentTypeRespuesta(responseReceived);
        assertEquals("application/json", resultado);
    }

    @Test
    void testEsSolicitudValida_ConResponseReceivedNull_ReturnsFalse() {
        boolean resultado = processor.esSolicitudValida(null);
        assertFalse(resultado);
        verify(gestorLogging).error(eq("HttpRequestProcessor"), eq("Respuesta recibida es null"));
    }

    @Test
    void testEsSolicitudValida_ConInitiatingRequestNull_ReturnsFalse() {
        when(responseReceived.initiatingRequest()).thenReturn(null);
        boolean resultado = processor.esSolicitudValida(responseReceived);
        assertFalse(resultado);
        verify(gestorLogging).error(eq("HttpRequestProcessor"), eq("Solicitud iniciadora es null"));
    }

    @Test
    void testEsSolicitudValida_ConResponseValida_ReturnsTrue() {
        when(responseReceived.initiatingRequest()).thenReturn(request);
        boolean resultado = processor.esSolicitudValida(responseReceived);
        assertTrue(resultado);
    }

    @Test
    void testPuedeIniciarAnalisis_ConConfiguracionValida_ReturnsTrue() {
        when(config.validarCodigoParaConsultaModelo()).thenReturn(ConfiguracionAPI.CodigoValidacionConsulta.OK);
        boolean resultado = processor.puedeIniciarAnalisis();
        assertTrue(resultado);
    }

    @Test
    void testPuedeIniciarAnalisis_ConConfiguracionInvalida_ReturnsFalse() {
        when(config.validarCodigoParaConsultaModelo()).thenReturn(ConfiguracionAPI.CodigoValidacionConsulta.API_KEY_REQUERIDA);
        boolean resultado = processor.puedeIniciarAnalisis();
        assertFalse(resultado);
    }

    @Test
    void testPuedeIniciarAnalisis_ConConfigNull_UsaConfiguracionPorDefectoValida() {
        processor = new HttpRequestProcessor(api, null, gestorLogging);
        boolean resultado = processor.puedeIniciarAnalisis();
        assertTrue(resultado);
    }

    @Test
    void testAbreviarHash_ConHashVacio_ReturnsVacio() {
        String resultado = processor.abreviarHash("");
        assertEquals("", resultado);
    }

    @Test
    void testAbreviarHash_ConHashNull_ReturnsVacio() {
        String resultado = processor.abreviarHash(null);
        assertEquals("", resultado);
    }

    @Test
    void testAbreviarHash_ConHashValido_ReturnsAbreviado() {
        String resultado = processor.abreviarHash("abcdef1234567890");
        assertEquals("abcdef12", resultado);
    }

    @Test
    void testAbreviarHash_ConHashCorto_ReturnsCompleto() {
        String resultado = processor.abreviarHash("abc");
        assertEquals("abc", resultado);
    }

    @Test
    void testConstruirEvidenciaHttp_ConRequestNull_ReturnsNull() {
        HttpRequestResponse resultado = processor.construirEvidenciaHttp(null, response);
        assertNull(resultado);
    }

    @Test
    void testConstruirEvidenciaHttp_ConResponseNull_ReturnsNull() {
        HttpRequestResponse resultado = processor.construirEvidenciaHttp(request, null);
        assertNull(resultado);
    }

    @Test
    void testNormalizarEvidenciaManual_ConSolicitudRespuestaOriginalNull_ReturnsNull() {
        HttpRequestResponse resultado = processor.normalizarEvidenciaManual(request, null);
        assertNull(resultado);
        if (gestorLogging != null) {
            verify(gestorLogging).info(eq("HttpRequestProcessor"), eq("Analisis manual sin solicitud/respuesta original: se registraran hallazgos, pero no Issue."));
        }
    }

    @Test
    void testInspeccionarSolicitudContextual_ConRequestYResponse_RetornaResumenValido() {
        HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
        ByteArray cuerpoRequest = mock(ByteArray.class);

        when(request.url()).thenReturn("https://example.com/api/login");
        when(request.method()).thenReturn("POST");
        when(request.headers()).thenReturn(List.of(header));
        when(request.body()).thenReturn(cuerpoRequest);
        when(cuerpoRequest.length()).thenReturn(0);
        when(api.scope()).thenReturn(scope);
        when(scope.isInScope("https://example.com/api/login")).thenReturn(true);

        HttpHeader contentType = mock(HttpHeader.class);
        when(contentType.name()).thenReturn("Content-Type");
        when(contentType.value()).thenReturn("application/json");
        when(response.statusCode()).thenReturn((short) 200);
        when(response.headers()).thenReturn(List.of(contentType));

        when(solicitudRespuesta.request()).thenReturn(request);
        when(solicitudRespuesta.hasResponse()).thenReturn(true);
        when(solicitudRespuesta.response()).thenReturn(response);

        HttpRequestProcessor.ResumenSolicitudContextual resumen =
            processor.inspeccionarSolicitudContextual(solicitudRespuesta);

        assertTrue(resumen.esValida());
        assertTrue(resumen.tieneRequest());
        assertTrue(resumen.tieneResponse());
        assertEquals("POST", resumen.obtenerMetodo());
        assertEquals("https://example.com/api/login", resumen.obtenerUrl());
        assertEquals(200, resumen.obtenerCodigoEstado());
        assertEquals("application/json", resumen.obtenerContentType());
        assertEquals(1, resumen.obtenerCantidadEncabezados());
        assertTrue(resumen.estaEnScope());
        assertFalse(resumen.esRecursoEstatico());
        assertEquals(Boolean.TRUE, resumen.obtenerContenidoAnalizable());
        assertFalse(processor.construirTrazasDetalleContextual(resumen).isEmpty());
    }

    @Test
    void testInspeccionarSolicitudContextual_ConRequestSinResponse_ToleraAusencia() {
        HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
        ByteArray cuerpoRequest = mock(ByteArray.class);

        when(request.url()).thenReturn("https://example.com/api/ping");
        when(request.method()).thenReturn("GET");
        when(request.headers()).thenReturn(List.of());
        when(request.body()).thenReturn(cuerpoRequest);
        when(cuerpoRequest.length()).thenReturn(0);
        when(api.scope()).thenReturn(scope);
        when(scope.isInScope("https://example.com/api/ping")).thenReturn(false);

        when(solicitudRespuesta.request()).thenReturn(request);
        when(solicitudRespuesta.hasResponse()).thenReturn(false);
        when(solicitudRespuesta.response()).thenReturn(null);

        HttpRequestProcessor.ResumenSolicitudContextual resumen =
            processor.inspeccionarSolicitudContextual(solicitudRespuesta);

        assertTrue(resumen.esValida());
        assertTrue(resumen.tieneRequest());
        assertFalse(resumen.tieneResponse());
        assertEquals(-1, resumen.obtenerCodigoEstado());
        assertEquals("", resumen.obtenerContentType());
        assertNull(resumen.obtenerContenidoAnalizable());
        List<String> trazas = processor.construirTrazasDetalleContextual(resumen);
        assertTrue(trazas.stream().anyMatch(traza -> traza.contains("sin response asociada")));
    }

    @Test
    void testInspeccionarSolicitudContextual_SinRequest_RetornaResumenInvalido() {
        HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
        when(solicitudRespuesta.request()).thenReturn(null);

        HttpRequestProcessor.ResumenSolicitudContextual resumen =
            processor.inspeccionarSolicitudContextual(solicitudRespuesta);

        assertFalse(resumen.esValida());
        assertFalse(resumen.tieneRequest());
        assertTrue(processor.construirTrazasDetalleContextual(resumen).isEmpty());
    }

    @Test
    void testContarSolicitudesSinRequestYSinResponse_Contextual() {
        HttpRequestResponse conTodo = mock(HttpRequestResponse.class);
        HttpRequestResponse sinResponse = mock(HttpRequestResponse.class);
        HttpRequestResponse sinRequest = mock(HttpRequestResponse.class);

        when(conTodo.request()).thenReturn(request);
        when(conTodo.hasResponse()).thenReturn(true);
        when(conTodo.response()).thenReturn(response);
        when(sinResponse.request()).thenReturn(request);
        when(sinResponse.hasResponse()).thenReturn(false);
        when(sinResponse.response()).thenReturn(null);
        when(sinRequest.request()).thenReturn(null);

        List<HttpRequestResponse> solicitudes = List.of(conTodo, sinResponse, sinRequest);

        assertEquals(1, processor.contarSolicitudesSinRequest(solicitudes));
        assertEquals(1, processor.contarSolicitudesSinResponse(solicitudes));
    }


}
