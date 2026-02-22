package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("AnalizadorAI Tests")
class AnalizadorAITest {

    @Test
    @DisplayName("Parseo de hallazgos tolera elementos JSON invalidos sin romper")
    void testParseoHallazgosInvalidosNoRompe() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("Z.ai");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        AnalizadorAI analizador = new AnalizadorAI(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                }

                @Override
                public void alErrorAnalisis(String error) {
                }
            }
        );

        String respuesta = "{\"hallazgos\":[123,{\"descripcion\":\"SQLi detectada\",\"severidad\":\"High\",\"confianza\":\"Medium\"},{\"descripcion\":{},\"severidad\":{},\"confianza\":{}}]}";
        Method metodoParseo = AnalizadorAI.class.getDeclaredMethod("parsearRespuesta", String.class);
        metodoParseo.setAccessible(true);

        ResultadoAnalisisMultiple resultado = (ResultadoAnalisisMultiple) metodoParseo.invoke(analizador, respuesta);

        assertTrue(resultado.obtenerNumeroHallazgos() >= 1);
        assertEquals("SQLi detectada", resultado.obtenerHallazgos().get(0).obtenerHallazgo());
    }

    @Test
    @DisplayName("Constructor tolera dependencias nulas sin lanzar excepcion")
    void testConstructorConDependenciasNulasNoRompe() {
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        assertDoesNotThrow(() -> new AnalizadorAI(
            solicitud,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    @DisplayName("Run falla rapido con alerta cuando falta API key requerida")
    void testRunFallaRapidoConAlertaPorApiKeyFaltante() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        AtomicReference<String> error = new AtomicReference<>("");
        AnalizadorAI analizador = new AnalizadorAI(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                }

                @Override
                public void alErrorAnalisis(String errorMsg) {
                    error.set(errorMsg);
                }
            }
        );

        analizador.run();

        assertTrue(error.get().contains("ALERTA: Clave de API requerida para OpenAI"));
    }

    @Test
    @DisplayName("Notifica inicio de analisis antes de validar configuracion")
    void testNotificaInicioAntesDeValidarConfiguracion() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-5-mini");
        config.establecerUrlBaseParaProveedor("OpenAI", "https://api.openai.com/v1");
        config.establecerApiKeyParaProveedor("OpenAI", "");

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );

        AtomicBoolean inicioNotificado = new AtomicBoolean(false);
        AtomicReference<String> error = new AtomicReference<>("");
        AnalizadorAI analizador = new AnalizadorAI(
            solicitud,
            config,
            null,
            null,
            new LimitadorTasa(1),
            new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                }

                @Override
                public void alErrorAnalisis(String errorMsg) {
                    error.set(errorMsg);
                }
            },
            () -> inicioNotificado.set(true),
            null,
            null,
            null
        );

        analizador.run();

        assertTrue(inicioNotificado.get());
        assertTrue(error.get().contains("ALERTA: Clave de API requerida para OpenAI"));
    }

    @Test
    @DisplayName("No duplica logs en stdout cuando hay GestorConsola")
    void testNoDuplicaLogsEnStdoutConGestorConsola() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com",
            "GET",
            "",
            "",
            "hash-test"
        );
        StringWriter salida = new StringWriter();
        GestorConsolaGUI gestorConsola = mock(GestorConsolaGUI.class);
        AnalizadorAI analizador = new AnalizadorAI(
            solicitud,
            config,
            new PrintWriter(salida, true),
            new PrintWriter(new StringWriter(), true),
            new LimitadorTasa(1),
            null,
            gestorConsola,
            null,
            null
        );

        Method registrar = AnalizadorAI.class.getDeclaredMethod("registrar", String.class);
        registrar.setAccessible(true);
        registrar.invoke(analizador, "mensaje de prueba");

        assertEquals("", salida.toString());
        verify(gestorConsola).registrar("AnalizadorAI", "mensaje de prueba", GestorConsolaGUI.TipoLog.INFO);
        verifyNoMoreInteractions(gestorConsola);
    }
}
