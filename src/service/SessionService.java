package service;

import db.CloudDBConnection;
import model.Session;

import java.sql.*;
import java.time.LocalDateTime;

public class SessionService {
    public Session getActiveSession() {
        try (Connection conn = CloudDBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM sessions WHERE is_open=1 ORDER BY id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return map(rs);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load active session", e);
        }
    }

    public void createAndOpen(String name, String subject, String type, int officerId, int lockDurationMinutes, LocalDateTime openTime) {
        String sql = "INSERT INTO sessions(name, subject, opened_by, open_time, lock_duration_minutes, is_open, session_type) VALUES (?,?,?,?,?,1,?)";
        try (Connection conn = CloudDBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, subject);
            ps.setInt(3, officerId);
            ps.setTimestamp(4, Timestamp.valueOf(openTime));
            ps.setInt(5, lockDurationMinutes);
            ps.setString(6, type);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create session", e);
        }
    }

    public void closeSession(int id) {
        try (Connection conn = CloudDBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE sessions SET is_open=0, close_time=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Unable to close session", e);
        }
    }

    private Session map(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setSubject(rs.getString("subject"));
        s.setSessionType(rs.getString("session_type"));
        s.setLockDurationMinutes(rs.getInt("lock_duration_minutes"));
        Timestamp open = rs.getTimestamp("open_time");
        if (open != null) s.setOpenTime(open.toLocalDateTime());
        Timestamp close = rs.getTimestamp("close_time");
        if (close != null) s.setCloseTime(close.toLocalDateTime());
        s.setOpen(rs.getBoolean("is_open"));
        return s;
    }
}
