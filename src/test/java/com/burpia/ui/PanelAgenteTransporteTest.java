package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.util.OSUtils;
import com.jediterm.terminal.TtyConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PanelAgente Transporte Tests")
class PanelAgenteTransporteTest {

    @Test
    @DisplayName("Payload corto por TTY se envia en una sola escritura")
    void testEscrituraCortaViaTtySinChunking() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            boolean resultado = invocarEscrituraTty(panel, "hola");

            assertTrue(resultado);
            verify(connector, times(1)).write("hola");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Payload largo por TTY se divide en chunks y preserva contenido")
    void testEscrituraLargaViaTtyConChunking() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            String payload = "A".repeat(260);
            boolean resultado = invocarEscrituraTty(panel, payload);

            assertTrue(resultado);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(connector, times(3)).write(captor.capture());
            String reconstruido = String.join("", captor.getAllValues());
            assertEquals(payload, reconstruido);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Escritura TTY falla limpio si conector no esta conectado")
    void testEscrituraTtyConectorDesconectado() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(false);
            inyectarTtyConnector(panel, connector);

            boolean resultado = invocarEscrituraTty(panel, "payload");

            assertFalse(resultado);
            verify(connector, never()).write(anyString());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Sesiones viejas no pueden escribir en PTY despues de reinicio")
    void testSesionViejaNoEscribeEnPty() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);
            establecerCampoLong(panel, "sesionActivaId", 200L);

            Method method = PanelAgente.class.getDeclaredMethod("escribirComandoCrudoSeguro", String.class, long.class);
            method.setAccessible(true);
            boolean resultado = (boolean) method.invoke(panel, "payload", 199L);

            assertFalse(resultado);
            verify(connector, never()).write(anyString());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Prompt inicial del agente usa preflight configurado y no prompt de validacion")
    void testPromptInicialUsaPreflightConfigurado() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgentePreflightPrompt("PROMPT_PREFLIGHT_CUSTOM");
        config.establecerAgentePrompt("PROMPT_VALIDACION_CUSTOM");
        config.establecerAgenteDelay(4000);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            marcarConsolaArrancando(panel, true);

            panel.forzarInyeccionPromptInicial();

            String promptPendiente = obtenerCampoString(panel, "promptPendiente");
            int delayPendiente = obtenerCampoInt(panel, "delayPendienteMs");
            String esperado = "PROMPT_PREFLIGHT_CUSTOM";

            assertEquals(esperado, promptPendiente);
            assertNotEquals("PROMPT_VALIDACION_CUSTOM", promptPendiente);
            assertEquals(4000, delayPendiente);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Inyeccion manual de payload usa preflight configurado sin delay")
    void testInyeccionManualUsaPreflightConfiguradoSinDelay() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgentePreflightPrompt("PROMPT_PREFLIGHT_CUSTOM");
        config.establecerAgentePrompt("PROMPT_VALIDACION_CUSTOM");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            marcarConsolaArrancando(panel, true);

            panel.inyectarPayloadInicialManual();

            String promptPendiente = obtenerCampoString(panel, "promptPendiente");
            int delayPendiente = obtenerCampoInt(panel, "delayPendienteMs");

            assertEquals("PROMPT_PREFLIGHT_CUSTOM", promptPendiente);
            assertEquals(0, delayPendiente);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Arranque automatico siempre ejecuta binario aunque prompt inicial ya este marcado")
    void testArranqueAutomaticoEjecutaBinarioConPromptInicialYaMarcado() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente("FACTORY_DROID");
        config.establecerAgentePreflightPrompt("PRECHECK");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            Field flagPrompt = PanelAgente.class.getDeclaredField("promptInicialEnviado");
            flagPrompt.setAccessible(true);
            AtomicBoolean promptInicial = (AtomicBoolean) flagPrompt.get(panel);
            promptInicial.set(true);

            establecerCampoLong(panel, "sesionActivaId", 77L);

            Method method = PanelAgente.class.getDeclaredMethod("programarInyeccionInicial", long.class);
            method.setAccessible(true);
            method.invoke(panel, 77L);

            verify(connector, timeout(2500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("droid")
                ));
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Cambiar agente rapido recupera foco mediante manejador configurado")
    void testCambiarAgenteRapidoRecuperaFocoConManejador() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
        String binarioExistente = OSUtils.esWindows() ? "cmd.exe" : "sh";
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name(), binarioExistente);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            AtomicBoolean focoSolicitado = new AtomicBoolean(false);
            panel.establecerManejadorFocoPestania(() -> focoSolicitado.set(true));

            invocarCambiarAgenteRapido(panel);

            assertTrue(focoSolicitado.get());
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), config.obtenerTipoAgente());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Reiniciar reutiliza la misma ruta de foco configurada")
    void testReiniciarReutilizaRutaUnicaDeFoco() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            AtomicBoolean focoSolicitado = new AtomicBoolean(false);
            panel.establecerManejadorFocoPestania(() -> focoSolicitado.set(true));

            invocarReiniciarYSolicitarFoco(panel);

            assertTrue(focoSolicitado.get());
        } finally {
            panel.destruir();
        }
    }

    private PanelAgente crearPanelSinConsola() throws Exception {
        return crearPanelSinConsola(new ConfiguracionAPI());
    }

    private PanelAgente crearPanelSinConsola(ConfiguracionAPI config) throws Exception {
        final PanelAgente[] holder = new PanelAgente[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelAgente(config, false));
        return holder[0];
    }

    private void inyectarTtyConnector(PanelAgente panel, TtyConnector connector) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("ttyConnector");
        field.setAccessible(true);
        field.set(panel, connector);
    }

    private void invocarCambiarAgenteRapido(PanelAgente panel) throws Exception {
        Method method = PanelAgente.class.getDeclaredMethod("cambiarAgenteRapido");
        method.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                method.invoke(panel);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void invocarReiniciarYSolicitarFoco(PanelAgente panel) throws Exception {
        Method method = PanelAgente.class.getDeclaredMethod("reiniciarYSolicitarFoco");
        method.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                method.invoke(panel);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean invocarEscrituraTty(PanelAgente panel, String payload) throws Exception {
        Method method = PanelAgente.class.getDeclaredMethod("escribirTextoViaTtyConnector", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(panel, payload);
    }

    private void marcarConsolaArrancando(PanelAgente panel, boolean valor) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("consolaArrancando");
        field.setAccessible(true);
        AtomicBoolean bandera = (AtomicBoolean) field.get(panel);
        bandera.set(valor);
    }

    private String obtenerCampoString(PanelAgente panel, String nombre) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        Object valor = field.get(panel);
        return valor != null ? valor.toString() : null;
    }

    private int obtenerCampoInt(PanelAgente panel, String nombre) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        return field.getInt(panel);
    }

    private void establecerCampoLong(PanelAgente panel, String nombre, long valor) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        field.setLong(panel, valor);
    }
}
