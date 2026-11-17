package io.github.nikhilvirdi.retroamp98.library;

import io.github.nikhilvirdi.retroamp98.playlist.Track;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class MetadataReader {
    private static final int MAX_TAG_BYTES = 32 * 1024 * 1024;
    private static final int MAX_ART_BYTES = 16 * 1024 * 1024;

    public Track read(Path path) throws IOException {
        AudioFile audio = readAudio(path);
        Tag tag = audio.getTag();
        return new Track(path, new TrackMetadata(value(tag, FieldKey.TITLE), value(tag, FieldKey.ARTIST),
                value(tag, FieldKey.ALBUM), value(tag, FieldKey.ALBUM_ARTIST), value(tag, FieldKey.YEAR),
                value(tag, FieldKey.GENRE), trackNumber(tag), value(tag, FieldKey.COMMENT),
                duration(audio)));
    }

    public byte[] artwork(Path path) throws IOException {
        Tag tag = readAudio(path).getTag();
        if (tag == null || tag.getFirstArtwork() == null) return null;
        byte[] bytes = tag.getFirstArtwork().getBinaryData();
        return bytes != null && bytes.length > 0 && bytes.length <= MAX_ART_BYTES ? bytes : null;
    }

    private AudioFile readAudio(Path path) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Import cancelled");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) throw new IOException("Unreadable audio file");
        try {
            if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mp3")) {
                try (var input = Files.newInputStream(path)) {
                    byte[] header = input.readNBytes(10);
                    if (header.length == 10 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                        int size = 0;
                        for (int i = 6; i < 10; i++) {
                            if (header[i] < 0) throw new IOException("Invalid ID3 size");
                            size = (size << 7) | header[i];
                        }
                        if (size > MAX_TAG_BYTES || size > Files.size(path) - 10) throw new IOException("Invalid or oversized ID3 tag");
                    }
                }
                return new MP3File(path.toFile(), MP3File.LOAD_ALL, true);
            }
            return AudioFileIO.read(path.toFile());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot read audio metadata: " + path.getFileName(), e);
        }
    }

    private static String trackNumber(Tag tag) {
        String number = value(tag, FieldKey.TRACK);
        String total = value(tag, FieldKey.TRACK_TOTAL);
        return number.isEmpty() || total.isEmpty() || number.contains("/") ? number : number + " / " + total;
    }

    private static Duration duration(AudioFile audio) {
        double seconds = audio.getAudioHeader().getPreciseTrackLength();
        return Double.isFinite(seconds) && seconds >= 0 && seconds <= Long.MAX_VALUE / 1000.0
                ? Duration.ofMillis((long) (seconds * 1000)) : Duration.ZERO;
    }

    private static String value(Tag tag, FieldKey key) {
        if (tag == null) return "";
        try {
            String value = tag.getFirst(key);
            if (value == null) return "";
            value = value.replace("\u0000", "").trim();
            return value.substring(0, Math.min(value.length(), key == FieldKey.COMMENT ? 8192 : 512));
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            return "";
        }
    }
}
