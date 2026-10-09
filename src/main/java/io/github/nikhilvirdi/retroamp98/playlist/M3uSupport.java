package io.github.nikhilvirdi.retroamp98.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Utility for saving and loading M3U and M3U8 playlists in UTF-8 format.
 * Skips missing files and reports them during loading.
 */
public final class M3uSupport {

    private static final String EXTM3U_HEADER = "#EXTM3U";
    private static final String EXTINF_PREFIX = "#EXTINF:";

    private M3uSupport() {
        // Static utility
    }

    /**
     * Result of an M3U load operation, containing the loaded Playlist and skipped missing files.
     */
    public record M3uResult(Playlist playlist, List<Path> missingFiles) {
        public M3uResult {
            missingFiles = Collections.unmodifiableList(missingFiles);
        }
    }

    /**
     * Saves the playlist to an M3U/M3U8 file at the given path in UTF-8.
     * Uses relative paths where possible.
     */
    public static void save(Playlist playlist, Path m3uPath) throws IOException {
        Objects.requireNonNull(playlist, "playlist cannot be null");
        Objects.requireNonNull(m3uPath, "m3uPath cannot be null");

        Path parentDir = m3uPath.toAbsolutePath().getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(m3uPath, StandardCharsets.UTF_8)) {
            writer.write(EXTM3U_HEADER);
            writer.newLine();

            for (Track track : playlist.getTracks()) {
                // Extended info line with track title
                writer.write(EXTINF_PREFIX + "-1," + track.getTitle());
                writer.newLine();

                // Compute relative path if on the same root, otherwise fallback to absolute
                String pathStr;
                Path trackAbs = track.getPath().toAbsolutePath().normalize();
                if (parentDir != null && Objects.equals(parentDir.getRoot(), trackAbs.getRoot())) {
                    pathStr = parentDir.relativize(trackAbs).toString();
                } else {
                    pathStr = trackAbs.toString();
                }

                writer.write(pathStr);
                writer.newLine();
            }
        }
    }

    /**
     * Loads a playlist from an M3U/M3U8 file.
     * Skips non-existent files and logs them to System.err.
     */
    public static Playlist load(Path m3uPath) throws IOException {
        return loadWithReport(m3uPath, missing -> System.err.println("[M3U] Skipping missing file: " + missing)).playlist();
    }

    /**
     * Loads a playlist and returns both the loaded Playlist and any missing files encountered.
     */
    public static M3uResult loadWithReport(Path m3uPath) throws IOException {
        return loadWithReport(m3uPath, null);
    }

    /**
     * Loads a playlist with an optional consumer callback for each missing file encountered.
     */
    public static M3uResult loadWithReport(Path m3uPath, Consumer<Path> missingReporter) throws IOException {
        Objects.requireNonNull(m3uPath, "m3uPath cannot be null");

        if (!Files.exists(m3uPath)) {
            throw new IOException("M3U file not found: " + m3uPath);
        }

        Path baseDir = m3uPath.toAbsolutePath().getParent();
        Playlist playlist = new Playlist();
        List<Path> missingFiles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(m3uPath, StandardCharsets.UTF_8)) {
            String line;
            String pendingTitle = null;

            while ((line = reader.readLine()) != null) {
                // Strip possible UTF-8 BOM
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Header
                if (line.equalsIgnoreCase(EXTM3U_HEADER)) {
                    continue;
                }

                // Track title metadata directive
                if (line.startsWith(EXTINF_PREFIX)) {
                    int commaIdx = line.indexOf(',');
                    if (commaIdx >= 0 && commaIdx < line.length() - 1) {
                        pendingTitle = line.substring(commaIdx + 1).trim();
                    }
                    continue;
                }

                // Ignore other comment lines
                if (line.startsWith("#")) {
                    continue;
                }

                // Resolve path (relative or absolute)
                Path candidate = Paths.get(line);
                Path resolved = candidate.isAbsolute()
                        ? candidate.normalize()
                        : (baseDir != null ? baseDir.resolve(candidate).normalize() : candidate.normalize());

                // Skip missing files and report them
                if (!Files.exists(resolved)) {
                    missingFiles.add(resolved);
                    if (missingReporter != null) {
                        missingReporter.accept(resolved);
                    }
                    pendingTitle = null;
                    continue;
                }

                playlist.add(new Track(resolved, pendingTitle));
                pendingTitle = null;
            }
        }

        return new M3uResult(playlist, missingFiles);
    }
}
