package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Visual Windows XP window resize grip painted in the bottom-right corner.
 * Features 3-step diagonal embossed dots.
 */
public class ResizeGrip extends JComponent {

    public ResizeGrip() {
        setPreferredSize(new Dimension(14, 14));
        setMinimumSize(new Dimension(14, 14));
        setMaximumSize(new Dimension(14, 14));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();

        // 3x3 diagonal triangle pattern of grip dots
        int[] xs = {w - 4, w - 8, w - 4, w - 12, w - 8, w - 4};
        int[] ys = {h - 4, h - 4, h - 8, h - 4, h - 8, h - 12};

        for (int i = 0; i < xs.length; i++) {
            // Light highlight dot
            g2.setColor(Theme.GRIP_DOT_LIGHT);
            g2.fillRect(xs[i] - 1, ys[i] - 1, 2, 2);

            // Dark drop shadow dot
            g2.setColor(Theme.GRIP_DOT_DARK);
            g2.fillRect(xs[i], ys[i], 2, 2);
        }

        g2.dispose();
    }
}
