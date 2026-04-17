package ui.officer;

import db.SyncManager;
import model.User;
import util.Constants;

import javax.swing.*;
import java.awt.*;

public class OfficerDashboard extends JPanel {
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
    }

    private JPanel statsPanel(SyncManager syncManager) {
        JPanel p = new JPanel(new GridLayout(1, 4, 8, 8));
        p.setBackground(Constants.BG);
        p.add(statCard("Total Attendees", "See Accounts tab"));
        p.add(statCard("Open Sessions", "See Session tab"));
        p.add(statCard("Today's Attendance %", "See Reports tab"));
        p.add(statCard("Pending Syncs", String.valueOf(syncManager.pendingCount())));
        return p;
    }

    private JPanel statCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Constants.SIDEBAR);
        JLabel t = new JLabel(title); t.setForeground(Constants.ACCENT);
        JLabel v = new JLabel(value); v.setForeground(Constants.TEXT);
        v.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        card.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        return card;
    }
}
