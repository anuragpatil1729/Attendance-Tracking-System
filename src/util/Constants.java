package util;

import java.awt.*;

public final class Constants {
    public static final String APP_NAME = "Smart Attendance";
    public static final Color BG = Color.decode("#0d1117");
    public static final Color SIDEBAR = Color.decode("#161b22");
    public static final Color INPUT = Color.decode("#21262d");
    public static final Color ACCENT = Color.decode("#58a6ff");
    public static final Color TEXT = Color.decode("#e6edf3");
    public static final Color GREEN = Color.decode("#3fb950");
    public static final Color ORANGE = Color.decode("#d29922");
    public static final Color RED = Color.decode("#f85149");

    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private Constants() {
    }

    public static Color blend(Color c1, Color c2, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        float ir = 1f - r;
        return new Color(
                Math.min(255, Math.round(c1.getRed() * ir + c2.getRed() * r)),
                Math.min(255, Math.round(c1.getGreen() * ir + c2.getGreen() * r)),
                Math.min(255, Math.round(c1.getBlue() * ir + c2.getBlue() * r))
        );
    }

    public static Color brighten(Color color, float factor) {
        float f = Math.max(0f, factor);
        return new Color(
                Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * f)),
                Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * f)),
                Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * f))
        );
    }

    public static Color darken(Color color, float factor) {
        float f = Math.max(0f, Math.min(1f, factor));
        return new Color(
                Math.max(0, (int) (color.getRed() * (1f - f))),
                Math.max(0, (int) (color.getGreen() * (1f - f))),
                Math.max(0, (int) (color.getBlue() * (1f - f)))
        );
    }
}
