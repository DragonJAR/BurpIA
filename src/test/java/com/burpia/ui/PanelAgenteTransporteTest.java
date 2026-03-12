package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;
import com.jediterm.terminal.TtyConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PanelAgente Transporte Tests")
class PanelAgenteTransporteTest {

    @BeforeEach
    void setUp() {
        TestDialogUtils.registrarCapturaDialogos();
    }

    @AfterEach
    void tearDown() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.desregistrarCapturaDialogos();
    }

    @Test
    @DisplayName("Payload corto por TTY se envia en una sola escritura")
    void testEscrituraCortaViaTtySinChunking() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            boolean resultado = invocarEscrituraTty(panel, "hola");

            assertTrue(resultado, "assertTrue failed at PanelAgenteTransporteTest.java:50");
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

            assertTrue(resultado, "assertTrue failed at PanelAgenteTransporteTest.java:69");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(connector, times(3)).write(captor.capture());
            String reconstruido = String.join("", captor.getAllValues());
            assertEquals(payload, reconstruido, "assertEquals failed at PanelAgenteTransporteTest.java:73");
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

            assertFalse(resultado, "assertFalse failed at PanelAgenteTransporteTest.java:90");
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

            assertFalse(resultado, "assertFalse failed at PanelAgenteTransporteTest.java:111");
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

            int totalPendientes = contarInyeccionesPendientes(panel);
            String promptPendiente = obtenerTextoInyeccionPendiente(panel, 0);
            int delayPendiente = obtenerDelayInyeccionPendiente(panel, 0);
            String esperado = "PROMPT_PREFLIGHT_CUSTOM";

            assertEquals(1, totalPendientes, "assertEquals failed at PanelAgenteTransporteTest.java:136");
            assertEquals(esperado, promptPendiente, "assertEquals failed at PanelAgenteTransporteTest.java:136");
            assertNotEquals("PROMPT_VALIDACION_CUSTOM", promptPendiente);
            assertEquals(4000, delayPendiente, "assertEquals failed at PanelAgenteTransporteTest.java:138");
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

            int totalPendientes = contarInyeccionesPendientes(panel);
            String promptPendiente = obtenerTextoInyeccionPendiente(panel, 0);
            int delayPendiente = obtenerDelayInyeccionPendiente(panel, 0);

            assertEquals(1, totalPendientes, "assertEquals failed at PanelAgenteTransporteTest.java:163");
            assertEquals("PROMPT_PREFLIGHT_CUSTOM", promptPendiente, "assertEquals failed at PanelAgenteTransporteTest.java:160");
            assertEquals(0, delayPendiente, "assertEquals failed at PanelAgenteTransporteTest.java:161");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Forzar prompt inicial no marca envio si el preflight esta vacio")
    void testForzarPromptInicialIgnoraPreflightVacioSinConsumirBandera() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        establecerCampoString(config, "agentePreflightPrompt", "   ");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            panel.forzarInyeccionPromptInicial();

            assertEquals(0, contarInyeccionesPendientes(panel), "assertEquals failed at PanelAgenteTransporteTest.java:182");
            assertFalse(obtenerBandera(panel, "promptInicialEnviado"), "assertFalse failed at PanelAgenteTransporteTest.java:183");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Prompt inicial puede inyectarse despues de configurar preflight tras intento vacio")
    void testPromptInicialPuedeReintentarseTrasConfigurarPreflight() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        establecerCampoString(config, "agentePreflightPrompt", "");
        config.establecerAgenteDelay(1500);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            panel.forzarInyeccionPromptInicial();
            config.establecerAgentePreflightPrompt("PROMPT_POSTERIOR");

            marcarConsolaArrancando(panel, true);
            panel.forzarInyeccionPromptInicial();

            assertEquals(1, contarInyeccionesPendientes(panel), "assertEquals failed at PanelAgenteTransporteTest.java:201");
            assertEquals("PROMPT_POSTERIOR", obtenerTextoInyeccionPendiente(panel, 0), "assertEquals failed at PanelAgenteTransporteTest.java:202");
            assertTrue(obtenerBandera(panel, "promptInicialEnviado"), "assertTrue failed at PanelAgenteTransporteTest.java:203");
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
    @DisplayName("Inyeccion diferida se despacha despues del comando de arranque del agente")
    void testInyeccionDiferidaDespuesDelArranque() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
        config.establecerAgentePreflightPrompt("");
        config.establecerAgenteDelay(0);
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), "droid-test");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            establecerCampoLong(panel, "sesionActivaId", 88L);
            encolarInyeccionPendiente(panel, "PAYLOAD_DIFERIDO", 0);
            establecerBandera(panel, "inicializacionPendiente", true);

            Method method = PanelAgente.class.getDeclaredMethod("programarInyeccionInicial", long.class);
            method.setAccessible(true);
            method.invoke(panel, 88L);

            verify(connector, timeout(3500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("droid-test")
                ));
            verify(connector, timeout(3500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("PAYLOAD_DIFERIDO")
                ));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(connector, atLeast(1)).write(captor.capture());
            List<String> escrituras = captor.getAllValues();

            int indiceArranque = buscarPrimeraCoincidencia(escrituras, "droid-test");
            int indicePayload = buscarPrimeraCoincidencia(escrituras, "PAYLOAD_DIFERIDO");

            assertTrue(indiceArranque >= 0, "Debe enviarse el comando de arranque");
            assertTrue(indicePayload >= 0, "Debe enviarse el payload diferido");
            assertTrue(indiceArranque < indicePayload,
                "El payload diferido debe salir despues del arranque. Escrituras: " + escrituras);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Multiples payloads pendientes se preservan y se despachan en orden")
    void testMultiplesPayloadsPendientesSePreservanEnOrden() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
        config.establecerAgentePreflightPrompt("");
        config.establecerAgenteDelay(0);
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), "droid-test");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);
            marcarConsolaArrancando(panel, true);

            panel.inyectarComando("PAYLOAD_1", 0);
            panel.inyectarComando("PAYLOAD_2", 0);

            assertEquals(2, contarInyeccionesPendientes(panel), "assertEquals failed at PanelAgenteTransporteTest.java:259");

            establecerCampoLong(panel, "sesionActivaId", 144L);

            Method method = PanelAgente.class.getDeclaredMethod("programarInyeccionInicial", long.class);
            method.setAccessible(true);
            method.invoke(panel, 144L);

            verify(connector, timeout(3500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("droid-test")
                ));
            verify(connector, timeout(3500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("PAYLOAD_1")
                ));
            verify(connector, timeout(3500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains("PAYLOAD_2")
                ));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(connector, atLeast(1)).write(captor.capture());
            List<String> escrituras = captor.getAllValues();

            int indiceArranque = buscarPrimeraCoincidencia(escrituras, "droid-test");
            int indicePayload1 = buscarPrimeraCoincidencia(escrituras, "PAYLOAD_1");
            int indicePayload2 = buscarPrimeraCoincidencia(escrituras, "PAYLOAD_2");

            assertTrue(indiceArranque >= 0, "Debe enviarse el comando de arranque");
            assertTrue(indicePayload1 >= 0, "Debe enviarse el primer payload pendiente");
            assertTrue(indicePayload2 >= 0, "Debe enviarse el segundo payload pendiente");
            assertTrue(indiceArranque < indicePayload1,
                "El primer payload debe salir despues del arranque. Escrituras: " + escrituras);
            assertTrue(indicePayload1 < indicePayload2,
                "Los payloads pendientes deben conservar el orden original. Escrituras: " + escrituras);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Arranque normaliza rutas de binario con tilde y comillas")
    void testArranqueNormalizaRutaBinarioConTildeYComillas() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
        config.establecerAgentePreflightPrompt("");
        config.establecerAgenteDelay(0);
        config.establecerRutaBinarioAgente(
            AgenteTipo.CLAUDE_CODE.name(),
            "\"~/bin/claude\" --dangerously-skip-permissions"
        );

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);
            establecerCampoLong(panel, "sesionActivaId", 145L);

            Method method = PanelAgente.class.getDeclaredMethod("programarInyeccionInicial", long.class);
            method.setAccessible(true);
            method.invoke(panel, 145L);

            String rutaEsperada = "\"" + System.getProperty("user.home") + "/bin/claude\" --dangerously-skip-permissions";
            verify(connector, timeout(2500).atLeastOnce())
                .write(org.mockito.ArgumentMatchers.<String>argThat(
                    cmd -> cmd != null && cmd.contains(rutaEsperada)
                ));
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Shell configurado con comillas se normaliza antes de iniciar PTY")
    void testResolverShellEjecutableNormalizaComillas() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            String shellConfigurado = OSUtils.esWindows() ? "\"cmd.exe\"" : "\"/bin/sh\"";
            String esperado = OSUtils.esWindows() ? "cmd.exe" : "/bin/sh";

            Method method = PanelAgente.class.getDeclaredMethod("resolverShellEjecutable", String.class);
            method.setAccessible(true);
            String resultado = (String) method.invoke(panel, shellConfigurado);

            assertEquals(esperado, resultado, "assertEquals failed at PanelAgenteTransporteTest.java:344");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Cambiar agente rapido recupera foco mediante manejador configurado")
    void testCambiarAgenteRapidoRecuperaFocoConManejador() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
        config.establecerAgenteHabilitado(AgenteTipo.FACTORY_DROID.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.CLAUDE_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.GEMINI_CLI.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.OPEN_CODE.name(), true);
        String binarioExistente = OSUtils.esWindows() ? "cmd.exe" : "sh";
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.OPEN_CODE.name(), binarioExistente);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            AtomicBoolean focoSolicitado = new AtomicBoolean(false);
            panel.establecerManejadorFocoPestania(() -> focoSolicitado.set(true));

            // Probar el ciclo completo de agentes
            invocarCambiarAgenteRapido(panel);
            assertEquals(AgenteTipo.CLAUDE_CODE.name(), config.obtenerTipoAgente(), "assertEquals failed at PanelAgenteTransporteTest.java:268");

            invocarCambiarAgenteRapido(panel);
            assertEquals(AgenteTipo.GEMINI_CLI.name(), config.obtenerTipoAgente(), "assertEquals failed at PanelAgenteTransporteTest.java:272");

            invocarCambiarAgenteRapido(panel);
            assertEquals(AgenteTipo.OPEN_CODE.name(), config.obtenerTipoAgente(), "assertEquals failed at PanelAgenteTransporteTest.java:276");

            invocarCambiarAgenteRapido(panel);
            assertEquals(AgenteTipo.FACTORY_DROID.name(), config.obtenerTipoAgente(), "assertEquals failed at PanelAgenteTransporteTest.java:280");

            assertTrue(focoSolicitado.get(), "assertTrue failed at PanelAgenteTransporteTest.java:282");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Cambiar agente rapido muestra cada error y continua hasta el siguiente agente valido")
    void testCambiarAgenteRapidoContinuaTrasMultiplesAgentesInvalidos() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        String binarioExistente = OSUtils.esWindows() ? "cmd.exe" : "sh";
        config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
        config.establecerAgenteHabilitado(AgenteTipo.CLAUDE_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.GEMINI_CLI.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.OPEN_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.FACTORY_DROID.name(), true);
        config.establecerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name(), "/ruta/inexistente/gemini");
        config.establecerRutaBinarioAgente(AgenteTipo.OPEN_CODE.name(), "/ruta/inexistente/opencode");
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), binarioExistente);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            AtomicBoolean focoSolicitado = new AtomicBoolean(false);
            panel.establecerManejadorFocoPestania(() -> focoSolicitado.set(true));

            invocarCambiarAgenteRapido(panel);

            assertEquals(AgenteTipo.FACTORY_DROID.name(), config.obtenerTipoAgente(),
                "assertEquals failed at PanelAgenteTransporteTest.java:463");
            assertTrue(focoSolicitado.get(), "assertTrue failed at PanelAgenteTransporteTest.java:464");
            assertTrue(TestDialogUtils.contarVentanasPendientes() >= 2,
                "assertTrue failed at PanelAgenteTransporteTest.java:465");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Cambiar agente rapido agota alternativas invalidas y mantiene el agente actual")
    void testCambiarAgenteRapidoMantieneActualSiNoHayAlternativasValidas() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        String binarioExistente = OSUtils.esWindows() ? "cmd.exe" : "sh";
        config.establecerTipoAgente(AgenteTipo.OPEN_CODE.name());
        config.establecerAgenteHabilitado(AgenteTipo.OPEN_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.FACTORY_DROID.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.CLAUDE_CODE.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.GEMINI_CLI.name(), true);
        config.establecerRutaBinarioAgente(AgenteTipo.OPEN_CODE.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), "/ruta/inexistente/droid");
        config.establecerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name(), "/ruta/inexistente/claude");
        config.establecerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name(), "/ruta/inexistente/gemini");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            AtomicBoolean focoSolicitado = new AtomicBoolean(false);
            panel.establecerManejadorFocoPestania(() -> focoSolicitado.set(true));

            invocarCambiarAgenteRapido(panel);

            assertEquals(AgenteTipo.OPEN_CODE.name(), config.obtenerTipoAgente(),
                "assertEquals failed at PanelAgenteTransporteTest.java:487");
            assertFalse(focoSolicitado.get(), "assertFalse failed at PanelAgenteTransporteTest.java:488");
            assertTrue(TestDialogUtils.contarVentanasPendientes() >= 3,
                "assertTrue failed at PanelAgenteTransporteTest.java:489");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Cambiar agente rapido omite agentes deshabilitados sin mostrar errores")
    void testCambiarAgenteRapidoOmiteAgentesDeshabilitados() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        String binarioExistente = OSUtils.esWindows() ? "cmd.exe" : "sh";
        config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
        config.establecerAgenteHabilitado(AgenteTipo.FACTORY_DROID.name(), true);
        config.establecerAgenteHabilitado(AgenteTipo.GEMINI_CLI.name(), true);
        config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.CLAUDE_CODE.name(), "/ruta/inexistente/claude");
        config.establecerRutaBinarioAgente(AgenteTipo.GEMINI_CLI.name(), binarioExistente);
        config.establecerRutaBinarioAgente(AgenteTipo.OPEN_CODE.name(), "/ruta/inexistente/opencode");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            invocarCambiarAgenteRapido(panel);

            assertEquals(AgenteTipo.GEMINI_CLI.name(), config.obtenerTipoAgente(),
                "assertEquals failed at PanelAgenteTransporteTest.java:521");
            assertEquals(0, TestDialogUtils.contarVentanasPendientes(),
                "assertEquals failed at PanelAgenteTransporteTest.java:523");
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

            assertTrue(focoSolicitado.get(), "assertTrue failed at PanelAgenteTransporteTest.java:282");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Controles incluyen boton de ayuda con emoji")
    void testControlesIncluyenBotonAyuda() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            JButton botonAyuda = obtenerBotonAyuda(panel);
            assertNotNull(botonAyuda, "assertNotNull failed at PanelAgenteTransporteTest.java:294");
            assertEquals("❓", botonAyuda.getText(), "assertEquals failed at PanelAgenteTransporteTest.java:295");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Spinner de delay usa el rango configurado y refleja valores normalizados")
    void testSpinnerDelayUsaRangoConfigurado() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgenteDelay(-500);

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            JSpinner spinnerDelay = obtenerSpinnerDelay(panel);
            SpinnerNumberModel modelo = (SpinnerNumberModel) spinnerDelay.getModel();

            assertEquals(ConfiguracionAPI.AGENTE_DELAY_MINIMO_MS, ((Number) modelo.getMinimum()).intValue(),
                "assertEquals failed at PanelAgenteTransporteTest.java:485");
            assertEquals(ConfiguracionAPI.AGENTE_DELAY_MAXIMO_MS, ((Number) modelo.getMaximum()).intValue(),
                "assertEquals failed at PanelAgenteTransporteTest.java:487");
            assertEquals(ConfiguracionAPI.AGENTE_DELAY_MINIMO_MS, ((Number) spinnerDelay.getValue()).intValue(),
                "assertEquals failed at PanelAgenteTransporteTest.java:489");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Destruir reinicia el inyector PTY para permitir reutilización segura")
    void testDestruirReiniciaInyectorPty() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            ExecutorService inyectorInicial = obtenerInyectorPty(panel);
            assertNotNull(inyectorInicial, "assertNotNull failed at PanelAgenteTransporteTest.java:307");

            panel.destruir();

            ExecutorService inyectorPostDestruir = obtenerInyectorPty(panel);
            assertNotNull(inyectorPostDestruir, "assertNotNull failed at PanelAgenteTransporteTest.java:312");
            assertNotSame(inyectorInicial, inyectorPostDestruir, "assertNotSame failed at PanelAgenteTransporteTest.java:313");
            assertFalse(inyectorPostDestruir.isShutdown(), "assertFalse failed at PanelAgenteTransporteTest.java:314");

            panel.escribirComandoCrudo("echo test");
            ExecutorService inyectorPostEscritura = obtenerInyectorPty(panel);
            assertFalse(inyectorPostEscritura.isShutdown(), "assertFalse failed at PanelAgenteTransporteTest.java:318");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("URL de guía se resuelve por agente e idioma con fallback")
    void testResolverUrlGuiaPorAgenteEIdioma() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        PanelAgente panel = crearPanelSinConsola(config);
        try {
            config.establecerTipoAgente(AgenteTipo.FACTORY_DROID.name());
            config.establecerIdiomaUi("es");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-DROID-ES.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:332");

            config.establecerIdiomaUi("en");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-DROID-EN.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:338");

            config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
            config.establecerIdiomaUi("es");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-CLAUDE-ES.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:345");

            config.establecerIdiomaUi("en");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-CLAUDE-EN.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:351");

            config.establecerTipoAgente(AgenteTipo.GEMINI_CLI.name());
            config.establecerIdiomaUi("es");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-GEMINI-ES.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:358");

            config.establecerIdiomaUi("en");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-GEMINI-EN.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:364");

            config.establecerTipoAgente(AgenteTipo.OPEN_CODE.name());
            config.establecerIdiomaUi("es");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-OPENCODE-ES.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:371");

            config.establecerIdiomaUi("en");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-OPENCODE-EN.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:376");

            config.establecerTipoAgente("INVALIDO");
            config.establecerIdiomaUi("en");
            assertEquals(
                "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-DROID-EN.md",
                invocarResolverUrlGuia(panel)
            , "assertEquals failed at PanelAgenteTransporteTest.java:371");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("aplicarIdioma refresca tooltip del botón ayuda según idioma y agente")
    void testAplicarIdiomaRefrescaTooltipBotonAyuda() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTipoAgente(AgenteTipo.CLAUDE_CODE.name());
        config.establecerIdiomaUi("en");

        PanelAgente panel = crearPanelSinConsola(config);
        try {
            JButton botonAyuda = obtenerBotonAyuda(panel);

            I18nUI.establecerIdioma("en");
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            assertTrue(botonAyuda.getToolTipText().contains("Claude Code"), "assertTrue failed at PanelAgenteTransporteTest.java:393");
            assertTrue(botonAyuda.getToolTipText().contains("installation/setup guide"), "assertTrue failed at PanelAgenteTransporteTest.java:394");

            I18nUI.establecerIdioma("es");
            config.establecerIdiomaUi("es");
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            assertTrue(botonAyuda.getToolTipText().contains("Claude Code"), "assertTrue failed at PanelAgenteTransporteTest.java:399");
            assertTrue(botonAyuda.getToolTipText().contains("guía de instalación/configuración"), "assertTrue failed at PanelAgenteTransporteTest.java:400");
        } finally {
            panel.destruir();
            I18nUI.establecerIdioma("es");
        }
    }

    @Test
    @DisplayName("aplicarIdioma refresca textos de botones principales del agente")
    void testAplicarIdiomaRefrescaTextosBotonesPrincipales() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        PanelAgente panel = crearPanelSinConsola(config);
        try {
            JButton botonReiniciar = obtenerBotonPorNombre(panel, "btnReiniciar");
            JButton botonCtrlC = obtenerBotonPorNombre(panel, "btnCtrlC");
            JButton botonPayload = obtenerBotonPorNombre(panel, "btnInyectarPayload");

            I18nUI.establecerIdioma("en");
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            assertTrue(botonReiniciar.getText().contains("Restart"), "assertTrue failed at PanelAgenteTransporteTest.java:419");
            assertTrue(botonCtrlC.getText().contains("Ctrl+C"), "assertTrue failed at PanelAgenteTransporteTest.java:420");
            assertTrue(botonPayload.getText().contains("Inject Payload"), "assertTrue failed at PanelAgenteTransporteTest.java:421");

            I18nUI.establecerIdioma("es");
            SwingUtilities.invokeAndWait(panel::aplicarIdioma);
            assertTrue(botonReiniciar.getText().contains("Reiniciar"), "assertTrue failed at PanelAgenteTransporteTest.java:425");
            assertTrue(botonPayload.getText().contains("Inyectar Payload"), "assertTrue failed at PanelAgenteTransporteTest.java:426");
        } finally {
            panel.destruir();
            I18nUI.establecerIdioma("es");
        }
    }

    @Test
    @DisplayName("Modo angosto agrupa controles del agente en filas lógicas")
    void testModoAngostoAgrupaControlesEnFilasLogicas() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            JPanel panelControles = obtenerPanelControles(panel);

            SwingUtilities.invokeAndWait(() -> {
                panel.setSize(500, 280);
                panelControles.setSize(500, 140);
                panelControles.doLayout();
            });

            assertEquals(2, panelControles.getComponentCount(), "assertEquals failed at PanelAgenteTransporteTest.java:445");
            assertTrue(panelControles.getComponent(0) instanceof JPanel, "assertTrue failed at PanelAgenteTransporteTest.java:446");
            assertTrue(panelControles.getComponent(1) instanceof JPanel, "assertTrue failed at PanelAgenteTransporteTest.java:447");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Terminal embebida expone tooltip traducido")
    void testTerminalEmbebidaExponeTooltip() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            Method recrearTerminalWidget = PanelAgente.class.getDeclaredMethod("recrearTerminalWidget");
            recrearTerminalWidget.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    recrearTerminalWidget.invoke(panel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            JComponent terminal = obtenerTerminalWidget(panel);
            assertNotNull(terminal, "assertNotNull failed at PanelAgenteTransporteTest.java:468");
            assertEquals(I18nUI.Tooltips.Agente.TERMINAL(), terminal.getToolTipText(),
                    "assertEquals failed at PanelAgenteTransporteTest.java:470");
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

    private JButton obtenerBotonAyuda(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("btnAyudaAgente");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private JButton obtenerBotonPorNombre(PanelAgente panel, String nombreCampo) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private JSpinner obtenerSpinnerDelay(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("spinnerDelay");
        field.setAccessible(true);
        return (JSpinner) field.get(panel);
    }

    private JPanel obtenerPanelControles(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("panelControles");
        field.setAccessible(true);
        return (JPanel) field.get(panel);
    }

    private JComponent obtenerTerminalWidget(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("terminalWidget");
        field.setAccessible(true);
        return (JComponent) field.get(panel);
    }

    private ExecutorService obtenerInyectorPty(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("inyectorPty");
        field.setAccessible(true);
        return (ExecutorService) field.get(panel);
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

    private String invocarResolverUrlGuia(PanelAgente panel) throws Exception {
        Method method = PanelAgente.class.getDeclaredMethod("resolverUrlGuiaAgenteActual");
        method.setAccessible(true);
        return (String) method.invoke(panel);
    }

    private void marcarConsolaArrancando(PanelAgente panel, boolean valor) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("consolaArrancando");
        field.setAccessible(true);
        AtomicBoolean bandera = (AtomicBoolean) field.get(panel);
        bandera.set(valor);
    }

    private int contarInyeccionesPendientes(PanelAgente panel) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("inyeccionesPendientes");
        field.setAccessible(true);
        Queue<?> pendientes = (Queue<?>) field.get(panel);
        return pendientes != null ? pendientes.size() : 0;
    }

    private String obtenerTextoInyeccionPendiente(PanelAgente panel, int indice) throws Exception {
        Object pendiente = obtenerInyeccionPendiente(panel, indice);
        Field field = pendiente.getClass().getDeclaredField("texto");
        field.setAccessible(true);
        return (String) field.get(pendiente);
    }

    private int obtenerDelayInyeccionPendiente(PanelAgente panel, int indice) throws Exception {
        Object pendiente = obtenerInyeccionPendiente(panel, indice);
        Field field = pendiente.getClass().getDeclaredField("delayMs");
        field.setAccessible(true);
        return field.getInt(pendiente);
    }

    private void encolarInyeccionPendiente(PanelAgente panel, String texto, int delayMs) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("inyeccionesPendientes");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Queue<Object> pendientes = (Queue<Object>) field.get(panel);
        if (pendientes == null) {
            throw new IllegalStateException("La cola de inyecciones pendientes no debe ser null");
        }
        Class<?> clasePendiente = Class.forName("com.burpia.ui.PanelAgente$InyeccionPendiente");
        java.lang.reflect.Constructor<?> constructor = clasePendiente.getDeclaredConstructor(String.class, int.class);
        constructor.setAccessible(true);
        pendientes.add(constructor.newInstance(texto, delayMs));
    }

    private Object obtenerInyeccionPendiente(PanelAgente panel, int indice) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("inyeccionesPendientes");
        field.setAccessible(true);
        Queue<?> pendientes = (Queue<?>) field.get(panel);
        if (pendientes == null || indice < 0 || indice >= pendientes.size()) {
            throw new IllegalArgumentException("Indice de inyeccion pendiente invalido: " + indice);
        }
        int actual = 0;
        for (Object pendiente : pendientes) {
            if (actual == indice) {
                return pendiente;
            }
            actual++;
        }
        throw new IllegalArgumentException("No existe inyeccion pendiente en el indice: " + indice);
    }

    private void establecerCampoLong(PanelAgente panel, String nombre, long valor) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        field.setLong(panel, valor);
    }

    private void establecerBandera(PanelAgente panel, String nombre, boolean valor) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        AtomicBoolean bandera = (AtomicBoolean) field.get(panel);
        bandera.set(valor);
    }

    private boolean obtenerBandera(PanelAgente panel, String nombre) throws Exception {
        Field field = PanelAgente.class.getDeclaredField(nombre);
        field.setAccessible(true);
        AtomicBoolean bandera = (AtomicBoolean) field.get(panel);
        return bandera.get();
    }

    private void establecerCampoString(Object objetivo, String nombre, String valor) throws Exception {
        Field field = objetivo.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        field.set(objetivo, valor);
    }

    private int buscarPrimeraCoincidencia(List<String> valores, String fragmento) {
        if (valores == null || valores.isEmpty() || Normalizador.esVacio(fragmento)) {
            return -1;
        }
        for (int i = 0; i < valores.size(); i++) {
            String actual = valores.get(i);
            if (Normalizador.noEsVacio(actual) && actual.contains(fragmento)) {
                return i;
            }
        }
        return -1;
    }
}
