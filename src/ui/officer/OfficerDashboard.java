package ui.officer;

import db.SyncManager;
import model.User;
import service.SessionService;
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
        tabs.addTab("Overview", statsPanel(syncManager));
        tabs.addTab("Session Manager", new SessionManagerPanel(user));
        tabs.addTab("Attendance Reports", new AttendanceReportPanel());
        tabs.addTab("Accounts", new AccountManagerPanel());
        tabs.addTab("Device Logs", new DeviceLogPanel());

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
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        JLabel t = new JLabel(title);
        t.setForeground(Constants.TEXT);
        t.setFont(Constants.FONT.deriveFont(Font.PLAIN, 12f));

        valueLabel.setForeground(Constants.ACCENT);
        valueLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 32f));

        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(0, 5));
        stripe.setBackground(Constants.ACCENT);

        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(stripe, BorderLayout.SOUTH);

        installHoverLift(card);
        return card;
    }

    private void installHoverLift(JPanel card) {
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createEmptyBorder(12, 16, 18, 16));
                card.setBackground(Constants.blend(Constants.SIDEBAR, Constants.ACCENT, 0.08f));
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));
                card.setBackground(Constants.SIDEBAR);
                card.repaint();
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
}
