package com.burpia.util;

import com.burpia.i18n.I18nLogs;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GestorConsolaGUI {
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
    private static final int MAXIMO_CARACTERES = 200_000;

    private Style estiloInfo;
    private Style estiloVerbose;
    private Style estiloError;

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
    }

    public void establecerConsola(JTextPane consola) {
        this.consola = consola;
        this.documento = consola.getStyledDocument();

        estiloInfo = documento.addStyle("Info", null);
        StyleConstants.setForeground(estiloInfo, Color.BLACK);
        StyleConstants.setFontFamily(estiloInfo, Font.MONOSPACED);

        estiloVerbose = documento.addStyle("Verbose", null);
        StyleConstants.setForeground(estiloVerbose, Color.GRAY);
        StyleConstants.setFontFamily(estiloVerbose, Font.MONOSPACED);
        StyleConstants.setItalic(estiloVerbose, true);

        estiloError = documento.addStyle("Error", null);
        StyleConstants.setForeground(estiloError, Color.RED);
        StyleConstants.setFontFamily(estiloError, Font.MONOSPACED);
        StyleConstants.setBold(estiloError, true);
    }

    public void capturarStreamsOriginales(PrintWriter stdout, PrintWriter stderr) {
        this.stdoutOriginal = stdout;
        this.stderrOriginal = stderr;
    }

    public void registrar(String mensaje, TipoLog tipo) {
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        incrementarContador(tipo);

        String hora = LocalTime.now().format(formateadorHora);
        String mensajeFormateado = String.format("[%s] %s%n", hora, mensajeLocalizado);

        if (documento != null) {
            colaPendiente.add(new EntradaLog(mensajeFormateado, tipo));
            programarFlush();
        }

        duplicarAStreamOriginal(mensajeFormateado, tipo);
    }

    public void registrarInfo(String mensaje) {
        registrar(mensaje, TipoLog.INFO);
    }

    public void registrarVerbose(String mensaje) {
        registrar(mensaje, TipoLog.VERBOSE);
    }

    public void registrarError(String mensaje) {
        registrar(mensaje, TipoLog.ERROR);
    }

    public void limpiarConsola() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (documento != null) {
                    documento.remove(0, documento.getLength());
                }
                colaPendiente.clear();
                contadorInfo.set(0);
                contadorVerbose.set(0);
                contadorError.set(0);
            } catch (BadLocationException e) {
                // Ignorar errores de UI
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
        return String.format("Total: %d | Info: %d | Verbose: %d | Errores: %d",
            obtenerTotalLogs(), contadorInfo.get(), contadorVerbose.get(), contadorError.get());
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

    private void duplicarAStreamOriginal(String mensaje, TipoLog tipo) {
        String mensajeConPrefijo = "[BurpIA] " + mensaje;
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

    /**
     * Método shutdown para limpieza de recursos (llamado por ExtensionBurpIA.unload())
     */
    public void shutdown() {
        colaPendiente.clear();
        stdoutOriginal = null;
        stderrOriginal = null;
    }

    private void programarFlush() {
        if (flushProgramado.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(this::flushPendientesEnEdt);
        }
    }

    private void flushPendientesEnEdt() {
        try {
            if (documento == null) {
                return;
            }

            EntradaLog entrada;
            while ((entrada = colaPendiente.poll()) != null) {
                documento.insertString(documento.getLength(), entrada.mensaje, obtenerEstilo(entrada.tipo));
            }

            trimDocumentoSiEsNecesario();
            if (autoScroll && consola != null) {
                consola.setCaretPosition(documento.getLength());
            }
        } catch (BadLocationException ignored) {
            // Ignorar errores de inserción en UI
        } finally {
            flushProgramado.set(false);
            if (!colaPendiente.isEmpty()) {
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
