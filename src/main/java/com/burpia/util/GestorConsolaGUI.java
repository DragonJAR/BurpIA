package com.burpia.util;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.ui.EstilosUI;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class GestorConsolaGUI {
    private static final Logger LOGGER = Logger.getLogger(GestorConsolaGUI.class.getName());
    public enum TipoLog {
        INFO,
        VERBOSE,
        ERROR
    }

    private JTextPane consola;
    private StyledDocument documento;
    private boolean autoScroll;
    private final AtomicInteger contadorInfo;
    private final AtomicInteger contadorVerbose;
    private final AtomicInteger contadorError;
    private final DateTimeFormatter formateadorHora;
    private final ConcurrentLinkedQueue<EntradaLog> colaPendiente;
    private final AtomicBoolean flushProgramado;
    private final AtomicInteger logsPendientes;
    private final AtomicInteger versionCambios;
    private static final int MAXIMO_CARACTERES = 200_000;
    private static final Pattern ETIQUETAS_DESTACADAS =
        Pattern.compile("(?iu)\\b(?:NOTA|ACCION|ACCIÓN|NOTE|ACTION|PROVEEDOR|PROVIDER):");

    private Style estiloInfo;
    private Style estiloVerbose;
    private Style estiloError;
    private Style estiloInfoDestacado;
    private Style estiloVerboseDestacado;
    private Style estiloErrorDestacado;

    private PrintWriter stdoutOriginal;
    private PrintWriter stderrOriginal;

    public GestorConsolaGUI() {
        this.autoScroll = true;
        this.contadorInfo = new AtomicInteger(0);
        this.contadorVerbose = new AtomicInteger(0);
        this.contadorError = new AtomicInteger(0);
        this.formateadorHora = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.colaPendiente = new ConcurrentLinkedQueue<>();
        this.flushProgramado = new AtomicBoolean(false);
        this.logsPendientes = new AtomicInteger(0);
        this.versionCambios = new AtomicInteger(0);
    }

    public void establecerConsola(JTextPane consola) {
        this.consola = consola;
        this.documento = consola.getStyledDocument();
        aplicarTemaConsola(consola);

        programarFlush();
    }

    public void aplicarTemaConsola() {
        if (consola == null) {
            return;
        }
        aplicarTemaConsola(consola);
    }

    public void aplicarTemaConsola(JTextPane nuevaConsola) {
        if (nuevaConsola == null) {
            return;
        }

        Runnable aplicar = () -> {
            StyledDocument doc = nuevaConsola.getStyledDocument();

            Color fondo = nuevaConsola.getBackground();
            if (fondo == null) {
                fondo = UIManager.getColor("TextPane.background");
            }
            if (fondo == null) {
                fondo = EstilosUI.obtenerFondoPanel();
            }
            if (fondo == null) {
                fondo = Color.WHITE;
            }

            Color colorInfo = EstilosUI.colorTextoPrimario(fondo);
            Color colorVerbose = EstilosUI.colorTextoSecundario(fondo);
            Color colorError = EstilosUI.colorErrorAccesible(fondo);

            nuevaConsola.setForeground(colorInfo);
            nuevaConsola.setCaretColor(colorInfo);

            estiloInfo = obtenerOCrearEstilo(doc, "Info", null);
            StyleConstants.setForeground(estiloInfo, colorInfo);
            StyleConstants.setFontFamily(estiloInfo, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloInfo, nuevaConsola.getFont().getSize());
            StyleConstants.setBold(estiloInfo, false);
            StyleConstants.setItalic(estiloInfo, false);

            estiloInfoDestacado = obtenerOCrearEstilo(doc, "InfoDestacado", estiloInfo);
            StyleConstants.setForeground(estiloInfoDestacado, colorInfo);
            StyleConstants.setFontFamily(estiloInfoDestacado, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloInfoDestacado, nuevaConsola.getFont().getSize());
            StyleConstants.setBold(estiloInfoDestacado, true);
            StyleConstants.setItalic(estiloInfoDestacado, false);

            estiloVerbose = obtenerOCrearEstilo(doc, "Verbose", null);
            StyleConstants.setForeground(estiloVerbose, colorVerbose);
            StyleConstants.setFontFamily(estiloVerbose, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloVerbose, nuevaConsola.getFont().getSize());
            StyleConstants.setItalic(estiloVerbose, true);
            StyleConstants.setBold(estiloVerbose, false);

            estiloVerboseDestacado = obtenerOCrearEstilo(doc, "VerboseDestacado", estiloVerbose);
            StyleConstants.setForeground(estiloVerboseDestacado, colorVerbose);
            StyleConstants.setFontFamily(estiloVerboseDestacado, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloVerboseDestacado, nuevaConsola.getFont().getSize());
            StyleConstants.setItalic(estiloVerboseDestacado, true);
            StyleConstants.setBold(estiloVerboseDestacado, true);

            estiloError = obtenerOCrearEstilo(doc, "Error", null);
            StyleConstants.setForeground(estiloError, colorError);
            StyleConstants.setFontFamily(estiloError, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloError, nuevaConsola.getFont().getSize());
            StyleConstants.setBold(estiloError, true);
            StyleConstants.setItalic(estiloError, false);

            estiloErrorDestacado = obtenerOCrearEstilo(doc, "ErrorDestacado", estiloError);
            StyleConstants.setForeground(estiloErrorDestacado, colorError);
            StyleConstants.setFontFamily(estiloErrorDestacado, Font.MONOSPACED);
            StyleConstants.setFontSize(estiloErrorDestacado, nuevaConsola.getFont().getSize());
            StyleConstants.setBold(estiloErrorDestacado, true);
            StyleConstants.setItalic(estiloErrorDestacado, false);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            aplicar.run();
        } else {
            ejecutarEnEdt(aplicar);
        }
    }

    private Style obtenerOCrearEstilo(StyledDocument doc, String nombre, Style padre) {
        Style estilo = doc.getStyle(nombre);
        if (estilo == null) {
            estilo = doc.addStyle(nombre, padre);
        }
        return estilo;
    }

    private Style obtenerOCrearEstilo(String nombre, Style padre) {
        return obtenerOCrearEstilo(documento, nombre, padre);
    }

    public void capturarStreamsOriginales(PrintWriter stdout, PrintWriter stderr) {
        this.stdoutOriginal = stdout;
        this.stderrOriginal = stderr;
    }

    public void registrar(String mensaje, TipoLog tipo) {
        registrar("BurpIA", mensaje, tipo);
    }

    public void registrar(String origen, String mensaje, TipoLog tipo) {
        registrarInterno(origen, mensaje, tipo, true);
    }

    public void registrarTecnico(String origen, String mensaje, TipoLog tipo) {
        registrarInterno(origen, mensaje, tipo, false);
    }

    private void registrarInterno(String origen, String mensaje, TipoLog tipo, boolean traducirMensaje) {
        String origenSeguro = normalizarOrigen(origen);
        String mensajeSeguro = mensaje != null ? mensaje : "";
        String mensajeLocalizado = traducirMensaje ? I18nLogs.tr(mensajeSeguro) : I18nLogs.trTecnico(mensajeSeguro);
        incrementarContador(tipo);

        String hora = LocalTime.now().format(formateadorHora);
        String etiquetaNivel = etiquetaNivel(tipo);
        String mensajeFormateado = construirMensajeConsola(hora, origenSeguro, etiquetaNivel, mensajeLocalizado);

        agregarPendiente(new EntradaLog(mensajeFormateado, tipo));
        programarFlush();

        duplicarAStreamOriginal(origenSeguro, mensajeLocalizado, tipo, hora);
    }

    public void registrarInfo(String mensaje) {
        registrar(mensaje, TipoLog.INFO);
    }

    public void registrarInfo(String origen, String mensaje) {
        registrar(origen, mensaje, TipoLog.INFO);
    }

    public void registrarVerbose(String mensaje) {
        registrar(mensaje, TipoLog.VERBOSE);
    }

    public void registrarVerbose(String origen, String mensaje) {
        registrar(origen, mensaje, TipoLog.VERBOSE);
    }

    public void registrarError(String mensaje) {
        registrar(mensaje, TipoLog.ERROR);
    }

    public void registrarError(String origen, String mensaje) {
        registrar(origen, mensaje, TipoLog.ERROR);
    }

    public void limpiarConsola() {
        ejecutarEnEdt(() -> {
            try {
                if (documento != null) {
                    documento.remove(0, documento.getLength());
                }
                colaPendiente.clear();
                logsPendientes.set(0);
                contadorInfo.set(0);
                contadorVerbose.set(0);
                contadorError.set(0);
                marcarCambioVersion();
            } catch (BadLocationException e) {
                registrarErrorInterno("No se pudo limpiar consola: " + e.getMessage());
            }
        });
    }

    public void establecerAutoScroll(boolean activado) {
        this.autoScroll = activado;
    }

    public int obtenerTotalLogs() {
        return contadorInfo.get() + contadorVerbose.get() + contadorError.get();
    }

    public int obtenerContadorInfo() {
        return contadorInfo.get();
    }

    public int obtenerContadorVerbose() {
        return contadorVerbose.get();
    }

    public int obtenerContadorError() {
        return contadorError.get();
    }

    public String generarResumen() {
        return I18nUI.Consola.RESUMEN_LOGS(
            obtenerTotalLogs(),
            contadorInfo.get(),
            contadorVerbose.get(),
            contadorError.get()
        );
    }

    private Style obtenerEstilo(TipoLog tipo) {
        switch (tipo) {
            case VERBOSE:
                return estiloVerbose;
            case ERROR:
                return estiloError;
            case INFO:
            default:
                return estiloInfo;
        }
    }

    private Style obtenerEstiloDestacado(TipoLog tipo) {
        switch (tipo) {
            case VERBOSE:
                return estiloVerboseDestacado;
            case ERROR:
                return estiloErrorDestacado;
            case INFO:
            default:
                return estiloInfoDestacado;
        }
    }

    private void duplicarAStreamOriginal(String origen, String mensajeLocalizado, TipoLog tipo, String hora) {
        if (stdoutOriginal == null && stderrOriginal == null) {
            return;
        }
        String etiquetaNivel = etiquetaNivel(tipo);
        String mensajeConPrefijo = construirMensajeDuplicado(origen, etiquetaNivel, hora, mensajeLocalizado);
        if (tipo == TipoLog.ERROR) {
            if (stderrOriginal != null) {
                stderrOriginal.print(mensajeConPrefijo);
                stderrOriginal.flush();
            }
        } else {
            if (stdoutOriginal != null) {
                stdoutOriginal.print(mensajeConPrefijo);
                stdoutOriginal.flush();
            }
        }
    }

    public void shutdown() {
        colaPendiente.clear();
        logsPendientes.set(0);
        stdoutOriginal = null;
        stderrOriginal = null;
    }

    private void programarFlush() {
        if (documento == null) {
            return;
        }
        if (flushProgramado.compareAndSet(false, true)) {
            ejecutarEnEdt(this::flushPendientesEnEdt);
        }
    }

    private void flushPendientesEnEdt() {
        try {
            if (documento == null) {
                return;
            }

            EntradaLog entrada;
            while ((entrada = colaPendiente.poll()) != null) {
                logsPendientes.updateAndGet(actual -> actual > 0 ? actual - 1 : 0);
                insertarConEtiquetasDestacadas(entrada);
            }

            trimDocumentoSiEsNecesario();
            if (autoScroll && consola != null) {
                consola.setCaretPosition(documento.getLength());
            }
        } catch (BadLocationException ignored) {
            registrarErrorInterno("No se pudo renderizar log en consola GUI");
        } finally {
            flushProgramado.set(false);
            if (documento != null && logsPendientes.get() > 0) {
                programarFlush();
            }
        }
    }

    private void trimDocumentoSiEsNecesario() throws BadLocationException {
        int longitud = documento.getLength();
        if (longitud <= MAXIMO_CARACTERES) {
            return;
        }
        int exceso = longitud - MAXIMO_CARACTERES;
        documento.remove(0, exceso);
    }

    private void insertarConEtiquetasDestacadas(EntradaLog entrada) throws BadLocationException {
        String texto = entrada.mensaje != null ? entrada.mensaje : "";
        Style estiloBase = obtenerEstilo(entrada.tipo);
        Matcher matcher = ETIQUETAS_DESTACADAS.matcher(texto);
        if (!matcher.find()) {
            documento.insertString(documento.getLength(), texto, estiloBase);
            return;
        }
        Style estiloDestacado = obtenerEstiloDestacado(entrada.tipo);
        int cursor = 0;
        do {
            int inicio = matcher.start();
            if (inicio > cursor) {
                documento.insertString(documento.getLength(), texto.substring(cursor, inicio), estiloBase);
            }
            documento.insertString(documento.getLength(), texto.substring(inicio, matcher.end()), estiloDestacado);
            cursor = matcher.end();
        } while (matcher.find());
        if (cursor < texto.length()) {
            documento.insertString(documento.getLength(), texto.substring(cursor), estiloBase);
        }
    }

    private void incrementarContador(TipoLog tipo) {
        switch (tipo) {
            case VERBOSE:
                contadorVerbose.incrementAndGet();
                break;
            case ERROR:
                contadorError.incrementAndGet();
                break;
            case INFO:
            default:
                contadorInfo.incrementAndGet();
                break;
        }
        marcarCambioVersion();
    }

    public int obtenerVersion() { return versionCambios.get(); }

    private void agregarPendiente(EntradaLog entrada) {
        colaPendiente.add(entrada);
        int total = logsPendientes.incrementAndGet();
        if (total > PoliticaMemoria.MAXIMO_BACKLOG_CONSOLA) {
            recortarBacklogSinConsola();
        }
    }

    private void recortarBacklogSinConsola() {
        while (logsPendientes.get() > PoliticaMemoria.MAXIMO_BACKLOG_CONSOLA) {
            EntradaLog eliminado = colaPendiente.poll();
            if (eliminado == null) {
                logsPendientes.set(0);
                return;
            }
            logsPendientes.updateAndGet(actual -> actual > 0 ? actual - 1 : 0);
        }
    }

    private void marcarCambioVersion() {
        versionCambios.incrementAndGet();
    }

    private String normalizarOrigen(String origen) {
        if (Normalizador.esVacio(origen)) {
            return "BurpIA";
        }
        return origen.trim();
    }

    private String etiquetaNivel(TipoLog tipo) {
        if (tipo == TipoLog.ERROR) {
            return " [ERROR]";
        }
        if (tipo == TipoLog.VERBOSE) {
            return com.burpia.i18n.I18nUI.Consola.TAG_RASTREO();
        }
        return "";
    }

    private String construirMensajeConsola(String hora, String origen, String etiquetaNivel, String mensaje) {
        StringBuilder sb = new StringBuilder(64 + (mensaje != null ? mensaje.length() : 0));
        sb.append('[').append(hora).append("] [").append(origen).append(']').append(etiquetaNivel).append(' ');
        if (mensaje != null) {
            sb.append(mensaje);
        }
        sb.append('\n');
        return sb.toString();
    }

    private String construirMensajeDuplicado(String origen, String etiquetaNivel, String hora, String mensaje) {
        StringBuilder sb = new StringBuilder(64 + (mensaje != null ? mensaje.length() : 0));
        sb.append('[').append(origen).append(']').append(etiquetaNivel).append(" [").append(hora).append("] ");
        if (mensaje != null) {
            sb.append(mensaje);
        }
        sb.append('\n');
        return sb.toString();
    }

    private void registrarErrorInterno(String mensaje) {
        String texto = "[BurpIA] [ERROR] " + (mensaje != null ? mensaje : "Error interno de consola");
        if (stderrOriginal != null) {
            stderrOriginal.println(texto);
            stderrOriginal.flush();
            return;
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.log(Level.SEVERE, texto);
        }
    }

    private static final class EntradaLog {
        private final String mensaje;
        private final TipoLog tipo;

        private EntradaLog(String mensaje, TipoLog tipo) {
            this.mensaje = mensaje;
            this.tipo = tipo;
        }
    }
}
