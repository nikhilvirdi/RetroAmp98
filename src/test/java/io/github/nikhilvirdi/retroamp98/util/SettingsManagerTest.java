package io.github.nikhilvirdi.retroamp98.util;

import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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
        manager.setRepeatMode(RepeatMode.ALL);
        manager.setShuffle(true);
        manager.setWindowWidth(700);
        manager.setWindowHeight(480);
        manager.setLastPlaylistPath("C:/music/sample.m3u");

        manager.save();

        // Reload into a fresh manager
        SettingsManager reloaded = new SettingsManager(settingsFile);
        reloaded.load();

        assertEquals(0.65, reloaded.getVolume(), 0.001);
        assertTrue(reloaded.isMuted());
        assertEquals(RepeatMode.ALL, reloaded.getRepeatMode());
        assertTrue(reloaded.isShuffle());
        assertEquals(700, reloaded.getWindowWidth());
        assertEquals(480, reloaded.getWindowHeight());
        assertEquals("C:/music/sample.m3u", reloaded.getLastPlaylistPath());
    }

    @Test
    void testMissingFileLoadsDefaultsGracefully(@TempDir Path tempDir) {
        Path missingFile = tempDir.resolve("non-existent.properties");
        SettingsManager manager = new SettingsManager(missingFile);
        manager.load();

        assertEquals(0.8, manager.getVolume(), 0.001);
        assertFalse(manager.isMuted());
        assertEquals(RepeatMode.OFF, manager.getRepeatMode());
        assertFalse(manager.isShuffle());
        assertEquals(600, manager.getWindowWidth());
        assertEquals(420, manager.getWindowHeight());
    }
}
