package io.github.nikhilvirdi.retroamp98.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard 10-band equalizer presets for RetroAmp98.
 * Decoupled from UI and JavaFX types for standalone unit testing.
 */
public final class EqualizerPresets {

    public static final int NUM_BANDS = 10;
    public static final double MIN_GAIN_DB = -12.0;
    public static final double MAX_GAIN_DB = 12.0;

    public static final String[] BAND_LABELS = {
            "32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    };

    private static final Map<String, double[]> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("Flat", new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});
        PRESETS.put("Rock", new double[]{4.8, 3.2, -1.6, -3.2, -1.0, 1.2, 3.6, 4.8, 4.8, 4.8});
        PRESETS.put("Pop", new double[]{-1.6, 1.2, 3.0, 3.6, 2.0, -1.0, -1.6, -1.6, 1.2, 2.4});
        PRESETS.put("Jazz", new double[]{3.2, 2.0, 1.2, 2.0, -1.6, -1.6, 0.0, 1.2, 2.4, 3.2});
        PRESETS.put("Classical", new double[]{4.0, 3.0, 2.0, 2.0, -1.0, -1.0, 0.0, 2.0, 3.0, 3.6});
        PRESETS.put("Bass Boost", new double[]{6.0, 5.0, 4.0, 2.5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0});
        PRESETS.put("Vocal", new double[]{-2.0, -1.6, 0.0, 2.4, 4.0, 4.0, 2.4, 1.0, 0.0, -1.0});
    }

    private EqualizerPresets() {
    }

    public static List<String> getPresetNames() {
        return List.copyOf(PRESETS.keySet());
    }

    public static double[] getPreset(String name) {
        double[] gains = PRESETS.get(name);
        if (gains == null) {
            gains = PRESETS.get("Flat");
        }
        return gains.clone();
    }

    public static double clampGain(double gainDb) {
        return Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, gainDb));
    }
}
