package io.github.nikhilvirdi.retroamp98.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.time.Duration;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {

    @Test void mixedDroppedPathsUseTheSameValidationAndKeepExplicitOrder(@TempDir Path directory) throws Exception {
        Path first = AudioFixtures.wave(directory.resolve("a.wav"));
        Path second = AudioFixtures.wave(directory.resolve("z.wav"));
        Path folder = Files.createDirectory(directory.resolve("folder"));
        Path third = AudioFixtures.wave(folder.resolve("inside.wav"));
        Path unsupported = Files.writeString(directory.resolve("notes.txt"), "not audio");
        try (LibraryService library = new LibraryService()) {
            var imported = library.readPaths(java.util.List.of(second, first, folder, first, unsupported, directory.resolve("missing.wav")));
            assertEquals(java.util.List.of(second, first, third), imported.tracks().stream().map(io.github.nikhilvirdi.retroamp98.playlist.Track::getPath).toList());
            assertEquals(2, imported.skipped());
        }
    }
    @Test void readsId3v23CommentsUnicodeAndOriginalArtwork(@TempDir Path directory) throws Exception {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", image);
        Path path = AudioFixtures.taggedMp3(directory.resolve("misleading filename.mp3"), image.toByteArray());
        byte[] before = Files.readAllBytes(path);
        MetadataReader reader = new MetadataReader();
        var m = reader.read(path).getMetadata();
        assertEquals("Embedded title – café", m.title());
        assertEquals("Embedded artist", m.artist());
        assertEquals("Embedded album", m.album());
        assertEquals("Album artist", m.albumArtist());
        assertEquals("1998", m.year());
        assertEquals("Electronic", m.genre());
        assertEquals("4 / 12", m.trackNumber());
        assertEquals("A real COMMENT.\nSecond line – café.", m.description());
        assertTrue(m.duration().compareTo(Duration.ZERO) > 0);
        assertArrayEquals(image.toByteArray(), reader.artwork(path));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test void untaggedWaveHasNeutralFallbackAndMalformedTagsAreRejected(@TempDir Path directory) throws Exception {
        MetadataReader reader = new MetadataReader();
        var wave = reader.read(AudioFixtures.wave(directory.resolve("plain.wav")));
        assertEquals("plain", wave.getTitle());
        assertNull(wave.getArtist());
        assertEquals("", wave.getMetadata().description());
        assertEquals(Duration.ofSeconds(1), wave.getMetadata().duration());
        assertNull(reader.artwork(wave.getPath()));
        Path bad = Files.write(directory.resolve("bad.mp3"), new byte[]{'I','D','3',3,0,0,127,127,127,127});
        assertThrows(IOException.class, () -> reader.read(bad));
        Path badM4a = Files.writeString(directory.resolve("bad.m4a"), "not audio");
        assertThrows(IOException.class, () -> reader.read(badM4a));
        assertThrows(IOException.class, () -> reader.read(directory.resolve("missing.wav")));
    }

    @Test void recursiveImportDeduplicatesAndSkipsBrokenFiles(@TempDir Path directory) throws Exception {
        Path first = AudioFixtures.wave(directory.resolve("first.wav"));
        Path nested = Files.createDirectory(directory.resolve("nested"));
        AudioFixtures.wave(nested.resolve("second.WAV"));
        Files.writeString(directory.resolve("bad.mp3"), "bad");
        Files.writeString(directory.resolve("ignore.txt"), "text");
        try (LibraryService library = new LibraryService()) {
            var result = library.submit(() -> library.readFolder(directory)).get();
            assertEquals(2, result.tracks().size());
            assertEquals(1, result.skipped());
            assertEquals(1, library.readFiles(java.util.List.of(first, first, directory.resolve("nested/../first.wav"))).tracks().size());
            assertTrue(library.readFolder(Files.createDirectory(directory.resolve("empty"))).tracks().isEmpty());
        }
    }
}
