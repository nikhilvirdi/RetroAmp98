package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom dark slider for RetroAmp98 progress and volume controls.
 * Features a slim 3px track in Theme.PROGRESS_TRACK, active fill,
 * and a white circular thumb shown exclusively on hover or dragging.
 */
public class SkinSlider extends JSlider {

    private boolean isHovered = false;
    private Color fillColor = Theme.ACCENT;

    public SkinSlider(int min, int max, int value) {
        this(JSlider.HORIZONTAL, min, max, value);
    }

    public SkinSlider(int orientation, int min, int max, int value) {
        super(orientation, min, max, value);
        setOpaque(false);
        setFocusable(false);
        setUI(new SkinSliderUI(this));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    public Color getFillColor() {
        return fillColor;
    }

    public boolean isHovered() {
        return isHovered;
    }

    private static class SkinSliderUI extends BasicSliderUI {

        public SkinSliderUI(JSlider b) {
            super(b);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(Theme.THUMB_SIZE, Theme.THUMB_SIZE);
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
            int ty = tr.y + (tr.height - Theme.PROGRESS_TRACK_HEIGHT) / Theme.TWO;

            // Recessed dark track line
            RoundRectangle2D groove = new RoundRectangle2D.Double(
                    tr.x, ty, tr.width, Theme.PROGRESS_TRACK_HEIGHT,
                    Theme.PROGRESS_TRACK_HEIGHT, Theme.PROGRESS_TRACK_HEIGHT
            );
            g2.setColor(Theme.PROGRESS_TRACK);
            g2.fill(groove);

            // Active elapsed / volume fill
            int thumbX = thumbRect.x + thumbRect.width / Theme.TWO;
            int fillWidth = Math.max(Theme.ZERO, thumbX - tr.x);
            if (fillWidth > Theme.ZERO) {
                Color fill = (slider instanceof SkinSlider ss) ? ss.getFillColor() : Theme.ACCENT;
                RoundRectangle2D activeFill = new RoundRectangle2D.Double(
                        tr.x, ty, fillWidth, Theme.PROGRESS_TRACK_HEIGHT,
                        Theme.PROGRESS_TRACK_HEIGHT, Theme.PROGRESS_TRACK_HEIGHT
                );
                g2.setColor(fill);
                g2.fill(activeFill);
            }

            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            boolean hovered = (slider instanceof SkinSlider ss) && ss.isHovered();
            boolean adjusting = slider.getValueIsAdjusting();

            // Only display thumb during hover or active dragging
            if (hovered || adjusting) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Rectangle tr = thumbRect;
                int x = tr.x + (tr.width - Theme.THUMB_SIZE) / Theme.TWO;
                int y = tr.y + (tr.height - Theme.THUMB_SIZE) / Theme.TWO;

                // Subtle drop shadow
                g2.setColor(Theme.THUMB_SHADOW);
                g2.fillOval(x, y + Theme.ONE, Theme.THUMB_SIZE, Theme.THUMB_SIZE);

                // Round white thumb
                g2.setColor(Theme.THUMB_COLOR);
                g2.fillOval(x, y, Theme.THUMB_SIZE, Theme.THUMB_SIZE);

                g2.dispose();
            }
        }
    }
}
