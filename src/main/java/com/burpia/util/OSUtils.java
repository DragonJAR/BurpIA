package com.burpia.util;

public class OSUtils {

    private static final String OS = System.getProperty("os.name").toLowerCase();

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
        return esWindows() ? "\r\n" : "\r";
    }

    public static String prepararComando(String texto) {
        if (texto == null) return null;
        String eol = obtenerEolTerminal();
        // Si ya termina en un fin de línea, no agregar otro
        if (texto.endsWith("\r") || texto.endsWith("\n")) {
            return texto;
        }
        return texto + eol;
    }

    public static void copiarAlPortapapeles(String texto) {
        if (texto == null) return;
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(texto);
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } catch (Exception ignored) {
            // Silencioso
        }
    }

    public static void pegarDesdePortapapeles() {
        try {
            java.awt.Robot robot = new java.awt.Robot();
            int modKey = esMac() ? java.awt.event.KeyEvent.VK_META : java.awt.event.KeyEvent.VK_CONTROL;
            
            try {
                robot.keyPress(modKey);
                robot.keyPress(java.awt.event.KeyEvent.VK_V);
                robot.delay(50); // Pequeño delay de pulsación
            } finally {
                robot.keyRelease(java.awt.event.KeyEvent.VK_V);
                robot.keyRelease(modKey);
            }
        } catch (Exception ignored) {
            // Silencioso
        }
    }

    /**
     * Cierra solo la ventana de ajustes (DialogoConfiguracion) para evitar bloqueos del sistema.
     */
    public static void cerrarVentanaAjustes() {
        java.awt.Window[] windows = java.awt.Window.getWindows();
        for (java.awt.Window window : windows) {
            // Buscamos específicamente el diálogo de configuración
            if (window instanceof javax.swing.JDialog && window.isVisible()) {
                javax.swing.JDialog dialog = (javax.swing.JDialog) window;
                // Si la clase coincide o el título contiene "Configuracion" (por si acaso hay ofuscación o wrappers)
                if (dialog.getClass().getSimpleName().contains("DialogoConfiguracion") || 
                    (dialog.getTitle() != null && dialog.getTitle().contains("BurpIA"))) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        }
    }

    /**
     * Intenta traer al frente la ventana que contiene el componente dado y solicita el foco.
     */
    public static void forzarEnfoqueVentana(java.awt.Component componente) {
        if (componente == null) return;
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(componente);
        if (window != null) {
            if (window instanceof java.awt.Frame) {
                java.awt.Frame frame = (java.awt.Frame) window;
                if ((frame.getExtendedState() & java.awt.Frame.ICONIFIED) != 0) {
                    frame.setExtendedState(java.awt.Frame.NORMAL);
                }
            }
            window.toFront();
            window.requestFocus();
            // Asegurar que el componente específico también pida el foco
            componente.requestFocusInWindow();
        }
    }
}
