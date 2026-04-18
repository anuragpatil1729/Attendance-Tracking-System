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
    private JLabel activeUsersValue;
    private JLabel todayAttendanceValue;

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
        applyTabColors(tabs);
        tabs.addChangeListener(e -> {
            applyTabColors(tabs);
        });

        add(tabs, BorderLayout.CENTER);
        loadOpenSessions();
        loadActiveUsers();
        loadTodayAttendancePercentage();
    }

    private void applyTabColors(JTabbedPane tabs) {
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setForegroundAt(i, i == selected ? Color.BLACK : Constants.darken(Constants.TEXT, 0.15f));
        }
    }

    private JPanel statsPanel(SyncManager syncManager) {
        JPanel p = new JPanel(new GridLayout(1, 4, 12, 12));
        p.setBackground(Constants.BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        activeUsersValue = new JLabel("Loading...");
        p.add(statCard("Total Attendees", activeUsersValue));

        openSessionsValue = new JLabel("Loading...");
        p.add(statCard("Open Sessions", openSessionsValue));

        todayAttendanceValue = new JLabel("Loading...");
        p.add(statCard("Today's Attendance %", todayAttendanceValue));
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

    private void loadActiveUsers() {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return sessionService.countActiveUsers();
            }

            @Override
            protected void done() {
                try {
                    int target = get();
                    animateIntegerValue(activeUsersValue, target, false);
                } catch (Exception e) {
                    activeUsersValue.setText("Error");
                }
            }
        }.execute();
    }

    private void loadTodayAttendancePercentage() {
        new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() {
                return sessionService.getTodayAttendancePercentage();
            }

            @Override
            protected void done() {
                try {
                    int target = (int) Math.round(get());
                    animateIntegerValue(todayAttendanceValue, target, true);
                } catch (Exception e) {
                    todayAttendanceValue.setText("Error");
                }
            }
        }.execute();
    }

    private void animateIntegerValue(JLabel label, int target, boolean asPercent) {
        int start = 0;
        int duration = 400;
        int step = 20;
        long startTime = System.currentTimeMillis();

        Timer timer = new Timer(step, null);
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, elapsed / (float) duration);
            int currentValue = Math.round(start + (target - start) * progress);
            label.setText(asPercent ? currentValue + "%" : String.valueOf(currentValue));
            if (progress >= 1f) {
                timer.stop();
            }
        });
        timer.setInitialDelay(0);
        timer.start();
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
            Shape roundRect = new java.awt.geom.RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 18, 18);
            g2.setClip(roundRect);
            g2.setColor(Constants.blend(Constants.SIDEBAR, Constants.ACCENT, 0.10f));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setClip(null);
            g2.setColor(Constants.ACCENT);
            g2.fillRect(0, 0, 4, getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
