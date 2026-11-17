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
    void synchronousSaveSupersedesAnOlderQueuedSnapshot(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.properties");
        var error = new java.util.concurrent.atomic.AtomicReference<Exception>();
        try (SettingsManager settings = new SettingsManager(file)) {
            settings.setVolume(0.2);
            settings.saveAsync(error::set);
            settings.setVolume(0.9);
            settings.save();
            Thread.sleep(600);
            settings.load();
            assertEquals(0.9, settings.getVolume());
            assertEquals(null, error.get());
        }
    }

    @Test
    void sessionOrderSelectionAndWindowPositionRoundTrip(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.properties");
        var first = new io.github.nikhilvirdi.retroamp98.playlist.Track(directory.resolve("first.mp3"), "First title");
        var second = new io.github.nikhilvirdi.retroamp98.playlist.Track(directory.resolve("second.wav"));
        try (SettingsManager settings = new SettingsManager(file)) {
            settings.setSession(java.util.List.of(second, first), first);
            settings.setWindowX(-900); settings.setWindowY(40);
            settings.setVolume(0.35); settings.save();
        }
        try (SettingsManager restored = new SettingsManager(file)) {
            restored.load();
            assertTrue(restored.hasSession());
            assertEquals(java.util.List.of(second, first), restored.getSessionTracks());
            assertEquals(first.getPath().toString(), restored.getCurrentTrackPath());
            assertEquals(-900, restored.getWindowX()); assertEquals(40, restored.getWindowY());
            assertEquals(0.35, restored.getVolume(), 0.001);
            restored.setSession(java.util.List.of(), null); restored.save();
            restored.load(); assertTrue(restored.hasSession()); assertTrue(restored.getSessionTracks().isEmpty());
        }
    }

    @Test
    void malformedPreferencesUseSafeDefaults(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.properties");
        java.nio.file.Files.writeString(file, "volume=NaN\nrepeatMode=bad\nwindowX=Infinity\nwindowY=oops\nwindowWidth=oops\nsession.count=2\nsession.0.path=bad\\u0000path\nsession.1.path=valid.wav\n");
        try (SettingsManager settings = new SettingsManager(file)) {
            settings.load();
            assertEquals(0.8, settings.getVolume()); assertEquals(RepeatMode.OFF, settings.getRepeatMode());
            assertTrue(Double.isNaN(settings.getWindowX())); assertEquals(600, settings.getWindowWidth());
            assertEquals(1, settings.getSessionTracks().size());
        }
        java.nio.file.Files.writeString(file, "volume=\\uZZZZ");
        try (SettingsManager settings = new SettingsManager(file)) { settings.load(); assertEquals(0.8, settings.getVolume()); }
    }

    @Test
    void debouncedSaveFlushesNewestSnapshotOnClose(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.properties");
        var error = new java.util.concurrent.atomic.AtomicReference<Exception>();
        try (SettingsManager settings = new SettingsManager(file)) {
            for (int i = 0; i < 10; i++) { settings.setVolume(i / 10.0); settings.saveAsync(error::set); }
        }
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!java.nio.file.Files.exists(file) && System.nanoTime() < deadline) Thread.sleep(10);
        try (SettingsManager restored = new SettingsManager(file)) { restored.load(); assertEquals(0.9, restored.getVolume(), 0.001); }
        assertEquals(null, error.get()); assertFalse(java.nio.file.Files.exists(directory.resolve("settings.properties.tmp")));
    }

    @Test
    void testSaveAndLoadSettings(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("test-settings.properties");
        SettingsManager manager = new SettingsManager(settingsFile);

        manager.setVolume(0.65);
        manager.setMuted(true);
        manager.setRepeatMode(RepeatMode.ALL);
        manager.setShuffle(true);
        manager.setWindowWidth(700);
        manager.setWindowHeight(480);
        manager.setLastPlaylistPath("C:/music/sample.m3u");

        manager.save();

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
