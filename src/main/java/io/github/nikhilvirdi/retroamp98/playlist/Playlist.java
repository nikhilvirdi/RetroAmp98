package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Playlist {
    private final List<Track> tracks = new ArrayList<>();

    public Playlist() {
    }

    public Playlist(Collection<Track> initialTracks) {
        addAll(initialTracks);
    }

    public void add(Track track) {
        tracks.add(Objects.requireNonNull(track, "track cannot be null"));
    }

    public void add(int index, Track track) {
        tracks.add(index, Objects.requireNonNull(track, "track cannot be null"));
    }

    public void addAll(Collection<Track> newTracks) {
        if (newTracks != null) {
            for (Track t : newTracks) {
                if (t != null) {
                    tracks.add(t);
                }
            }
        }
    }

    public Track remove(int index) {
        return tracks.remove(index);
    }

    public void move(int from, int to) {
        if (from < 0 || from >= tracks.size() || to < 0 || to >= tracks.size() || from == to) {
            return;
        }
        Track track = tracks.remove(from);
        tracks.add(to, track);
    }

    public void clear() {
        tracks.clear();
    }

    public Track get(int index) {
        return tracks.get(index);
    }

    public int size() {
        return tracks.size();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public int indexOf(Track track) {
        return tracks.indexOf(track);
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }
}
