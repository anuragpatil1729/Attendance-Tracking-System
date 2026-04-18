package ui.officer;

import db.CloudDBConnection;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AttendanceReportPanel extends JPanel {
    private static final String SEARCH_PLACEHOLDER = "Search attendee name...";

    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Session", "Type", "Status", "Marked At"}, 0);
    private final JLabel rowCountLabel = new JLabel("0 rows");

    public AttendanceReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTable table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new ZebraRenderer());
        JTableHeader header = table.getTableHeader();
        header.setBackground(Constants.blend(Constants.ACCENT, Constants.SIDEBAR, 0.35f));
        header.setForeground(Constants.TEXT);

        add(UiStyle.wrapScroll(table, Constants.BG), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);

        JTextField search = new JTextField(18);
        UiStyle.styleField(search, "Search");
        UiStyle.installPlaceholder(search, SEARCH_PLACEHOLDER);

        JButton refresh = UiStyle.createButton("Filter/Search", Constants.INPUT, Constants.TEXT);
        refresh.addActionListener(e -> load(extractQuery(search)));

        JButton export = UiStyle.createButton("Export CSV", Constants.GREEN, Color.BLACK);
        export.addActionListener(e -> exportCsv());

        rowCountLabel.setForeground(Constants.TEXT);
        rowCountLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));

        top.add(search);
        top.add(refresh);
        top.add(export);
        top.add(Box.createHorizontalStrut(8));
        top.add(rowCountLabel);

        add(top, BorderLayout.NORTH);
        load("");
    }

    private String extractQuery(JTextField searchField) {
        return UiStyle.isShowingPlaceholder(searchField, SEARCH_PLACEHOLDER) ? "" : searchField.getText().trim();
    }

    private void load(String search) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                model.setRowCount(0);
                String sql = """
                        SELECT a.user_id, u.full_name, a.session_id, s.session_type, a.status, a.marked_at
                        FROM attendance a
                        JOIN users u ON u.id=a.user_id
                        JOIN sessions s ON s.id=a.session_id
                        WHERE u.full_name LIKE ?
                        ORDER BY a.marked_at DESC
                        """;
                try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, "%" + search + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getTimestamp(6)});
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            protected void done() {
                rowCountLabel.setText(model.getRowCount() + " rows");
            }
        }.execute();
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

    private static class ZebraRenderer extends DefaultTableCellRenderer {
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
}
