package io.github.nikhilvirdi.retroamp98.util;

import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsManagerTest {

    @Test
    void testSaveAndLoadSettings(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("test-settings.properties");
        SettingsManager manager = new SettingsManager(settingsFile);

        // Modify settings
        manager.setVolume(0.65);
        manager.setMuted(true);
        manager.setSpeed(1.25);
        manager.setRepeatMode(RepeatMode.ALL);
        manager.setShuffle(true);
        manager.setEqualizerEnabled(true);

        double[] testBands = new double[]{1.0, 2.5, -3.0, 4.0, 0.0, -1.5, 3.0, 2.0, -4.5, 5.0};
        manager.setEqualizerBands(testBands);

        manager.setVisualizerEnabled(false);
        manager.setResizeToVideo(false);
        manager.setWindowX(120);
        manager.setWindowY(180);
        manager.setWindowWidth(850);
        manager.setWindowHeight(620);
        manager.setLastPlaylistPath("C:/music/sample.m3u");

        manager.save();

        // Reload into a fresh manager
        SettingsManager reloaded = new SettingsManager(settingsFile);
        reloaded.load();

        assertEquals(0.65, reloaded.getVolume(), 0.001);
        assertTrue(reloaded.isMuted());
        assertEquals(1.25, reloaded.getSpeed(), 0.001);
        assertEquals(RepeatMode.ALL, reloaded.getRepeatMode());
        assertTrue(reloaded.isShuffle());
        assertTrue(reloaded.isEqualizerEnabled());
        assertArrayEquals(testBands, reloaded.getEqualizerBands(), 0.001);
        assertFalse(reloaded.isVisualizerEnabled());
        assertFalse(reloaded.isResizeToVideo());
        assertEquals(120, reloaded.getWindowX());
        assertEquals(180, reloaded.getWindowY());
        assertEquals(850, reloaded.getWindowWidth());
        assertEquals(620, reloaded.getWindowHeight());
        assertEquals("C:/music/sample.m3u", reloaded.getLastPlaylistPath());
    }

    @Test
    void testMissingFileLoadsDefaultsGracefully(@TempDir Path tempDir) {
        Path missingFile = tempDir.resolve("non-existent.properties");
        SettingsManager manager = new SettingsManager(missingFile);
        manager.load();

        assertEquals(0.8, manager.getVolume(), 0.001);
        assertFalse(manager.isMuted());
        assertEquals(1.0, manager.getSpeed(), 0.001);
        assertEquals(RepeatMode.OFF, manager.getRepeatMode());
        assertFalse(manager.isShuffle());
        assertFalse(manager.isEqualizerEnabled());
        assertTrue(manager.isVisualizerEnabled());
        assertTrue(manager.isResizeToVideo());
    }
}
