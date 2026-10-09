package io.github.nikhilvirdi.retroamp98.playlist;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable representation of an audio/video media track in RetroAmp98.
 * Title defaults to file name without extension.
 * Metadata fields (artist, album) are reserved for future ID3/tag parsing.
 */
public final class Track {
    private final Path path;
    private final String title;
    private final String artist;
    private final String album;

    /**
     * Constructs a track with default title derived from the file name.
     */
    public Track(Path path) {
        this(path, null, null, null);
    }

    /**
     * Constructs a track with a specific title.
     */
    public Track(Path path, String title) {
        this(path, title, null, null);
    }

    /**
     * Full constructor with metadata fields for future tag support.
     */
    public Track(Path path, String title, String artist, String album) {
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.title = (title != null && !title.isBlank()) ? title.trim() : extractDefaultTitle(path);
        this.artist = (artist != null && !artist.isBlank()) ? artist.trim() : null;
        this.album = (album != null && !album.isBlank()) ? album.trim() : null;
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

    /**
     * Derives title from filename stripping the extension.
     */
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
