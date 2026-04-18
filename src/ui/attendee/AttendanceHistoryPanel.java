package ui.attendee;

import model.AttendanceRecord;
import ui.components.UiStyle;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttendanceHistoryPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Session", "Status", "When", "Sync"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public AttendanceHistoryPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JTable table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new HistoryRenderer());
        add(UiStyle.wrapScroll(table, Constants.SIDEBAR), BorderLayout.CENTER);
    }

    public void setRecords(List<AttendanceRecord> records) {
        model.setRowCount(0);
        for (AttendanceRecord r : records) {
            model.addRow(new Object[]{
                    r.getSessionId(),
                    r.getStatus(),
                    r.getMarkedAt() == null ? "-" : r.getMarkedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    r.getSyncStatus()
            });
        }
    }

    private static class HistoryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Constants.INPUT : Constants.SIDEBAR);
            }
            c.setForeground(Constants.TEXT);
            return c;
        }
    }
}
