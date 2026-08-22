package com.burpia.core;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.InvocationType;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.processor.HttpRequestProcessor;
import com.burpia.ui.FabricaMenuContextual;
import com.burpia.util.TrazadorContextual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TrazadorContextual Tests")
class TrazadorContextualTest {

    private IdiomaUI idiomaOriginal;
    private List<String> trazas;

    @BeforeEach
    void setUp() {
        idiomaOriginal = I18nUI.obtenerIdioma();
        I18nUI.establecerIdioma("es");
        trazas = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        I18nUI.establecerIdioma(idiomaOriginal);
    }

    @Test
    @DisplayName("No emite trazas cuando la configuración es null")
    void testNoEmiteConConfigNula() {
        TrazadorContextual trazador = new TrazadorContextual(() -> null, () -> null, trazas::add);

        trazador.rastrearContextual("mensaje");
        trazador.registrarBypassContextualDetallado("bypass");
        trazador.registrarResumenSeleccionContextualDetallado(List.of(mock(HttpRequestResponse.class)));

        assertTrue(trazas.isEmpty(), "Sin config no debe emitirse ninguna traza");
    }

    @Test
    @DisplayName("No emite trazas cuando el modo detallado está desactivado")
    void testNoEmiteSinModoDetallado() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(false);
        TrazadorContextual trazador = new TrazadorContextual(() -> config, () -> null, trazas::add);

        trazador.rastrearContextual("mensaje");
        trazador.registrarBypassContextualDetallado("bypass");

