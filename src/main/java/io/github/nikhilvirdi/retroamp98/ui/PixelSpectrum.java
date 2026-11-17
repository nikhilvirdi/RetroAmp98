package io.github.nikhilvirdi.retroamp98.ui;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/** The reference's 13 columns of six rectangular pixels, fed only by audio magnitudes. */
public final class PixelSpectrum extends Pane {
    private static final int[] BANDS = {0, 1, 2, 3, 4, 6, 9, 13, 19, 28, 41, 60, 88, 128};
    private final Rectangle[][] pixels = new Rectangle[13][6];
    private final int[] levels = new int[13];

    public PixelSpectrum() {
        setMinSize(216, 48);
        setPrefSize(216, 48);
        setMaxSize(216, 48);
        setMouseTransparent(true);
        setAccessibleText("Audio spectrum");
        for (int column = 0; column < 13; column++) {
            for (int row = 0; row < 6; row++) {
                Rectangle pixel = new Rectangle(column * 17, 39 - row * 7, 12, 5);
                pixel.getStyleClass().add("spectrum-pixel");
                pixel.setVisible(false);
                pixels[column][row] = pixel;
                getChildren().add(pixel);
            }
        }
    }

    public void update(float[] magnitudes) {
        for (int column = 0; column < 13; column++) {
            float peak = -60;
            for (int band = BANDS[column]; band < Math.min(BANDS[column + 1], magnitudes.length); band++) {
                if (Float.isFinite(magnitudes[band])) peak = Math.max(peak, magnitudes[band]);
            }
            int target = (int) Math.ceil(Math.clamp((peak + 60) / 60.0, 0, 1) * 6);
            levels[column] = Math.max(target, levels[column] - 1);
            for (int row = 0; row < 6; row++) pixels[column][row].setVisible(row < levels[column]);
        }
    }

    public void reset() {
        java.util.Arrays.fill(levels, 0);
        for (Rectangle[] column : pixels) for (Rectangle pixel : column) pixel.setVisible(false);
    }
}
