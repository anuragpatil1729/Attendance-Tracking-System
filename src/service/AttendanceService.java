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


    public List<AttendanceRecord> getAll() {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = """
            SELECT a.user_id, u.full_name, a.session_id, s.session_type, a.status, a.marked_at
            FROM attendance a
            JOIN users u ON u.id = a.user_id
            JOIN sessions s ON s.id = a.session_id
            ORDER BY a.marked_at DESC
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("[AttendanceService.getAll] DB URL: "
                + conn.getMetaData().getURL());
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
            System.out.println("[AttendanceService.getAll] Rows: " + list.size());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("getAll failed", e);
        }
        return list;
    }

    public List<Object[]> getAllRecords() {
        List<Object[]> rows = new ArrayList<>();
        String sql = """
            SELECT a.user_id, u.full_name, a.session_id, s.session_type, a.status, a.marked_at
            FROM attendance a
            JOIN users u ON u.id = a.user_id
            JOIN sessions s ON s.id = a.session_id
            ORDER BY a.marked_at DESC
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getInt("session_id"),
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

    private void saveCloud(int userId, int sessionId, String ip, String fingerprint, String status, String remarks, String localId, String syncStatus) {
        String sql = "INSERT INTO attendance(user_id, session_id, status, remarks, marked_at, ip_address, device_fingerprint, sync_status, local_id) VALUES(?,?,?,?,CURRENT_TIMESTAMP,?,?,?,?)";
        try (Connection conn = ConnectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sessionId);
            ps.setString(3, status);
            ps.setString(4, remarks);
            ps.setString(5, ip);
            ps.setString(6, fingerprint);
            ps.setString(7, syncStatus);
            ps.setString(8, localId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Cloud save failed", e);
        }
    }

    private void saveLocal(int userId, int sessionId, String ip, String fingerprint, String status, String remarks, String localId) {
        String sql = "INSERT INTO attendance_local(user_id, session_id, status, remarks, marked_at, ip_address, device_fingerprint, sync_status, local_id) VALUES(?,?,?,?,CURRENT_TIMESTAMP,?,?, 'pending',?)";
        try (Connection conn = LocalDBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sessionId);
            ps.setString(3, status);
            ps.setString(4, remarks);
            ps.setString(5, ip);
            ps.setString(6, fingerprint);
            ps.setString(7, localId);
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
}
