package io.github.nikhilvirdi.retroamp98;

import io.github.nikhilvirdi.retroamp98.engine.JavaFxMediaEngine;
import io.github.nikhilvirdi.retroamp98.engine.MediaEngine;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.ui.MainWindow;
import io.github.nikhilvirdi.retroamp98.ui.PlayerController;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application bootstrap for RetroAmp98.
 * Sets the Windows Classic Look and Feel, initializes the media engine,
 * playlist manager, controller, and displays the main window on the EDT.
 */
public class RetroAmpApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();

            // Initialize media engine and playlist layers
            MediaEngine engine = new JavaFxMediaEngine();
            PlaylistManager playlistManager = new PlaylistManager();

            // Wire controller and UI
            PlayerController controller = new PlayerController(engine, playlistManager);
            MainWindow mainWindow = new MainWindow(controller);

            mainWindow.setVisible(true);
        });
    }

    /**
     * Tries Windows Classic Look and Feel first, falling back to System LAF,
     * then cross-platform LAF.
     */
    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {
                }
            }
        }
    }
}
