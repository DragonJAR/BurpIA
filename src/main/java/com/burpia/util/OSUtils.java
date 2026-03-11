package com.burpia.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public final class OSUtils {

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
        if (texto.endsWith("\r") || texto.endsWith("\n")) {
            return texto;
        }
        return texto + obtenerEolTerminal();
    }

    public static void cerrarVentanaAjustes() {
        try {
            Window[] windows = Window.getWindows();
            for (Window window : windows) {
                if (window instanceof JDialog && window.isVisible()) {
                    JDialog dialog = (JDialog) window;
                    String className = dialog.getClass().getSimpleName();

                    if (debeCerrarVentanaAjustes(className)) {
                        ejecutarEnEdt(() -> {
                            dialog.setVisible(false);
                            dialog.dispose();
                        });
                    }
                }
            }
        } catch (Exception ignored) {
            // Non-critical UI operation: closing configuration dialogs is best-effort
        }
    }

    static boolean debeCerrarVentanaAjustes(String className) {
        if (Normalizador.esVacio(className)) {
            return false;
        }
        return className.trim().contains("DialogoConfiguracion");
    }

    public static String expandirRuta(String rutaObj) {
        if (Normalizador.esVacio(rutaObj)) {
            return rutaObj;
        }
        String expansion = rutaObj;

        if (esWindows() && expansion.contains("%USERPROFILE%")) {
            String userProfile = System.getenv("USERPROFILE");
            if (Normalizador.noEsVacio(userProfile)) {
                expansion = expansion.replace("%USERPROFILE%", userProfile);
            }
        }

        if (expansion.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            if (Normalizador.noEsVacio(userHome)) {
                expansion = userHome + expansion.substring(1);
            }
        }

        return expansion;
    }

    public static boolean existeBinario(String ruta) {
        if (Normalizador.esVacio(ruta)) {
            return false;
        }

        String rutaExpandida = resolverEjecutableComando(ruta);
        if (Normalizador.esVacio(rutaExpandida)) {
            return false;
        }

        File binario = new File(rutaExpandida);
        if (esArchivoEjecutable(binario)) {
            return true;
        }

        if (tieneSeparadorRuta(rutaExpandida)) {
            return false;
        }

        return existeEnPath(rutaExpandida);
    }

    /**
     * Resuelve el ejecutable real de un comando que puede incluir argumentos.
     * Ejemplos válidos: "claude --dangerously-skip-permissions" o "\"/path/claude\" --flag".
     */
    public static String resolverEjecutableComando(String comando) {
        if (Normalizador.esVacio(comando)) {
            return comando;
        }
        String ejecutable = descomponerComando(comando).ejecutable();
        if (Normalizador.esVacio(ejecutable)) {
            return ejecutable;
        }
        return expandirRuta(ejecutable.trim());
    }

    public static String extraerEjecutableComando(String comando) {
        if (Normalizador.esVacio(comando)) {
            return comando;
        }
        return descomponerComando(comando).ejecutable();
    }

    /**
     * Normaliza un comando para enviarlo a un shell interactivo.
     * Expande la ruta del ejecutable principal y preserva los argumentos.
     */
    public static String normalizarComandoParaShell(String comando) {
        if (Normalizador.esVacio(comando)) {
            return comando;
        }

        PartesComando partes = descomponerComando(comando);
        if (Normalizador.esVacio(partes.ejecutable())) {
            return comando.trim();
        }

        String ejecutable = expandirRuta(partes.ejecutable().trim());
        String ejecutableNormalizado = requiereComillas(ejecutable) || partes.ejecutableEntreComillas()
            ? "\"" + ejecutable.replace("\"", "\\\"") + "\""
            : ejecutable;

        if (Normalizador.esVacio(partes.argumentos())) {
            return ejecutableNormalizado;
        }
        return ejecutableNormalizado + " " + partes.argumentos();
    }

    private static boolean tieneSeparadorRuta(String ejecutable) {
        if (Normalizador.esVacio(ejecutable)) {
            return false;
        }
        return ejecutable.contains("/")
            || ejecutable.contains("\\")
            || ejecutable.startsWith(".")
            || ejecutable.startsWith("~");
    }

    private static boolean existeEnPath(String ejecutable) {
        String path = System.getenv("PATH");
        if (Normalizador.esVacio(path)) {
            return false;
        }

        String[] directorios = path.split(java.io.File.pathSeparator);
        List<String> candidatos = construirNombresCandidatos(ejecutable);
        for (String dir : directorios) {
            if (Normalizador.esVacio(dir)) {
                continue;
            }
            File base = new File(dir);
            if (!base.isDirectory()) {
                continue;
            }
            for (String nombre : candidatos) {
                if (Normalizador.esVacio(nombre)) {
                    continue;
                }
                File candidato = new File(base, nombre);
                if (esArchivoEjecutable(candidato)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean esArchivoEjecutable(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return false;
        }
        if (esWindows()) {
            return true;
        }
        return archivo.canExecute();
    }

    private static List<String> construirNombresCandidatos(String ejecutable) {
        List<String> candidatos = new ArrayList<>();
        if (Normalizador.esVacio(ejecutable)) {
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

    private static boolean requiereComillas(String texto) {
        if (Normalizador.esVacio(texto)) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isWhitespace(texto.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static PartesComando descomponerComando(String comando) {
        String limpio = comando.trim();
        char primero = limpio.charAt(0);

        if (primero == '"' || primero == '\'') {
            int cierre = limpio.indexOf(primero, 1);
            if (cierre > 1) {
                return new PartesComando(
                    limpio.substring(1, cierre).trim(),
                    limpio.substring(cierre + 1).trim(),
                    true
                );
            }
            return new PartesComando(limpio.substring(1).trim(), "", true);
        }

        int i = 0;
        while (i < limpio.length() && !Character.isWhitespace(limpio.charAt(i))) {
            i++;
        }
        return new PartesComando(
            limpio.substring(0, i).trim(),
            limpio.substring(i).trim(),
            false
        );
    }

    private record PartesComando(String ejecutable, String argumentos, boolean ejecutableEntreComillas) {
    }
}
