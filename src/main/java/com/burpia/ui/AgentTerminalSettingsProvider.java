package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Arrays;

final class AgentTerminalSettingsProvider extends DefaultSettingsProvider {
    private final Color fondoTerminal;
    private final Color textoTerminal;
    private final Color colorSeleccionFondo;
    private final Color colorSeleccionTexto;
    private final Color colorBusquedaFondo;
    private final Color colorBusquedaTexto;
    private final Color colorEnlace;
    private final ColorPalette paletaTerminal;

    AgentTerminalSettingsProvider() {
        Color fondoBase = UIManager.getColor("TextPane.background");
        if (fondoBase == null) {
            fondoBase = EstilosUI.obtenerFondoPanel();
        }

        boolean temaOscuro = EstilosUI.esTemaOscuro(fondoBase);
        this.fondoTerminal = fondoBase;
        this.textoTerminal = EstilosUI.colorTextoPrimario(fondoTerminal);
        this.colorEnlace = EstilosUI.colorEnlaceAccesible(fondoTerminal);

        Color seleccionBase = temaOscuro ? new Color(65, 102, 157) : new Color(190, 220, 255);
        this.colorSeleccionFondo = EstilosUI.ajustarParaContrasteMinimo(
            seleccionBase,
            fondoTerminal,
            EstilosUI.CONTRASTE_AA_GRANDE
        );
        this.colorSeleccionTexto = EstilosUI.ajustarParaContrasteMinimo(
            EstilosUI.obtenerColorTextoContraste(colorSeleccionFondo),
            colorSeleccionFondo,
            EstilosUI.CONTRASTE_AA_NORMAL
        );

        Color busquedaBase = temaOscuro ? new Color(151, 123, 34) : new Color(255, 231, 145);
        this.colorBusquedaFondo = EstilosUI.ajustarParaContrasteMinimo(
            busquedaBase,
            fondoTerminal,
            EstilosUI.CONTRASTE_AA_GRANDE
        );
        this.colorBusquedaTexto = EstilosUI.ajustarParaContrasteMinimo(
            EstilosUI.obtenerColorTextoContraste(colorBusquedaFondo),
            colorBusquedaFondo,
            EstilosUI.CONTRASTE_AA_NORMAL
        );

        Color[] coloresAnsi = temaOscuro ? construirPaletaOscura() : construirPaletaClara();
        this.paletaTerminal = new AgentColorPalette(coloresAnsi);
    }

    @Override
    public float getTerminalFontSize() {
        return (float) EstilosUI.FUENTE_MONO.getSize();
    }

    @Override
    public boolean useAntialiasing() {
        return true;
    }

    @Override
    public boolean copyOnSelect() {
        return true;
    }

    @Override
    public boolean pasteOnMiddleMouseClick() {
        return true;
    }

    @Override
    public ColorPalette getTerminalColorPalette() {
        return paletaTerminal;
    }

    @Override
    public TextStyle getDefaultStyle() {
        return new TextStyle(TerminalColor.awt(textoTerminal), TerminalColor.awt(fondoTerminal));
    }

    @Override
    public TextStyle getSelectionColor() {
        return new TextStyle(TerminalColor.awt(colorSeleccionTexto), TerminalColor.awt(colorSeleccionFondo));
    }

    @Override
    public TextStyle getFoundPatternColor() {
        return new TextStyle(TerminalColor.awt(colorBusquedaTexto), TerminalColor.awt(colorBusquedaFondo));
    }

    @Override
    public TextStyle getHyperlinkColor() {
        return new TextStyle(TerminalColor.awt(colorEnlace), TerminalColor.awt(fondoTerminal));
    }

    private Color[] construirPaletaOscura() {
        return new Color[] {
            new Color(32, 35, 41),
            new Color(231, 94, 111),
            new Color(136, 192, 118),
            new Color(224, 175, 104),
            new Color(106, 171, 250),
            new Color(193, 132, 255),
            new Color(82, 191, 200),
            new Color(212, 218, 233),
            new Color(88, 94, 105),
            new Color(255, 133, 148),
            new Color(167, 225, 142),
            new Color(246, 205, 135),
            new Color(144, 198, 255),
            new Color(218, 178, 255),
            new Color(131, 226, 233),
            new Color(246, 248, 255)
        };
    }

    private Color[] construirPaletaClara() {
        return new Color[] {
            new Color(30, 35, 43),
            new Color(194, 54, 34),
            new Color(26, 122, 55),
            new Color(161, 93, 0),
            new Color(13, 93, 191),
            new Color(130, 80, 223),
            new Color(32, 118, 122),
            new Color(227, 230, 235),
            new Color(102, 109, 121),
            new Color(212, 63, 42),
            new Color(36, 140, 65),
            new Color(186, 116, 0),
            new Color(20, 116, 227),
            new Color(145, 96, 238),
            new Color(43, 136, 141),
            new Color(250, 251, 252)
        };
    }

    private static final class AgentColorPalette extends ColorPalette {
        private final Color[] colores;

        private AgentColorPalette(Color... colores) {
            if (colores == null || colores.length < 16) {
                throw new IllegalArgumentException(I18nUI.General.ERROR_PALETA_ANSI_INVALIDA());
            }
            this.colores = Arrays.copyOf(colores, colores.length);
        }

        @Override
        protected Color getForegroundByColorIndex(int colorIndex) {
            return obtenerColorSeguro(colorIndex);
        }

        @Override
        protected Color getBackgroundByColorIndex(int colorIndex) {
            return obtenerColorSeguro(colorIndex);
        }

        private Color obtenerColorSeguro(int colorIndex) {
            if (colorIndex >= 0 && colorIndex < colores.length) {
                return colores[colorIndex];
            }
            return colores[0];
        }
    }
}
