package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.TimeUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Duration;

/**
 * Windows Media Player 9 Series status strip situated below the LCD display.
 * Features a glowing LED indicator, state status text, current track title, and duration counters.
 */
public class StatusStrip extends JPanel implements ControllerListener {

    private final LedIndicator ledIndicator;
    private final JLabel statusLabel;
    private final JLabel trackLabel;
    private final JLabel timeLabel;

    public StatusStrip(PlayerController controller) {
        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(0, 24));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.STATUS_BORDER),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        // Left section: LED + State + Track Title
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));

        ledIndicator = new LedIndicator();
        statusLabel = new JLabel("Stopped");
        statusLabel.setFont(Theme.FONT_STATUS_LCD);
        statusLabel.setForeground(Theme.STATUS_LCD_TEXT);

        trackLabel = new JLabel(" - Ready");
        trackLabel.setFont(Theme.FONT_STATUS_LCD);
        trackLabel.setForeground(Theme.STATUS_LCD_TEXT);

        leftPanel.add(ledIndicator);
        leftPanel.add(Box.createHorizontalStrut(6));
        leftPanel.add(statusLabel);
        leftPanel.add(trackLabel);

        // Right section: Time counter
        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));

        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(Theme.FONT_STATUS_LCD);
        timeLabel.setForeground(Theme.STATUS_TIME_TEXT);

        rightPanel.add(timeLabel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        controller.addListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // Dark metallic gradient bar
        GradientPaint barGrad = new GradientPaint(
                0, 0, Theme.STATUS_BG_TOP,
                0, h, Theme.STATUS_BG_BOTTOM
        );
        g2.setPaint(barGrad);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    @Override
    public void onStateChanged(PlayerState state) {
        ledIndicator.setState(state);
        switch (state) {
            case PLAYING -> statusLabel.setText("Playing");
            case PAUSED -> statusLabel.setText("Paused");
            case LOADING -> statusLabel.setText("Loading");
            case STOPPED -> {
                statusLabel.setText("Stopped");
                timeLabel.setText("00:00 / 00:00");
            }
            case ERROR -> statusLabel.setText("Error");
            case IDLE -> {
                statusLabel.setText("Ready");
                timeLabel.setText("00:00 / 00:00");
            }
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        String pos = TimeUtils.formatDuration(position);
        String dur = (duration != null && !duration.isZero()) ? TimeUtils.formatDuration(duration) : "--:--";
        timeLabel.setText(pos + " / " + dur);
    }

    @Override
    public void onDurationKnown(Duration duration) {
        String dur = TimeUtils.formatDuration(duration);
        timeLabel.setText("00:00 / " + dur);
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        if (track != null) {
            trackLabel.setText(" - " + track.getTitle());
        } else {
            trackLabel.setText(" - Ready");
            timeLabel.setText("00:00 / 00:00");
        }
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        if (trackInfo != null && trackInfo.getDisplayTitle() != null) {
            trackLabel.setText(" - " + trackInfo.getDisplayTitle());
        }
    }

    /**
     * Circular glowing hardware status LED.
     */
    private static class LedIndicator extends JComponent {
        private PlayerState state = PlayerState.IDLE;

        LedIndicator() {
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            setMaximumSize(new Dimension(10, 10));
        }

        void setState(PlayerState state) {
            this.state = state;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            java.awt.Color ledColor;
            boolean glow = false;

            switch (state) {
                case PLAYING -> {
                    ledColor = Theme.LED_PLAYING;
                    glow = true;
                }
                case PAUSED -> {
                    ledColor = Theme.LED_PAUSED;
                    glow = true;
                }
                case LOADING -> ledColor = Theme.LED_LOADING;
                case ERROR -> ledColor = Theme.LED_ERROR;
                default -> ledColor = Theme.LED_STOPPED;
            }

            int w = getWidth();
            int h = getHeight();
            int d = Math.min(w, h) - 2;
            int x = (w - d) / 2;
            int y = (h - d) / 2;

            if (glow) {
                g2.setColor(Theme.STATUS_LED_GLOW);
                g2.fillOval(x - 2, y - 2, d + 4, d + 4);
            }

            g2.setColor(ledColor);
            g2.fillOval(x, y, d, d);

            if (glow) {
                g2.setColor(Theme.STATUS_LED_HIGHLIGHT);
                g2.fillOval(x + 2, y + 1, d / 3, d / 3);
            }

            g2.dispose();
        }
    }
}
