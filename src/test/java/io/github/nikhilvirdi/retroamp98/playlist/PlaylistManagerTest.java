package io.github.nikhilvirdi.retroamp98.playlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistManagerTest {

    @Test
    void bulkAdditionSnapshotsLiveViewsAndSkipsNullsConsistently() {
        manager.setPlayOrderStrategy(new ShuffleOrder(new Random(42)));
        var notified = new java.util.ArrayList<Track>();
        manager.addListener(new PlaylistListener() {
            @Override public void onTracksAdded(int index, java.util.List<Track> tracks) { notified.addAll(tracks); }
        });
        manager.addTracks(java.util.Arrays.asList(track1, null, track2));
        manager.addTracks(manager.getPlaylist().getTracks());
        assertEquals(10, manager.getPlaylist().size());
        assertEquals(7, notified.size());
        assertFalse(notified.contains(null));
        Set<Integer> visited = new HashSet<>();
        while (manager.hasNext()) {
            assertNotNull(manager.next());
            assertTrue(visited.add(manager.getCurrentIndex()));
        }
        assertEquals(10, visited.size());
    }

    @Test
    void removingCurrentTrackKeepsShuffleCursorOnItsReplacement() {
        var shuffle = new ShuffleOrder(new Random(42));
        manager.setPlayOrderStrategy(shuffle);
        manager.jumpTo(0);
        manager.removeTrack(0);
        assertEquals(manager.getCurrentIndex(), shuffle.getShuffleOrder().get(shuffle.getPointer()));
        int pointer = shuffle.getPointer();
        if (pointer + 1 < shuffle.getShuffleOrder().size()) {
            int expected = shuffle.getShuffleOrder().get(pointer + 1);
            manager.next();
            assertEquals(expected, manager.getCurrentIndex());
        } else assertNull(manager.next());
    }

    private PlaylistManager manager;
    private Track track1;
    private Track track2;
    private Track track3;

    @BeforeEach
    void setUp() {
        manager = new PlaylistManager();
        track1 = new Track(Paths.get("C:/music/track1.mp3"), "Track 1");
        track2 = new Track(Paths.get("C:/music/track2.wav"), "Track 2");
        track3 = new Track(Paths.get("C:/music/track3.aac"), "Track 3");

        manager.addTrack(track1);
        manager.addTrack(track2);
        manager.addTrack(track3);
    }

    @Test
    @DisplayName("Boundary test: next and previous under RepeatMode.OFF")
    void testNextPreviousBoundariesRepeatOff() {
        manager.setRepeatMode(RepeatMode.OFF);

        manager.jumpTo(0);
        assertEquals(track1, manager.current());
        assertFalse(manager.hasPrevious(), "Should not have previous track at index 0 under RepeatMode.OFF");
        assertNull(manager.previous(), "previous() at start boundary should return null");

        assertTrue(manager.hasNext());
        assertEquals(track2, manager.next());

        assertTrue(manager.hasNext());
        assertEquals(track3, manager.next());
        assertEquals(2, manager.getCurrentIndex());

        assertFalse(manager.hasNext(), "Should not have next track at end under RepeatMode.OFF");
        assertNull(manager.next(), "next() at end boundary should return null");
        assertEquals(track3, manager.current(), "Current track should remain at end boundary");
    }

    @Test
    @DisplayName("Boundary test: next and previous wrap around under RepeatMode.ALL")
    void testNextPreviousBoundariesRepeatAll() {
        manager.setRepeatMode(RepeatMode.ALL);

        manager.jumpTo(0);
        assertTrue(manager.hasPrevious());
        assertEquals(track3, manager.previous(), "previous() at index 0 should wrap to last track");
        assertEquals(2, manager.getCurrentIndex());

        assertTrue(manager.hasNext());
        assertEquals(track1, manager.next(), "next() at end boundary should wrap to index 0");
        assertEquals(0, manager.getCurrentIndex());
    }

    @Test
    @DisplayName("Boundary test: next and previous stay on current track under RepeatMode.ONE")
    void testNextPreviousBoundariesRepeatOne() {
        manager.setRepeatMode(RepeatMode.ONE);

        manager.jumpTo(0);
        assertTrue(manager.hasNext());
        assertTrue(manager.hasPrevious());
        assertEquals(track1, manager.next(), "next() under RepeatMode.ONE should repeat current track");
        assertEquals(0, manager.getCurrentIndex());
        assertEquals(track1, manager.previous(), "previous() under RepeatMode.ONE should repeat current track");
        assertEquals(0, manager.getCurrentIndex());

        manager.jumpTo(2);
        assertTrue(manager.hasNext());
        assertTrue(manager.hasPrevious());
        assertEquals(track3, manager.next(), "next() at end boundary under RepeatMode.ONE should repeat current track");
        assertEquals(2, manager.getCurrentIndex());
    }

    @Test
    @DisplayName("Shuffle fairness: no track repeats until all have played in a cycle")
    void testShuffleFairness() {
        PlaylistManager shuffleManager = new PlaylistManager();
        for (int i = 0; i < 5; i++) {
            shuffleManager.addTrack(new Track(Paths.get("C:/music/track" + i + ".mp3")));
        }

        shuffleManager.setRepeatMode(RepeatMode.OFF);
        shuffleManager.setPlayOrderStrategy(new ShuffleOrder(new Random(42)));

        Set<Track> played = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            assertTrue(shuffleManager.hasNext(), "Should have next track until all 5 have played");
            Track t = shuffleManager.next();
            assertNotNull(t);
            boolean isNew = played.add(t);
            assertTrue(isNew, "Fairness violation: track repeated before cycle completed: " + t);
        }

        assertEquals(5, played.size(), "All 5 unique tracks must be played exactly once");
        assertFalse(shuffleManager.hasNext(), "Cycle completed; hasNext should be false under RepeatMode.OFF");
    }

    @Test
    @DisplayName("Remove while playing: active index and track are safely maintained")
    void testRemoveWhilePlaying() {
        manager.jumpTo(1);
        assertEquals(track2, manager.current());

        AtomicBoolean indexChangedFired = new AtomicBoolean(false);
        manager.addListener(new PlaylistListener() {
            @Override
            public void onCurrentIndexChanged(int newIndex, Track currentTrack) {
                indexChangedFired.set(true);
            }
        });

        Track removed0 = manager.removeTrack(0);
        assertEquals(track1, removed0);
        assertEquals(0, manager.getCurrentIndex(), "Index should shift from 1 to 0");
        assertEquals(track2, manager.current(), "Current track must remain track 2");

        Track removedCurrent = manager.removeTrack(0);
        assertEquals(track2, removedCurrent);
        assertEquals(0, manager.getCurrentIndex());
        assertEquals(track3, manager.current(), "Current track should now be track 3");
        assertTrue(indexChangedFired.get());

        manager.removeTrack(0);
        assertEquals(-1, manager.getCurrentIndex());
        assertNull(manager.current(), "Current track should be null when playlist is empty");
    }

    @Test
    @DisplayName("Move track: active index tracks the moved item accurately")
    void testMoveTrack() {
        manager.jumpTo(1);
        assertEquals(track2, manager.current());

        manager.moveTrack(1, 2);
        assertEquals(2, manager.getCurrentIndex(), "Current index should follow track 2 to index 2");
        assertEquals(track2, manager.current());

        manager.moveTrack(0, 2);
        assertEquals(1, manager.getCurrentIndex(), "Current index should adjust to 1 as items shift before it");
        assertEquals(track2, manager.current(), "Active track must still be track 2");
    }
}
