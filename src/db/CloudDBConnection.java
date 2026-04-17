package db;

import config.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class CloudDBConnection {

    private CloudDBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String host = ConfigLoader.get("db.host");
        int port = ConfigLoader.getInt("db.port", 3306);
        String dbName = ConfigLoader.get("db.name");
        String user = ConfigLoader.get("db.user");
        String pass = ConfigLoader.get("db.password");
        String caPath = ConfigLoader.get("db.ssl.ca");

        boolean hasCaPath = caPath != null && !caPath.isEmpty();

        // ✅ Aiven-compatible JDBC URL
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?sslMode=" + (hasCaPath ? "VERIFY_CA" : "REQUIRED")
                + "&enabledTLSProtocols=TLSv1.2"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&serverTimezone=UTC"
                + "&connectTimeout=10000"
                + "&socketTimeout=30000";

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);

        // ✅ Optional CA certificate (recommended for Aiven)
        if (hasCaPath) {
            props.setProperty("sslMode", "VERIFY_CA");
            props.setProperty("sslCa", caPath);
        }

        try {
            Connection conn = DriverManager.getConnection(url, props);
            System.out.println("✅ DB CONNECTED SUCCESSFULLY");
            return conn;

        } catch (SQLException e) {
            System.out.println("❌ DB CONNECTION FAILED");
            System.out.println("Reason: " + e.getMessage());
            e.printStackTrace();

            throw new SQLException(
                    "Could not connect to Aiven MySQL. Check credentials, SSL config, or IP access.",
                    e);
        }
    }

    public static boolean isReachable() {
        try (Connection conn = getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}