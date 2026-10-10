package io.github.nikhilvirdi.retroamp98;

/**
 * Entry point launcher for RetroAmp98.
 * Avoids JavaFX runtime main class check when running from an executable shaded fat JAR.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        RetroAmpApp.main(args);
    }
}
