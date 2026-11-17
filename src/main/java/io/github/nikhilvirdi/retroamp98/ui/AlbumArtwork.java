package io.github.nikhilvirdi.retroamp98.ui;

import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

final class AlbumArtwork {
    private AlbumArtwork() {}

    static Image decode(byte[] bytes) {
        if (bytes == null) return null;
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 16384 || height > 16384
                        || (long) width * height > 40_000_000) return null;
            } finally { reader.dispose(); }
            Image image = new Image(new ByteArrayInputStream(bytes), 1024, 1024, true, true);
            return image.isError() ? null : image;
        } catch (Exception e) { return null; }
    }
}
