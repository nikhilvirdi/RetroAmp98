package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Visual window resize grip painted in the bottom-right corner.
 * Features 3-step diagonal subtle dots.
 */
public class ResizeGrip extends JComponent {

    public ResizeGrip() {
        setPreferredSize(new Dimension(Theme.FOURTEEN, Theme.FOURTEEN));
        setMinimumSize(new Dimension(Theme.FOURTEEN, Theme.FOURTEEN));
        setMaximumSize(new Dimension(Theme.FOURTEEN, Theme.FOURTEEN));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();

        // 3x3 diagonal triangle pattern of grip dots
        int[] xs = {
                w - Theme.FOUR, w - Theme.EIGHT, w - Theme.FOUR,
                w - Theme.TWELVE, w - Theme.EIGHT, w - Theme.FOUR
        };
        int[] ys = {
                h - Theme.FOUR, h - Theme.FOUR, h - Theme.EIGHT,
                h - Theme.FOUR, h - Theme.EIGHT, h - Theme.TWELVE
        };

        for (int i = Theme.ZERO; i < xs.length; i++) {
            // Light highlight dot
            g2.setColor(Theme.GRIP_DOT_LIGHT);
            g2.fillRect(xs[i] - Theme.ONE, ys[i] - Theme.ONE, Theme.TWO, Theme.TWO);

            // Dark drop shadow dot
            g2.setColor(Theme.GRIP_DOT_DARK);
            g2.fillRect(xs[i], ys[i], Theme.TWO, Theme.TWO);
        }

        g2.dispose();
    }
}
