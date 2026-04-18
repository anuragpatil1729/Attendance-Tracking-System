package ui.officer;

import db.ConnectionPool;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;
import util.PasswordUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class AccountManagerPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Username", "Full Name", "Email", "Role", "Active"}, 0);

    private final JTextField username = new JTextField(10);
    private final JTextField fullName = new JTextField(10);
    private final JTextField email = new JTextField(10);
    private final JPasswordField pwd = new JPasswordField(10);

    public AccountManagerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Constants.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTable table = new JTable(model);
        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new ZebraRenderer());

        add(UiStyle.wrapScroll(table, Constants.BG), BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(8, 8));
        actions.setOpaque(false);
        actions.add(buildAddCard(), BorderLayout.CENTER);

        JPanel rowActions = UiStyle.sectionCard(new FlowLayout(FlowLayout.LEFT, 8, 0), 14);
        JButton importCsv = UiStyle.createButton("Import CSV", Constants.GREEN, Color.BLACK);
        importCsv.addActionListener(e -> importCsv());
        JButton deactivate = UiStyle.createButton("Deactivate Selected", Constants.ORANGE, Color.BLACK);
        deactivate.addActionListener(e -> deactivate(table.getSelectedRow() >= 0 ? (Integer) model.getValueAt(table.getSelectedRow(), 0) : -1));
        JButton reset = UiStyle.createButton("Reset Password", Constants.RED, Constants.TEXT);
        reset.addActionListener(e -> resetPassword(table.getSelectedRow() >= 0 ? (Integer) model.getValueAt(table.getSelectedRow(), 0) : -1));
        rowActions.add(importCsv);
        rowActions.add(deactivate);
        rowActions.add(reset);
        actions.add(rowActions, BorderLayout.SOUTH);

        add(actions, BorderLayout.NORTH);
        installValidationReset();
        loadUsers();
    }

    private JComponent buildAddCard() {
        JPanel card = UiStyle.sectionCard(new BorderLayout(8, 8), 14);

        JLabel title = new JLabel("Add New Attendee");
        title.setForeground(Constants.ACCENT);
        title.setFont(Constants.FONT.deriveFont(Font.BOLD, 16f));

        JPanel fields = new JPanel(new GridLayout(1, 5, 8, 0));
        fields.setOpaque(false);

        UiStyle.styleField(username, "Username");
        UiStyle.styleField(fullName, "Full Name");
        UiStyle.styleField(email, "Email");
        UiStyle.styleField(pwd, "Password");

        JButton add = UiStyle.createButton("Add Attendee", Constants.ACCENT, Color.BLACK);
        add.addActionListener(e -> addUser(username.getText().trim(), new String(pwd.getPassword()).trim(), fullName.getText().trim(), email.getText().trim()));

        fields.add(username);
        fields.add(fullName);
        fields.add(email);
        fields.add(pwd);
        fields.add(add);

        JPanel fieldsWrap = new JPanel(new BorderLayout());
        fieldsWrap.setOpaque(false);
        fieldsWrap.setPreferredSize(new Dimension(0, 44));
        fieldsWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        fieldsWrap.add(fields, BorderLayout.CENTER);

        card.add(title, BorderLayout.NORTH);
        card.add(fieldsWrap, BorderLayout.CENTER);
        return card;
    }

    private void installValidationReset() {
        UiStyle.onType(username, () -> username.setBorder(UiStyle.roundedBorder(12)));
        UiStyle.onType(fullName, () -> fullName.setBorder(UiStyle.roundedBorder(12)));
        UiStyle.onType(email, () -> email.setBorder(UiStyle.roundedBorder(12)));
        UiStyle.onType(pwd, () -> pwd.setBorder(UiStyle.roundedBorder(12)));
    }

    private boolean validateInput() {
        boolean valid = true;
        valid &= validateField(username);
        valid &= validateField(fullName);
        valid &= validateField(email);
        valid &= validateField(pwd);
        return valid;
    }

    private boolean validateField(JTextField field) {
        if (field.getText().isBlank()) {
            field.setBorder(BorderFactory.createLineBorder(Constants.RED, 2, true));
            return false;
        }
        return true;
    }

    private void loadUsers() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<>();
                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement("SELECT id, username, full_name, email, role, is_active FROM users ORDER BY id DESC");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6)});
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
                    for (Object[] row : rows) {
                        model.addRow(row);
                    }
                } catch (Exception e) {
                    ToastNotification.showError(AccountManagerPanel.this, e.getMessage());
                }
            }
        }.execute();
    }

    private void addUser(String username, String password, String fullName, String email) {
        if (!validateInput()) {
            ToastNotification.showError(this, "Please complete all fields");
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password_hash,full_name,email,role,is_active) VALUES(?,?,?,?, 'attendee',1)")) {
                    ps.setString(1, username);
                    ps.setString(2, PasswordUtil.hash(password));
                    ps.setString(3, fullName);
                    ps.setString(4, email);
                    ps.executeUpdate();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    AccountManagerPanel.this.username.setText("");
                    AccountManagerPanel.this.fullName.setText("");
                    AccountManagerPanel.this.email.setText("");
                    AccountManagerPanel.this.pwd.setText("");
                    loadUsers();
                    ToastNotification.showSuccess(AccountManagerPanel.this, "Attendee added");
                } catch (Exception e) {
                    ToastNotification.showError(AccountManagerPanel.this, e.getMessage());
                }
            }
        }.execute();
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import attendees CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int imported = 0;
                int duplicates = 0;
                String sql = "INSERT INTO users(username,password_hash,full_name,email,role,is_active) VALUES(?,?,?,?, 'attendee',1)";

                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql);
                     BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        String[] cols = line.split(",", -1);
                        if (cols.length < 4) {
                            continue;
                        }
                        try {
                            ps.setString(1, cols[0].trim());
                            ps.setString(2, PasswordUtil.hash(cols[3].trim()));
                            ps.setString(3, cols[1].trim());
                            ps.setString(4, cols[2].trim());
                            ps.executeUpdate();
                            imported++;
                        } catch (SQLIntegrityConstraintViolationException dup) {
                            duplicates++;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return new int[]{imported, duplicates};
            }

            @Override
            protected void done() {
                try {
                    int[] counts = get();
                    loadUsers();
                    ToastNotification.showInfo(AccountManagerPanel.this, "Imported " + counts[0] + ", skipped " + counts[1] + " duplicates");
                } catch (Exception e) {
                    ToastNotification.showError(AccountManagerPanel.this, e.getMessage());
                }
            }
        }.execute();
    }

    private void deactivate(int id) {
        if (id < 0) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active=0 WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadUsers();
                    ToastNotification.showInfo(AccountManagerPanel.this, "User deactivated");
                } catch (Exception e) {
                    ToastNotification.showError(AccountManagerPanel.this, e.getMessage());
                }
            }
        }.execute();
    }

    private void resetPassword(int id) {
        if (id < 0) {
            return;
        }
        String temp = JOptionPane.showInputDialog(this, "Enter new password");
        if (temp == null || temp.isBlank()) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection c = ConnectionPool.getConnection();
                     PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
                    ps.setString(1, PasswordUtil.hash(temp));
                    ps.setInt(2, id);
                    ps.executeUpdate();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showSuccess(AccountManagerPanel.this, "Password reset");
                } catch (Exception e) {
                    ToastNotification.showError(AccountManagerPanel.this, e.getMessage());
                }
            }
        }.execute();
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
