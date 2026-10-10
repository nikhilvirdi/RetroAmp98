package io.github.nikhilvirdi.retroamp98.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Creates a Java2D-rendered retro media jewel disc application icon.
 */
public final class AppIcon {

    private AppIcon() {
    }

    public static Image createIcon() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // CD / Disc metallic gradient
        LinearGradientPaint discGrad = new LinearGradientPaint(
                0, 0, size, size,
                new float[]{0.0f, 0.45f, 1.0f},
                new Color[]{new Color(200, 225, 250), new Color(135, 170, 215), new Color(70, 110, 175)}
        );
        g2.setPaint(discGrad);
        g2.fillOval(2, 2, size - 4, size - 4);

        // Outer rim
        g2.setColor(new Color(40, 70, 120));
        g2.drawOval(2, 2, size - 5, size - 5);

        // Specular arc highlight
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawArc(4, 4, size - 9, size - 9, 45, 90);

        // Inner track ring
        g2.setColor(new Color(110, 150, 200, 140));
        g2.drawOval(8, 8, size - 17, size - 17);

        // Center hub
        g2.setColor(new Color(20, 38, 70));
        g2.fillOval(11, 11, 10, 10);
        g2.setColor(new Color(210, 230, 255));
        g2.drawOval(11, 11, 9, 9);

        // Glowing green play accent
        int[] xp = {14, 14, 19};
        int[] yp = {13, 19, 16};
        g2.setColor(Theme.LED_PLAYING);
        g2.fillPolygon(xp, yp, 3);

        g2.dispose();
        return img;
    }
}
