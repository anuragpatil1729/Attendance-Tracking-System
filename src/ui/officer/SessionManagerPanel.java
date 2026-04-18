package ui.officer;

import model.Session;
import model.User;
import service.SessionService;
import ui.components.PanelHeader;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class SessionManagerPanel extends JPanel {

    private final SessionService sessionService = new SessionService();
    private final User user;

    private final JTextField name = new JTextField();
    private final JTextField subject = new JTextField();
    private final JSpinner lockSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 240, 1));
    private final JRadioButton practical = new JRadioButton("Practical", true);
    private final JRadioButton lecture = new JRadioButton("Lecture");
    private final JRadioButton openNow = new JRadioButton("Open Now", true);
    private final JRadioButton schedule = new JRadioButton("Schedule");
    private final JSpinner scheduleDateTime = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE));
    private final JPanel scheduleDateTimeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    private final JLabel openBanner = new JLabel("0 sessions open");

    private JTable table;
    private DefaultTableModel model;
    private Timer autoRefreshTimer;

    public SessionManagerPanel(User user) {
        this.user = user;

        setLayout(new BorderLayout(12, 12));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Constants.BG);

        container.add(buildForm());
        container.add(Box.createVerticalStrut(12));
        container.add(PanelHeader.create("Active Sessions"));
        container.add(Box.createVerticalStrut(8));
        container.add(buildBanner());
        container.add(Box.createVerticalStrut(8));
        container.add(buildTable());

        add(container, BorderLayout.CENTER);

        loadSessions();

        autoRefreshTimer = new Timer(5000, e -> loadSessions());
        autoRefreshTimer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (autoRefreshTimer != null && !autoRefreshTimer.isRunning()) {
            autoRefreshTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }

    private JPanel buildForm() {
        JPanel card = UiStyle.sectionCard(new GridLayout(0, 2, 8, 8), 16);

        UiStyle.styleField(name, "Session name");
        UiStyle.styleField(subject, "Subject");
        UiStyle.styleComponent(lockSpinner, 10);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(scheduleDateTime, "yyyy-MM-dd HH:mm");
        scheduleDateTime.setEditor(editor);
        UiStyle.styleComponent(scheduleDateTime, 10);
        scheduleDateTime.setPreferredSize(new Dimension(160, 30));
        scheduleDateTimeWrap.setOpaque(false);
        scheduleDateTimeWrap.add(scheduleDateTime);
        scheduleDateTimeWrap.setVisible(false);

        ButtonGroup g = new ButtonGroup();
        g.add(practical);
        g.add(lecture);
        practical.setOpaque(false);
        lecture.setOpaque(false);
        practical.setForeground(Constants.TEXT);
        lecture.setForeground(Constants.TEXT);

        ButtonGroup openModeGroup = new ButtonGroup();
        openModeGroup.add(openNow);
        openModeGroup.add(schedule);
        openNow.setOpaque(false);
        schedule.setOpaque(false);
        openNow.setForeground(Constants.TEXT);
        schedule.setForeground(Constants.TEXT);

        schedule.addActionListener(e -> scheduleDateTimeWrap.setVisible(true));
        openNow.addActionListener(e -> scheduleDateTimeWrap.setVisible(false));

        JButton create = UiStyle.createButton("Create Session", Constants.ACCENT, Color.BLACK);
        create.addActionListener(e -> createSession());

        card.add(label("Session Name"));
        card.add(name);
        card.add(label("Subject"));
        card.add(subject);

        card.add(label("Type"));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radios.setOpaque(false);
        radios.add(practical);
        radios.add(lecture);
        card.add(radios);

        card.add(label("Open Mode"));
        JPanel openModes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        openModes.setOpaque(false);
        openModes.add(openNow);
        openModes.add(schedule);
        openModes.add(scheduleDateTimeWrap);
        card.add(openModes);

        card.add(label("Lock Duration (min)"));
        card.add(lockSpinner);
        card.add(new JLabel());
        card.add(create);

        return card;
    }

    private JComponent buildBanner() {
        JPanel banner = UiStyle.sectionCard(new FlowLayout(FlowLayout.LEFT, 10, 6), 12);
        banner.setBackground(Constants.blend(Constants.SIDEBAR, Constants.ACCENT, 0.12f));

        openBanner.setForeground(Constants.TEXT);
        openBanner.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        banner.add(openBanner);
        return banner;
    }

    private JScrollPane buildTable() {

        String[] cols = {"ID", "Session Name", "Subject", "Type", "Action"};

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };

        table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new SessionRenderer());

        table.getColumnModel().getColumn(4).setCellRenderer(new CloseButtonRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new CloseButtonEditor());

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scroll = UiStyle.wrapScroll(table, Constants.BG);
        scroll.setPreferredSize(new Dimension(800, 260));
        return scroll;
    }

    private void createSession() {
        final boolean openNowSelected = openNow.isSelected();
        final String sessionName = name.getText().trim();
        final String subjectName = subject.getText().trim();
        final String sessionType = practical.isSelected() ? "practical" : "lecture";
        final Integer lockDuration = (Integer) lockSpinner.getValue();
        final Date selectedDateTime = (Date) scheduleDateTime.getValue();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (openNowSelected) {
                    sessionService.createAndOpen(
                            sessionName,
                            subjectName,
                            sessionType,
                            user.getId(),
                            lockDuration,
                            LocalDateTime.now());
                } else {
                    LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(selectedDateTime.getTime()), ZoneId.systemDefault());
                    sessionService.schedule(
                            sessionName,
                            subjectName,
                            sessionType,
                            user.getId(),
                            lockDuration,
                            when);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(SessionManagerPanel.this,
                            openNowSelected ? "Session opened 🚀" : "Session scheduled ⏱");
                    loadSessions();
                    name.setText("");
                    scheduleDateTime.setValue(new java.util.Date());
                    subject.setText("");
                } catch (Exception ex) {
                    ToastNotification.showError(SessionManagerPanel.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void closeSession(int sessionId) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                sessionService.closeSession(sessionId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showInfo(SessionManagerPanel.this, "Session closed");
                    loadSessions();
                } catch (Exception ex) {
                    ToastNotification.showError(SessionManagerPanel.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadSessions() {
        new SwingWorker<List<Session>, Void>() {
            @Override
            protected List<Session> doInBackground() {
                return sessionService.getOpenSessions();
            }

            @Override
            protected void done() {
                try {
                    List<Session> sessions = get();
                    model.setRowCount(0);
                    openBanner.setText(sessions.size() + " sessions open");

                    for (Session s : sessions) {
                        model.addRow(new Object[]{
                                s.getId(),
                                s.getName(),
                                s.getSubject(),
                                s.getSessionType(),
                                "Close"
                        });
                    }
                } catch (Exception ex) {
                    ToastNotification.showError(SessionManagerPanel.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Constants.TEXT);
        return l;
    }

    private static class SessionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR);
            }
            c.setForeground(Constants.TEXT);
            return c;
        }
    }

    private class CloseButtonRenderer extends JButton implements TableCellRenderer {
        private CloseButtonRenderer() {
            super("Close");
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setForeground(Constants.TEXT);
            setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Constants.RED);
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
            g2.setColor(Constants.TEXT);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR));
            return this;
        }
    }

    private class CloseButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Close");
        private int sessionId;

        private CloseButtonEditor() {
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorder(UiStyle.roundedBorder(10));
            button.setBackground(Constants.RED);
            button.setForeground(Constants.TEXT);
            UiStyle.installButtonEffects(button, Constants.RED);
            button.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        SessionManagerPanel.this,
                        "Close this session? Attendees will no longer be able to mark attendance.",
                        "Confirm Close",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                fireEditingStopped();
                closeSession(sessionId);
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "Close";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            sessionId = (Integer) model.getValueAt(row, 0);
            return button;
        }
    }
}
