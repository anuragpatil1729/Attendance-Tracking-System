package ui;

import model.User;
import service.AuthService;
import ui.components.ShakeAnimation;
import ui.components.StatusBadge;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;
import util.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

public class LoginFrame extends JFrame {

    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JCheckBox remember = new JCheckBox("Remember Me");
    private final AuthService authService = new AuthService();
    private final Preferences preferences = Preferences.userRoot().node("attendance-app");
    private final JLabel spinner = new JLabel("◐", SwingConstants.CENTER);

    private GlowCard card;
    private JButton loginBtn;
    private Timer spinnerTimer;
    private int spinnerIndex;

    public LoginFrame() {
        super(Constants.APP_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(560, 470);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Constants.BG);
        add(buildCard());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (card != null) {
                    card.startGlow();
                }
            }
        });
        loadRemembered();
    }

    private JComponent buildCard() {
        JPanel wrap = new GradientWrapPanel();
        wrap.setLayout(new GridBagLayout());

        card = new GlowCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 360));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel logo = new JLabel("🐾 Smart Attendance", SwingConstants.CENTER);
        logo.setFont(Constants.FONT.deriveFont(Font.BOLD, 24f));
        logo.setForeground(Constants.ACCENT);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        StatusBadge badge = new StatusBadge(
                NetworkUtil.isOnline() ? "Online" : "Offline",
                NetworkUtil.isOnline());
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        UiStyle.styleField(username, "Username");
        UiStyle.styleField(password, "Password");

        remember.setBackground(new Color(0, 0, 0, 0));
        remember.setOpaque(false);
        remember.setForeground(Constants.TEXT);

        JButton toggle = UiStyle.createButton("Show/Hide", Constants.SIDEBAR, Constants.TEXT);
        toggle.addActionListener(e -> password.setEchoChar(password.getEchoChar() == 0 ? '•' : (char) 0));

        loginBtn = UiStyle.createButton("Login", Constants.ACCENT, Color.BLACK);
        loginBtn.addActionListener(e -> login());

        spinner.setFont(Constants.FONT.deriveFont(Font.BOLD, 16f));
        spinner.setForeground(Constants.ACCENT);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        spinner.setVisible(false);

        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        card.add(badge);
        card.add(Box.createVerticalStrut(14));
        card.add(username);
        card.add(Box.createVerticalStrut(8));
        card.add(password);
        card.add(Box.createVerticalStrut(8));
        card.add(toggle);
        card.add(Box.createVerticalStrut(6));
        card.add(remember);
        card.add(Box.createVerticalStrut(14));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(spinner);

        wrap.add(card);
        return wrap;
    }

    private void login() {
        setLoading(true);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.login(username.getText().trim(), new String(password.getPassword()).trim());
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    User user = get();
                    if (user == null) {
                        ShakeAnimation.shake(card);
                        ToastNotification.showError(LoginFrame.this, "Wrong credentials. Try again ✨");
                        return;
                    }

                    if (remember.isSelected()) {
                        preferences.put("username", user.getUsername());
                    } else {
                        preferences.remove("username");
                    }

                    new MainFrame(user).setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    ToastNotification.showError(LoginFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        spinner.setVisible(loading);
        if (loading) {
            startSpinner();
        } else {
            stopSpinner();
        }
    }

    private void startSpinner() {
        if (spinnerTimer != null && spinnerTimer.isRunning()) {
            return;
        }
        final String[] states = {"◐", "◓", "◑", "◒"};
        spinnerTimer = new Timer(100, e -> {
            spinner.setText(states[spinnerIndex % states.length] + " Signing in...");
            spinnerIndex++;
        });
        spinnerTimer.start();
    }

    private void stopSpinner() {
        if (spinnerTimer != null) {
            spinnerTimer.stop();
        }
        spinner.setText("◐");
        spinnerIndex = 0;
    }

    private void loadRemembered() {
        username.setText(preferences.get("username", ""));
    }

    private static final class GradientWrapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, Constants.BG, 0, getHeight(), Constants.blend(Constants.BG, Constants.SIDEBAR, 0.3f));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class GlowCard extends JPanel {
        private Color glowColor = Constants.ACCENT;
        private Timer glowTimer;
        private float ratio = 0f;
        private boolean increasing = true;

        private GlowCard() {
            setOpaque(false);
        }

        private void startGlow() {
            if (glowTimer != null && glowTimer.isRunning()) {
                return;
            }
            glowTimer = new Timer(45, e -> {
                ratio += increasing ? 0.06f : -0.06f;
                if (ratio >= 1f) {
                    ratio = 1f;
                    increasing = false;
                }
                if (ratio <= 0f) {
                    ratio = 0f;
                    increasing = true;
                }
                glowColor = Constants.blend(Constants.ACCENT, Constants.brighten(Constants.ACCENT, 0.35f), ratio);
                repaint();
            });
            glowTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Constants.SIDEBAR);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(glowColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
        }
    }
}
