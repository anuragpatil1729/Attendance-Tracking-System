package ui.components;

import javax.swing.*;

public final class ToastNotification {
    private ToastNotification() {}

    public static void showSuccess(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
