package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockConstruction;

@DisplayName("GestorMultiProveedor Tests")
@ExtendWith(MockitoExtension.class)
class GestorMultiProveedorTest {

    private static final String PROVEEDOR_OPENAI = "OpenAI";
    private static final String MODELO_OPENAI = "gpt-5-mini";

    @Mock
    private ConfiguracionAPI configMock;

    @Mock
    private GestorConsolaGUI gestorConsola;

    @Mock
    private GestorLoggingUnificado gestorLogging;

    @Mock
    private HttpRequest solicitudHttp;

    private SolicitudAnalisis solicitudMock;
    private PrintWriter stdout;
    private PrintWriter stderr;

    @BeforeEach
    void setUp() {
        solicitudMock = new SolicitudAnalisis(
            "https://test.com",
            "GET",
            "test-hash",
            solicitudHttp,
            null,
            200
        );
        stdout = new PrintWriter(OutputStream.nullOutputStream(), true);
        stderr = new PrintWriter(OutputStream.nullOutputStream(), true);
    }

    private ConfiguracionAPI crearConfiguracionValida(String proveedor, String modelo) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI(proveedor);
        config.establecerModeloParaProveedor(proveedor, modelo);
        config.establecerUrlBaseParaProveedor(proveedor, "https://api.example.com/v1");
        config.establecerApiKeyParaProveedor(proveedor, "sk-test");
        return config;
    }

    private SolicitudAnalisis crearSolicitudBasica(String url, String metodo, String hash) {
        return new SolicitudAnalisis(url, metodo, "", "", hash);
    }

    private ResultadoAnalisisMultiple invocarParsearRespuesta(GestorMultiProveedor gestor,
            String respuesta,
            String proveedor) throws Exception {
        Method metodoParseo = GestorMultiProveedor.class.getDeclaredMethod("parsearRespuesta", String.class, String.class);
        metodoParseo.setAccessible(true);
        return (ResultadoAnalisisMultiple) metodoParseo.invoke(gestor, respuesta, proveedor);
    }

    private ResultadoAnalisisMultiple invocarEtiquetarResultado(GestorMultiProveedor gestor,
            ResultadoAnalisisMultiple resultado,
            String proveedor,
            String modelo) throws Exception {
        Method metodo = GestorMultiProveedor.class.getDeclaredMethod(
            "etiquetarResultado",
            ResultadoAnalisisMultiple.class,
            String.class,
            String.class
        );
        metodo.setAccessible(true);
        return (ResultadoAnalisisMultiple) metodo.invoke(gestor, resultado, proveedor, modelo);
    }

    @Test
    @DisplayName("Validar cancelación inmediata")
    void testEjecutarAnalisisMultiProveedorCanceladoInmediatamente() {
        lenient().when(configMock.obtenerProveedoresMultiConsulta()).thenReturn(List.of("openai", "claude"));

        GestorMultiProveedor gestorCancelado = new GestorMultiProveedor(
            solicitudMock,
            configMock,
            stdout,
            stderr,
            gestorConsola,
            () -> true,
            () -> false,
            gestorLogging
        );

        assertThrows(InterruptedException.class, gestorCancelado::ejecutarAnalisisMultiProveedor,
            "La cancelación inmediata debe interrumpir el análisis multi proveedor");
    }


    @Test
    @DisplayName("Aplica delay entre proveedores aunque el primero no produzca hallazgos")
    void testAplicaDelayEntreProveedoresConPrimerResultadoVacio() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        config.establecerProveedorAI(PROVEEDOR_OPENAI);
        config.establecerProveedoresMultiConsulta(List.of(PROVEEDOR_OPENAI, "Z.ai"));
        config.establecerModeloParaProveedor("Z.ai", "glm-5");
        config.establecerApiKeyParaProveedor("Z.ai", "z-ai-key");
        config.establecerUrlBaseParaProveedor("Z.ai", "https://api.z.ai/api/paas/v4");

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PrintWriter stdoutCapturado = new PrintWriter(salida, true);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/multi", "GET", "hash-multi-delay");

        try (MockedConstruction<AnalizadorHTTP> mocked = mockConstruction(
                AnalizadorHTTP.class,
                (mock, context) -> lenient().when(mock.llamarAPI(anyString())).thenReturn("{\"hallazgos\":[]}"))) {
            GestorMultiProveedor gestor = new GestorMultiProveedor(
                solicitud,
                config,
                stdoutCapturado,
                stderr,
                null,
                () -> false,
                () -> false,
                null
            );

            gestor.ejecutarAnalisisMultiProveedor();

            assertEquals(2, mocked.constructed().size(),
                "Deben ejecutarse ambos proveedores configurados");
            assertTrue(salida.toString().contains("Esperando 2 segundos antes del siguiente proveedor"),
                "Debe aplicar delay antes del segundo proveedor aunque el primero retorne cero hallazgos");
        }
    }

    @Test
    @DisplayName("Parsea usando el formato del proveedor recibido en multi proveedor")
    void testParsearRespuestaUsaProveedorDeLaRespuesta() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        SolicitudAnalisis solicitud = crearSolicitudBasica("https://example.com/claude", "GET", "hash-multi-claude");
        GestorMultiProveedor gestor = new GestorMultiProveedor(
            solicitud,
            config,
            null,
            null,
            null,
            null,
            null,
            null
        );

        String respuestaClaude = "{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"hallazgos\\\":[{\\\"titulo\\\":\\\"Header inseguro\\\",\\\"severidad\\\":\\\"Low\\\",\\\"confianza\\\":\\\"High\\\",\\\"descripcion\\\":\\\"Falta endurecimiento\\\"}]}\"}]}";

        ResultadoAnalisisMultiple resultado = invocarParsearRespuesta(gestor, respuestaClaude, "Claude");

        assertEquals(1, resultado.obtenerNumeroHallazgos(),
            "El gestor multi proveedor debe extraer el contenido usando el proveedor que realmente respondió");
        assertEquals("Header inseguro", resultado.obtenerHallazgos().get(0).obtenerTitulo(),
            "El título debe provenir del payload con formato Claude");
    }

    @Test
    @DisplayName("Etiquetar resultado multi proveedor conserva evidencia HTTP e ID")
    void testEtiquetarResultadoConservaEvidenciaHttpEId() throws Exception {
        ConfiguracionAPI config = crearConfiguracionValida(PROVEEDOR_OPENAI, MODELO_OPENAI);
        HttpRequestResponse evidencia = org.mockito.Mockito.mock(HttpRequestResponse.class);
        Hallazgo hallazgo = new Hallazgo(
            "12:00:00",
            "https://example.com/evidencia",
            "Titulo",
            "Descripcion",
            Hallazgo.SEVERIDAD_HIGH,
            Hallazgo.CONFIANZA_ALTA,
            solicitudHttp,
            evidencia,
            "evidencia-multi"
        );
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            "https://example.com/evidencia",
            List.of(hallazgo),
            solicitudHttp,
            List.of()
        );
        GestorMultiProveedor gestor = new GestorMultiProveedor(
            solicitudMock,
            config,
            stdout,
            stderr,
            gestorConsola,
            () -> false,
            () -> false,
            gestorLogging
        );

        ResultadoAnalisisMultiple etiquetado = invocarEtiquetarResultado(
            gestor,
            resultado,
            PROVEEDOR_OPENAI,
            MODELO_OPENAI
        );

        Hallazgo hallazgoEtiquetado = etiquetado.obtenerHallazgos().get(0);
        assertEquals("evidencia-multi", hallazgoEtiquetado.obtenerEvidenciaId(),
            "El etiquetado multi proveedor debe conservar evidenciaId");
        assertSame(evidencia, hallazgoEtiquetado.obtenerEvidenciaHttp(),
            "El etiquetado multi proveedor debe conservar evidencia directa");
        assertSame(solicitudHttp, hallazgoEtiquetado.obtenerSolicitudHttp(),
            "El etiquetado multi proveedor debe conservar la solicitud HTTP");
    }
}
