package ui;

import db.SyncManager;
import model.User;
import ui.attendee.AttendeeDashboard;
import ui.officer.OfficerDashboard;
import util.Constants;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final SyncManager syncManager = new SyncManager();

    public MainFrame(User user) {
        super(Constants.APP_NAME + " • " + user.getRole());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Constants.BG);
        setLayout(new BorderLayout());

        if ("officer".equalsIgnoreCase(user.getRole())) {
            add(new OfficerDashboard(user, syncManager), BorderLayout.CENTER);
        } else {
            add(new AttendeeDashboard(user, syncManager), BorderLayout.CENTER);
        }

        syncManager.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                syncManager.stop();
            }
        });
    }
}
