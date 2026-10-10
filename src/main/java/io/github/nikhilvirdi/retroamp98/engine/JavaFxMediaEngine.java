package io.github.nikhilvirdi.retroamp98.engine;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.VideoTrack;
import javafx.scene.paint.Color;

import javax.swing.JComponent;
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

    // Equalizer state (persisted across media loads)
    private volatile boolean equalizerEnabled = false;
    private final double[] equalizerBands = new double[EqualizerPresets.NUM_BANDS];

    // JavaFX player reference - manipulated strictly on the JavaFX thread
    private MediaPlayer currentMediaPlayer;

    // Swing video viewport surface backed by JavaFX MediaView
    private final JFXPanel videoComponent = new JFXPanel();
    private MediaView mediaView;
    private StackPane videoRoot;

    public JavaFxMediaEngine() {
        runOnFxThread(() -> {
            mediaView = new MediaView();
            mediaView.setPreserveRatio(true);
            videoRoot = new StackPane(mediaView);
            videoRoot.setStyle("-fx-background-color: black;");
            mediaView.fitWidthProperty().bind(videoRoot.widthProperty());
            mediaView.fitHeightProperty().bind(videoRoot.heightProperty());
            Scene scene = new Scene(videoRoot, Color.BLACK);
            videoComponent.setScene(scene);
        });
    }

    @Override
    public JComponent getVideoComponent() {
        return videoComponent;
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

                media.getMetadata().addListener((MapChangeListener<String, Object>) change -> {
                    extractAndDispatchTrackInfo(media);
                });

                media.setOnError(() -> {
                    String msg = (media.getError() != null) ? media.getError().getMessage() : "Media parse error.";
                    transitionState(PlayerState.ERROR);
                    dispatchError(msg);
                });

                MediaPlayer player = new MediaPlayer(media);
                if (mediaView != null) {
                    mediaView.setMediaPlayer(player);
                }

                // Persist audio volume, mute state, and rate across media loads
                player.setVolume(this.volume);
                player.setMute(this.muted);
                player.setRate(this.rate);

                // Apply persistent equalizer settings
                applyEqualizerSettings(player);

                // Spectrum visualizer hook: ~20 updates/sec, 32 bands, -60dB threshold
                player.setAudioSpectrumInterval(0.05);
                player.setAudioSpectrumNumBands(32);
                player.setAudioSpectrumThreshold(-60);
                player.setAudioSpectrumListener((timestamp, dur, magnitudes, phases) -> {
                    if (this.state == PlayerState.PLAYING && magnitudes != null && magnitudes.length > 0) {
                        float[] copy = magnitudes.clone();
                        runOnEdt(() -> {
                            for (EngineListener listener : listeners) {
                                listener.onSpectrumData(copy);
                            }
                        });
                    }
                });

                // Metadata, duration, and video dimension readiness
                player.setOnReady(() -> {
                    javafx.util.Duration total = player.getTotalDuration();
                    Duration dur = (total != null && !total.isUnknown())
                            ? Duration.ofMillis((long) total.toMillis())
                            : Duration.ZERO;
                    this.duration = dur;
                    dispatchDurationKnown(dur);

                    int w = media.getWidth();
                    int h = media.getHeight();
                    boolean hasVideo = (w > 0 && h > 0);
                    if (!hasVideo) {
                        for (javafx.scene.media.Track t : media.getTracks()) {
                            if (t instanceof VideoTrack) {
                                hasVideo = true;
                                break;
                            }
                        }
                    }
                    dispatchHasVideoChanged(hasVideo);
                    if (w > 0 && h > 0) {
                        dispatchVideoSizeKnown(w, h);
                    }

                    extractAndDispatchTrackInfo(media);

                    // Move to STOPPED if playback has not already been started
                    if (player.getStatus() != MediaPlayer.Status.PLAYING) {
                        transitionState(PlayerState.STOPPED);
                    }
                });

                media.widthProperty().addListener((obs, oldW, newW) -> {
                    int w = newW.intValue();
                    int h = media.getHeight();
                    if (w > 0 && h > 0) {
                        dispatchHasVideoChanged(true);
                        dispatchVideoSizeKnown(w, h);
                    }
                });
                media.heightProperty().addListener((obs, oldH, newH) -> {
                    int w = media.getWidth();
                    int h = newH.intValue();
                    if (w > 0 && h > 0) {
                        dispatchHasVideoChanged(true);
                        dispatchVideoSizeKnown(w, h);
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
    public void setEqualizerEnabled(boolean enabled) {
        this.equalizerEnabled = enabled;
        runOnFxThread(() -> {
            if (currentMediaPlayer != null) {
                AudioEqualizer eq = currentMediaPlayer.getAudioEqualizer();
                if (eq != null) {
                    eq.setEnabled(enabled);
                }
            }
        });
    }

    @Override
    public boolean isEqualizerEnabled() {
        return equalizerEnabled;
    }

    @Override
    public void setEqualizerBand(int index, double gainDb) {
        if (index >= 0 && index < equalizerBands.length) {
            double clamped = EqualizerPresets.clampGain(gainDb);
            equalizerBands[index] = clamped;
            runOnFxThread(() -> {
                if (currentMediaPlayer != null) {
                    AudioEqualizer eq = currentMediaPlayer.getAudioEqualizer();
                    if (eq != null && index < eq.getBands().size()) {
                        eq.getBands().get(index).setGain(clamped);
                    }
                }
            });
        }
    }

    @Override
    public double getEqualizerBand(int index) {
        if (index >= 0 && index < equalizerBands.length) {
            return equalizerBands[index];
        }
        return 0.0;
    }

    private void applyEqualizerSettings(MediaPlayer player) {
        if (player == null) return;
        AudioEqualizer eq = player.getAudioEqualizer();
        if (eq != null) {
            eq.setEnabled(this.equalizerEnabled);
            ObservableList<EqualizerBand> bands = eq.getBands();
            for (int i = 0; i < Math.min(bands.size(), equalizerBands.length); i++) {
                bands.get(i).setGain(equalizerBands[i]);
            }
        }
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

    private void dispatchVideoSizeKnown(int w, int h) {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onVideoSizeKnown(w, h);
            }
        });
    }

    private void dispatchHasVideoChanged(boolean hasVideo) {
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onHasVideoChanged(hasVideo);
            }
        });
    }

    private void extractAndDispatchTrackInfo(Media media) {
        if (media == null) return;
        ObservableMap<String, Object> meta = media.getMetadata();
        String title = (meta.get("title") instanceof String s && !s.isBlank()) ? s.trim() : null;
        String artist = (meta.get("artist") instanceof String s && !s.isBlank()) ? s.trim() : null;
        String album = (meta.get("album") instanceof String s && !s.isBlank()) ? s.trim() : null;

        java.awt.Image coverImg = null;
        Object imgObj = meta.get("image");
        if (imgObj instanceof javafx.scene.image.Image fxImg) {
            try {
                coverImg = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImg, null);
            } catch (Exception ignored) {
            }
        }
        TrackInfo info = new TrackInfo(title, artist, album, coverImg);
        runOnEdt(() -> {
            for (EngineListener listener : listeners) {
                listener.onTrackInfoReady(info);
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
        if (mediaView != null) {
            mediaView.setMediaPlayer(null);
        }
        dispatchHasVideoChanged(false);
    }
}
