package io.github.nikhilvirdi.retroamp98.engine;

public record TrackInfo(String title, String artist) {

    public String getDisplayTitle() {
        if (artist != null && !artist.isBlank() && title != null && !title.isBlank()) {
            return artist.trim() + " - " + title.trim();
        } else if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return null;
    }
}
