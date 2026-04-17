package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public class StatusBadge extends JLabel {
    public StatusBadge(String text, boolean online) {
        super(text);
        setOpaque(true);
        setBackground(online ? Constants.GREEN : Constants.ORANGE);
        setForeground(Color.BLACK);
        setBorder(new RoundedBorder(12));
    }
}
