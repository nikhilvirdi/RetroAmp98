package io.github.nikhilvirdi.retroamp98.playlist;

/**
 * Strategy pattern interface governing playback order traversal (sequential vs shuffle).
 */
public interface PlayOrderStrategy {

    /**
     * Calculates the next track index to play.
     *
     * @param currentIndex current active index (-1 if none)
     * @param playlistSize total number of tracks
     * @param repeatMode active repeat mode
     * @return next track index, or -1 if no next track exists
     */
    int nextIndex(int currentIndex, int playlistSize, RepeatMode repeatMode);

    /**
     * Calculates the previous track index to play.
     *
     * @param currentIndex current active index (-1 if none)
     * @param playlistSize total number of tracks
     * @param repeatMode active repeat mode
     * @return previous track index, or -1 if no previous track exists
     */
    int previousIndex(int currentIndex, int playlistSize, RepeatMode repeatMode);

    /**
     * Checks if a next track is available from the current position.
     */
    boolean hasNext(int currentIndex, int playlistSize, RepeatMode repeatMode);

    /**
     * Checks if a previous track is available from the current position.
     */
    boolean hasPrevious(int currentIndex, int playlistSize, RepeatMode repeatMode);

    /**
     * Notifies strategy that a track was inserted at the given index.
     */
    default void onTrackAdded(int addedIndex, int newSize) {}

    /**
     * Notifies strategy that a track was removed from the given index.
     */
    default void onTrackRemoved(int removedIndex, int newSize) {}

    /**
     * Notifies strategy that a track was moved from fromIndex to toIndex.
     */
    default void onTrackMoved(int fromIndex, int toIndex, int size) {}

    /**
     * Notifies strategy that a track was directly selected (e.g. via jumpTo).
     */
    default void onIndexSelected(int newIndex, int playlistSize) {}

    /**
     * Notifies strategy that the playlist was initialized, cleared, or replaced.
     */
    default void onPlaylistReset(int size, int currentIndex) {}
}
