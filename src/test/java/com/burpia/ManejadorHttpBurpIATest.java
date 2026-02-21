package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ManejadorHttpBurpIA Tests")
class ManejadorHttpBurpIATest {

    private final List<ManejadorHttpBurpIA> manejadores = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (ManejadorHttpBurpIA manejador : manejadores) {
            manejador.shutdown();
        }
        manejadores.clear();
    }

    @Test
    @DisplayName("Detecta recursos estaticos con mayusculas y query params")
    void testDetectaRecursosEstaticosRobusto() throws Exception {
        ManejadorHttpBurpIA manejador = crearManejador(null);

        assertTrue(invocarEsRecursoEstatico(manejador, "https://example.com/assets/app.JS?v=123"));
        assertTrue(invocarEsRecursoEstatico(manejador, "https://example.com/img/logo.PNG#v2"));
        assertFalse(invocarEsRecursoEstatico(manejador, "https://example.com/api/users"));
    }

    @Test
    @DisplayName("Falla cerrado cuando scope API no esta disponible")
    void testScopeSinApi() throws Exception {
        ManejadorHttpBurpIA manejador = crearManejador(null);
        HttpRequest solicitud = mock(HttpRequest.class);
        when(solicitud.url()).thenReturn("https://example.com");

        assertFalse(invocarEstaEnScope(manejador, solicitud));
    }

    @Test
    @DisplayName("Falla cerrado cuando API de scope lanza error")
    void testScopeConExcepcion() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(api.scope().isInScope("https://example.com")).thenThrow(new RuntimeException("boom"));

        ManejadorHttpBurpIA manejador = crearManejador(api);
        HttpRequest solicitud = mock(HttpRequest.class);
        when(solicitud.url()).thenReturn("https://example.com");

        assertFalse(invocarEstaEnScope(manejador, solicitud));
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(false);

        ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
            api,
            config,
            null,
            new PrintWriter(new StringWriter(), true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1)
        );
        manejadores.add(manejador);
        return manejador;
    }

    private boolean invocarEsRecursoEstatico(ManejadorHttpBurpIA manejador, String url) throws Exception {
        Method metodo = ManejadorHttpBurpIA.class.getDeclaredMethod("esRecursoEstatico", String.class);
        metodo.setAccessible(true);
        return (boolean) metodo.invoke(manejador, url);
    }

    private boolean invocarEstaEnScope(ManejadorHttpBurpIA manejador, HttpRequest solicitud) throws Exception {
        Method metodo = ManejadorHttpBurpIA.class.getDeclaredMethod("estaEnScope", HttpRequest.class);
        metodo.setAccessible(true);
        return (boolean) metodo.invoke(manejador, solicitud);
    }
}
