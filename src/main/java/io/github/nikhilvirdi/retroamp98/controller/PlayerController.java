package io.github.nikhilvirdi.retroamp98.controller;

import io.github.nikhilvirdi.retroamp98.controller.ControllerListener.NoticeLevel;
import io.github.nikhilvirdi.retroamp98.engine.EngineListener;
import io.github.nikhilvirdi.retroamp98.engine.MediaEngine;
import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.M3uSupport;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistListener;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.SettingsManager;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import io.github.nikhilvirdi.retroamp98.library.LibraryService;
import io.github.nikhilvirdi.retroamp98.library.ListeningHistory;
import io.github.nikhilvirdi.retroamp98.playlist.Playlist;

public class PlayerController implements EngineListener, PlaylistListener, AutoCloseable {

    private final MediaEngine mediaEngine;
    private final PlaylistManager playlistManager;
    private final List<ControllerListener> listeners = new CopyOnWriteArrayList<>();
    private Track loadedTrack;
    private final LibraryService library;
    private final ListeningHistory historyStore;
    private final Executor delivery;
    private final java.util.ArrayList<Track> history = new java.util.ArrayList<>();
    private long libraryRevision;
    private long historyRevision;
    private long playbackRevision;
    private boolean recordedPlayback;
    private volatile boolean closed;
    private String lastPlaylistPath;
    private boolean restoringSession;

    public PlayerController(MediaEngine mediaEngine, PlaylistManager playlistManager) {
        this(mediaEngine, playlistManager, new LibraryService(), new ListeningHistory(null), Runnable::run);
    }

    public PlayerController(MediaEngine mediaEngine, PlaylistManager playlistManager, LibraryService library,
                            ListeningHistory historyStore, Executor delivery) {
        this.mediaEngine = Objects.requireNonNull(mediaEngine, "mediaEngine cannot be null");
        this.playlistManager = Objects.requireNonNull(playlistManager, "playlistManager cannot be null");
        this.library = library;
        this.historyStore = historyStore;
        this.delivery = delivery;

        this.mediaEngine.addListener(this);
        this.playlistManager.addListener(this);
    }

    public void restoreSettings(SettingsManager settings) {
        settings.load();
        applySettings(settings);
    }

    /** Applies a settings snapshot already loaded by the application launcher. */
    public CompletableFuture<Void> applySettings(SettingsManager settings) {
        restoringSession = true;
        setVolume(settings.getVolume());
        setMuted(settings.isMuted());
        setRepeatMode(settings.getRepeatMode());
        setShuffle(settings.isShuffle());

        String lastPlaylist = settings.getLastPlaylistPath();
        lastPlaylistPath = lastPlaylist;
        CompletableFuture<Void> restored = CompletableFuture.completedFuture(null);
        if (settings.hasSession()) {
            long revision = ++libraryRevision;
            List<Track> saved = settings.getSessionTracks();
            restored = background(() -> library.readFiles(saved.stream().map(Track::getPath).toList()), result -> {
                if (revision != libraryRevision) return;
                var titles = saved.stream().collect(java.util.stream.Collectors.toMap(
                        t -> t.getPath().toAbsolutePath().normalize(), Track::getTitle, (a, b) -> a));
                List<Track> tracks = result.tracks().stream().map(t -> t.withFallbackTitle(titles.get(t.getPath()))).toList();
                playlistManager.setPlaylist(new Playlist(tracks));
                for (int i = 0; i < tracks.size(); i++) {
                    if (tracks.get(i).getPath().toString().equalsIgnoreCase(settings.getCurrentTrackPath())) {
                        playlistManager.jumpTo(i);
                        break;
                    }
                }
                if (result.skipped() > 0) notifyNotice(NoticeLevel.INFO, "Session restored", result.skipped() + " unavailable tracks skipped.");
            });
        } else if (lastPlaylist != null && !lastPlaylist.isBlank()) {
            try {
                restored = importPlaylist(Path.of(lastPlaylist), false);
            } catch (InvalidPathException ignored) {
                // A stale playlist path should not prevent startup.
            }
        }
        return restored.whenComplete((ignored, error) -> {
            restoringSession = false;
            if (!closed) notifyPlaylistUpdated();
        });
    }

    public void saveSettings(SettingsManager settings) {
        updateSettings(settings);
        settings.save();
    }

    public void updateSettings(SettingsManager settings) {
        settings.setVolume(mediaEngine.getVolume());
        settings.setMuted(mediaEngine.isMuted());
        settings.setRepeatMode(playlistManager.getRepeatMode());
        settings.setShuffle(playlistManager.isShuffle());
        if (lastPlaylistPath != null) settings.setLastPlaylistPath(lastPlaylistPath);
        if (!restoringSession) settings.setSession(playlistManager.getPlaylist().getTracks(), playlistManager.current());
    }

