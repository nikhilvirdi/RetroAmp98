package io.github.nikhilvirdi.retroamp98.playlist;

/**
 * Repeat modes for playlist playback traversal.
 */
public enum RepeatMode {
    /** Play through the playlist once and stop at the boundary. */
    OFF,
    /** Repeat the current track indefinitely. */
    ONE,
    /** Loop through the entire playlist continuously. */
    ALL
}
