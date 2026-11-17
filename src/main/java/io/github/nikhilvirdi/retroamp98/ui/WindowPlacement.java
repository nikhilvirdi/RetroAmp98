package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.util.SettingsManager;
import javafx.geometry.Rectangle2D;
import java.util.List;

public final class WindowPlacement {
    private WindowPlacement() {}

    public static Rectangle2D restore(SettingsManager settings, List<Rectangle2D> screens, double defaultWidth, double defaultHeight) {
        double width = settings.getWindowWidth() >= 420 ? settings.getWindowWidth() : defaultWidth;
        double height = settings.getWindowHeight() >= 570 ? settings.getWindowHeight() : defaultHeight;
        // Legacy defaults were landscape; retain the approved first-launch portrait composition.
        if (settings.getWindowHeight() < 570) width = defaultWidth;
        double x = settings.getWindowX(), y = settings.getWindowY();
        Rectangle2D screen = screens.getFirst();
        double bestArea = 0;
        if (Double.isFinite(x) && Double.isFinite(y)) {
            for (Rectangle2D candidate : screens) {
                double area = Math.max(0, Math.min(x + width, candidate.getMaxX()) - Math.max(x, candidate.getMinX()))
                        * Math.max(0, Math.min(y + height, candidate.getMaxY()) - Math.max(y, candidate.getMinY()));
                if (area > bestArea) { bestArea = area; screen = candidate; }
            }
        }
        width = Math.min(Math.max(420, width), screen.getWidth());
        height = Math.min(Math.max(570, height), screen.getHeight());
        if (bestArea == 0) {
            x = screen.getMinX() + (screen.getWidth() - width) / 2;
            y = screen.getMinY() + (screen.getHeight() - height) / 2;
        }
        return new Rectangle2D(Math.clamp(x, screen.getMinX(), screen.getMaxX() - width),
                Math.clamp(y, screen.getMinY(), screen.getMaxY() - height), width, height);
    }
}
