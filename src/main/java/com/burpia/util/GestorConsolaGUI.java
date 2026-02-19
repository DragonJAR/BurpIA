package com.burpia.util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

public class GestorConsolaGUI {
    public enum TipoLog {
        INFO,
        VERBOSE,
        ERROR
    }

    private JTextPane consola;
    private StyledDocument documento;
    private final ReentrantLock candado;
    private boolean autoScroll;
    private int contadorInfo;
    private int contadorVerbose;
    private int contadorError;
    private final DateTimeFormatter formateadorHora;

    // Estilos para diferentes tipos de log
    private Style estiloInfo;
    private Style estiloVerbose;
    private Style estiloError;

    // Streams originales de Burp
    private PrintWriter stdoutOriginal;
    private PrintWriter stderrOriginal;

    public GestorConsolaGUI() {
        this.candado = new ReentrantLock();
        this.autoScroll = true;
        this.contadorInfo = 0;
        this.contadorVerbose = 0;
        this.contadorError = 0;
        this.formateadorHora = DateTimeFormatter.ofPattern("HH:mm:ss");
    }

    public void establecerConsola(JTextPane consola) {
        this.consola = consola;
        this.documento = consola.getStyledDocument();

        StyleContext contextoEstilos = StyleContext.getDefaultStyleContext();
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
        candado.lock();
        try {
            switch (tipo) {
                case INFO:
                    contadorInfo++;
                    break;
                case VERBOSE:
                    contadorVerbose++;
                    break;
                case ERROR:
                    contadorError++;
                    break;
            }

            // Formatear mensaje con timestamp
            String hora = LocalTime.now().format(formateadorHora);
            String mensajeFormateado = String.format("[%s] %s\n", hora, mensaje);

            // Escribir a consola GUI
            if (documento != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        Style estilo = obtenerEstilo(tipo);
                        documento.insertString(documento.getLength(), mensajeFormateado, estilo);

                        // Auto-scroll si está activado
                        if (autoScroll) {
                            consola.setCaretPosition(documento.getLength());
                        }
                    } catch (BadLocationException e) {
                        // Ignorar errores de inserción
                    }
                });
            }

            duplicarAStreamOriginal(mensajeFormateado, tipo);

        } finally {
            candado.unlock();
        }
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
                documento.remove(0, documento.getLength());
                // Reiniciar contadores
                contadorInfo = 0;
                contadorVerbose = 0;
                contadorError = 0;
            } catch (BadLocationException e) {
                // Ignorar errores
            }
        });
    }

    public void establecerAutoScroll(boolean activado) {
        this.autoScroll = activado;
    }

    public int obtenerTotalLogs() {
        return contadorInfo + contadorVerbose + contadorError;
    }

    public int obtenerContadorInfo() {
        return contadorInfo;
    }

    public int obtenerContadorVerbose() {
        return contadorVerbose;
    }

    public int obtenerContadorError() {
        return contadorError;
    }

    public String generarResumen() {
        return String.format("Total: %d | Info: %d | Verbose: %d | Errores: %d",
            obtenerTotalLogs(), contadorInfo, contadorVerbose, contadorError);
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
        // Limpiar referencias a streams originales
        stdoutOriginal = null;
        stderrOriginal = null;
    }
}
