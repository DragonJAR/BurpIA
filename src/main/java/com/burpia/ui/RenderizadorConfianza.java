package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renderizador de celdas para mostrar niveles de confianza con estilo visual (texto + barra de segmentos).
 * Implementa el patrón DRY centralizando la configuración de colores y segmentos.
 * 
 * <p>Los niveles de confianza se muestran con colores coherentes al nivel:
 * <ul>
 *   <li>ALTA (High) - Color rojo prominente</li>
 *   <li>MEDIA (Medium) - Color naranja intermedio</li>
 *   <li>BAJA (Low) - Color verde suave</li>
 * </ul>
 */
public class RenderizadorConfianza extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    // Constantes de diseño para la barra de segmentos
    private static final int ANCHO_BARRA = 30;
    private static final int ALTO_BARRA = 8;
    private static final int ESPACIO_TEXTO_BARRA = 8;
    private static final int ANCHO_SEGMENTO = 8;
    private static final int ESPACIO_SEGMENTOS = 3;
    private static final int RADIO_ESQUINA_SEGMENTO = 4;
    private static final int ALPHA_SEGMENTO_FONDO = 96;
    private static final int TOTAL_SEGMENTOS = 3;

    // Cache LRU para segmentos calculados
    private static final int MAX_CACHE_SEGMENTOS = 100;
    private static final Map<String, Integer> SEGMENT_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Integer>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                    return size() > MAX_CACHE_SEGMENTOS;
                }
            });

    private String confianzaStr = "";
    private boolean isIgnorado = false;

    @SuppressWarnings("this-escape")
    public RenderizadorConfianza() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        this.confianzaStr = (value != null) ? value.toString() : "";
        this.setText("");

        Font f = getFont();
        this.isIgnorado = f.getAttributes().containsKey(java.awt.font.TextAttribute.STRIKETHROUGH);

        return this;
    }

    /**
     * Obtiene el color asociado a un nivel de confianza traducido.
     * 
     * <p>Mapeo de colores coherente con el nivel de confianza:
     * <ul>
     *   <li>ALTA - COLOR_CRITICAL (púrpura) - máxima confianza merece máxima atención</li>
     *   <li>MEDIA - COLOR_MEDIUM (naranja) - confianza intermedia</li>
     *   <li>BAJA - COLOR_LOW (verde) - baja confianza, menor urgencia</li>
     * </ul>
     * 
     * @param confTraducida Nivel de confianza traducido
     * @return Color correspondiente al nivel de confianza
     */
    private Color obtenerColorConfianzaTraducida(String confTraducida) {
        if (Normalizador.esVacio(confTraducida)) {
            return Color.GRAY;
        }

        if (confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_ALTA())) return EstilosUI.COLOR_CRITICAL;
        if (confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_MEDIA())) return EstilosUI.COLOR_MEDIUM;
        if (confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_BAJA())) return EstilosUI.COLOR_LOW;

        return Color.GRAY;
    }

    /**
     * Obtiene el número de segmentos rellenos para un nivel de confianza.
     * Usa cache LRU para optimizar rendimiento en renderizado frecuente.
     * 
     * @param conf Nivel de confianza
     * @return Número de segmentos (0-3)
     */
    private int obtenerSegmentos(String conf) {
        if (Normalizador.esVacio(conf)) return 0;

        return SEGMENT_CACHE.computeIfAbsent(conf, c -> {
            if (c.equals(I18nUI.Hallazgos.CONFIANZA_ALTA())) return 3;
            if (c.equals(I18nUI.Hallazgos.CONFIANZA_MEDIA())) return 2;
            if (c.equals(I18nUI.Hallazgos.CONFIANZA_BAJA())) return 1;
            return 0;
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (Normalizador.esVacio(confianzaStr)) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color tableBg = getBackground();
            Color textColor = EstilosUI.colorTextoSecundario(tableBg);

            if (isIgnorado) {
                textColor = EstilosUI.colorTextoIgnorado(tableBg);
            }

            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int filledSegments = obtenerSegmentos(confianzaStr);

            Color colorBase = obtenerColorConfianzaTraducida(confianzaStr);
            colorBase = EstilosUI.ajustarParaContrasteMinimo(
                    colorBase,
                    tableBg,
                    EstilosUI.CONTRASTE_AA_GRANDE
            );
            if (isIgnorado) {
                colorBase = EstilosUI.colorFondoIgnorado(tableBg);
            }

            int textWidth = fm.stringWidth(confianzaStr);
            int totalWidth = textWidth + ESPACIO_TEXTO_BARRA + ANCHO_BARRA;

            int x = (getWidth() - totalWidth) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(textColor);
            g2.drawString(confianzaStr, x, textY);

            if (isIgnorado) {
                int lineY = textY - fm.getAscent() / 2;
                g2.drawLine(x, lineY, x + textWidth, lineY);
            }

            int barX = x + textWidth + ESPACIO_TEXTO_BARRA;
            int barY = (getHeight() - ALTO_BARRA) / 2;

            Color baseSegmento = EstilosUI.colorSeparador(tableBg);
            Color bgSegmentColor = new Color(
                    baseSegmento.getRed(),
                    baseSegmento.getGreen(),
                    baseSegmento.getBlue(),
                    ALPHA_SEGMENTO_FONDO
            );

            for (int i = 0; i < TOTAL_SEGMENTOS; i++) {
                if (i < filledSegments) {
                    g2.setColor(colorBase);
                } else {
                    g2.setColor(bgSegmentColor);
                }
                int segX = barX + i * (ANCHO_SEGMENTO + ESPACIO_SEGMENTOS);
                g2.fillRoundRect(segX, barY, ANCHO_SEGMENTO, ALTO_BARRA, RADIO_ESQUINA_SEGMENTO, RADIO_ESQUINA_SEGMENTO);
            }
        } finally {
            g2.dispose();
        }
    }
}
