package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;

import javax.swing.JSlider;
import java.time.Duration;

/**
 * Seek bar slider for playback position tracking and seeking.
 * Prevents UI fighting by isolating engine updates from active user dragging.
 */
public class SeekBar extends SkinSlider implements ControllerListener {

    private static final int RESOLUTION = 1000;

    private final PlayerController controller;
    private Duration currentDuration = Duration.ZERO;
    private boolean isUserSeeking = false;

    public SeekBar(PlayerController controller) {
        super(0, RESOLUTION, 0);
        this.controller = controller;

        setFocusable(false);

        addChangeListener(e -> {
            if (getValueIsAdjusting()) {
                isUserSeeking = true;
            } else if (isUserSeeking) {
                // User finished dragging and released thumb
                isUserSeeking = false;
                if (currentDuration != null && !currentDuration.isZero()) {
                    long targetMillis = (long) ((getValue() / (double) RESOLUTION) * currentDuration.toMillis());
                    controller.seek(Duration.ofMillis(targetMillis));
                }
            }
        });

        controller.addListener(this);
    }

    @Override
    public void onStateChanged(PlayerState state) {
        if (state == PlayerState.STOPPED || state == PlayerState.IDLE) {
            if (!isUserSeeking) {
                setValue(0);
            }
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        this.currentDuration = duration;
        if (!isUserSeeking && duration != null && !duration.isZero()) {
            int progress = (int) ((position.toMillis() / (double) duration.toMillis()) * RESOLUTION);
            setValue(Math.min(RESOLUTION, Math.max(0, progress)));
        }
    }

    @Override
    public void onDurationKnown(Duration duration) {
        this.currentDuration = duration;
    }
}
