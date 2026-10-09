package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom glossy round button rendering Windows Media Player 9 Series jewel controls.
 * Features multi-stop metallic gradients, glossy specular reflections, hover highlights,
 * and vector-drawn transport icons.
 */
public class RoundGlossButton extends JButton {

    public enum IconType {
        PLAY,
        PAUSE,
        STOP,
        PREV,
        NEXT,
        TEXT_ONLY
    }

    private IconType iconType;
    private final boolean isCircular;
    private boolean isHovered = false;
    private boolean isSelectedToggle = false;

    public void setIconType(IconType iconType) {
        this.iconType = iconType;
        repaint();
    }

    public RoundGlossButton(IconType iconType, int diameter, String tooltip) {
        this.iconType = iconType;
        this.isCircular = true;
        setPreferredSize(new Dimension(diameter, diameter));
        setMinimumSize(new Dimension(diameter, diameter));
        setMaximumSize(new Dimension(diameter, diameter));
        initButton(tooltip);
    }

    public RoundGlossButton(String text, int width, int height, String tooltip) {
        super(text);
        this.iconType = IconType.TEXT_ONLY;
        this.isCircular = false;
        setPreferredSize(new Dimension(width, height));
        initButton(tooltip);
    }

    private void initButton(String tooltip) {
        setToolTipText(tooltip);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(Theme.FONT_BUTTON_LABEL);

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

    public void setSelectedToggle(boolean selected) {
        this.isSelectedToggle = selected;
        repaint();
    }

    public boolean isSelectedToggle() {
        return isSelectedToggle;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isPressed() || isSelectedToggle;

        int size = Math.min(w, h) - 2;
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        if (isCircular) {
            paintCircularButton(g2, x, y, size, pressed);
        } else {
            paintPillButton(g2, 1, 1, w - 2, h - 2, pressed);
        }

        // Paint transport icon or text
        paintContent(g2, w, h, pressed);

        g2.dispose();
    }

    private void paintCircularButton(Graphics2D g2, int x, int y, int size, boolean pressed) {
        // Outer dark shadow ring
        g2.setColor(new Color(0x15, 0x24, 0x35, 180));
        g2.fillOval(x, y + 1, size, size);

        // Base metallic gradient
        Color top = pressed ? Theme.BTN_PRESSED_TOP : (isHovered ? Theme.BTN_HOVER_GLOW : Theme.BTN_GRAD_TOP);
        Color mid = pressed ? Theme.BTN_PRESSED_BOTTOM : Theme.BTN_GRAD_MID;
        Color bottom = pressed ? Theme.BTN_GRAD_MID : Theme.BTN_GRAD_BOTTOM;

        LinearGradientPaint baseGrad = new LinearGradientPaint(
                x, y, x, y + size,
                new float[]{0.0f, 0.45f, 1.0f},
                new Color[]{top, mid, bottom}
        );
        g2.setPaint(baseGrad);
        g2.fillOval(x, y, size, size);

        if (!pressed) {
            // Specular gloss dome on top half
            int glossW = size - 6;
            int glossH = (int) (size * 0.48);
            int glossX = x + 3;
            int glossY = y + 2;

            GradientPaint glossGrad = new GradientPaint(
                    glossX, glossY, new Color(255, 255, 255, 200),
                    glossX, glossY + glossH, new Color(255, 255, 255, 10)
            );
            g2.setPaint(glossGrad);
            g2.fill(new Ellipse2D.Double(glossX, glossY, glossW, glossH));
        }

        // Rim border
        g2.setColor(isHovered ? Theme.BTN_HOVER_GLOW : Theme.BTN_BORDER);
        g2.drawOval(x, y, size, size);
    }

    private void paintPillButton(Graphics2D g2, int x, int y, int w, int h, boolean pressed) {
        int arc = h;
        // Base metallic gradient
        Color top = pressed ? Theme.BTN_PRESSED_TOP : (isHovered ? Theme.BTN_HOVER_GLOW : Theme.BTN_GRAD_TOP);
        Color mid = pressed ? Theme.BTN_PRESSED_BOTTOM : Theme.BTN_GRAD_MID;
        Color bottom = pressed ? Theme.BTN_GRAD_MID : Theme.BTN_GRAD_BOTTOM;

        LinearGradientPaint baseGrad = new LinearGradientPaint(
                x, y, x, y + h,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{top, mid, bottom}
        );
        g2.setPaint(baseGrad);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));

