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
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para mostrar niveles de confianza con estilo visual (texto + barra de segmentos).
 * Implementa el patrón DRY centralizando la configuración de colores y segmentos.
 *
 * <p>Paleta propia de confianza (familia cian/azul, ver EstilosUI), deliberadamente
 * distinta de la paleta de severidad (rojo/ámbar/verde) para evitar ambigüedad:
 * <ul>
 *   <li>ALTA (High) - teal</li>
 *   <li>MEDIA (Medium) - azul</li>
 *   <li>BAJA (Low) - azul lavanda</li>
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

    // Cache LRU para segmentos calculados (helper compartido en UIUtils)
    private static final int MAX_CACHE_SEGMENTOS = 100;
    private static final Map<String, Integer> SEGMENT_CACHE = UIUtils.crearCacheLru(MAX_CACHE_SEGMENTOS);

    private String confianzaStr = "";
    private boolean isIgnorado = false;

    // Cache del color de fondo de segmento — recomputado solo cuando cambia
    // la identidad del color base de fondo de la tabla (cambio de tema).
    // Antes se alocaba new Color(...) por cada paint × N filas, generando
    // miles de allocs/seg al scrollear.
    private Color tableBgCacheado;
    private Color bgSegmentColorCacheado;

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

        // isIgnorado se detecta en paintComponent, no aquí: RenderizadorHallazgoBorrado
        // modifica la fuente (strikethrough) DESPUÉS de que este método retorna, así
        // que getFont() aquí refleja el estado de la fila ANTERIOR (stale).

        return this;
    }

    /**
     * Obtiene el color asociado a un nivel de confianza traducido.
     *
     * <p>Usa la paleta propia de confianza (familia cian/azul) deliberadamente distinta de la
     * paleta de severidad, para que el usuario distinga "severidad Critical" de "confianza Alta"
     * por color sin ambigüedad.
     *
     * @param confTraducida Nivel de confianza traducido
     * @return Color accesible correspondiente al nivel de confianza
     */
    private Color obtenerColorConfianzaTraducida(String confTraducida) {
        if (Normalizador.esVacio(confTraducida)) {
            return EstilosUI.colorDesconocidoAccesible(getBackground());
        }
        boolean alta = confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_ALTA());
        boolean media = confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_MEDIA());
        boolean baja = confTraducida.equals(I18nUI.Hallazgos.CONFIANZA_BAJA());
        return EstilosUI.colorConfianza(getBackground(), alta, media, baja);
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

        // Detectar "ignorado" aquí: el decorador RenderizadorHallazgoBorrado ya
        // aplicó/reseteó la fuente para esta fila, así que getFont() es correcto.
        this.isIgnorado = UIUtils.esFuenteTachada(getFont());

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
            if (isIgnorado) {
                colorBase = EstilosUI.colorFondoIgnorado(tableBg);
            }

            int textWidth = fm.stringWidth(confianzaStr);
            int totalWidth = textWidth + ESPACIO_TEXTO_BARRA + ANCHO_BARRA;

            // Math.max: con celdas más angostas que el contenido, el centrado daría
            // una x negativa y el texto quedaría recortado por la izquierda.
            int x = Math.max(0, (getWidth() - totalWidth) / 2);
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(textColor);
            g2.drawString(confianzaStr, x, textY);

            if (isIgnorado) {
                int lineY = textY - fm.getAscent() / 2;
                g2.drawLine(x, lineY, x + textWidth, lineY);
            }

            int barX = x + textWidth + ESPACIO_TEXTO_BARRA;
            int barY = (getHeight() - ALTO_BARRA) / 2;

            // Cache invalidación por identidad de tableBg (cambio de tema).
            // Reduce drásticamente la GC pressure al scrollear muchas filas.
            if (tableBg != tableBgCacheado || bgSegmentColorCacheado == null) {
                Color baseSegmento = EstilosUI.colorSeparador(tableBg);
                bgSegmentColorCacheado = new Color(
                        baseSegmento.getRed(),
                        baseSegmento.getGreen(),
                        baseSegmento.getBlue(),
                        ALPHA_SEGMENTO_FONDO
                );
                tableBgCacheado = tableBg;
            }
            Color bgSegmentColor = bgSegmentColorCacheado;

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
