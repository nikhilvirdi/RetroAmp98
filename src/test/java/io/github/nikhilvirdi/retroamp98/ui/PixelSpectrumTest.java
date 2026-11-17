package io.github.nikhilvirdi.retroamp98.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelSpectrumTest {
    @Test void usesMeasuredLevelsAndResetsWithoutInventingActivity() {
        var spectrum = new PixelSpectrum();
        float[] silence = new float[128]; java.util.Arrays.fill(silence, -60);
        spectrum.update(silence);
        assertEquals(0, spectrum.getChildren().stream().filter(javafx.scene.Node::isVisible).count());
        float[] tone = silence.clone(); tone[2] = -10;
        spectrum.update(tone);
        assertTrue(spectrum.getChildren().stream().anyMatch(javafx.scene.Node::isVisible));
        for (int i = 0; i < 6; i++) spectrum.update(silence);
        assertEquals(0, spectrum.getChildren().stream().filter(javafx.scene.Node::isVisible).count());
        spectrum.update(tone); spectrum.reset();
        assertEquals(0, spectrum.getChildren().stream().filter(javafx.scene.Node::isVisible).count());
        assertEquals(216, spectrum.getPrefWidth()); assertEquals(48, spectrum.getPrefHeight());
    }
}
