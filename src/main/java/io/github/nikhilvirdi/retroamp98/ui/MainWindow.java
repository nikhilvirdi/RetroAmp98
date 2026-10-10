package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.playlist.M3uSupport;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.util.SettingsManager;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Windows Media Player 9 Series ("Corona" XP era) styled main application window.
 * Features silver-blue metallic body, top menu bar, central stretching video area,
 * dark status strip, collapsible playlist, glossy bottom docked controls, drag-and-drop,
 * settings persistence, and borderless fullscreen playback.
 */
public class MainWindow extends JFrame implements ControllerListener {

    private final PlayerController controller;
    private final SettingsManager settingsManager;

    private JPanel videoContainer;
    private VideoPanel videoPanel;
    private StatusStrip statusStrip;
    private PlaylistPanel playlistPanel;
    private TransportBar transportBar;
    private FullscreenWindow fullscreenWindow;

    // Menu items
    private JRadioButtonMenuItem repeatOffItem;
    private JRadioButtonMenuItem repeatOneItem;
    private JRadioButtonMenuItem repeatAllItem;
    private JCheckBoxMenuItem shuffleMenuItem;
    private JCheckBoxMenuItem playlistViewItem;
    private JCheckBoxMenuItem visualizerMenuItem;
    private JCheckBoxMenuItem resizeToVideoItem;

    public MainWindow(PlayerController controller) {
        this(controller, new SettingsManager());
    }

