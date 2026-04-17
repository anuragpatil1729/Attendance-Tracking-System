package ui.officer;

import db.SyncManager;
import model.User;
import service.SessionService;
import util.Constants;

import javax.swing.*;
import java.awt.*;

public class OfficerDashboard extends JPanel {

    private final SessionService sessionService = new SessionService();
    private JLabel openSessionsValue;

    public OfficerDashboard(User user, SyncManager syncManager) {
        setLayout(new BorderLayout());
        setBackground(Constants.BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", statsPanel(syncManager));
        tabs.addTab("Session Manager", new SessionManagerPanel(user));
        tabs.addTab("Attendance Reports", new AttendanceReportPanel());
        tabs.addTab("Accounts", new AccountManagerPanel());
        tabs.addTab("Device Logs", new DeviceLogPanel());

        add(tabs, BorderLayout.CENTER);

        loadOpenSessions(); // 🔥 important
    }

    private JPanel statsPanel(SyncManager syncManager) {
        JPanel p = new JPanel(new GridLayout(1, 4, 8, 8));
        p.setBackground(Constants.BG);

        p.add(statCard("Total Attendees", "See Accounts tab"));

        // 🔥 dynamic label
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
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Constants.SIDEBAR);

        JLabel t = new JLabel(title);
        t.setForeground(Constants.ACCENT);

        valueLabel.setForeground(Constants.TEXT);
        valueLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));

        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        return card;
    }

    // 🔥 THIS IS THE REAL FIX
    private void loadOpenSessions() {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return sessionService.getOpenSessions().size();
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    openSessionsValue.setText(String.valueOf(count));
                } catch (Exception e) {
                    openSessionsValue.setText("Error");
                }
            }
        }.execute();
    }
}