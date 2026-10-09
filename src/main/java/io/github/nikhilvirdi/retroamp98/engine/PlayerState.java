package io.github.nikhilvirdi.retroamp98.engine;

/**
 * Playback lifecycle states for the media engine.
 */
public enum PlayerState {
    /** Engine is idle with no media or initial uninitialized state. */
    IDLE,
    /** Media is currently being parsed and prepared for playback. */
    LOADING,
    /** Media is actively playing. */
    PLAYING,
    /** Media playback is paused at the current position. */
    PAUSED,
    /** Media playback is stopped and position is reset to start. */
    STOPPED,
    /** Media failed to load or encountered a playback error. */
    ERROR
}
