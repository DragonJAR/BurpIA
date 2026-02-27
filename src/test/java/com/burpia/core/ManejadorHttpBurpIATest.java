package com.burpia.core;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.ManejadorHttpBurpIA;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ManejadorHttpBurpIA Tests")
class ManejadorHttpBurpIATest {

    private final List<ManejadorHttpBurpIA> manejadores = new ArrayList<>();

    private static final class SalidaManejador {
        private final ManejadorHttpBurpIA manejador;
        private final StringWriter stdout;
        private final StringWriter stderr;

        private SalidaManejador(ManejadorHttpBurpIA manejador, StringWriter stdout, StringWriter stderr) {
            this.manejador = manejador;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

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
    @DisplayName("Avisa con accion cuando analisis manual se bloquea por configuracion LLM invalida")
    void testAnalisisManualBloqueadoPorConfigInvalidaMuestraAccion() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerApiKeyParaProveedor("OpenAI", "");
        SalidaManejador salida = crearManejadorConSalida(null, config);

        HttpRequest solicitud = mock(HttpRequest.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(solicitud.url()).thenReturn("https://example.com/login");
        when(solicitud.method()).thenReturn("POST");
        when(solicitud.headers()).thenReturn(List.of());
        when(solicitud.body().length()).thenReturn("username=admin".length());
        when(solicitud.body().toString()).thenReturn("username=admin");
        when(solicitud.body().subArray(anyInt(), anyInt()).toString()).thenReturn("username=admin");

        salida.manejador.analizarSolicitudForzada(solicitud);
        String error = salida.stderr.toString();

        assertTrue(error.contains("ANALISIS BLOQUEADO"));
        assertTrue(error.contains("Config") || error.contains("config"));
        assertTrue(error.contains("Probar Conexion") || error.contains("Test Connection"));
    }

    @Test
    @DisplayName("Rate-limit evita spam cuando configuracion LLM sigue invalida")
    void testRateLimitAlertaConfiguracion() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerApiKeyParaProveedor("OpenAI", "");
        SalidaManejador salida = crearManejadorConSalida(null, config);

        HttpRequest solicitud = mock(HttpRequest.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(solicitud.url()).thenReturn("https://example.com/login");
        when(solicitud.method()).thenReturn("POST");
        when(solicitud.headers()).thenReturn(List.of());
        when(solicitud.body().length()).thenReturn("username=admin".length());
        when(solicitud.body().toString()).thenReturn("username=admin");
        when(solicitud.body().subArray(anyInt(), anyInt()).toString()).thenReturn("username=admin");

        salida.manejador.analizarSolicitudForzada(solicitud);
        salida.manejador.analizarSolicitudForzada(solicitud);

        String error = salida.stderr.toString();
        int ocurrencias = contarCoincidencias(error, "ANALISIS BLOQUEADO");
        assertTrue(ocurrencias <= 1);
    }

    @Test
    @DisplayName("Nota de scope incluye guia de Target > Scope")
    void testNotaScopeIncluyeGuia() {
        SalidaManejador salida = crearManejadorConSalida(null, new ConfiguracionAPI());
        String info = salida.stdout.toString();
        assertTrue(info.contains("Target > Scope"));
    }

    @Test
    @DisplayName("Desde configuracion limpia informa que LLM no esta listo al iniciar")
    void testEstadoInicialLlMNoListoConConfiguracionLimpia() {
        SalidaManejador salida = crearManejadorConSalida(null, new ConfiguracionAPI());
        String error = salida.stderr.toString();
        String info = salida.stdout.toString();

        assertTrue(error.contains("Estado LLM al inicio") || error.contains("LLM startup status"));
        assertTrue(info.contains("Probar Conexion") || info.contains("Test Connection"));
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api) {
        return crearManejador(api, null);
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api, ConfiguracionAPI config) {
        return crearManejadorConSalida(api, config).manejador;
    }

    private SalidaManejador crearManejadorConSalida(MontoyaApi api, ConfiguracionAPI config) {
        ConfiguracionAPI configFinal = config != null ? config : new ConfiguracionAPI();
        configFinal.establecerDetallado(false);
        StringWriter stdoutBuffer = new StringWriter();
        StringWriter stderrBuffer = new StringWriter();
        ManejadorHttpBurpIA manejador = new ManejadorHttpBurpIA(
            api,
            configFinal,
            null,
            new PrintWriter(stdoutBuffer, true),
            new PrintWriter(stderrBuffer, true),
            new LimitadorTasa(1)
        );
        manejadores.add(manejador);
        return new SalidaManejador(manejador, stdoutBuffer, stderrBuffer);
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

    private int contarCoincidencias(String texto, String patron) {
        if (texto == null || patron == null || patron.isEmpty()) {
            return 0;
        }
        int contador = 0;
        int indice = 0;
        while ((indice = texto.indexOf(patron, indice)) >= 0) {
            contador++;
            indice += patron.length();
        }
        return contador;
    }
}
