package service;

import db.CloudDBConnection;
import db.ConnectionPool;
import db.LocalDBConnection;
import model.AttendanceRecord;
import model.Session;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendanceService {
    private final AntiProxyService antiProxyService = new AntiProxyService();

    public void markAttendance(int userId, Session session, String ip, String fingerprint, String status, String remarks) {
        String blockMessage = antiProxyService.check(userId, session, ip, fingerprint);
        if (blockMessage != null && "practical".equalsIgnoreCase(session.getSessionType())) {
            logDevice(userId, ip, fingerprint, "blocked");
            throw new RuntimeException(blockMessage);
        }

        if (alreadyMarked(userId, session.getId())) {
            throw new RuntimeException("Attendance already marked for this session.");
        }

        String localId = UUID.randomUUID().toString();
        if (CloudDBConnection.isReachable()) {
            saveCloud(userId, session.getId(), ip, fingerprint, status, remarks, localId, "synced");
        } else {
            saveLocal(userId, session.getId(), ip, fingerprint, status, remarks, localId);
        }
        logDevice(userId, ip, fingerprint, "success");
    }

    public List<AttendanceRecord> attendeeHistory(int userId, int limit) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE user_id=? ORDER BY marked_at DESC LIMIT ?";
        try (Connection conn = ConnectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setId(rs.getInt("id"));
                    r.setSessionId(rs.getInt("session_id"));
                    r.setUserId(userId);
                    r.setStatus(rs.getString("status"));
                    r.setRemarks(rs.getString("remarks"));
                    Timestamp t = rs.getTimestamp("marked_at");
                    if (t != null) r.setMarkedAt(t.toLocalDateTime());
                    r.setSyncStatus(rs.getString("sync_status"));
                    list.add(r);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load history", e);
        }
        return list;
    }

    public List<SessionAttendanceView> getOpenSessionsForAttendee(int userId) {
        List<SessionAttendanceView> list = new ArrayList<>();
        String sql = """
            SELECT
                s.id,
                s.name,
                s.subject,
                s.session_type,
                s.lock_duration_minutes,
                s.open_time,
                a.status,
                a.marked_at
            FROM sessions s
            LEFT JOIN attendance a
                ON s.id = a.session_id
                AND a.user_id = ?
            WHERE s.is_open = 1
            ORDER BY s.open_time DESC, s.id DESC
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Session session = new Session();
                    session.setId(rs.getInt("id"));
                    session.setName(rs.getString("name"));
                    session.setSubject(rs.getString("subject"));
                    session.setSessionType(rs.getString("session_type"));
                    session.setLockDurationMinutes(rs.getInt("lock_duration_minutes"));
                    Timestamp openTime = rs.getTimestamp("open_time");
                    if (openTime != null) {
                        session.setOpenTime(openTime.toLocalDateTime());
                    }

                    SessionAttendanceView row = new SessionAttendanceView();
                    row.setSession(session);
                    row.setStatus(rs.getString("status"));
                    Timestamp markedAt = rs.getTimestamp("marked_at");
                    if (markedAt != null) {
                        row.setMarkedAt(markedAt.toLocalDateTime());
                    }

                    list.add(row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load attendee sessions", e);
        }

        System.out.println("Sessions fetched: " + list.size());
        return list;
    }

    public AttendanceStats getTodayAttendanceStats(int userId) {
        String sql = """
            SELECT
                COUNT(DISTINCT s.id) AS total,
                COUNT(DISTINCT CASE WHEN a.status = 'Present' THEN s.id END) AS attended
            FROM sessions s
            LEFT JOIN attendance a
                ON s.id = a.session_id AND a.user_id = ?
            WHERE DATE(s.open_time) = CURDATE()
            """;

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                AttendanceStats stats = new AttendanceStats();
                if (rs.next()) {
                    stats.total = rs.getInt("total");
                    stats.attended = rs.getInt("attended");
                }
                return stats;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load today attendance stats", e);
        }
    }

    public List<AttendanceRecord> getAll() {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = """
            SELECT a.user_id, u.full_name, a.session_id, s.session_type, a.status, a.marked_at
            FROM attendance a
            JOIN users u ON a.user_id = u.id
            JOIN sessions s ON a.session_id = s.id
            ORDER BY a.marked_at DESC
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AttendanceRecord r = new AttendanceRecord();
                r.setUserId(rs.getInt("user_id"));
                r.setSessionId(rs.getInt("session_id"));
                r.setStatus(rs.getString("status"));
                Timestamp t = rs.getTimestamp("marked_at");
                if (t != null) r.setMarkedAt(t.toLocalDateTime());
                r.setSyncStatus("synced");
                list.add(r);
            }
        } catch (Exception e) {
            throw new RuntimeException("getAll failed", e);
        }
        return list;
    }

    public List<Object[]> getAllRecords() {
        List<Object[]> rows = new ArrayList<>();
        String sql = """
            SELECT
                u.full_name,
                s.name AS session_name,
                s.session_type,
                a.status,
                a.marked_at,
                a.user_id
            FROM attendance a
            JOIN users u ON a.user_id = u.id
            JOIN sessions s ON a.session_id = s.id
            ORDER BY a.marked_at DESC
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("session_name"),
                        rs.getString("session_type"),
                        rs.getString("status"),
                        rs.getTimestamp("marked_at")
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load attendance records", e);
        }
        return rows;
    }

    public void upsertLectureAttendance(int officerId, int userId, int sessionId, String status, String remarks, String action, String reason) {
        try (Connection conn = ConnectionPool.getConnection(); CallableStatement cs = conn.prepareCall("{CALL sp_lecture_upsert(?,?,?,?,?,?)}")) {
            cs.setInt(1, officerId);
            cs.setInt(2, userId);
            cs.setInt(3, sessionId);
            cs.setString(4, status);
            cs.setString(5, remarks);
            cs.setString(6, reason == null ? "manual update" : reason);
            cs.execute();
        } catch (Exception e) {
            throw new RuntimeException("Lecture upsert failed", e);
        }
    }

    public void deleteLectureAttendance(int officerId, int userId, int sessionId, String reason) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement del = conn.prepareStatement("DELETE FROM attendance WHERE user_id=? AND session_id=?")) {
            del.setInt(1, userId);
            del.setInt(2, sessionId);
            del.executeUpdate();
            insertAudit(conn, officerId, "delete", "attendance", sessionId, "record", "deleted", reason);
        } catch (Exception e) {
            throw new RuntimeException("Lecture delete failed", e);
        }
    }

    private boolean alreadyMarked(int userId, int sessionId) {
        String sql = "SELECT 1 FROM attendance WHERE user_id=? AND session_id=? LIMIT 1";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate existing attendance", e);
        }
    }

    private void saveCloud(int userId, int sessionId, String ip, String fingerprint, String status, String remarks, String localId, String syncStatus) {
        String sql = "INSERT INTO attendance (local_id, user_id, session_id, status, marked_at, remarks, ip_address, device_fingerprint, sync_status) VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, localId);
            ps.setInt(2, userId);
            ps.setInt(3, sessionId);
            ps.setString(4, status);
            ps.setString(5, remarks);
            ps.setString(6, ip);
            ps.setString(7, fingerprint);
            ps.setString(8, syncStatus);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new RuntimeException("Attendance already marked for this session.", e);
            }
            throw new RuntimeException("Cloud save failed", e);
        } catch (Exception e) {
            throw new RuntimeException("Cloud save failed", e);
        }
    }

    private void saveLocal(int userId, int sessionId, String ip, String fingerprint, String status, String remarks, String localId) {
        String sql = "INSERT INTO attendance_local (local_id, user_id, session_id, status, marked_at, remarks, ip_address, device_fingerprint, sync_status) VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, 'pending')";
        try (Connection conn = LocalDBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, localId);
            ps.setInt(2, userId);
            ps.setInt(3, sessionId);
            ps.setString(4, status);
            ps.setString(5, remarks);
            ps.setString(6, ip);
            ps.setString(7, fingerprint);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Local save failed", e);
        }
    }

    private void logDevice(int userId, String ip, String fingerprint, String status) {
        String sql = "INSERT INTO device_log(user_id, ip_address, device_fingerprint, login_time, attempt_status) VALUES(?,?,?,CURRENT_TIMESTAMP,?)";
        try (Connection conn = ConnectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ip);
            ps.setString(3, fingerprint);
            ps.setString(4, status);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void insertAudit(Connection conn, int officerId, String action, String table, int targetId, String oldValue, String newValue, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO audit_log(officer_id, action, target_table, target_id, old_value, new_value, reason) VALUES(?,?,?,?,?,?,?)")) {
            ps.setInt(1, officerId);
            ps.setString(2, action);
            ps.setString(3, table);
            ps.setInt(4, targetId);
            ps.setString(5, oldValue);
            ps.setString(6, newValue);
            ps.setString(7, reason);
            ps.executeUpdate();
        }
    }

    public static class SessionAttendanceView {
        private Session session;
        private String status;
        private LocalDateTime markedAt;

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
            this.session = session;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getMarkedAt() {
            return markedAt;
        }

        public void setMarkedAt(LocalDateTime markedAt) {
            this.markedAt = markedAt;
        }
    }

    public static class AttendanceStats {
        private int total;
        private int attended;

        public int getTotal() {
            return total;
        }

        public int getAttended() {
            return attended;
        }

        public int getPercentage() {
            if (total <= 0) {
                return 0;
            }
            return Math.min(100, (int) Math.round((attended * 100.0) / total));
        }
    }
}
