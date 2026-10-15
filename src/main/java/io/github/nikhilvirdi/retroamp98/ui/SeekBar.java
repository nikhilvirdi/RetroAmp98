package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Duration;

/**
 * Seek bar slider for playback position tracking and seeking.
 * Renders a 3px accent line with hover/drag thumb and elapsed ("1:33") and
 * remaining ("-2:00") times below the track in Theme.TEXT_MUTED.
 * Isolates active seeking from engine updates to prevent fighting.
 */
public class SeekBar extends SkinSlider implements ControllerListener {

    private final PlayerController controller;
    private Duration currentDuration = Duration.ZERO;
    private Duration currentPosition = Duration.ZERO;
    private boolean isUserSeeking = false;

    public SeekBar(PlayerController controller) {
        super(Theme.ZERO, Theme.SEEK_RESOLUTION, Theme.ZERO);
        this.controller = controller;

        setFillColor(Theme.ACCENT);
        setPreferredSize(new Dimension(Theme.THREE_HUNDRED, Theme.THIRTY_TWO));
        setMinimumSize(new Dimension(Theme.TWO_HUNDRED, Theme.TWENTY_EIGHT));

        addChangeListener(e -> {
            if (getValueIsAdjusting()) {
                isUserSeeking = true;
                repaint();
            } else if (isUserSeeking) {
                isUserSeeking = false;
                if (currentDuration != null && !currentDuration.isZero()) {
                    long targetMillis = (long) ((getValue() / (double) Theme.SEEK_RESOLUTION) * currentDuration.toMillis());
                    this.controller.seek(Duration.ofMillis(targetMillis));
                }
                repaint();
            }
        });

        if (controller != null) {
            controller.addListener(this);
        }
    }

    private Duration calculateSeekPosition() {
        if (currentDuration == null || currentDuration.isZero()) {
            return Duration.ZERO;
        }
        long millis = (long) ((getValue() / (double) Theme.SEEK_RESOLUTION) * currentDuration.toMillis());
        return Duration.ofMillis(millis);
    }

    private String formatDurationTime(Duration d, boolean isNegative) {
        if (d == null || d.isNegative() || d.isZero()) {
            return isNegative ? "-0:00" : "0:00";
        }
        long totalSecs = d.toSeconds();
        long mins = totalSecs / Theme.SIXTY;
        long secs = totalSecs % Theme.SIXTY;
        String prefix = isNegative ? "-" : "";
        if (secs < Theme.TEN) {
            return prefix + mins + ":0" + secs;
        } else {
            return prefix + mins + ":" + secs;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(Theme.FONT_TIME);
        g2.setColor(Theme.TEXT_MUTED);

        Duration pos = isUserSeeking ? calculateSeekPosition() : currentPosition;
        Duration rem = Duration.ZERO;
        if (currentDuration != null && !currentDuration.isZero()) {
            rem = currentDuration.minus(pos);
            if (rem.isNegative()) {
                rem = Duration.ZERO;
            }
        }

        String elapsedStr = formatDurationTime(pos, false);
        String remainingStr = formatDurationTime(rem, true);

        FontMetrics fm = g2.getFontMetrics();
        int textY = getHeight() - fm.getDescent();

        g2.drawString(elapsedStr, Theme.ZERO, textY);
        int remWidth = fm.stringWidth(remainingStr);
        g2.drawString(remainingStr, Math.max(Theme.ZERO, getWidth() - remWidth), textY);

        g2.dispose();
    }

    @Override
    public void onStateChanged(PlayerState state) {
        if (state == PlayerState.STOPPED || state == PlayerState.IDLE) {
            if (!isUserSeeking) {
                setValue(Theme.ZERO);
                this.currentPosition = Duration.ZERO;
                repaint();
            }
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        this.currentDuration = duration;
        this.currentPosition = position;
        if (!isUserSeeking && duration != null && !duration.isZero()) {
            int progress = (int) ((position.toMillis() / (double) duration.toMillis()) * Theme.SEEK_RESOLUTION);
            setValue(Math.min(Theme.SEEK_RESOLUTION, Math.max(Theme.ZERO, progress)));
        }
        repaint();
    }

    @Override
    public void onDurationKnown(Duration duration) {
        this.currentDuration = duration;
        repaint();
    }
}
