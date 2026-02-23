package com.burpia.ui;
import java.awt.Font;



public class EstilosUI {
    private EstilosUI() {}

    public static final Font FUENTE_ESTANDAR = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public static final Font FUENTE_NEGRITA = new Font(Font.MONOSPACED, Font.BOLD, 11);

    public static final Font FUENTE_TABLA = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public static final Font FUENTE_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static final Font FUENTE_MONO_NEGRITA = new Font(Font.MONOSPACED, Font.BOLD, 12);

    public static final Font FUENTE_BOTON_PRINCIPAL = new Font(Font.MONOSPACED, Font.BOLD, 12);

    public static final Font FUENTE_CAMPO_TEXTO = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public static final java.awt.Color COLOR_FONDO_PANEL = new java.awt.Color(245, 245, 245);

    public static final java.awt.Color COLOR_BORDE_PANEL = new java.awt.Color(200, 200, 200);

    public static final java.awt.Color COLOR_TEXTO_NORMAL = java.awt.Color.BLACK;

    public static final java.awt.Color COLOR_TEXTO_DESHABILITADO = new java.awt.Color(150, 150, 150);
    public static final int ALTURA_FILA_TABLA = 32;

    public static final int MARGEN_PANEL = 10;

    public static final int ESPACIADO_COMPONENTES = 5;

    public static java.awt.Color obtenerColorTextoContraste(java.awt.Color colorFondo) {
        if (colorFondo == null) return java.awt.Color.BLACK;
        double y = (299 * colorFondo.getRed() + 587 * colorFondo.getGreen() + 114 * colorFondo.getBlue()) / 1000.0;
        return y >= 128 ? java.awt.Color.BLACK : java.awt.Color.WHITE;
    }

    public static boolean esTemaOscuro(java.awt.Color colorFondo) {
        if (colorFondo == null) return false;
        return (colorFondo.getRed() + colorFondo.getGreen() + colorFondo.getBlue()) / 3 < 128;
    }
}
