package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Windows Media Player 9 Series styled slider.
 * Renders a slim recessed metallic track with cyan fill and a glossy rounded silver-blue thumb.
 */
public class SkinSlider extends JSlider {

    public SkinSlider(int min, int max, int value) {
        super(min, max, value);
        setOpaque(false);
        setFocusable(false);
        setUI(new SkinSliderUI(this));
    }

    private static class SkinSliderUI extends BasicSliderUI {

        private static final int TRACK_HEIGHT = 4;
        private static final int THUMB_SIZE = 12;

        public SkinSliderUI(JSlider b) {
            super(b);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(THUMB_SIZE, THUMB_SIZE);
        }

        @Override
        public void paintFocus(Graphics g) {
            // Suppress default focus rectangle
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle tr = trackRect;
            int ty = tr.y + (tr.height - TRACK_HEIGHT) / 2;

            // Recessed background groove
            RoundRectangle2D groove = new RoundRectangle2D.Double(tr.x, ty, tr.width, TRACK_HEIGHT, 4, 4);
            g2.setColor(Theme.SLIDER_TRACK_BG);
            g2.fill(groove);
            g2.setColor(Theme.SLIDER_TRACK_BORDER);
            g2.draw(groove);

            // Active elapsed fill from track start to thumb center
            int thumbX = thumbRect.x + thumbRect.width / 2;
            int fillWidth = Math.max(0, thumbX - tr.x);
            if (fillWidth > 0) {
                RoundRectangle2D fill = new RoundRectangle2D.Double(tr.x, ty, fillWidth, TRACK_HEIGHT, 4, 4);
                g2.setPaint(new GradientPaint(
                        tr.x, ty, Theme.SLIDER_TRACK_FILL,
                        tr.x, ty + TRACK_HEIGHT, Theme.SLIDER_TRACK_FILL.darker()
                ));
                g2.fill(fill);
            }

            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle tr = thumbRect;
            int x = tr.x;
            int y = tr.y;

            // Soft drop shadow
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(x, y + 1, THUMB_SIZE, THUMB_SIZE);

            // Metallic silver-blue thumb gradient
            GradientPaint thumbGrad = new GradientPaint(
                    x, y, Theme.SLIDER_THUMB_TOP,
                    x, y + THUMB_SIZE, Theme.SLIDER_THUMB_BOTTOM
            );
            g2.setPaint(thumbGrad);
            g2.fillOval(x, y, THUMB_SIZE, THUMB_SIZE);

            // Specular gloss arc
            GradientPaint gloss = new GradientPaint(
                    x, y, new Color(255, 255, 255, 220),
                    x, y + THUMB_SIZE / 2.0f, new Color(255, 255, 255, 20)
            );
            g2.setPaint(gloss);
            g2.fill(new Ellipse2D.Double(x + 2, y + 1, THUMB_SIZE - 4, (THUMB_SIZE / 2.0) - 1));

            // Outer border ring
            g2.setColor(Theme.SLIDER_THUMB_BORDER);
            g2.drawOval(x, y, THUMB_SIZE, THUMB_SIZE);

            g2.dispose();
        }
    }
}
