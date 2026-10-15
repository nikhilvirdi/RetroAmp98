package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.EngineListener;
import io.github.nikhilvirdi.retroamp98.engine.MediaEngine;
import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.FolderImporter;
import io.github.nikhilvirdi.retroamp98.playlist.M3uSupport;
import io.github.nikhilvirdi.retroamp98.playlist.Playlist;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistListener;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller mediating interactions between MediaEngine, PlaylistManager, and the UI.
 * Handles auto-advance, user transport commands, and error dialogs.
 */
public class PlayerController implements EngineListener, PlaylistListener {

    private final MediaEngine mediaEngine;
    private final PlaylistManager playlistManager;
    private final List<ControllerListener> listeners = new CopyOnWriteArrayList<>();

    private Component parentWindow;

    public PlayerController(MediaEngine mediaEngine, PlaylistManager playlistManager) {
        this.mediaEngine = Objects.requireNonNull(mediaEngine, "mediaEngine cannot be null");
        this.playlistManager = Objects.requireNonNull(playlistManager, "playlistManager cannot be null");

        this.mediaEngine.addListener(this);
        this.playlistManager.addListener(this);
    }

    public void setParentWindow(Component parentWindow) {
        this.parentWindow = parentWindow;
    }

    public MediaEngine getMediaEngine() {
        return mediaEngine;
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public void addListener(ControllerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ControllerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    // --- User Transport Actions ---

    public void playPause() {
        PlayerState state = mediaEngine.getState();
        if (state == PlayerState.PLAYING) {
            mediaEngine.pause();
        } else if (state == PlayerState.PAUSED) {
            mediaEngine.play();
        } else {
            // Stopped or idle
            Track track = playlistManager.current();
            if (track == null && playlistManager.hasNext()) {
                track = playlistManager.next();
            }
            if (track != null) {
                mediaEngine.load(track.getPath());
                mediaEngine.play();
            }
        }
    }

    public void play() {
        PlayerState state = mediaEngine.getState();
        if (state == PlayerState.PAUSED) {
            mediaEngine.play();
        } else {
            Track track = playlistManager.current();
            if (track == null && playlistManager.hasNext()) {
                track = playlistManager.next();
            }
            if (track != null) {
                mediaEngine.load(track.getPath());
                mediaEngine.play();
            }
        }
    }

    public void pause() {
        mediaEngine.pause();
    }

    public void stop() {
        mediaEngine.stop();
    }

    public void next() {
        Track nextTrack = playlistManager.next();
        if (nextTrack != null) {
            mediaEngine.load(nextTrack.getPath());
            mediaEngine.play();
        } else {
            mediaEngine.stop();
        }
    }

    public void previous() {
        // If > 3 seconds in, restart track like standard hardware CD players
        if (mediaEngine.getPosition().compareTo(Duration.ofSeconds(3)) > 0) {
            mediaEngine.seek(Duration.ZERO);
            return;
        }

        Track prevTrack = playlistManager.previous();
        if (prevTrack != null) {
            mediaEngine.load(prevTrack.getPath());
            mediaEngine.play();
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
        Track track = playlistManager.jumpTo(index);
        if (track != null) {
            mediaEngine.load(track.getPath());
            mediaEngine.play();
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

    // --- Playlist File Operations ---

    public void openFiles(List<Path> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return;

        List<Path> supported = new java.util.ArrayList<>();
        List<Path> unsupported = new java.util.ArrayList<>();
        for (Path p : filePaths) {
            if (FolderImporter.isSupported(p)) {
                supported.add(p);
            } else {
                unsupported.add(p);
            }
        }

        if (!unsupported.isEmpty() && supported.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "Unsupported file format:\n" + unsupported.get(0).getFileName()
                            + "\n\nRetroAmp98 supports the following audio formats:\n.mp3, .wav, .m4a",
                    "Unsupported Format",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!unsupported.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parentWindow,
                    unsupported.size() + " unsupported file(s) skipped.\nSupported formats: .mp3, .wav, .m4a",
                    "Format Notice",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        List<Track> tracks = supported.stream().map(Track::new).toList();
        boolean wasEmpty = playlistManager.getPlaylist().isEmpty();
        playlistManager.addTracks(tracks);

        if (wasEmpty || mediaEngine.getState() == PlayerState.IDLE || mediaEngine.getState() == PlayerState.STOPPED) {
            playTrack(0);
        }
    }

    public void openFolder(Path folder) {
        if (folder == null) return;
        List<Track> imported = FolderImporter.importFolder(folder);
        if (imported.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "No supported audio files found in:\n" + folder,
                    "Open Folder",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        boolean wasEmpty = playlistManager.getPlaylist().isEmpty();
        playlistManager.addTracks(imported);

        if (wasEmpty || mediaEngine.getState() == PlayerState.IDLE || mediaEngine.getState() == PlayerState.STOPPED) {
            playTrack(0);
        }
    }

    public void openPlaylist(Path m3uPath) {
        if (m3uPath == null) return;
        try {
            M3uSupport.M3uResult result = M3uSupport.loadWithReport(m3uPath);
            if (!result.missingFiles().isEmpty()) {
                JOptionPane.showMessageDialog(
                    parentWindow,
                    "Playlist loaded with " + result.missingFiles().size() + " missing file(s) skipped.",
                    "Playlist Notice",
                    JOptionPane.WARNING_MESSAGE
                );
            }

            playlistManager.setPlaylist(result.playlist());
            if (!result.playlist().isEmpty()) {
                playTrack(0);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "Failed to load playlist file:\n" + e.getMessage(),
                    "Playlist Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void savePlaylist(Path m3uPath) {
        if (m3uPath == null) return;
        try {
            M3uSupport.save(playlistManager.getPlaylist(), m3uPath);
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "Playlist saved successfully to:\n" + m3uPath.getFileName(),
                    "Save Playlist",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "Failed to save playlist:\n" + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void removeTrack(int index) {
        boolean wasCurrent = (index == playlistManager.getCurrentIndex());
        playlistManager.removeTrack(index);

        if (wasCurrent) {
            Track newCurrent = playlistManager.current();
            if (newCurrent != null) {
                mediaEngine.load(newCurrent.getPath());
                mediaEngine.play();
            } else {
                mediaEngine.stop();
            }
        }
    }

    public void clearPlaylist() {
        mediaEngine.stop();
        playlistManager.clear();
    }

    // --- EngineListener Callbacks (from MediaEngine on EDT) ---

    @Override
    public void onStateChanged(PlayerState state) {
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
        // Auto-advance according to RepeatMode
        Track nextTrack = playlistManager.next();
        if (nextTrack != null) {
            mediaEngine.load(nextTrack.getPath());
            mediaEngine.play();
        } else {
            mediaEngine.stop();
        }
    }

    @Override
    public void onError(String message) {
        JOptionPane.showMessageDialog(
                parentWindow,
                "Playback error:\n" + message,
                "RetroAmp98 Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        for (ControllerListener l : listeners) {
            l.onTrackInfoReady(trackInfo);
        }
    }

    // --- PlaylistListener Callbacks (from PlaylistManager) ---

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
}
