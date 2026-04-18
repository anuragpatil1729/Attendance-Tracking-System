package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public class CircularProgressBar extends JComponent {
    private int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(100, value));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 10;
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Constants.INPUT);
        g2.drawArc(x, y, size, size, 0, 360);

        g2.setColor(Constants.ACCENT);
        g2.drawArc(x, y, size, size, 90, -(value * 360 / 100));

        g2.setColor(Constants.TEXT);
        g2.setFont(Constants.FONT.deriveFont(Font.BOLD, 16f));
        String txt = value + "%";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, w / 2 - fm.stringWidth(txt) / 2, h / 2 + fm.getAscent() / 3);
        g2.dispose();
    }
}
