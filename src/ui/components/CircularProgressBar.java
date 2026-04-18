package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public class CircularProgressBar extends JProgressBar {
    public CircularProgressBar() {
        super(0, 100);
        setValue(0);
        setStringPainted(true);
        setString("0%");
        setForeground(Constants.ACCENT);
        setBackground(Constants.INPUT);
        setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
        setBorder(BorderFactory.createLineBorder(Constants.ACCENT.darker(), 1, true));
    }

    @Override
    public void setValue(int n) {
        int safe = Math.max(0, Math.min(100, n));
        super.setValue(safe);
        setString(safe + "%");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(140, size.width), Math.max(36, size.height));
    }
}
