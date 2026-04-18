package ui.components;

import util.Constants;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class AccentRoundedBorder extends AbstractBorder {
    private final int radius;

    public AccentRoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(8, 10, 8, 10);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 10;
        insets.right = 10;
        insets.top = 8;
        insets.bottom = 8;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Constants.ACCENT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
}
