package ui.attendee;

import db.SyncManager;
import model.AttendanceRecord;
import model.User;
import service.AttendanceService;
import ui.components.CircularProgressBar;
import ui.components.PanelHeader;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;
import util.DeviceFingerprint;
import util.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AttendeeDashboard extends JPanel {
    private final User user;
    private final AttendanceService attendanceService = new AttendanceService();
    private final SyncManager syncManager;
    private final CircularProgressBar progress = new CircularProgressBar();
    private final JLabel summary = new JLabel("Total: 0 • Attended: 0");
    private final JLabel syncIndicator = new JLabel("Sync pending: 0");
    private final StatusDot syncDot = new StatusDot();
    private final AttendanceHistoryPanel historyPanel = new AttendanceHistoryPanel();
    private final JPanel sessionsContainer = new JPanel();

    private Timer progressTimer;

    public AttendeeDashboard(User user, SyncManager syncManager) {
        this.user = user;
        this.syncManager = syncManager;
        setLayout(new BorderLayout(14, 14));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        add(top(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);

        refreshAll();
    }

    private JPanel top() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);
        String first = user.getFullName() == null ? user.getUsername() : user.getFullName().split(" ")[0];
        JLabel greet = new JLabel("Hey, " + first + " 👋");
        greet.setForeground(Constants.TEXT);
        greet.setFont(Constants.FONT.deriveFont(Font.BOLD, 24f));
        p.add(greet);
        return p;
    }

    private JPanel center() {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 14));
        p.setOpaque(false);

        JPanel cardA = stripeCard("Attendance");
        progress.setPreferredSize(new Dimension(170, 170));
        summary.setForeground(Constants.TEXT);
        summary.setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
        JPanel cardAContent = cardContent(cardA);
        cardAContent.add(progress);
        cardAContent.add(Box.createVerticalStrut(8));
        cardAContent.add(summary);

        JPanel cardB = stripeCard("Open Sessions");
        sessionsContainer.setOpaque(false);
        sessionsContainer.setLayout(new BoxLayout(sessionsContainer, BoxLayout.Y_AXIS));
        JScrollPane sessionsScroll = UiStyle.wrapScroll(sessionsContainer, Constants.SIDEBAR);
        sessionsScroll.setBorder(BorderFactory.createEmptyBorder());
        sessionsScroll.setPreferredSize(new Dimension(520, 300));
        JPanel cardBContent = cardContent(cardB);
        cardBContent.add(sessionsScroll);

        p.add(cardA);
        p.add(cardB);
        return p;
    }

    private JPanel stripeCard(String title) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Constants.SIDEBAR);
        shell.setBorder(UiStyle.roundedBorder(16));

        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(3, 0));
        stripe.setBackground(Constants.ACCENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.add(PanelHeader.create(title));
        content.add(Box.createVerticalStrut(8));

        shell.add(stripe, BorderLayout.WEST);
        shell.add(content, BorderLayout.CENTER);
        shell.putClientProperty("contentPanel", content);
        return shell;
    }

    private JPanel cardContent(JPanel card) {
        Object content = card.getClientProperty("contentPanel");
        if (content instanceof JPanel panel) {
            return panel;
        }
        return card;
    }

    private JComponent statusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(Constants.SIDEBAR);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        syncIndicator.setForeground(Constants.GREEN);
        syncIndicator.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        left.add(syncDot);
        left.add(syncIndicator);

        bar.add(left, BorderLayout.WEST);
        bar.add(historyPanel, BorderLayout.CENTER);
        return bar;
    }

    private void refreshAll() {
        new SwingWorker<Void, Void>() {
            List<AttendanceService.SessionAttendanceView> sessions;
            List<AttendanceRecord> history;
            AttendanceService.AttendanceStats stats;

            @Override
            protected Void doInBackground() {
                sessions = attendanceService.getOpenSessionsForAttendee(user.getId());
                history = attendanceService.attendeeHistory(user.getId(), 10);
                stats = attendanceService.getTodayAttendanceStats(user.getId());
                return null;
            }

            @Override
            protected void done() {
                int pct = stats.getPercentage();
                animateProgressTo(pct);
                summary.setText("Total: " + stats.getTotal() + " • Attended: " + stats.getAttended());
                updateSessionRows(sessions);

                int pending = syncManager.pendingCount();
                syncIndicator.setText("Sync pending: " + pending);
                syncIndicator.setForeground(pending == 0 ? Constants.GREEN : Constants.ORANGE);
                syncDot.setColor(pending == 0 ? Constants.GREEN : Constants.ORANGE);
                historyPanel.setRecords(history);
            }
        }.execute();
    }

    private void updateSessionRows(List<AttendanceService.SessionAttendanceView> sessions) {
        sessionsContainer.removeAll();
        if (sessions.isEmpty()) {
            JLabel none = new JLabel("No open sessions");
            none.setForeground(Constants.TEXT);
            sessionsContainer.add(none);
        }

        for (AttendanceService.SessionAttendanceView row : sessions) {
            sessionsContainer.add(buildSessionRow(row));
            sessionsContainer.add(Box.createVerticalStrut(8));
        }

        sessionsContainer.revalidate();
        sessionsContainer.repaint();
    }

    private JComponent buildSessionRow(AttendanceService.SessionAttendanceView row) {
        JPanel line = UiStyle.sectionCard(new BorderLayout(8, 8), 10);
        line.setBackground(Constants.INPUT);

        JLabel text = new JLabel(row.getSession().getName() + " • " + row.getSession().getSubject() + " (" + row.getSession().getSessionType() + ")");
        text.setForeground(Constants.TEXT);
        text.setFont(Constants.FONT.deriveFont(Font.PLAIN, 13f));

        JComponent action = sessionActionComponent(row);

        line.add(text, BorderLayout.CENTER);
        line.add(action, BorderLayout.EAST);
        return line;
    }

    private JComponent sessionActionComponent(AttendanceService.SessionAttendanceView row) {
        String status = row.getStatus();
        if (status == null) {
            JButton markBtn = UiStyle.createButton("Mark Attendance", Constants.ACCENT, Color.BLACK);
            markBtn.addActionListener(e -> markAttendance(row));
            return markBtn;
        }

        String labelText;
        Color bg;
        if ("Present".equalsIgnoreCase(status)) {
            labelText = "Marked Present";
            bg = Constants.GREEN;
        } else if ("Late".equalsIgnoreCase(status)) {
            labelText = "Marked Late";
            bg = Constants.ORANGE;
        } else {
            labelText = "Marked " + status;
            bg = Constants.INPUT;
        }

        JLabel badge = new JLabel(labelText);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(Color.BLACK);
        badge.setBorder(BorderFactory.createCompoundBorder(UiStyle.roundedBorder(8), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return badge;
    }

    private void animateProgressTo(int target) {
        if (progressTimer != null) {
            progressTimer.stop();
        }
        int start = progress.getValue();
        int durationMs = 600;
        int stepMs = 30;
        int steps = durationMs / stepMs;
        final int[] currentStep = {0};
        progressTimer = new Timer(stepMs, e -> {
            currentStep[0]++;
            float t = Math.min(1f, currentStep[0] / (float) steps);
            int value = start + Math.round((target - start) * t);
            progress.setValue(value);
            if (t >= 1f) {
                progressTimer.stop();
            }
        });
        progressTimer.start();
    }

    private void markAttendance(AttendanceService.SessionAttendanceView row) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                attendanceService.markAttendance(
                        user.getId(),
                        row.getSession(),
                        NetworkUtil.detectIpAddress(),
                        DeviceFingerprint.generate(),
                        "Present",
                        "Self marked"
                );
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(AttendeeDashboard.this, "Attendance marked 🎉");
                    refreshAll();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    ToastNotification.showError(AttendeeDashboard.this, cause.getMessage());
                }
            }
        }.execute();
    }

    private static class StatusDot extends JComponent {
        private Color color = Constants.GREEN;
        private float alpha = 0.4f;
        private boolean rising = true;

        private StatusDot() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 10));
            Timer timer = new Timer(70, e -> {
                alpha += rising ? 0.06f : -0.06f;
                if (alpha >= 1f) {
                    alpha = 1f;
                    rising = false;
                } else if (alpha <= 0.4f) {
                    alpha = 0.4f;
                    rising = true;
                }
                repaint();
            });
            timer.start();
        }

        private void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
    }
}
