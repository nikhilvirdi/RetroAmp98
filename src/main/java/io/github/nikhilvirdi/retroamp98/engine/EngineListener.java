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

    /**
     * Invoked when video dimensions become known.
     *
     * @param width  native video width in pixels
     * @param height native video height in pixels
     */
    default void onVideoSizeKnown(int width, int height) {}

    /**
     * Invoked when media changes between video and audio-only.
     *
     * @param hasVideo true if loaded media contains an active video track
     */
    default void onHasVideoChanged(boolean hasVideo) {}

    /**
     * Invoked when media metadata (ID3 tags / cover art) is resolved.
     *
     * @param trackInfo resolved metadata record
     */
    default void onTrackInfoReady(TrackInfo trackInfo) {}

    /**
     * Invoked periodically during playback with audio spectrum magnitudes.
     *
     * @param magnitudes array of band magnitudes in dB (typically -60dB to 0dB)
     */
    default void onSpectrumData(float[] magnitudes) {}
}
