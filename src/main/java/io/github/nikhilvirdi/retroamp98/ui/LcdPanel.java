package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.TimeUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;

/**
 * Retro Windows 98 / Winamp LCD display panel.
 * Dark background with phosphor green monospaced text displaying track info and time.
 */
public class LcdPanel extends JPanel implements ControllerListener {

    private final JLabel titleLabel;
    private final JLabel statusLabel;
    private final JLabel timeLabel;

    public LcdPanel(PlayerController controller) {
        setLayout(new BorderLayout(0, 4));
        setBackground(Theme.STATUS_BG_TOP);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        Font monoBoldLarge = new Font("Monospaced", Font.BOLD, 13);
        Font monoBoldSmall = new Font("Monospaced", Font.BOLD, 11);

        // Title row
        titleLabel = new JLabel("RetroAmp98 - Ready", SwingConstants.LEFT);
        titleLabel.setFont(monoBoldLarge);
        titleLabel.setForeground(Theme.STATUS_LCD_TEXT);
        add(titleLabel, BorderLayout.NORTH);

        // Bottom status row (State on left, Time on right)
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);

        statusLabel = new JLabel("[IDLE]", SwingConstants.LEFT);
        statusLabel.setFont(monoBoldSmall);
        statusLabel.setForeground(Theme.STATUS_LCD_DIM);
        statusRow.add(statusLabel, BorderLayout.WEST);

        timeLabel = new JLabel("00:00 / 00:00", SwingConstants.RIGHT);
        timeLabel.setFont(monoBoldLarge);
        timeLabel.setForeground(Theme.STATUS_LCD_TEXT);
        statusRow.add(timeLabel, BorderLayout.EAST);

        add(statusRow, BorderLayout.SOUTH);

        controller.addListener(this);
    }

    @Override
    public void onStateChanged(PlayerState state) {
        statusLabel.setText("[" + state.name() + "]");
        if (state == PlayerState.STOPPED || state == PlayerState.IDLE) {
            timeLabel.setText("00:00 / 00:00");
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        String posStr = TimeUtils.formatDuration(position);
        String durStr = (duration != null && !duration.isZero()) ? TimeUtils.formatDuration(duration) : "--:--";
        timeLabel.setText(posStr + " / " + durStr);
    }

    @Override
    public void onDurationKnown(Duration duration) {
        String durStr = TimeUtils.formatDuration(duration);
        timeLabel.setText("00:00 / " + durStr);
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        if (track != null) {
            titleLabel.setText((index + 1) + ". " + track.getTitle());
        } else {
            titleLabel.setText("RetroAmp98 - No Track Loaded");
            timeLabel.setText("00:00 / 00:00");
        }
    }

    @Override
    public void onTrackInfoReady(io.github.nikhilvirdi.retroamp98.engine.TrackInfo trackInfo) {
        if (trackInfo != null && trackInfo.getDisplayTitle() != null) {
            titleLabel.setText(trackInfo.getDisplayTitle());
        }
    }
}
