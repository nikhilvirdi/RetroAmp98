package io.github.nikhilvirdi.retroamp98.library;

import io.github.nikhilvirdi.retroamp98.playlist.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ListeningHistoryTest {
    @Test void synchronousSaveSupersedesQueuedHistory(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("history.properties");
        Track old = new Track(directory.resolve("old.mp3")), newest = new Track(directory.resolve("new.mp3"));
        var error = new java.util.concurrent.atomic.AtomicReference<Exception>();
        try (ListeningHistory store = new ListeningHistory(file)) {
            store.saveAsync(List.of(old), error::set);
            store.save(List.of(newest));
            Thread.sleep(250);
            assertEquals(List.of(newest), store.load());
            assertNull(error.get());
        }
    }

    @Test void queuedHistoryOwnsAnImmutableSnapshot(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("history.properties");
        Track track = new Track(directory.resolve("song.mp3"));
        var tracks = new java.util.ArrayList<>(List.of(track));
        var error = new java.util.concurrent.atomic.AtomicReference<Exception>();
        try (ListeningHistory store = new ListeningHistory(file)) {
            store.saveAsync(tracks, error::set);
            tracks.clear();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (!Files.exists(file) && System.nanoTime() < deadline) Thread.sleep(10);
            assertEquals(List.of(track), store.load());
            assertNull(error.get());
        }
    }
    @Test void restoresFullHistoryWithMaximumLengthUnicodeMetadata(@TempDir Path directory) throws Exception {
        String field = "音".repeat(512), description = "音".repeat(8192);
        var metadata = new TrackMetadata(field, field, field, field, field, field, field, description, Duration.ofSeconds(10));
        var tracks = java.util.stream.IntStream.range(0, ListeningHistory.LIMIT)
                .mapToObj(i -> new Track(directory.resolve(i + ".mp3"), metadata)).toList();
        Path file = directory.resolve("history.properties");
        try (ListeningHistory store = new ListeningHistory(file)) {
            store.save(tracks);
            assertTrue(Files.size(file) > 8 * 1024 * 1024);
            var restored = store.load();
            assertEquals(tracks, restored);
            assertEquals(metadata, restored.getLast().getMetadata());
        }
    }
    @Test void persistsLightweightMetadataAndKeepsMissingReferences(@TempDir Path directory) throws Exception {
        Track track = new Track(directory.resolve("relocated.mp3"), new TrackMetadata("Title", "Artist", "Album",
                "Album Artist", "1998", "Electronic", "4", "Comment\nUnicode café", Duration.ofSeconds(98)));
        Path path = directory.resolve("history.properties");
        try (ListeningHistory store = new ListeningHistory(path)) {
            store.save(List.of(track));
            Track restored = store.load().getFirst();
            assertEquals(track, restored);
            assertEquals(track.getMetadata(), restored.getMetadata());
            assertTrue(Files.size(path) < 2048);
            store.save(List.of());
            assertTrue(store.load().isEmpty());
        }
    }

    @Test void skipsDamagedEntriesAndDuplicates(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("history.properties");
        Files.writeString(path, "0.path=bad\\u0000.mp3\n1.path=valid.mp3\n1.title=Valid\n2.path=valid.mp3\n3.path=broken.mp3\n3.duration=oops\n");
        try (ListeningHistory store = new ListeningHistory(path)) {
            assertEquals(List.of("Valid"), store.load().stream().map(Track::getTitle).toList());
        }
    }
}
