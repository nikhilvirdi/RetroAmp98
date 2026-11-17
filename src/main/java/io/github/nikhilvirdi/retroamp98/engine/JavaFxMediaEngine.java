package io.github.nikhilvirdi.retroamp98.engine;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JavaFxMediaEngine implements MediaEngine {

    static {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    private final CopyOnWriteArrayList<EngineListener> listeners = new CopyOnWriteArrayList<>();

    private volatile PlayerState state = PlayerState.IDLE;
    private volatile Duration position = Duration.ZERO;
    private volatile Duration duration = Duration.ZERO;
    private volatile double volume = 1.0;
    private volatile boolean muted;
    private MediaPlayer currentMediaPlayer;
    private PlayerState requestedState = PlayerState.STOPPED;
    private boolean ended;
    private boolean closed;
    private long revision;
    private Duration pendingSeek;

    @Override
    public void load(Path path) {
        runOnFxThread(() -> {
            if (closed) return;
            revision++;
            disposeCurrentPlayer();
            requestedState = PlayerState.STOPPED;
            ended = false;
            pendingSeek = null;
            position = Duration.ZERO;
            duration = Duration.ZERO;

            if (path == null || !Files.isRegularFile(path)) {
                fail(path == null ? "Media path cannot be null." : "Media file does not exist: " + path);
                return;
            }

            transitionState(PlayerState.LOADING);
            dispatch(listener -> listener.onPositionChanged(Duration.ZERO));
            dispatch(listener -> listener.onDurationKnown(Duration.ZERO));
            try {
                Media media = new Media(path.toAbsolutePath().toUri().toString());
                MediaPlayer player = new MediaPlayer(media);
                currentMediaPlayer = player;
                player.setVolume(volume);
                player.setMute(muted);
                player.setAudioSpectrumNumBands(128);
                player.setAudioSpectrumInterval(1.0 / 30);
                player.setAudioSpectrumThreshold(-60);
                player.setAudioSpectrumListener((timestamp, interval, magnitudes, phases) -> {
                    if (currentMediaPlayer != player || state != PlayerState.PLAYING
                            || requestedState != PlayerState.PLAYING || closed) return;
                    for (EngineListener listener : listeners) listener.onSpectrum(magnitudes);
                });

                media.getMetadata().addListener((MapChangeListener<String, Object>) change -> {
                    if (currentMediaPlayer == player) dispatchTrackInfo(media);
                });
                media.setOnError(() -> {
                    if (currentMediaPlayer == player) {
                        fail(media.getError() == null ? "Media parse error." : media.getError().getMessage());
                    }
                });
                player.setOnError(() -> {
                    if (currentMediaPlayer == player) {
                        fail(player.getError() == null ? "Playback error." : player.getError().getMessage());
                    }
                });
                player.setOnReady(() -> {
                    if (currentMediaPlayer != player) return;
                    duration = finiteDuration(player.getTotalDuration());
                    Duration knownDuration = duration;
                    dispatch(listener -> listener.onDurationKnown(knownDuration));
                    dispatchTrackInfo(media);
                    if (requestedState == PlayerState.PLAYING) {
                        player.play();
                    } else {
                        transitionState(requestedState);
                    }
                });
                player.currentTimeProperty().addListener((observable, previous, current) -> {
                    if (currentMediaPlayer != player) return;
                    position = requestedState == PlayerState.STOPPED && !ended
                            ? stoppedPosition() : finiteDuration(current);
                    Duration updatedPosition = position;
                    dispatch(listener -> listener.onPositionChanged(updatedPosition));
                });
                player.statusProperty().addListener((observable, previous, current) -> {
                    if (currentMediaPlayer != player || current == null) return;
                    switch (current) {
                        case PLAYING -> {
                            applyPendingSeek(player);
                            transitionState(PlayerState.PLAYING);
                        }
                        case PAUSED -> transitionState(PlayerState.PAUSED);
                        case STOPPED -> {
                            position = stoppedPosition();
                            transitionState(PlayerState.STOPPED);
                            Duration stoppedPosition = position;
                            dispatch(listener -> listener.onPositionChanged(stoppedPosition));
                        }
                        case HALTED -> fail("Player halted unexpectedly.");
                        default -> {}
                    }
                });
                player.setOnEndOfMedia(() -> {
                    if (currentMediaPlayer != player) return;
                    ended = true;
                    requestedState = PlayerState.STOPPED;
                    position = duration;
                    transitionState(PlayerState.STOPPED);
                    Duration finalPosition = position;
                    dispatch(listener -> listener.onPositionChanged(finalPosition));
                    dispatch(EngineListener::onEndOfMedia);
                });
            } catch (Exception e) {
                fail("Failed to load media: " + e.getMessage());
            }
        });
    }

    @Override
    public void play() {
        runOnFxThread(() -> {
            if (closed || currentMediaPlayer == null || state == PlayerState.ERROR) return;
            requestedState = PlayerState.PLAYING;
            if (ended) {
                revision++;
                ended = false;
                currentMediaPlayer.stop();
            }
            currentMediaPlayer.play();
            applyPendingSeek(currentMediaPlayer);
        });
    }

    @Override
    public void pause() {
        runOnFxThread(() -> {
            if (closed || currentMediaPlayer == null) return;
            requestedState = PlayerState.PAUSED;
            currentMediaPlayer.pause();
            if (state == PlayerState.LOADING) transitionState(PlayerState.PAUSED);
        });
    }

    @Override
    public void stop() {
        runOnFxThread(() -> {
            if (closed) return;
            revision++;
            requestedState = PlayerState.STOPPED;
            ended = false;
            pendingSeek = null;
            if (currentMediaPlayer != null) currentMediaPlayer.stop();
            position = Duration.ZERO;
            if (state != PlayerState.IDLE) transitionState(PlayerState.STOPPED);
            dispatch(listener -> listener.onPositionChanged(Duration.ZERO));
        });
    }

    @Override
    public void seek(Duration targetPosition) {
        runOnFxThread(() -> {
            if (closed || currentMediaPlayer == null || targetPosition == null
                    || duration.isZero() || state == PlayerState.LOADING || state == PlayerState.ERROR) return;
            Duration target = targetPosition.isNegative() ? Duration.ZERO : targetPosition;
            if (target.compareTo(duration) > 0) target = duration;
            if (state == PlayerState.STOPPED) {
                // JavaFX ignores seek while stopped, so apply it when playback resumes.
                pendingSeek = target;
            } else {
                ended = false;
                currentMediaPlayer.seek(javafx.util.Duration.millis(target.toMillis()));
            }
            position = target;
            Duration updatedPosition = target;
            dispatch(listener -> listener.onPositionChanged(updatedPosition));
        });
    }

    @Override
    public void setVolume(double volume) {
        if (!Double.isFinite(volume)) return;
        this.volume = Math.clamp(volume, 0.0, 1.0);
        runOnFxThread(() -> {
            if (!closed && currentMediaPlayer != null) currentMediaPlayer.setVolume(this.volume);
        });
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        runOnFxThread(() -> {
            if (!closed && currentMediaPlayer != null) currentMediaPlayer.setMute(this.muted);
        });
    }

    @Override public PlayerState getState() { return state; }
    @Override public Duration getPosition() { return position; }
    @Override public Duration getDuration() { return duration; }
    @Override public double getVolume() { return volume; }
    @Override public boolean isMuted() { return muted; }

    @Override
    public void addListener(EngineListener listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    @Override
    public void removeListener(EngineListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        runOnFxThread(() -> {
            if (closed) return;
            closed = true;
            revision++;
            listeners.clear();
            disposeCurrentPlayer();
            position = Duration.ZERO;
            duration = Duration.ZERO;
            state = PlayerState.IDLE;
        });
    }

    private void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

    private void transitionState(PlayerState newState) {
        if (state == newState) return;
        state = newState;
        dispatch(listener -> listener.onStateChanged(newState));
    }

    private void dispatch(Consumer<EngineListener> event) {
        long eventRevision = revision;
        // Ignore queued events from a player that has since been replaced or stopped.
        Platform.runLater(() -> {
            if (closed || revision != eventRevision) return;
            for (EngineListener listener : listeners) {
                event.accept(listener);
            }
        });
    }

    private void dispatchTrackInfo(Media media) {
        Object title = media.getMetadata().get("title");
        Object artist = media.getMetadata().get("artist");
        TrackInfo info = new TrackInfo(title instanceof String s && !s.isBlank() ? s.trim() : null,
                artist instanceof String s && !s.isBlank() ? s.trim() : null);
        dispatch(listener -> listener.onTrackInfoReady(info));
    }

    private void fail(String message) {
        revision++;
        ended = false;
        disposeCurrentPlayer();
        requestedState = PlayerState.STOPPED;
        pendingSeek = null;
        position = Duration.ZERO;
        duration = Duration.ZERO;
        transitionState(PlayerState.ERROR);
        dispatch(listener -> listener.onError(message));
    }

    private Duration stoppedPosition() {
        return pendingSeek == null ? Duration.ZERO : pendingSeek;
    }

    private void applyPendingSeek(MediaPlayer player) {
        if (pendingSeek != null && player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.seek(javafx.util.Duration.millis(pendingSeek.toMillis()));
            pendingSeek = null;
        }
    }

    private static Duration finiteDuration(javafx.util.Duration value) {
        if (value == null || !Double.isFinite(value.toMillis()) || value.toMillis() < 0) return Duration.ZERO;
        return Duration.ofMillis((long) value.toMillis());
    }

    private void disposeCurrentPlayer() {
        MediaPlayer player = currentMediaPlayer;
        currentMediaPlayer = null;
        if (player != null) {
            player.setAudioSpectrumListener(null);
            player.dispose();
        }
    }
}
