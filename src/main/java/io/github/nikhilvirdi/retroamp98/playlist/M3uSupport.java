package io.github.nikhilvirdi.retroamp98.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class M3uSupport {

    private static final String EXTM3U_HEADER = "#EXTM3U";
    private static final String EXTINF_PREFIX = "#EXTINF:";

    private M3uSupport() {
    }

    public record M3uResult(Playlist playlist, List<Path> missingFiles) {
        public M3uResult {
            missingFiles = Collections.unmodifiableList(missingFiles);
        }
    }

    public static void save(Playlist playlist, Path m3uPath) throws IOException {
        Objects.requireNonNull(playlist, "playlist cannot be null");
        Objects.requireNonNull(m3uPath, "m3uPath cannot be null");
        if (FolderImporter.isSupported(m3uPath)) throw new IOException("Choose a playlist filename (.m3u or .m3u8), not an audio file.");

        Path parentDir = m3uPath.toAbsolutePath().getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Path temporary = Files.createTempFile(parentDir, ".retroamp98-", ".m3u.tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(EXTM3U_HEADER);
                writer.newLine();

                for (Track track : playlist.getTracks()) {
                    writer.write(EXTINF_PREFIX + "-1," + track.getTitle().replace('\n', ' ').replace('\r', ' '));
                    writer.newLine();

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
            try { Files.move(temporary, m3uPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, m3uPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static Playlist load(Path m3uPath) throws IOException {
        return loadWithReport(m3uPath, missing -> System.err.println("[M3U] Skipping missing file: " + missing)).playlist();
    }

    public static M3uResult loadWithReport(Path m3uPath) throws IOException {
        return loadWithReport(m3uPath, null);
    }

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
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.equalsIgnoreCase(EXTM3U_HEADER)) {
                    continue;
                }

                if (line.startsWith(EXTINF_PREFIX)) {
                    int commaIdx = line.indexOf(',');
                    if (commaIdx >= 0 && commaIdx < line.length() - 1) {
                        pendingTitle = line.substring(commaIdx + 1).trim();
                    }
                    continue;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                Path candidate;
                try { candidate = Paths.get(line); }
                catch (InvalidPathException e) { pendingTitle = null; continue; }
                Path resolved = candidate.isAbsolute()
                        ? candidate.normalize()
                        : (baseDir != null ? baseDir.resolve(candidate).normalize() : candidate.normalize());

                if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
                    missingFiles.add(resolved);
                    if (missingReporter != null) {
                        missingReporter.accept(resolved);
                    }
                    pendingTitle = null;
                    continue;
                }

                try {
                    Path real = resolved.toRealPath();
                    if (playlist.getTracks().stream().noneMatch(t -> t.getPath().equals(real))) playlist.add(new Track(real, pendingTitle));
                } catch (IOException | SecurityException e) { missingFiles.add(resolved); }
                pendingTitle = null;
            }
        }

        return new M3uResult(playlist, missingFiles);
    }
}
