package io.github.nikhilvirdi.retroamp98.playlist;

import java.nio.file.Path;
import java.util.Objects;
import io.github.nikhilvirdi.retroamp98.library.TrackMetadata;

public final class Track {
    private final Path path;
    private final String title;
    private final String artist;
    private final String album;
    private final TrackMetadata metadata;

    public Track(Path path) {
        this(path, null, null, null);
    }

    public Track(Path path, String title) {
        this(path, title, null, null);
    }

    public Track(Path path, String title, String artist, String album) {
        this(path, title, artist, album, TrackMetadata.EMPTY);
    }

    public Track(Path path, TrackMetadata metadata) {
        this(path, metadata.title(), metadata.artist(), metadata.album(), metadata);
    }

    private Track(Path path, String title, String artist, String album, TrackMetadata metadata) {
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.title = (title != null && !title.isBlank()) ? title.trim() : extractDefaultTitle(path);
        this.artist = (artist != null && !artist.isBlank()) ? artist.trim() : null;
        this.album = (album != null && !album.isBlank()) ? album.trim() : null;
        this.metadata = metadata;
    }

    public TrackMetadata getMetadata() { return metadata; }

    public Track withFallbackTitle(String fallback) {
        if (!metadata.title().isBlank()) return this;
        return new Track(path, fallback, artist, album, metadata);
    }

    public Path getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    private static String extractDefaultTitle(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "Unknown";
        }
        String name = fileName.toString();
        int dotIdx = name.lastIndexOf('.');
        return (dotIdx > 0) ? name.substring(0, dotIdx) : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track track)) return false;
        return path.equals(track.path) && title.equals(track.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, title);
    }

    @Override
    public String toString() {
        if (artist != null && !artist.isEmpty()) {
            return artist + " - " + title;
        }
        return title;
    }
}
