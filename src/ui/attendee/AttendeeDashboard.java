package ui.attendee;

import db.SyncManager;
import model.Session;
import model.User;
import service.AttendanceService;
import service.SessionService;
import ui.components.CircularProgressBar;
import ui.components.RoundedBorder;
import ui.components.ToastNotification;
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
    private final JButton markBtn = new JButton("Mark Attendance");
    private final AttendanceHistoryPanel historyPanel = new AttendanceHistoryPanel();

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

        JPanel cardA = card("Attendance");
        progress.setPreferredSize(new Dimension(160, 160));
        summary.setForeground(Constants.TEXT);
        cardA.add(progress); cardA.add(summary);

        JPanel cardB = card("Active Session");
        activeSessionLabel.setForeground(Constants.TEXT);
        markBtn.setBorder(new RoundedBorder(12));
        markBtn.setBackground(Constants.ACCENT); markBtn.setForeground(Color.BLACK);
        markBtn.addActionListener(e -> markAttendance());
        cardB.add(activeSessionLabel); cardB.add(markBtn);

        p.add(cardA); p.add(cardB);
        return p;
    }

    private JPanel card(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(16), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JLabel l = new JLabel(title); l.setForeground(Constants.ACCENT); l.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(l); card.add(Box.createVerticalStrut(12));
        return card;
    }

    private JComponent statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Constants.SIDEBAR);
        JLabel s = new JLabel("Sync pending: " + syncManager.pendingCount());
        s.setForeground(Constants.TEXT);
        bar.add(s, BorderLayout.WEST);
        bar.add(historyPanel, BorderLayout.CENTER);
        return bar;
    }

    private void refreshAll() {
        new SwingWorker<Void, Void>() {
            Session active;
            List<model.AttendanceRecord> history;
            @Override protected Void doInBackground() {
                active = sessionService.getActiveSession();
                history = attendanceService.attendeeHistory(user.getId(), 10);
                return null;
            }
            @Override protected void done() {
                int total = history.size();
                int attended = (int) history.stream().filter(h -> "Present".equalsIgnoreCase(h.getStatus()) || "Late".equalsIgnoreCase(h.getStatus())).count();
                int pct = total == 0 ? 0 : (int) ((attended * 100.0) / total);
                progress.setValue(pct);
                summary.setText("Total: " + total + " • Attended: " + attended);
                if (active == null) {
                    activeSessionLabel.setText("No active session");
                    markBtn.setEnabled(false);
                } else if ("lecture".equalsIgnoreCase(active.getSessionType())) {
                    activeSessionLabel.setText(active.getName() + " (Lecture - self marking disabled)");
                    markBtn.setEnabled(false);
                } else {
                    activeSessionLabel.setText(active.getName() + " • " + active.getSubject());
                    markBtn.setEnabled(true);
                }
                historyPanel.setRecords(history);
            }
        }.execute();
    }

    private void markAttendance() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                Session active = sessionService.getActiveSession();
                if (active == null) throw new RuntimeException("No open session");
                attendanceService.markAttendance(user.getId(), active, NetworkUtil.detectIpAddress(), DeviceFingerprint.generate(), "Present", "Self marked");
                return null;
            }
            @Override protected void done() {
                try { get(); ToastNotification.showSuccess(AttendeeDashboard.this, "Attendance marked 🎉"); refreshAll(); }
                catch (Exception ex) { ToastNotification.showError(AttendeeDashboard.this, ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()); }
            }
        }.execute();
    }
}
