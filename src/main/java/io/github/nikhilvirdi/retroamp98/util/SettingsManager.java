package io.github.nikhilvirdi.retroamp98.util;

import io.github.nikhilvirdi.retroamp98.engine.MediaEngine;
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
 * Persists volume, mute, speed, repeat mode, shuffle, equalizer bands, visualizer,
 * window geometry, and the last loaded playlist path.
 */
public class SettingsManager {

    private static final String DEFAULT_FILE_NAME = "settings.properties";
    private static final String CONFIG_DIR_NAME = ".retroamp98";

    private final Path settingsPath;

    // Persisted settings with sensible defaults
    private double volume = 0.8;
    private boolean muted = false;
    private double speed = 1.0;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean shuffle = false;
    private boolean equalizerEnabled = false;
    private double[] equalizerBands = new double[10];
    private boolean visualizerEnabled = true;
    private boolean resizeToVideo = true;
    private int windowX = -1;
    private int windowY = -1;
    private int windowWidth = 740;
    private int windowHeight = 540;
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
            speed = Double.parseDouble(props.getProperty("speed", Double.toString(speed)));
            speed = Math.clamp(speed, 0.5, 2.0);
        } catch (NumberFormatException ignored) {}

        try {
            String rmStr = props.getProperty("repeatMode", repeatMode.name());
            repeatMode = RepeatMode.valueOf(rmStr.toUpperCase());
        } catch (Exception ignored) {}

        shuffle = Boolean.parseBoolean(props.getProperty("shuffle", Boolean.toString(shuffle)));
        equalizerEnabled = Boolean.parseBoolean(props.getProperty("equalizerEnabled", Boolean.toString(equalizerEnabled)));

        String eqBandsStr = props.getProperty("equalizerBands");
        if (eqBandsStr != null && !eqBandsStr.isBlank()) {
            String[] tokens = eqBandsStr.split(",");
            for (int i = 0; i < Math.min(tokens.length, 10); i++) {
                try {
                    equalizerBands[i] = Math.clamp(Double.parseDouble(tokens[i].trim()), -12.0, 12.0);
                } catch (NumberFormatException ignored) {}
            }
        }

        visualizerEnabled = Boolean.parseBoolean(props.getProperty("visualizerEnabled", Boolean.toString(visualizerEnabled)));
        resizeToVideo = Boolean.parseBoolean(props.getProperty("resizeToVideo", Boolean.toString(resizeToVideo)));

        try {
            windowX = Integer.parseInt(props.getProperty("windowX", "-1"));
            windowY = Integer.parseInt(props.getProperty("windowY", "-1"));
            windowWidth = Integer.parseInt(props.getProperty("windowWidth", "740"));
            windowHeight = Integer.parseInt(props.getProperty("windowHeight", "540"));
        } catch (NumberFormatException ignored) {}

        lastPlaylistPath = props.getProperty("lastPlaylistPath", "");
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("volume", Double.toString(volume));
        props.setProperty("muted", Boolean.toString(muted));
        props.setProperty("speed", Double.toString(speed));
        props.setProperty("repeatMode", repeatMode.name());
        props.setProperty("shuffle", Boolean.toString(shuffle));
        props.setProperty("equalizerEnabled", Boolean.toString(equalizerEnabled));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(java.util.Locale.ROOT, "%.1f", equalizerBands[i]));
        }
        props.setProperty("equalizerBands", sb.toString());

        props.setProperty("visualizerEnabled", Boolean.toString(visualizerEnabled));
        props.setProperty("resizeToVideo", Boolean.toString(resizeToVideo));
        props.setProperty("windowX", Integer.toString(windowX));
        props.setProperty("windowY", Integer.toString(windowY));
        props.setProperty("windowWidth", Integer.toString(windowWidth));
        props.setProperty("windowHeight", Integer.toString(windowHeight));
        props.setProperty("lastPlaylistPath", lastPlaylistPath != null ? lastPlaylistPath : "");

        try {
            if (settingsPath.getParent() != null) {
                Files.createDirectories(settingsPath.getParent());
            }
            try (OutputStream out = Files.newOutputStream(settingsPath)) {
                props.store(out, "RetroAmp98 User Settings");
            }
        } catch (IOException ignored) {
            // Silently ignore write failures on read-only environments
        }
    }

    // Getters and Setters

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public boolean isEqualizerEnabled() {
        return equalizerEnabled;
    }

    public void setEqualizerEnabled(boolean equalizerEnabled) {
        this.equalizerEnabled = equalizerEnabled;
    }

    public double[] getEqualizerBands() {
        return equalizerBands;
    }

    public void setEqualizerBands(double[] equalizerBands) {
        if (equalizerBands != null && equalizerBands.length == 10) {
            System.arraycopy(equalizerBands, 0, this.equalizerBands, 0, 10);
        }
    }

    public boolean isVisualizerEnabled() {
        return visualizerEnabled;
    }

    public void setVisualizerEnabled(boolean visualizerEnabled) {
        this.visualizerEnabled = visualizerEnabled;
    }

    public boolean isResizeToVideo() {
        return resizeToVideo;
    }

    public void setResizeToVideo(boolean resizeToVideo) {
        this.resizeToVideo = resizeToVideo;
    }

    public int getWindowX() {
        return windowX;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
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
        this.lastPlaylistPath = lastPlaylistPath;
    }
}
