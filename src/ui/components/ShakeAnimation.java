package ui.components;

import javax.swing.*;
import java.awt.*;

public final class ShakeAnimation {
    private ShakeAnimation() {}

    public static void shake(JComponent c) {
        Point p = c.getLocation();
        Timer timer = new Timer(20, null);
        timer.addActionListener(e -> {
            int tick = ((Timer)e.getSource()).getDelay();
            int dx = (int) (Math.sin(System.currentTimeMillis() / 25.0) * 8);
            c.setLocation(p.x + dx, p.y);
        });
        timer.start();
        new Timer(300, e -> { timer.stop(); c.setLocation(p); ((Timer)e.getSource()).stop(); }).start();
    }
}
