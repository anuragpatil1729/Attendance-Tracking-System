package db;

import java.sql.Connection;
import java.sql.SQLException;

public final class CloudDBConnection {

    private CloudDBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return ConnectionPool.getConnection();
    }

    public static boolean isReachable() {
        try (Connection ignored = ConnectionPool.getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
