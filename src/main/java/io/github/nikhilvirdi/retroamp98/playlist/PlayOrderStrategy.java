package io.github.nikhilvirdi.retroamp98.playlist;

public interface PlayOrderStrategy {

    int nextIndex(int currentIndex, int playlistSize, RepeatMode repeatMode);

    int previousIndex(int currentIndex, int playlistSize, RepeatMode repeatMode);

    boolean hasNext(int currentIndex, int playlistSize, RepeatMode repeatMode);

    boolean hasPrevious(int currentIndex, int playlistSize, RepeatMode repeatMode);

    default void onTrackAdded(int addedIndex, int newSize) {}

    default void onTrackRemoved(int removedIndex, int newSize) {}

    default void onTrackMoved(int fromIndex, int toIndex, int size) {}

    default void onIndexSelected(int newIndex, int playlistSize) {}

    default void onPlaylistReset(int size, int currentIndex) {}
}
