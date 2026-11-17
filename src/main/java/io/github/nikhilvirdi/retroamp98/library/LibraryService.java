package io.github.nikhilvirdi.retroamp98.library;

import io.github.nikhilvirdi.retroamp98.playlist.FolderImporter;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class LibraryService implements AutoCloseable {
    private final MetadataReader metadata = new MetadataReader();
    private final java.util.Set<CompletableFuture<?>> pending = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), task -> {
                Thread thread = new Thread(task, "retroamp-library");
                thread.setDaemon(true);
                return thread;
            });

    public record ImportResult(List<Track> tracks, int skipped) {}

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        try {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try { return task.call(); }
                catch (Exception e) { throw new CompletionException(e); }
            }, worker);
            pending.add(future);
            future.whenComplete((value, error) -> pending.remove(future));
            return future;
        } catch (java.util.concurrent.RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new IOException("Library busy; try again after the current import.", e));
        }
    }

    public MetadataReader metadata() { return metadata; }

    public ImportResult readFiles(List<Path> paths) throws IOException {
        List<Track> tracks = new ArrayList<>();
        var seen = new LinkedHashSet<Path>();
        int skipped = 0;
        for (Path path : paths) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Import cancelled");
            if (!FolderImporter.isSupported(path)) { skipped++; continue; }
            try {
                Path real = path.toRealPath();
                if (seen.add(real)) tracks.add(metadata.read(real));
            } catch (IOException | RuntimeException e) { skipped++; }
        }
        return new ImportResult(List.copyOf(tracks), skipped);
    }

    public ImportResult readFolder(Path folder) throws IOException {
        return readPaths(List.of(folder));
    }

    public ImportResult readPaths(List<Path> paths) throws IOException {
        List<Path> files = new ArrayList<>();
        int[] skipped = {0};
        for (Path path : paths) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Import cancelled");
            if (!Files.isDirectory(path)) { files.add(path); continue; }
            List<Path> folderFiles = new ArrayList<>();
            try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return Thread.currentThread().isInterrupted() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
                if (attrs.isRegularFile() && FolderImporter.isSupported(file)) folderFiles.add(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path file, IOException error) {
                skipped[0]++;
                return FileVisitResult.CONTINUE;
            }
            });
            } catch (IOException | SecurityException e) { skipped[0]++; }
            folderFiles.sort(java.util.Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER));
            files.addAll(folderFiles);
        }
        ImportResult result = readFiles(files);
        return new ImportResult(result.tracks(), result.skipped() + skipped[0]);
    }

    @Override public void close() {
        worker.shutdownNow();
        for (CompletableFuture<?> future : pending) future.cancel(false);
    }
}
