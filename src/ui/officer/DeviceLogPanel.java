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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeviceLogPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"User", "IP", "Fingerprint", "Time", "Status"}, 0);
    private final JLabel blockedBadge = new JLabel("0 blocked attempts");

    public DeviceLogPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTable table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new LogRenderer());
        JTableHeader header = table.getTableHeader();
        header.setBackground(Constants.blend(Constants.ACCENT, Constants.SIDEBAR, 0.35f));
        header.setForeground(Constants.TEXT);

        add(UiStyle.wrapScroll(table, Constants.BG), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JButton refresh = UiStyle.createButton("Refresh", Constants.INPUT, Constants.TEXT);
        refresh.addActionListener(e -> load());

        blockedBadge.setOpaque(true);
        blockedBadge.setBackground(Constants.RED);
        blockedBadge.setForeground(Color.BLACK);
        blockedBadge.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(10),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        top.add(refresh);
        top.add(blockedBadge);
        add(top, BorderLayout.NORTH);
        load();
    }

    private void load() {
        model.setRowCount(0);
        int blockedCount = 0;
        try (Connection c = CloudDBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT user_id, ip_address, device_fingerprint, login_time, attempt_status FROM device_log ORDER BY id DESC LIMIT 500");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString(5);
                if ("blocked".equalsIgnoreCase(status)) {
                    blockedCount++;
                }
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4), status});
            }
            blockedBadge.setText(blockedCount + " blocked attempts");
        } catch (Exception e) {
            ToastNotification.showError(this, e.getMessage());
        }
    }

    private static class LogRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            String status = String.valueOf(table.getValueAt(row, 4));
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR);
            }
            c.setForeground(Constants.TEXT);
            c.setFont(Constants.FONT.deriveFont("blocked".equalsIgnoreCase(status) ? Font.BOLD : Font.PLAIN, 14f));
            return c;
        }
    }
}