        if (!pressed) {
            // Pill top gloss
            GradientPaint glossGrad = new GradientPaint(
                    x, y, new Color(255, 255, 255, 170),
                    x, y + h / 2.0f, new Color(255, 255, 255, 15)
            );
            g2.setPaint(glossGrad);
            g2.fill(new RoundRectangle2D.Double(x + 2, y + 1, w - 4, (h / 2.0) - 1, arc / 2.0, arc / 2.0));
        }

        g2.setColor(isHovered ? Theme.BTN_HOVER_GLOW : Theme.BTN_BORDER);
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    private void paintContent(Graphics2D g2, int w, int h, boolean pressed) {
        int cx = w / 2;
        int cy = h / 2 + (pressed ? 1 : 0);

        Color iconColor = isHovered ? Color.WHITE : Theme.BTN_ICON_NORMAL;
        Color shadowColor = Theme.BTN_ICON_SHADOW;

        switch (iconType) {
            case PLAY -> {
                int s = 6;
                Polygon shadow = new Polygon(
                        new int[]{cx - s + 1, cx + s + 1, cx - s + 1},
                        new int[]{cy - s + 1, cy + 1, cy + s + 1}, 3
                );
                Polygon tri = new Polygon(
                        new int[]{cx - s, cx + s, cx - s},
                        new int[]{cy - s, cy, cy + s}, 3
                );
                g2.setColor(shadowColor);
                g2.fill(shadow);
                g2.setColor(iconColor);
                g2.fill(tri);
            }
            case PAUSE -> {
                int bw = 3;
                int bh = 11;
                int gap = 3;
                // Shadow
                g2.setColor(shadowColor);
                g2.fillRect(cx - bw - gap / 2 + 1, cy - bh / 2 + 1, bw, bh);
                g2.fillRect(cx + gap / 2 + 1, cy - bh / 2 + 1, bw, bh);
                // Foreground
                g2.setColor(iconColor);
                g2.fillRect(cx - bw - gap / 2, cy - bh / 2, bw, bh);
                g2.fillRect(cx + gap / 2, cy - bh / 2, bw, bh);
            }
            case STOP -> {
                int sq = 9;
                g2.setColor(shadowColor);
                g2.fillRect(cx - sq / 2 + 1, cy - sq / 2 + 1, sq, sq);
                g2.setColor(iconColor);
                g2.fillRect(cx - sq / 2, cy - sq / 2, sq, sq);
            }
            case PREV -> {
                int s = 5;
                // Vertical bar
                g2.setColor(shadowColor);
                g2.fillRect(cx - s - 2 + 1, cy - s + 1, 2, s * 2);
                Polygon shadow = new Polygon(
                        new int[]{cx + s + 1, cx - s + 1, cx + s + 1},
                        new int[]{cy - s + 1, cy + 1, cy + s + 1}, 3
                );
                g2.fill(shadow);

                g2.setColor(iconColor);
                g2.fillRect(cx - s - 2, cy - s, 2, s * 2);
                Polygon tri = new Polygon(
                        new int[]{cx + s, cx - s, cx + s},
                        new int[]{cy - s, cy, cy + s}, 3
                );
                g2.fill(tri);
            }
            case NEXT -> {
                int s = 5;
                // Vertical bar
                g2.setColor(shadowColor);
                g2.fillRect(cx + s + 1, cy - s + 1, 2, s * 2);
                Polygon shadow = new Polygon(
                        new int[]{cx - s + 1, cx + s + 1, cx - s + 1},
                        new int[]{cy - s + 1, cy + 1, cy + s + 1}, 3
                );
                g2.fill(shadow);

                g2.setColor(iconColor);
                g2.fillRect(cx + s, cy - s, 2, s * 2);
                Polygon tri = new Polygon(
                        new int[]{cx - s, cx + s, cx - s},
                        new int[]{cy - s, cy, cy + s}, 3
                );
                g2.fill(tri);
            }
            case TEXT_ONLY -> {
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(text);
                    int th = fm.getAscent();
                    int tx = cx - tw / 2;
                    int ty = cy + th / 2 - 2;

                    // Text shadow
                    g2.setColor(shadowColor);
                    g2.drawString(text, tx + 1, ty + 1);
                    g2.setColor(iconColor);
                    g2.drawString(text, tx, ty);
                }
            }
        }
    }
}
