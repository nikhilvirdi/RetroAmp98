package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern dark-styled vector icon and pill button for RetroAmp98.
 * Supports Play/Pause (large white circle), Shuffle, Prev, Next, Repeat (with "1" badge),
 * Mute icon, and small rounded "PL" pill button.
 * Drawn entirely with Java2D paths and Theme constants.
 */
public class RoundGlossButton extends JButton {

    public enum IconType {
        PLAY_PAUSE,
        PLAY,
        PAUSE,
        STOP,
        PREV,
        NEXT,
        SHUFFLE,
        REPEAT,
        MUTE,
        PL,
        TEXT_ONLY
    }

    private IconType iconType;
    private boolean isCircular;
    private boolean isHovered = false;
    private boolean isSelectedToggle = false;
    private boolean isPlaying = false;
    private boolean isRepeatOne = false;
    private boolean isMuted = false;

    public RoundGlossButton(IconType iconType, int diameter, String tooltip) {
        this.iconType = iconType;
        this.isCircular = true;
        setPreferredSize(new Dimension(diameter, diameter));
        setMinimumSize(new Dimension(diameter, diameter));
        setMaximumSize(new Dimension(diameter, diameter));
        initButton(tooltip);
    }

    public RoundGlossButton(IconType iconType, int width, int height, String tooltip) {
        this.iconType = iconType;
        this.isCircular = false;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        initButton(tooltip);
    }

