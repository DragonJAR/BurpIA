package com.burpia.ui;

import com.burpia.util.Normalizador;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public class EstilosUI {
    // Constantes de validación
    private static final int TAMANIO_FUENTE_MIN = 6;
    private static final int TAMANIO_FUENTE_MAX = 72;
    private static final String FUENTE_POR_DEFECTO = Font.MONOSPACED;
    private static final int TAMANIO_ESTANDAR_POR_DEFECTO = 11;
    private static final int TAMANIO_MONO_POR_DEFECTO = 12;

    // Constantes de contraste y diseño (WCAG 2.0 y proporciones visuales)
    private static final double PROPORCION_BANNER = 2.36; // ~26px desde 11px base
    private static final double PROPORCION_ICONO = 1.63; // ~18px desde 11px base
    private static final double UMBRAL_TEMA_OSCURO = 0.45; // Luminancia para considerar tema oscuro
    private static final int ITERACIONES_BUSQUEDA_CONTRASTE = 18; // Precisión binaria suficiente

    private EstilosUI() {
    }

    // Fuentes base - todas derivan de estas dos
    public static Font FUENTE_ESTANDAR = new Font(FUENTE_POR_DEFECTO, Font.PLAIN, TAMANIO_ESTANDAR_POR_DEFECTO);
    public static Font FUENTE_NEGRITA = new Font(FUENTE_POR_DEFECTO, Font.BOLD, TAMANIO_ESTANDAR_POR_DEFECTO);
    public static Font FUENTE_MONO = new Font(FUENTE_POR_DEFECTO, Font.PLAIN, TAMANIO_MONO_POR_DEFECTO);
    public static Font FUENTE_MONO_NEGRITA = new Font(FUENTE_POR_DEFECTO, Font.BOLD, TAMANIO_MONO_POR_DEFECTO);

    // Aliases para semántica - referencian las bases (se actualizan en actualizarFuentes)
    public static Font FUENTE_TABLA = FUENTE_ESTANDAR;
    public static Font FUENTE_CAMPO_TEXTO = FUENTE_ESTANDAR;
    public static Font FUENTE_BOTON_PRINCIPAL = FUENTE_MONO_NEGRITA;
    public static Font FUENTE_TITULO_BANNER = new Font(FUENTE_POR_DEFECTO, Font.BOLD, (int) (TAMANIO_ESTANDAR_POR_DEFECTO * PROPORCION_BANNER));
    public static Font FUENTE_ICONO_GRANDE = new Font(FUENTE_POR_DEFECTO, Font.PLAIN, (int) (TAMANIO_ESTANDAR_POR_DEFECTO * PROPORCION_ICONO));

    public static void actualizarFuentes(com.burpia.config.ConfiguracionAPI config) {
        if (config == null) {
            return;
        }

        // Obtener y validar nombres de fuente
        String nombreEstandar = config.obtenerNombreFuenteEstandar();
        String nombreMono = config.obtenerNombreFuenteMono();

        // Validar nombres de fuente con DRY principle
        if (Normalizador.esVacio(nombreEstandar)) {
            nombreEstandar = FUENTE_POR_DEFECTO;
        }
        if (Normalizador.esVacio(nombreMono)) {
            nombreMono = FUENTE_POR_DEFECTO;
        }

        // Obtener y validar tamaños de fuente
        int tamanioEstandar = validarTamanioFuente(config.obtenerTamanioFuenteEstandar(), TAMANIO_ESTANDAR_POR_DEFECTO);
        int tamanioMono = validarTamanioFuente(config.obtenerTamanioFuenteMono(), TAMANIO_MONO_POR_DEFECTO);

        // Fuentes base
        FUENTE_ESTANDAR = new Font(nombreEstandar, Font.PLAIN, tamanioEstandar);
        FUENTE_NEGRITA = new Font(nombreEstandar, Font.BOLD, tamanioEstandar);
        FUENTE_MONO = new Font(nombreMono, Font.PLAIN, tamanioMono);
        FUENTE_MONO_NEGRITA = new Font(nombreMono, Font.BOLD, tamanioMono);

        // Aliases - referencian las bases actualizadas
        FUENTE_TABLA = FUENTE_ESTANDAR;
        FUENTE_CAMPO_TEXTO = FUENTE_ESTANDAR;
        FUENTE_BOTON_PRINCIPAL = FUENTE_MONO_NEGRITA;

        // Fuentes especiales con tamaño proporcional
        FUENTE_TITULO_BANNER = new Font(nombreEstandar, Font.BOLD, (int) (tamanioEstandar * PROPORCION_BANNER));
        FUENTE_ICONO_GRANDE = new Font(nombreEstandar, Font.PLAIN, (int) (tamanioEstandar * PROPORCION_ICONO));
    }

    /**
     * Valida que el tamaño de fuente esté dentro del rango permitido.
     *
     * @param tamanio Tamaño a validar
     * @param porDefecto Valor por defecto si está fuera de rango
     * @return Tamaño validado dentro del rango [TAMANIO_FUENTE_MIN, TAMANIO_FUENTE_MAX]
     */
    private static int validarTamanioFuente(int tamanio, int porDefecto) {
        if (tamanio < TAMANIO_FUENTE_MIN || tamanio > TAMANIO_FUENTE_MAX) {
            return porDefecto;
        }
        return tamanio;
    }

    public static final Color COLOR_FONDO_PANEL = new Color(245, 245, 245);
    public static final Color COLOR_TEXTO_NORMAL = Color.BLACK;
    public static final Color COLOR_TEXTO_DESHABILITADO = new Color(150, 150, 150);

    public static final Color COLOR_CRITICAL = new Color(156, 39, 176);
    public static final Color COLOR_HIGH = new Color(220, 53, 69);
    public static final Color COLOR_MEDIUM = new Color(253, 126, 20);
    public static final Color COLOR_LOW = new Color(40, 167, 69);
    public static final Color COLOR_INFO = new Color(13, 110, 253);

    public static final Color COLOR_TASK_EN_COLA = Color.GRAY;
    public static final Color COLOR_TASK_ANALIZANDO = new Color(0, 120, 215);
    public static final Color COLOR_TASK_COMPLETADO = new Color(0, 153, 0);
    public static final Color COLOR_TASK_ERROR = new Color(204, 0, 0);
    public static final Color COLOR_TASK_CANCELADO = new Color(153, 76, 0);
    public static final Color COLOR_TASK_PAUSADO = new Color(255, 153, 0);

    public static final int ALTURA_FILA_TABLA = 32;
    public static final int MARGEN_PANEL = 10;
    public static final int ESPACIADO_COMPONENTES = 5;

    public static final double CONTRASTE_AA_NORMAL = 4.5d;
    public static final double CONTRASTE_AA_GRANDE = 3.0d;

    public static Color obtenerColorTextoContraste(Color colorFondo) {
        Color fondo = normalizarColor(colorFondo, Color.WHITE);
        double contrasteNegro = ratioContraste(Color.BLACK, fondo);
        double contrasteBlanco = ratioContraste(Color.WHITE, fondo);
        return contrasteNegro >= contrasteBlanco ? Color.BLACK : Color.WHITE;
    }

    public static boolean esTemaOscuro(Color colorFondo) {
        Color fondo = normalizarColor(colorFondo, obtenerColorFondoBase());
        return luminanciaRelativa(fondo) < UMBRAL_TEMA_OSCURO;
    }

    public static double ratioContraste(Color fg, Color bg) {
        Color texto = normalizarColor(fg, Color.BLACK);
        Color fondo = normalizarColor(bg, Color.WHITE);
        double l1 = luminanciaRelativa(texto);
        double l2 = luminanciaRelativa(fondo);
        double claro = Math.max(l1, l2);
        double oscuro = Math.min(l1, l2);
        return (claro + 0.05d) / (oscuro + 0.05d);
    }

    public static Color ajustarParaContrasteMinimo(Color base, Color bg, double minRatio) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color colorBase = normalizarColor(base, obtenerColorTextoContraste(fondo));
        double objetivo = Math.max(1.0d, minRatio);

        if (ratioContraste(colorBase, fondo) >= objetivo) {
            return colorBase;
        }

        Color negro = new Color(0, 0, 0, colorBase.getAlpha());
        Color blanco = new Color(255, 255, 255, colorBase.getAlpha());
        double contrasteNegro = ratioContraste(negro, fondo);
        double contrasteBlanco = ratioContraste(blanco, fondo);
        Color destino = contrasteNegro >= contrasteBlanco ? negro : blanco;

        if (Math.max(contrasteNegro, contrasteBlanco) < objetivo) {
            return destino;
        }

        double min = 0.0d;
        double max = 1.0d;
        Color mejor = destino;
        for (int i = 0; i < ITERACIONES_BUSQUEDA_CONTRASTE; i++) {
            double t = (min + max) / 2.0d;
            Color candidato = mezclar(colorBase, destino, t);
            if (ratioContraste(candidato, fondo) >= objetivo) {
                mejor = candidato;
                max = t;
            } else {
                min = t;
            }
        }
        return mejor;
    }

    public static Color colorTextoPrimario(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color candidato = UIManager.getColor("Label.foreground");
        if (candidato == null) {
            candidato = esTemaOscuro(fondo) ? new Color(234, 234, 234) : new Color(28, 28, 28);
        }
        return ajustarParaContrasteMinimo(candidato, fondo, CONTRASTE_AA_NORMAL);
    }

    public static Color colorTextoSecundario(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color candidato = esTemaOscuro(fondo) ? new Color(188, 188, 188) : new Color(92, 92, 92);
        return ajustarParaContrasteMinimo(candidato, fondo, CONTRASTE_AA_NORMAL);
    }

    public static Color colorSeparador(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color candidato = esTemaOscuro(fondo) ? new Color(120, 120, 120) : new Color(170, 170, 170);
        return ajustarParaContrasteMinimo(candidato, fondo, CONTRASTE_AA_GRANDE);
    }

    public static Color colorEnlaceAccesible(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color candidato = esTemaOscuro(fondo) ? new Color(124, 187, 255) : new Color(0, 102, 204);
        return ajustarParaContrasteMinimo(candidato, fondo, CONTRASTE_AA_NORMAL);
    }

    public static Color colorErrorAccesible(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color candidato = esTemaOscuro(fondo) ? new Color(255, 125, 125) : new Color(165, 28, 48);
        return ajustarParaContrasteMinimo(candidato, fondo, CONTRASTE_AA_NORMAL);
    }

    public static Color colorFondoSecundario(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        return esTemaOscuro(fondo) ? mezclar(fondo, Color.WHITE, 0.08d) : mezclar(fondo, Color.BLACK, 0.04d);
    }

    public static Color colorTextoIgnorado(Color bg) {
        Color fondo = normalizarColor(bg, obtenerColorFondoBase());
        Color base = esTemaOscuro(fondo) ? new Color(154, 154, 154) : new Color(118, 118, 118);
        return ajustarParaContrasteMinimo(base, fondo, CONTRASTE_AA_NORMAL);
    }

    public static Color colorTextoIgnorado(boolean esTemaOscuro) {
        Color fondoReferencia = esTemaOscuro ? new Color(32, 32, 32) : new Color(246, 246, 246);
        return colorTextoIgnorado(fondoReferencia);
    }

    public static Color colorFondoIgnorado(Color colorFondoTabla) {
        Color fondoTabla = normalizarColor(colorFondoTabla, obtenerColorFondoBase());
        return esTemaOscuro(fondoTabla)
                ? mezclar(fondoTabla, Color.BLACK, 0.18d)
                : mezclar(fondoTabla, Color.BLACK, 0.06d);
    }

    public static Color colorFondoIgnorado(Color colorFondoTabla, boolean esTemaOscuro) {
        Color fondoTabla = normalizarColor(colorFondoTabla, obtenerColorFondoBase());
        if (esTemaOscuro != esTemaOscuro(fondoTabla)) {
            fondoTabla = esTemaOscuro ? new Color(40, 40, 40) : new Color(248, 248, 248);
        }
        return colorFondoIgnorado(fondoTabla);
    }

    private static Color normalizarColor(Color color, Color fallback) {
        return color != null ? color : fallback;
    }

    /**
     * Obtiene el color de fondo del panel desde UIManager, con fallback a COLOR_FONDO_PANEL.
     * Método centralizado para evitar duplicación de este patrón en todo el código.
     */
    public static Color obtenerFondoPanel() {
        Color panel = UIManager.getColor("Panel.background");
        return panel != null ? panel : COLOR_FONDO_PANEL;
    }

    private static Color obtenerColorFondoBase() {
        return obtenerFondoPanel();
    }

    private static double luminanciaRelativa(Color color) {
        double r = componenteLineal(color.getRed() / 255.0d);
        double g = componenteLineal(color.getGreen() / 255.0d);
        double b = componenteLineal(color.getBlue() / 255.0d);
        return (0.2126d * r) + (0.7152d * g) + (0.0722d * b);
    }

    private static double componenteLineal(double c) {
        if (c <= 0.03928d) {
            return c / 12.92d;
        }
        return Math.pow((c + 0.055d) / 1.055d, 2.4d);
    }

    private static Color mezclar(Color origen, Color destino, double proporcionDestino) {
        double t = Math.max(0.0d, Math.min(1.0d, proporcionDestino));
        int r = interpolar(origen.getRed(), destino.getRed(), t);
        int g = interpolar(origen.getGreen(), destino.getGreen(), t);
        int b = interpolar(origen.getBlue(), destino.getBlue(), t);
        int a = interpolar(origen.getAlpha(), destino.getAlpha(), t);
        return new Color(r, g, b, a);
    }

    private static int interpolar(int inicio, int fin, double t) {
        return (int) Math.round(inicio + ((fin - inicio) * t));
    }
}
