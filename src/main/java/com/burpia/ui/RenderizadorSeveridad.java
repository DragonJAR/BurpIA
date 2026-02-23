package com.burpia.ui;
import com.burpia.model.Hallazgo;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;




public class RenderizadorSeveridad extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private String severidadStr = "";
    private boolean isIgnorado = false;

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (severidadStr == null || severidadStr.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color tableBg = getBackground();
        boolean isDarkTheme = EstilosUI.esTemaOscuro(tableBg);

        StringBuilder texto = new StringBuilder(severidadStr);
        if (severidadStr.equals(Hallazgo.SEVERIDAD_CRITICAL)) texto.insert(0, "⚠ ");
        else if (severidadStr.equals(Hallazgo.SEVERIDAD_HIGH)) texto.insert(0, "▲ ");
        else if (severidadStr.equals(Hallazgo.SEVERIDAD_MEDIUM)) texto.insert(0, "◆ ");
        else if (severidadStr.equals(Hallazgo.SEVERIDAD_LOW)) texto.insert(0, "▽ ");
        else if (severidadStr.equals(Hallazgo.SEVERIDAD_INFO)) texto.insert(0, "ℹ ");

        String displayText = texto.toString();

        Font font = getFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        Color colorFondoPildora = Hallazgo.obtenerColorSeveridad(severidadStr);
        Color colorTexto = EstilosUI.obtenerColorTextoContraste(colorFondoPildora);

        if (isIgnorado) {
            colorFondoPildora = isDarkTheme ? new Color(70, 70, 70) : new Color(220, 220, 220);
            colorTexto = isDarkTheme ? new Color(130, 130, 130) : new Color(150, 150, 150);
        }

        int textWidth = fm.stringWidth(displayText);
        int paddingX = 12;
        int paddingY = 4;

        int badgeWidth = textWidth + paddingX * 2;
        int badgeHeight = fm.getHeight() + paddingY * 2;

        int x = (getWidth() - badgeWidth) / 2;
        int y = (getHeight() - badgeHeight) / 2;

        g2.setColor(colorFondoPildora);
        g2.fill(new RoundRectangle2D.Float(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight));

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
