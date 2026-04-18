package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public class CircularProgressBar extends JComponent {
    private int value;

    public CircularProgressBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(170, 170));
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        int safe = Math.max(0, Math.min(100, value));
        if (safe != this.value) {
            this.value = safe;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 14;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        int stroke = Math.max(10, size / 10);

        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Constants.blend(Constants.INPUT, Color.BLACK, 0.35f));
        g2.drawArc(x, y, size, size, 90, -360);

        int sweep = Math.round((value / 100f) * 360f);
        if (sweep > 0) {
            for (int i = 0; i < sweep; i++) {
                float ratio = sweep == 1 ? 1f : i / (float) (sweep - 1);
                g2.setColor(Constants.blend(Constants.ACCENT, Constants.GREEN, ratio));
                g2.drawArc(x, y, size, size, 90 - i, -1);
            }
        }

        String text = value + "%";
        g2.setFont(Constants.FONT.deriveFont(Font.BOLD, 20f));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (getWidth() - fm.stringWidth(text)) / 2;
        int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(Constants.TEXT);
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}