        assertTrue(trazas.isEmpty(), "Sin modo detallado no debe emitirse ninguna traza");
    }

    @Test
    @DisplayName("Emite trazas en modo detallado e ignora mensajes vacíos")
    void testEmiteEnModoDetallado() {
        TrazadorContextual trazador = new TrazadorContextual(this::configDetallada, () -> null, trazas::add);

        trazador.rastrearContextual("traza visible");
        trazador.rastrearContextual("");
        trazador.rastrearContextual(null);

        assertTrue(trazas.equals(List.of("traza visible")),
            "Solo el mensaje no vacío debe llegar al consumidor, got: " + trazas);
    }

    @Test
    @DisplayName("Tolerante a suppliers y consumidor nulos")
    void testToleranteADependenciasNulas() {
        TrazadorContextual trazador = new TrazadorContextual(null, null, null);
        assertDoesNotThrow(() -> {
            trazador.rastrearContextual("x");
            trazador.registrarSolicitudContextualDetallada(mock(HttpRequestResponse.class));
            trazador.registrarSolicitudesContextualesDetalladas(List.of(mock(HttpRequestResponse.class)));
        });
    }

    @Test
    @DisplayName("Inicio contextual con contexto null no lanza NPE ni emite (regresión)")
    void testInicioContextualConContextoNuloNoLanza() {
        // Regresión: la copia de ExtensionBurpIA no validaba el contexto y la
        // evaluación de ACCION_INICIADA lanzaba NPE en modo detallado, abortando
        // la acción del menú vía el catch genérico del llamador.
        TrazadorContextual trazador = new TrazadorContextual(this::configDetallada, () -> null, trazas::add);

        assertDoesNotThrow(() -> trazador.registrarInicioContextualDetallado("Acción de prueba", null));
        assertTrue(trazas.isEmpty(), "Con contexto null no debe emitirse la traza de inicio");
    }

    @Test
    @DisplayName("Inicio contextual emite la acción con contexto válido")
    void testInicioContextualEmiteConContextoValido() {
        TrazadorContextual trazador = new TrazadorContextual(this::configDetallada, () -> null, trazas::add);

        trazador.registrarInicioContextualDetallado(
            "Analizar solicitud",
            crearContextoInvocacion(InvocationType.PROXY_HISTORY, ToolType.PROXY, 1));

        assertTrue(trazas.size() == 1, "Debe emitirse exactamente una traza de inicio, got: " + trazas);
        assertTrue(trazas.get(0).contains("Acción contextual iniciada"), trazas.get(0));
        assertTrue(trazas.get(0).contains("PROXY_HISTORY"), trazas.get(0));
    }

    @Test
    @DisplayName("Resumen de selección usa el procesador inyectado y resume nulos")
    void testResumenSeleccionUsaProcesadorInyectado() {
        ConfiguracionAPI config = configDetallada();
        HttpRequestProcessor procesador = new HttpRequestProcessor(null, config, null);
        TrazadorContextual trazador = new TrazadorContextual(() -> config, () -> procesador, trazas::add);

        HttpRequestResponse sinRequest = mock(HttpRequestResponse.class);
        when(sinRequest.request()).thenReturn(null);

        trazador.registrarResumenSeleccionContextualDetallado(List.of(
            crearSolicitudRespuestaValida("https://example.com/1", "GET", ""),
            crearSolicitudRespuestaValida("https://example.com/2", "POST", "{}"),
            sinRequest));

        assertTrue(trazas.size() == 1, "Debe emitirse una única traza de resumen, got: " + trazas);
        assertTrue(trazas.get(0).contains("total=3"), trazas.get(0));
        assertTrue(trazas.get(0).contains("sin request=1"), trazas.get(0));
    }

    @Test
    @DisplayName("Solicitud contextual nula es un no-op seguro")
    void testSolicitudContextualNulaEsNoOp() {
        ConfiguracionAPI config = configDetallada();
        HttpRequestProcessor procesador = new HttpRequestProcessor(null, config, null);
        TrazadorContextual trazador = new TrazadorContextual(() -> config, () -> procesador, trazas::add);

        assertDoesNotThrow(() -> trazador.registrarSolicitudContextualDetallada(null));
        assertTrue(trazas.isEmpty(), "Una solicitud null no debe generar trazas");
    }

    @Test
    @DisplayName("Solicitud contextual válida emite las trazas construidas por el procesador")
    void testSolicitudContextualValidaEmiteTrazas() {
        ConfiguracionAPI config = configDetallada();
        HttpRequestProcessor procesador = new HttpRequestProcessor(null, config, null);
        TrazadorContextual trazador = new TrazadorContextual(() -> config, () -> procesador, trazas::add);

        HttpRequestResponse solicitud = crearSolicitudRespuestaValida("https://example.com/1", "GET", "");
        when(solicitud.hasResponse()).thenReturn(false);
        when(solicitud.response()).thenReturn(null);

        trazador.registrarSolicitudContextualDetallada(solicitud);

        String unido = String.join("\n", trazas);
        assertTrue(unido.contains("Respuesta observada"), unido);
        assertTrue(unido.contains("sin response asociada"), unido);
    }

    @Test
    @DisplayName("Bypass contextual solo se emite en modo detallado")
    void testBypassSoloEnModoDetallado() {
        ConfiguracionAPI configNormal = new ConfiguracionAPI();
        configNormal.establecerDetallado(false);
        TrazadorContextual trazadorApagado = new TrazadorContextual(() -> configNormal, () -> null, trazas::add);
        trazadorApagado.registrarBypassContextualDetallado("bypass oculto");
        assertTrue(trazas.isEmpty(), "Sin modo detallado el bypass no debe emitirse");

        TrazadorContextual trazadorActivo = new TrazadorContextual(this::configDetallada, () -> null, trazas::add);
        trazadorActivo.registrarBypassContextualDetallado("bypass visible");
        assertTrue(trazas.equals(List.of("bypass visible")), "El bypass debe emitirse tal cual, got: " + trazas);
    }

    private ConfiguracionAPI configDetallada() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(true);
        return config;
    }

    private HttpRequestResponse crearSolicitudRespuestaValida(String url, String metodo, String cuerpo) {
        HttpRequestResponse rr = mock(HttpRequestResponse.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ByteArray body = mock(ByteArray.class);
        HttpHeader header1 = mock(HttpHeader.class);
        HttpHeader header2 = mock(HttpHeader.class);
        when(header1.toString()).thenReturn(metodo + " / HTTP/1.1");
        when(header2.toString()).thenReturn("Host: example.com");
        when(rr.request().url()).thenReturn(url);
        when(rr.request().method()).thenReturn(metodo);
        when(rr.request().headers()).thenReturn(List.of(header1, header2));
        when(body.length()).thenReturn(cuerpo.length());
        when(body.toString()).thenReturn(cuerpo);
        when(rr.request().body()).thenReturn(body);
        return rr;
    }

    private FabricaMenuContextual.ContextoInvocacion crearContextoInvocacion(
            InvocationType invocationType, ToolType toolType, int cantidadSeleccionada) {
        try {
            var constructor = FabricaMenuContextual.ContextoInvocacion.class.getDeclaredConstructor(
                InvocationType.class, ToolType.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(invocationType, toolType, cantidadSeleccionada);
        } catch (Exception e) {
            throw new AssertionError("No se pudo crear ContextoInvocacion para el test", e);
        }
    }
}
