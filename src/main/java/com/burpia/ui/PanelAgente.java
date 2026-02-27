package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PanelAgente extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(PanelAgente.class.getName());

    private static final int DELAY_INICIO_BINARIO_MS = 800;
    private static final int MAX_REINTENTOS_INYECCION = 3;
    private static final int CHUNK_ESCRITURA_PTY = 128;
    private static final int DELAY_ENTRE_CHUNKS_PTY_MS = 10;

    private static final ExecutorService INYECTOR_PTY = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BurpIA-PTY-Injector");
        t.setDaemon(true);
        return t;
    });

    private JPanel panelControles;
    private JPanel panelResultadosWrapper;
    private final ConfiguracionAPI config;
    private JediTermWidget terminalWidget;
    private JLabel lblDelay;
    private JSpinner spinnerDelay;
    private JButton btnCambiarAgente;

    private PtyProcess process;
    private TtyConnector ttyConnector;

    private final AtomicReference<Runnable> manejadorFocoPestania;
    private final AtomicReference<Runnable> manejadorCambioConfiguracion;
    private final AtomicBoolean inicializacionPendiente;
    private final AtomicBoolean promptInicialEnviado;
    private final AtomicLong contadorInyeccion;
    private volatile String promptPendiente = null;
    private volatile int delayPendienteMs = 0;
    private volatile String ultimoAgenteIniciado = null;

    public PanelAgente(ConfiguracionAPI config) {
        this.config = config;
        this.promptInicialEnviado = new AtomicBoolean(false);
        this.inicializacionPendiente = new AtomicBoolean(false);
        this.contadorInyeccion = new AtomicLong(0);
        this.manejadorFocoPestania = new AtomicReference<>();
        this.manejadorCambioConfiguracion = new AtomicReference<>();
        
        BorderLayout layout = new BorderLayout(EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL);
        setLayout(layout);
        setBorder(BorderFactory.createEmptyBorder(
            EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL, 
            EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL
        ));

        inicializarComponentesUI();
        iniciarConsola();
    }

    public void establecerManejadorFocoPestania(Runnable manejador) {
        manejadorFocoPestania.set(manejador);

        if (inicializacionPendiente.get() && manejador != null) {
            inicializacionPendiente.set(false);
            procesarInicializacionDiferida();
        }
    }

    public void establecerManejadorCambioConfiguracion(Runnable manejador) {
        manejadorCambioConfiguracion.set(manejador);
    }

    public void escribirComando(String comando) {
        if (comando == null || comando.trim().isEmpty()) {
            return;
        }
        escribirComandoCrudo(OSUtils.prepararComando(comando));
    }

    public void escribirComandoCrudo(String comando) {
        if (comando == null || comando.isEmpty()) {
            return;
        }
        INYECTOR_PTY.execute(() -> escribirComandoCrudoSeguro(comando));
    }

    private boolean escribirComandoCrudoSeguro(String comando) {
        try {
            if (escribirTextoViaTtyConnector(comando)) {
                return true;
            }
            return escribirTextoDirectoPTY(comando);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, I18nLogs.tr("Error escribiendo comando crudo PTY"), e);
            return false;
        }
    }

    private boolean escribirTextoViaTtyConnector(String texto) {
        if (texto == null || texto.isEmpty() || ttyConnector == null) {
            return false;
        }

        try {
            if (!ttyConnector.isConnected()) {
                return false;
            }
            for (int i = 0; i < texto.length(); i += CHUNK_ESCRITURA_PTY) {
                int end = Math.min(texto.length(), i + CHUNK_ESCRITURA_PTY);
                ttyConnector.write(texto.substring(i, end));
                if (texto.length() > CHUNK_ESCRITURA_PTY) {
                    dormirSilencioso(DELAY_ENTRE_CHUNKS_PTY_MS);
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, I18nLogs.tr("Error escribiendo por ttyConnector"), e);
            return false;
        }
    }

    private boolean escribirTextoDirectoPTY(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false;
        }
        
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (process == null || !process.isAlive()) {
            LOGGER.fine(I18nLogs.tr("Escritura PTY omitida: proceso no disponible"));
            return false;
        }
        
        try {
            java.io.OutputStream os = process.getOutputStream();
            if (os == null) {
                LOGGER.fine(I18nLogs.tr("Escritura PTY omitida: stream de salida nulo"));
                return false;
            }
            for (int i = 0; i < bytes.length; i += CHUNK_ESCRITURA_PTY) {
                int end = Math.min(bytes.length, i + CHUNK_ESCRITURA_PTY);
                os.write(bytes, i, end - i);
                os.flush();
                if (bytes.length > CHUNK_ESCRITURA_PTY) {
                    dormirSilencioso(DELAY_ENTRE_CHUNKS_PTY_MS);
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error escritura directa PTY", e);
            return false;
        }
    }


    public void inyectarComando(String texto, int delayMs) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        if (!estaPanelListoParaInyeccion()) {
            promptPendiente = texto;
            this.delayPendienteMs = delayMs;
            inicializacionPendiente.set(true);
            return;
        }

        ejecutarInyeccionConOpciones(texto, 0, delayMs);
    }

    public void forzarInyeccionPromptInicial() {
        if (promptInicialEnviado.compareAndSet(false, true)) {
            String prompt = config.obtenerAgentePrompt();
            inyectarComando(prompt, config.obtenerAgenteDelay());
        }
    }

    public void reinyectarPromptInicial() {
        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);
        forzarInyeccionPromptInicial();
    }

    public void reiniciar() {
        destruir();
        iniciarConsola();
    }

    public void destruir() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            process = null;
        }
        if (ttyConnector != null) {
            try {
                ttyConnector.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, I18nLogs.tr("Error cerrando ttyConnector"), e);
            }
            ttyConnector = null;
        }
        promptInicialEnviado.set(false);
        promptPendiente = null;
        delayPendienteMs = 0;
        inicializacionPendiente.set(false);
    }

    public void aplicarIdioma() {
        actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        actualizarTituloPanel(panelResultadosWrapper, I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO());

        if (btnCambiarAgente != null) {
            btnCambiarAgente.setText("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO());
            btnCambiarAgente.setToolTipText(I18nUI.Tooltips.FactoryDroid.CAMBIAR_AGENTE_RAPIDO());
        }
        if (lblDelay != null) {
            lblDelay.setText(I18nUI.Consola.ETIQUETA_DELAY());
            lblDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        }
        if (spinnerDelay != null) {
            spinnerDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        }

        revalidate();
        repaint();
    }
    
    private void inicializarComponentesUI() {
        panelControles = crearPanelControles();
        panelResultadosWrapper = crearPanelResultados();
        
        add(panelControles, BorderLayout.NORTH);
        add(panelResultadosWrapper, BorderLayout.CENTER);
    }

    private JPanel crearPanelControles() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, EstilosUI.ESPACIADO_COMPONENTES, 4));
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), 12, 16));

        JButton btnReiniciar = crearBoton("🔄 " + I18nUI.Consola.BOTON_REINICIAR(), 
            I18nUI.Tooltips.FactoryDroid.REINICIAR(), e -> reiniciar());

        JButton btnCtrlC = crearBoton("⚡ " + I18nUI.Consola.BOTON_CTRL_C(), 
            I18nUI.Tooltips.FactoryDroid.CTRL_C(), e -> escribirComandoCrudo("\u0003"));

        JButton btnInyectarPayload = crearBoton("💉 " + I18nUI.Consola.BOTON_INYECTAR_PAYLOAD(), 
            I18nUI.Tooltips.FactoryDroid.INYECTAR_PAYLOAD(), e -> inyectarPayloadInicialManual());

        btnCambiarAgente = crearBoton("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO(),
            I18nUI.Tooltips.FactoryDroid.CAMBIAR_AGENTE_RAPIDO(), e -> cambiarAgenteRapido());

        panel.add(btnReiniciar);
        panel.add(btnCtrlC);
        panel.add(btnInyectarPayload);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(btnCambiarAgente);

        lblDelay = new JLabel(I18nUI.Consola.ETIQUETA_DELAY());
        lblDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());

        spinnerDelay = new JSpinner(new SpinnerNumberModel(
            config.obtenerAgenteDelay(), 1000, 30000, 500));
        spinnerDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        spinnerDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        spinnerDelay.setPreferredSize(new Dimension(80, 24));
        spinnerDelay.addChangeListener(e -> {
            int nuevoDelay = (int) spinnerDelay.getValue();
            config.establecerAgenteDelay(nuevoDelay);
            Runnable handler = manejadorCambioConfiguracion.get();
            if (handler != null) {
                handler.run();
            }
        });

        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblDelay);
        panel.add(spinnerDelay);

        return panel;
    }
    
    private void cambiarAgenteRapido() {
        try {
            AgenteTipo actual = AgenteTipo.desdeCodigo(config.obtenerTipoAgente(), AgenteTipo.FACTORY_DROID);
            if (actual == null) {
                actual = AgenteTipo.FACTORY_DROID;
            }
            AgenteTipo destino = (actual == AgenteTipo.FACTORY_DROID)
                ? AgenteTipo.CLAUDE_CODE
                : AgenteTipo.FACTORY_DROID;

            String rutaBinario = resolverRutaBinario(destino);
            if (!OSUtils.existeBinario(rutaBinario)) {
                UIUtils.mostrarErrorBinarioAgenteNoEncontrado(
                    this,
                    destino.getNombreVisible(),
                    I18nUI.Configuracion.Agentes.MSG_BINARIO_NO_EXISTE_SIMPLE(rutaBinario),
                    I18nUI.Configuracion.Agentes.ENLACE_INSTALAR_AGENTE(destino.getNombreVisible()),
                    destino.getUrlDocPorIdioma(config.obtenerIdiomaUi())
                );
                return;
            }

            config.establecerTipoAgente(destino.name());

            Runnable handler = manejadorCambioConfiguracion.get();
            if (handler != null) {
                handler.run();
            }

            reiniciar();

            actualizarEstadoBotones();
            aplicarIdioma();

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, I18nLogs.tr("Error al cambiar de agente"), ex);
        }
    }

    public String obtenerUltimoAgenteIniciado() {
        return ultimoAgenteIniciado;
    }

    private void actualizarEstadoBotones() {
        if (btnCambiarAgente != null) {
            btnCambiarAgente.setEnabled(true);
            btnCambiarAgente.setText("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO());
        }
    }

    private JPanel crearPanelResultados() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO(), 3, 3));
        return panel;
    }

    private JButton crearBoton(String texto, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(texto);
        btn.setFont(EstilosUI.FUENTE_ESTANDAR);
        btn.setToolTipText(tooltip);
        btn.addActionListener(listener);
        return btn;
    }

    private void iniciarConsola() {
        destruir();

        terminalWidget = crearTerminalWidget();
        
        panelResultadosWrapper.removeAll();
        panelResultadosWrapper.add(terminalWidget, BorderLayout.CENTER);
        panelResultadosWrapper.revalidate();
        panelResultadosWrapper.repaint();

        new Thread(this::iniciarProcesoPty, "BurpIA-PTY-Starter").start();
    }

    public void enfocarTerminal() {
        SwingUtilities.invokeLater(() -> {
            if (terminalWidget != null && terminalWidget.getTerminalPanel() != null) {
                terminalWidget.getTerminalPanel().requestFocusInWindow();
            }
        });
    }   

    private JediTermWidget crearTerminalWidget() {
        JediTermWidget widget = new JediTermWidget(120, 24, new DefaultSettingsProvider() {
            @Override
            public float getTerminalFontSize() { return 14; }
            @Override
            public boolean useAntialiasing() { return true; }
            @Override
            public boolean copyOnSelect() { return true; }
            @Override
            public boolean pasteOnMiddleMouseClick() { return true; }
        });
        widget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return widget;
    }

    private void iniciarProcesoPty() {
        try {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm-256color");

            String[] command = construirComandoShell();

            process = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(env)
                .start();

            TtyConnector rawConnector = new PtyProcessTtyConnector(process, StandardCharsets.UTF_8);
            ttyConnector = new TtyConnector() {
                @Override
                public boolean init(com.jediterm.terminal.Questioner q) { return rawConnector.init(q); }
                @Override
                public void close() { rawConnector.close(); }
                @Override
                public String getName() { return rawConnector.getName(); }
                @Override
                public int read(char[] buf, int offset, int length) throws java.io.IOException { return rawConnector.read(buf, offset, length); }
                @Override
                public void write(byte[] bytes) throws java.io.IOException {
                    int chunkSize = 64;
                    int delaySleep = 20;
                    for (int i = 0; i < bytes.length; i += chunkSize) {
                        int end = Math.min(bytes.length, i + chunkSize);
                        byte[] chunk = java.util.Arrays.copyOfRange(bytes, i, end);
                        rawConnector.write(chunk);
                        if (bytes.length > chunkSize) {
                            try { Thread.sleep(delaySleep); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
                    }
                }
                @Override
                public void write(String string) throws java.io.IOException {
                    int chunkSize = 64;
                    int delaySleep = 20;
                    for (int i = 0; i < string.length(); i += chunkSize) {
                        int end = Math.min(string.length(), i + chunkSize);
                        rawConnector.write(string.substring(i, end));
                        if (string.length() > chunkSize) {
                            try { Thread.sleep(delaySleep); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
                    }
                }
                @Override
                public boolean isConnected() { return rawConnector.isConnected(); }
                @Override
                public int waitFor() throws InterruptedException { return rawConnector.waitFor(); }
                @Override
                public boolean ready() throws java.io.IOException { return rawConnector.ready(); }
            @Override
            public void resize(java.awt.Dimension termSize, java.awt.Dimension pixelSize) { rawConnector.resize(termSize, pixelSize); }
            };

            SwingUtilities.invokeLater(() -> {
                if (terminalWidget != null) {
                    terminalWidget.setTtyConnector(ttyConnector);
                    terminalWidget.start();
                    ultimoAgenteIniciado = config.obtenerTipoAgente();
                    if (inicializacionPendiente.get()) {
                        inicializacionPendiente.set(false);
                        procesarInicializacionDiferida();
                    }
                    programarInyeccionInicial();
                }
            });

        } catch (Throwable t) {
            manejarErrorPty(t);
        }
    }

    private String[] construirComandoShell() {
        if (OSUtils.esWindows()) {
            return new String[]{"cmd.exe"};
        }
        
        String shell = System.getenv("SHELL");
        if (Normalizador.esVacio(shell)) {
            shell = "/bin/bash";
        }
        return new String[]{shell, "--login"};
    }

    private void programarInyeccionInicial() {
        if (promptInicialEnviado.get()) return;

        AgenteTipo tipoActual = AgenteTipo.desdeCodigo(config.obtenerTipoAgente(), AgenteTipo.FACTORY_DROID);
        String prompt = config.obtenerAgentePrompt();
        int usuarioDelay = config.obtenerAgenteDelay();
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(tipoActual);
        String comandoArranque = resolverComandoArranque(tipoActual, opciones);

        LOGGER.info(I18nLogs.tr("Iniciando secuencia de inyeccion automatica de agente..."));

        INYECTOR_PTY.execute(() -> {
            dormirSilencioso(DELAY_INICIO_BINARIO_MS);

            if (!escribirComandoCrudoSeguro(OSUtils.prepararComando(comandoArranque))) {
                return;
            }

            if (prompt == null || prompt.trim().isEmpty()) {
                return;
            }
            if (!promptInicialEnviado.compareAndSet(false, true)) {
                return;
            }

            inyectarComandoConRetraso(
                prompt,
                Math.max(0, usuarioDelay),
                opciones,
                "AUTO_INIT"
            );
        });
    }

    private String resolverComandoArranque(AgenteTipo tipoActual, AgentRuntimeOptions.EnterOptions opciones) {
        String comandoProbe = construirComandoProbe(opciones.probeMode());
        if (comandoProbe != null) {
            LOGGER.info(I18nLogs.trTecnico(
                "[ENTER-PROBE] mode=" + opciones.probeMode().name() +
                    " os=" + describirPlataformaActual() +
                    " command='" + comandoProbe + "'"
            ));
            return comandoProbe;
        }
        return resolverRutaBinario(tipoActual);
    }

    private String construirComandoProbe(AgentRuntimeOptions.ProbeMode probeMode) {
        AgentRuntimeOptions.ProbeMode modo = probeMode != null ? probeMode : AgentRuntimeOptions.ProbeMode.OFF;
        if (modo == AgentRuntimeOptions.ProbeMode.OFF) {
            return null;
        }

        String rutaRelativa = modo == AgentRuntimeOptions.ProbeMode.WINDOWS
            ? "scripts/diagnostics/enter-probe-windows.ps1"
            : "scripts/diagnostics/enter-probe-posix.sh";
        File script = new File(rutaRelativa);
        if (!script.isFile()) {
            LOGGER.warning(I18nLogs.trTecnico(
                "[ENTER-PROBE] script no encontrado: " + script.getAbsolutePath() + ". Usando agente real."
            ));
            return null;
        }

        String ruta = escaparComillasDobles(script.getAbsolutePath());
        if (modo == AgentRuntimeOptions.ProbeMode.WINDOWS) {
            return "powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File \"" + ruta + "\"";
        }
        return "bash \"" + ruta + "\"";
    }

    private String escaparComillasDobles(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\"", "\\\"");
    }

    private String resolverRutaBinario(AgenteTipo tipo) {
        String binarioConfig = config.obtenerRutaBinarioAgente(tipo != null ? tipo.name() : config.obtenerTipoAgente());
        
        if (Normalizador.esVacio(binarioConfig)) {
            return tipo != null ? tipo.getRutaPorDefecto() : "droid";
        }

        return OSUtils.expandirRuta(binarioConfig.trim());
    }

    private boolean estaPanelListoParaInyeccion() {
        return manejadorFocoPestania.get() != null
            && terminalWidget != null
            && terminalWidget.getTerminalPanel() != null
            && process != null
            && process.isAlive();
    }

    private void procesarInicializacionDiferida() {
        if (promptPendiente != null) {
            String prompt = promptPendiente;
            int delay = this.delayPendienteMs;

            promptPendiente = null;
            this.delayPendienteMs = 0;
            
            SwingUtilities.invokeLater(() -> 
                inyectarComandoConRetraso(prompt, delay)
            );
        }
    }

    private void ejecutarInyeccionConOpciones(String texto, int intentoActual, int delayPendienteMsUsuario) {
        if (intentoActual >= MAX_REINTENTOS_INYECCION) {
            LOGGER.warning(I18nLogs.tr("Maximo de reintentos de inyeccion alcanzado"));
            return;
        }
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(config.obtenerTipoAgente());
        inyectarComandoConRetraso(texto, delayPendienteMsUsuario, opciones, "API_OR_UI");
    }

    private void inyectarComandoConRetraso(String texto, int delayMs) {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(config.obtenerTipoAgente());
        inyectarComandoConRetraso(texto, delayMs, opciones, "API_OR_UI");
    }

    private void inyectarComandoConRetraso(
        String texto,
        int delayMs,
        AgentRuntimeOptions.EnterOptions opciones,
        String origen
    ) {
        SwingUtilities.invokeLater(() -> {
            OSUtils.cerrarVentanaAjustes();
            if (delayMs > 0) {
                LOGGER.info(I18nLogs.tr("Esperando el delay establecido por el usuario (" + delayMs + " ms) antes de la inyeccion..."));
                new Timer(delayMs, ev -> {
                    ((Timer) ev.getSource()).stop();
                    aplicarEscrituraDirecta(texto, opciones, origen);
                }).start();
            } else {
                aplicarEscrituraDirecta(texto, opciones, origen);
            }
        });
    }

    private void aplicarEscrituraDirecta(String texto, AgentRuntimeOptions.EnterOptions opciones, String origen) {
        String ansiStart = "\u001b[200~";
        String ansiEnd = "\u001b[201~";

        String payloadConBrackets = ansiStart + texto + ansiEnd;
        long injectionId = contadorInyeccion.incrementAndGet();
        SubmitSequenceBuilder.SubmitSequence secuencia = SubmitSequenceBuilder.construir(
            opciones.tipoAgente(),
            opciones.estrategiaSubmitOverride()
        );

        INYECTOR_PTY.execute(() -> {
            LOGGER.info(I18nLogs.trTecnico(
                "[ENTER-FLOW] id=" + injectionId +
                    " origin=" + origen +
                    " agent=" + opciones.tipoAgente().name() +
                    " os=" + describirPlataformaActual() +
                    " strategyOverride=" + (opciones.estrategiaSubmitOverride() != null ? opciones.estrategiaSubmitOverride() : "AUTO") +
                    " strategyFinal=" + secuencia.descripcion()
            ));
            logDebugTransporte(opciones.enterDebugActivo(), "PASTE_WRITE#" + injectionId, payloadConBrackets);
            if (!escribirComandoCrudoSeguro(payloadConBrackets)) {
                return;
            }
            LOGGER.info(I18nLogs.tr("Payload en bufer usando escritura directa (tty stream con bracketed paste). Esperando confirmacion..."));
            dormirSilencioso(opciones.delaySubmitPostPasteMs());
            enviarSecuenciaSubmit(opciones, secuencia, injectionId, origen);
        });
    }

    private void enviarSecuenciaSubmit(
        AgentRuntimeOptions.EnterOptions opciones,
        SubmitSequenceBuilder.SubmitSequence secuencia,
        long injectionId,
        String origen
    ) {
        if (opciones.probeSubmitActivo()) {
            LOGGER.info(I18nLogs.trTecnico(
                "[ENTER-PROBE] agent=" + opciones.tipoAgente().name() +
                " os=" + describirPlataformaActual() +
                " strategyOverride=" + (opciones.estrategiaSubmitOverride() != null ? opciones.estrategiaSubmitOverride() : "AUTO") +
                " sequence=" + secuencia.descripcion() +
                " payloadEsc='" + escaparControl(secuencia.payload()) + "'"
            ));
        }

        for (int i = 0; i < secuencia.repeticiones(); i++) {
            logDebugTransporte(
                opciones.enterDebugActivo(),
                "SUBMIT_WRITE#" + injectionId + "[" + (i + 1) + "/" + secuencia.repeticiones() + "]",
                secuencia.payload()
            );
            if (!escribirComandoCrudoSeguro(secuencia.payload())) {
                return;
            }
            if (i + 1 < secuencia.repeticiones()) {
                dormirSilencioso(secuencia.delayEntreEnviosMs());
            }
        }
        LOGGER.info(I18nLogs.tr("Se ha despachado la secuencia VK_ENTER") + " [id=" + injectionId + ", " + secuencia.descripcion() + "]");
        LOGGER.info(I18nLogs.trTecnico(
            "[ENTER-RESULT] id=" + injectionId +
                " origin=" + origen +
                " outcome=unknown reason=transport-level-dispatch-only"
        ));
    }

    private void dormirSilencioso(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logDebugTransporte(boolean activo, String etapa, String payload) {
        if (!activo) {
            return;
        }
        String seguro = payload != null ? payload : "";
        LOGGER.info(I18nLogs.trTecnico(
            "[ENTER-DEBUG] " + etapa +
            " len=" + seguro.length() +
            " esc='" + escaparControl(seguro) + "'" +
            " hex=" + hexResumen(seguro)
        ));
    }

    private String escaparControl(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
            .replace("\\", "\\\\")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\u001b", "\\u001b");
    }

    private String hexResumen(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "[]";
        }
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int limite = Math.min(bytes.length, 16);
        for (int i = 0; i < limite; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", bytes[i]));
        }
        if (bytes.length > limite) {
            sb.append(" ...");
        }
        sb.append(']');
        return sb.toString();
    }

    private String describirPlataformaActual() {
        return SubmitSequenceBuilder.Plataforma.desdeSistemaActual()
            .name()
            .toLowerCase(java.util.Locale.ROOT);
    }

    private void manejarErrorPty(Throwable t) {
        logErrorPty(t);
        String mensaje = t.getMessage() != null ? t.getMessage() : I18nLogs.tr("Error desconocido PTY");
        SwingUtilities.invokeLater(() -> {
            UIUtils.mostrarError(PanelAgente.this, I18nUI.Consola.TITULO_ERROR_PTY(), mensaje);
        });
    }

    private void logErrorPty(Throwable t) {
        try {
            String homeDir = System.getProperty("user.home");
            java.io.FileWriter fw = new java.io.FileWriter(homeDir + "/burpia_pty_error.log", true);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println(I18nUI.Consola.HEADER_LOG_ERROR_PTY());
                t.printStackTrace(pw);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, I18nLogs.tr("No se pudo escribir log de error PTY"), e);
        }
    }

    private void actualizarTituloPanel(JPanel panel, String titulo) {
        UIUtils.actualizarTituloPanel(panel, titulo);
    }

    public void inyectarPayloadInicialManual() {
        Runnable focoHandler = manejadorFocoPestania.get();
        if (focoHandler != null) {
            focoHandler.run();
        }

        String prompt = config.obtenerAgentePrompt();

        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);

        inyectarComando(prompt, 0);

        LOGGER.info(I18nLogs.tr("Payload inicial encolado para inyeccion manual por el usuario"));
    }
}
