package util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

public final class DeviceFingerprint {
    private DeviceFingerprint() {
    }

    public static String generate() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            String os = System.getProperty("os.name", "unknown");
            StringBuilder macs = new StringBuilder();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                byte[] mac = interfaces.nextElement().getHardwareAddress();
                if (mac == null) {
                    continue;
                }
                for (byte b : mac) {
                    macs.append(String.format("%02X", b));
                }
            }
            String raw = host + "|" + os + "|" + macs;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build device fingerprint", e);
        }
    }
}
