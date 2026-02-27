package com.burpia.util;

import javax.swing.*;
import java.awt.*;
import com.burpia.i18n.I18nLogs;
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
}
