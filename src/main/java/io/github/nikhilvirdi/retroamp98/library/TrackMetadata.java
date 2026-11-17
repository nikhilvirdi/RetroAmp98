package io.github.nikhilvirdi.retroamp98.library;

import java.time.Duration;

/** Lightweight tags only; artwork is read on demand. */
public record TrackMetadata(String title, String artist, String album, String albumArtist,
                            String year, String genre, String trackNumber, String description,
                            Duration duration) {
    public static final TrackMetadata EMPTY = new TrackMetadata("", "", "", "", "", "", "", "", Duration.ZERO);
}
