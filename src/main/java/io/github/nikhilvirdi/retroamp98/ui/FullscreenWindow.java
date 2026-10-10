package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.TimeUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Borderless fullscreen presentation window for RetroAmp98.
 * Provides seamless video playback without reloading or interruptions,
 * an auto-hiding floating glossy control bar, and full keyboard navigation.
 */
public class FullscreenWindow extends JDialog implements ControllerListener {

    private static final Cursor BLANK_CURSOR;
    static {
        BufferedImage blankImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(blankImg, new Point(0, 0), "blank");
    }

    private final MainWindow owner;
    private final PlayerController controller;
    private final VideoPanel videoPanel;
    private final Runnable onExit;

    private final JLayeredPane layeredPane;
    private final JPanel overlayPanel;
    private final RoundGlossButton playPauseBtn;
    private final SkinSlider seekSlider;
    private final JLabel timeLabel;
    private final JLabel titleLabel;
    private final SkinSlider volSlider;
    private final RoundGlossButton muteBtn;

    private final Timer autoHideTimer;
    private final AWTEventListener mouseActivityListener;

    private boolean isUserSeeking = false;
    private Duration currentDuration = Duration.ZERO;
    private boolean isExited = false;

    public FullscreenWindow(MainWindow owner, PlayerController controller, VideoPanel videoPanel, Runnable onExit) {
        super(owner, "RetroAmp98 Fullscreen", false);
        this.owner = owner;
        this.controller = controller;
        this.videoPanel = videoPanel;
        this.onExit = onExit;

        setUndecorated(true);
        getContentPane().setBackground(Color.BLACK);

        Rectangle screenBounds = (owner.getGraphicsConfiguration() != null)
                ? owner.getGraphicsConfiguration().getBounds()
                : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(screenBounds);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        setContentPane(layeredPane);

        // Add videoPanel to default layer
        videoPanel.setBounds(0, 0, getWidth(), getHeight());
        layeredPane.add(videoPanel, JLayeredPane.DEFAULT_LAYER);

        // Double click on video in fullscreen exits fullscreen
        videoPanel.setOnDoubleClickListener(this::exitFullscreen);

        // Build floating bottom overlay
        overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Dark translucent metallic glass background
                LinearGradientPaint bg = new LinearGradientPaint(
                        0, 0, 0, h,
                        new float[]{0.0f, 0.5f, 1.0f},
                        new Color[]{new Color(30, 42, 60, 220), new Color(18, 25, 38, 230), new Color(10, 15, 25, 240)}
                );
                g2.setPaint(bg);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Highlight and border
                g2.setColor(new Color(110, 140, 180, 160));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawLine(10, 1, w - 10, 1);

                g2.dispose();
            }
        };
        overlayPanel.setOpaque(false);
        overlayPanel.setLayout(new BorderLayout(6, 4));
        overlayPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Top line of overlay: Track title and time display
        JPanel topInfoPanel = new JPanel(new BorderLayout(6, 0));
        topInfoPanel.setOpaque(false);

        titleLabel = new JLabel("RetroAmp98");
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        titleLabel.setForeground(Theme.STATUS_LCD_TEXT);

        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(Theme.FONT_STATUS_LCD);
        timeLabel.setForeground(Theme.STATUS_TIME_TEXT);

        topInfoPanel.add(titleLabel, BorderLayout.WEST);
        topInfoPanel.add(timeLabel, BorderLayout.EAST);
        overlayPanel.add(topInfoPanel, BorderLayout.NORTH);

        // Center line: Seek slider
        seekSlider = new SkinSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        seekSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isUserSeeking = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isUserSeeking) {
                    isUserSeeking = false;
                    if (currentDuration != null && !currentDuration.isZero()) {
                        double fraction = seekSlider.getValue() / 1000.0;
                        long targetSeconds = (long) (fraction * currentDuration.toSeconds());
                        controller.seek(Duration.ofSeconds(targetSeconds));
                    }
                }
            }
        });
        seekSlider.addChangeListener(e -> {
            if (isUserSeeking && currentDuration != null && !currentDuration.isZero()) {
                double fraction = seekSlider.getValue() / 1000.0;
                long targetSeconds = (long) (fraction * currentDuration.toSeconds());
                timeLabel.setText(TimeUtils.formatDuration(Duration.ofSeconds(targetSeconds)) + " / " + TimeUtils.formatDuration(currentDuration));
            }
        });
        overlayPanel.add(seekSlider, BorderLayout.CENTER);

        // Bottom line: Controls row
        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);

        // Transport controls
        JPanel transGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        transGroup.setOpaque(false);

        RoundGlossButton prevBtn = new RoundGlossButton(RoundGlossButton.IconType.PREV, 26, "Previous (P)");
        prevBtn.addActionListener(e -> controller.previous());

        playPauseBtn = new RoundGlossButton(RoundGlossButton.IconType.PLAY, 32, "Play/Pause (Space)");
        playPauseBtn.addActionListener(e -> controller.playPause());

        RoundGlossButton stopBtn = new RoundGlossButton(RoundGlossButton.IconType.STOP, 24, "Stop");
        stopBtn.addActionListener(e -> controller.stop());

        RoundGlossButton nextBtn = new RoundGlossButton(RoundGlossButton.IconType.NEXT, 26, "Next (N)");
        nextBtn.addActionListener(e -> controller.next());

        transGroup.add(prevBtn);
        transGroup.add(playPauseBtn);
        transGroup.add(stopBtn);
        transGroup.add(nextBtn);

        // Right side: Volume and Exit Fullscreen
        JPanel rightGroup = new JPanel();
        rightGroup.setOpaque(false);
        rightGroup.setLayout(new BoxLayout(rightGroup, BoxLayout.X_AXIS));

        volSlider = new SkinSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(controller.getMediaEngine().getVolume() * 100));
        volSlider.setPreferredSize(new Dimension(80, 20));
        volSlider.addChangeListener(e -> {
            if (!volSlider.getValueIsAdjusting()) {
                controller.setVolume(volSlider.getValue() / 100.0);
            }
        });

        muteBtn = new RoundGlossButton("Vol", 36, 22, "Mute (M)");
        muteBtn.addActionListener(e -> controller.toggleMute());

        RoundGlossButton exitBtn = new RoundGlossButton("Exit [Esc]", 80, 24, "Exit Fullscreen (Esc)");
        exitBtn.addActionListener(e -> exitFullscreen());

        rightGroup.add(muteBtn);
        rightGroup.add(Box.createHorizontalStrut(4));
        rightGroup.add(volSlider);
        rightGroup.add(Box.createHorizontalStrut(10));
        rightGroup.add(exitBtn);

        bottomRow.add(transGroup, BorderLayout.WEST);
        bottomRow.add(rightGroup, BorderLayout.EAST);
        overlayPanel.add(bottomRow, BorderLayout.SOUTH);

        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // Responsive positioning on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutComponents();
            }
        });

        // Auto-hide controls and cursor after 3 seconds of inactivity while playing
        autoHideTimer = new Timer(3000, e -> setControlsAndCursorVisible(false));
        autoHideTimer.setRepeats(false);

        // Global AWT mouse listener to detect any mouse activity over the fullscreen window
        mouseActivityListener = event -> {
            if (isExited) return;
            if (event instanceof MouseEvent me) {
                // Check if event belongs to this window or any child
                if (me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_DRAGGED
                        || me.getID() == MouseEvent.MOUSE_PRESSED) {
                    onUserActivity();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseActivityListener,
                AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

        // Keyboard actions
        setupKeyBindings();

        // Register controller listener
        controller.addListener(this);

        // Initial state sync
        syncState(controller.getMediaEngine().getState());
        onTrackChanged(controller.getPlaylistManager().current(), controller.getPlaylistManager().getCurrentIndex());

        relayoutComponents();
        autoHideTimer.start();
    }

    private void relayoutComponents() {
        int w = getWidth();
        int h = getHeight();
        videoPanel.setBounds(0, 0, w, h);

        int overlayW = Math.min(680, w - 40);
        int overlayH = 88;
        int overlayX = (w - overlayW) / 2;
        int overlayY = h - overlayH - 24;
        overlayPanel.setBounds(overlayX, overlayY, overlayW, overlayH);
    }

    private void onUserActivity() {
        setControlsAndCursorVisible(true);
        if (controller.getMediaEngine().getState() == PlayerState.PLAYING) {
            autoHideTimer.restart();
        } else {
            autoHideTimer.stop();
        }
    }

    private void setControlsAndCursorVisible(boolean visible) {
        if (!visible) {
            if (controller.getMediaEngine().getState() == PlayerState.PLAYING) {
                overlayPanel.setVisible(false);
                setCursor(BLANK_CURSOR);
            }
        } else {
            overlayPanel.setVisible(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void exitFullscreen() {
        if (isExited) return;
        isExited = true;

        autoHideTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(mouseActivityListener);
        controller.removeListener(this);
        setCursor(Cursor.getDefaultCursor());

        // Restore videoPanel to normal window
        layeredPane.remove(videoPanel);
        dispose();

        if (onExit != null) {
            onExit.run();
        }
    }

    private void setupKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exit");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "exit");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "exit");
        am.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                exitFullscreen();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "playPause");
        am.put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.playPause();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "skipBack5");
        am.put("skipBack5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.skip(-5);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "skipFwd5");
        am.put("skipFwd5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.skip(5);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), "skipBack10");
        am.put("skipBack10", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.skip(-10);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), "skipFwd10");
        am.put("skipFwd10", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.skip(10);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "volUp");
        am.put("volUp", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.setVolume(Math.min(1.0, controller.getMediaEngine().getVolume() + 0.05));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "volDown");
        am.put("volDown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.setVolume(Math.max(0.0, controller.getMediaEngine().getVolume() - 0.05));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "mute");
        am.put("mute", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.toggleMute();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "next");
        am.put("next", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.next();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "prev");
        am.put("prev", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.previous();
            }
        });
    }

    private void syncState(PlayerState state) {
        if (state == PlayerState.PLAYING) {
            playPauseBtn.setIconType(RoundGlossButton.IconType.PAUSE);
            autoHideTimer.restart();
        } else {
            playPauseBtn.setIconType(RoundGlossButton.IconType.PLAY);
            setControlsAndCursorVisible(true);
            autoHideTimer.stop();
        }
    }

    @Override
    public void onStateChanged(PlayerState state) {
        syncState(state);
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        this.currentDuration = duration;
        if (!isUserSeeking && duration != null && !duration.isZero()) {
            double fraction = (double) position.toMillis() / duration.toMillis();
            seekSlider.setValue((int) Math.round(fraction * 1000));
            timeLabel.setText(TimeUtils.formatDuration(position) + " / " + TimeUtils.formatDuration(duration));
        }
    }

    @Override
    public void onDurationKnown(Duration duration) {
        this.currentDuration = duration;
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        if (track != null) {
            titleLabel.setText((index + 1) + ". " + track.getTitle());
        } else {
            titleLabel.setText("RetroAmp98");
        }
    }

    @Override
    public void onTrackInfoReady(io.github.nikhilvirdi.retroamp98.engine.TrackInfo trackInfo) {
        if (trackInfo != null && trackInfo.getDisplayTitle() != null) {
            titleLabel.setText(trackInfo.getDisplayTitle());
        }
    }

    @Override
    public void onVolumeChanged(double volume, boolean muted) {
        volSlider.setValue((int) Math.round(volume * 100));
        muteBtn.setText(muted ? "Muted" : "Vol");
        muteBtn.setSelectedToggle(muted);
    }
}
