package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public final class PanelHeader {
    private PanelHeader() {
    }

    public static JComponent create(String title) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JLabel label = new JLabel(title);
        label.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        label.setForeground(Constants.ACCENT);

        JSeparator separator = new JSeparator();
        separator.setForeground(Constants.ACCENT.darker());
        separator.setBackground(Constants.ACCENT.darker());

        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(separator, BorderLayout.SOUTH);
        return wrapper;
    }
}
