package com.burpia.ui;
import com.burpia.model.Hallazgo;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RenderizadorConfianza extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private String confianzaStr = "";
    private boolean isIgnorado = false;

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

    private Color obtenerColorConfianzaTraducida(String confTraducida) {
        if (confTraducida == null) return Color.GRAY;
        if (confTraducida.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_ALTA())) return EstilosUI.COLOR_LOW;    
        if (confTraducida.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_MEDIA())) return EstilosUI.COLOR_MEDIUM; 
        if (confTraducida.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_BAJA())) return EstilosUI.COLOR_HIGH;   
        return Color.GRAY;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (confianzaStr == null || confianzaStr.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color tableBg = getBackground();
        boolean isDarkTheme = EstilosUI.esTemaOscuro(tableBg);
        Color textColor = getForeground();

        if (isIgnorado) {
            textColor = isDarkTheme ? new Color(130, 130, 130) : new Color(150, 150, 150);
        }

        Font font = getFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int filledSegments = 0;
        if (confianzaStr.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_ALTA())) filledSegments = 3;
        else if (confianzaStr.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_MEDIA())) filledSegments = 2;
        else if (confianzaStr.equals(com.burpia.i18n.I18nUI.Hallazgos.CONFIANZA_BAJA())) filledSegments = 1;

        Color colorBase = obtenerColorConfianzaTraducida(confianzaStr);
        if (isIgnorado) {
            colorBase = isDarkTheme ? new Color(100, 100, 100) : new Color(180, 180, 180);
        }

        int textWidth = fm.stringWidth(confianzaStr);
        int barWidth = 30;
        int barHeight = 8;
        int gap = 8;
        int totalWidth = textWidth + gap + barWidth;

        int x = (getWidth() - totalWidth) / 2;
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(textColor);
        g2.drawString(confianzaStr, x, textY);

        if (isIgnorado) {
            int lineY = textY - fm.getAscent() / 2;
            g2.drawLine(x, lineY, x + textWidth, lineY);
        }

        int barX = x + textWidth + gap;
        int barY = (getHeight() - barHeight) / 2;

        int segmentWidth = 8;
        int segmentGap = 3;

        Color bgSegmentColor = isDarkTheme ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 30);

        for (int i = 0; i < 3; i++) {
            if (i < filledSegments) {
                g2.setColor(colorBase);
            } else {
                g2.setColor(bgSegmentColor);
            }
            int segX = barX + i * (segmentWidth + segmentGap);
            g2.fill(new RoundRectangle2D.Float(segX, barY, segmentWidth, barHeight, 4, 4));
        }

        g2.dispose();
    }
}
