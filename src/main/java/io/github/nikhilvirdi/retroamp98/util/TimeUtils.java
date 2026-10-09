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

    /**
     * Formats a java.time.Duration into mm:ss format.
     */
    public static String formatDuration(java.time.Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "00:00";
        }
        long totalSeconds = duration.toSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
