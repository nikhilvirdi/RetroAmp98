package io.github.nikhilvirdi.retroamp98.engine;

/**
 * Immutable metadata record for currently loaded media track (title and artist only).
 * Decoupled completely from JavaFX types.
 */
public record TrackInfo(String title, String artist) {

    /**
     * Returns "Artist - Title" if both exist, title if only title exists, or null.
     */
    public String getDisplayTitle() {
        if (artist != null && !artist.isBlank() && title != null && !title.isBlank()) {
            return artist.trim() + " - " + title.trim();
        } else if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return null;
    }
}
