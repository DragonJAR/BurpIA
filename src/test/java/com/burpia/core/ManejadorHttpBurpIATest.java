package com.burpia.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.ManejadorHttpBurpIA;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * CONFIABILIDAD: Establecer idioma español antes de cada test para que sean deterministas.
     * Los tests verifican mensajes localizados y deben ser consistentes sin importar
     * el idioma del sistema.
     */
    @BeforeEach
    void setUp() {
        I18nUI.establecerIdioma("es");
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
    @DisplayName("Inicializa captura según escaneoPasivoHabilitado en configuración")
    void testEstadoInicialCapturaDesdeConfiguracion() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerEscaneoPasivoHabilitado(false);

        ManejadorHttpBurpIA manejador = crearManejador(null, config);
        assertFalse(manejador.estaCapturaActiva());
    }

    @Test
    @DisplayName("Avisa con acción cuando análisis manual se bloquea por configuración LLM invalida")
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

        assertTrue(error.contains("ANÁLISIS BLOQUEADO") || error.contains("ANALISIS BLOQUEADO"));
        assertTrue(error.contains("Config") || error.contains("config"));
        assertTrue(error.contains("Probar Conexión") || error.contains("Probar Conexion")
            || error.contains("Test Connection"));
    }

    @Test
    @DisplayName("Rate-limit evita spam cuando configuración LLM sigue invalida")
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
        int ocurrencias = contarCoincidencias(error, "ANÁLISIS BLOQUEADO")
            + contarCoincidencias(error, "ANALISIS BLOQUEADO");
        assertTrue(ocurrencias <= 1);
    }

    @Test
    @DisplayName("Nota de scope incluye guia de Target > Scope")
    void testNotaScopeIncluyeGuia() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerDetallado(true); // Habilitar modo detallado para ver notas de scope
        SalidaManejador salida = crearManejadorConSalida(null, config);
        String info = salida.stdout.toString();
        assertTrue(info.contains("Target > Scope"));
    }

    @Test
    @DisplayName("Desde configuración limpia informa que LLM no esta listo al iniciar")
    void testEstadoInicialLlMNoListoConConfiguracionLimpia() {
        SalidaManejador salida = crearManejadorConSalida(null, new ConfiguracionAPI());
        String error = salida.stderr.toString();
        String info = salida.stdout.toString();

        assertTrue(error.contains("Estado LLM al inicio") || error.contains("LLM startup status"));
        assertTrue(info.contains("Probar Conexión") || info.contains("Probar Conexion")
            || info.contains("Test Connection"));
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api) {
        return crearManejador(api, null);
    }

    private ManejadorHttpBurpIA crearManejador(MontoyaApi api, ConfiguracionAPI config) {
        return crearManejadorConSalida(api, config).manejador;
    }

    private SalidaManejador crearManejadorConSalida(MontoyaApi api, ConfiguracionAPI config) {
        ConfiguracionAPI configFinal = config != null ? config : new ConfiguracionAPI();
        // CONFIABILIDAD: Solo sobrescribir detallado si no se pasó una configuración explícita
        // Esto permite que los tests controlen el modo detallado según sea necesario
        if (config == null) {
            configFinal.establecerDetallado(false);
        }
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

    /**
     * Invoca el método privado {@code esRecursoEstatico} mediante reflexión.
     * <p>
     * Se usa reflexión porque el método es de implementación interna y no está expuesto
     * en la API pública, pero contiene lógica de filtrado importante que debe probarse.
     * </p>
     *
     * @param manejador la instancia del manejador
     * @param url la URL a verificar
     * @return {@code true} si la URL corresponde a un recurso estático
     * @throws Exception si falla la invocación por reflexión
     */
    private boolean invocarEsRecursoEstatico(ManejadorHttpBurpIA manejador, String url) throws Exception {
        Method metodo = ManejadorHttpBurpIA.class.getDeclaredMethod("esRecursoEstatico", String.class);
        metodo.setAccessible(true);
        return (boolean) metodo.invoke(manejador, url);
    }

    /**
     * Invoca el método privado {@code estaEnScope} mediante reflexión.
     * <p>
     * Se usa reflexión porque el método es de implementación interna y no está expuesto
     * en la API pública, pero contiene lógica de verificación de scope importante.
     * </p>
     *
     * @param manejador la instancia del manejador
     * @param solicitud la solicitud HTTP a verificar
     * @return {@code true} si la solicitud está dentro del scope de Burp
     * @throws Exception si falla la invocación por reflexión
     */
    private boolean invocarEstaEnScope(ManejadorHttpBurpIA manejador, HttpRequest solicitud) throws Exception {
        Method metodo = ManejadorHttpBurpIA.class.getDeclaredMethod("estaEnScope", HttpRequest.class);
        metodo.setAccessible(true);
        return (boolean) metodo.invoke(manejador, solicitud);
    }

    /**
     * Cuenta el número de ocurrencias de un patrón en un texto.
     * 
     * @param texto el texto donde buscar (puede ser null o vacío)
     * @param patron el patrón a buscar (puede ser null o vacío)
     * @return el número de ocurrencias encontradas, 0 si texto o patrón están vacíos
     */
    private int contarCoincidencias(String texto, String patron) {
        // CONFIABILIDAD: Usar Normalizador.esVacio() para Strings (principio DRY)
        if (Normalizador.esVacio(texto) || Normalizador.esVacio(patron)) {
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
