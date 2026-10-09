package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages playlist queue, track ordering, and shuffle/repeat modes.
 */
public class PlaylistManager {
    private final List<String> queue = new ArrayList<>();
    private boolean shuffle = false;
    private boolean repeat = false;

    public List<String> getQueue() {
        return queue;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }
}
