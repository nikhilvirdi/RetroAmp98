package io.github.nikhilvirdi.retroamp98.engine;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * ============================================================================
 * DEV-ONLY TEST UTILITY: DO NOT USE IN PRODUCTION CODE
 * ============================================================================
 * Throwaway command-line smoke test to verify MediaEngine functionality:
 * - Loads a media file provided via command-line arguments.
 * - Confirms playback and thread bridge (events arriving on Swing EDT).
 * - Plays audio for 5 seconds while logging position updates, then stops.
 */
public final class EngineSmokeTest {

    private EngineSmokeTest() {
        // Dev-only entry point
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" [DEV-ONLY] RetroAmp98 MediaEngine Smoke Test    ");
        System.out.println("=================================================");

        if (args.length < 1) {
            System.err.println("Error: Missing media file argument.");
            System.err.println("Usage:");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"io.github.nikhilvirdi.retroamp98.engine.EngineSmokeTest\" -Dexec.args=\"<path-to-media-file>\"");
            System.exit(1);
            return;
        }

        Path mediaPath = Paths.get(args[0]).toAbsolutePath();
        System.out.println("[SmokeTest] Media file: " + mediaPath);

        MediaEngine engine = new JavaFxMediaEngine();

        // Register listener to monitor engine events
        engine.addListener(new EngineListener() {
            @Override
            public void onStateChanged(PlayerState state) {
                System.out.println("[SmokeTest EVENT] State changed -> " + state
                        + " (on EDT: " + SwingUtilities.isEventDispatchThread() + ")");
            }

            @Override
            public void onPositionChanged(Duration position) {
                long totalSeconds = position.toSeconds();
                long millis = position.toMillisPart();
                System.out.printf("[SmokeTest EVENT] Position -> %02d:%02d.%03d (on EDT: %b)%n",
                        totalSeconds / 60, totalSeconds % 60, millis, SwingUtilities.isEventDispatchThread());
            }

            @Override
            public void onDurationKnown(Duration duration) {
                long totalSeconds = duration.toSeconds();
                System.out.printf("[SmokeTest EVENT] Duration known -> %02d:%02d (%d ms)%n",
                        totalSeconds / 60, totalSeconds % 60, duration.toMillis());
            }

            @Override
            public void onEndOfMedia() {
                System.out.println("[SmokeTest EVENT] End of media reached.");
            }

            @Override
            public void onError(String message) {
                System.err.println("[SmokeTest EVENT] Engine error -> " + message);
            }
        });

        System.out.println("[SmokeTest] 1. Loading media...");
        engine.load(mediaPath);

        System.out.println("[SmokeTest] 2. Starting playback...");
        engine.play();

        System.out.println("[SmokeTest] 3. Playing for 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[SmokeTest] 4. Stopping playback...");
        engine.stop();

        System.out.println("[SmokeTest] 5. Final state: " + engine.getState()
                + ", Final position: " + engine.getPosition().toMillis() + "ms");
        System.out.println("=================================================");
        System.out.println(" [DEV-ONLY] Smoke test completed successfully    ");
        System.out.println("=================================================");
        System.exit(0);
    }
}
