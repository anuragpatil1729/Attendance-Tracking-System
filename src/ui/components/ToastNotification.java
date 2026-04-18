package ui.components;

import util.Constants;

import javax.swing.*;
import java.awt.*;

public final class ToastNotification {
    private ToastNotification() {
    }

    public static void showSuccess(Component parent, String message) {
        show(parent, message, Constants.GREEN, Color.BLACK);
    }

    public static void showError(Component parent, String message) {
        show(parent, message, Constants.RED, Color.BLACK);
    }

    public static void showInfo(Component parent, String message) {
        show(parent, message, Constants.ACCENT, Color.BLACK);
    }

    private static void show(Component parent, String message, Color bg, Color fg) {
        Window owner = parent == null ? JOptionPane.getRootFrame() : SwingUtilities.getWindowAncestor(parent);
        JWindow window = new JWindow(owner);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(bg);
        content.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(14),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel label = new JLabel(message);
        label.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        label.setForeground(fg);

        content.add(label, BorderLayout.CENTER);
        window.setBackground(new Color(0, 0, 0, 0));
        window.setContentPane(content);
        window.pack();

        Rectangle bounds = owner != null ? owner.getBounds() : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        int x = bounds.x + bounds.width - window.getWidth() - 20;
        int y = bounds.y + bounds.height - window.getHeight() - 40;
        window.setLocation(x, y);

        animate(window);
    }

    private static void animate(JWindow window) {
        window.setOpacity(0f);
        window.setAlwaysOnTop(true);
        window.setVisible(true);

        Timer fadeIn = new Timer(20, null);
        fadeIn.addActionListener(e -> {
            float next = Math.min(1f, window.getOpacity() + 0.1f);
            window.setOpacity(next);
            if (next >= 1f) {
                fadeIn.stop();
                scheduleFadeOut(window);
            }
        });
        fadeIn.start();
    }

    private static void scheduleFadeOut(JWindow window) {
        Timer wait = new Timer(3000, e -> {
            Timer fadeOut = new Timer(20, null);
            fadeOut.addActionListener(ev -> {
                float next = Math.max(0f, window.getOpacity() - 0.1f);
                window.setOpacity(next);
                if (next <= 0f) {
                    fadeOut.stop();
                    window.dispose();
                }
            });
            fadeOut.start();
        });
        wait.setRepeats(false);
        wait.start();
    }
}