    public MediaEngine getMediaEngine() {
        return mediaEngine;
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public LibraryService getLibrary() { return library; }
    public List<Track> getHistory() { return List.copyOf(history); }

    public CompletableFuture<Void> restoreHistory() {
        long revision = historyRevision;
        return background(historyStore::load, tracks -> {
            if (revision != historyRevision) return;
            for (Track track : tracks) {
                if (history.stream().noneMatch(t -> t.getPath().equals(track.getPath()))) history.add(track);
                if (history.size() >= ListeningHistory.LIMIT) break;
            }
            notifyHistoryUpdated();
            if (!history.isEmpty()) persistHistory();
        });
    }

    public void clearHistory() {
        historyRevision++;
        history.clear();
        persistHistory();
        notifyHistoryUpdated();
    }

    public CompletableFuture<Void> replayHistory(Track track) {
        long revision = libraryRevision;
        long playRevision = ++playbackRevision;
        return background(() -> library.metadata().read(track.getPath()), fresh -> {
            if (revision != libraryRevision || playRevision != playbackRevision) return;
            int index = indexOfPath(fresh.getPath());
            if (index < 0) {
                index = playlistManager.getPlaylist().size();
                playlistManager.addTrack(fresh);
            }
            playTrack(index);
        });
    }

    private int indexOfPath(Path path) {
        List<Track> tracks = playlistManager.getPlaylist().getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getPath().toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize())) return i;
        }
        return -1;
    }

    public void addListener(ControllerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ControllerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void playPause() {
        if (mediaEngine.getState() == PlayerState.PLAYING) {
            mediaEngine.pause();
        } else {
            play();
        }
    }

    public void play() {
        playbackRevision++;
        PlayerState state = mediaEngine.getState();
        Track track = playlistManager.current();
        if (track == null && playlistManager.hasNext()) {
            track = playlistManager.next();
        }
        if (track == null) return;
        if (track.equals(loadedTrack) && state != PlayerState.ERROR && state != PlayerState.IDLE) {
            if (state != PlayerState.PLAYING) mediaEngine.play();
            return;
        }
        loadAndPlay(track);
    }

    public void pause() {
        playbackRevision++;
        mediaEngine.pause();
    }

    public void stop() {
        playbackRevision++;
        recordedPlayback = false;
        if (mediaEngine.getState() == PlayerState.ERROR) loadedTrack = null;
        mediaEngine.stop();
    }

    public void next() {
        playbackRevision++;
        Track nextTrack = playlistManager.next();
        if (nextTrack != null) {
            loadAndPlay(nextTrack);
        } else {
            mediaEngine.stop();
        }
    }

    public void previous() {
        playbackRevision++;
        if (mediaEngine.getPosition().compareTo(Duration.ofSeconds(3)) > 0) {
            mediaEngine.seek(Duration.ZERO);
            return;
        }

        Track prevTrack = playlistManager.previous();
        if (prevTrack != null) {
            loadAndPlay(prevTrack);
        } else {
            mediaEngine.stop();
        }
    }

    public void seek(Duration position) {
        mediaEngine.seek(position);
    }

    public void skip(int seconds) {
        Duration current = mediaEngine.getPosition();
        Duration total = mediaEngine.getDuration();
        long newSec = current.toSeconds() + seconds;
        if (newSec < 0) newSec = 0;
        if (total != null && !total.isZero() && newSec > total.toSeconds()) {
            newSec = total.toSeconds();
        }
        mediaEngine.seek(Duration.ofSeconds(newSec));
    }

    public void playTrack(int index) {
        playbackRevision++;
        Track track = playlistManager.jumpTo(index);
        if (track != null) {
            loadAndPlay(track);
        }
    }

    public void setVolume(double volume) {
        mediaEngine.setVolume(volume);
        notifyVolumeChanged(mediaEngine.getVolume(), mediaEngine.isMuted());
    }

    public void setMuted(boolean muted) {
        mediaEngine.setMuted(muted);
        notifyVolumeChanged(mediaEngine.getVolume(), mediaEngine.isMuted());
    }

    public void toggleMute() {
        setMuted(!mediaEngine.isMuted());
    }

    public void setRepeatMode(RepeatMode mode) {
        playlistManager.setRepeatMode(mode);
        notifyRepeatModeChanged(mode);
    }

    public void cycleRepeatMode() {
        RepeatMode current = playlistManager.getRepeatMode();
        RepeatMode next = switch (current) {
            case OFF -> RepeatMode.ONE;
            case ONE -> RepeatMode.ALL;
            case ALL -> RepeatMode.OFF;
        };
        setRepeatMode(next);
    }

    public void setShuffle(boolean shuffle) {
        playlistManager.setShuffle(shuffle);
        notifyShuffleChanged(shuffle);
    }

    public void toggleShuffle() {
        setShuffle(!playlistManager.isShuffle());
    }

    public CompletableFuture<Void> openFiles(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return CompletableFuture.completedFuture(null);
        long revision = libraryRevision;
        List<Path> snapshot = List.copyOf(paths);
        return background(() -> library.readPaths(snapshot), result -> {
            if (revision == libraryRevision) appendImport(result);
        });
    }

    public CompletableFuture<Void> openFolder(Path folder) {
        return folder == null ? CompletableFuture.completedFuture(null) : openFiles(List.of(folder));
    }

    private void appendImport(LibraryService.ImportResult result) {
        var existing = new java.util.HashSet<Path>();
        for (Track track : playlistManager.getPlaylist().getTracks()) existing.add(track.getPath().toAbsolutePath().normalize());
        List<Track> added = result.tracks().stream().filter(t -> existing.add(t.getPath().toAbsolutePath().normalize())).toList();
        int first = playlistManager.getPlaylist().size();
        playlistManager.addTracks(added);
        PlayerState state = mediaEngine.getState();
        if (!added.isEmpty() && (first == 0 || state == PlayerState.IDLE || state == PlayerState.STOPPED || state == PlayerState.ERROR)) playTrack(first);
        if (result.skipped() > 0 || added.isEmpty()) notifyNotice(NoticeLevel.INFO, "Import",
                added.size() + " tracks added. " + result.skipped() + " unreadable or unsupported files skipped. Existing tracks are kept once.");
    }

    public CompletableFuture<Void> openPlaylist(Path path) { return importPlaylist(path, true); }

    private CompletableFuture<Void> importPlaylist(Path path, boolean autoPlay) {
        long revision = ++libraryRevision;
        return background(() -> {
            var m3u = M3uSupport.loadWithReport(path);
            var result = library.readFiles(m3u.playlist().getTracks().stream().map(Track::getPath).toList());
            var titles = m3u.playlist().getTracks().stream().collect(java.util.stream.Collectors.toMap(Track::getPath, Track::getTitle));
            var tracks = result.tracks().stream().map(t -> t.withFallbackTitle(titles.get(t.getPath()))).toList();
            return new LibraryService.ImportResult(tracks, result.skipped() + m3u.missingFiles().size());
        }, result -> {
            if (revision != libraryRevision) return;
            mediaEngine.stop();
            loadedTrack = null;
            recordedPlayback = false;
            playlistManager.setPlaylist(new Playlist(result.tracks()));
            lastPlaylistPath = path.toAbsolutePath().toString();
            if (autoPlay && !result.tracks().isEmpty()) playTrack(0);
            if (result.skipped() > 0) notifyNotice(NoticeLevel.WARNING, "Playlist", result.skipped() + " missing or invalid entries skipped.");
        });
    }

    public CompletableFuture<Void> savePlaylist(Path path) {
        Playlist snapshot = new Playlist(playlistManager.getPlaylist().getTracks());
        return background(() -> { M3uSupport.save(snapshot, path); return path; }, saved -> {
            lastPlaylistPath = saved.toAbsolutePath().toString();
            notifyNotice(NoticeLevel.INFO, "Playlist saved", saved.getFileName().toString());
        });
    }

    public void moveTrack(int from, int to) { playlistManager.moveTrack(from, to); }

    private <T> CompletableFuture<Void> background(Callable<T> work, Consumer<T> apply) {
        return library.submit(work).handleAsync((result, error) -> {
            if (closed) return null;
            if (error == null) apply.accept(result);
            else notifyNotice(NoticeLevel.WARNING, "Library", error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
            return null;
        }, delivery);
    }

    public void removeTrack(int index) {
        boolean wasCurrent = (index == playlistManager.getCurrentIndex());
        playlistManager.removeTrack(index);

        if (wasCurrent) {
            Track newCurrent = playlistManager.current();
            if (newCurrent != null) {
                loadAndPlay(newCurrent);
            } else {
                mediaEngine.stop();
            }
        }
    }

    public void clearPlaylist() {
        playbackRevision++;
        libraryRevision++;
        loadedTrack = null;
        recordedPlayback = false;
        mediaEngine.stop();
        playlistManager.clear();
    }

    @Override
    public void onStateChanged(PlayerState state) {
        if (state == PlayerState.PLAYING && mediaEngine.getState() == PlayerState.PLAYING
                && loadedTrack != null && !recordedPlayback) {
            recordedPlayback = true;
            history.removeIf(t -> t.getPath().equals(loadedTrack.getPath()));
            Track historyTrack = loadedTrack;
            if (historyTrack.getMetadata().duration().isZero() && !mediaEngine.getDuration().isZero()) {
                var m = historyTrack.getMetadata();
                historyTrack = new Track(historyTrack.getPath(), new io.github.nikhilvirdi.retroamp98.library.TrackMetadata(
                        historyTrack.getTitle(), historyTrack.getArtist() == null ? "" : historyTrack.getArtist(),
                        historyTrack.getAlbum() == null ? "" : historyTrack.getAlbum(), m.albumArtist(), m.year(), m.genre(),
                        m.trackNumber(), m.description(), mediaEngine.getDuration()));
            }
            history.addFirst(historyTrack);
            if (history.size() > ListeningHistory.LIMIT) history.removeLast();
            persistHistory();
            notifyHistoryUpdated();
        }
        for (ControllerListener l : listeners) {
            l.onStateChanged(state);
        }
    }

    @Override
    public void onPositionChanged(Duration position) {
        Duration total = mediaEngine.getDuration();
        for (ControllerListener l : listeners) {
            l.onPositionChanged(position, total);
        }
    }

    @Override
    public void onDurationKnown(Duration duration) {
        for (ControllerListener l : listeners) {
            l.onDurationKnown(duration);
        }
    }

    @Override
    public void onEndOfMedia() {
        Track nextTrack = playlistManager.next();
        if (nextTrack != null) {
            loadAndPlay(nextTrack);
        } else {
            mediaEngine.stop();
        }
    }

    @Override
    public void onError(String message) {
        loadedTrack = null;
        notifyNotice(NoticeLevel.ERROR, "RetroAmp98 Error", "Playback error:\n" + message);
    }

    private void notifyNotice(NoticeLevel level, String title, String message) {
        for (ControllerListener listener : listeners) {
            listener.onNotice(level, title, message);
        }
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        for (ControllerListener l : listeners) {
            l.onTrackInfoReady(trackInfo);
        }
    }

    @Override public void onSpectrum(float[] magnitudes) {
        for (ControllerListener listener : listeners) listener.onSpectrum(magnitudes);
    }

    @Override
    public void onTracksAdded(int index, List<Track> tracks) {
        notifyPlaylistUpdated();
    }

    @Override
    public void onTrackRemoved(int index, Track removedTrack) {
        notifyPlaylistUpdated();
    }

    @Override
    public void onTrackMoved(int fromIndex, int toIndex) {
        notifyPlaylistUpdated();
    }

    @Override
    public void onPlaylistCleared() {
        notifyPlaylistUpdated();
    }

    @Override
    public void onCurrentIndexChanged(int newIndex, Track currentTrack) {
        for (ControllerListener l : listeners) {
            l.onTrackChanged(currentTrack, newIndex);
        }
        notifyPlaylistUpdated();
    }

    private void notifyVolumeChanged(double volume, boolean muted) {
        for (ControllerListener l : listeners) {
            l.onVolumeChanged(volume, muted);
        }
    }

    private void notifyRepeatModeChanged(RepeatMode mode) {
        for (ControllerListener l : listeners) {
            l.onRepeatModeChanged(mode);
        }
    }

    private void notifyShuffleChanged(boolean shuffle) {
        for (ControllerListener l : listeners) {
            l.onShuffleChanged(shuffle);
        }
    }

    private void notifyPlaylistUpdated() {
        for (ControllerListener l : listeners) {
            l.onPlaylistUpdated();
        }
    }

    private void loadAndPlay(Track track) {
        loadedTrack = track;
        recordedPlayback = false;
        mediaEngine.load(track.getPath());
        mediaEngine.play();
    }

    @Override
    public void close() {
        closed = true;
        library.close();
        historyStore.close();
        mediaEngine.removeListener(this);
        playlistManager.removeListener(this);
        listeners.clear();
        mediaEngine.close();
    }

    private void persistHistory() {
        historyStore.saveAsync(List.copyOf(history), error -> delivery.execute(() -> {
            if (!closed) notifyNotice(NoticeLevel.WARNING, "History", "Could not save history: " + error.getMessage());
        }));
    }

    private void notifyHistoryUpdated() {
        for (ControllerListener l : listeners) l.onHistoryUpdated();
    }
}
