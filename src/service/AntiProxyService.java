package service;

import db.ConnectionPool;
import model.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;

public class AntiProxyService {
    public String check(int userId, Session session, String ip, String fingerprint) {
        String sql = """
                SELECT user_id, marked_at
                FROM attendance
                WHERE session_id=? AND ip_address=? AND device_fingerprint=?
                ORDER BY marked_at DESC
                LIMIT 1
                """;
        try (Connection conn = ConnectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, session.getId());
            ps.setString(2, ip);
            ps.setString(3, fingerprint);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int previousUser = rs.getInt("user_id");
                if (previousUser == userId) {
                    return null;
                }
                LocalDateTime markedAt = rs.getTimestamp("marked_at").toLocalDateTime();
                LocalDateTime until = markedAt.plusMinutes(session.getLockDurationMinutes());
                if (LocalDateTime.now().isBefore(until)) {
                    long mins = Math.max(1, Duration.between(LocalDateTime.now(), until).toMinutes());
                    return "This device is locked for " + mins + " more minutes.";
                }
                return "Proxy attempt blocked: this device was already used by another attendee.";
            }
        } catch (Exception e) {
            throw new RuntimeException("Anti-proxy validation failed", e);
        }
    }
}
