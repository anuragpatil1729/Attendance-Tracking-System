package ui.officer;

import db.CloudDBConnection;
import ui.components.ToastNotification;
import ui.components.UiStyle;
import util.Constants;
import util.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
        JTableHeader header = table.getTableHeader();
        header.setBackground(Constants.blend(Constants.ACCENT, Constants.SIDEBAR, 0.35f));
        header.setForeground(Constants.TEXT);

        add(UiStyle.wrapScroll(table, Constants.BG), BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(buildAddCard(), BorderLayout.CENTER);

        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowActions.setOpaque(false);
        JButton deactivate = UiStyle.createButton("Deactivate Selected", Constants.ORANGE, Color.BLACK);
        deactivate.addActionListener(e -> deactivate(table.getSelectedRow() >= 0 ? (Integer) model.getValueAt(table.getSelectedRow(), 0) : -1));
        JButton reset = UiStyle.createButton("Reset Password", Constants.RED, Color.BLACK);
        reset.addActionListener(e -> resetPassword(table.getSelectedRow() >= 0 ? (Integer) model.getValueAt(table.getSelectedRow(), 0) : -1));
        rowActions.add(deactivate);
        rowActions.add(reset);
        actions.add(rowActions, BorderLayout.SOUTH);

        add(actions, BorderLayout.NORTH);
        installValidationReset();
        loadUsers();
    }

    private JComponent buildAddCard() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                UiStyle.roundedBorder(14),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

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

        card.add(title, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
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
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                model.setRowCount(0);
                try (Connection c = CloudDBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("SELECT id, username, full_name, email, role, is_active FROM users ORDER BY id DESC");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6)});
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }.execute();
    }

    private void addUser(String username, String password, String fullName, String email) {
        if (!validateInput()) {
            ToastNotification.showError(this, "Please complete all fields");
            return;
        }

        try (Connection c = CloudDBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password_hash,full_name,email,role,is_active) VALUES(?,?,?,?, 'attendee',1)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.executeUpdate();
            loadUsers();
            this.username.setText("");
            this.fullName.setText("");
            this.email.setText("");
            this.pwd.setText("");
            ToastNotification.showSuccess(this, "Attendee added");
        } catch (Exception e) {
            ToastNotification.showError(this, e.getMessage());
        }
    }

    private void deactivate(int id) {
        if (id < 0) {
            return;
        }
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadUsers();
            ToastNotification.showInfo(this, "User deactivated");
        } catch (Exception e) {
            ToastNotification.showError(this, e.getMessage());
        }
    }

    private void resetPassword(int id) {
        if (id < 0) {
            return;
        }
        String temp = JOptionPane.showInputDialog(this, "Enter new password");
        if (temp == null || temp.isBlank()) {
            return;
        }
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, PasswordUtil.hash(temp));
            ps.setInt(2, id);
            ps.executeUpdate();
            ToastNotification.showSuccess(this, "Password reset");
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
