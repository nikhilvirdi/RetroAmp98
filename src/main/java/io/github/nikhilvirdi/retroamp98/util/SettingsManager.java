package io.github.nikhilvirdi.retroamp98.util;

import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

public class SettingsManager implements AutoCloseable {

    private static final String DEFAULT_FILE_NAME = "settings.properties";
    private static final String CONFIG_DIR_NAME = ".retroamp98";

    private final Path settingsPath;

    private double volume = 0.8;
    private boolean muted = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean shuffle = false;
    private int windowWidth = 600;
    private int windowHeight = 420;
    private String lastPlaylistPath = "";
    private double windowX = Double.NaN;
    private double windowY = Double.NaN;
    private boolean hasSession;
    private List<Track> sessionTracks = List.of();
    private String currentTrackPath = "";
    private final ScheduledThreadPoolExecutor writer = new ScheduledThreadPoolExecutor(1, task -> new Thread(task, "retroamp-settings"));
    private ScheduledFuture<?> pending;
    private final Object fileLock = new Object();
    private volatile long saveRevision;

    public SettingsManager() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME, DEFAULT_FILE_NAME));
    }

    public SettingsManager(Path settingsPath) {
        this.settingsPath = settingsPath;
        writer.setRemoveOnCancelPolicy(true);
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
            if (Files.size(settingsPath) > 16 * 1024 * 1024) return;
            props.load(in);
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            return;
        }

        try {
            setVolume(Double.parseDouble(props.getProperty("volume", Double.toString(volume))));
        } catch (NumberFormatException ignored) {}

        muted = Boolean.parseBoolean(props.getProperty("muted", Boolean.toString(muted)));

        try {
            String rmStr = props.getProperty("repeatMode", repeatMode.name());
            repeatMode = RepeatMode.valueOf(rmStr.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ignored) {}

        shuffle = Boolean.parseBoolean(props.getProperty("shuffle", Boolean.toString(shuffle)));

        try {
            windowWidth = Integer.parseInt(props.getProperty("windowWidth", Integer.toString(windowWidth)));
            windowWidth = Math.max(1, windowWidth);
        } catch (NumberFormatException ignored) {}

        try {
            windowHeight = Integer.parseInt(props.getProperty("windowHeight", Integer.toString(windowHeight)));
            windowHeight = Math.max(1, windowHeight);
        } catch (NumberFormatException ignored) {}

        lastPlaylistPath = props.getProperty("lastPlaylistPath", "");
        windowX = coordinate(props.getProperty("windowX"));
        windowY = coordinate(props.getProperty("windowY"));
        hasSession = props.containsKey("session.count");
        List<Track> restored = new ArrayList<>();
        try {
            int count = Math.clamp(Integer.parseInt(props.getProperty("session.count", "0")), 0, 100000);
            for (int i = 0; i < count; i++) {
                String path = props.getProperty("session." + i + ".path", "");
                if (path.isBlank()) continue;
                try { restored.add(new Track(Path.of(path), props.getProperty("session." + i + ".title"))); }
                catch (java.nio.file.InvalidPathException ignored) { }
            }
        } catch (NumberFormatException ignored) { }
        sessionTracks = List.copyOf(restored);
        currentTrackPath = props.getProperty("session.currentPath", "");
    }

    public synchronized void save() {
        long revision = ++saveRevision;
        if (pending != null) pending.cancel(false);
        try { write(properties(), revision); }
        catch (IOException e) { System.getLogger(SettingsManager.class.getName()).log(System.Logger.Level.WARNING, "Could not save settings", e); }
    }

    public synchronized void saveAsync(Consumer<IOException> error) {
        Properties snapshot = properties();
        long revision = ++saveRevision;
        if (pending != null) pending.cancel(false);
        pending = writer.schedule(() -> {
            try { write(snapshot, revision); } catch (IOException e) { error.accept(e); }
        }, 400, TimeUnit.MILLISECONDS);
    }

    private Properties properties() {
        Properties props = new Properties();
        props.setProperty("volume", Double.toString(volume));
        props.setProperty("muted", Boolean.toString(muted));
        props.setProperty("repeatMode", repeatMode.name());
        props.setProperty("shuffle", Boolean.toString(shuffle));
        props.setProperty("windowWidth", Integer.toString(windowWidth));
        props.setProperty("windowHeight", Integer.toString(windowHeight));
        props.setProperty("lastPlaylistPath", (lastPlaylistPath != null) ? lastPlaylistPath : "");
        if (Double.isFinite(windowX)) props.setProperty("windowX", Double.toString(windowX));
        if (Double.isFinite(windowY)) props.setProperty("windowY", Double.toString(windowY));
        if (hasSession) {
            props.setProperty("session.count", Integer.toString(sessionTracks.size()));
            props.setProperty("session.currentPath", currentTrackPath);
            for (int i = 0; i < sessionTracks.size(); i++) {
                Track track = sessionTracks.get(i);
                props.setProperty("session." + i + ".path", track.getPath().toAbsolutePath().normalize().toString());
                props.setProperty("session." + i + ".title", track.getTitle());
            }
        }
        return props;
    }

    private void write(Properties props, long revision) throws IOException {
        synchronized (fileLock) {
        if (revision != saveRevision) return;
        Path destination = settingsPath.toAbsolutePath();
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) {
                props.store(out, "RetroAmp98 Settings");
            }
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        }
    }

    private static double coordinate(String text) {
        try { double value = Double.parseDouble(text); return Double.isFinite(value) ? value : Double.NaN; }
        catch (NullPointerException | NumberFormatException e) { return Double.NaN; }
    }

    public double getWindowX() { return windowX; }
    public double getWindowY() { return windowY; }
    public void setWindowX(double value) { windowX = value; }
    public void setWindowY(double value) { windowY = value; }
    public boolean hasSession() { return hasSession; }
    public List<Track> getSessionTracks() { return sessionTracks; }
    public String getCurrentTrackPath() { return currentTrackPath; }
    public void setSession(List<Track> tracks, Track current) {
        sessionTracks = List.copyOf(tracks);
        currentTrackPath = current == null ? "" : current.getPath().toAbsolutePath().normalize().toString();
        hasSession = true;
    }

    @Override public void close() { writer.shutdown(); }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        if (Double.isFinite(volume)) this.volume = Math.clamp(volume, 0.0, 1.0);
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
