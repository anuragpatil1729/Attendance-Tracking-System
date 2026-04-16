package ui;

import model.RollCall;
import service.AttendanceService;
import util.Constants;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance capture panel with date/class filters and bulk save.
 */
public class AttendanceForm extends JPanel {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    private final AttendanceService service;
    private final JSpinner dateSpinner;
    private final JComboBox<String> classCombo;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusBar;

    private final Map<Integer, Integer> rowToStudentId = new HashMap<>();

    public AttendanceForm(AttendanceService service) {
        this.service = service;
        this.dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        this.classCombo = new JComboBox<>();
        this.tableModel = new DefaultTableModel(new String[]{"Roll No", "Name", "Class", "Status", "Remarks"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4;
            }
        };
        this.table = new JTable(tableModel);
        this.statusBar = new JLabel("Ready.");

        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildFilterPanel(), BorderLayout.NORTH);
        add(buildTablePane(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadClassOptions();
        styleTable();
    }

    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Constants.BG_COLOR);

        styleInput(dateSpinner);
        styleInput(classCombo);

        JButton loadButton = createActionButton("Load Students");
        JButton saveButton = createActionButton("Save All");

        loadButton.addActionListener(e -> loadStudents());
        saveButton.addActionListener(e -> saveAll());

        panel.add(newLabel("Date:"));
        panel.add(dateSpinner);
        panel.add(newLabel("Class:"));
        classCombo.setPreferredSize(new Dimension(150, 36));
        panel.add(classCombo);
        panel.add(loadButton);
        panel.add(saveButton);

        return panel;
    }

    private JScrollPane buildTablePane() {
        table.setRowHeight(30);
        table.setFont(Constants.APP_FONT);
        table.setBackground(Constants.INPUT_BG);
        table.setForeground(Constants.TEXT_COLOR);
        table.setGridColor(Constants.SIDEBAR_COLOR);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Present", "Absent", "Late"});
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusCombo));

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Constants.INPUT_BG : Constants.SIDEBAR_COLOR);
                    c.setForeground(Constants.TEXT_COLOR);
                }
                return c;
            }
        });

        JScrollPane pane = new JScrollPane(table);
        pane.getViewport().setBackground(Constants.BG_COLOR);
        pane.setBorder(new RoundedBorder(10));
        return pane;
    }

    private JLabel buildStatusBar() {
        statusBar.setForeground(Constants.TEXT_COLOR);
        statusBar.setFont(Constants.APP_FONT);
        statusBar.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        return statusBar;
    }

    /**
     * Loads classes into class dropdown.
     */
    private void loadClassOptions() {
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

    /**
     * Loads students filtered by selected class and date.
     */
    private void loadStudents() {
        tableModel.setRowCount(0);
        rowToStudentId.clear();

        String selectedClass = (String) classCombo.getSelectedItem();
        LocalDate date = extractDate(dateSpinner);

        try {
            List<RollCall> students = service.getAllStudents();
            int rowIndex = 0;
            for (RollCall student : students) {
                if ("All".equals(selectedClass) || student.getStudentClass().equals(selectedClass)) {
                    tableModel.addRow(new Object[]{
                            student.getRollNo(),
                            student.getName(),
                            student.getStudentClass(),
                            "Present",
                            ""
                    });
                    rowToStudentId.put(rowIndex++, student.getStudentId());
                }
            }
            statusBar.setText("Loaded " + tableModel.getRowCount() + " students for " + date.format(DATE_FMT));
        } catch (RuntimeException ex) {
            showDbError(ex.getMessage());
        }
    }

    /**
     * Collects table rows and sends them to service as a bulk save.
     */
    private void saveAll() {
        try {
            LocalDate date = extractDate(dateSpinner);
            List<RollCall> records = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                Integer studentId = rowToStudentId.get(row);
                if (studentId == null) {
                    continue;
                }

                RollCall rc = new RollCall();
                rc.setStudentId(studentId);
                rc.setDate(date);
                rc.setStatus(String.valueOf(tableModel.getValueAt(row, 3)));
                rc.setRemarks(String.valueOf(tableModel.getValueAt(row, 4)));
                records.add(rc);
            }
            service.markBulk(records);
            statusBar.setText("Attendance saved for " + records.size() + " records.");
        } catch (IllegalArgumentException ex) {
            statusBar.setForeground(Color.PINK);
            statusBar.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            showDbError(ex.getMessage());
        }
    }

    private void styleTable() {
        JTableHeader header = table.getTableHeader();
        header.setBackground(Constants.SIDEBAR_COLOR);
        header.setForeground(Constants.ACCENT_COLOR);
        header.setFont(Constants.APP_FONT.deriveFont(java.awt.Font.BOLD));
    }

    private JLabel newLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Constants.TEXT_COLOR);
        label.setFont(Constants.APP_FONT);
        return label;
    }

    private void styleInput(Component component) {
        if (component instanceof JSpinner spinner) {
            spinner.setPreferredSize(new Dimension(150, 36));
            spinner.setBorder(new RoundedBorder(8));
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            JTextField tf = editor.getTextField();
            tf.setBackground(Constants.INPUT_BG);
            tf.setForeground(Constants.TEXT_COLOR);
            tf.setCaretColor(Constants.ACCENT_COLOR);
            tf.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setBackground(Constants.INPUT_BG);
            combo.setForeground(Constants.TEXT_COLOR);
            combo.setBorder(new RoundedBorder(8));
            combo.setFont(Constants.APP_FONT);
        }
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Constants.INPUT_BG);
        button.setForeground(Constants.TEXT_COLOR);
        button.setFont(Constants.APP_FONT);
        button.setBorder(new RoundedBorder(8));
        button.setFocusPainted(false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Constants.ACCENT_COLOR);
                button.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Constants.INPUT_BG);
                button.setForeground(Constants.TEXT_COLOR);
            }
        });
        return button;
    }

    private LocalDate extractDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void showDbError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(null, msg, "DB Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        statusBar.setText("Database error occurred.");
    }
}
