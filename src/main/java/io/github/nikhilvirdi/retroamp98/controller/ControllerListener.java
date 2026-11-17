package io.github.nikhilvirdi.retroamp98.controller;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import java.time.Duration;

public interface ControllerListener {
    enum NoticeLevel { INFO, WARNING, ERROR }

    default void onStateChanged(PlayerState state) {}
    default void onPositionChanged(Duration position, Duration duration) {}
    default void onDurationKnown(Duration duration) {}
    default void onTrackChanged(Track track, int index) {}
    default void onVolumeChanged(double volume, boolean muted) {}
    default void onRepeatModeChanged(RepeatMode mode) {}
    default void onShuffleChanged(boolean shuffle) {}
    default void onPlaylistUpdated() {}
    default void onHistoryUpdated() {}
    default void onTrackInfoReady(TrackInfo trackInfo) {}
    default void onSpectrum(float[] magnitudes) {}
    default void onNotice(NoticeLevel level, String title, String message) {}
}
