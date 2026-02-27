package com.burpia.util;

import javax.swing.*;
import java.awt.*;
import com.burpia.i18n.I18nLogs;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OSUtils {

    private static final Logger LOGGER = Logger.getLogger(OSUtils.class.getName());
    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    private OSUtils() {
    }

    public static boolean esWindows() {
        return OS.contains("win");
    }

    public static boolean esMac() {
        return OS.contains("mac");
    }

    public static boolean esLinux() {
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
    }

    public static String obtenerEolTerminal() {
        if (esWindows()) {
            return "\r\n";
        }
        return "\n";
    }

    public static String prepararComando(String texto) {
        if (texto == null) {
            return null;
        }
        String eol = obtenerEolTerminal();
        if (texto.endsWith("\r") || texto.endsWith("\n")) {
            return texto;
        }
        return texto + eol;
    }

    public static void cerrarVentanaAjustes() {
        try {
            Window[] windows = Window.getWindows();
            for (Window window : windows) {
                if (window instanceof JDialog && window.isVisible()) {
                    JDialog dialog = (JDialog) window;
                    String className = dialog.getClass().getSimpleName();
                    String title = dialog.getTitle();

                    if (className.contains("DialogoConfiguracion") ||
                        (title != null && title.contains("BurpIA"))) {
                        SwingUtilities.invokeLater(() -> {
                            dialog.setVisible(false);
                            dialog.dispose();
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, I18nLogs.tr("Error cerrando ventanas de ajustes"), e);
        }
    }

    public static String expandirRuta(String rutaObj) {
        if (rutaObj == null) return null;
        String expansion = rutaObj;
        
        if (esWindows() && expansion.contains("%USERPROFILE%")) {
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile != null) {
                expansion = expansion.replace("%USERPROFILE%", userProfile);
            }
        }

        if (expansion.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                expansion = userHome + expansion.substring(1);
            }
        }
        
        return expansion;
    }

    public static boolean existeBinario(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            return false;
        }

        String ejecutable = extraerEjecutableComando(ruta);
        if (ejecutable == null || ejecutable.trim().isEmpty()) {
            return false;
        }

        String rutaExpandida = expandirRuta(ejecutable.trim());
        File binario = new File(rutaExpandida);
        if (binario.isFile()) {
            return true;
        }

        if (tieneSeparadorRuta(rutaExpandida)) {
            return false;
        }

        return existeEnPath(rutaExpandida);
    }

    public static String extraerEjecutableComando(String comando) {
        if (comando == null) {
            return null;
        }
        String limpio = comando.trim();
        if (limpio.isEmpty()) {
            return "";
        }

        char primero = limpio.charAt(0);
        if (primero == '"' || primero == '\'') {
            int cierre = limpio.indexOf(primero, 1);
            if (cierre > 1) {
                return limpio.substring(1, cierre).trim();
            }
            return limpio.substring(1).trim();
        }

        int i = 0;
        while (i < limpio.length() && !Character.isWhitespace(limpio.charAt(i))) {
            i++;
        }
        return limpio.substring(0, i).trim();
    }

    private static boolean tieneSeparadorRuta(String ejecutable) {
        if (ejecutable == null || ejecutable.isEmpty()) {
            return false;
        }
        return ejecutable.contains("/")
            || ejecutable.contains("\\")
            || ejecutable.startsWith(".")
            || ejecutable.startsWith("~");
    }

    private static boolean existeEnPath(String ejecutable) {
        String path = System.getenv("PATH");
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String[] directorios = path.split(java.io.File.pathSeparator);
        List<String> candidatos = construirNombresCandidatos(ejecutable);
        for (String dir : directorios) {
            if (dir == null || dir.trim().isEmpty()) {
                continue;
            }
            File base = new File(dir);
            if (!base.isDirectory()) {
                continue;
            }
            for (String nombre : candidatos) {
                if (nombre == null || nombre.isEmpty()) {
                    continue;
                }
                File candidato = new File(base, nombre);
                if (candidato.isFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> construirNombresCandidatos(String ejecutable) {
        List<String> candidatos = new ArrayList<>();
        if (ejecutable == null || ejecutable.isEmpty()) {
            return candidatos;
        }
        candidatos.add(ejecutable);

        if (!esWindows()) {
            return candidatos;
        }

        String nombreMinuscula = ejecutable.toLowerCase(Locale.ROOT);
        if (nombreMinuscula.endsWith(".exe")
            || nombreMinuscula.endsWith(".cmd")
            || nombreMinuscula.endsWith(".bat")
            || nombreMinuscula.endsWith(".com")) {
            return candidatos;
        }

        String[] extensiones = {".exe", ".cmd", ".bat", ".com"};
        for (String extension : extensiones) {
            candidatos.add(ejecutable + extension);
        }
        return candidatos;
    }
}
