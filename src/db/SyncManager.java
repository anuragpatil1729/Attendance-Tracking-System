package db;

import ui.components.ToastNotification;

import javax.swing.*;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncManager {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        scheduler.scheduleAtFixedRate(this::syncPending, 5, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public int pendingCount() {
        try (Connection local = LocalDBConnection.getConnection();
             PreparedStatement ps = local.prepareStatement("SELECT COUNT(*) FROM attendance_local WHERE sync_status='pending'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void syncPending() {
        if (!CloudDBConnection.isReachable()) {
            return;
        }

        try (Connection local = LocalDBConnection.getConnection();
             Connection cloud = ConnectionPool.getConnection();
             PreparedStatement fetch = local.prepareStatement("SELECT * FROM attendance_local WHERE sync_status='pending'");
             ResultSet rs = fetch.executeQuery()) {

            while (rs.next()) {
                String localId = rs.getString("local_id");
                if (existsInCloud(cloud, localId)) {
                    markSynced(local, localId);
                    continue;
                }

                try (PreparedStatement insert = cloud.prepareStatement("""
                        INSERT INTO attendance (user_id, session_id, status, remarks, marked_at, ip_address, device_fingerprint, sync_status, local_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'synced', ?)
                        """)) {
                    insert.setInt(1, rs.getInt("user_id"));
                    insert.setInt(2, rs.getInt("session_id"));
                    insert.setString(3, rs.getString("status"));
                    insert.setString(4, rs.getString("remarks"));
                    insert.setString(5, rs.getString("marked_at"));
                    insert.setString(6, rs.getString("ip_address"));
                    insert.setString(7, rs.getString("device_fingerprint"));
                    insert.setString(8, localId);
                    insert.executeUpdate();
                }
                markSynced(local, localId);
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> ToastNotification.showError(null, "Sync failed: " + e.getMessage()));
        }
    }

    private boolean existsInCloud(Connection cloud, String localId) throws SQLException {
        try (PreparedStatement ps = cloud.prepareStatement("SELECT id FROM attendance WHERE local_id = ?")) {
            ps.setString(1, localId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markSynced(Connection local, String localId) throws SQLException {
        try (PreparedStatement ps = local.prepareStatement("UPDATE attendance_local SET sync_status='synced' WHERE local_id=?")) {
            ps.setString(1, localId);
            ps.executeUpdate();
        }
    }
}
