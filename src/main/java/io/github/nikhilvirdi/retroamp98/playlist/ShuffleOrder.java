package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Shuffle play order strategy.
 * Guarantees fair shuffling (no track repeated until all tracks in the playlist have played).
 * Dynamically adapts to tracks added, removed, or moved mid-shuffle.
 */
public class ShuffleOrder implements PlayOrderStrategy {

    private final Random random;
    private final List<Integer> order = new ArrayList<>();
    private int pointer = -1;

    public ShuffleOrder() {
        this(new Random());
    }

    public ShuffleOrder(Random random) {
        this.random = Objects.requireNonNull(random, "random cannot be null");
    }

    public ShuffleOrder(int playlistSize, int currentTrackIndex, Random random) {
        this.random = Objects.requireNonNull(random, "random cannot be null");
        onPlaylistReset(playlistSize, currentTrackIndex);
    }

    @Override
    public int nextIndex(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return -1;
        }

        // Initialize if order is out of sync with playlist size
        if (order.size() != playlistSize) {
            onPlaylistReset(playlistSize, currentIndex);
        }

        if (repeatMode == RepeatMode.ONE && currentIndex >= 0) {
            return currentIndex;
        }

        if (pointer < order.size() - 1) {
            pointer++;
            return order.get(pointer);
        }

        // Reached end of current shuffle cycle
        if (repeatMode == RepeatMode.ALL) {
            int lastPlayed = (pointer >= 0 && pointer < order.size()) ? order.get(pointer) : -1;
            rebuildCycle(playlistSize, lastPlayed);
            pointer = 0;
            return order.get(pointer);
        }

        // RepeatMode.OFF
        return -1;
    }

    @Override
    public int previousIndex(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0 || order.isEmpty()) {
            return -1;
        }

        if (repeatMode == RepeatMode.ONE && currentIndex >= 0) {
            return currentIndex;
        }

        if (pointer > 0) {
            pointer--;
            return order.get(pointer);
        }

        if (repeatMode == RepeatMode.ALL && !order.isEmpty()) {
            pointer = order.size() - 1;
            return order.get(pointer);
        }

        return -1;
    }

    @Override
    public boolean hasNext(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0) {
            return false;
        }
        if (order.size() != playlistSize) {
            return true;
        }
        if (repeatMode == RepeatMode.ONE || repeatMode == RepeatMode.ALL) {
            return true;
        }
        return pointer < order.size() - 1;
    }

    @Override
    public boolean hasPrevious(int currentIndex, int playlistSize, RepeatMode repeatMode) {
        if (playlistSize <= 0 || order.isEmpty()) {
            return false;
        }
        if (repeatMode == RepeatMode.ONE || repeatMode == RepeatMode.ALL) {
            return true;
        }
        return pointer > 0;
    }

    @Override
    public void onTrackAdded(int addedIndex, int newSize) {
        // Shift existing indices that are >= addedIndex
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) >= addedIndex) {
                order.set(i, order.get(i) + 1);
            }
        }

        // Insert new track into the unplayed portion of the shuffle cycle
        if (order.isEmpty()) {
            order.add(addedIndex);
            pointer = -1;
        } else if (pointer < order.size() - 1) {
            int unplayedRange = order.size() - (pointer + 1);
            int insertPos = (pointer + 1) + random.nextInt(unplayedRange + 1);
            order.add(insertPos, addedIndex);
        } else {
            order.add(addedIndex);
        }
    }

    @Override
    public void onTrackRemoved(int removedIndex, int newSize) {
        int removedPos = order.indexOf(removedIndex);
        if (removedPos >= 0) {
            order.remove(removedPos);
            if (removedPos < pointer) {
                pointer--;
            } else if (removedPos == pointer && pointer >= order.size()) {
                pointer = order.size() - 1;
            }
        }

        // Shift down indices that were above removedIndex
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) > removedIndex) {
                order.set(i, order.get(i) - 1);
            }
        }
    }

    @Override
    public void onTrackMoved(int fromIndex, int toIndex, int size) {
        for (int i = 0; i < order.size(); i++) {
            int current = order.get(i);
            if (current == fromIndex) {
                order.set(i, toIndex);
            } else if (fromIndex < toIndex && current > fromIndex && current <= toIndex) {
                order.set(i, current - 1);
            } else if (fromIndex > toIndex && current >= toIndex && current < fromIndex) {
                order.set(i, current + 1);
            }
        }
    }

    @Override
    public void onIndexSelected(int newIndex, int playlistSize) {
        if (order.size() != playlistSize) {
            onPlaylistReset(playlistSize, newIndex);
            return;
        }
        int pos = order.indexOf(newIndex);
        if (pos >= 0) {
            pointer = pos;
        } else {
            onPlaylistReset(playlistSize, newIndex);
        }
    }

    @Override
    public void onPlaylistReset(int size, int currentIndex) {
        order.clear();
        if (size <= 0) {
            pointer = -1;
            return;
        }

        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i != currentIndex) {
                pool.add(i);
            }
        }
        Collections.shuffle(pool, random);

        if (currentIndex >= 0 && currentIndex < size) {
            order.add(currentIndex);
            order.addAll(pool);
            pointer = 0;
        } else {
            order.addAll(pool);
            pointer = -1;
        }
    }

    private void rebuildCycle(int playlistSize, int lastPlayed) {
        order.clear();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < playlistSize; i++) {
            pool.add(i);
        }
        Collections.shuffle(pool, random);

        // Avoid starting the new cycle with the exact same track if size > 1
        if (playlistSize > 1 && !pool.isEmpty() && pool.get(0) == lastPlayed) {
            Collections.swap(pool, 0, random.nextInt(playlistSize - 1) + 1);
        }
        order.addAll(pool);
    }

    public List<Integer> getShuffleOrder() {
        return Collections.unmodifiableList(order);
    }

    public int getPointer() {
        return pointer;
    }
}
