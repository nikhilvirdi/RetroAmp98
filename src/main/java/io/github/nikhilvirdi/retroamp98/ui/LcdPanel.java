package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.time.Duration;

/**
 * Left poster panel for RetroAmp98.
 * Renders a rich deep red to near-black diagonal gradient with radial glow,
 * edge vignette, bottom-left bold logo, and a seamless 80px horizontal right fade
 * blending into the right control panel.
 */
public class LcdPanel extends JPanel implements ControllerListener {

    private static final String LOGO_TEXT = "RetroAmp98";

    public LcdPanel(PlayerController controller) {
        setOpaque(false);
        setMinimumSize(new Dimension(Theme.POSTER_MIN_WIDTH, Theme.WINDOW_MIN_HEIGHT));
        setPreferredSize(new Dimension(Theme.FOUR_HUNDRED_SIXTY, Theme.FOUR_HUNDRED_SIXTY));

        if (controller != null) {
            controller.addListener(this);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (w <= Theme.ZERO || h <= Theme.ZERO) {
            g2.dispose();
            return;
        }

        // 1. Diagonal gradient from deep red (top-left) to near-black (bottom-right)
        LinearGradientPaint diagonalGrad = new LinearGradientPaint(
                Theme.FLOAT_ZERO, Theme.FLOAT_ZERO,
                (float) w, (float) h,
                Theme.GRADIENT_STOPS_TWO,
                new Color[]{Theme.POSTER_TOP_LEFT, Theme.POSTER_BOTTOM_RIGHT}
        );
        g2.setPaint(diagonalGrad);
        g2.fillRect(Theme.ZERO, Theme.ZERO, w, h);

        // 2. Soft radial glow
        float glowRadius = Math.max(w, h) * Theme.FLOAT_GLOW_RADIUS;
        float glowCx = (float) w * Theme.FLOAT_GLOW_CX;
        float glowCy = (float) h * Theme.FLOAT_GLOW_CY;
        RadialGradientPaint glowPaint = new RadialGradientPaint(
                glowCx, glowCy, glowRadius,
                Theme.GRADIENT_STOPS_TWO,
                new Color[]{Theme.GLOW_COLOR_WITH_ALPHA, Theme.COLOR_TRANSPARENT}
        );
        g2.setPaint(glowPaint);
        g2.fillRect(Theme.ZERO, Theme.ZERO, w, h);

        // 3. Darker vignette around the edges
        float vignetteRadius = Math.max(w, h) * Theme.FLOAT_VIGNETTE_RADIUS;
        float vigCx = (float) w * Theme.FLOAT_HALF;
        float vigCy = (float) h * Theme.FLOAT_HALF;
        RadialGradientPaint vignettePaint = new RadialGradientPaint(
                vigCx, vigCy, vignetteRadius,
                Theme.GRADIENT_STOPS_TWO,
                new Color[]{Theme.COLOR_TRANSPARENT, Theme.VIGNETTE_COLOR_WITH_ALPHA}
        );
        g2.setPaint(vignettePaint);
        g2.fillRect(Theme.ZERO, Theme.ZERO, w, h);

        // 4. Horizontal right-edge fade into PANEL_BG (over Theme.FADE_WIDTH)
        int fadeX = Math.max(Theme.ZERO, w - Theme.FADE_WIDTH);
        LinearGradientPaint rightEdgeFade = new LinearGradientPaint(
                (float) fadeX, Theme.FLOAT_ZERO,
                (float) w, Theme.FLOAT_ZERO,
                Theme.GRADIENT_STOPS_TWO,
                new Color[]{Theme.POSTER_FADE_START, Theme.POSTER_FADE_END}
        );
        g2.setPaint(rightEdgeFade);
        g2.fillRect(fadeX, Theme.ZERO, Theme.FADE_WIDTH, h);

        // 5. Draw "RetroAmp98" logo in white, bottom-left, bold sans-serif
        g2.setFont(Theme.FONT_POSTER_LOGO);
        g2.setColor(Theme.POSTER_LOGO_COLOR);
        g2.drawString(LOGO_TEXT, Theme.POSTER_LOGO_X, h - Theme.POSTER_LOGO_Y);

        g2.dispose();
    }

    @Override
    public void onStateChanged(PlayerState state) {
        // Visual poster update if needed
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        // Visual poster update if needed
    }

    @Override
    public void onDurationKnown(Duration duration) {
        // Visual poster update if needed
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        repaint();
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        repaint();
    }
}
