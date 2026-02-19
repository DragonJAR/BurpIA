package com.burpia.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GestorLogging {
    private final String nombreComponente;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final boolean detallado;
    private final GestorConsolaGUI gestorConsola;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public GestorLogging(String nombreComponente, PrintWriter stdout, PrintWriter stderr, boolean detallado) {
        this(nombreComponente, stdout, stderr, detallado, null);
    }

    public GestorLogging(String nombreComponente, PrintWriter stdout, PrintWriter stderr,
                        boolean detallado, GestorConsolaGUI gestorConsola) {
        this.nombreComponente = nombreComponente;
        this.stdout = stdout;
        this.stderr = stderr;
        this.detallado = detallado;
        this.gestorConsola = gestorConsola;
    }

    public void registrar(String mensaje) {
        String mensajeFormateado = formatearMensaje(mensaje);

        if (gestorConsola != null) {
            gestorConsola.registrarInfo(mensajeFormateado);
        }
        // También escribir al stdout original
        stdout.println(mensajeFormateado);
        stdout.flush();
    }

    public void rastrear(String mensaje) {
        if (detallado) {
            String mensajeFormateado = formatearMensajeVerbose(mensaje);

            if (gestorConsola != null) {
                gestorConsola.registrarVerbose(mensajeFormateado);
            }
            // También escribir al stdout original
            stdout.println(mensajeFormateado);
            stdout.flush();
        }
    }

    public void registrarError(String mensaje) {
        String mensajeFormateado = formatearError(mensaje);

        if (gestorConsola != null) {
            gestorConsola.registrarError(mensajeFormateado);
        }
        // También escribir al stderr original
        stderr.println(mensajeFormateado);
        stderr.flush();
    }

    public void rastrear(String mensaje, Throwable e) {
        if (detallado) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            rastrear(mensaje + "\n" + sw.toString());
        }
    }

    private String formatearMensaje(String mensaje) {
        String hora = LocalDateTime.now().format(FORMATO_HORA);
        return "[" + hora + "] [" + nombreComponente + "] " + mensaje;
    }

    private String formatearMensajeVerbose(String mensaje) {
        String hora = LocalDateTime.now().format(FORMATO_HORA);
        return "[" + hora + "] [" + nombreComponente + "] [RASTREO] " + mensaje;
    }

    private String formatearError(String mensaje) {
        String hora = LocalDateTime.now().format(FORMATO_HORA);
        return "[" + hora + "] [" + nombreComponente + "] [ERROR] " + mensaje;
    }
}
