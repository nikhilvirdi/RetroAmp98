package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Transport bar for RetroAmp98 right control panel.
 * Contains:
 * 1. Transport row: evenly spaced and vertically centered Shuffle, Previous,
 *    large white circle Play/Pause with dark glyph, Next, Repeat (with "1" badge).
 * 2. Slim bottom row: small icon-style Mute button on left, status text,
 *    and volume slider on right matching progress bar style.
 */
public class TransportBar extends JPanel implements ControllerListener {

    private final PlayerController controller;

    private final RoundGlossButton shuffleButton;
    private final RoundGlossButton prevButton;
    private final RoundGlossButton playPauseButton;
    private final RoundGlossButton nextButton;
    private final RoundGlossButton repeatButton;

    private final RoundGlossButton muteButton;
    private final SkinSlider volumeSlider;
    private final JLabel statusLabel;
    private final RoundGlossButton playlistButton;

    private java.util.function.Consumer<Boolean> playlistToggleListener;
    private RepeatMode currentRepeatMode = RepeatMode.OFF;

    public TransportBar(PlayerController controller) {
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.SIX, Theme.ZERO, Theme.FOUR, Theme.ZERO));

        // --- 1. Transport Row: Evenly spaced, vertically centered ---
        JPanel transportRow = new JPanel();
        transportRow.setLayout(new BoxLayout(transportRow, BoxLayout.X_AXIS));
        transportRow.setOpaque(false);
        transportRow.setBorder(BorderFactory.createEmptyBorder(Theme.FOUR, Theme.ZERO, Theme.FOUR, Theme.ZERO));

        shuffleButton = new RoundGlossButton(
                RoundGlossButton.IconType.SHUFFLE,
                Theme.TRANSPORT_BUTTON_SIZE,
                "Toggle Shuffle (S)"
        );
        shuffleButton.addActionListener(e -> {
            boolean nextVal = !shuffleButton.isSelectedToggle();
            shuffleButton.setSelectedToggle(nextVal);
            if (this.controller != null) {
                this.controller.setShuffle(nextVal);
            }
        });

        prevButton = new RoundGlossButton(
                RoundGlossButton.IconType.PREV,
                Theme.TRANSPORT_BUTTON_SIZE,
                "Previous Track (P)"
        );
        prevButton.addActionListener(e -> {
            if (this.controller != null) {
                this.controller.previous();
            }
        });

        playPauseButton = new RoundGlossButton(
                RoundGlossButton.IconType.PLAY_PAUSE,
                Theme.PLAY_BUTTON_SIZE,
                "Play / Pause (Space)"
        );
        playPauseButton.addActionListener(e -> {
            if (this.controller != null) {
                this.controller.playPause();
            }
        });

        nextButton = new RoundGlossButton(
                RoundGlossButton.IconType.NEXT,
                Theme.TRANSPORT_BUTTON_SIZE,
                "Next Track (N)"
        );
        nextButton.addActionListener(e -> {
            if (this.controller != null) {
                this.controller.next();
            }
        });

        repeatButton = new RoundGlossButton(
                RoundGlossButton.IconType.REPEAT,
                Theme.TRANSPORT_BUTTON_SIZE,
                "Repeat Mode (Off / All / One) (R)"
        );
        repeatButton.addActionListener(e -> cycleRepeatMode());

        transportRow.add(Box.createHorizontalGlue());
        transportRow.add(shuffleButton);
        transportRow.add(Box.createHorizontalGlue());
        transportRow.add(prevButton);
        transportRow.add(Box.createHorizontalGlue());
        transportRow.add(playPauseButton);
        transportRow.add(Box.createHorizontalGlue());
        transportRow.add(nextButton);
        transportRow.add(Box.createHorizontalGlue());
        transportRow.add(repeatButton);
        transportRow.add(Box.createHorizontalGlue());

        add(transportRow);
        add(Box.createVerticalStrut(Theme.EIGHT));

        // --- 2. Slim Bottom Row: Mute on left, status center, Volume slider on right ---
        JPanel bottomRow = new JPanel(new BorderLayout(Theme.EIGHT, Theme.ZERO));
        bottomRow.setOpaque(false);
        bottomRow.setPreferredSize(new Dimension(Theme.THREE_HUNDRED, Theme.TWENTY_EIGHT));
        bottomRow.setBorder(BorderFactory.createEmptyBorder(Theme.TWO, Theme.FOUR, Theme.TWO, Theme.FOUR));

        muteButton = new RoundGlossButton(
                RoundGlossButton.IconType.MUTE,
                Theme.MUTE_BUTTON_SIZE,
                "Toggle Mute (M)"
        );
        muteButton.addActionListener(e -> {
            boolean nextVal = !muteButton.isSelectedToggle();
            muteButton.setSelectedToggle(nextVal);
            muteButton.setMuted(nextVal);
            if (this.controller != null) {
                this.controller.setMuted(nextVal);
            }
        });
        bottomRow.add(muteButton, BorderLayout.WEST);

        statusLabel = new JLabel("Ready", JLabel.CENTER);
        statusLabel.setFont(Theme.FONT_STATUS);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        bottomRow.add(statusLabel, BorderLayout.CENTER);

        JPanel volumeGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.FOUR, Theme.ZERO));
        volumeGroup.setOpaque(false);

        volumeSlider = new SkinSlider(Theme.ZERO, Theme.VOLUME_MAX, Theme.VOLUME_MAX);
        volumeSlider.setFillColor(Theme.VOLUME_FILL);
        volumeSlider.setPreferredSize(new Dimension(Theme.VOLUME_SLIDER_WIDTH, Theme.VOLUME_SLIDER_HEIGHT));
        volumeSlider.setToolTipText("Volume (Up / Down)");
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting() && this.controller != null) {
                this.controller.setVolume(volumeSlider.getValue() / Theme.PERCENT_DIVISOR);
            }
        });

        volumeGroup.add(volumeSlider);
        volumeGroup.add(new ResizeGrip());
        bottomRow.add(volumeGroup, BorderLayout.EAST);

        add(bottomRow);

        // Standalone PL button instance for the Top Row
        playlistButton = new RoundGlossButton(
                RoundGlossButton.IconType.PL,
                Theme.PL_BUTTON_WIDTH,
                Theme.PL_BUTTON_HEIGHT,
                "Toggle Playlist Panel"
        );
        playlistButton.setSelectedToggle(false);
        playlistButton.addActionListener(e -> {
            boolean nextVal = !playlistButton.isSelectedToggle();
            playlistButton.setSelectedToggle(nextVal);
            if (playlistToggleListener != null) {
                playlistToggleListener.accept(nextVal);
            }
        });

        if (controller != null) {
            controller.addListener(this);
        }
    }

    public RoundGlossButton getPlaylistButton() {
        return playlistButton;
    }

    public void setOnPlaylistToggle(java.util.function.Consumer<Boolean> listener) {
        this.playlistToggleListener = listener;
    }

    public void setPlaylistButtonSelected(boolean selected) {
        playlistButton.setSelectedToggle(selected);
    }

    private void cycleRepeatMode() {
        RepeatMode nextMode = switch (currentRepeatMode) {
            case OFF -> RepeatMode.ALL;
            case ALL -> RepeatMode.ONE;
            case ONE -> RepeatMode.OFF;
        };
        if (controller != null) {
            controller.setRepeatMode(nextMode);
        }
    }

    @Override
    public void onStateChanged(PlayerState state) {
        boolean playing = (state == PlayerState.PLAYING);
        playPauseButton.setPlaying(playing);

        switch (state) {
            case PLAYING -> statusLabel.setText("Playing");
            case PAUSED -> statusLabel.setText("Paused");
            case LOADING -> statusLabel.setText("Loading...");
            case STOPPED -> statusLabel.setText("Stopped");
            case ERROR -> statusLabel.setText("Error");
            case IDLE -> statusLabel.setText("Ready");
        }
    }

    @Override
    public void onVolumeChanged(double volume, boolean muted) {
        volumeSlider.setValue((int) Math.round(volume * Theme.PERCENT_MULTIPLIER));
        muteButton.setSelectedToggle(muted);
        muteButton.setMuted(muted);
    }

    @Override
    public void onRepeatModeChanged(RepeatMode mode) {
        this.currentRepeatMode = mode;
        switch (mode) {
            case OFF -> {
                repeatButton.setSelectedToggle(false);
                repeatButton.setRepeatOne(false);
            }
            case ALL -> {
                repeatButton.setSelectedToggle(true);
                repeatButton.setRepeatOne(false);
            }
            case ONE -> {
                repeatButton.setSelectedToggle(true);
                repeatButton.setRepeatOne(true);
            }
        }
    }

    @Override
    public void onShuffleChanged(boolean shuffle) {
        shuffleButton.setSelectedToggle(shuffle);
    }
}
