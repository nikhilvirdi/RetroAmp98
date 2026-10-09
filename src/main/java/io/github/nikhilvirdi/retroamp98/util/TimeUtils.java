package io.github.nikhilvirdi.retroamp98.util;

/**
 * Utility helper methods for formatting and time calculations.
 */
public final class TimeUtils {
    private TimeUtils() {
        // Prevent instantiation
    }

    /**
     * Formats duration in seconds into mm:ss format.
     */
    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
