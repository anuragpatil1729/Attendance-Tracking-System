package ui.officer;

import db.CloudDBConnection;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AttendanceReportPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Session", "Type", "Status", "Marked At"}, 0);

    public AttendanceReportPanel() {
        setLayout(new BorderLayout(8,8));
        setBackground(Constants.BG);
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
        JTextField search = new JTextField(16);
        JButton refresh = new JButton("Filter/Search");
        refresh.addActionListener(e -> load(search.getText().trim()));
        JButton export = new JButton("Export CSV");
        export.addActionListener(e -> exportCsv());
        top.add(search); top.add(refresh); top.add(export);
        add(top, BorderLayout.NORTH);
        load("");
    }

    private void load(String search) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
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
            JOptionPane.showMessageDialog(this, "Exported attendance_export.csv");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
