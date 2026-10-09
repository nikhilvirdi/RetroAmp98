package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import java.time.Duration;

/**
 * Listener for high-level playback and playlist events published by PlayerController.
 * All callbacks execute on the Swing EDT.
 */
public interface ControllerListener {
    default void onStateChanged(PlayerState state) {}
    default void onPositionChanged(Duration position, Duration duration) {}
    default void onDurationKnown(Duration duration) {}
    default void onTrackChanged(Track track, int index) {}
    default void onVolumeChanged(double volume, boolean muted) {}
    default void onRepeatModeChanged(RepeatMode mode) {}
    default void onShuffleChanged(boolean shuffle) {}
    default void onPlaylistUpdated() {}
}
