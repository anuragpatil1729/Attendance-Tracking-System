package ui.attendee;

import db.SyncManager;
import model.AttendanceRecord;
import model.Session;
import model.User;
import service.AttendanceService;
import service.SessionService;
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
    private final SessionService sessionService = new SessionService();
    private final SyncManager syncManager;
    private final CircularProgressBar progress = new CircularProgressBar();
    private final JLabel summary = new JLabel("Total: 0 • Attended: 0");
    private final JLabel activeSessionLabel = new JLabel("No active session");
    private final JLabel syncIndicator = new JLabel("Sync pending: 0");
    private final JButton markBtn = UiStyle.createButton("Mark Attendance", Constants.ACCENT, Color.BLACK);
    private final AttendanceHistoryPanel historyPanel = new AttendanceHistoryPanel();

    private Timer pulseTimer;
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

        markBtn.addActionListener(e -> markAttendance());
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
        progress.setPreferredSize(new Dimension(160, 160));
        summary.setForeground(Constants.TEXT);
        summary.setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
        JPanel cardAContent = cardContent(cardA);
        cardAContent.add(progress);
        cardAContent.add(Box.createVerticalStrut(8));
        cardAContent.add(summary);

        JPanel cardB = stripeCard("Active Session");
        activeSessionLabel.setForeground(Constants.TEXT);
        activeSessionLabel.setFont(Constants.FONT.deriveFont(Font.PLAIN, 14f));
        JPanel cardBContent = cardContent(cardB);
        cardBContent.add(activeSessionLabel);
        cardBContent.add(Box.createVerticalStrut(10));
        cardBContent.add(markBtn);

        p.add(cardA);
        p.add(cardB);
        return p;
    }

    private JPanel stripeCard(String title) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Constants.SIDEBAR);
        shell.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(16),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(8, 0));
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
        bar.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(14),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        syncIndicator.setForeground(Constants.GREEN);
        syncIndicator.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        bar.add(syncIndicator, BorderLayout.WEST);
        bar.add(historyPanel, BorderLayout.CENTER);
        return bar;
    }

    private void refreshAll() {
        new SwingWorker<Void, Void>() {
            Session active;
            List<AttendanceRecord> history;

            @Override
            protected Void doInBackground() {
                active = sessionService.getActiveSession();
                history = attendanceService.attendeeHistory(user.getId(), 10);
                return null;
            }

            @Override
            protected void done() {
                int total = history.size();
                int attended = (int) history.stream().filter(h -> "Present".equalsIgnoreCase(h.getStatus()) || "Late".equalsIgnoreCase(h.getStatus())).count();
                int pct = total == 0 ? 0 : (int) ((attended * 100.0) / total);
                animateProgressTo(pct);
                summary.setText("Total: " + total + " • Attended: " + attended);
                if (active == null) {
                    activeSessionLabel.setText("No active session");
                    setMarkButtonEnabled(false);
                } else if ("lecture".equalsIgnoreCase(active.getSessionType())) {
                    activeSessionLabel.setText(active.getName() + " (Lecture - self marking disabled)");
                    setMarkButtonEnabled(false);
                } else {
                    activeSessionLabel.setText(active.getName() + " • " + active.getSubject());
                    setMarkButtonEnabled(true);
                }

                int pending = syncManager.pendingCount();
                syncIndicator.setText("Sync pending: " + pending);
                syncIndicator.setForeground(pending == 0 ? Constants.GREEN : Constants.ORANGE);
                historyPanel.setRecords(history);
            }
        }.execute();
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

    private void setMarkButtonEnabled(boolean enabled) {
        markBtn.setEnabled(enabled);
        if (enabled) {
            startPulse();
        } else {
            stopPulse();
        }
    }

    private void startPulse() {
        if (pulseTimer != null && pulseTimer.isRunning()) {
            return;
        }
        final float[] ratio = {0f};
        final boolean[] up = {true};
        pulseTimer = new Timer(55, e -> {
            ratio[0] += up[0] ? 0.08f : -0.08f;
            if (ratio[0] >= 1f) {
                ratio[0] = 1f;
                up[0] = false;
            } else if (ratio[0] <= 0f) {
                ratio[0] = 0f;
                up[0] = true;
            }
            markBtn.setBackground(Constants.blend(Constants.ACCENT, Constants.brighten(Constants.ACCENT, 0.28f), ratio[0]));
        });
        pulseTimer.start();
    }

    private void stopPulse() {
        if (pulseTimer != null) {
            pulseTimer.stop();
        }
        markBtn.setBackground(Constants.ACCENT);
    }

    private void markAttendance() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Session active = sessionService.getActiveSession();
                if (active == null) {
                    throw new RuntimeException("No open session");
                }
                attendanceService.markAttendance(user.getId(), active, NetworkUtil.detectIpAddress(), DeviceFingerprint.generate(), "Present", "Self marked");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(AttendeeDashboard.this, "Attendance marked 🎉");
                    refreshAll();
                } catch (Exception ex) {
                    ToastNotification.showError(AttendeeDashboard.this, ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
                }
            }
        }.execute();
    }
}
