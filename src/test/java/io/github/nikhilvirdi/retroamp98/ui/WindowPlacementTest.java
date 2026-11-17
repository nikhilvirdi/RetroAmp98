package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.util.SettingsManager;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WindowPlacementTest {
    @Test void clampsToAvailableDisplaysIncludingNegativeCoordinates() {
        var primary = new Rectangle2D(0, 0, 1920, 1040);
        var left = new Rectangle2D(-1280, 0, 1280, 1024);
        try (var settings = new SettingsManager(Path.of("unused"))) {
            settings.setWindowWidth(600); settings.setWindowHeight(800);
            settings.setWindowX(-1200); settings.setWindowY(900);
            var placed = WindowPlacement.restore(settings, List.of(primary, left), 630, 850);
            assertTrue(left.contains(placed)); assertEquals(-1200, placed.getMinX());
            settings.setWindowX(9000); settings.setWindowY(-9000);
            assertTrue(primary.contains(WindowPlacement.restore(settings, List.of(primary, left), 630, 850)));
            settings.setWindowWidth(Integer.MAX_VALUE); settings.setWindowHeight(Integer.MAX_VALUE);
            assertTrue(primary.contains(WindowPlacement.restore(settings, List.of(primary), 630, 850)));
        }
    }
}
