package io.github.nikhilvirdi.retroamp98;

import javafx.embed.swing.JFXPanel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Main entry point for the RetroAmp98 desktop media player.
 * Initializes Swing UI with Windows Look and Feel and bootstraps JavaFX runtime.
 */
public class RetroAmpApp {

    public static void main(String[] args) {
        // Run UI initialization on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            initLookAndFeel();
            createAndShowGui();
        });
    }

    /**
     * Attempts to set the Windows Look and Feel with fallback to system default.
     */
    private static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Fallback to default Swing Look and Feel
            }
        }
    }

    /**
     * Creates and displays the main Swing JFrame and confirms JavaFX initializes via JFXPanel.
     */
    private static void createAndShowGui() {
        // Instantiate JFXPanel once to confirm JavaFX initializes alongside Swing
        new JFXPanel();

        JFrame frame = new JFrame("RetroAmp98");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(480, 260));

        JLabel label = new JLabel("RetroAmp98 - Windows 98 Media Player", SwingConstants.CENTER);
        frame.getContentPane().add(label, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
