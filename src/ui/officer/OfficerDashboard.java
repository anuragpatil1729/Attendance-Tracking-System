package ui.officer;

import db.SyncManager;
import model.User;
import service.SessionService;
import ui.components.UiStyle;
import util.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OfficerDashboard extends JPanel {

    private final SessionService sessionService = new SessionService();
    private JLabel openSessionsValue;

    public OfficerDashboard(User user, SyncManager syncManager) {
        setLayout(new BorderLayout(12, 12));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTabbedPane tabs = new JTabbedPane();
        UiStyle.styleTabbedPane(tabs);
        tabs.addTab("Overview", statsPanel(syncManager));
        tabs.addTab("Session Manager", new SessionManagerPanel(user));
        tabs.addTab("Attendance Reports", new AttendanceReportPanel());
        tabs.addTab("Accounts", new AccountManagerPanel());
        tabs.addTab("Device Logs", new DeviceLogPanel());
        tabs.addChangeListener(e -> {
            int i = tabs.getSelectedIndex();
            if (i >= 0) {
                tabs.setForegroundAt(i, Color.BLACK);
                for (int t = 0; t < tabs.getTabCount(); t++) {
                    if (t != i) {
                        tabs.setForegroundAt(t, Constants.TEXT);
                    }
                }
            }
        });
        tabs.setForegroundAt(0, Color.BLACK);

        add(tabs, BorderLayout.CENTER);
        loadOpenSessions();
    }

    private JPanel statsPanel(SyncManager syncManager) {
        JPanel p = new JPanel(new GridLayout(1, 4, 12, 12));
        p.setBackground(Constants.BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        p.add(statCard("Total Attendees", "See Accounts tab"));

        openSessionsValue = new JLabel("Loading...");
        p.add(statCard("Open Sessions", openSessionsValue));

        p.add(statCard("Today's Attendance %", "See Reports tab"));
        p.add(statCard("Pending Syncs", String.valueOf(syncManager.pendingCount())));

        return p;
    }

    private JPanel statCard(String title, String value) {
        JLabel v = new JLabel(value);
        return statCard(title, v);
    }

    private JPanel statCard(String title, JLabel valueLabel) {
        StatCardPanel card = new StatCardPanel();

        JLabel t = new JLabel(title);
        t.setForeground(Constants.TEXT);
        t.setFont(Constants.FONT.deriveFont(Font.PLAIN, 12f));

        valueLabel.setForeground(Constants.ACCENT);
        valueLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 28f));

        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        installHoverLift(card);
        return card;
    }

    private void installHoverLift(StatCardPanel card) {
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.animateLift(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.animateLift(false);
            }
        });
    }

    private void loadOpenSessions() {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return sessionService.getOpenSessions().size();
            }

            @Override
            protected void done() {
                try {
                    openSessionsValue.setText(String.valueOf(get()));
                } catch (Exception e) {
                    openSessionsValue.setText("Error");
                }
            }
        }.execute();
    }

    private static class StatCardPanel extends JPanel {
        private int topPadding = 16;
        private Timer liftTimer;

        private StatCardPanel() {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));
        }

        private void animateLift(boolean hover) {
            if (liftTimer != null && liftTimer.isRunning()) {
                liftTimer.stop();
            }
            int target = hover ? 10 : 16;
            int start = topPadding;
            int duration = 150;
            int step = 15;
            int steps = duration / step;
            final int[] tick = {0};
            liftTimer = new Timer(step, e -> {
                tick[0]++;
                float t = Math.min(1f, tick[0] / (float) steps);
                topPadding = Math.round(start + (target - start) * t);
                setBorder(BorderFactory.createEmptyBorder(topPadding, 16, 30 - topPadding, 16));
                repaint();
                if (t >= 1f) {
                    liftTimer.stop();
                }
            });
            liftTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Constants.blend(Constants.SIDEBAR, Constants.ACCENT, 0.10f));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(Constants.ACCENT);
            g2.fillRoundRect(0, 0, 3, getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
