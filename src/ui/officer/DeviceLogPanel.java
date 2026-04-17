package ui.officer;

import db.CloudDBConnection;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeviceLogPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"User","IP","Fingerprint","Time","Status"}, 0);

    public DeviceLogPanel() {
        setLayout(new BorderLayout());
        setBackground(Constants.BG);
        JTable table = new JTable(model);
        table.setDefaultRenderer(Object.class, new LogRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh"); refresh.addActionListener(e -> load());
        add(refresh, BorderLayout.NORTH);
        load();
    }

    private void load() {
        model.setRowCount(0);
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT user_id, ip_address, device_fingerprint, login_time, attempt_status FROM device_log ORDER BY id DESC LIMIT 500"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4), rs.getString(5)});
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private static class LogRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = String.valueOf(table.getValueAt(row, 4));
            if (!isSelected) {
                c.setBackground("blocked".equalsIgnoreCase(status) ? Constants.RED : (row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR));
                c.setForeground(Constants.TEXT);
            }
            return c;
        }
    }
}
