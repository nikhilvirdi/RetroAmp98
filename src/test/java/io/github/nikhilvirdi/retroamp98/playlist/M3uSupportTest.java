package io.github.nikhilvirdi.retroamp98.playlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests covering M3U/M3U8 round-trip serialization, missing file reporting,
 * and folder import helper.
 */
class M3uSupportTest {

    @Test
    @DisplayName("M3U Round-trip: save and load playlist preserving tracks, relative paths, and titles")
    void testM3uRoundTrip(@TempDir Path tempDir) throws IOException {
        // Create actual physical media files in temp directory
        Path audioDir = tempDir.resolve("music");
        Files.createDirectories(audioDir);

        Path file1 = Files.createFile(audioDir.resolve("song1.mp3"));
        Path file2 = Files.createFile(audioDir.resolve("song2.wav"));
        Path file3 = Files.createFile(audioDir.resolve("song3.m4a"));

        Playlist original = new Playlist();
        original.add(new Track(file1, "Song One Custom"));
        original.add(new Track(file2, "Song Two Custom"));
        original.add(new Track(file3, "Song Three Custom"));

        Path m3uFile = tempDir.resolve("playlist.m3u");

        // 1. Save
        M3uSupport.save(original, m3uFile);
        assertTrue(Files.exists(m3uFile), "M3U file must be created");

        // 2. Load
        Playlist loaded = M3uSupport.load(m3uFile);
        assertNotNull(loaded);
        assertEquals(3, loaded.size(), "All 3 tracks should be loaded");

        assertEquals(file1.toAbsolutePath().normalize(), loaded.get(0).getPath().toAbsolutePath().normalize());
        assertEquals("Song One Custom", loaded.get(0).getTitle());

        assertEquals(file2.toAbsolutePath().normalize(), loaded.get(1).getPath().toAbsolutePath().normalize());
        assertEquals("Song Two Custom", loaded.get(1).getTitle());

        assertEquals(file3.toAbsolutePath().normalize(), loaded.get(2).getPath().toAbsolutePath().normalize());
        assertEquals("Song Three Custom", loaded.get(2).getTitle());
    }

    @Test
    @DisplayName("M3U Missing files: non-existent tracks are skipped and reported without failing")
    void testM3uSkipMissingFiles(@TempDir Path tempDir) throws IOException {
        Path existing = Files.createFile(tempDir.resolve("valid.mp3"));
        Path nonExistent = tempDir.resolve("deleted.mp3");

        String m3uContent = """
                #EXTM3U
                #EXTINF:180,Valid Track
                valid.mp3
                #EXTINF:200,Ghost Track
                deleted.mp3
                """;

        Path m3uFile = tempDir.resolve("test_missing.m3u");
        Files.writeString(m3uFile, m3uContent);

        M3uSupport.M3uResult result = M3uSupport.loadWithReport(m3uFile);

        assertEquals(1, result.playlist().size(), "Only the existing file should be loaded");
        assertEquals(existing.toAbsolutePath().normalize(), result.playlist().get(0).getPath().toAbsolutePath().normalize());

        assertEquals(1, result.missingFiles().size(), "Missing file must be recorded in report");
        assertEquals(nonExistent.toAbsolutePath().normalize(), result.missingFiles().get(0).toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Folder importer: imports only supported audio/video formats sorted alphabetically")
    void testFolderImport(@TempDir Path tempDir) throws IOException {
        // Create mixed set of supported and unsupported files
        Files.createFile(tempDir.resolve("zeta.mp3"));
        Files.createFile(tempDir.resolve("alpha.wav"));
        Files.createFile(tempDir.resolve("beta.mp4"));
        Files.createFile(tempDir.resolve("gamma.aac"));
        Files.createFile(tempDir.resolve("delta.m4a"));
        Files.createFile(tempDir.resolve("notes.txt"));
        Files.createFile(tempDir.resolve("cover.jpg"));

        List<Track> imported = FolderImporter.importFolder(tempDir);

        // Expect 5 supported files, sorted case-insensitively
        assertEquals(5, imported.size());
        assertEquals("alpha", imported.get(0).getTitle());
        assertEquals("beta", imported.get(1).getTitle());
        assertEquals("delta", imported.get(2).getTitle());
        assertEquals("gamma", imported.get(3).getTitle());
        assertEquals("zeta", imported.get(4).getTitle());
    }
}
