package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
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
 * Windows Media Player 9 Series status strip situated directly below the video viewport.
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
        statusLabel.setFont(Theme.FONT_STATUS_TITLE);
        statusLabel.setForeground(Theme.STATUS_TEXT_PRIMARY);

        trackLabel = new JLabel(" - Ready");
        trackLabel.setFont(Theme.FONT_STATUS_TIME);
        trackLabel.setForeground(Theme.STATUS_TEXT_SECONDARY);

        leftPanel.add(ledIndicator);
        leftPanel.add(Box.createHorizontalStrut(6));
        leftPanel.add(statusLabel);
        leftPanel.add(trackLabel);

        // Right section: Time counter
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(Theme.FONT_STATUS_TIME);
        timeLabel.setForeground(Theme.STATUS_TEXT_PRIMARY);

        add(leftPanel, BorderLayout.WEST);
        add(timeLabel, BorderLayout.EAST);

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
            case IDLE -> statusLabel.setText("Ready");
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        String pos = TimeUtils.formatDuration(position);
        String dur = (duration != null && !duration.isZero()) ? TimeUtils.formatDuration(duration) : "00:00";
        timeLabel.setText(pos + " / " + dur);
    }

    @Override
    public void onDurationKnown(Duration duration) {
        timeLabel.setText("00:00 / " + TimeUtils.formatDuration(duration));
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        if (track != null) {
            trackLabel.setText(" - " + (index + 1) + ". " + track.getTitle());
        } else {
            trackLabel.setText(" - No Track Loaded");
            timeLabel.setText("00:00 / 00:00");
        }
    }

    /**
     * Small glowing square LED indicator showing active playback status.
     */
    private static class LedIndicator extends JComponent {
        private PlayerState state = PlayerState.STOPPED;

        public LedIndicator() {
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            setMaximumSize(new Dimension(10, 10));
        }

        public void setState(PlayerState state) {
            this.state = state;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = 7;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            java.awt.Color ledColor = switch (state) {
                case PLAYING -> Theme.LED_PLAYING;
                case PAUSED -> Theme.LED_PAUSED;
                case LOADING -> Theme.LED_LOADING;
                case ERROR -> Theme.LED_ERROR;
                case STOPPED, IDLE -> Theme.LED_STOPPED;
            };

            // Glow aura when playing
            if (state == PlayerState.PLAYING) {
                g2.setColor(new java.awt.Color(0, 230, 77, 70));
                g2.fillRoundRect(x - 2, y - 2, size + 4, size + 4, 3, 3);
            }

            // Square LED
            g2.setColor(ledColor);
            g2.fillRect(x, y, size, size);

            // Specular highlight dot
            g2.setColor(new java.awt.Color(255, 255, 255, 140));
            g2.fillRect(x, y, 2, 2);

            // Dark border
            g2.setColor(Theme.STATUS_BG_TOP);
            g2.drawRect(x, y, size, size);

            g2.dispose();
        }
    }
}
