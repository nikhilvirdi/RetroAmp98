package io.github.nikhilvirdi.retroamp98.library;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AudioFixtures {
    private AudioFixtures() {}

    public static Path wave(Path path) throws IOException {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(new byte[88200]), format, 44100)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, path.toFile());
        }
        return path;
    }

    public static Path taggedMp3(Path path, byte[] artwork) throws IOException {
        ByteArrayOutputStream frames = new ByteArrayOutputStream();
        text(frames, "TIT2", "Embedded title – café");
        text(frames, "TPE1", "Embedded artist");
        text(frames, "TALB", "Embedded album");
        text(frames, "TPE2", "Album artist");
        text(frames, "TYER", "1998");
        text(frames, "TCON", "Electronic");
        text(frames, "TRCK", "4/12");
        ByteArrayOutputStream comment = new ByteArrayOutputStream();
        comment.write(1);
        comment.write("eng".getBytes(StandardCharsets.US_ASCII));
        comment.write(new byte[]{(byte) 0xfe, (byte) 0xff, 0, 0});
        comment.write("A real COMMENT.\nSecond line – café.".getBytes(StandardCharsets.UTF_16));
        frame(frames, "COMM", comment.toByteArray());
        if (artwork != null) {
            ByteArrayOutputStream picture = new ByteArrayOutputStream();
            picture.write(0);
            picture.write("image/png".getBytes(StandardCharsets.US_ASCII));
            picture.write(new byte[]{0, 3, 0});
            picture.write(artwork);
            frame(frames, "APIC", picture.toByteArray());
        }
        try (var out = Files.newOutputStream(path)) {
            int size = frames.size();
            out.write(new byte[]{'I','D','3',3,0,0,(byte)(size >> 21 & 127),(byte)(size >> 14 & 127),(byte)(size >> 7 & 127),(byte)(size & 127)});
            frames.writeTo(out);
            byte[] mpeg = new byte[417];
            mpeg[0]=(byte)0xff; mpeg[1]=(byte)0xfb; mpeg[2]=(byte)0x90;
            for (int i=0; i<80; i++) out.write(mpeg);
        }
        return path;
    }

    private static void text(OutputStream out, String id, String value) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(1);
        data.write(value.getBytes(StandardCharsets.UTF_16));
        frame(out, id, data.toByteArray());
    }

    private static void frame(OutputStream out, String id, byte[] data) throws IOException {
        DataOutputStream frame = new DataOutputStream(out);
        frame.writeBytes(id); frame.writeInt(data.length); frame.writeShort(0); frame.write(data);
    }
}
