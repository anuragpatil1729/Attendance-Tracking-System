package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class LocalDBConnection {
    private static final String URL = "jdbc:sqlite:attendance_local.db";

    private LocalDBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        initSchema(conn);
        return conn;
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attendance_local (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    session_id INTEGER NOT NULL,
                    status TEXT DEFAULT 'Present',
                    remarks TEXT,
                    marked_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    ip_address TEXT,
                    device_fingerprint TEXT,
                    sync_status TEXT NOT NULL DEFAULT 'pending',
                    local_id TEXT NOT NULL UNIQUE
                )
                """);
        }
    }
}
