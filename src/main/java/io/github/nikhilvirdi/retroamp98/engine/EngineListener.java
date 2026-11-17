package io.github.nikhilvirdi.retroamp98.engine;

import java.time.Duration;

/** JavaFxMediaEngine delivers these callbacks on the JavaFX Application Thread. */
public interface EngineListener {

    void onStateChanged(PlayerState state);

    void onPositionChanged(Duration position);

    void onDurationKnown(Duration duration);

    void onEndOfMedia();

    void onError(String message);

    default void onTrackInfoReady(TrackInfo trackInfo) {}
    /** Magnitudes are borrowed, read-only, and valid only for this callback. */
    default void onSpectrum(float[] magnitudes) {}
}
