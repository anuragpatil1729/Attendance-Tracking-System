package ui;

import db.SyncManager;
import model.User;
import service.SessionService;
import ui.attendee.AttendeeDashboard;
import ui.components.ToastNotification;
import ui.officer.OfficerDashboard;
import util.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {
    private final SyncManager syncManager = new SyncManager();
    private final SessionService sessionService = new SessionService();
    private ScheduledExecutorService scheduleActivator;

    public MainFrame(User user) {
        super(Constants.APP_NAME + " • " + user.getRole());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Constants.BG);
        setLayout(new BorderLayout());

        if ("officer".equalsIgnoreCase(user.getRole())) {
            add(new OfficerDashboard(user, syncManager), BorderLayout.CENTER);
        } else {
            add(new AttendeeDashboard(user, syncManager), BorderLayout.CENTER);
        }

        syncManager.start();
        startScheduledSessionActivator();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                syncManager.stop();
                stopScheduledSessionActivator();
            }
        });
    }

    private void startScheduledSessionActivator() {
        scheduleActivator = Executors.newSingleThreadScheduledExecutor();
        scheduleActivator.scheduleAtFixedRate(() -> {
            try {
                int activated = sessionService.activateScheduled();
                if (activated > 0) {
                    ToastNotification.showInfo(MainFrame.this,
                            "Activated " + activated + " scheduled session(s).");
                }
            } catch (Exception ignored) {
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    private void stopScheduledSessionActivator() {
        if (scheduleActivator != null) {
            scheduleActivator.shutdownNow();
        }
    }
}
