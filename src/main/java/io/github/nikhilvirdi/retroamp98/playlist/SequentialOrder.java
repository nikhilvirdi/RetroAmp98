package io.github.nikhilvirdi.retroamp98.playlist;

/**
 * Sequential playback order traversal strategy.
 * Advances linearly through track indices 0..size-1 with wrap-around under RepeatMode.ALL
 * and current index retention under RepeatMode.ONE.
 */
public class SequentialOrder implements PlayOrderStrategy {

    @Override
    public int nextIndex(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return -1;
        }
        if (currentIndex < 0) {
            return 0;
        }
        if (repeatMode == RepeatMode.ONE) {
            return currentIndex;
        }
        if (repeatMode == RepeatMode.ALL) {
            return (currentIndex + 1) % playlistSize;
        }
        // RepeatMode.OFF
        return (currentIndex < playlistSize - 1) ? currentIndex + 1 : -1;
    }

    @Override
    public int previousIndex(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return -1;
        }
        if (currentIndex < 0) {
            return -1;
        }
        if (repeatMode == RepeatMode.ONE) {
            return currentIndex;
        }
        if (repeatMode == RepeatMode.ALL) {
            return (currentIndex - 1 + playlistSize) % playlistSize;
        }
        // RepeatMode.OFF
        return (currentIndex > 0) ? currentIndex - 1 : -1;
    }

    @Override
    public boolean hasNext(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return false;
        }
        if (currentIndex < 0) {
            return true;
        }
        if (repeatMode == RepeatMode.ONE || repeatMode == RepeatMode.ALL) {
            return true;
        }
        return currentIndex < playlistSize - 1;
    }

    @Override
    public boolean hasPrevious(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return false;
        }
        if (currentIndex < 0) {
            return false;
        }
        if (repeatMode == RepeatMode.ONE || repeatMode == RepeatMode.ALL) {
            return true;
        }
        return currentIndex > 0;
    }
}
