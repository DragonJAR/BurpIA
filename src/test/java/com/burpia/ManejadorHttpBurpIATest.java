package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
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

    @Test
    @DisplayName("Creacion es segura cuando streams de salida son nulos")
    void testCreacionConStreamsNulosNoFalla() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(false);

        assertDoesNotThrow(() -> {
            ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
                null,
                config,
                null,
                null,
                null,
                new LimitadorTasa(1)
            );
            manejadores.add(manejador);
        });
    }

    @Test
    @DisplayName("Inicializa captura segun escaneoPasivoHabilitado en configuracion")
    void testEstadoInicialCapturaDesdeConfiguracion() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerEscaneoPasivoHabilitado(false);

        ManejadorHttpBurpIA manejador = crearManejador(null, config);
        assertFalse(manejador.estaCapturaActiva());
    }

    @Test
    @DisplayName("Cuando hay GestorConsola no duplica logs en stdout")
    void testNoDuplicaLogsEnStdoutConGestorConsola() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(false);
        StringWriter salida = new StringWriter();
        PrintWriter stdout = new PrintWriter(salida, true);
        GestorConsolaGUI gestorConsola = mock(GestorConsolaGUI.class);

        ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
            null,
            config,
            null,
            stdout,
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            null,
            null,
            gestorConsola,
            null
        );
        manejadores.add(manejador);

        Method registrar = ManejadorHttpBurpIA.class.getDeclaredMethod("registrar", String.class);
        registrar.setAccessible(true);
        registrar.invoke(manejador, "mensaje de prueba");

        verify(gestorConsola, atLeastOnce()).registrar(
            eq("ManejadorBurpIA"),
            anyString(),
            eq(GestorConsolaGUI.TipoLog.INFO)
        );
        assertFalse(salida.toString().contains("[ManejadorBurpIA]"));
    }

    @Test
    @DisplayName("Cuando inicia el worker se marca tarea como Analizando")
    void testMarcaTareaAnalizandoAlIniciarWorker() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerApiKeyParaProveedor("OpenAI", "");
        config.establecerDetallado(false);

        GestorTareas gestorTareas = mock(GestorTareas.class);
        Tarea tarea = new Tarea("t-1", "Analisis HTTP", "https://example.com", Tarea.ESTADO_EN_COLA);
        when(gestorTareas.crearTarea(anyString(), anyString(), anyString(), anyString())).thenReturn(tarea);
        when(gestorTareas.marcarTareaAnalizando(eq("t-1"), anyString())).thenReturn(true);
        when(gestorTareas.estaTareaCancelada("t-1")).thenReturn(false);
        when(gestorTareas.estaTareaPausada("t-1")).thenReturn(false);

        ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
            null,
            config,
            null,
            new PrintWriter(new StringWriter(), true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            null,
            gestorTareas,
            null,
            null
        );
        manejadores.add(manejador);

        Method programar = ManejadorHttpBurpIA.class.getDeclaredMethod(
            "programarAnalisis",
            SolicitudAnalisis.class,
            burp.api.montoya.http.message.HttpRequestResponse.class,
            String.class
        );
        programar.setAccessible(true);
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "Host: example.com",
            "",
            "hash-1"
        );
        programar.invoke(manejador, solicitud, null, "Analisis HTTP");

        verify(gestorTareas, timeout(2000)).marcarTareaAnalizando(eq("t-1"), anyString());
    }

    @Test
    @DisplayName("Cancelar ejecucion activa invoca cancel sobre Future en vuelo")
    void testCancelarEjecucionActivaInvocaCancelFuture() throws Exception {
        ManejadorHttpBurpIA manejador = crearManejador(null);
        Future<?> future = mock(Future.class);
        when(future.cancel(true)).thenReturn(true);

        Field campo = ManejadorHttpBurpIA.class.getDeclaredField("ejecucionesActivas");
        campo.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Future<?>> mapa = (Map<String, Future<?>>) campo.get(manejador);
        mapa.put("task-1", future);

        manejador.cancelarEjecucionActiva("task-1");

        verify(future).cancel(true);
        assertFalse(mapa.containsKey("task-1"));
    }

    @Test
    @DisplayName("Adjunta evidencia HTTP al hallazgo cuando estaba ausente")
    void testAdjuntarEvidenciaSiDisponible() throws Exception {
        ManejadorHttpBurpIA manejador = crearManejador(null);
        Method metodo = ManejadorHttpBurpIA.class.getDeclaredMethod(
            "adjuntarEvidenciaSiDisponible",
            Hallazgo.class,
            HttpRequestResponse.class
        );
        metodo.setAccessible(true);

        Hallazgo original = new Hallazgo("https://example.com", "Detalle", "High", "High");
        HttpRequestResponse evidencia = mock(HttpRequestResponse.class);
        Hallazgo resultado = (Hallazgo) metodo.invoke(manejador, original, evidencia);

        assertTrue(resultado != null && resultado.obtenerEvidenciaHttp() == evidencia);

        Hallazgo conEvidencia = new Hallazgo(
            "https://example.com",
            "Detalle",
            "High",
            "High",
            null,
            evidencia
        );
        Hallazgo sinCambio = (Hallazgo) metodo.invoke(manejador, conEvidencia, mock(HttpRequestResponse.class));
        assertSame(conEvidencia, sinCambio);
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api) {
        return crearManejador(api, null);
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api, ConfiguracionAPI config) {
        ConfiguracionAPI configFinal = config != null ? config : new ConfiguracionAPI();
        configFinal.establecerDetallado(false);
        ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
            api,
            configFinal,
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
