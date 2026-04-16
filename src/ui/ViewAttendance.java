package ui;

import model.RollCall;
import service.AttendanceService;
import util.Constants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Reporting/search panel for attendance data with CSV export.
 */
public class ViewAttendance extends JPanel {
    private final AttendanceService service;
    private final JSpinner fromDate;
    private final JSpinner toDate;
    private final JComboBox<String> classCombo;
    private final JTextField nameSearch;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel summaryLabel;

    public ViewAttendance(AttendanceService service) {
        this.service = service;
        this.fromDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        this.toDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        this.classCombo = new JComboBox<>();
        this.nameSearch = new JTextField(16);
        this.tableModel = new DefaultTableModel(new String[]{"Roll No", "Name", "Class", "Date", "Status", "Remarks"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(tableModel);
        this.summaryLabel = new JLabel("Total: 0 | Present %: 0.00");

        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildTablePane(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        loadClasses();
        styleTable();
    }

    private JPanel buildFilterBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Constants.BG_COLOR);

        styleSpinner(fromDate);
        styleSpinner(toDate);

        classCombo.setPreferredSize(new Dimension(140, 36));
        classCombo.setBorder(new RoundedBorder(8));
        classCombo.setBackground(Constants.INPUT_BG);
        classCombo.setForeground(Constants.TEXT_COLOR);
        classCombo.setFont(Constants.APP_FONT);

        nameSearch.setPreferredSize(new Dimension(180, 36));
        nameSearch.setBorder(new RoundedBorder(8));
        nameSearch.setBackground(Constants.INPUT_BG);
        nameSearch.setForeground(Constants.TEXT_COLOR);
        nameSearch.setCaretColor(Constants.ACCENT_COLOR);
        nameSearch.setFont(Constants.APP_FONT);

        JButton searchBtn = actionButton("Search");
        searchBtn.addActionListener(e -> search());

        panel.add(label("From:"));
        panel.add(fromDate);
        panel.add(label("To:"));
        panel.add(toDate);
        panel.add(label("Class:"));
        panel.add(classCombo);
        panel.add(label("Name:"));
        panel.add(nameSearch);
        panel.add(searchBtn);

        return panel;
    }

    private JScrollPane buildTablePane() {
        table.setRowHeight(30);
        table.setFont(Constants.APP_FONT);
        table.setForeground(Constants.TEXT_COLOR);
        table.setBackground(Constants.INPUT_BG);
        table.setGridColor(Constants.SIDEBAR_COLOR);

        table.setDefaultRenderer(Object.class, new AttendanceRenderer());

        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(new RoundedBorder(10));
        return pane;
    }

    private JPanel buildBottomBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        summaryLabel.setForeground(Constants.TEXT_COLOR);
        summaryLabel.setFont(Constants.APP_FONT);

        JButton exportBtn = actionButton("Export CSV");
        exportBtn.addActionListener(e -> exportCsv());

        panel.add(summaryLabel, BorderLayout.WEST);
        panel.add(exportBtn, BorderLayout.EAST);
        return panel;
    }

    /**
     * Executes the filter query and updates summary metrics.
     */
    private void search() {
        tableModel.setRowCount(0);
        try {
            LocalDate from = toLocalDate(fromDate);
            LocalDate to = toLocalDate(toDate);
            String selectedClass = (String) classCombo.getSelectedItem();
            String nameQuery = nameSearch.getText().trim().toLowerCase();

            List<RollCall> records = service.generateSummaryReport(from, to, selectedClass);
            List<RollCall> filtered = new ArrayList<>();
            for (RollCall rc : records) {
                if (nameQuery.isEmpty() || rc.getName().toLowerCase().contains(nameQuery)) {
                    filtered.add(rc);
                }
            }

            int present = 0;
            for (RollCall rc : filtered) {
                tableModel.addRow(new Object[]{
                        rc.getRollNo(),
                        rc.getName(),
                        rc.getStudentClass(),
                        rc.getDate().format(DateTimeFormatter.ISO_DATE),
                        rc.getStatus(),
                        rc.getRemarks()
                });
                if ("Present".equalsIgnoreCase(rc.getStatus())) {
                    present++;
                }
            }

            double pct = filtered.isEmpty() ? 0.0 : (present * 100.0) / filtered.size();
            summaryLabel.setText("Total: " + filtered.size() + " | Present %: " + new DecimalFormat("0.00").format(pct));
        } catch (IllegalArgumentException ex) {
            summaryLabel.setText(ex.getMessage());
            summaryLabel.setForeground(Color.PINK);
        } catch (RuntimeException ex) {
            showDbError(ex.getMessage());
        }
    }

    /**
     * Writes current table rows into a timestamped CSV in project root.
     */
    private void exportCsv() {
        String fileName = "attendance_export_" + System.currentTimeMillis() + ".csv";
        Path path = Path.of(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            writer.write("Roll No,Name,Class,Date,Status,Remarks\n");
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    if (col > 0) {
                        line.append(',');
                    }
                    String value = String.valueOf(tableModel.getValueAt(row, col));
                    line.append('"').append(value.replace("\"", "\"\"")).append('"');
                }
                line.append('\n');
                writer.write(line.toString());
            }
            summaryLabel.setText(summaryLabel.getText() + " | Exported: " + fileName);
        } catch (IOException ex) {
            summaryLabel.setText("Failed to export CSV: " + ex.getMessage());
        }
    }

    private void loadClasses() {
        classCombo.removeAllItems();
        classCombo.addItem("All");
        try {
            for (String cls : service.getAllClasses()) {
                classCombo.addItem(cls);
            }
        } catch (RuntimeException ex) {
            showDbError(ex.getMessage());
        }
    }

    private void styleTable() {
        table.getTableHeader().setBackground(Constants.SIDEBAR_COLOR);
        table.getTableHeader().setForeground(Constants.ACCENT_COLOR);
        table.getTableHeader().setFont(Constants.APP_FONT.deriveFont(java.awt.Font.BOLD));
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Constants.TEXT_COLOR);
        l.setFont(Constants.APP_FONT);
        return l;
    }

    private JButton actionButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(new RoundedBorder(8));
        button.setBackground(Constants.INPUT_BG);
        button.setForeground(Constants.TEXT_COLOR);
        button.setFont(Constants.APP_FONT);
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(Constants.ACCENT_COLOR);
                button.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(Constants.INPUT_BG);
                button.setForeground(Constants.TEXT_COLOR);
            }
        });
        return button;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(140, 36));
        spinner.setBorder(new RoundedBorder(8));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(Constants.INPUT_BG);
        editor.getTextField().setForeground(Constants.TEXT_COLOR);
        editor.getTextField().setCaretColor(Constants.ACCENT_COLOR);
    }

    private LocalDate toLocalDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void showDbError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(null, msg, "DB Error", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Row renderer that color-codes status values.
     */
    private static class AttendanceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String status = String.valueOf(table.getValueAt(row, 4));
            if (!isSelected) {
                switch (status) {
                    case "Present" -> c.setBackground(new Color(46, 125, 50));
                    case "Absent" -> c.setBackground(new Color(183, 28, 28));
                    case "Late" -> c.setBackground(new Color(245, 158, 11));
                    default -> c.setBackground(row % 2 == 0 ? Constants.INPUT_BG : Constants.SIDEBAR_COLOR);
                }
                c.setForeground(Color.WHITE);
            }
            return c;
        }
    }
}
