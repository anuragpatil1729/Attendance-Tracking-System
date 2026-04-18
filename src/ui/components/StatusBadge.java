package ui.components;

import util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatusBadge extends JLabel {
    public StatusBadge(String text, boolean online) {
        super(text);
        setOpaque(true);
        setBackground(online ? Constants.GREEN : Constants.ORANGE);
        setForeground(Color.BLACK);
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(12),
                new EmptyBorder(4, 10, 4, 10)));
    }
}
