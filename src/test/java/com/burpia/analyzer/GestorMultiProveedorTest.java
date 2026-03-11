package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

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

    @Test
    @DisplayName("Validar inicialización del gestor")
    void testConstructorConParametrosValidos() {
        GestorMultiProveedor gestor = new GestorMultiProveedor(
            solicitudMock,
            configMock,
            stdout,
            stderr,
            gestorConsola,
            () -> false,
            () -> false,
            gestorLogging
        );

        assertNotNull(gestor, "El gestor multi proveedor debe inicializarse con dependencias válidas");
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
}
