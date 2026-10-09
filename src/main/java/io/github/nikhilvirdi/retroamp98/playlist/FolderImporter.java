package io.github.nikhilvirdi.retroamp98.playlist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Helper utility for importing supported media files from directory folders.
 */
public final class FolderImporter {

    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp3", "wav", "aac", "m4a", "mp4"
    );

    private FolderImporter() {
        // Utility class
    }

    /**
     * Scans a folder for supported audio and video media files, returning them sorted by file name.
     *
     * @param folder directory path to scan
     * @return sorted list of matching file paths
     */
    public static List<Path> findMediaFiles(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(FolderImporter::isSupported)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Scans a folder and returns a list of Tracks created from the supported files.
     *
     * @param folder directory path to scan
     * @return sorted list of Track instances
     */
    public static List<Track> importFolder(Path folder) {
        return findMediaFiles(folder).stream()
                .map(Track::new)
                .toList();
    }

    /**
     * Checks if a given file has one of the supported media extensions.
     */
    public static boolean isSupported(Path path) {
        if (path == null) return false;
        Path fileName = path.getFileName();
        if (fileName == null) return false;
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }
}
