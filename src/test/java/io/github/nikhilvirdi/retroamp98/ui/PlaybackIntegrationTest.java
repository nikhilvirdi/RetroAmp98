package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.RetroAmpApp;
import io.github.nikhilvirdi.retroamp98.controller.PlayerController;
import io.github.nikhilvirdi.retroamp98.engine.EngineListener;
import io.github.nikhilvirdi.retroamp98.engine.JavaFxMediaEngine;
import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.SettingsManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PlaybackIntegrationTest {

    @Test
    void explicitRestartAtTheEndCancelsQueuedAutomaticCompletion(@TempDir Path directory) throws Exception {
        Path track = wave(directory.resolve("restart.wav"), 1_000);
        JavaFxMediaEngine engine = new JavaFxMediaEngine();
        var restarted = new java.util.concurrent.atomic.AtomicBoolean();
        AtomicInteger endings = new AtomicInteger();
        engine.addListener(new EngineListener() {
            @Override public void onStateChanged(PlayerState state) {
                if (state == PlayerState.STOPPED && restarted.compareAndSet(false, true)) engine.play();
            }
            @Override public void onPositionChanged(Duration position) {}
            @Override public void onDurationKnown(Duration duration) {}
            @Override public void onEndOfMedia() { endings.incrementAndGet(); }
            @Override public void onError(String message) { fail(message); }
        });
        try {
            fx(() -> { engine.setMuted(true); engine.load(track); engine.play(); });
            await(() -> restarted.get() && engine.getState() == PlayerState.PLAYING);
            fx(() -> assertEquals(0, endings.get(), "A newer play action supersedes the old automatic transition"));
        } finally { fx(engine::close); }
    }

    @Test
    void reattachingViewReloadsCurrentArtwork(@TempDir Path directory) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3, 3, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        Path track = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.taggedMp3(directory.resolve("art.mp3"), bytes.toByteArray());
        var playlist = new PlaylistManager(new io.github.nikhilvirdi.retroamp98.playlist.Playlist(List.of(new Track(track))));
        playlist.jumpTo(0);
        PlayerController controller = new PlayerController(new JavaFxMediaEngine(), playlist);
        PlayerViewController[] view = new PlayerViewController[1];
        javafx.scene.image.ImageView[] artwork = new javafx.scene.image.ImageView[1];
        try {
            fx(() -> {
                FXMLLoader loader = new FXMLLoader(RetroAmpApp.class.getResource("ui/main-view.fxml"));
                Parent root = loader.load(); new Scene(root);
                artwork[0] = (javafx.scene.image.ImageView) root.lookup("#artwork");
                view[0] = loader.getController(); view[0].attach(controller);
            });
            controller.getLibrary().submit(() -> null).get(10, TimeUnit.SECONDS);
            fx(() -> { assertNotNull(artwork[0].getImage()); view[0].attach(controller); });
            controller.getLibrary().submit(() -> null).get(10, TimeUnit.SECONDS);
            fx(() -> assertNotNull(artwork[0].getImage()));
        } finally { fx(() -> { if (view[0] != null) view[0].close(); controller.close(); }); }
    }

    @Test
    void actionIconAndNowPlayingLayoutFollowActualStateAndImageBounds() throws Exception {
        var engine = new JavaFxMediaEngine() {
            private PlayerState actualState = PlayerState.IDLE;
            @Override public PlayerState getState() { return actualState; }
        };
        PlayerController controller = new PlayerController(engine, new PlaylistManager());
        fx(() -> {
            FXMLLoader loader = new FXMLLoader(RetroAmpApp.class.getResource("ui/main-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1086, 1448);
            scene.getStylesheets().add(RetroAmpApp.class.getResource("ui/styles/main-view.css").toExternalForm());
            PlayerViewController view = loader.getController();
            try {
                view.attach(controller);
                Region icon = (Region) root.lookup("#progressState");
                for (PlayerState state : PlayerState.values()) {
                    engine.actualState = state;
                    view.onStateChanged(state == PlayerState.PLAYING ? PlayerState.PAUSED : PlayerState.PLAYING);
                    root.applyCss(); root.layout();
                    assertEquals(state != PlayerState.PLAYING, icon.getShape().contains(10, 14),
                            "Pause bars have a central gap; play triangles contain their center: " + state);
                    assertEquals(state.name().toLowerCase(java.util.Locale.ROOT), icon.getPseudoClassStates().stream()
                            .map(javafx.css.PseudoClass::getPseudoClassName).filter(name -> java.util.Arrays.stream(PlayerState.values())
                                    .anyMatch(value -> value.name().equalsIgnoreCase(name))).findFirst().orElseThrow());
                }
                assertNull(root.lookup(".rating-star")); assertNull(root.lookup(".rating-preview"));
                Label title = (Label) root.lookup("#songTitle");
                title.setText("Adventure of a Lifetime");
                var artwork = (javafx.scene.image.ImageView) root.lookup("#artwork");
                Region frame = (Region) root.lookup("#artworkFrame");
                for (int[] size : new int[][]{{640, 640}, {300, 267}}) {
                    artwork.setImage(new javafx.scene.image.WritableImage(size[0], size[1]));
                    root.applyCss(); root.layout();
                    var image = artwork.getLayoutBounds();
                    assertEquals(size[0] / (double) size[1], image.getWidth() / image.getHeight(), 0.001);
                    assertEquals(image.getWidth() + frame.getInsets().getLeft() + frame.getInsets().getRight(), frame.getWidth(), 0.01);
                    assertEquals(image.getHeight() + frame.getInsets().getTop() + frame.getInsets().getBottom(), frame.getHeight(), 0.01);
                    assertEquals(197, frame.localToScene(frame.getLayoutBounds()).getMinY(), 1);
                }
                assertTrue(title.isWrapText()); assertEquals(38, title.getFont().getSize());
                assertEquals("Adventure of a Lifetime", ((javafx.scene.text.Text) title.lookup(".text")).getText());
                assertTrue(((javafx.scene.text.Text) title.lookup(".text")).getLayoutBounds().getHeight() <= title.getHeight());
                assertEquals(688, root.lookup(".progress-band").localToScene(root.lookup(".progress-band").getLayoutBounds()).getMinY(), 1);
                assertEquals(761, root.lookup(".list-heading").localToScene(root.lookup(".list-heading").getLayoutBounds()).getMinY(), 1);
            } finally { view.close(); controller.close(); }
        });
    }

    @Test
    void realSpectrumReactsToToneAndClearsOnPauseAndStop(@TempDir Path directory) throws Exception {
        int samples = 44100 * 4;
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            short sample = (short) (Math.sin(2 * Math.PI * 440 * i / 44100) * 16000);
            pcm[i * 2] = (byte) sample; pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        Path tone = directory.resolve("tone.wav");
        try (var audio = new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(44100, 16, 1, true, false), samples)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, tone.toFile());
        }
        JavaFxMediaEngine engine = new JavaFxMediaEngine();
        AtomicInteger measured = new AtomicInteger(), wrongThread = new AtomicInteger();
        PlayerController controller = new PlayerController(engine, new PlaylistManager());
        PlayerViewController[] view = new PlayerViewController[1];
        PixelSpectrum[] spectrum = new PixelSpectrum[1];
        engine.addListener(new EngineListener() {
            public void onStateChanged(PlayerState state) {}
            public void onPositionChanged(Duration position) {}
            public void onDurationKnown(Duration duration) {}
            public void onEndOfMedia() {}
            public void onError(String message) { fail(message); }
            @Override public void onSpectrum(float[] magnitudes) {
                if (!Platform.isFxApplicationThread()) wrongThread.incrementAndGet();
                for (float magnitude : magnitudes) if (magnitude > -59) { measured.incrementAndGet(); break; }
            }
        });
        try {
            fx(() -> {
                FXMLLoader loader = new FXMLLoader(RetroAmpApp.class.getResource("ui/main-view.fxml"));
                Parent root = loader.load(); new Scene(root); view[0] = loader.getController(); view[0].attach(controller);
                spectrum[0] = (PixelSpectrum) root.lookup("#spectrum");
                engine.setMuted(true);
                controller.getPlaylistManager().addTrack(new Track(tone)); controller.play();
            });
            await(() -> measured.get() >= 3);
            assertEquals(0, wrongThread.get());
            fx(() -> assertTrue(spectrum[0].getChildren().stream().anyMatch(javafx.scene.Node::isVisible)));
            fx(controller::pause); await(() -> engine.getState() == PlayerState.PAUSED);
            fx(() -> assertTrue(spectrum[0].getChildren().stream().noneMatch(javafx.scene.Node::isVisible)));
            int pausedCount = measured.get(); Thread.sleep(150); assertEquals(pausedCount, measured.get());
            fx(controller::play); await(() -> measured.get() > pausedCount);
            fx(controller::stop); await(() -> engine.getState() == PlayerState.STOPPED);
            fx(() -> assertTrue(spectrum[0].getChildren().stream().noneMatch(javafx.scene.Node::isVisible)));
        } finally { fx(() -> { if (view[0] != null) view[0].close(); controller.close(); }); }
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // Other media tests can share the JavaFX runtime.
        } catch (UnsupportedOperationException unavailable) {
            assumeTrue(false, "JavaFX display unavailable: " + unavailable.getMessage());
        }
        fx(() -> Platform.setImplicitExit(false));
    }

    @Test
    void footerAndSeekingControlRealPlayback(@TempDir Path directory) throws Exception {
        Path first = wave(directory.resolve("First.wav"), 10_000);
        Path second = wave(directory.resolve("Second.wav"), 10_000);
        RecordingEngine engine = new RecordingEngine();
        PlayerController controller = new PlayerController(engine, new PlaylistManager());
        PlayerViewController[] view = new PlayerViewController[1];
        Parent[] root = new Parent[1];
        try {
            fx(() -> {
                SettingsManager settings = new SettingsManager(directory.resolve("settings.properties"));
                settings.setVolume(0.35);
                settings.setMuted(true);
                settings.setRepeatMode(RepeatMode.ALL);
                settings.setShuffle(true);
                settings.save();
                controller.restoreSettings(settings);
                FXMLLoader loader = new FXMLLoader(RetroAmpApp.class.getResource("ui/main-view.fxml"));
                root[0] = loader.load();
                Scene scene = new Scene(root[0], 1086, 1448);
                scene.getStylesheets().add(RetroAmpApp.class.getResource("ui/styles/main-view.css").toExternalForm());
                view[0] = loader.getController();
                view[0].attach(controller);
                scene.snapshot(null);
                assertEquals(0, ((Region) root[0].lookup("#progressFill")).getWidth());
                assertTrue(((ListView<?>) root[0].lookup(".recent-tracks")).getItems().isEmpty());
                assertTrue(button(root[0], "muteButton").getPseudoClassStates().stream()
                        .anyMatch(state -> state.getPseudoClassName().equals("active")));
                assertEquals(0.35, engine.getVolume(), 0.001);
                controller.setShuffle(false);
                controller.setRepeatMode(RepeatMode.OFF);
                controller.openFiles(List.of(first, second));
            });
            await(() -> engine.getState() == PlayerState.PLAYING && engine.getPosition().toMillis() > 200);
            fx(() -> {
                assertEquals("First", ((Label) root[0].lookup("#songTitle")).getText());
                ListView<?> tracks = (ListView<?>) root[0].lookup("#recentTracks");
                assertEquals(2, tracks.getItems().size());
                button(root[0], "viewSwitch").fire();
                assertEquals("RECENTLY PLAYED", button(root[0], "viewSwitch").getText());
                assertEquals(1, tracks.getItems().size());
                button(root[0], "viewSwitch").fire();
                assertEquals(2, tracks.getItems().size());
                assertEquals("Unknown Artist", ((Label) root[0].lookup("#artist")).getText());
                assertEquals("", ((Label) root[0].lookup("#trackNumber")).getText());
                button(root[0], "pauseButton").fire();
            });
            await(() -> engine.getState() == PlayerState.PAUSED);
            Duration pausedPosition = engine.getPosition();
            Thread.sleep(200);
            assertTrue(Math.abs(engine.getPosition().toMillis() - pausedPosition.toMillis()) < 100);
            fx(() -> button(root[0], "playButton").fire());
            await(() -> engine.getPosition().toMillis() > pausedPosition.toMillis() + 100);
            assertEquals(1, engine.loads.get());
            fx(() -> button(root[0], "playButton").fire());
            assertEquals(1, engine.loads.get());
            fx(() -> {
                Slider slider = (Slider) root[0].lookup("#seekSlider");
                assertFalse(slider.isDisabled());
                assertEquals(0, engine.seeks.get(), "Progress callbacks must not seek");
                view[0].onPositionChanged(Duration.ofHours(3), Duration.ofHours(4));
                assertEquals("180:00", ((Label) root[0].lookup("#elapsedTime")).getText());
                assertEquals("240:00", ((Label) root[0].lookup("#totalTime")).getText());
                slider.setValueChanging(true);
                view[0].onDurationKnown(Duration.ZERO);
                assertTrue(slider.isDisabled());
                assertFalse(slider.isValueChanging());
                assertEquals(0, engine.seeks.get());
                view[0].onPositionChanged(engine.getPosition(), engine.getDuration());
                slider.setValueChanging(true);
                slider.setValue(0.5);
                root[0].applyCss();
                root[0].layout();
                assertEquals((slider.getWidth() - 13) * 0.5,
                        ((Region) root[0].lookup("#progressFill")).getWidth(), 1);
                assertEquals(0, engine.seeks.get(), "Dragging previews until release");
                slider.setValueChanging(false);
                assertEquals(1, engine.seeks.get());
            });
            await(() -> engine.getPosition().toMillis() >= 5_000);
            fx(() -> {
                button(root[0], "stopButton").fire();
                assertEquals(Duration.ZERO, engine.getPosition());
            });
            await(() -> engine.getState() == PlayerState.STOPPED);
            fx(() -> {
                Slider slider = (Slider) root[0].lookup("#seekSlider");
                slider.setValue(0.2);
                button(root[0], "playButton").fire();
            });
            await(() -> engine.getState() == PlayerState.PLAYING && engine.getPosition().toMillis() > 2_150);
            fx(() -> {
                double volume = engine.getVolume();
                button(root[0], "volumeDownButton").fire();
                assertEquals(volume - 0.05, engine.getVolume(), 0.001);
                button(root[0], "volumeUpButton").fire();
                assertEquals(volume, engine.getVolume(), 0.001);
                button(root[0], "muteButton").fire();
                assertFalse(engine.isMuted());
                button(root[0], "muteButton").fire();
                assertTrue(engine.isMuted());
                assertEquals(volume, engine.getVolume(), 0.001);
                button(root[0], "shuffleButton").fire();
                assertTrue(controller.getPlaylistManager().isShuffle());
                button(root[0], "shuffleButton").fire();
                button(root[0], "repeatButton").fire();
                assertEquals(RepeatMode.ONE, controller.getPlaylistManager().getRepeatMode());
                button(root[0], "repeatButton").fire();
                assertEquals(RepeatMode.ALL, controller.getPlaylistManager().getRepeatMode());
                button(root[0], "repeatButton").fire();
                assertEquals(RepeatMode.OFF, controller.getPlaylistManager().getRepeatMode());
                Slider slider = (Slider) root[0].lookup("#seekSlider");
                int seeksBeforeTrackChange = engine.seeks.get();
                slider.setValueChanging(true);
                slider.setValue(0.8);
                button(root[0], "nextButton").fire();
                assertFalse(slider.isValueChanging());
                assertEquals(seeksBeforeTrackChange, engine.seeks.get(), "Track changes cancel an old drag");
            });
            await(() -> engine.getState() == PlayerState.PLAYING && engine.getPosition().toMillis() > 50);
            fx(() -> {
                assertEquals(second, controller.getPlaylistManager().current().getPath());
                button(root[0], "previousButton").fire();
            });
            await(() -> engine.getState() == PlayerState.PLAYING);
            fx(() -> assertEquals(first, controller.getPlaylistManager().current().getPath()));
            fx(() -> {
                ListView<?> tracks = (ListView<?>) root[0].lookup("#recentTracks");
                tracks.getSelectionModel().select(1);
                root[0].applyCss(); root[0].layout();
                var cell = tracks.lookupAll(".list-cell").stream()
                        .filter(node -> node instanceof javafx.scene.control.ListCell<?> c && c.getIndex() == 1)
                        .findFirst().orElseThrow();
                javafx.event.Event.fireEvent(cell, new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                        javafx.scene.input.MouseButton.PRIMARY, 2, false, false, false, false,
                        false, false, false, false, false, false, null));
            });
            await(() -> engine.getState() == PlayerState.PLAYING && controller.getPlaylistManager().current().getPath().equals(second));
            fx(() -> {
                button(root[0], "viewSwitch").fire();
                ListView<?> tracks = (ListView<?>) root[0].lookup("#recentTracks");
                tracks.getSelectionModel().select(1);
                tracks.getContextMenu().getItems().stream().filter(item -> "Play selected".equals(item.getText()))
                        .findFirst().orElseThrow().fire();
            });
            await(() -> engine.getState() == PlayerState.PLAYING && controller.getPlaylistManager().current().getPath().equals(first));
        } finally {
            fx(() -> {
                if (view[0] != null) view[0].close();
                controller.close();
            });
        }
    }

    @Test
    void rapidLoadsErrorsAndCloseCannotReviveAnOldPlayer(@TempDir Path directory) throws Exception {
        Path first = wave(directory.resolve("first.wav"), 2_000);
        Path second = wave(directory.resolve("second.wav"), 3_000);
        JavaFxMediaEngine engine = new JavaFxMediaEngine();
        List<PlayerState> events = new CopyOnWriteArrayList<>();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger wrongThread = new AtomicInteger();
        engine.addListener(new EngineListener() {
            private void checkThread() { if (!Platform.isFxApplicationThread()) wrongThread.incrementAndGet(); }
            @Override public void onStateChanged(PlayerState state) { checkThread(); events.add(state); }
            @Override public void onPositionChanged(Duration value) { checkThread(); }
            @Override public void onDurationKnown(Duration value) { checkThread(); }
            @Override public void onEndOfMedia() { checkThread(); }
            @Override public void onError(String message) { checkThread(); errors.incrementAndGet(); }
        });
        try {
            fx(() -> {
                engine.setMuted(true);
                engine.load(first);
                engine.play();
                engine.load(second);
                engine.play();
                engine.stop();
            });
            await(() -> engine.getDuration().toMillis() == 3_000);
            assertEquals(PlayerState.STOPPED, engine.getState());
            assertEquals(Duration.ZERO, engine.getPosition());
            assertFalse(events.contains(PlayerState.PLAYING));
            fx(engine::play);
            await(() -> engine.getState() == PlayerState.PLAYING);
            fx(() -> engine.load(directory.resolve("missing.wav")));
            await(() -> errors.get() == 1);
            assertEquals(PlayerState.ERROR, engine.getState());
            assertEquals(Duration.ZERO, engine.getDuration());
            Path invalid = Files.writeString(directory.resolve("invalid.mp3"), "invalid audio");
            fx(() -> engine.load(invalid));
            await(() -> errors.get() == 2);
            assertEquals(PlayerState.ERROR, engine.getState());
            fx(() -> {
                engine.setVolume(-2);
                assertEquals(0, engine.getVolume());
                engine.setVolume(3);
                assertEquals(1, engine.getVolume());
                engine.setVolume(Double.NaN);
                assertEquals(1, engine.getVolume());
                engine.load(first);
                engine.play();
                engine.close();
            });
            Thread.sleep(200);
            assertEquals(PlayerState.IDLE, engine.getState());
            assertEquals(Duration.ZERO, engine.getPosition());
            assertEquals(0, wrongThread.get());
            // JavaFX 21's empty ID3 parser retains its file channel until collection.
            System.gc();
            await(() -> {
                try {
                    Files.deleteIfExists(invalid);
                    return true;
                } catch (java.io.IOException stillOpen) {
                    return false;
                }
            });
        } finally {
            fx(engine::close);
        }
    }

    @Test
    void completionUsesExistingRepeatAndPlaylistOrder(@TempDir Path directory) throws Exception {
        Path first = wave(directory.resolve("first.wav"), 500);
        Path second = wave(directory.resolve("second.wav"), 500);
        JavaFxMediaEngine engine = new JavaFxMediaEngine();
        PlaylistManager playlist = new PlaylistManager();
        PlayerController controller = new PlayerController(engine, playlist);
        AtomicInteger endings = new AtomicInteger();
        engine.addListener(new EngineListener() {
            @Override public void onStateChanged(PlayerState state) {}
            @Override public void onPositionChanged(Duration value) {}
            @Override public void onDurationKnown(Duration value) {}
            @Override public void onError(String message) { fail(message); }
            @Override public void onEndOfMedia() { endings.incrementAndGet(); }
        });
        try {
            fx(() -> {
                engine.setMuted(true);
                playlist.addTrack(new Track(first));
                playlist.addTrack(new Track(second));
                controller.play();
            });
            await(() -> endings.get() >= 2 && engine.getState() == PlayerState.STOPPED);
            fx(() -> {
                assertEquals(1, playlist.getCurrentIndex());
                controller.setRepeatMode(RepeatMode.ONE);
                controller.playTrack(0);
            });
            await(() -> endings.get() >= 4);
            fx(() -> {
                assertEquals(0, playlist.getCurrentIndex());
                controller.stop();
                controller.setRepeatMode(RepeatMode.ALL);
                controller.playTrack(1);
            });
            await(() -> endings.get() >= 5);
            fx(() -> assertEquals(0, playlist.getCurrentIndex()));
        } finally {
            fx(controller::close);
        }
    }

    private static Button button(Parent root, String id) {
        return (Button) root.lookup("#" + id);
    }

    private static Path wave(Path path, int milliseconds) throws Exception {
        int samples = 16 * milliseconds;
        byte[] pcm = new byte[samples * 2];
        AudioFormat format = new AudioFormat(16_000, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(pcm), format, samples)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, path.toFile());
        }
        return path;
    }

    private static void fx(Checked action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertTrue(condition.getAsBoolean(), "Timed out waiting for media state");
    }

    private interface Checked { void run() throws Exception; }

    private static final class RecordingEngine extends JavaFxMediaEngine {
        private final AtomicInteger loads = new AtomicInteger();
        private final AtomicInteger seeks = new AtomicInteger();
        @Override public void load(Path path) { loads.incrementAndGet(); super.load(path); }
        @Override public void seek(Duration position) { seeks.incrementAndGet(); super.seek(position); }
    }
}
