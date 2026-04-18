package ui.components;

import util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class ToastNotification {
    private ToastNotification() {
    }

    public static void showSuccess(Component parent, String message) {
        show(parent, message, Constants.GREEN, Color.BLACK);
    }

    public static void showError(Component parent, String message) {
        show(parent, message, Constants.RED, Constants.TEXT);
    }

    public static void showInfo(Component parent, String message) {
        show(parent, message, Constants.ACCENT, Color.BLACK);
    }

    private static void show(Component parent, String message, Color accent, Color textColor) {
        SwingUtilities.invokeLater(() -> {
            Window owner = parent == null ? KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() : SwingUtilities.getWindowAncestor(parent);
            JWindow toast = new JWindow(owner);
            toast.setBackground(new Color(0, 0, 0, 0));

            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);
            panel.setBackground(Constants.SIDEBAR);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1, true),
                    new EmptyBorder(10, 12, 10, 12)));

            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(accent);
                    g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(10, 10));

            JLabel label = new JLabel(message);
            label.setForeground(textColor);
            label.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));

            panel.add(dot, BorderLayout.WEST);
            panel.add(label, BorderLayout.CENTER);

            toast.setContentPane(panel);
            toast.pack();
            positionToast(toast, owner);

            toast.setOpacity(0f);
            toast.setVisible(true);

            int fadeInMs = 200;
            int stayMs = 3000;
            int fadeOutMs = 300;
            int step = 25;

            Timer fadeIn = new Timer(step, null);
            final long startIn = System.currentTimeMillis();
            fadeIn.addActionListener(e -> {
                float t = Math.min(1f, (System.currentTimeMillis() - startIn) / (float) fadeInMs);
                toast.setOpacity(t);
                if (t >= 1f) {
                    fadeIn.stop();
                    Timer stay = new Timer(stayMs, ev -> fadeOutAndDispose(toast, fadeOutMs, step));
                    stay.setRepeats(false);
                    stay.start();
                }
            });
            fadeIn.start();
        });
    }

    private static void fadeOutAndDispose(JWindow toast, int fadeOutMs, int stepMs) {
        Timer fadeOut = new Timer(stepMs, null);
        final long startOut = System.currentTimeMillis();
        fadeOut.addActionListener(e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - startOut) / (float) fadeOutMs);
            toast.setOpacity(1f - t);
            if (t >= 1f) {
                fadeOut.stop();
                toast.setVisible(false);
                toast.dispose();
            }
        });
        fadeOut.start();
    }

    private static void positionToast(JWindow toast, Window owner) {
        Rectangle bounds;
        if (owner != null && owner.isShowing()) {
            Point p = owner.getLocationOnScreen();
            bounds = new Rectangle(p.x, p.y, owner.getWidth(), owner.getHeight());
        } else {
            bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        }

        int x = bounds.x + bounds.width - toast.getWidth() - 18;
        int y = bounds.y + bounds.height - toast.getHeight() - 18;
        toast.setLocation(x, y);
    }
}
