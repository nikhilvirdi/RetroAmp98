package io.github.nikhilvirdi.retroamp98.engine;

import java.time.Duration;

/**
 * Listener interface for playback events emitted by MediaEngine.
 * All callbacks are guaranteed to be delivered on the Swing EDT.
 */
public interface EngineListener {

    /**
     * Invoked when the engine transitions to a new PlayerState.
     *
     * @param state the new player state
     */
    void onStateChanged(PlayerState state);

    /**
     * Invoked periodically during playback when playback position changes.
     *
     * @param position current playback position
     */
    void onPositionChanged(Duration position);

    /**
     * Invoked once media metadata is resolved and total duration becomes known.
     *
     * @param duration total duration of the loaded media
     */
    void onDurationKnown(Duration duration);

    /**
     * Invoked when playback completes reaching the end of the media file.
     */
    void onEndOfMedia();

    /**
     * Invoked when an error occurs during loading or playback.
     *
     * @param message descriptive error message
     */
    void onError(String message);
}
