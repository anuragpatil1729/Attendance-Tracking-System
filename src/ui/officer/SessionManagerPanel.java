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
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
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

    private final JLabel openBanner = new JLabel("0 sessions open");

    private JTable table;
    private DefaultTableModel model;

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
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridLayout(0, 2, 8, 8));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(16),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        UiStyle.styleField(name, "Session name");
        UiStyle.styleField(subject, "Subject");
        UiStyle.styleComponent(lockSpinner, 10);

        ButtonGroup g = new ButtonGroup();
        g.add(practical);
        g.add(lecture);
        practical.setOpaque(false);
        lecture.setOpaque(false);
        practical.setForeground(Constants.TEXT);
        lecture.setForeground(Constants.TEXT);

        JButton create = UiStyle.createButton("Create & Open", Constants.ACCENT, Color.BLACK);
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

        card.add(label("Lock Duration (min)"));
        card.add(lockSpinner);
        card.add(new JLabel());
        card.add(create);

        return card;
    }

    private JComponent buildBanner() {
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        banner.setBackground(Constants.blend(Constants.SIDEBAR, Constants.ACCENT, 0.12f));
        banner.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(12),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

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

        JTableHeader header = table.getTableHeader();
        header.setBackground(Constants.blend(Constants.ACCENT, Constants.SIDEBAR, 0.35f));
        header.setForeground(Constants.TEXT);

        table.getColumnModel().getColumn(4).setCellRenderer(new CloseButtonRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new CloseButtonEditor());

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scroll = UiStyle.wrapScroll(table, Constants.BG);
        scroll.setPreferredSize(new Dimension(800, 260));
        return scroll;
    }

    private void createSession() {
        new SwingWorker<Void, Void>() {
            @Override
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

            @Override
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
            setBorder(UiStyle.roundedBorder(10));
            setBackground(Constants.RED);
            setForeground(Color.BLACK);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class CloseButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Close");
        private int sessionId;

        private CloseButtonEditor() {
            button.setFocusPainted(false);
            button.setBorder(UiStyle.roundedBorder(10));
            button.setBackground(Constants.RED);
            button.setForeground(Color.BLACK);
            button.addActionListener(e -> {
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
