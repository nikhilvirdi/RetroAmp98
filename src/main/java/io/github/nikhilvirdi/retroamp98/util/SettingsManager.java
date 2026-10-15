package io.github.nikhilvirdi.retroamp98.util;

import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages persisting and loading user preferences from ~/.retroamp98/settings.properties.
 * Persists volume, mute, repeat mode, shuffle, window size, and the last loaded playlist path.
 */
public class SettingsManager {

    private static final String DEFAULT_FILE_NAME = "settings.properties";
    private static final String CONFIG_DIR_NAME = ".retroamp98";

    private final Path settingsPath;

    // Persisted settings with sensible defaults
    private double volume = 0.8;
    private boolean muted = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean shuffle = false;
    private int windowWidth = 600;
    private int windowHeight = 420;
    private String lastPlaylistPath = "";

    public SettingsManager() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, DEFAULT_FILE_NAME));
    }

    public SettingsManager(Path settingsPath) {
        this.settingsPath = settingsPath;
    }

    public Path getSettingsPath() {
        return settingsPath;
    }

    public void load() {
        if (!Files.exists(settingsPath)) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(settingsPath)) {
            props.load(in);
        } catch (IOException e) {
            // Ignore corrupted or unreadable config, retain defaults
            return;
        }

        try {
            volume = Double.parseDouble(props.getProperty("volume", Double.toString(volume)));
            volume = Math.clamp(volume, 0.0, 1.0);
        } catch (NumberFormatException ignored) {}

        muted = Boolean.parseBoolean(props.getProperty("muted", Boolean.toString(muted)));

        try {
            String rmStr = props.getProperty("repeatMode", repeatMode.name());
            repeatMode = RepeatMode.valueOf(rmStr.toUpperCase());
        } catch (Exception ignored) {}

        shuffle = Boolean.parseBoolean(props.getProperty("shuffle", Boolean.toString(shuffle)));

        try {
            windowWidth = Integer.parseInt(props.getProperty("windowWidth", Integer.toString(windowWidth)));
            windowWidth = Math.max(480, windowWidth);
        } catch (NumberFormatException ignored) {}

        try {
            windowHeight = Integer.parseInt(props.getProperty("windowHeight", Integer.toString(windowHeight)));
            windowHeight = Math.max(300, windowHeight);
        } catch (NumberFormatException ignored) {}

        lastPlaylistPath = props.getProperty("lastPlaylistPath", "");
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("volume", Double.toString(volume));
        props.setProperty("muted", Boolean.toString(muted));
        props.setProperty("repeatMode", repeatMode.name());
        props.setProperty("shuffle", Boolean.toString(shuffle));
        props.setProperty("windowWidth", Integer.toString(windowWidth));
        props.setProperty("windowHeight", Integer.toString(windowHeight));
        props.setProperty("lastPlaylistPath", (lastPlaylistPath != null) ? lastPlaylistPath : "");

        try {
            Path parentDir = settingsPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            try (OutputStream out = Files.newOutputStream(settingsPath)) {
                props.store(out, "RetroAmp98 Settings");
            }
        } catch (IOException ignored) {
            // Fail silently on write errors
        }
    }

    // --- Getters & Setters ---

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.clamp(volume, 0.0, 1.0);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        if (repeatMode != null) {
            this.repeatMode = repeatMode;
        }
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public String getLastPlaylistPath() {
        return lastPlaylistPath;
    }

    public void setLastPlaylistPath(String lastPlaylistPath) {
        this.lastPlaylistPath = (lastPlaylistPath != null) ? lastPlaylistPath : "";
    }
}
