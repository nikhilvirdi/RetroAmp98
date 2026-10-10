package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Windows Media Player 9 Series styled transport bar.
 * Features large glossy round Play/Pause, smaller round Stop/Prev/Next,
 * volume slider with mute, and mode toggles.
 */
public class TransportBar extends JPanel implements ControllerListener {

    private final PlayerController controller;

    private final RoundGlossButton prevButton;
    private final RoundGlossButton playPauseButton;
    private final RoundGlossButton stopButton;
    private final RoundGlossButton nextButton;

    private final RoundGlossButton muteButton;
    private final SkinSlider volumeSlider;

    private final RoundGlossButton repeatButton;
    private final RoundGlossButton shuffleButton;
    private final RoundGlossButton playlistButton;

    private java.util.function.Consumer<Boolean> playlistToggleListener;
    private RepeatMode currentRepeatMode = RepeatMode.OFF;

    public TransportBar(PlayerController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(4, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 6));

        // --- Left / Center Area: Transport buttons ---
        JPanel controlsGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        controlsGroup.setOpaque(false);

        prevButton = new RoundGlossButton(RoundGlossButton.IconType.PREV, 30, "Previous Track");
        playPauseButton = new RoundGlossButton(RoundGlossButton.IconType.PLAY, 42, "Play / Pause");
        stopButton = new RoundGlossButton(RoundGlossButton.IconType.STOP, 30, "Stop");
        nextButton = new RoundGlossButton(RoundGlossButton.IconType.NEXT, 30, "Next Track");

        prevButton.addActionListener(e -> controller.previous());
        playPauseButton.addActionListener(e -> controller.playPause());
        stopButton.addActionListener(e -> controller.stop());
        nextButton.addActionListener(e -> controller.next());

        controlsGroup.add(prevButton);
        controlsGroup.add(playPauseButton);
        controlsGroup.add(stopButton);
        controlsGroup.add(nextButton);

        controlsGroup.add(Box.createHorizontalStrut(8));

        // Mode toggles (shared compact glossy pill style with clear active highlight state)
        repeatButton = new RoundGlossButton("Rpt: Off", 60, 22, "Repeat Mode (Off / All / One)");
        repeatButton.setSelectedToggle(false);
        repeatButton.addActionListener(e -> cycleRepeatMode());

        shuffleButton = new RoundGlossButton("Shuf: Off", 60, 22, "Toggle Shuffle (On / Off)");
        shuffleButton.setSelectedToggle(false);
        shuffleButton.addActionListener(e -> {
            boolean nextVal = !shuffleButton.isSelectedToggle();
            shuffleButton.setSelectedToggle(nextVal);
            shuffleButton.setText(nextVal ? "Shuf: On" : "Shuf: Off");
            controller.setShuffle(nextVal);
        });

        playlistButton = new RoundGlossButton("PL", 36, 22, "Toggle Playlist Panel");
        playlistButton.setSelectedToggle(false);
        playlistButton.addActionListener(e -> {
            boolean nextVal = !playlistButton.isSelectedToggle();
            playlistButton.setSelectedToggle(nextVal);
            if (playlistToggleListener != null) {
                playlistToggleListener.accept(nextVal);
            }
        });

        controlsGroup.add(repeatButton);
        controlsGroup.add(shuffleButton);
        controlsGroup.add(playlistButton);

        add(controlsGroup, BorderLayout.CENTER);

        // --- Right Area: Volume slider + Mute button + Resize grip ---
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        rightGroup.setOpaque(false);

        muteButton = new RoundGlossButton("Mute", 44, 22, "Mute Volume");
        muteButton.addActionListener(e -> {
            boolean nextVal = !muteButton.isSelectedToggle();
            muteButton.setSelectedToggle(nextVal);
            controller.setMuted(nextVal);
        });

        volumeSlider = new SkinSlider(0, 100, 100);
        volumeSlider.setPreferredSize(new Dimension(80, 20));
        volumeSlider.setToolTipText("Volume");
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                controller.setVolume(volumeSlider.getValue() / 100.0);
            }
        });

        rightGroup.add(muteButton);
        rightGroup.add(volumeSlider);
        rightGroup.add(Box.createHorizontalStrut(4));
        rightGroup.add(new ResizeGrip());

        add(rightGroup, BorderLayout.EAST);

        controller.addListener(this);
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
        controller.setRepeatMode(nextMode);
    }

    @Override
    public void onStateChanged(PlayerState state) {
        if (state == PlayerState.PLAYING) {
            playPauseButton.setIconType(RoundGlossButton.IconType.PAUSE);
        } else {
            playPauseButton.setIconType(RoundGlossButton.IconType.PLAY);
        }
    }

    @Override
    public void onVolumeChanged(double volume, boolean muted) {
        volumeSlider.setValue((int) (volume * 100));
        muteButton.setSelectedToggle(muted);
    }

    @Override
    public void onRepeatModeChanged(RepeatMode mode) {
        this.currentRepeatMode = mode;
        switch (mode) {
            case OFF -> {
                repeatButton.setText("Rpt: Off");
                repeatButton.setSelectedToggle(false);
            }
            case ALL -> {
                repeatButton.setText("Rpt: All");
                repeatButton.setSelectedToggle(true);
            }
            case ONE -> {
                repeatButton.setText("Rpt: One");
                repeatButton.setSelectedToggle(true);
            }
        }
    }

    @Override
    public void onShuffleChanged(boolean shuffle) {
        shuffleButton.setText(shuffle ? "Shuf: On" : "Shuf: Off");
        shuffleButton.setSelectedToggle(shuffle);
    }
}
