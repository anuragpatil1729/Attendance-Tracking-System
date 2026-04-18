package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public class PanelHeader extends JLabel {
    public PanelHeader(String title) {
        super(title);
        setForeground(Constants.ACCENT);
        setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    }

    public static PanelHeader create(String title) {
        return new PanelHeader(title);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Constants.ACCENT);
        g2.setStroke(new BasicStroke(1f));
        int y = getHeight() - 5;
        g2.drawLine(0, y, getWidth(), y);
        g2.dispose();
    }

}
