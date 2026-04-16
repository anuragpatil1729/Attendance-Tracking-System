package db;

import util.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe singleton connection manager.
 */
public final class DBConnection {
    private static volatile Connection connection;

    private DBConnection() {
    }

    /**
     * Returns a live shared SQL connection and reopens when closed.
     */
    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        Constants.DB_URL,
                        Constants.DB_USER,
                        Constants.DB_PASSWORD
                );
            }
            return connection;
        } catch (Exception ex) {
            System.err.println("DB Connection failed: " + ex.getMessage());
            throw new RuntimeException("Unable to establish database connection", ex);
        }
    }

    /**
     * Closes the shared SQL connection.
     */
    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            System.err.println("DB Connection close failed: " + ex.getMessage());
            throw new RuntimeException("Unable to close database connection", ex);
        }
    }
}
