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
}
