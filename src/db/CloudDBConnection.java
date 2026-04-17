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
        boolean hasCaPath = !caPath.isEmpty();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=true"
                + "&requireSSL=true"
                + "&verifyServerCertificate=" + (hasCaPath ? "true" : "false")
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&serverTimezone=UTC"
                + "&connectTimeout=10000"
                + "&socketTimeout=30000";

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);

        if (hasCaPath) {
            props.setProperty("trustCertificateKeyStoreUrl", "file:" + caPath);
            props.setProperty("trustCertificateKeyStorePassword", "");
        }

        try {
            return DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            throw new SQLException(
                    "Could not connect to Aiven MySQL. Check config.properties and ensure your IP is not blocked.",
                    e
            );
        }
    }

    public static boolean isReachable() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
