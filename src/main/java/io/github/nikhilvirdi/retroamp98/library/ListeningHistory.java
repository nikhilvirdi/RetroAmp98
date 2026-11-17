package io.github.nikhilvirdi.retroamp98.library;

import io.github.nikhilvirdi.retroamp98.playlist.Track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ListeningHistory implements AutoCloseable {
    public static final int LIMIT = 200;
    private final Path path;
    private final java.util.concurrent.ScheduledThreadPoolExecutor writer = new java.util.concurrent.ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "retroamp-history");
        return thread;
    });
    private java.util.concurrent.ScheduledFuture<?> pending;
    private final Object fileLock = new Object();
    private volatile long saveRevision;

    public ListeningHistory(Path path) {
        this.path = path;
        writer.setRemoveOnCancelPolicy(true);
        writer.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);
    }

    public synchronized void saveAsync(List<Track> tracks, java.util.function.Consumer<IOException> error) {
        if (path == null) return;
        List<Track> snapshot = List.copyOf(tracks);
        long revision = ++saveRevision;
        if (pending != null) pending.cancel(false);
        pending = writer.schedule(() -> {
            try { write(snapshot, revision); } catch (IOException e) { error.accept(e); }
        }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override public void close() { writer.shutdown(); }

    public List<Track> load() throws IOException {
        if (path == null || !Files.exists(path)) return List.of();
        // Properties escapes Unicode to six bytes per character, including 200 full-length comments.
        if (Files.size(path) > 16 * 1024 * 1024) throw new IOException("History file too large");
        Properties p = new Properties();
        try (var in = Files.newInputStream(path)) { p.load(in); }
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            String prefix = i + ".";
            String file = p.getProperty(prefix + "path");
            if (file == null) continue;
            try {
                Path trackPath = Path.of(file).toAbsolutePath().normalize();
                if (tracks.stream().anyMatch(t -> t.getPath().equals(trackPath))) continue;
                tracks.add(new Track(trackPath, new TrackMetadata(p.getProperty(prefix + "title", ""),
                        p.getProperty(prefix + "artist", ""), p.getProperty(prefix + "album", ""),
                        p.getProperty(prefix + "albumArtist", ""), p.getProperty(prefix + "year", ""),
                        p.getProperty(prefix + "genre", ""), p.getProperty(prefix + "track", ""),
                        p.getProperty(prefix + "description", ""), Duration.ofMillis(Math.max(0,
                        Long.parseLong(p.getProperty(prefix + "duration", "0")))))));
            } catch (IllegalArgumentException ignored) { /* Skip a damaged entry independently. */ }
        }
        return List.copyOf(tracks);
    }

    public synchronized void save(List<Track> tracks) throws IOException {
        if (path == null) return;
        long revision = ++saveRevision;
        if (pending != null) pending.cancel(false);
        write(tracks, revision);
    }

    private void write(List<Track> tracks, long revision) throws IOException {
        synchronized (fileLock) {
        if (revision != saveRevision) return;
        Properties p = new Properties();
        for (int i = 0; i < Math.min(LIMIT, tracks.size()); i++) {
            Track t = tracks.get(i);
            TrackMetadata m = t.getMetadata();
            String prefix = i + ".";
            p.setProperty(prefix + "path", t.getPath().toAbsolutePath().normalize().toString());
            p.setProperty(prefix + "title", t.getTitle());
            p.setProperty(prefix + "artist", t.getArtist() == null ? "" : t.getArtist());
            p.setProperty(prefix + "album", t.getAlbum() == null ? "" : t.getAlbum());
            p.setProperty(prefix + "albumArtist", m.albumArtist());
            p.setProperty(prefix + "year", m.year());
            p.setProperty(prefix + "genre", m.genre());
            p.setProperty(prefix + "track", m.trackNumber());
            p.setProperty(prefix + "description", m.description());
            p.setProperty(prefix + "duration", Long.toString(m.duration().toMillis()));
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            try (var out = Files.newOutputStream(temp)) { p.store(out, "RetroAmp98 listening history"); }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
        }
    }
}
