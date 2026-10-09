package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Center display panel for video playback rendering.
 * Designed to scale content preserving aspect ratio (16:9, 4:3, vertical 9:16)
 * with automatic letterboxing/pillarboxing.
 * Independent of JavaFX types (uses standard AWT graphics).
 */
public class VideoPanel extends JPanel {

    private static final Font NO_VIDEO_FONT = new Font("Tahoma", Font.PLAIN, 13);

    private int videoWidth = 0;
    private int videoHeight = 0;
    private Image currentFrame = null;

    public VideoPanel() {
        setBackground(Theme.VIDEO_BG);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.VIDEO_FRAME_BORDER, 1),
                javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
    }

    /**
     * Sets native video dimensions for aspect ratio scaling calculations.
     */
    public void setVideoSize(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
        repaint();
    }

    /**
     * Updates the current video frame image to draw within the fitted viewport.
     */
    public void setFrame(Image frame, int width, int height) {
        this.currentFrame = frame;
        this.videoWidth = width;
        this.videoHeight = height;
        repaint();
    }

    /**
     * Clears any active video and resets to the "No video" placeholder.
     */
    public void clearVideo() {
        this.currentFrame = null;
        this.videoWidth = 0;
        this.videoHeight = 0;
        repaint();
    }

    /**
     * Computes the letterboxed/pillarboxed viewport rectangle inside this panel,
     * maintaining the video's native aspect ratio.
     * Works for standard 16:9, 4:3, vertical 9:16, or any arbitrary ratio.
     */
    public Rectangle calculateViewport(int panelWidth, int panelHeight) {
        if (videoWidth <= 0 || videoHeight <= 0 || panelWidth <= 0 || panelHeight <= 0) {
            return new Rectangle(0, 0, panelWidth, panelHeight);
        }

        double videoAspect = (double) videoWidth / videoHeight;
        double panelAspect = (double) panelWidth / panelHeight;

        int drawWidth;
        int drawHeight;

        if (videoAspect > panelAspect) {
            // Video is wider than panel -> letterbox (black bars top & bottom)
            drawWidth = panelWidth;
            drawHeight = (int) Math.round(panelWidth / videoAspect);
        } else {
            // Video is taller than panel -> pillarbox (black bars left & right)
            drawHeight = panelHeight;
            drawWidth = (int) Math.round(panelHeight * videoAspect);
        }

        int x = (panelWidth - drawWidth) / 2;
        int y = (panelHeight - drawHeight) / 2;

        return new Rectangle(x, y, drawWidth, drawHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // Always fill entire background with solid black
        g2.setColor(Theme.VIDEO_BG);
        g2.fillRect(0, 0, panelWidth, panelHeight);

        if (currentFrame != null && videoWidth > 0 && videoHeight > 0) {
            Rectangle vp = calculateViewport(panelWidth, panelHeight);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(currentFrame, vp.x, vp.y, vp.width, vp.height, null);
        } else {
            // Placeholder: centered "No video"
            g2.setColor(Theme.VIDEO_TEXT_COLOR);
            g2.setFont(NO_VIDEO_FONT);
            FontMetrics fm = g2.getFontMetrics();
            String message = "No video";
            int textWidth = fm.stringWidth(message);
            int textHeight = fm.getAscent();
            int x = (panelWidth - textWidth) / 2;
            int y = (panelHeight + textHeight) / 2 - 2;
            g2.drawString(message, x, y);
        }
    }
}
