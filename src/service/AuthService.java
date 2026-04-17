package service;

import db.CloudDBConnection;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {
    public User login(String username, String password) {
        try (Connection conn = CloudDBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username=? AND is_active=1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String hash = rs.getString("password_hash");

                System.out.println("INPUT PASSWORD: [" + password + "]");
                System.out.println("DB HASH: " + hash);
                System.out.println("MATCH RESULT: " + PasswordUtil.verify(password, hash));
                if (!PasswordUtil.verify(password, hash)) {
                    return null;
                }
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setFullName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setActive(rs.getBoolean("is_active"));
                return u;
            }
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }
}
