package ui;

import model.User;
import service.AuthService;
import ui.components.RoundedBorder;
import ui.components.ShakeAnimation;
import ui.components.StatusBadge;
import ui.components.ToastNotification;
import util.Constants;
import util.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class LoginFrame extends JFrame {

    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JCheckBox remember = new JCheckBox("Remember Me");
    private final AuthService authService = new AuthService();
    private final Preferences preferences = Preferences.userRoot().node("attendance-app");

    public LoginFrame() {
        super(Constants.APP_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Constants.BG);
        add(buildCard());
        loadRemembered();
    }

    private JComponent buildCard() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(20),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        card.setPreferredSize(new Dimension(380, 300));

        JLabel logo = new JLabel("🐾 Smart Attendance", SwingConstants.CENTER);
        logo.setFont(Constants.FONT.deriveFont(Font.BOLD, 24f));
        logo.setForeground(Constants.ACCENT);

        StatusBadge badge = new StatusBadge(
                NetworkUtil.isOnline() ? "Online" : "Offline",
                NetworkUtil.isOnline());
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        styleField(username, "Username");
        styleField(password, "Password");

        remember.setOpaque(false);
        remember.setForeground(Constants.TEXT);

        JButton toggle = button("Show/Hide");
        toggle.addActionListener(e -> password.setEchoChar(password.getEchoChar() == 0 ? '•' : (char) 0));

        JButton loginBtn = button("Login");
        loginBtn.addActionListener(e -> login(card));

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

        wrap.add(card);
        return wrap;
    }

    private void login(JPanel card) {
        new SwingWorker<User, Void>() {

            @Override
            protected User doInBackground() {

                // ✅ FIXED HERE (trim added)
                return authService.login(
                        username.getText().trim(),
                        new String(password.getPassword()).trim());
            }

            @Override
            protected void done() {
                try {
                    User user = get();

                    if (user == null) {
                        ShakeAnimation.shake(card);
                        ToastNotification.showError(LoginFrame.this,
                                "Wrong credentials. Try again ✨");
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

    private void styleField(JTextField field, String tip) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setBorder(new RoundedBorder(10));
        field.setBackground(Constants.INPUT);
        field.setForeground(Constants.TEXT);
        field.setCaretColor(Constants.ACCENT);
        field.setToolTipText(tip);
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(Constants.INPUT);
        b.setForeground(Constants.TEXT);
        b.setBorder(new RoundedBorder(10));
        return b;
    }

    private void loadRemembered() {
        username.setText(preferences.get("username", ""));
    }
}