    public MainWindow(PlayerController controller, SettingsManager settingsManager) {
        super("RetroAmp98");
        this.controller = controller;
        this.settingsManager = settingsManager;
        this.controller.setParentWindow(this);

        setIconImage(AppIcon.createIcon());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        setResizable(true);
        setMinimumSize(new Dimension(560, 480));
        setPreferredSize(new Dimension(740, 540));

        // Root container with WMP 9 silver-blue metallic body gradient
        JPanel rootPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();

                LinearGradientPaint bodyGrad = new LinearGradientPaint(
                        0, 0, 0, h,
                        new float[]{0.0f, 0.40f, 1.0f},
                        new Color[]{Theme.BODY_GRADIENT_TOP, Theme.BODY_GRADIENT_MID, Theme.BODY_GRADIENT_BOTTOM}
                );
                g2.setPaint(bodyGrad);
                g2.fillRect(0, 0, w, h);

                // Top highlight line
                g2.setColor(Theme.BODY_HIGHLIGHT);
                g2.drawLine(0, 0, w, 0);

                g2.dispose();
            }
        };

        // Center workspace: Video area + Status strip + Collapsible Playlist
        JPanel centerWorkspace = new JPanel(new BorderLayout(4, 0));
        centerWorkspace.setOpaque(false);
        centerWorkspace.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));

        // Video container holding VideoPanel and StatusStrip directly below it
        videoContainer = new JPanel(new BorderLayout(0, 2));
        videoContainer.setOpaque(false);

        videoPanel = new VideoPanel(controller);
        videoPanel.setOnDoubleClickListener(this::toggleFullscreen);

        statusStrip = new StatusStrip(controller);

        videoContainer.add(videoPanel, BorderLayout.CENTER);
        videoContainer.add(statusStrip, BorderLayout.SOUTH);

        centerWorkspace.add(videoContainer, BorderLayout.CENTER);

        // Collapsible Playlist Panel on the side (starts collapsed so video has full width)
        playlistPanel = new PlaylistPanel(controller);
        playlistPanel.setPreferredSize(new Dimension(250, 200));
        playlistPanel.setVisible(false);
        centerWorkspace.add(playlistPanel, BorderLayout.EAST);

        rootPanel.add(centerWorkspace, BorderLayout.CENTER);

        // Bottom docked controls: Seek Bar + Transport Bar
        JPanel bottomControls = new JPanel();
        bottomControls.setOpaque(false);
        bottomControls.setLayout(new BoxLayout(bottomControls, BoxLayout.Y_AXIS));
        bottomControls.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 6));

        SeekBar seekBar = new SeekBar(controller);
        seekBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        transportBar = new TransportBar(controller);
        transportBar.setOnPlaylistToggle(this::setPlaylistVisible);
        transportBar.setPlaylistButtonSelected(false);

        bottomControls.add(seekBar);
        bottomControls.add(transportBar);

        rootPanel.add(bottomControls, BorderLayout.SOUTH);

        setJMenuBar(buildMenuBar());
        setContentPane(rootPanel);

        // Install Drag & Drop onto main window, video panel, and playlist panel
        MediaDropHandler.install(rootPanel, controller);
        MediaDropHandler.install(videoPanel, controller);
        MediaDropHandler.install(playlistPanel, controller);

        // Global keyboard navigation
        setupGlobalKeyBindings();

        pack();

        // Load & apply persistent settings
        applySettings();

        controller.addListener(this);
    }

    public JPanel getVideoContainer() {
        return videoContainer;
    }

    public VideoPanel getVideoPanel() {
        return videoPanel;
    }

    public void setPlaylistVisible(boolean visible) {
        playlistPanel.setVisible(visible);
        if (playlistViewItem != null) {
            playlistViewItem.setSelected(visible);
        }
        if (transportBar != null) {
            transportBar.setPlaylistButtonSelected(visible);
        }
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    public void toggleFullscreen() {
        if (fullscreenWindow != null) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    public void enterFullscreen() {
        if (fullscreenWindow != null) return;

        videoContainer.remove(videoPanel);
        videoContainer.revalidate();
        videoContainer.repaint();

        fullscreenWindow = new FullscreenWindow(this, controller, videoPanel, () -> {
            videoContainer.add(videoPanel, BorderLayout.CENTER);
            videoPanel.setOnDoubleClickListener(this::toggleFullscreen);
            videoContainer.revalidate();
            videoContainer.repaint();
            fullscreenWindow = null;
            requestFocusInWindow();
        });
        fullscreenWindow.setVisible(true);
    }

    public void exitFullscreen() {
        if (fullscreenWindow != null) {
            fullscreenWindow.exitFullscreen();
        }
    }

    private void handleExit() {
        saveSettings();
        controller.stop();
        dispose();
        System.exit(0);
    }

    private void applySettings() {
        settingsManager.load();

        controller.setVolume(settingsManager.getVolume());
        controller.setMuted(settingsManager.isMuted());
        controller.setRate(settingsManager.getSpeed());
        controller.setRepeatMode(settingsManager.getRepeatMode());
        controller.setShuffle(settingsManager.isShuffle());

        controller.getMediaEngine().setEqualizerEnabled(settingsManager.isEqualizerEnabled());
        double[] eqBands = settingsManager.getEqualizerBands();
        for (int i = 0; i < 10; i++) {
            controller.getMediaEngine().setEqualizerBand(i, eqBands[i]);
        }

        if (videoPanel != null) {
            videoPanel.setVisualizerEnabled(settingsManager.isVisualizerEnabled());
        }
        if (visualizerMenuItem != null) {
            visualizerMenuItem.setSelected(settingsManager.isVisualizerEnabled());
        }
        if (resizeToVideoItem != null) {
            resizeToVideoItem.setSelected(settingsManager.isResizeToVideo());
        }

        if (settingsManager.getWindowWidth() >= 560 && settingsManager.getWindowHeight() >= 480) {
            setSize(settingsManager.getWindowWidth(), settingsManager.getWindowHeight());
        } else {
            setSize(740, 540);
        }

        if (settingsManager.getWindowX() >= 0 && settingsManager.getWindowY() >= 0) {
            setLocation(settingsManager.getWindowX(), settingsManager.getWindowY());
        } else {
            setLocationRelativeTo(null);
        }

        // Last playlist: reload without auto-play if file exists
        String lastPl = settingsManager.getLastPlaylistPath();
        if (lastPl != null && !lastPl.isBlank()) {
            Path plPath = Path.of(lastPl);
            if (Files.exists(plPath)) {
                try {
                    M3uSupport.M3uResult res = M3uSupport.loadWithReport(plPath);
                    controller.getPlaylistManager().setPlaylist(res.playlist());
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveSettings() {
        settingsManager.setVolume(controller.getMediaEngine().getVolume());
        settingsManager.setMuted(false);
        settingsManager.setSpeed(controller.getRate());
        settingsManager.setRepeatMode(controller.getPlaylistManager().getRepeatMode());
        settingsManager.setShuffle(controller.getPlaylistManager().isShuffle());
        settingsManager.setEqualizerEnabled(controller.getMediaEngine().isEqualizerEnabled());
        double[] eqBands = new double[10];
        for (int i = 0; i < 10; i++) {
            eqBands[i] = controller.getMediaEngine().getEqualizerBand(i);
        }
        settingsManager.setEqualizerBands(eqBands);
        settingsManager.setVisualizerEnabled(visualizerMenuItem != null && visualizerMenuItem.isSelected());
        settingsManager.setResizeToVideo(resizeToVideoItem != null && resizeToVideoItem.isSelected());
        if (fullscreenWindow == null) {
            settingsManager.setWindowX(getX());
            settingsManager.setWindowY(getY());
            settingsManager.setWindowWidth(getWidth());
            settingsManager.setWindowHeight(getHeight());
        }
        settingsManager.save();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                java.awt.GradientPaint menuGrad = new java.awt.GradientPaint(
                        0, 0, Theme.MENUBAR_BG_TOP,
                        0, getHeight(), Theme.MENUBAR_BG_BOTTOM
                );
                g2.setPaint(menuGrad);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.MENUBAR_BORDER));

        // --- File Menu ---
        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(Theme.MENUBAR_TEXT);

        JMenuItem openFileItem = new JMenuItem("Open File...");
        openFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openFileItem.addActionListener(e -> handleOpenFile());
        fileMenu.add(openFileItem);

        JMenuItem openFolderItem = new JMenuItem("Open Folder...");
        openFolderItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        openFolderItem.addActionListener(e -> handleOpenFolder());
        fileMenu.add(openFolderItem);

        JMenuItem openPlaylistItem = new JMenuItem("Open Playlist...");
        openPlaylistItem.addActionListener(e -> handleOpenPlaylist());
        fileMenu.add(openPlaylistItem);

        JMenuItem savePlaylistItem = new JMenuItem("Save Playlist...");
        savePlaylistItem.addActionListener(e -> handleSavePlaylist());
        fileMenu.add(savePlaylistItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> handleExit());
        fileMenu.add(exitItem);

        // --- Play Menu ---
        JMenu playMenu = new JMenu("Play");
        playMenu.setForeground(Theme.MENUBAR_TEXT);

        JMenuItem playPauseItem = new JMenuItem("Play / Pause");
        playPauseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        playPauseItem.addActionListener(e -> controller.playPause());
        playMenu.add(playPauseItem);

        JMenuItem stopItem = new JMenuItem("Stop");
        stopItem.addActionListener(e -> controller.stop());
        playMenu.add(stopItem);

        JMenuItem prevItem = new JMenuItem("Previous");
        prevItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
        prevItem.addActionListener(e -> controller.previous());
        playMenu.add(prevItem);

        JMenuItem nextItem = new JMenuItem("Next");
        nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
        nextItem.addActionListener(e -> controller.next());
        playMenu.add(nextItem);

        JMenuItem skipBackItem = new JMenuItem("Skip Back 10s");
        skipBackItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK));
        skipBackItem.addActionListener(e -> controller.skip(-10));
        playMenu.add(skipBackItem);

        JMenuItem skipFwdItem = new JMenuItem("Skip Forward 10s");
        skipFwdItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK));
        skipFwdItem.addActionListener(e -> controller.skip(10));
        playMenu.add(skipFwdItem);

        playMenu.addSeparator();

        // A-B Loop
        JMenuItem setAItem = new JMenuItem("Set Loop Point A");
        setAItem.addActionListener(e -> controller.setLoopA());
        playMenu.add(setAItem);

        JMenuItem setBItem = new JMenuItem("Set Loop Point B");
        setBItem.addActionListener(e -> controller.setLoopB());
        playMenu.add(setBItem);

        JMenuItem clearLoopItem = new JMenuItem("Clear Loop");
        clearLoopItem.addActionListener(e -> controller.clearLoop());
        playMenu.add(clearLoopItem);

        playMenu.addSeparator();

        // Sleep Timer submenu
        JMenu sleepMenu = new JMenu("Sleep Timer");
        ButtonGroup sleepGroup = new ButtonGroup();

        JRadioButtonMenuItem sleepOff = new JRadioButtonMenuItem("Off", true);
        sleepOff.addActionListener(e -> controller.setSleepTimerMinutes(0));
        sleepGroup.add(sleepOff);
        sleepMenu.add(sleepOff);

        int[] sleepMins = {15, 30, 45, 60};
        for (int m : sleepMins) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(m + " min", false);
            item.addActionListener(e -> controller.setSleepTimerMinutes(m));
            sleepGroup.add(item);
            sleepMenu.add(item);
        }

        JRadioButtonMenuItem sleepEnd = new JRadioButtonMenuItem("End of current track", false);
        sleepEnd.addActionListener(e -> controller.setSleepAtEndOfTrack(true));
        sleepGroup.add(sleepEnd);
        sleepMenu.add(sleepEnd);

        playMenu.add(sleepMenu);

        playMenu.addSeparator();

        // Speed submenu
        JMenu speedMenu = new JMenu("Speed");
        ButtonGroup speedGroup = new ButtonGroup();
        double[] speeds = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
        String[] speedLabels = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};
        for (int i = 0; i < speeds.length; i++) {
            double s = speeds[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(speedLabels[i], Math.abs(s - 1.0) < 0.001);
            item.addActionListener(e -> controller.setRate(s));
            speedGroup.add(item);
            speedMenu.add(item);
        }
        playMenu.add(speedMenu);

        playMenu.addSeparator();

        // Repeat submenu
        JMenu repeatMenu = new JMenu("Repeat");
        ButtonGroup repeatGroup = new ButtonGroup();

        repeatOffItem = new JRadioButtonMenuItem("Off", true);
        repeatOneItem = new JRadioButtonMenuItem("One", false);
        repeatAllItem = new JRadioButtonMenuItem("All", false);

        repeatOffItem.addActionListener(e -> controller.setRepeatMode(RepeatMode.OFF));
        repeatOneItem.addActionListener(e -> controller.setRepeatMode(RepeatMode.ONE));
        repeatAllItem.addActionListener(e -> controller.setRepeatMode(RepeatMode.ALL));

        repeatGroup.add(repeatOffItem);
        repeatGroup.add(repeatOneItem);
        repeatGroup.add(repeatAllItem);

        repeatMenu.add(repeatOffItem);
        repeatMenu.add(repeatOneItem);
        repeatMenu.add(repeatAllItem);
        playMenu.add(repeatMenu);

        // Shuffle checkbox
        shuffleMenuItem = new JCheckBoxMenuItem("Shuffle", false);
        shuffleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
        shuffleMenuItem.addActionListener(e -> controller.setShuffle(shuffleMenuItem.isSelected()));
        playMenu.add(shuffleMenuItem);

        // --- View Menu ---
        JMenu viewMenu = new JMenu("View");
        viewMenu.setForeground(Theme.MENUBAR_TEXT);

        JMenuItem fullscreenItem = new JMenuItem("Fullscreen");
        fullscreenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        fullscreenItem.addActionListener(e -> enterFullscreen());
        viewMenu.add(fullscreenItem);

        playlistViewItem = new JCheckBoxMenuItem("Playlist", false);
        playlistViewItem.addActionListener(e -> setPlaylistVisible(playlistViewItem.isSelected()));
        viewMenu.add(playlistViewItem);

        resizeToVideoItem = new JCheckBoxMenuItem("Resize Window to Video", true);
        viewMenu.add(resizeToVideoItem);

        visualizerMenuItem = new JCheckBoxMenuItem("Visualizer", true);
        visualizerMenuItem.addActionListener(e -> videoPanel.setVisualizerEnabled(visualizerMenuItem.isSelected()));
        viewMenu.add(visualizerMenuItem);

        JMenuItem equalizerItem = new JMenuItem("Equalizer...");
        equalizerItem.addActionListener(e -> {
            EqualizerDialog eqDialog = new EqualizerDialog(this, controller);
            eqDialog.setVisible(true);
        });
        viewMenu.add(equalizerItem);

        // --- Help Menu ---
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(Theme.MENUBAR_TEXT);
        JMenuItem aboutItem = new JMenuItem("About RetroAmp98");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(playMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void setupGlobalKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "playPause", controller::playPause);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "seekBack5", () -> controller.skip(-5));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "seekFwd5", () -> controller.skip(5));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), "seekBack10", () -> controller.skip(-10));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), "seekFwd10", () -> controller.skip(10));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "volUp", () -> controller.setVolume(Math.min(1.0, controller.getMediaEngine().getVolume() + 0.05)));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "volDown", () -> controller.setVolume(Math.max(0.0, controller.getMediaEngine().getVolume() - 0.05)));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "mute", controller::toggleMute);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "next", controller::next);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "prev", controller::previous);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "shuffle", controller::toggleShuffle);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "repeat", controller::cycleRepeatMode);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fsF", this::toggleFullscreen);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fsF11", this::toggleFullscreen);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "openFileKey", this::handleOpenFile);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "openFolderKey", this::handleOpenFolder);
    }

    private void registerKey(InputMap im, ActionMap am, KeyStroke stroke, String name, Runnable action) {
        im.put(stroke, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JTextComponent) {
                    return; // Ignore shortcuts while typing in text components
                }
                action.run();
            }
        });
    }

    private void handleOpenFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Audio/Video File");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Supported Media (*.mp3, *.wav, *.aac, *.m4a, *.mp4)",
                "mp3", "wav", "aac", "m4a", "mp4"
        ));

        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            if (files != null && files.length > 0) {
                List<Path> paths = Arrays.stream(files).map(File::toPath).toList();
                controller.openFiles(paths);
            }
        }
    }

    private void handleOpenFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Media Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            if (folder != null) {
                controller.openFolder(folder.toPath());
            }
        }
    }

    private void handleOpenPlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open M3U Playlist");
        chooser.setFileFilter(new FileNameExtensionFilter("M3U Playlist (*.m3u, *.m3u8)", "m3u", "m3u8"));

        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                settingsManager.setLastPlaylistPath(file.getAbsolutePath());
                controller.openPlaylist(file.toPath());
            }
        }
    }

    private void handleSavePlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Playlist As M3U");
        chooser.setFileFilter(new FileNameExtensionFilter("M3U Playlist (*.m3u)", "m3u"));

        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                String pathStr = file.getAbsolutePath();
                if (!pathStr.toLowerCase().endsWith(".m3u") && !pathStr.toLowerCase().endsWith(".m3u8")) {
                    file = new File(pathStr + ".m3u");
                }
                settingsManager.setLastPlaylistPath(file.getAbsolutePath());
                controller.savePlaylist(file.toPath());
            }
        }
    }

    private void showAboutDialog() {
        String message = """
                RetroAmp98
                Version 0.1.0

                Windows Media Player 9 Series (XP Era) Styled Media Player.
                Built with Java Swing and JavaFX.
                """;
        JOptionPane.showMessageDialog(this, message, "About RetroAmp98", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onRepeatModeChanged(RepeatMode mode) {
        switch (mode) {
            case OFF -> repeatOffItem.setSelected(true);
            case ONE -> repeatOneItem.setSelected(true);
            case ALL -> repeatAllItem.setSelected(true);
        }
    }

    @Override
    public void onShuffleChanged(boolean shuffle) {
        shuffleMenuItem.setSelected(shuffle);
    }

    @Override
    public void onVideoSizeKnown(int width, int height) {
        if (resizeToVideoItem != null && resizeToVideoItem.isSelected() && width > 0 && height > 0 && fullscreenWindow == null) {
            resizeWindowToVideo(width, height);
        }
    }

    private void resizeWindowToVideo(int videoWidth, int videoHeight) {
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        java.awt.Rectangle screenBounds = (gc != null)
                ? gc.getBounds()
                : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        java.awt.Insets screenInsets = (gc != null)
                ? java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc)
                : new java.awt.Insets(0, 0, 0, 0);

        int maxW = screenBounds.width - screenInsets.left - screenInsets.right;
        int maxH = screenBounds.height - screenInsets.top - screenInsets.bottom;

        int extraHeight = getHeight() - videoPanel.getHeight();
        int extraWidth = getWidth() - videoPanel.getWidth();
        if (extraHeight <= 0) extraHeight = 160;
        if (extraWidth <= 0) extraWidth = 20;

        double maxVidW = maxW * 0.85;
        double maxVidH = (maxH - extraHeight) * 0.85;

        double scale = Math.min(1.0, Math.min(maxVidW / videoWidth, maxVidH / videoHeight));
        int finalVidW = Math.max(360, (int) Math.round(videoWidth * scale));
        int finalVidH = Math.max(200, (int) Math.round(videoHeight * scale));

        int targetWinW = Math.min(maxW, finalVidW + extraWidth);
        int targetWinH = Math.min(maxH, finalVidH + extraHeight);

        setSize(targetWinW, targetWinH);
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }
}
