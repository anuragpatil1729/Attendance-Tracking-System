package service;

import db.ConnectionPool;
import model.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SessionService {

    public void createAndOpen(String name, String subject, String type,
                              int userId, int lockDuration, LocalDateTime time) {

        String sql = "INSERT INTO sessions (name, subject, session_type, opened_by, open_time, lock_duration_minutes, is_open) VALUES (?, ?, ?, ?, ?, ?, 1)";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, subject);
            ps.setString(3, type);
            ps.setInt(4, userId);
            ps.setTimestamp(5, Timestamp.valueOf(time));
            ps.setInt(6, lockDuration);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    public void schedule(String name, String subject, String type,
                         int userId, int lockDuration, LocalDateTime openTime) {
        String sql = "INSERT INTO sessions (name, subject, session_type, opened_by, open_time, lock_duration_minutes, is_open) VALUES (?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, subject);
            ps.setString(3, type);
            ps.setInt(4, userId);
            ps.setTimestamp(5, Timestamp.valueOf(openTime));
            ps.setInt(6, lockDuration);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to schedule session", e);
        }
    }

    public int activateScheduled() {
        String sql = "UPDATE sessions SET is_open=1 WHERE is_open=0 AND open_time <= NOW()";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to activate scheduled sessions", e);
        }
    }

    public Session getActiveSession() {
        String sql = "SELECT id, name, subject, session_type, lock_duration_minutes, open_time FROM sessions WHERE is_open = 1 LIMIT 1";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                Session s = new Session();

                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setSubject(rs.getString("subject"));
                s.setSessionType(rs.getString("session_type"));
                s.setLockDurationMinutes(rs.getInt("lock_duration_minutes"));
                Timestamp openTime = rs.getTimestamp("open_time");
                if (openTime != null) {
                    s.setOpenTime(openTime.toLocalDateTime());
                }

                return s;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch active session", e);
        }

        return null;
    }

    public void closeSession(int sessionId) {
        String sql = "UPDATE sessions SET is_open=0, close_time=NOW() WHERE id=?";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to close session", e);
        }
    }

    public List<Session> getOpenSessions() {
        List<Session> list = new ArrayList<>();

        String sql = "SELECT id, name, subject, session_type, lock_duration_minutes, open_time FROM sessions WHERE is_open = 1";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Session s = new Session();

                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setSubject(rs.getString("subject"));
                s.setSessionType(rs.getString("session_type"));
                s.setLockDurationMinutes(rs.getInt("lock_duration_minutes"));
                Timestamp openTime = rs.getTimestamp("open_time");
                if (openTime != null) {
                    s.setOpenTime(openTime.toLocalDateTime());
                }

                list.add(s);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sessions", e);
        }

        return list;
    }

    public int countActiveUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 1 AND role = 'attendee'";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to count active users", e);
        }
    }

    public double getTodayAttendancePercentage() {
        String sql = "SELECT COUNT(DISTINCT CASE WHEN status IN ('Present', 'Late') THEN user_id END) * 100.0 / NULLIF(COUNT(DISTINCT user_id), 0) " +
                "FROM attendance WHERE DATE(marked_at) = CURDATE()";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            double percentage = 0.0;
            if (rs.next()) {
                percentage = rs.getDouble(1);
                if (rs.wasNull()) {
                    percentage = 0.0;
                }
            }
            return Math.min(percentage, 100.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute today's attendance percentage", e);
        }
    }
}
