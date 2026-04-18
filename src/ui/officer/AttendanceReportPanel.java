package ui.officer;

import db.ConnectionPool;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttendanceReportPanel extends JPanel {
    private static final String SEARCH_PLACEHOLDER = "Search attendee name...";

    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Session", "Type", "Status", "Marked At"}, 0);
    private final JLabel rowCountLabel = new JLabel("0 rows");
    private final JLabel thresholdLabel = new JLabel("⚠ 0 students below 75% attendance");
    private final Set<Integer> belowThresholdUserIds = new HashSet<>();

    private JTable table;

    public AttendanceReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new ThresholdRenderer());

        add(UiStyle.wrapScroll(table, Constants.BG), BorderLayout.CENTER);

        JPanel top = UiStyle.sectionCard(new FlowLayout(FlowLayout.LEFT, 8, 0), 14);

        JTextField search = new JTextField(18);
        UiStyle.styleField(search, "Search");
        UiStyle.installPlaceholder(search, SEARCH_PLACEHOLDER);

        JButton refresh = UiStyle.createButton("Filter/Search", Constants.INPUT, Constants.TEXT);
        refresh.addActionListener(e -> load(extractQuery(search)));

        JButton export = UiStyle.createButton("Export CSV", Constants.GREEN, Color.BLACK);
        export.addActionListener(e -> exportCsv());

        rowCountLabel.setForeground(Constants.TEXT);
        rowCountLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        thresholdLabel.setForeground(Constants.ORANGE);
        thresholdLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));

        top.add(search);
        top.add(refresh);
        top.add(export);
        top.add(Box.createHorizontalStrut(8));
        top.add(rowCountLabel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(thresholdLabel);

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.SOUTH);
        load("");
    }

    private String extractQuery(JTextField searchField) {
        return UiStyle.isShowingPlaceholder(searchField, SEARCH_PLACEHOLDER) ? "" : searchField.getText().trim();
    }

    private void load(String search) {
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                String sql = """
                        SELECT a.user_id, u.full_name, a.session_id, s.session_type, a.status, a.marked_at
                        FROM attendance a
                        JOIN users u ON u.id=a.user_id
                        JOIN sessions s ON s.id=a.session_id
                        WHERE u.full_name LIKE ?
                        ORDER BY a.marked_at DESC
                        """;
                List<Object[]> rows = new ArrayList<>();
                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, "%" + search + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getTimestamp(6)});
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    model.setRowCount(0);
                    belowThresholdUserIds.clear();
                    for (Object[] row : rows) {
                        model.addRow(row);
                    }
                    computeThresholds();
                    rowCountLabel.setText(model.getRowCount() + " rows");
                    thresholdLabel.setText("⚠ " + belowThresholdUserIds.size() + " students below 75% attendance");
                    table.repaint();
                } catch (Exception e) {
                    ToastNotification.showError(AttendanceReportPanel.this, e.getMessage());
                }
            }
        }.execute();
    }

    private void computeThresholds() {
        Map<Integer, int[]> stats = new HashMap<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            int userId = (Integer) model.getValueAt(row, 0);
            String status = String.valueOf(model.getValueAt(row, 4));
            int[] userStats = stats.computeIfAbsent(userId, k -> new int[2]);
            userStats[1]++;
            if ("Present".equalsIgnoreCase(status) || "Late".equalsIgnoreCase(status)) {
                userStats[0]++;
            }
        }

        for (Map.Entry<Integer, int[]> entry : stats.entrySet()) {
            int[] values = entry.getValue();
            double pct = values[1] == 0 ? 0 : (values[0] * 100.0) / values[1];
            if (pct < 75.0) {
                belowThresholdUserIds.add(entry.getKey());
            }
        }
    }

    private void exportCsv() {
        try (FileWriter fw = new FileWriter("attendance_export.csv")) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                fw.write(model.getColumnName(c) + (c == model.getColumnCount() - 1 ? "\n" : ","));
            }
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    fw.write(String.valueOf(model.getValueAt(r, c)).replace(",", " ") + (c == model.getColumnCount() - 1 ? "\n" : ","));
                }
            }
            ToastNotification.showSuccess(this, "Exported attendance_export.csv");
        } catch (Exception e) {
            ToastNotification.showError(this, e.getMessage());
        }
    }

    private class ThresholdRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            int userId = (Integer) table.getValueAt(row, 0);
            if (!isSelected) {
                Color base = row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR;
                c.setBackground(belowThresholdUserIds.contains(userId) ? Constants.blend(base, Constants.RED, 0.25f) : base);
            }
            c.setForeground(Constants.TEXT);
            return c;
        }
    }
}
