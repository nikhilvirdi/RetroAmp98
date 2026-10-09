package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the active playlist, current playback index, and play order traversal.
 * Completely decoupled from the audio playback engine (does not touch MediaEngine).
 */
public class PlaylistManager {

    private final List<PlaylistListener> listeners = new CopyOnWriteArrayList<>();

    private Playlist playlist;
    private int currentIndex = -1;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private PlayOrderStrategy playOrderStrategy;

    public PlaylistManager() {
        this(new Playlist());
    }

    public PlaylistManager(Playlist playlist) {
        this.playlist = (playlist != null) ? playlist : new Playlist();
        this.playOrderStrategy = new SequentialOrder();
    }

    // --- Navigation Queries and Commands ---

    /**
     * Returns the currently active track, or null if no track is selected / playlist is empty.
     */
    public Track current() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            return null;
        }
        return playlist.get(currentIndex);
    }

    /**
     * Advances to the next track according to the active strategy and repeat mode.
     * Returns the new track, or null if no next track exists.
     */
    public Track next() {
        if (playlist.isEmpty()) {
            return null;
        }
        int nextIdx = playOrderStrategy.nextIndex(currentIndex, playlist.size(), repeatMode);
        if (nextIdx < 0 || nextIdx >= playlist.size()) {
            return null;
        }
        this.currentIndex = nextIdx;
        Track track = current();
        notifyCurrentIndexChanged(currentIndex, track);
        return track;
    }

    /**
     * Moves to the previous track according to the active strategy and repeat mode.
     * Returns the previous track, or null if no previous track exists.
     */
    public Track previous() {
        if (playlist.isEmpty()) {
            return null;
        }
        int prevIdx = playOrderStrategy.previousIndex(currentIndex, playlist.size(), repeatMode);
        if (prevIdx < 0 || prevIdx >= playlist.size()) {
            return null;
        }
        this.currentIndex = prevIdx;
        Track track = current();
        notifyCurrentIndexChanged(currentIndex, track);
        return track;
    }

    /**
     * Jumps directly to a specific track index in the playlist.
     */
    public Track jumpTo(int index) {
        if (index >= 0 && index < playlist.size()) {
            this.currentIndex = index;
            playOrderStrategy.onIndexSelected(currentIndex, playlist.size());
            Track track = current();
            notifyCurrentIndexChanged(currentIndex, track);
            return track;
        }
        return null;
    }

    /**
     * Checks if a next track is available from the current position.
     */
    public boolean hasNext() {
        if (playlist.isEmpty()) {
            return false;
        }
        return playOrderStrategy.hasNext(currentIndex, playlist.size(), repeatMode);
    }

    /**
     * Checks if a previous track is available from the current position.
     */
    public boolean hasPrevious() {
        if (playlist.isEmpty()) {
            return false;
        }
        return playOrderStrategy.hasPrevious(currentIndex, playlist.size(), repeatMode);
    }

    // --- Playlist Access & Mutation Delegation ---

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist newPlaylist) {
        this.playlist = (newPlaylist != null) ? newPlaylist : new Playlist();
        this.currentIndex = -1;
        this.playOrderStrategy.onPlaylistReset(playlist.size(), -1);
        notifyPlaylistCleared();
        if (!playlist.isEmpty()) {
            notifyTracksAdded(0, playlist.getTracks());
        }
        notifyCurrentIndexChanged(-1, null);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void addTrack(Track track) {
        if (track == null) return;
        int idx = playlist.size();
        playlist.add(track);
        playOrderStrategy.onTrackAdded(idx, playlist.size());
        notifyTracksAdded(idx, List.of(track));
    }

    public void addTracks(Collection<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        int startIdx = playlist.size();
        playlist.addAll(tracks);
        for (int i = 0; i < tracks.size(); i++) {
            playOrderStrategy.onTrackAdded(startIdx + i, playlist.size());
        }
        notifyTracksAdded(startIdx, List.copyOf(tracks));
    }

    public Track removeTrack(int index) {
        if (index < 0 || index >= playlist.size()) {
            return null;
        }
        Track removed = playlist.remove(index);
        playOrderStrategy.onTrackRemoved(index, playlist.size());

        int oldIndex = currentIndex;
        if (playlist.isEmpty()) {
            currentIndex = -1;
        } else if (index < currentIndex) {
            currentIndex--;
        } else if (index == currentIndex) {
            if (currentIndex >= playlist.size()) {
                currentIndex = playlist.size() - 1;
            }
        }

        notifyTrackRemoved(index, removed);
        if (oldIndex != currentIndex || index == oldIndex) {
            notifyCurrentIndexChanged(currentIndex, current());
        }
        return removed;
    }

    public void moveTrack(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= playlist.size() || toIndex < 0 || toIndex >= playlist.size() || fromIndex == toIndex) {
            return;
        }
        playlist.move(fromIndex, toIndex);
        playOrderStrategy.onTrackMoved(fromIndex, toIndex, playlist.size());

        int oldIndex = currentIndex;
        if (currentIndex == fromIndex) {
            currentIndex = toIndex;
        } else if (fromIndex < currentIndex && toIndex >= currentIndex) {
            currentIndex--;
        } else if (fromIndex > currentIndex && toIndex <= currentIndex) {
            currentIndex++;
        }

        notifyTrackMoved(fromIndex, toIndex);
        if (oldIndex != currentIndex) {
            notifyCurrentIndexChanged(currentIndex, current());
        }
    }

    public void clear() {
        playlist.clear();
        currentIndex = -1;
        playOrderStrategy.onPlaylistReset(0, -1);
        notifyPlaylistCleared();
        notifyCurrentIndexChanged(-1, null);
    }

    // --- Mode & Strategy Configuration ---

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = Objects.requireNonNull(repeatMode, "repeatMode cannot be null");
    }

    public PlayOrderStrategy getPlayOrderStrategy() {
        return playOrderStrategy;
    }

    public void setPlayOrderStrategy(PlayOrderStrategy strategy) {
        this.playOrderStrategy = Objects.requireNonNull(strategy, "strategy cannot be null");
        this.playOrderStrategy.onPlaylistReset(playlist.size(), currentIndex);
    }

    public boolean isShuffle() {
        return playOrderStrategy instanceof ShuffleOrder;
    }

    public void setShuffle(boolean shuffle) {
        if (shuffle && !isShuffle()) {
            setPlayOrderStrategy(new ShuffleOrder(playlist.size(), currentIndex, new java.util.Random()));
        } else if (!shuffle && isShuffle()) {
            setPlayOrderStrategy(new SequentialOrder());
        }
    }

    // --- Listener Management ---

    public void addListener(PlaylistListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(PlaylistListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyTracksAdded(int index, List<Track> addedTracks) {
        for (PlaylistListener l : listeners) {
            l.onTracksAdded(index, addedTracks);
        }
    }

    private void notifyTrackRemoved(int index, Track removedTrack) {
        for (PlaylistListener l : listeners) {
            l.onTrackRemoved(index, removedTrack);
        }
    }

    private void notifyTrackMoved(int fromIndex, int toIndex) {
        for (PlaylistListener l : listeners) {
            l.onTrackMoved(fromIndex, toIndex);
        }
    }

    private void notifyPlaylistCleared() {
        for (PlaylistListener l : listeners) {
            l.onPlaylistCleared();
        }
    }

    private void notifyCurrentIndexChanged(int newIndex, Track track) {
        for (PlaylistListener l : listeners) {
            l.onCurrentIndexChanged(newIndex, track);
        }
    }
}
