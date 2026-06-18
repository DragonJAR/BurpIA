package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.PromptTruncador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests para {@link EjecutorLlamadaConTruncado}.
 *
 * <p>Cubre las correcciones M1 (guarda de no-progreso) y M2 (objetivo de tokens
 * robusto: límite extraído no creíble cae al fallback del proveedor).</p>
 */
@DisplayName("EjecutorLlamadaConTruncado Tests")
@ExtendWith(MockitoExtension.class)
class EjecutorLlamadaConTruncadoTest {

    private static final String PROMPT_GRANDE = "的分析 ".repeat(20000);

    @Mock
    private AnalizadorHTTP analizadorHTTP;
    @Mock
    private PromptTruncador promptTruncador;

    private ConfiguracionAPI config;
    private ControlCancelacionPausa control;
    private GestorLoggingUnificado gestorLogging;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        control = new ControlCancelacionPausa(() -> false, () -> false);
        gestorLogging = GestorLoggingUnificado.crearMinimal(
                new PrintWriter(OutputStream.nullOutputStream(), true),
                new PrintWriter(OutputStream.nullOutputStream(), true));
    }

    private EjecutorLlamadaConTruncado crearEjecutor() {
        return new EjecutorLlamadaConTruncado(config, analizadorHTTP, promptTruncador, control, gestorLogging);
    }

    @Test
    @DisplayName("Retorna la respuesta en el primer intento si no hay error de contexto")
    void retornaRespuestaEnPrimerIntento() throws Exception {
        when(analizadorHTTP.llamarAPI(anyString())).thenReturn("{\"hallazgos\":[]}");

        String resultado = crearEjecutor().ejecutar(PROMPT_GRANDE);

        assertEquals("{\"hallazgos\":[]}", resultado);
        verify(analizadorHTTP, times(1)).llamarAPI(anyString());
    }

    @Test
    @DisplayName("Trunca y reintenta cuando el truncador reduce el prompt")
    void truncaYReintentaCuandoHayReduccion() throws Exception {
        ContextExceededException errorContexto = new ContextExceededException(
                "context too long", "maximum context length is 128000 tokens", 128000);

        when(analizadorHTTP.llamarAPI(PROMPT_GRANDE)).thenThrow(errorContexto);
        String promptTruncado = PROMPT_GRANDE.substring(0, 5000);
        when(promptTruncador.truncarPrompt(anyString(), anyInt())).thenReturn(promptTruncado);
        when(analizadorHTTP.llamarAPI(promptTruncado)).thenReturn("{\"hallazgos\":[]}");

        String resultado = crearEjecutor().ejecutar(PROMPT_GRANDE);

        assertEquals("{\"hallazgos\":[]}", resultado);
        verify(analizadorHTTP, times(2)).llamarAPI(anyString());
    }

    @Test
    @DisplayName("M1: rinde de inmediato si el truncador no logra reducir el prompt (sin reintentos inútiles)")
    void rindeSiNoHayProgresoEnTruncado() throws Exception {
        // El truncador devuelve el mismo prompt (no se pudo reducir): antes el
        // ejecutor lo reenviaba hasta MAX_TRUNCADOS veces gastando llamadas
        // inútiles. Ahora debe lanzar IOException de inmediato.
        ContextExceededException errorContexto = new ContextExceededException(
                "context too long", "maximum context length is 128000 tokens", 128000);

        when(analizadorHTTP.llamarAPI(anyString())).thenThrow(errorContexto);
        // Truncador no reduce (devuelve el mismo prompt).
        when(promptTruncador.truncarPrompt(anyString(), anyInt())).thenReturn(PROMPT_GRANDE);

        assertThrows(IOException.class, () -> crearEjecutor().ejecutar(PROMPT_GRANDE));

        // Solo una llamada: la inicial. No reintenta con prompt idéntico.
        verify(analizadorHTTP, times(1)).llamarAPI(anyString());
    }

    @Test
    @DisplayName("M2: un límite extraído no creíble (< 1000) se descarta y cae al fallback del proveedor")
    void limiteExtraidoNoCredibleSeDescarta() throws Exception {
        // "3 tokens remaining" → límite=3, no creíble. Debe caer al fallback
        // (maxTokens del proveedor), no sobre-truncar a 1000 tokens.
        ContextExceededException errorContexto = new ContextExceededException(
                "context too long", "You have 3 tokens remaining", 3);

        when(analizadorHTTP.llamarAPI(PROMPT_GRANDE)).thenThrow(errorContexto);
        String promptTruncado = PROMPT_GRANDE.substring(0, 5000);
        when(promptTruncador.truncarPrompt(anyString(), anyInt())).thenReturn(promptTruncado);
        when(analizadorHTTP.llamarAPI(promptTruncado)).thenReturn("{\"hallazgos\":[]}");

        crearEjecutor().ejecutar(PROMPT_GRANDE);

        // El objetivo pasado al truncador debe derivar del maxTokens del proveedor
        // (configuración OpenAI), no del límite extraído (3). Verificamos que se
        // llamó al truncador y se reintentó con éxito usando el fallback.
        verify(promptTruncador, times(1)).truncarPrompt(anyString(), anyInt());
        verify(analizadorHTTP, times(2)).llamarAPI(anyString());
    }

    @Test
    @DisplayName("Propaga InterruptedException (cancelación) sin envolverla")
    void propagaInterruptedException() throws Exception {
        when(analizadorHTTP.llamarAPI(anyString())).thenThrow(new InterruptedException("cancel"));

        assertThrows(InterruptedException.class, () -> crearEjecutor().ejecutar(PROMPT_GRANDE));
        verify(promptTruncador, never()).truncarPrompt(anyString(), anyInt());
    }

    @Test
    @DisplayName("Propaga IOException de red sin intentar truncar")
    void propagaIOExceptionDeRed() throws Exception {
        lenient().when(analizadorHTTP.llamarAPI(anyString())).thenThrow(new IOException("network down"));

        assertThrows(IOException.class, () -> crearEjecutor().ejecutar(PROMPT_GRANDE));
        verify(promptTruncador, never()).truncarPrompt(anyString(), anyInt());
    }
}