    public RoundGlossButton(String text, int width, int height, String tooltip) {
        super(text);
        this.iconType = IconType.TEXT_ONLY;
        this.isCircular = false;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
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

    public void setIconType(IconType iconType) {
        this.iconType = iconType;
        repaint();
    }

    public void setSelectedToggle(boolean selected) {
        this.isSelectedToggle = selected;
        repaint();
    }

    public boolean isSelectedToggle() {
        return isSelectedToggle;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        repaint();
    }

    public void setRepeatOne(boolean repeatOne) {
        this.isRepeatOne = repeatOne;
        repaint();
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isPressed();

        if (iconType == IconType.PLAY_PAUSE || (iconType == IconType.PLAY && isCircular && w >= Theme.FORTY)) {
            paintLargePlayPauseCircle(g2, w, h, pressed);
        } else if (iconType == IconType.PL) {
            paintPlButton(g2, w, h, pressed);
        } else if (isCircular) {
            paintTransportCircle(g2, w, h, pressed);
        } else {
            paintGenericPill(g2, w, h, pressed);
        }

        paintGlyph(g2, w, h, pressed);

        g2.dispose();
    }

    private void paintLargePlayPauseCircle(Graphics2D g2, int w, int h, boolean pressed) {
        int size = Math.min(w, h) - Theme.TWO;
        int x = (w - size) / Theme.TWO;
        int y = (h - size) / Theme.TWO;

        Color bg = pressed ? Theme.PLAY_BUTTON_PRESSED : (isHovered ? Theme.PLAY_BUTTON_HOVER : Theme.PLAY_BUTTON_BG);
        g2.setColor(bg);
        g2.fillOval(x, y, size, size);
    }

    private void paintTransportCircle(Graphics2D g2, int w, int h, boolean pressed) {
        if (pressed) {
            g2.setColor(Theme.BTN_BG_PRESSED);
            g2.fillOval(Theme.ONE, Theme.ONE, w - Theme.TWO, h - Theme.TWO);
        } else if (isHovered) {
            g2.setColor(Theme.BTN_BG_HOVER);
            g2.fillOval(Theme.ONE, Theme.ONE, w - Theme.TWO, h - Theme.TWO);
        }
    }

    private void paintPlButton(Graphics2D g2, int w, int h, boolean pressed) {
        Color bg = isSelectedToggle ? Theme.BTN_PILL_ACTIVE_BG : (pressed ? Theme.BTN_PILL_PRESSED : (isHovered ? Theme.BTN_PILL_HOVER : Theme.BTN_PILL_BG));
        Color border = isSelectedToggle ? Theme.BTN_PILL_ACTIVE_BORDER : Theme.BTN_PILL_BORDER;

        RoundRectangle2D pill = new RoundRectangle2D.Double(
                Theme.ONE, Theme.ONE,
                w - Theme.TWO, h - Theme.TWO,
                Theme.RADIUS_SMALL, Theme.RADIUS_SMALL
        );
        g2.setColor(bg);
        g2.fill(pill);

        g2.setColor(border);
        g2.setStroke(Theme.STROKE_ONE);
        g2.draw(pill);
    }

    private void paintGenericPill(Graphics2D g2, int w, int h, boolean pressed) {
        Color bg = pressed ? Theme.BTN_PILL_PRESSED : (isHovered ? Theme.BTN_PILL_HOVER : Theme.BTN_PILL_BG);
        Color border = Theme.BTN_PILL_BORDER;

        RoundRectangle2D pill = new RoundRectangle2D.Double(
                Theme.ONE, Theme.ONE,
                w - Theme.TWO, h - Theme.TWO,
                Theme.RADIUS_SMALL, Theme.RADIUS_SMALL
        );
        g2.setColor(bg);
        g2.fill(pill);

        g2.setColor(border);
        g2.setStroke(Theme.STROKE_ONE);
        g2.draw(pill);
    }

    private void paintGlyph(Graphics2D g2, int w, int h, boolean pressed) {
        int cx = w / Theme.TWO;
        int cy = h / Theme.TWO + (pressed ? Theme.ONE : Theme.ZERO);

        Color glyphColor;
        if (isSelectedToggle) {
            glyphColor = Theme.ACCENT;
        } else if (isHovered) {
            glyphColor = Theme.BTN_ICON_HOVER;
        } else {
            glyphColor = Theme.BTN_ICON_NORMAL;
        }

        switch (iconType) {
            case PLAY_PAUSE -> {
                g2.setColor(Theme.PLAY_BUTTON_ICON);
                if (isPlaying) {
                    paintPauseBars(g2, cx, cy);
                } else {
                    paintPlayTriangle(g2, cx, cy);
                }
            }
            case PLAY -> {
                g2.setColor(w >= Theme.FORTY ? Theme.PLAY_BUTTON_ICON : glyphColor);
                paintPlayTriangle(g2, cx, cy);
            }
            case PAUSE -> {
                g2.setColor(w >= Theme.FORTY ? Theme.PLAY_BUTTON_ICON : glyphColor);
                paintPauseBars(g2, cx, cy);
            }
            case STOP -> {
                g2.setColor(glyphColor);
                g2.fillRect(cx - Theme.FIVE, cy - Theme.FIVE, Theme.TEN, Theme.TEN);
            }
            case PREV -> {
                g2.setColor(glyphColor);
                g2.fillRect(cx - Theme.SEVEN, cy - Theme.SIX, Theme.TWO, Theme.TWELVE);
                Polygon tri = new Polygon(
                        new int[]{cx + Theme.SIX, cx - Theme.FOUR, cx + Theme.SIX},
                        new int[]{cy - Theme.SIX, cy, cy + Theme.SIX},
                        Theme.THREE
                );
                g2.fill(tri);
            }
            case NEXT -> {
                g2.setColor(glyphColor);
                Polygon tri = new Polygon(
                        new int[]{cx - Theme.SIX, cx + Theme.FOUR, cx - Theme.SIX},
                        new int[]{cy - Theme.SIX, cy, cy + Theme.SIX},
                        Theme.THREE
                );
                g2.fill(tri);
                g2.fillRect(cx + Theme.FIVE, cy - Theme.SIX, Theme.TWO, Theme.TWELVE);
            }
            case SHUFFLE -> {
                g2.setColor(glyphColor);
                g2.setStroke(Theme.STROKE_TWO);

                // Upper curved path
                Path2D path1 = new Path2D.Double();
                path1.moveTo(cx - Theme.EIGHT, cy + Theme.FIVE);
                path1.curveTo(cx - Theme.TWO, cy + Theme.FIVE, cx + Theme.TWO, cy - Theme.FIVE, cx + Theme.EIGHT, cy - Theme.FIVE);
                g2.draw(path1);

                // Lower curved path
                Path2D path2 = new Path2D.Double();
                path2.moveTo(cx - Theme.EIGHT, cy - Theme.FIVE);
                path2.curveTo(cx - Theme.TWO, cy - Theme.FIVE, cx + Theme.TWO, cy + Theme.FIVE, cx + Theme.EIGHT, cy + Theme.FIVE);
                g2.draw(path2);

                // Arrowheads
                g2.setStroke(Theme.STROKE_ONE);
                Polygon arrow1 = new Polygon(
                        new int[]{cx + Theme.FIVE, cx + Theme.NINE, cx + Theme.FIVE},
                        new int[]{cy - Theme.EIGHT, cy - Theme.FIVE, cy - Theme.TWO},
                        Theme.THREE
                );
                g2.fill(arrow1);

                Polygon arrow2 = new Polygon(
                        new int[]{cx + Theme.FIVE, cx + Theme.NINE, cx + Theme.FIVE},
                        new int[]{cy + Theme.TWO, cy + Theme.FIVE, cy + Theme.EIGHT},
                        Theme.THREE
                );
                g2.fill(arrow2);
            }
            case REPEAT -> {
                g2.setColor(glyphColor);
                g2.setStroke(Theme.STROKE_TWO);

                // Top loop line
                g2.drawLine(cx - Theme.FIVE, cy - Theme.FIVE, cx + Theme.FOUR, cy - Theme.FIVE);
                g2.drawLine(cx + Theme.FOUR, cy - Theme.FIVE, cx + Theme.SIX, cy - Theme.THREE);
                g2.drawLine(cx + Theme.SIX, cy - Theme.THREE, cx + Theme.SIX, cy - Theme.ONE);

                // Bottom loop line
                g2.drawLine(cx + Theme.FIVE, cy + Theme.FIVE, cx - Theme.FOUR, cy + Theme.FIVE);
                g2.drawLine(cx - Theme.FOUR, cy + Theme.FIVE, cx - Theme.SIX, cy + Theme.THREE);
                g2.drawLine(cx - Theme.SIX, cy + Theme.THREE, cx - Theme.SIX, cy + Theme.ONE);

                // Arrowhead top
                g2.setStroke(Theme.STROKE_ONE);
                Polygon arrowTop = new Polygon(
                        new int[]{cx + Theme.TWO, cx + Theme.SIX, cx + Theme.SIX},
                        new int[]{cy - Theme.EIGHT, cy - Theme.FIVE, cy - Theme.ONE},
                        Theme.THREE
                );
                g2.fill(arrowTop);

                // Arrowhead bottom
                Polygon arrowBottom = new Polygon(
                        new int[]{cx - Theme.TWO, cx - Theme.SIX, cx - Theme.SIX},
                        new int[]{cy + Theme.EIGHT, cy + Theme.FIVE, cy + Theme.ONE},
                        Theme.THREE
                );
                g2.fill(arrowBottom);

                // Badge "1" if repeat mode is ONE
                if (isRepeatOne) {
                    g2.setFont(Theme.FONT_BADGE);
                    g2.setColor(Theme.ACCENT);
                    FontMetrics bfm = g2.getFontMetrics();
                    int bw = bfm.stringWidth("1");
                    g2.drawString("1", cx - bw / Theme.TWO, cy + Theme.THREE);
                }
            }
            case MUTE -> {
                g2.setColor(isMuted ? Theme.ACCENT : glyphColor);

                // Speaker base rectangle
                g2.fillRect(cx - Theme.SIX, cy - Theme.THREE, Theme.THREE, Theme.SIX);

                // Speaker cone polygon
                Polygon cone = new Polygon(
                        new int[]{cx - Theme.THREE, cx + Theme.ONE, cx + Theme.ONE, cx - Theme.THREE},
                        new int[]{cy - Theme.THREE, cy - Theme.SIX, cy + Theme.SIX, cy + Theme.THREE},
                        Theme.FOUR
                );
                g2.fill(cone);

                if (isMuted) {
                    // Draw 'X' mute indicator
                    g2.setStroke(Theme.STROKE_TWO);
                    g2.drawLine(cx + Theme.FOUR, cy - Theme.THREE, cx + Theme.EIGHT, cy + Theme.THREE);
                    g2.drawLine(cx + Theme.EIGHT, cy - Theme.THREE, cx + Theme.FOUR, cy + Theme.THREE);
                } else {
                    // Draw sound wave arcs
                    g2.setStroke(Theme.STROKE_ONE);
                    g2.drawArc(cx + Theme.ONE, cy - Theme.FOUR, Theme.FOUR, Theme.EIGHT, -Theme.FORTY_FIVE, Theme.NINETY);
                    g2.drawArc(cx + Theme.THREE, cy - Theme.SIX, Theme.SIX, Theme.TWELVE, -Theme.FORTY_FIVE, Theme.NINETY);
                }
            }
            case PL -> {
                g2.setFont(Theme.FONT_BUTTON_LABEL);
                g2.setColor(isSelectedToggle ? Theme.BTN_PILL_ACTIVE_TEXT : Theme.BTN_PILL_TEXT);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth("PL");
                int ty = cy + fm.getAscent() / Theme.TWO - Theme.ONE;
                g2.drawString("PL", cx - tw / Theme.TWO, ty);
            }
            case TEXT_ONLY -> {
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    g2.setFont(Theme.FONT_BUTTON_LABEL);
                    g2.setColor(Theme.BTN_PILL_TEXT);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(text);
                    int ty = cy + fm.getAscent() / Theme.TWO - Theme.ONE;
                    g2.drawString(text, cx - tw / Theme.TWO, ty);
                }
            }
        }
    }

    private void paintPlayTriangle(Graphics2D g2, int cx, int cy) {
        Polygon tri = new Polygon(
                new int[]{cx - Theme.FOUR, cx + Theme.EIGHT, cx - Theme.FOUR},
                new int[]{cy - Theme.SEVEN, cy, cy + Theme.SEVEN},
                Theme.THREE
        );
        g2.fill(tri);
    }

    private void paintPauseBars(Graphics2D g2, int cx, int cy) {
        int bw = Theme.FOUR;
        int bh = Theme.FOURTEEN;
        int gap = Theme.FOUR;
        g2.fillRoundRect(cx - bw - gap / Theme.TWO, cy - bh / Theme.TWO, bw, bh, Theme.TWO, Theme.TWO);
        g2.fillRoundRect(cx + gap / Theme.TWO, cy - bh / Theme.TWO, bw, bh, Theme.TWO, Theme.TWO);
    }
}
