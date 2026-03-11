package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para mostrar severidades con estilo visual (píldora coloreada).
 * Implementa el patrón DRY centralizando la configuración de severidades en un mapa unificado.
 */
public class RenderizadorSeveridad extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    private static final String ICONO_CRITICAL = "⚠ ";
    private static final String ICONO_HIGH = "▲ ";
    private static final String ICONO_MEDIUM = "◆ ";
    private static final String ICONO_LOW = "▽ ";
    private static final String ICONO_INFO = "ℹ ";

    private static final int MAX_CACHE_TEXTO = 100;
    private static final Map<String, String> TEXTO_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_TEXTO;
                }
            });

    private String severidadStr = "";
    private boolean isIgnorado = false;

    @SuppressWarnings("this-escape")
    public RenderizadorSeveridad() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        this.severidadStr = (value != null) ? value.toString() : "";
        this.setText("");

        Font f = getFont();
        this.isIgnorado = f.getAttributes().containsKey(java.awt.font.TextAttribute.STRIKETHROUGH);

        return this;
    }

    /**
     * Obtiene el color asociado a una severidad traducida.
     * Usa comparación centralizada para cumplir con DRY.
     */
    private Color obtenerColorSeveridadTraducida(String sevTraducida) {
        if (Normalizador.esVacio(sevTraducida)) {
            return Color.GRAY;
        }

        if (sevTraducida.equals(I18nUI.Hallazgos.SEVERIDAD_CRITICAL())) return EstilosUI.COLOR_CRITICAL;
        if (sevTraducida.equals(I18nUI.Hallazgos.SEVERIDAD_HIGH())) return EstilosUI.COLOR_HIGH;
        if (sevTraducida.equals(I18nUI.Hallazgos.SEVERIDAD_MEDIUM())) return EstilosUI.COLOR_MEDIUM;
        if (sevTraducida.equals(I18nUI.Hallazgos.SEVERIDAD_LOW())) return EstilosUI.COLOR_LOW;
        if (sevTraducida.equals(I18nUI.Hallazgos.SEVERIDAD_INFO())) return EstilosUI.COLOR_INFO;

        return Color.GRAY;
    }

    /**
     * Obtiene el icono asociado a una severidad traducida.
     * Centraliza la lógica de iconos para cumplir con DRY.
     */
    private String obtenerIconoSeveridad(String severidad) {
        if (Normalizador.esVacio(severidad)) {
            return "";
        }

        if (severidad.equals(I18nUI.Hallazgos.SEVERIDAD_CRITICAL())) return ICONO_CRITICAL;
        if (severidad.equals(I18nUI.Hallazgos.SEVERIDAD_HIGH())) return ICONO_HIGH;
        if (severidad.equals(I18nUI.Hallazgos.SEVERIDAD_MEDIUM())) return ICONO_MEDIUM;
        if (severidad.equals(I18nUI.Hallazgos.SEVERIDAD_LOW())) return ICONO_LOW;
        if (severidad.equals(I18nUI.Hallazgos.SEVERIDAD_INFO())) return ICONO_INFO;

        return "";
    }

    /**
     * Obtiene el texto a mostrar con icono, usando cache para optimizar rendimiento.
     * El cache implementa LRU (Least Recently Used) con LinkedHashMap access-order.
     */
    private String obtenerTextoMostrar(String severidad) {
        if (Normalizador.esVacio(severidad)) {
            return "";
        }

        synchronized (TEXTO_CACHE) {
            String cached = TEXTO_CACHE.get(severidad);
            if (cached != null) {
                return cached;
            }

            String icono = obtenerIconoSeveridad(severidad);
            String texto = icono + severidad;
            TEXTO_CACHE.put(severidad, texto);
            return texto;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (Normalizador.esVacio(severidadStr)) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color tableBg = getBackground();

        String displayText = obtenerTextoMostrar(severidadStr);

        Font font = getFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        Color colorFondoPildora = obtenerColorSeveridadTraducida(severidadStr);
        Color colorTexto = EstilosUI.ajustarParaContrasteMinimo(
            EstilosUI.obtenerColorTextoContraste(colorFondoPildora),
            colorFondoPildora,
            EstilosUI.CONTRASTE_AA_NORMAL
        );

        if (isIgnorado) {
            colorFondoPildora = EstilosUI.colorFondoIgnorado(tableBg);
            colorTexto = EstilosUI.colorTextoIgnorado(colorFondoPildora);
        }

        int textWidth = fm.stringWidth(displayText);
        int paddingX = 12;
        int paddingY = 4;

        int badgeWidth = textWidth + paddingX * 2;
        int badgeHeight = fm.getHeight() + paddingY * 2;

        int x = (getWidth() - badgeWidth) / 2;
        int y = (getHeight() - badgeHeight) / 2;

        g2.setColor(colorFondoPildora);
        g2.fillRoundRect(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        g2.setColor(colorTexto);
        int textX = x + paddingX;
        int textY = y + (badgeHeight - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(displayText, textX, textY);

        if (isIgnorado) {
            int lineY = textY - fm.getAscent() / 2 + 1;
            g2.drawLine(textX, lineY, textX + textWidth, lineY);
        }

        g2.dispose();
    }
}
