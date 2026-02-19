package com.burpia.ui;

import java.awt.Font;

public class EstilosUI {
    private EstilosUI() {} // Clase de utilidad, no instanciable

    // Toda la interfaz usa Monospaced para consistencia visual

    public static final Font FUENTE_ESTANDAR = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public static final Font FUENTE_NEGRITA = new Font(Font.MONOSPACED, Font.BOLD, 11);

    public static final Font FUENTE_TABLA = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public static final Font FUENTE_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static final Font FUENTE_MONO_NEGRITA = new Font(Font.MONOSPACED, Font.BOLD, 12);

    public static final Font FUENTE_BOTON_PRINCIPAL = new Font(Font.MONOSPACED, Font.BOLD, 12);

    public static final Font FUENTE_CAMPO_TEXTO = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    // Colores inspirados en Burp Suite (tonos oscuros profesionales)

    public static final java.awt.Color COLOR_FONDO_PANEL = new java.awt.Color(245, 245, 245);

    public static final java.awt.Color COLOR_BORDE_PANEL = new java.awt.Color(200, 200, 200);

    public static final java.awt.Color COLOR_TEXTO_NORMAL = java.awt.Color.BLACK;

    public static final java.awt.Color COLOR_TEXTO_DESHABILITADO = new java.awt.Color(150, 150, 150);


    public static final int ALTURA_FILA_TABLA = 22;

    public static final int MARGEN_PANEL = 10;

    public static final int ESPACIADO_COMPONENTES = 5;


    public static void aplicarFontEstandar(javax.swing.JComponent componente) {
        componente.setFont(FUENTE_ESTANDAR);
    }

    public static void aplicarFontNegrita(javax.swing.JComponent componente) {
        componente.setFont(FUENTE_NEGRITA);
    }

    public static void aplicarFontTabla(javax.swing.JComponent componente) {
        componente.setFont(FUENTE_TABLA);
    }
}
