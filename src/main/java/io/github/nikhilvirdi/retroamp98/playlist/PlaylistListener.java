package io.github.nikhilvirdi.retroamp98.playlist;

import java.util.List;

public interface PlaylistListener {

    default void onTracksAdded(int index, List<Track> tracks) {}

    default void onTrackRemoved(int index, Track removedTrack) {}

    default void onTrackMoved(int fromIndex, int toIndex) {}

    default void onPlaylistCleared() {}

    default void onCurrentIndexChanged(int newIndex, Track currentTrack) {}
}
