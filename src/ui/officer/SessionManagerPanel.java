package ui.officer;

import model.Session;
import model.User;
import service.SessionService;
import ui.components.RoundedBorder;
import ui.components.ToastNotification;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.List;

public class SessionManagerPanel extends JPanel {

    private final SessionService sessionService = new SessionService();
    private final User user;

    private final JTextField name = new JTextField();
    private final JTextField subject = new JTextField();
    private final JSpinner lockSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 240, 1));
    private final JRadioButton practical = new JRadioButton("Practical", true);
    private final JRadioButton lecture = new JRadioButton("Lecture");

    private JTable table;
    private DefaultTableModel model;

    public SessionManagerPanel(User user) {
        this.user = user;

        setLayout(new BorderLayout());
        setBackground(Constants.BG);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Constants.BG);

        container.add(buildForm());
        container.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("Active Sessions");
        title.setForeground(Constants.ACCENT);
        title.setFont(Constants.FONT.deriveFont(Font.BOLD, 16f));

        container.add(title);
        container.add(Box.createVerticalStrut(6));

        container.add(buildTable());

        add(container, BorderLayout.CENTER);

        loadSessions();
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridLayout(0, 2, 8, 8));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(16),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        style(name);
        style(subject);
        style(lockSpinner);

        ButtonGroup g = new ButtonGroup();
        g.add(practical);
        g.add(lecture);

        JButton create = new JButton("Create & Open");
        create.setBorder(new RoundedBorder(10));
        create.setBackground(Constants.INPUT);
        create.setForeground(Constants.TEXT);

        create.addActionListener(e -> createSession());

        card.add(label("Session Name"));
        card.add(name);
        card.add(label("Subject"));
        card.add(subject);

        card.add(label("Type"));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radios.setOpaque(false);
        radios.add(practical);
        radios.add(lecture);
        card.add(radios);

        card.add(label("Lock Duration (min)"));
        card.add(lockSpinner);
        card.add(new JLabel());
        card.add(create);

        return card;
    }

    private JScrollPane buildTable() {

        String[] cols = { "ID", "Session Name", "Subject", "Type", "Action" };

        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 4; // only button column
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setBackground(Constants.INPUT);
        table.setForeground(Constants.TEXT);

        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        // Click listener
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();

                int sessionId = (int) model.getValueAt(row, 0);

                // Click on Action column
                if (col == 4) {
                    closeSession(sessionId);
                } else {
                    ToastNotification.showSuccess(SessionManagerPanel.this,
                            "Opened session ID: " + sessionId);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 250));

        return scroll;
    }

    private void createSession() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                sessionService.createAndOpen(
                        name.getText().trim(),
                        subject.getText().trim(),
                        practical.isSelected() ? "practical" : "lecture",
                        user.getId(),
                        (Integer) lockSpinner.getValue(),
                        LocalDateTime.now());
                return null;
            }

            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(SessionManagerPanel.this, "Session opened 🚀");

                    loadSessions();

                    name.setText("");
                    subject.setText("");

                } catch (Exception ex) {
                    ToastNotification.showError(SessionManagerPanel.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void closeSession(int sessionId) {
        sessionService.closeSession(sessionId);
        ToastNotification.showSuccess(this, "Session closed ❌");
        loadSessions();
    }

    private void loadSessions() {
        model.setRowCount(0);

        List<Session> sessions = sessionService.getOpenSessions();

        for (Session s : sessions) {
            model.addRow(new Object[] {
                    s.getId(),
                    s.getName(),
                    s.getSubject(),
                    s.getSessionType(),
                    "Close"
            });
        }
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Constants.TEXT);
        return l;
    }

    private void style(JComponent c) {
        c.setBorder(new RoundedBorder(10));
        c.setForeground(Constants.TEXT);
        c.setBackground(Constants.INPUT);
    }
}