package io.github.nikhilvirdi.retroamp98.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EqualizerPresetsTest {

    @Test
    void testPresetListContainsStandardPresets() {
        List<String> names = EqualizerPresets.getPresetNames();
        assertTrue(names.contains("Flat"));
        assertTrue(names.contains("Rock"));
        assertTrue(names.contains("Pop"));
        assertTrue(names.contains("Jazz"));
        assertTrue(names.contains("Classical"));
        assertTrue(names.contains("Bass Boost"));
        assertTrue(names.contains("Vocal"));
    }

    @Test
    void testAllPresetsHaveExactlyTenBandsWithinLimits() {
        for (String name : EqualizerPresets.getPresetNames()) {
            double[] bands = EqualizerPresets.getPreset(name);
            assertNotNull(bands);
            assertEquals(10, bands.length, "Preset " + name + " must have 10 bands");
            for (int i = 0; i < bands.length; i++) {
                assertTrue(bands[i] >= -12.0 && bands[i] <= 12.0,
                        "Band " + i + " in preset " + name + " out of [-12, 12] range: " + bands[i]);
            }
        }
    }

    @Test
    void testFlatPresetHasAllZeros() {
        double[] flat = EqualizerPresets.getPreset("Flat");
        for (double gain : flat) {
            assertEquals(0.0, gain, 0.001);
        }
    }

    @Test
    void testClampingLogic() {
        assertEquals(12.0, EqualizerPresets.clampGain(15.0));
        assertEquals(-12.0, EqualizerPresets.clampGain(-20.0));
        assertEquals(4.5, EqualizerPresets.clampGain(4.5));
    }
}
