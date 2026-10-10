package io.github.nikhilvirdi.retroamp98.engine;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Media playback engine interface for RetroAmp98.
 * Exposes commands, queries, and listeners without exposing any JavaFX types.
 * All public methods are safe to invoke from the Swing EDT.
 */
public interface MediaEngine {

    // --- Commands ---

    /**
     * Loads a media file from the given file system path.
     * Cleanly disposes any previous player and moves to LOADING then STOPPED or ERROR.
     *
     * @param path local path to audio file
     */
    void load(Path path);

    /**
     * Starts or resumes media playback.
     */
    void play();

    /**
     * Pauses media playback at the current position.
     */
    void pause();

    /**
     * Stops media playback and resets position to the beginning.
     */
    void stop();

    /**
     * Seeks to a specific position in the loaded media.
     *
     * @param position target playback position
     */
    void seek(Duration position);

    /**
     * Sets playback volume.
     *
     * @param volume volume level clamped between 0.0 (silent) and 1.0 (full)
     */
    void setVolume(double volume);

    /**
     * Mutes or unmutes audio playback.
     *
     * @param muted true to mute, false to unmute
     */
    void setMuted(boolean muted);

    /**
     * Sets playback rate/speed.
     *
     * @param rate playback multiplier clamped between 0.5 (half speed) and 2.0 (double speed)
     */
    void setRate(double rate);

    // --- Queries ---

    /**
     * Returns current playback state.
     */
    PlayerState getState();

    /**
     * Returns current playback position.
     */
    Duration getPosition();

    /**
     * Returns total duration of loaded media, or Duration.ZERO if not known.
     */
    Duration getDuration();

    /**
     * Returns current volume level between 0.0 and 1.0.
     */
    double getVolume();

    /**
     * Returns true if audio is muted.
     */
    boolean isMuted();

    /**
     * Returns current playback rate between 0.5 and 2.0.
     */
    double getRate();

    /**
     * Returns the plain Swing component surface used for rendering video.
     * Contains no JavaFX types in its public interface.
     */
    javax.swing.JComponent getVideoComponent();

    // --- Listeners ---

    /**
     * Adds an engine listener. Callbacks are delivered on the Swing EDT.
     */
    void addListener(EngineListener listener);

    /**
     * Removes a previously registered engine listener.
     */
    void removeListener(EngineListener listener);
}
