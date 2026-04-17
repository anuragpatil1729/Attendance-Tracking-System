package main;

import ui.LoginFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try {
            Class<?> laf = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            laf.getMethod("setup").invoke(null);
        } catch (Throwable e) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
            }
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
