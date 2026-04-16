package main;

import db.DBConnection;
import ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // Ignore to keep default look and feel as final fallback.
            }
        }

        // Validate DB connectivity before showing UI and offer retry on failure.
        while (true) {
            try {
                DBConnection.getConnection();
                break;
            } catch (RuntimeException ex) {
                if (!MainFrame.showRetryDialog(ex)) {
                    return;
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
