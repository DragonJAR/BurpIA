package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelAgente extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(PanelAgente.class.getName());
    private final GestorLoggingUnificado gestorLogging;

    private static final int DELAY_INICIO_BINARIO_MS = 800;
    private static final int DELAY_DIFERIDA_POST_ARRANQUE_MS = 180;
    private static final int CHUNK_ESCRITURA_PTY = 128;
    private static final int DELAY_ENTRE_CHUNKS_PTY_MS = 10;
    private static final int INTENTOS_ENVIO_ARRANQUE = 6;
    private static final int DELAY_REINTENTO_ARRANQUE_MS = 200;
    private static final int DELAY_REINTENTO_FOCO_TERMINAL_MS = 120;

    private final Object lockInyectorPty = new Object();
    private ExecutorService inyectorPty;

    private JPanel panelControles;
    private JPanel panelResultadosWrapper;
    private final ConfiguracionAPI config;
    private JediTermWidget terminalWidget;
    private JLabel lblDelay;
    private JSpinner spinnerDelay;
    private JButton btnReiniciar;
    private JButton btnCtrlC;
    private JButton btnInyectarPayload;
    private JButton btnCambiarAgente;
    private JButton btnAyudaAgente;

    private PtyProcess process;
    private TtyConnector ttyConnector;

    private final AtomicReference<Runnable> manejadorFocoPestania;
    private final AtomicReference<Runnable> manejadorCambioConfiguracion;
    private final AtomicBoolean inicializacionPendiente;
    private final AtomicBoolean promptInicialEnviado;
    private final AtomicBoolean arranqueAgenteDespachado;
    private final AtomicBoolean consolaArrancando;
    private final AtomicLong contadorInyeccion;
    private final AtomicLong contadorSesiones;
    private volatile long sesionActivaId;
    private volatile String promptPendiente = null;
    private volatile int delayPendienteMs = 0;
    private volatile String ultimoAgenteIniciado = null;

    public PanelAgente(ConfiguracionAPI config) {
        this(config, true);
    }

    @SuppressWarnings("this-escape")
    public PanelAgente(ConfiguracionAPI config, boolean iniciarConsola) {
        this.config = config;
        this.gestorLogging = GestorLoggingUnificado.crearConLogger(LOGGER);
        this.inyectorPty = crearInyectorPty();
        this.promptInicialEnviado = new AtomicBoolean(false);
        this.inicializacionPendiente = new AtomicBoolean(false);
        this.arranqueAgenteDespachado = new AtomicBoolean(false);
        this.consolaArrancando = new AtomicBoolean(false);
        this.contadorInyeccion = new AtomicLong(0);
        this.contadorSesiones = new AtomicLong(0);
        this.sesionActivaId = 0L;
        this.manejadorFocoPestania = new AtomicReference<>();
        this.manejadorCambioConfiguracion = new AtomicReference<>();

        setLayout(new BorderLayout(EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL));
        setBorder(BorderFactory.createEmptyBorder(
            EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL,
            EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL
        ));

        inicializarComponentesUI();
        aplicarTema();

        if (iniciarConsola) {
            iniciarConsola();
        }
    }

    public void establecerManejadorFocoPestania(Runnable manejador) {
        manejadorFocoPestania.set(manejador);

        if (manejador != null && inicializacionPendiente.get() && estaPanelListoParaInyeccion()) {
            if (inicializacionPendiente.compareAndSet(true, false)) {
                procesarInicializacionDiferida(0, sesionActivaId);
            }
        }
    }

    public void establecerManejadorCambioConfiguracion(Runnable manejador) {
        manejadorCambioConfiguracion.set(manejador);
    }

    private ExecutorService crearInyectorPty() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BurpIA-PTY-Injector");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService obtenerInyectorPty() {
        synchronized (lockInyectorPty) {
            if (inyectorPty == null || inyectorPty.isShutdown() || inyectorPty.isTerminated()) {
                inyectorPty = crearInyectorPty();
            }
            return inyectorPty;
        }
    }

    private void ejecutarEnInyectorPty(Runnable tarea) {
        if (tarea == null) {
            return;
        }
        try {
            obtenerInyectorPty().execute(tarea);
        } catch (RejectedExecutionException ex) {
            synchronized (lockInyectorPty) {
                inyectorPty = crearInyectorPty();
            }
            try {
                obtenerInyectorPty().execute(tarea);
            } catch (RejectedExecutionException retryEx) {
                registrarLog(Level.WARNING, I18nLogs.tr("No se pudo encolar tarea en inyector PTY"), retryEx);
            }
        }
    }

    private void reiniciarInyectorPty() {
        synchronized (lockInyectorPty) {
            if (inyectorPty != null) {
                inyectorPty.shutdownNow();
            }
            inyectorPty = crearInyectorPty();
        }
    }

    public void escribirComando(String comando) {
        if (Normalizador.esVacio(comando)) {
            return;
        }
        escribirComandoCrudo(OSUtils.prepararComando(comando));
    }

    public void escribirComandoCrudo(String comando) {
        if (comando == null || comando.isEmpty()) {
            return;
        }
        long sesionObjetivo = sesionActivaId;
        ejecutarEnInyectorPty(() -> escribirComandoCrudoSeguro(comando, sesionObjetivo));
    }

    private boolean escribirComandoCrudoSeguro(String comando) {
        try {
            if (escribirTextoViaTtyConnector(comando)) {
                return true;
            }
            return escribirTextoDirectoPTY(comando);
        } catch (Exception e) {
            registrarLog(Level.FINE, I18nLogs.tr("Error escribiendo comando crudo PTY"), e);
            return false;
        }
    }

    private boolean escribirComandoCrudoSeguro(String comando, long sesionObjetivo) {
        if (!esSesionVigente(sesionObjetivo)) {
            return false;
        }
        return escribirComandoCrudoSeguro(comando);
    }

    private boolean escribirTextoViaTtyConnector(String texto) {
        if (texto == null || texto.isEmpty() || ttyConnector == null) {
            return false;
        }

        try {
            if (!ttyConnector.isConnected()) {
                return false;
            }
            int longitud = texto.length();
            if (!requiereChunking(longitud)) {
                ttyConnector.write(texto);
                return true;
            }
            for (int i = 0; i < longitud; i += CHUNK_ESCRITURA_PTY) {
                int end = Math.min(longitud, i + CHUNK_ESCRITURA_PTY);
                ttyConnector.write(texto.substring(i, end));
                aplicarRetardoEntreChunks(end, longitud);
            }
            return true;
        } catch (Exception e) {
            registrarLog(Level.FINE, I18nLogs.tr("Error escribiendo por ttyConnector"), e);
            return false;
        }
    }

    private boolean escribirDirectoAlPTY(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || process == null || !process.isAlive()) {
            return false;
        }
        try {
            java.io.OutputStream os = process.getOutputStream();
            if (os == null) return false;
            os.write(bytes);
            os.flush();
            return true;
        } catch (Exception e) {
            registrarLog(Level.FINE, "Error escritura raw PTY", e);
            return false;
        }
    }

    private boolean escribirDirectoAlPTYSeguro(byte[] bytes, long sesionObjetivo) {
        if (!esSesionVigente(sesionObjetivo)) {
            return false;
        }
        return escribirDirectoAlPTY(bytes);
    }

    private boolean escribirTextoDirectoPTY(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false;
        }
        
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (process == null || !process.isAlive()) {
            registrarLog(Level.FINE, I18nLogs.tr("Escritura PTY omitida: proceso no disponible"));
            return false;
        }
        
        try {
            java.io.OutputStream os = process.getOutputStream();
            if (os == null) {
                registrarLog(Level.FINE, I18nLogs.tr("Escritura PTY omitida: stream de salida nulo"));
                return false;
            }
            int longitud = bytes.length;
            if (!requiereChunking(longitud)) {
                os.write(bytes);
                os.flush();
                return true;
            }
            for (int i = 0; i < longitud; i += CHUNK_ESCRITURA_PTY) {
                int end = Math.min(longitud, i + CHUNK_ESCRITURA_PTY);
                os.write(bytes, i, end - i);
                aplicarRetardoEntreChunks(end, longitud);
            }
            os.flush();
            return true;
        } catch (Exception e) {
            registrarLog(Level.FINE, "Error escritura directa PTY", e);
            return false;
        }
    }


    public void inyectarComando(String texto, int delayMs) {
        if (Normalizador.esVacio(texto)) {
            return;
        }

        asegurarConsolaIniciada();
        if (!estaPanelListoParaInyeccion()) {
            promptPendiente = texto;
            this.delayPendienteMs = delayMs;
            inicializacionPendiente.set(true);
            return;
        }

        ejecutarInyeccionConOpciones(texto, delayMs);
    }

    public void forzarInyeccionPromptInicial() {
        if (promptInicialEnviado.compareAndSet(false, true)) {
            String prompt = obtenerPromptPreflightFijo();
            inyectarComando(prompt, config.obtenerAgenteDelay());
        }
    }

    public void reinyectarPromptInicial() {
        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);
        forzarInyeccionPromptInicial();
    }

    public void reiniciar() {
        iniciarConsola();
    }

    private void reiniciarYSolicitarFoco() {
        reiniciar();
        solicitarFocoPestaniaAgente();
    }

    public void asegurarConsolaIniciada() {
        PtyProcess procesoActual = process;
        if (procesoActual != null && procesoActual.isAlive()) {
            return;
        }
        if (consolaArrancando.get()) {
            return;
        }
        iniciarConsola();
    }

    public void destruir() {
        invalidarSesionActiva();
        cerrarSesionActiva();
        reiniciarInyectorPty();
        UIUtils.ejecutarEnEdtYEsperar(() -> {
            cerrarTerminalWidget();
            terminalWidget = null;
            if (panelResultadosWrapper != null) {
                panelResultadosWrapper.removeAll();
                panelResultadosWrapper.revalidate();
                panelResultadosWrapper.repaint();
            }
        });
        consolaArrancando.set(false);
    }

    public void aplicarIdioma() {
        UIUtils.ejecutarEnEdtYEsperar(this::aplicarIdiomaEnEdt);
    }

    private void aplicarIdiomaEnEdt() {
        UIUtils.actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        UIUtils.actualizarTituloPanel(panelResultadosWrapper, I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO());

        refrescarTextosBotones();
        if (lblDelay != null) {
            lblDelay.setText(I18nUI.Consola.ETIQUETA_DELAY());
            lblDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        }
        if (spinnerDelay != null) {
            spinnerDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        }

        aplicarTema();
        revalidate();
        repaint();
    }

    public void aplicarTema() {
        Runnable aplicar = () -> {
            Color fondoPanel = EstilosUI.obtenerFondoPanel();

            setBackground(fondoPanel);
            if (panelControles != null) {
                panelControles.setBackground(fondoPanel);
                panelControles.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), 12, 16));
            }
            if (panelResultadosWrapper != null) {
                panelResultadosWrapper.setBackground(fondoPanel);
                panelResultadosWrapper.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO(), 3, 3));
            }

            if (lblDelay != null) {
                lblDelay.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));
            }

            if (terminalWidget != null && terminalWidget.getTerminalPanel() != null) {
                terminalWidget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            aplicar.run();
        } else {
            ejecutarEnEdt(aplicar);
        }
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

        btnReiniciar = crearBoton("🔄 " + I18nUI.Consola.BOTON_REINICIAR(),
            I18nUI.Tooltips.Agente.REINICIAR(), e -> reiniciarYSolicitarFoco());

        btnCtrlC = crearBoton("⚡ " + I18nUI.Consola.BOTON_CTRL_C(),
            I18nUI.Tooltips.Agente.CTRL_C(), e -> escribirComandoCrudo("\u0003"));

        btnInyectarPayload = crearBoton("💉 " + I18nUI.Consola.BOTON_INYECTAR_PAYLOAD(),
            I18nUI.Tooltips.Agente.INYECTAR_PAYLOAD(), e -> inyectarPayloadInicialManual());

        btnCambiarAgente = crearBoton("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO(),
            I18nUI.Tooltips.Agente.CAMBIAR_AGENTE_RAPIDO(), e -> cambiarAgenteRapido());

        btnAyudaAgente = crearBoton("❓",
            resolverTooltipAyudaAgente(), e -> abrirGuiaAgenteActual());

        panel.add(btnReiniciar);
        panel.add(btnCtrlC);
        panel.add(btnInyectarPayload);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(btnCambiarAgente);
        panel.add(btnAyudaAgente);

        lblDelay = new JLabel(I18nUI.Consola.ETIQUETA_DELAY());
        lblDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());

        spinnerDelay = new JSpinner(new SpinnerNumberModel(
            config.obtenerAgenteDelay(), null, null, ConfiguracionAPI.AGENTE_DELAY_PASO_MS));
        spinnerDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        spinnerDelay.setToolTipText(I18nUI.Tooltips.Configuracion.DELAY_PROMPT_AGENTE());
        spinnerDelay.setPreferredSize(new Dimension(80, 24));
        spinnerDelay.addChangeListener(e -> {
            int nuevoDelay = ((Number) spinnerDelay.getValue()).intValue();
            if (nuevoDelay == config.obtenerAgenteDelay()) {
                return;
            }
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
            AgenteTipo actual = obtenerAgenteActualSeguro();
            AgenteTipo destino = AgenteTipo.siguienteCircular(actual);

            String rutaBinario = resolverRutaBinario(destino);
            if (!OSUtils.existeBinario(rutaBinario)) {
                String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(destino.getNombreVisible(), rutaBinario);
                UIUtils.mostrarErrorBinarioAgenteNoEncontrado(
                    this,
                    destino.getNombreVisible(),
                    mensaje,
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
            
            reiniciarYSolicitarFoco();
            actualizarEstadoBotones();
            aplicarIdioma();

        } catch (Exception ex) {
            registrarLog(Level.WARNING, I18nLogs.tr("Error al cambiar de agente"), ex);
        }
    }

    public String obtenerUltimoAgenteIniciado() {
        return ultimoAgenteIniciado;
    }

    private AgenteTipo obtenerAgenteActualSeguro() {
        return AgenteTipo.desdeCodigo(config.obtenerTipoAgente(), AgenteTipo.porDefecto());
    }

    private String resolverTooltipAyudaAgente() {
        AgenteTipo agenteActual = obtenerAgenteActualSeguro();
        return I18nUI.Tooltips.Agente.GUIA_AGENTE(agenteActual.getNombreVisible());
    }

    private String resolverUrlGuiaAgenteActual() {
        return obtenerAgenteActualSeguro().getUrlDocPorIdioma(config.obtenerIdiomaUi());
    }

    private void abrirGuiaAgenteActual() {
        String url = resolverUrlGuiaAgenteActual();
        if (Normalizador.esVacio(url)) {
            return;
        }
        UIUtils.abrirUrlConFallbackInfo(
            this,
            I18nUI.Consola.TITULO_INFORMACION(),
            url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        );
    }

    private void actualizarEstadoBotones() {
        if (btnReiniciar != null) {
            btnReiniciar.setEnabled(true);
        }
        if (btnCtrlC != null) {
            btnCtrlC.setEnabled(true);
        }
        if (btnInyectarPayload != null) {
            btnInyectarPayload.setEnabled(true);
        }
        if (btnCambiarAgente != null) {
            btnCambiarAgente.setEnabled(true);
        }
        if (btnAyudaAgente != null) {
            btnAyudaAgente.setEnabled(true);
        }
        refrescarTextosBotones();
    }

    private void refrescarTextosBotones() {
        if (btnReiniciar != null) {
            btnReiniciar.setText("🔄 " + I18nUI.Consola.BOTON_REINICIAR());
            btnReiniciar.setToolTipText(I18nUI.Tooltips.Agente.REINICIAR());
        }
        if (btnCtrlC != null) {
            btnCtrlC.setText("⚡ " + I18nUI.Consola.BOTON_CTRL_C());
            btnCtrlC.setToolTipText(I18nUI.Tooltips.Agente.CTRL_C());
        }
        if (btnInyectarPayload != null) {
            btnInyectarPayload.setText("💉 " + I18nUI.Consola.BOTON_INYECTAR_PAYLOAD());
            btnInyectarPayload.setToolTipText(I18nUI.Tooltips.Agente.INYECTAR_PAYLOAD());
        }
        if (btnCambiarAgente != null) {
            btnCambiarAgente.setText("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO());
            btnCambiarAgente.setToolTipText(I18nUI.Tooltips.Agente.CAMBIAR_AGENTE_RAPIDO());
        }
        if (btnAyudaAgente != null) {
            btnAyudaAgente.setText("❓");
            btnAyudaAgente.setToolTipText(resolverTooltipAyudaAgente());
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

    private long activarNuevaSesion() {
        long nuevaSesion = contadorSesiones.incrementAndGet();
        sesionActivaId = nuevaSesion;
        return nuevaSesion;
    }

    private void invalidarSesionActiva() {
        sesionActivaId = contadorSesiones.incrementAndGet();
    }

    private boolean esSesionVigente(long sesionObjetivo) {
        return sesionObjetivo > 0 && sesionObjetivo == sesionActivaId;
    }

    private void recrearTerminalWidget() {
        cerrarTerminalWidget();
        terminalWidget = crearTerminalWidget();
        panelResultadosWrapper.removeAll();
        panelResultadosWrapper.add(terminalWidget, BorderLayout.CENTER);
        panelResultadosWrapper.revalidate();
        panelResultadosWrapper.repaint();
    }

    private void cerrarTerminalWidget() {
        if (terminalWidget == null) {
            return;
        }
        try {
            terminalWidget.stop();
        } catch (Exception e) {
            registrarLog(Level.FINE, I18nLogs.tr("Error deteniendo terminalWidget"), e);
        }
        try {
            terminalWidget.close();
        } catch (Exception e) {
            registrarLog(Level.FINE, I18nLogs.tr("Error cerrando terminalWidget"), e);
        }
    }

    private void cerrarSesionActiva() {
        arranqueAgenteDespachado.set(false);
        TtyConnector connectorActual = ttyConnector;
        ttyConnector = null;
        if (connectorActual != null) {
            try {
                connectorActual.close();
            } catch (Exception e) {
                registrarLog(Level.FINE, I18nLogs.tr("Error cerrando ttyConnector"), e);
            }
        }

        PtyProcess procesoActual = process;
        process = null;
        terminarProcesoSilencioso(procesoActual);

        promptInicialEnviado.set(false);
        promptPendiente = null;
        delayPendienteMs = 0;
        inicializacionPendiente.set(false);
    }

    private void terminarProcesoSilencioso(PtyProcess proceso) {
        if (proceso == null) {
            return;
        }
        try {
            if (proceso.isAlive()) {
                proceso.destroy();
                esperarProceso(proceso, 350L);
            }
            if (proceso.isAlive()) {
                proceso.destroyForcibly();
                esperarProceso(proceso, 1200L);
            }
        } catch (Exception e) {
            registrarLog(Level.FINE, I18nLogs.tr("Error cerrando proceso PTY"), e);
        }
    }

    private void esperarProceso(Process proceso, long timeoutMs) {
        if (proceso == null || timeoutMs <= 0) {
            return;
        }
        try {
            proceso.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    private void iniciarConsola() {
        if (!consolaArrancando.compareAndSet(false, true)) {
            return;
        }
        arranqueAgenteDespachado.set(false);
        invalidarSesionActiva();
        cerrarSesionActiva();
        try {
            UIUtils.ejecutarEnEdtYEsperar(this::recrearTerminalWidget);
        } catch (RuntimeException e) {
            consolaArrancando.set(false);
            manejarErrorPty(e);
            return;
        }

        long sesion = activarNuevaSesion();
        Thread arrancador = new Thread(() -> iniciarProcesoPty(sesion), "BurpIA-PTY-Starter-" + sesion);
        arrancador.setDaemon(true);
        arrancador.start();
    }

    public void enfocarTerminal() {
        ejecutarEnEdt(() -> {
            if (solicitarFocoTerminal()) {
                return;
            }
            Timer reintento = new Timer(DELAY_REINTENTO_FOCO_TERMINAL_MS, e -> {
                ((Timer) e.getSource()).stop();
                ejecutarEnEdt(this::solicitarFocoTerminal);
            });
            reintento.setRepeats(false);
            reintento.start();
        });
    }

    private boolean solicitarFocoTerminal() {
        if (terminalWidget == null || terminalWidget.getTerminalPanel() == null) {
            return false;
        }
        terminalWidget.getTerminalPanel().requestFocus();
        return terminalWidget.getTerminalPanel().requestFocusInWindow();
    }

    private JediTermWidget crearTerminalWidget() {
        JediTermWidget widget = new JediTermWidget(120, 24, new AgentTerminalSettingsProvider());
        widget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return widget;
    }

    private void iniciarProcesoPty(long sesionObjetivo) {
        try {
            if (!esSesionVigente(sesionObjetivo)) {
                consolaArrancando.set(false);
                return;
            }
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm-256color");

            String[] command = construirComandoShell();

            PtyProcess nuevoProceso = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(env)
                .start();

            if (!esSesionVigente(sesionObjetivo)) {
                terminarProcesoSilencioso(nuevoProceso);
                consolaArrancando.set(false);
                return;
            }

            TtyConnector rawConnector = new PtyProcessTtyConnector(nuevoProceso, StandardCharsets.UTF_8);
            TtyConnector nuevoConnector = new TtyConnector() {
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
                    rawConnector.write(bytes);
                }
                @Override
                public void write(String string) throws java.io.IOException {
                    rawConnector.write(string);
                }
                @Override
                public boolean isConnected() { return rawConnector.isConnected(); }
                @Override
                public int waitFor() throws InterruptedException { return rawConnector.waitFor(); }
                @Override
                public boolean ready() throws java.io.IOException { return rawConnector.ready(); }
            @Override
            @SuppressWarnings("deprecation")
            public void resize(java.awt.Dimension termSize, java.awt.Dimension pixelSize) {
                rawConnector.resize(termSize, pixelSize);
            }
            };

            process = nuevoProceso;
            ttyConnector = nuevoConnector;

            ejecutarEnEdt(() -> {
                if (esSesionVigente(sesionObjetivo) && terminalWidget != null) {
                    terminalWidget.setTtyConnector(nuevoConnector);
                    terminalWidget.start();
                    ultimoAgenteIniciado = config.obtenerTipoAgente();
                    programarInyeccionInicial(sesionObjetivo);
                }
                consolaArrancando.set(false);
            });

        } catch (Throwable t) {
            consolaArrancando.set(false);
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

    private void programarInyeccionInicial(long sesionObjetivo) {
        AgenteTipo tipoActual = AgenteTipo.desdeCodigo(config.obtenerTipoAgente(), AgenteTipo.porDefecto());
        String prompt = obtenerPromptPreflightFijo();
        int usuarioDelay = config.obtenerAgenteDelay();
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(tipoActual);
        String comandoArranque = resolverComandoArranque(tipoActual);

        registrarLog(Level.INFO, I18nLogs.tr("Iniciando secuencia de inyeccion automatica de agente..."));

        ejecutarEnInyectorPty(() -> {
            if (!esSesionVigente(sesionObjetivo)) {
                return;
            }
            dormirSilencioso(DELAY_INICIO_BINARIO_MS);
            if (!esSesionVigente(sesionObjetivo)) {
                return;
            }

            if (!escribirComandoConReintento(
                OSUtils.prepararComando(comandoArranque),
                sesionObjetivo,
                INTENTOS_ENVIO_ARRANQUE,
                DELAY_REINTENTO_ARRANQUE_MS
            )) {
                registrarLog(Level.WARNING, I18nLogs.tr(
                    "No se pudo enviar comando de arranque del agente tras reintentos"));
                return;
            }

            arranqueAgenteDespachado.set(true);

            if (inicializacionPendiente.getAndSet(false)) {
                int delayDiferida = Math.max(0, usuarioDelay) + DELAY_DIFERIDA_POST_ARRANQUE_MS;
                procesarInicializacionDiferida(delayDiferida, sesionObjetivo);
            }

            if (Normalizador.esVacio(prompt)) {
                return;
            }
            if (!promptInicialEnviado.compareAndSet(false, true)) {
                return;
            }

            inyectarComandoConRetraso(
                prompt,
                Math.max(0, usuarioDelay),
                opciones,
                "AUTO_INIT",
                sesionObjetivo
            );
        });
    }

    private boolean escribirComandoConReintento(
        String comando,
        long sesionObjetivo,
        int intentosMaximos,
        int delayEntreIntentosMs
    ) {
        int intentos = Math.max(1, intentosMaximos);
        for (int intento = 1; intento <= intentos; intento++) {
            if (!esSesionVigente(sesionObjetivo)) {
                return false;
            }
            if (escribirComandoCrudoSeguro(comando, sesionObjetivo)) {
                return true;
            }
            if (intento < intentos) {
                dormirSilencioso(Math.max(0, delayEntreIntentosMs));
            }
        }
        return false;
    }

    private String resolverComandoArranque(AgenteTipo tipoActual) {
        return resolverRutaBinario(tipoActual);
    }

    private String resolverRutaBinario(AgenteTipo tipo) {
        String binarioConfig = config.obtenerRutaBinarioAgente(tipo != null ? tipo.name() : config.obtenerTipoAgente());
        
        if (Normalizador.esVacio(binarioConfig)) {
            return tipo != null ? tipo.getRutaPorDefecto() : AgenteTipo.porDefecto().getRutaPorDefecto();
        }

        return OSUtils.expandirRuta(binarioConfig.trim());
    }

    private boolean estaPanelListoParaInyeccion() {
        return arranqueAgenteDespachado.get()
            && manejadorFocoPestania.get() != null
            && terminalWidget != null
            && terminalWidget.getTerminalPanel() != null
            && process != null
            && process.isAlive();
    }

    private void procesarInicializacionDiferida(int delayMinimoMs, long sesionObjetivo) {
        if (!arranqueAgenteDespachado.get() || !esSesionVigente(sesionObjetivo)) {
            return;
        }
        if (promptPendiente != null) {
            String prompt = promptPendiente;
            int delay = Math.max(this.delayPendienteMs, Math.max(0, delayMinimoMs));

            promptPendiente = null;
            this.delayPendienteMs = 0;

            inyectarComandoConRetraso(prompt, delay, AgentRuntimeOptions.cargar(config.obtenerTipoAgente()), "API_OR_UI", sesionObjetivo);
        }
    }

    private void ejecutarInyeccionConOpciones(String texto, int delayPendienteMsUsuario) {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(config.obtenerTipoAgente());
        inyectarComandoConRetraso(texto, delayPendienteMsUsuario, opciones, "API_OR_UI", sesionActivaId);
    }

    private void inyectarComandoConRetraso(
        String texto,
        int delayMs,
        AgentRuntimeOptions.EnterOptions opciones,
        String origen,
        long sesionObjetivo
    ) {
        ejecutarEnEdt(() -> {
            if (!esSesionVigente(sesionObjetivo)) {
                return;
            }
            OSUtils.cerrarVentanaAjustes();
            if (delayMs > 0) {
                registrarLog(Level.INFO, I18nLogs.tr("Esperando el delay establecido por el usuario (" + delayMs + " ms) antes de la inyeccion..."));
                new Timer(delayMs, ev -> {
                    ((Timer) ev.getSource()).stop();
                    if (esSesionVigente(sesionObjetivo)) {
                        aplicarEscrituraDirecta(texto, opciones, origen, sesionObjetivo);
                    }
                }).start();
            } else {
                aplicarEscrituraDirecta(texto, opciones, origen, sesionObjetivo);
            }
        });
    }

    private void aplicarEscrituraDirecta(
        String texto,
        AgentRuntimeOptions.EnterOptions opciones,
        String origen,
        long sesionObjetivo
    ) {
        String ansiStart = "\u001b[200~";
        String ansiEnd = "\u001b[201~";

        String payloadConBrackets = ansiStart + texto + ansiEnd;
        long injectionId = contadorInyeccion.incrementAndGet();
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(opciones.tipoAgente());

        ejecutarEnInyectorPty(() -> {
            if (!esSesionVigente(sesionObjetivo)) {
                return;
            }
            registrarLog(Level.INFO, I18nLogs.trTecnico(
                "[ENTER-FLOW] id=" + injectionId +
                    " origin=" + origen +
                    " agent=" + opciones.tipoAgente().name() +
                    " os=" + describirPlataformaActual() +
                    " strategyFinal=" + secuencia.descripcion()
            ));
            if (!escribirComandoCrudoSeguro(payloadConBrackets, sesionObjetivo)) {
                return;
            }
            registrarLog(Level.INFO, I18nLogs.tr("Payload en bufer usando escritura directa (tty stream con bracketed paste). Esperando confirmacion..."));
            dormirSilencioso(opciones.delaySubmitPostPasteMs());
            enviarSecuenciaSubmit(opciones, secuencia, injectionId, origen, sesionObjetivo);
        });
    }

    private void enviarSecuenciaSubmit(
        AgentRuntimeOptions.EnterOptions opciones,
        SubmitSequenceFactory.SubmitSequence secuencia,
        long injectionId,
        String origen,
        long sesionObjetivo
    ) {
        if (secuencia == null) return;
        if (!esSesionVigente(sesionObjetivo)) {
            return;
        }

        boolean envioExitoso = true;
        for (int i = 0; i < secuencia.repeticiones(); i++) {
            if (!esSesionVigente(sesionObjetivo)) {
                return;
            }
            if (OSUtils.esMac() && "\r".equals(secuencia.payload()) && i == 0) {
                if (!escribirDirectoAlPTYSeguro(new byte[]{13}, sesionObjetivo)) {
                    if (!escribirComandoCrudoSeguro(secuencia.payload(), sesionObjetivo)) {
                        envioExitoso = false;
                        break;
                    }
                }
            } else {
                if (!escribirComandoCrudoSeguro(secuencia.payload(), sesionObjetivo)) {
                    envioExitoso = false;
                    break;
                }
            }

            if (i + 1 < secuencia.repeticiones()) {
                dormirSilencioso(secuencia.delayEntreEnviosMs());
            }
        }

        if (!envioExitoso && secuencia.getFallback() != null) {
            registrarLog(Level.WARNING, I18nLogs.trTecnico(
                "[ENTER-FALLBACK] id=" + injectionId +
                    " origin=" + origen +
                    " motivo=fallo_escritura payload='" + escaparControl(secuencia.payload()) + "'"
            ));
            dormirSilencioso(secuencia.delayEntreEnviosMs() > 0 ? secuencia.delayEntreEnviosMs() : 100);
            enviarSecuenciaSubmit(opciones, secuencia.getFallback(), injectionId, origen + "->FALLBACK", sesionObjetivo);
            return;
        }

        if (!envioExitoso) {
            registrarLog(Level.WARNING, I18nLogs.trTecnico(
                "[ENTER-RESULT] id=" + injectionId +
                    " origin=" + origen +
                    " outcome=failed reason=submit-write-failed-no-fallback"
            ));
            return;
        }

        registrarLog(Level.INFO, I18nLogs.tr("Se ha despachado la secuencia VK_ENTER") + " [id=" + injectionId + ", " + secuencia.descripcion() + "]");
        registrarLog(Level.INFO, I18nLogs.trTecnico(
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

    private boolean requiereChunking(int longitud) {
        return longitud > CHUNK_ESCRITURA_PTY;
    }

    private void aplicarRetardoEntreChunks(int finChunk, int total) {
        if (finChunk < total) {
            dormirSilencioso(DELAY_ENTRE_CHUNKS_PTY_MS);
        }
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

    private String describirPlataformaActual() {
        return SubmitSequenceFactory.Plataforma.desdeSistemaActual()
            .name()
            .toLowerCase(java.util.Locale.ROOT);
    }

    private void manejarErrorPty(Throwable t) {
        registrarLog(Level.SEVERE, I18nLogs.tr("Error nativo iniciando Consola PTY"), t);
        String mensaje = t.getMessage() != null ? t.getMessage() : I18nLogs.tr("Error desconocido PTY");
        ejecutarEnEdt(() -> {
            UIUtils.mostrarError(PanelAgente.this, I18nUI.Consola.TITULO_ERROR_PTY(), mensaje);
        });
    }

    private void solicitarFocoPestaniaAgente() {
        Runnable focoHandler = manejadorFocoPestania.get();
        if (focoHandler != null) {
            focoHandler.run();
            return;
        }
        enfocarTerminal();
    }

    public void inyectarPayloadInicialManual() {
        solicitarFocoPestaniaAgente();

        String prompt = obtenerPromptPreflightFijo();

        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);

        inyectarComando(prompt, 0);

        registrarLog(Level.INFO, I18nLogs.tr("Payload inicial encolado para inyeccion manual por el usuario"));
    }

    private String obtenerPromptPreflightFijo() {
        return config.obtenerAgentePreflightPrompt();
    }

    private void registrarLog(Level nivel, String mensaje) {
        gestorLogging.log(nivel, mensaje);
    }

    private void registrarLog(Level nivel, String mensaje, Throwable error) {
        gestorLogging.log(nivel, mensaje, error);
    }
}
