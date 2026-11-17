package io.github.nikhilvirdi.retroamp98.controller;

import io.github.nikhilvirdi.retroamp98.engine.EngineListener;
import io.github.nikhilvirdi.retroamp98.engine.MediaEngine;
import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerControllerTest {

    @Test
    void restoresUnsavedSessionOrderWithoutPlaybackOrHistory(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path first = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("first.wav"));
        Path second = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("second.wav"));
        var settings = new io.github.nikhilvirdi.retroamp98.util.SettingsManager(directory.resolve("settings.properties"));
        settings.setSession(List.of(new Track(second), new Track(directory.resolve("missing.mp3")), new Track(first), new Track(first)), new Track(first));
        settings.setVolume(0.25); settings.setMuted(true); settings.setShuffle(true); settings.setRepeatMode(RepeatMode.ALL);
        TestEngine engine = new TestEngine();
        try (settings; PlayerController player = new PlayerController(engine, new PlaylistManager())) {
            player.applySettings(settings).get();
            assertEquals(List.of(second, first), player.getPlaylistManager().getPlaylist().getTracks().stream().map(Track::getPath).toList());
            assertEquals(first, player.getPlaylistManager().current().getPath());
            assertTrue(engine.loads.isEmpty()); assertTrue(player.getHistory().isEmpty());
            assertEquals(0.25, engine.volume); assertTrue(engine.muted); assertTrue(player.getPlaylistManager().isShuffle());
            assertEquals(RepeatMode.ALL, player.getPlaylistManager().getRepeatMode());
            player.clearPlaylist(); player.updateSettings(settings); settings.save(); settings.load();
            assertTrue(settings.hasSession()); assertTrue(settings.getSessionTracks().isEmpty());
        }
    }

    @Test
    void importsDeduplicateAndEditingKeepsTheActiveTrack(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path first = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("first.wav"));
        Path second = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("second.wav"));
        TestEngine engine = new TestEngine();
        PlaylistManager playlist = new PlaylistManager();
        try (PlayerController player = new PlayerController(engine, playlist)) {
            player.openFiles(List.of(first, first)).get();
            player.openFolder(directory).get();
            assertEquals(2, playlist.getPlaylist().size());
            int loads = engine.loads.size();
            player.moveTrack(0, 1);
            assertEquals(first, playlist.current().getPath());
            assertEquals(1, playlist.getCurrentIndex());
            assertEquals(loads, engine.loads.size());
            player.removeTrack(0);
            assertEquals(first, playlist.current().getPath());
            assertEquals(loads, engine.loads.size());
            player.removeTrack(0);
            assertTrue(playlist.getPlaylist().isEmpty());
            assertEquals(PlayerState.STOPPED, engine.state);
            player.openFiles(List.of(first, second)).get();
            Path m3u = directory.resolve("saved.m3u");
            player.savePlaylist(m3u).get();
            player.clearPlaylist();
            player.openPlaylist(m3u).get();
            assertEquals(2, playlist.getPlaylist().size());
            assertEquals(first, playlist.current().getPath());
            java.nio.file.Files.writeString(m3u, "#EXTM3U\n#EXTINF:1,Custom M3U title\nfirst.wav\n");
            player.openPlaylist(m3u).get();
            assertEquals("Custom M3U title", playlist.current().getTitle());
        }
    }

    @Test
    void historyRecordsActualStartsOnceAndRestoresAndReplays(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path first = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("first.wav"));
        Path second = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("second.wav"));
        Path historyPath = directory.resolve("history.properties");
        TestEngine engine = new TestEngine();
        try (PlayerController player = new PlayerController(engine, new PlaylistManager(),
                new io.github.nikhilvirdi.retroamp98.library.LibraryService(),
                new io.github.nikhilvirdi.retroamp98.library.ListeningHistory(historyPath), Runnable::run)) {
            player.openFiles(List.of(first, second)).get();
            assertTrue(player.getHistory().isEmpty(), "Import/load requests do not count as playback");
            player.onStateChanged(PlayerState.PLAYING);
            player.onStateChanged(PlayerState.PLAYING);
            player.pause(); player.play(); player.onStateChanged(PlayerState.PLAYING);
            assertEquals(1, player.getHistory().size());
            player.next(); player.onStateChanged(PlayerState.PLAYING);
            assertEquals(List.of(second, first), player.getHistory().stream().map(Track::getPath).toList());
            player.playTrack(0); player.onStateChanged(PlayerState.PLAYING);
            assertEquals(List.of(first, second), player.getHistory().stream().map(Track::getPath).toList());
        }
        // Closing drains the debounced history writer without blocking the application thread.
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (java.nio.file.Files.exists(historyPath)) {
                try (var store = new io.github.nikhilvirdi.retroamp98.library.ListeningHistory(historyPath)) {
                    if (store.load().size() == 2) break;
                }
            }
            Thread.sleep(10);
        }
        try (PlayerController player = new PlayerController(new TestEngine(), new PlaylistManager(),
                new io.github.nikhilvirdi.retroamp98.library.LibraryService(),
                new io.github.nikhilvirdi.retroamp98.library.ListeningHistory(historyPath), Runnable::run)) {
            player.restoreHistory().get();
            assertEquals(List.of(first, second), player.getHistory().stream().map(Track::getPath).toList());
            player.replayHistory(player.getHistory().getLast()).get();
            assertEquals(second, player.getPlaylistManager().current().getPath());
            java.nio.file.Files.delete(first);
            player.replayHistory(player.getHistory().getFirst()).get();
            assertEquals(second, player.getPlaylistManager().current().getPath(), "Missing history entries must not replace playback");
            player.clearHistory();
            assertTrue(player.getHistory().isEmpty());
        }
    }

    @Test
    void clearingPlaylistDiscardsPendingImport(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path first = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("first.wav"));
        var release = new java.util.concurrent.CountDownLatch(1);
        var library = new io.github.nikhilvirdi.retroamp98.library.LibraryService();
        try (PlayerController player = new PlayerController(new TestEngine(), new PlaylistManager(), library,
                new io.github.nikhilvirdi.retroamp98.library.ListeningHistory(null), Runnable::run)) {
            library.submit(() -> { release.await(); return null; });
            var imported = player.openFiles(List.of(first));
            player.clearPlaylist();
            release.countDown();
            imported.get();
            assertTrue(player.getPlaylistManager().getPlaylist().isEmpty());
            var releaseReplay = new java.util.concurrent.CountDownLatch(1);
            library.submit(() -> { releaseReplay.await(); return null; });
            var replay = player.replayHistory(new Track(first));
            player.stop();
            releaseReplay.countDown();
            replay.get();
            assertTrue(player.getPlaylistManager().getPlaylist().isEmpty(), "A newer stop cancels a pending replay");
        } finally { release.countDown(); }
    }

    @Test
    void repeatedPlayAndResumeRetainTheLoadedTrack() {
        TestEngine engine = new TestEngine();
        PlaylistManager playlist = new PlaylistManager();
        playlist.addTrack(new Track(Path.of("first.wav")));
        try (PlayerController player = new PlayerController(engine, playlist)) {
            player.play();
            engine.position = Duration.ofSeconds(2);
            player.play();
            player.pause();
            player.play();
            assertEquals(1, engine.loads.size());
            assertEquals(Duration.ofSeconds(2), engine.position);
            assertEquals(PlayerState.PLAYING, engine.state);
            player.stop();
            player.play();
            assertEquals(1, engine.loads.size());
        }
        assertTrue(engine.closed);
        assertTrue(engine.listeners.isEmpty());
    }

    @Test
    void playDuringLoadingDoesNotReloadAndNavigationLoadsTheSelectedTrack() {
        TestEngine engine = new TestEngine();
        PlaylistManager playlist = new PlaylistManager();
        playlist.addTrack(new Track(Path.of("first.wav")));
        playlist.addTrack(new Track(Path.of("second.wav")));
        try (PlayerController player = new PlayerController(engine, playlist)) {
            player.play();
            engine.state = PlayerState.LOADING;
            player.play();
            assertEquals(1, engine.loads.size());
            player.next();
            assertEquals(Path.of("second.wav"), engine.loads.getLast());
            player.previous();
            assertEquals(Path.of("first.wav"), engine.loads.getLast());
        }
    }

    @Test
    void openingFilesRecoversFromErrorAndSelectsTheNewFiles(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        TestEngine engine = new TestEngine();
        PlaylistManager playlist = new PlaylistManager();
        playlist.addTrack(new Track(Path.of("missing.wav")));
        playlist.jumpTo(0);
        engine.state = PlayerState.ERROR;
        try (PlayerController player = new PlayerController(engine, playlist)) {
            Path replacement = io.github.nikhilvirdi.retroamp98.library.AudioFixtures.wave(directory.resolve("replacement.wav"));
            player.openFiles(List.of(replacement)).get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1, playlist.getCurrentIndex());
            assertEquals(replacement, engine.loads.getLast());
        }
    }

    @Test
    void emptyPlaybackAndModeControlsAreSafe() {
        TestEngine engine = new TestEngine();
        try (PlayerController player = new PlayerController(engine, new PlaylistManager())) {
            player.play();
            player.pause();
            player.next();
            player.previous();
            assertTrue(engine.loads.isEmpty());
            player.setVolume(0.65);
            player.toggleMute();
            player.toggleMute();
            assertFalse(engine.muted);
            assertEquals(0.65, engine.volume);
            player.cycleRepeatMode();
            assertEquals(RepeatMode.ONE, player.getPlaylistManager().getRepeatMode());
            player.cycleRepeatMode();
            assertEquals(RepeatMode.ALL, player.getPlaylistManager().getRepeatMode());
            player.cycleRepeatMode();
            assertEquals(RepeatMode.OFF, player.getPlaylistManager().getRepeatMode());
            player.toggleShuffle();
            assertTrue(player.getPlaylistManager().isShuffle());
        }
    }

    public static final class TestEngine implements MediaEngine {
        public final List<Path> loads = new ArrayList<>();
        public final List<EngineListener> listeners = new ArrayList<>();
        public PlayerState state = PlayerState.IDLE;
        public Duration position = Duration.ZERO;
        public Duration duration = Duration.ofSeconds(10);
        public double volume = 0.8;
        public boolean muted;
        public boolean closed;
        public int seeks;

        @Override public void load(Path path) { loads.add(path); position = Duration.ZERO; state = PlayerState.LOADING; }
        @Override public void play() { state = PlayerState.PLAYING; }
        @Override public void pause() { state = PlayerState.PAUSED; }
        @Override public void stop() { state = PlayerState.STOPPED; position = Duration.ZERO; }
        @Override public void seek(Duration target) { position = target; seeks++; }
        @Override public void setVolume(double value) { volume = Math.clamp(value, 0, 1); }
        @Override public void setMuted(boolean value) { muted = value; }
        @Override public PlayerState getState() { return state; }
        @Override public Duration getPosition() { return position; }
        @Override public Duration getDuration() { return duration; }
        @Override public double getVolume() { return volume; }
        @Override public boolean isMuted() { return muted; }
        @Override public void addListener(EngineListener listener) { listeners.add(listener); }
        @Override public void removeListener(EngineListener listener) { listeners.remove(listener); }
        @Override public void close() { closed = true; }
    }
}
