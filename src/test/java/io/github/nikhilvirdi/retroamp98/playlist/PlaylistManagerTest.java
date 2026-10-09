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

/**
 * Tests covering playlist navigation boundaries across repeat modes,
 * shuffle fairness, remove-while-playing, and track reordering.
 */
class PlaylistManagerTest {

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

        // Initial jump to start
        manager.jumpTo(0);
        assertEquals(track1, manager.current());
        assertFalse(manager.hasPrevious(), "Should not have previous track at index 0 under RepeatMode.OFF");
        assertNull(manager.previous(), "previous() at start boundary should return null");

        // Advance to middle
        assertTrue(manager.hasNext());
        assertEquals(track2, manager.next());

        // Advance to end boundary
        assertTrue(manager.hasNext());
        assertEquals(track3, manager.next());
        assertEquals(2, manager.getCurrentIndex());

        // At end boundary
        assertFalse(manager.hasNext(), "Should not have next track at end under RepeatMode.OFF");
        assertNull(manager.next(), "next() at end boundary should return null");
        assertEquals(track3, manager.current(), "Current track should remain at end boundary");
    }

    @Test
    @DisplayName("Boundary test: next and previous wrap around under RepeatMode.ALL")
    void testNextPreviousBoundariesRepeatAll() {
        manager.setRepeatMode(RepeatMode.ALL);

        // At start boundary, previous should wrap to the last track
        manager.jumpTo(0);
        assertTrue(manager.hasPrevious());
        assertEquals(track3, manager.previous(), "previous() at index 0 should wrap to last track");
        assertEquals(2, manager.getCurrentIndex());

        // At end boundary, next should wrap back to index 0
        assertTrue(manager.hasNext());
        assertEquals(track1, manager.next(), "next() at end boundary should wrap to index 0");
        assertEquals(0, manager.getCurrentIndex());
    }

    @Test
    @DisplayName("Boundary test: next and previous stay on current track under RepeatMode.ONE")
    void testNextPreviousBoundariesRepeatOne() {
        manager.setRepeatMode(RepeatMode.ONE);

        // Start boundary
        manager.jumpTo(0);
        assertTrue(manager.hasNext());
        assertTrue(manager.hasPrevious());
        assertEquals(track1, manager.next(), "next() under RepeatMode.ONE should repeat current track");
        assertEquals(0, manager.getCurrentIndex());
        assertEquals(track1, manager.previous(), "previous() under RepeatMode.ONE should repeat current track");
        assertEquals(0, manager.getCurrentIndex());

        // End boundary
        manager.jumpTo(2);
        assertTrue(manager.hasNext());
        assertTrue(manager.hasPrevious());
        assertEquals(track3, manager.next(), "next() at end boundary under RepeatMode.ONE should repeat current track");
        assertEquals(2, manager.getCurrentIndex());
    }

    @Test
    @DisplayName("Shuffle fairness: no track repeats until all have played in a cycle")
    void testShuffleFairness() {
        // Setup 5 tracks
        PlaylistManager shuffleManager = new PlaylistManager();
        for (int i = 0; i < 5; i++) {
            shuffleManager.addTrack(new Track(Paths.get("C:/music/track" + i + ".mp3")));
        }

        shuffleManager.setRepeatMode(RepeatMode.OFF);
        // Use seeded random for reproducible shuffle behavior
        shuffleManager.setPlayOrderStrategy(new ShuffleOrder(new Random(42)));

        Set<Track> played = new HashSet<>();
        // Collect all tracks played across a complete shuffle cycle
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
        // Start playing track 2 (index 1)
        manager.jumpTo(1);
        assertEquals(track2, manager.current());

        AtomicBoolean indexChangedFired = new AtomicBoolean(false);
        manager.addListener(new PlaylistListener() {
            @Override
            public void onCurrentIndexChanged(int newIndex, Track currentTrack) {
                indexChangedFired.set(true);
            }
        });

        // Case 1: Remove track 1 (index 0, before current)
        // track 2 now shifts from index 1 to index 0
        Track removed0 = manager.removeTrack(0);
        assertEquals(track1, removed0);
        assertEquals(0, manager.getCurrentIndex(), "Index should shift from 1 to 0");
        assertEquals(track2, manager.current(), "Current track must remain track 2");

        // Case 2: Remove the currently playing track (track 2, now at index 0)
        // track 3 now slides into index 0
        Track removedCurrent = manager.removeTrack(0);
        assertEquals(track2, removedCurrent);
        assertEquals(0, manager.getCurrentIndex());
        assertEquals(track3, manager.current(), "Current track should now be track 3");
        assertTrue(indexChangedFired.get());

        // Case 3: Remove the last remaining track
        manager.removeTrack(0);
        assertEquals(-1, manager.getCurrentIndex());
        assertNull(manager.current(), "Current track should be null when playlist is empty");
    }

    @Test
    @DisplayName("Move track: active index tracks the moved item accurately")
    void testMoveTrack() {
        // Jump to track 2 (index 1)
        manager.jumpTo(1);
        assertEquals(track2, manager.current());

        // Move track 2 from index 1 to index 2 (end)
        manager.moveTrack(1, 2);
        assertEquals(2, manager.getCurrentIndex(), "Current index should follow track 2 to index 2");
        assertEquals(track2, manager.current());

        // Move track 1 from index 0 to index 2 (past current index 2)
        // Order before: [track1, track3, track2]
        // Moving track1 (index 0) to index 2: [track3, track2, track1]
        // track2 shifted from index 2 to index 1
        manager.moveTrack(0, 2);
        assertEquals(1, manager.getCurrentIndex(), "Current index should adjust to 1 as items shift before it");
        assertEquals(track2, manager.current(), "Active track must still be track 2");
    }
}
