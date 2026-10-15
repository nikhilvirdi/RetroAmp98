package io.github.nikhilvirdi.retroamp98.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Creates a Java2D-rendered dark media disc application icon.
 * Styled with Theme constants and zero hardcoded literals.
 */
public final class AppIcon {

    private AppIcon() {
        // Prevent instantiation
    }

    public static Image createIcon() {
        int size = Theme.THIRTY_TWO;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // CD / Disc dark gradient
        LinearGradientPaint discGrad = new LinearGradientPaint(
                Theme.FLOAT_ZERO, Theme.FLOAT_ZERO, (float) size, (float) size,
                Theme.GRADIENT_STOPS_THREE,
                new Color[]{Theme.ICON_DISC_OUTER, Theme.ICON_DISC_MID, Theme.ICON_DISC_INNER}
        );
        g2.setPaint(discGrad);
        g2.fillOval(Theme.TWO, Theme.TWO, size - Theme.FOUR, size - Theme.FOUR);

        // Outer rim
        g2.setColor(Theme.ICON_RIM);
        g2.drawOval(Theme.TWO, Theme.TWO, size - Theme.FIVE, size - Theme.FIVE);

        // Specular arc highlight
        g2.setColor(Theme.ICON_HIGHLIGHT);
        g2.drawArc(Theme.FOUR, Theme.FOUR, size - Theme.NINE, size - Theme.NINE, Theme.FORTY_FIVE, Theme.NINETY);

        // Inner track ring
        g2.setColor(Theme.ICON_TRACK_RING);
        g2.drawOval(Theme.EIGHT, Theme.EIGHT, size - Theme.SEVENTEEN, size - Theme.SEVENTEEN);

        // Center hub
        g2.setColor(Theme.ICON_HUB);
        g2.fillOval(Theme.ELEVEN, Theme.ELEVEN, Theme.TEN, Theme.TEN);
        g2.setColor(Theme.ICON_INNER_RING);
        g2.drawOval(Theme.ELEVEN, Theme.ELEVEN, Theme.NINE, Theme.NINE);

        // Yellow accent play icon
        int[] xp = {Theme.FOURTEEN, Theme.FOURTEEN, Theme.NINETEEN};
        int[] yp = {Theme.THIRTEEN, Theme.NINETEEN, Theme.SIXTEEN};
        g2.setColor(Theme.ICON_PLAY);
        g2.fillPolygon(xp, yp, Theme.THREE);

        g2.dispose();
        return img;
    }
}
