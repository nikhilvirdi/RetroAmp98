package io.github.nikhilvirdi.retroamp98.ui;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class AlbumArtworkTest {
    @Test void preservesProportionsAndRejectsCorruptedArt() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB), "png", out);
        var image = AlbumArtwork.decode(out.toByteArray());
        assertNotNull(image);
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        assertNull(AlbumArtwork.decode(null));
        assertNull(AlbumArtwork.decode(new byte[]{1, 2, 3}));
        byte[] oversized = out.toByteArray();
        java.nio.ByteBuffer.wrap(oversized, 16, 4).putInt(20000);
        assertNull(AlbumArtwork.decode(oversized));
    }
}
