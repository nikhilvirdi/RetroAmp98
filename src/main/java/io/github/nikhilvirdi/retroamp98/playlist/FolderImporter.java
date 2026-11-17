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

public final class FolderImporter {

    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp3", "wav", "m4a"
    );

    private FolderImporter() {
    }

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

    public static List<Track> importFolder(Path folder) {
        return findMediaFiles(folder).stream()
                .map(Track::new)
                .toList();
    }

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
