package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.List;

/**
 * Listener interface for observing mutations and selection changes in PlaylistManager.
 */
public interface PlaylistListener {

    /**
     * Fired when new tracks are added to the playlist.
     *
     * @param index starting index where tracks were inserted
     * @param tracks the tracks that were added
     */
    default void onTracksAdded(int index, List<Track> tracks) {}

    /**
     * Fired when a track is removed from the playlist.
     *
     * @param index index the track was removed from
     * @param removedTrack the track that was removed
     */
    default void onTrackRemoved(int index, Track removedTrack) {}

    /**
     * Fired when a track is moved from one position to another.
     *
     * @param fromIndex previous index
     * @param toIndex new index
     */
    default void onTrackMoved(int fromIndex, int toIndex) {}

    /**
     * Fired when the playlist is cleared.
     */
    default void onPlaylistCleared() {}

    /**
     * Fired when the active playback track or index changes.
     *
     * @param newIndex new active index (-1 if none)
     * @param currentTrack active track, or null if playlist is empty or index unselected
     */
    default void onCurrentIndexChanged(int newIndex, Track currentTrack) {}
}
