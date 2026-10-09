package io.github.nikhilvirdi.retroamp98.engine;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JavaFX-backed implementation of MediaEngine using javafx.scene.media.MediaPlayer.
 *
 * Threading bridge contract:
 * - All JavaFX calls execute strictly on the JavaFX Application Thread.
 * - All EngineListener callback events are delivered on the Swing EDT.
 * - All public interface methods are safe for invocation from the Swing EDT.
 */
public class JavaFxMediaEngine implements MediaEngine {

    // Initialize JavaFX Platform toolkit once and disable implicit exit
    static {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit was already started (e.g., via JFXPanel or test runner)
        }
        Platform.setImplicitExit(false);
    }

    private final List<EngineListener> listeners = new CopyOnWriteArrayList<>();

    // Cached engine state (thread-safe reads from EDT)
    private volatile PlayerState state = PlayerState.IDLE;
    private volatile Duration position = Duration.ZERO;
    private volatile Duration duration = Duration.ZERO;
    private volatile double volume = 1.0;
    private volatile boolean muted = false;
    private volatile double rate = 1.0;

    // JavaFX player reference - manipulated strictly on the JavaFX thread
    private MediaPlayer currentMediaPlayer;

    public JavaFxMediaEngine() {
        // Ready for media loading
    }

    @Override
    public void load(Path path) {
        if (path == null) {
            transitionState(PlayerState.ERROR);
            dispatchError("Media path cannot be null.");
            return;
        }

        if (!Files.exists(path)) {
            transitionState(PlayerState.ERROR);
            dispatchError("Media file does not exist: " + path);
            return;
        }

        // Move to LOADING state and reset playback offsets
        transitionState(PlayerState.LOADING);
        this.position = Duration.ZERO;
        this.duration = Duration.ZERO;

        runOnFxThread(() -> {
            // Cleanly dispose previous player to prevent native memory and audio leaks
            disposeCurrentPlayer();

            try {
                Media media = new Media(path.toUri().toString());

                media.setOnError(() -> {
                    String msg = (media.getError() != null) ? media.getError().getMessage() : "Media parse error.";
                    transitionState(PlayerState.ERROR);
                    dispatchError(msg);
                });

                MediaPlayer player = new MediaPlayer(media);

                // Persist audio volume, mute state, and rate across media loads
                player.setVolume(this.volume);
                player.setMute(this.muted);
                player.setRate(this.rate);

                // Metadata and duration ready
                player.setOnReady(() -> {
                    javafx.util.Duration total = player.getTotalDuration();
                    Duration dur = (total != null && !total.isUnknown())
                            ? Duration.ofMillis((long) total.toMillis())
                            : Duration.ZERO;
                    this.duration = dur;
                    dispatchDurationKnown(dur);

                    // Move to STOPPED if playback has not already been started
                    if (player.getStatus() != MediaPlayer.Status.PLAYING) {
                        transitionState(PlayerState.STOPPED);
                    }
                });

                // Position tracking listener
                player.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.isUnknown()) {
                        Duration pos = Duration.ofMillis((long) newVal.toMillis());
                        this.position = pos;
                        dispatchPositionChanged(pos);
                    }
                });

                // Status changes
                player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                    if (newStatus == null) return;
                    switch (newStatus) {
                        case PLAYING -> transitionState(PlayerState.PLAYING);
                        case PAUSED -> transitionState(PlayerState.PAUSED);
                        case STOPPED -> {
                            this.position = Duration.ZERO;
                            transitionState(PlayerState.STOPPED);
                        }
                        case HALTED -> {
                            transitionState(PlayerState.ERROR);
                            dispatchError("Player halted unexpectedly.");
                        }
                        default -> {}
                    }
                });

                // Reached end of media
                player.setOnEndOfMedia(() -> {
                    this.position = this.duration;
                    transitionState(PlayerState.STOPPED);
                    dispatchEndOfMedia();
                });

                // Runtime player errors
                player.setOnError(() -> {
                    String msg = (player.getError() != null) ? player.getError().getMessage() : "Playback error.";
                    transitionState(PlayerState.ERROR);
                    dispatchError(msg);
                });

                this.currentMediaPlayer = player;
            } catch (Exception e) {
                // Catch any instantiation or URI errors without throwing to caller
                transitionState(PlayerState.ERROR);
                dispatchError("Failed to load media: " + e.getMessage());
            }
        });
    }

    @Override
    public void play() {
        if (state == PlayerState.ERROR || state == PlayerState.IDLE) {
            return;
        }
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.play();
            }
        });
    }

    @Override
    public void pause() {
        if (state != PlayerState.PLAYING) {
            return;
        }
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.pause();
            }
        });
    }

    @Override
    public void stop() {
        if (state == PlayerState.ERROR || state == PlayerState.IDLE) {
            return;
        }
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.stop();
            }
            this.position = Duration.ZERO;
            transitionState(PlayerState.STOPPED);
        });
    }

    @Override
    public void seek(Duration targetPosition) {
        if (targetPosition == null || state == PlayerState.ERROR || state == PlayerState.IDLE) {
            return;
        }
        runOnFxThread(() -> {
            if (currentMediaPlayer != null && currentMediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN) {
                long targetMillis = Math.max(0, targetPosition.toMillis());
                currentMediaPlayer.seek(javafx.util.Duration.millis(targetMillis));
            }
        });
    }

    @Override
    public void setVolume(double volume) {
        // Clamp volume to valid range [0.0, 1.0]
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.setVolume(this.volume);
            }
        });
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.setMute(this.muted);
            }
        });
    }

    @Override
    public void setRate(double rate) {
        // Clamp rate to valid range [0.5, 2.0]
        this.rate = Math.max(0.5, Math.min(2.0, rate));
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.setRate(this.rate);
            }
        });
    }

    @Override
    public PlayerState getState() {
        return state;
    }

    @Override
    public Duration getPosition() {
        return position;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public double getRate() {
        return rate;
    }

    @Override
    public void addListener(EngineListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(EngineListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    // --- Thread Helpers & Event Dispatching ---

    /**
     * Executes action on JavaFX application thread.
     */
    private void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Executes action on Swing Event Dispatch Thread (EDT).
     */
    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private void transitionState(PlayerState newState) {
        if (this.state == newState) return;
        this.state = newState;
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onStateChanged(newState);
            }
        });
    }

    private void dispatchPositionChanged(Duration pos) {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onPositionChanged(pos);
            }
        });
    }

    private void dispatchDurationKnown(Duration dur) {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onDurationKnown(dur);
            }
        });
    }

    private void dispatchEndOfMedia() {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onEndOfMedia();
            }
        });
    }

    private void dispatchError(String errorMessage) {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Disposes the current MediaPlayer cleanly on the JavaFX thread.
     */
    private void disposeCurrentPlayer() {
        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
                currentMediaPlayer.dispose();
            } catch (Exception ignored) {
            }
            currentMediaPlayer = null;
        }
    }
}
