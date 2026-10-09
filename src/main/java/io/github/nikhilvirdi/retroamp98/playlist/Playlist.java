package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ordered collection of media tracks.
 * Provides mutation operations (add, remove, move, clear) and index access.
 */
public class Playlist {
    private final List<Track> tracks = new ArrayList<>();

    public Playlist() {
    }

    public Playlist(Collection<Track> initialTracks) {
        if (initialTracks != null) {
            this.tracks.addAll(initialTracks);
        }
    }

    /**
     * Appends a track to the end of the playlist.
     */
    public void add(Track track) {
        tracks.add(Objects.requireNonNull(track, "track cannot be null"));
    }

    /**
     * Inserts a track at the given index.
     */
    public void add(int index, Track track) {
        tracks.add(index, Objects.requireNonNull(track, "track cannot be null"));
    }

    /**
     * Appends all given tracks to the playlist.
     */
    public void addAll(Collection<Track> newTracks) {
        if (newTracks != null) {
            for (Track t : newTracks) {
                if (t != null) {
                    tracks.add(t);
                }
            }
        }
    }

    /**
     * Removes and returns the track at the specified index.
     */
    public Track remove(int index) {
        return tracks.remove(index);
    }

    /**
     * Moves a track from index 'from' to index 'to'.
     */
    public void move(int from, int to) {
        if (from < 0 || from >= tracks.size() || to < 0 || to >= tracks.size() || from == to) {
            return;
        }
        Track track = tracks.remove(from);
        tracks.add(to, track);
    }

    /**
     * Clears all tracks from the playlist.
     */
    public void clear() {
        tracks.clear();
    }

    /**
     * Retrieves the track at the specified index.
     */
    public Track get(int index) {
        return tracks.get(index);
    }

    /**
     * Returns the number of tracks in the playlist.
     */
    public int size() {
        return tracks.size();
    }

    /**
     * Checks if the playlist contains zero tracks.
     */
    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    /**
     * Returns the index of the first occurrence of the specified track, or -1 if not found.
     */
    public int indexOf(Track track) {
        return tracks.indexOf(track);
    }

    /**
     * Returns an unmodifiable view of the tracks.
     */
    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }
}
