package util;

import java.awt.Color;
import java.awt.Font;

/**
 * Application-wide constants for configuration and UI styling.
 */
public final class Constants {
    public static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_db?useSSL=false&serverTimezone=UTC";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "anuragpatil";

    public static final String APP_TITLE = "Attendance Manager";
    public static final int WINDOW_W = 1100;
    public static final int WINDOW_H = 700;

    public static final Color BG_COLOR = Color.decode("#1e1e2e");
    public static final Color SIDEBAR_COLOR = Color.decode("#181825");
    public static final Color ACCENT_COLOR = Color.decode("#cba6f7");
    public static final Color TEXT_COLOR = Color.decode("#cdd6f4");
    public static final Color INPUT_BG = Color.decode("#313244");

    public static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private Constants() {
    }
}
