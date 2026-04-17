package ui.officer;

import db.CloudDBConnection;
import util.Constants;
import util.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AccountManagerPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Username","Full Name","Email","Role","Active"}, 0);

    public AccountManagerPanel() {
        setLayout(new BorderLayout(8,8));
        setBackground(Constants.BG);
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField username = new JTextField(10);
        JTextField fullName = new JTextField(10);
        JTextField email = new JTextField(10);
        JPasswordField pwd = new JPasswordField(10);
        JButton add = new JButton("Add Attendee");
        add.addActionListener(e -> addUser(username.getText(), new String(pwd.getPassword()), fullName.getText(), email.getText()));
        JButton deactivate = new JButton("Deactivate Selected");
        deactivate.addActionListener(e -> deactivate(table.getSelectedRow()>=0 ? (Integer) model.getValueAt(table.getSelectedRow(),0) : -1));
        JButton reset = new JButton("Reset Password");
        reset.addActionListener(e -> resetPassword(table.getSelectedRow()>=0 ? (Integer) model.getValueAt(table.getSelectedRow(),0) : -1));
        actions.add(username); actions.add(fullName); actions.add(email); actions.add(pwd); actions.add(add); actions.add(deactivate); actions.add(reset);
        add(actions, BorderLayout.NORTH);
        loadUsers();
    }

    private void loadUsers() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                model.setRowCount(0);
                try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, username, full_name, email, role, is_active FROM users ORDER BY id DESC"); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6)});
                } catch (Exception e) { throw new RuntimeException(e); }
                return null;
            }
            @Override protected void done(){ }
        }.execute();
    }

    private void addUser(String username, String password, String fullName, String email) {
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password_hash,full_name,email,role,is_active) VALUES(?,?,?,?, 'attendee',1)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.executeUpdate();
            loadUsers();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void deactivate(int id) {
        if (id < 0) return;
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active=0 WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadUsers();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void resetPassword(int id) {
        if (id < 0) return;
        String temp = JOptionPane.showInputDialog(this, "Enter new password");
        if (temp == null || temp.isBlank()) return;
        try (Connection c = CloudDBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, PasswordUtil.hash(temp)); ps.setInt(2, id); ps.executeUpdate();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }
}
