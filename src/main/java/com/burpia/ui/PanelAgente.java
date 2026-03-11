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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelAgente extends JPanel {

    private static final String ORIGEN_LOG = "PanelAgente";
    private final GestorLoggingUnificado gestorLogging;

    private static final int DELAY_INICIO_BINARIO_MS = 800;
    private static final int DELAY_DIFERIDA_POST_ARRANQUE_MS = 180;
    private static final int CHUNK_ESCRITURA_PTY = 128;
    private static final int DELAY_ENTRE_CHUNKS_PTY_MS = 10;
    private static final int INTENTOS_ENVIO_ARRANQUE = 6;
    private static final int DELAY_REINTENTO_ARRANQUE_MS = 200;
    private static final int DELAY_REINTENTO_FOCO_TERMINAL_MS = 120;
    private static final int DELAY_ENTRE_INYECCIONES_PENDIENTES_MS = 25;

    private static final int TERMINAL_ANCHO_CARACTERES = 120;
    private static final int TERMINAL_ALTO_LINEAS = 24;
    private static final int TERMINAL_MARGEN = 4;

    private static final int MARGEN_BORDE_TITULO_GRANDE = 12;
    private static final int MARGEN_BORDE_TITULO_PEQUEÑO = 3;

    private static final String SHELL_POR_DEFECTO = "/bin/bash";
    private static final String PARAMETRO_SHELL_LOGIN = "--login";

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
    private final Queue<InyeccionPendiente> inyeccionesPendientes;
    private volatile long sesionActivaId;
    private volatile String ultimoAgenteIniciado = null;
    private Timer reintentoFocoTimer;

    public PanelAgente(ConfiguracionAPI config) {
        this(config, true);
    }

    @SuppressWarnings("this-escape")
    public PanelAgente(ConfiguracionAPI config, boolean iniciarConsola) {
        this.config = config;
        this.gestorLogging = GestorLoggingUnificado.crearMinimal(null, null);
        this.inyectorPty = crearInyectorPty();
        this.promptInicialEnviado = new AtomicBoolean(false);
        this.inicializacionPendiente = new AtomicBoolean(false);
        this.arranqueAgenteDespachado = new AtomicBoolean(false);
        this.consolaArrancando = new AtomicBoolean(false);
        this.contadorInyeccion = new AtomicLong(0);
        this.contadorSesiones = new AtomicLong(0);
        this.inyeccionesPendientes = new ConcurrentLinkedQueue<>();
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
            Thread t = new Thread(r, "BurpIA-PTY-CommandInjector");
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
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo encolar tarea en inyector PTY"), retryEx);
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

    /**
     * Escribe un comando en la terminal, agregando automáticamente el carácter de fin de línea
     * apropiado para el sistema operativo actual.
     *
     * @param comando El comando a enviar a la terminal
     */
    public void escribirComando(String comando) {
        if (Normalizador.esVacio(comando)) {
            return;
        }
        escribirComandoCrudo(OSUtils.prepararComando(comando));
    }

    /**
     * Escribe un comando crudo directamente a la terminal sin modificar su contenido.
     * Útil para enviar caracteres de control como Ctrl+C (\u0003) o secuencias ANSI.
     *
     * @param comando El comando crudo a enviar (sin EOL automático)
     */
    public void escribirComandoCrudo(String comando) {
        if (comando == null) {
            return;
        }
        long sesionObjetivo = sesionActivaId;
        ejecutarEnInyectorPty(() -> escribirComandoCrudoSeguro(comando, sesionObjetivo));
    }

    private static final long SESION_IGNORAR_VALIDACION = -1L;

    private boolean escribirComandoCrudoSeguro(String comando) {
        return escribirComandoCrudoSeguro(comando, SESION_IGNORAR_VALIDACION);
    }

    private boolean escribirComandoCrudoSeguro(String comando, long sesionObjetivo) {
        if (!esSesionVigenteONIgnorada(sesionObjetivo)) {
            return false;
        }
        try {
            if (escribirTextoViaTtyConnector(comando)) {
                return true;
            }
            return escribirTextoDirectoPTY(comando);
        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Error escribiendo comando crudo PTY"));
            return false;
        }
    }

    private boolean escribirTextoViaTtyConnector(String texto) {
        if (texto == null || ttyConnector == null) {
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
            escribirEnChunks(texto, (start, end) -> {
                try {
                    ttyConnector.write(texto.substring(start, end));
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (Exception e) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.tr("Error escribiendo por ttyConnector"));
            return false;
        }
    }

    private boolean escribirDirectoAlPTY(byte[] bytes) {
        return escribirDirectoAlPTY(bytes, SESION_IGNORAR_VALIDACION);
    }

    private boolean escribirDirectoAlPTY(byte[] bytes, long sesionObjetivo) {
        if (!esSesionVigenteONIgnorada(sesionObjetivo)) {
            return false;
        }
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
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.tr("Error escritura raw PTY"));
            return false;
        }
    }

    private boolean escribirTextoDirectoPTY(String texto) {
        // Nota: NO usar Normalizador.esVacio() porque elimina caracteres de control
        // como Ctrl+C (\u0003) que son necesarios para comandos crudos
        if (texto == null) {
            return false;
        }

        // Verificar proceso antes de crear el array de bytes (optimización)
        if (process == null || !process.isAlive()) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Escritura PTY omitida: proceso no disponible"));
            return false;
        }

        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);

        try {
            java.io.OutputStream os = process.getOutputStream();
            if (os == null) {
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Escritura PTY omitida: stream de salida nulo"));
                return false;
            }
            escribirEnChunks(bytes, (offset, length) -> {
                try {
                    os.write(bytes, offset, length);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            });
            os.flush();
            return true;
        } catch (Exception e) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.tr("Error escritura directa PTY"));
            return false;
        }
    }

    /**
     * Escribe texto en chunks para evitar sobrecargar el PTY.
     * Centraliza la lógica de chunking que se repite en 2 métodos.
     * El consumer recibe (start, end) para extraer el substring.
     */
    private void escribirEnChunks(String texto, java.util.function.BiConsumer<Integer, Integer> escritor) {
        int longitud = texto.length();
        if (!requiereChunking(longitud)) {
            escritor.accept(0, longitud);
            return;
        }
        for (int i = 0; i < longitud; i += CHUNK_ESCRITURA_PTY) {
            int end = Math.min(longitud, i + CHUNK_ESCRITURA_PTY);
            escritor.accept(i, end);
            aplicarRetardoEntreChunks(end, longitud);
        }
    }

    /**
     * Escribe bytes en chunks para evitar sobrecargar el PTY.
     * Sobrecarga para arrays de bytes.
     */
    private void escribirEnChunks(byte[] bytes, java.util.function.BiConsumer<Integer, Integer> escritor) {
        int longitud = bytes.length;
        if (!requiereChunking(longitud)) {
            escritor.accept(0, longitud);
            return;
        }
        for (int i = 0; i < longitud; i += CHUNK_ESCRITURA_PTY) {
            int end = Math.min(longitud, i + CHUNK_ESCRITURA_PTY);
            escritor.accept(i, end - i);
            aplicarRetardoEntreChunks(end, longitud);
        }
    }

    /**
     * Detiene el Timer de reintentos de foco si está corriendo.
     * Previene memory leaks de múltiples Timers.
     */
    private void detenerReintentoFocoTimer() {
        if (reintentoFocoTimer != null && reintentoFocoTimer.isRunning()) {
            reintentoFocoTimer.stop();
            reintentoFocoTimer = null;
        }
    }

    /**
     * Ejecuta una acción en el EDT, o directamente si ya está en el EDT.
     * Evita validaciones repetitivas de SwingUtilities.isEventDispatchThread().
     */
    private void ejecutarEnEdtSiEsNecesario(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
        } else {
            ejecutarEnEdt(accion);
        }
    }


    /**
     * Inyecta un comando en la terminal del agente con un delay opcional.
     * Si la consola no está lista, el comando se encola para ejecutarse cuando esté disponible.
     *
     * @param texto   El texto/comando a inyectar en la terminal
     * @param delayMs Milisegundos de espera antes de la inyección (0 para inyección inmediata)
     */
    public void inyectarComando(String texto, int delayMs) {
        if (Normalizador.esVacio(texto)) {
            return;
        }

        asegurarConsolaIniciada();
        if (!estaPanelListoParaInyeccion()) {
            encolarInyeccionPendiente(texto, delayMs);
            inicializacionPendiente.set(true);
            return;
        }

        ejecutarInyeccionConOpciones(texto, delayMs);
    }

    /**
     * Fuerza la inyección del prompt inicial (preflight) configurado.
     * Solo se ejecuta una vez por sesión de terminal.
     */
    public void forzarInyeccionPromptInicial() {
        String prompt = obtenerPromptPreflightDisponible();
        if (prompt == null) {
            return;
        }
        if (promptInicialEnviado.compareAndSet(false, true)) {
            inyectarComando(prompt, config.obtenerAgenteDelay());
        }
    }

    /**
     * Reinicializa la inyección del prompt inicial, permitiendo que se ejecute nuevamente.
     */
    public void reinyectarPromptInicial() {
        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);
        forzarInyeccionPromptInicial();
    }

    /**
     * Reinicia la terminal del agente, destruyendo el proceso PTY actual y creando uno nuevo.
     */
    public void reiniciar() {
        iniciarConsola();
    }

    private void reiniciarYSolicitarFoco() {
        reiniciar();
        solicitarFocoPestaniaAgente();
    }

    /**
     * Asegura que la consola del agente esté iniciada.
     * Si ya está corriendo, no hace nada. Si no, la inicia.
     */
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

    /**
     * Libera todos los recursos asociados al panel del agente.
     * Detiene el proceso PTY, cierra conectores y limpia el widget de terminal.
     * Este método debe llamarse cuando el panel ya no se vaya a utilizar más.
     */
    public void destruir() {
        detenerReintentoFocoTimer();
        cerrarYCrearNuevaSesion();
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

    /**
     * Aplica el idioma configurado a todos los elementos de la interfaz del panel.
     * Debe llamarse cuando se cambia el idioma de la configuración.
     */
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
                panelControles.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), MARGEN_BORDE_TITULO_GRANDE, 16));
            }
            if (panelResultadosWrapper != null) {
                panelResultadosWrapper.setBackground(fondoPanel);
                panelResultadosWrapper.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO(), MARGEN_BORDE_TITULO_PEQUEÑO, MARGEN_BORDE_TITULO_PEQUEÑO));
            }

            if (lblDelay != null) {
                lblDelay.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));
            }

            if (terminalWidget != null && terminalWidget.getTerminalPanel() != null) {
                terminalWidget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(TERMINAL_MARGEN, TERMINAL_MARGEN, TERMINAL_MARGEN, TERMINAL_MARGEN));
            }
        };

        ejecutarEnEdtSiEsNecesario(aplicar);
    }
    
    private void inicializarComponentesUI() {
        panelControles = crearPanelControles();
        panelResultadosWrapper = crearPanelResultados();
        
        add(panelControles, BorderLayout.NORTH);
        add(panelResultadosWrapper, BorderLayout.CENTER);
    }

    private JPanel crearPanelControles() {
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

        JPanel panel = new PanelFilasResponsive(
            750,
            EstilosUI.ESPACIADO_COMPONENTES,
            8,
            List.of(
                List.of(btnCtrlC, btnReiniciar, btnInyectarPayload, btnCambiarAgente, btnAyudaAgente),
                List.of(lblDelay, spinnerDelay)
            )
        );
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), MARGEN_BORDE_TITULO_GRANDE, 16));

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
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al cambiar de agente"), ex);
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

    /**
     * DRY: Helper para ejecutar acción en botón solo si no es null.
     */
    private static void siBotonPresente(JButton boton, Consumer<JButton> accion) {
        if (boton != null) {
            accion.accept(boton);
        }
    }

    /**
     * DRY: Lista de todos los botones de control del agente.
     */
    private List<JButton> obtenerBotonesControl() {
        return Arrays.asList(btnReiniciar, btnCtrlC, btnInyectarPayload, btnCambiarAgente, btnAyudaAgente);
    }

    private void actualizarEstadoBotones() {
        for (JButton boton : obtenerBotonesControl()) {
            siBotonPresente(boton, b -> b.setEnabled(true));
        }
        refrescarTextosBotones();
    }

    private void refrescarTextosBotones() {
        siBotonPresente(btnReiniciar, b -> {
            b.setText("🔄 " + I18nUI.Consola.BOTON_REINICIAR());
            b.setToolTipText(I18nUI.Tooltips.Agente.REINICIAR());
        });
        siBotonPresente(btnCtrlC, b -> {
            b.setText("⚡ " + I18nUI.Consola.BOTON_CTRL_C());
            b.setToolTipText(I18nUI.Tooltips.Agente.CTRL_C());
        });
        siBotonPresente(btnInyectarPayload, b -> {
            b.setText("💉 " + I18nUI.Consola.BOTON_INYECTAR_PAYLOAD());
            b.setToolTipText(I18nUI.Tooltips.Agente.INYECTAR_PAYLOAD());
        });
        siBotonPresente(btnCambiarAgente, b -> {
            b.setText("🔀 " + I18nUI.Consola.BOTON_CAMBIAR_AGENTE_GENERICO());
            b.setToolTipText(I18nUI.Tooltips.Agente.CAMBIAR_AGENTE_RAPIDO());
        });
        siBotonPresente(btnAyudaAgente, b -> {
            b.setText("❓");
            b.setToolTipText(resolverTooltipAyudaAgente());
        });
    }

    private JPanel crearPanelResultados() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_PANEL_AGENTE_GENERICO(), MARGEN_BORDE_TITULO_PEQUEÑO, MARGEN_BORDE_TITULO_PEQUEÑO));
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

    private void cerrarYCrearNuevaSesion() {
        sesionActivaId = contadorSesiones.incrementAndGet();
    }

    private boolean esSesionVigente(long sesionObjetivo) {
        return sesionObjetivo > 0 && sesionObjetivo == sesionActivaId;
    }

    /**
     * DRY: Verifica sesión vigente o si se debe ignorar la validación.
     * Útil para métodos que tienen versiones con y sin validación de sesión.
     */
    private boolean esSesionVigenteONIgnorada(long sesionObjetivo) {
        return sesionObjetivo == SESION_IGNORAR_VALIDACION || esSesionVigente(sesionObjetivo);
    }

    private void recrearTerminalWidget() {
        cerrarTerminalWidget();
        terminalWidget = crearTerminalWidget();
        panelResultadosWrapper.removeAll();
        panelResultadosWrapper.add(terminalWidget, BorderLayout.CENTER);
        panelResultadosWrapper.revalidate();
        panelResultadosWrapper.repaint();
    }

    /**
     * DRY: Ejecuta una operación que puede lanzar excepciones, manejándolas silenciosamente.
     */
    private void ejecutarSilencioso(Runnable operacion, String mensajeError) {
        try {
            operacion.run();
        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr(mensajeError));
        }
    }

    private void cerrarTerminalWidget() {
        if (terminalWidget == null) {
            return;
        }
        ejecutarSilencioso(() -> terminalWidget.stop(), "Error deteniendo terminalWidget");
        ejecutarSilencioso(() -> terminalWidget.close(), "Error cerrando terminalWidget");
    }

    private void cerrarSesionActiva() {
        arranqueAgenteDespachado.set(false);
        TtyConnector connectorActual = ttyConnector;
        ttyConnector = null;
        if (connectorActual != null) {
            ejecutarSilencioso(connectorActual::close, "Error cerrando ttyConnector");
        }

        PtyProcess procesoActual = process;
        process = null;
        terminarProcesoSilencioso(procesoActual);

        promptInicialEnviado.set(false);
        inyeccionesPendientes.clear();
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
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Error cerrando proceso PTY"));
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
        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("Error esperando cierre de proceso PTY"));
        }
    }

    private void iniciarConsola() {
        if (!consolaArrancando.compareAndSet(false, true)) {
            return;
        }
        arranqueAgenteDespachado.set(false);
        cerrarYCrearNuevaSesion();
        cerrarSesionActiva();
        try {
            UIUtils.ejecutarEnEdtYEsperar(this::recrearTerminalWidget);
        } catch (RuntimeException e) {
            consolaArrancando.set(false);
            manejarErrorPty(e);
            return;
        }

        long sesion = activarNuevaSesion();
        Thread arrancador = new Thread(() -> iniciarProcesoPty(sesion), "BurpIA-PTY-ShellStarter-" + sesion);
        arrancador.setDaemon(true);
        arrancador.start();
    }

    public void enfocarTerminal() {
        detenerReintentoFocoTimer();

        ejecutarEnEdt(() -> {
            if (solicitarFocoTerminal()) {
                return;
            }
            reintentoFocoTimer = new Timer(DELAY_REINTENTO_FOCO_TERMINAL_MS, e -> {
                detenerReintentoFocoTimer();
                ejecutarEnEdt(this::solicitarFocoTerminal);
            });
            reintentoFocoTimer.setRepeats(false);
            reintentoFocoTimer.start();
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
        JediTermWidget widget = new JediTermWidget(TERMINAL_ANCHO_CARACTERES, TERMINAL_ALTO_LINEAS, new AgentTerminalSettingsProvider());
        widget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(TERMINAL_MARGEN, TERMINAL_MARGEN, TERMINAL_MARGEN, TERMINAL_MARGEN));
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

        } catch (Exception e) {
            consolaArrancando.set(false);
            manejarErrorPty(e);
        }
    }

    private String[] construirComandoShell() {
        if (OSUtils.esWindows()) {
            return new String[]{"cmd.exe"};
        }

        return new String[]{resolverShellEjecutable(System.getenv("SHELL")), PARAMETRO_SHELL_LOGIN};
    }

    private void programarInyeccionInicial(long sesionObjetivo) {
        AgenteTipo tipoActual = AgenteTipo.desdeCodigo(config.obtenerTipoAgente(), AgenteTipo.porDefecto());
        String prompt = obtenerPromptPreflightDisponible();
        int usuarioDelay = config.obtenerAgenteDelay();
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(tipoActual);
        String comandoArranque = resolverRutaBinario(tipoActual);

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Iniciando secuencia de inyeccion automatica de agente..."));

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
                gestorLogging.warning(ORIGEN_LOG, I18nLogs.tr(
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

    private String resolverRutaBinario(AgenteTipo tipo) {
        String binarioConfig = config.obtenerRutaBinarioAgente(tipo != null ? tipo.name() : config.obtenerTipoAgente());
        
        if (Normalizador.esVacio(binarioConfig)) {
            return OSUtils.normalizarComandoParaShell(
                tipo != null ? tipo.getRutaPorDefecto() : AgenteTipo.porDefecto().getRutaPorDefecto()
            );
        }

        return OSUtils.normalizarComandoParaShell(binarioConfig.trim());
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
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(config.obtenerTipoAgente());
        List<InyeccionPendiente> pendientes = extraerInyeccionesPendientes();
        for (int i = 0; i < pendientes.size(); i++) {
            InyeccionPendiente pendiente = pendientes.get(i);
            int delay = Math.max(pendiente.delayMs(), Math.max(0, delayMinimoMs))
                + (i * DELAY_ENTRE_INYECCIONES_PENDIENTES_MS);
            inyectarComandoConRetraso(pendiente.texto(), delay, opciones, "API_OR_UI", sesionObjetivo);
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
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Esperando el delay establecido por el usuario") + " (" + delayMs + " ms) " + I18nLogs.tr("antes de la inyeccion") + "...");
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
            gestorLogging.info(ORIGEN_LOG, I18nLogs.trTecnico(
                "[ENTER-FLOW] id=" + injectionId +
                    " origin=" + origen +
                    " agent=" + opciones.tipoAgente().name() +
                    " os=" + describirPlataformaActual() +
                    " strategyFinal=" + secuencia.descripcion()
            ));
            if (!escribirComandoCrudoSeguro(payloadConBrackets, sesionObjetivo)) {
                return;
            }
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Payload en bufer usando escritura directa (tty stream con bracketed paste). Esperando confirmacion..."));
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
                if (!escribirDirectoAlPTY(new byte[]{13}, sesionObjetivo)) {
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
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.trTecnico(
                "[ENTER-FALLBACK] id=" + injectionId +
                    " origin=" + origen +
                    " motivo=fallo_escritura payload='" + escaparControl(secuencia.payload()) + "'"
            ));
            dormirSilencioso(secuencia.delayEntreEnviosMs() > 0 ? secuencia.delayEntreEnviosMs() : 100);
            enviarSecuenciaSubmit(opciones, secuencia.getFallback(), injectionId, origen + "->FALLBACK", sesionObjetivo);
            return;
        }

        if (!envioExitoso) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.trTecnico(
                "[ENTER-RESULT] id=" + injectionId +
                    " origin=" + origen +
                    " outcome=failed reason=submit-write-failed-no-fallback"
            ));
            return;
        }

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Se ha despachado la secuencia VK_ENTER") + " [id=" + injectionId + ", " + secuencia.descripcion() + "]");
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trTecnico(
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

    private static final Map<Class<? extends Exception>, String> MENSAJES_ERROR_PTY = Map.of(
        java.io.IOException.class, "Error de E/S iniciando proceso PTY",
        SecurityException.class, "Error de seguridad iniciando proceso PTY",
        InterruptedException.class, "Operación interrumpida al iniciar PTY"
    );

    private void manejarErrorPty(Exception e) {
        String mensajeLog = MENSAJES_ERROR_PTY.getOrDefault(e.getClass(), "Error inesperado iniciando Consola PTY");
        gestorLogging.error(ORIGEN_LOG, I18nLogs.tr(mensajeLog), e);

        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        String mensaje = e.getMessage() != null ? e.getMessage() : I18nLogs.tr("Error desconocido PTY");
        ejecutarEnEdt(() -> UIUtils.mostrarError(PanelAgente.this, I18nUI.Consola.TITULO_ERROR_PTY(), mensaje));
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

        String prompt = obtenerPromptPreflightDisponible();

        promptInicialEnviado.set(false);
        inicializacionPendiente.set(false);

        if (prompt == null) {
            return;
        }

        inyectarComando(prompt, 0);
        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Payload inicial encolado para inyeccion manual por el usuario"));
    }

    private String obtenerPromptPreflightFijo() {
        return config.obtenerAgentePreflightPrompt();
    }

    private String obtenerPromptPreflightDisponible() {
        String prompt = obtenerPromptPreflightFijo();
        return Normalizador.noEsVacio(prompt) ? prompt : null;
    }

    private void encolarInyeccionPendiente(String texto, int delayMs) {
        inyeccionesPendientes.add(new InyeccionPendiente(texto, Math.max(0, delayMs)));
    }

    private List<InyeccionPendiente> extraerInyeccionesPendientes() {
        List<InyeccionPendiente> pendientes = new java.util.ArrayList<>();
        InyeccionPendiente pendiente;
        while ((pendiente = inyeccionesPendientes.poll()) != null) {
            pendientes.add(pendiente);
        }
        return pendientes;
    }

    private String resolverShellEjecutable(String shellConfigurado) {
        String shell = Normalizador.noEsVacio(shellConfigurado) ? shellConfigurado : SHELL_POR_DEFECTO;
        String shellEjecutable = OSUtils.resolverEjecutableComando(shell);
        if (Normalizador.noEsVacio(shellEjecutable) && OSUtils.existeBinario(shellEjecutable)) {
            return shellEjecutable;
        }

        gestorLogging.warning(
            ORIGEN_LOG,
            I18nLogs.tr("Shell no encontrado") + ": "
                + I18nLogs.trTecnico(shell)
                + ", "
                + I18nLogs.tr("usando fallback a")
                + " "
                + I18nLogs.trTecnico(SHELL_POR_DEFECTO)
        );
        return SHELL_POR_DEFECTO;
    }

    private static final class InyeccionPendiente {
        private final String texto;
        private final int delayMs;

        private InyeccionPendiente(String texto, int delayMs) {
            this.texto = texto;
            this.delayMs = delayMs;
        }

        private String texto() {
            return texto;
        }

        private int delayMs() {
            return delayMs;
        }
    }
}
