package util;

import db.CloudDBConnection;
import java.net.InetAddress;

public final class NetworkUtil {
    private NetworkUtil() {
    }

    public static String detectIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException("Unable to detect IP address", e);
        }
    }

    public static boolean isOnline() {
        return CloudDBConnection.isReachable();
    }
}
