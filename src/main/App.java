package main;

import ui.LoginFrame;
import util.Constants;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        try {
            Class<?> laf = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            laf.getMethod("setup").invoke(null);
        } catch (Throwable e) {
            installManualTheme();
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void installManualTheme() {
        UIManager.put("Panel.background", Constants.BG);
        UIManager.put("Viewport.background", Constants.BG);
        UIManager.put("Label.foreground", Constants.TEXT);
        UIManager.put("TextField.background", Constants.INPUT);
        UIManager.put("TextField.foreground", Constants.TEXT);
        UIManager.put("PasswordField.background", Constants.INPUT);
        UIManager.put("PasswordField.foreground", Constants.TEXT);
        UIManager.put("Button.background", Constants.INPUT);
        UIManager.put("Button.foreground", Constants.TEXT);
        UIManager.put("Button.border", new LineBorder(Constants.ACCENT, 1, true));
        UIManager.put("Table.background", Constants.INPUT);
        UIManager.put("Table.foreground", Constants.TEXT);
        UIManager.put("Table.selectionBackground", Constants.darken(Constants.ACCENT, 0.35f));
        UIManager.put("Table.selectionForeground", Constants.TEXT);
        UIManager.put("TableHeader.background", Constants.SIDEBAR);
        UIManager.put("TableHeader.foreground", Constants.ACCENT);
        UIManager.put("TabbedPane.background", Constants.SIDEBAR);
        UIManager.put("TabbedPane.foreground", Constants.TEXT);
        UIManager.put("TabbedPane.selected", Constants.ACCENT);
        UIManager.put("ScrollPane.background", Constants.BG);
        UIManager.put("OptionPane.background", Constants.SIDEBAR);
        UIManager.put("OptionPane.messageForeground", Constants.TEXT);
    }
}
