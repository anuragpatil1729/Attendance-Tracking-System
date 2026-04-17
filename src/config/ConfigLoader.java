package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class ConfigLoader {
    private static final Properties PROPS = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("config.properties")) {
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config.properties", e);
        }
    }

    private ConfigLoader() {
    }

    public static String get(String key) {
        return PROPS.getProperty(key, "").trim();
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
