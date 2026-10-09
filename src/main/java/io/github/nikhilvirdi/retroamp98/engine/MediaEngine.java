package io.github.nikhilvirdi.retroamp98.engine;

/**
 * Interface defining media playback operations.
 * Concrete implementation will handle playback mechanics via JavaFX Media.
 */
public interface MediaEngine {
    // Playback control operations to be implemented
    void play(String mediaUri);
    void pause();
    void stop();
}
