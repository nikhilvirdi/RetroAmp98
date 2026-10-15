package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.playlist.M3uSupport;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.util.SettingsManager;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Main window for RetroAmp98 with a dark "Now Playing" layout.
 * Features an outer dark charcoal rounded card (Theme.RADIUS_OUTER) with subtle border,
 * split 50/50 horizontally into a left poster panel and a right control panel.
 */
public class MainWindow extends JFrame implements ControllerListener {

    private final PlayerController controller;
    private final SettingsManager settingsManager;

    private LcdPanel lcdPanel;
    private StatusStrip statusStrip;
    private PlaylistPanel playlistPanel;
    private TransportBar transportBar;

    // Menu items
    private JRadioButtonMenuItem repeatOffItem;
    private JRadioButtonMenuItem repeatOneItem;
    private JRadioButtonMenuItem repeatAllItem;
    private JCheckBoxMenuItem shuffleMenuItem;
    private JCheckBoxMenuItem playlistViewItem;

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
        setMinimumSize(new Dimension(Theme.WINDOW_MIN_WIDTH, Theme.WINDOW_MIN_HEIGHT));
        setPreferredSize(new Dimension(Theme.WINDOW_DEFAULT_WIDTH, Theme.WINDOW_DEFAULT_HEIGHT));

        // Root container with dark background and Theme.PADDING_OUTER around the card
        JPanel rootPanel = new JPanel(new BorderLayout(Theme.ZERO, Theme.ZERO));
        rootPanel.setBackground(Theme.WINDOW_BG);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING_OUTER, Theme.PADDING_OUTER,
                Theme.PADDING_OUTER, Theme.PADDING_OUTER
        ));

        // Dark charcoal rounded card container (Theme.RADIUS_OUTER) with 1px border
        JPanel cardPanel = new JPanel(new BorderLayout(Theme.ZERO, Theme.ZERO)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                RoundRectangle2D cardShape = new RoundRectangle2D.Double(
                        Theme.ZERO, Theme.ZERO,
                        w - Theme.ONE, h - Theme.ONE,
                        Theme.RADIUS_OUTER, Theme.RADIUS_OUTER
                );

                g2.setColor(Theme.CARD_BG);
                g2.fill(cardShape);

                g2.setColor(Theme.OUTER_BORDER);
                g2.setStroke(Theme.STROKE_ONE);
                g2.draw(cardShape);

                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Clip children inside the rounded rectangle card
                RoundRectangle2D clipShape = new RoundRectangle2D.Double(
                        Theme.ZERO, Theme.ZERO,
                        w, h,
                        Theme.RADIUS_OUTER, Theme.RADIUS_OUTER
                );
                g2.setClip(clipShape);

                super.paintChildren(g2);

                // Draw crisp border over child edges
                RoundRectangle2D borderShape = new RoundRectangle2D.Double(
                        Theme.ZERO, Theme.ZERO,
                        w - Theme.ONE, h - Theme.ONE,
                        Theme.RADIUS_OUTER, Theme.RADIUS_OUTER
                );
                g2.setColor(Theme.OUTER_BORDER);
                g2.setStroke(Theme.STROKE_ONE);
                g2.draw(borderShape);

                g2.dispose();
            }
        };
        cardPanel.setOpaque(false);

        // Horizontal split container: Left 50% Poster, Right 50% Controls
        JPanel splitContainer = new JPanel() {
            @Override
            public void doLayout() {
                int totalWidth = getWidth();
                int totalHeight = getHeight();

                if (getComponentCount() < Theme.TWO) {
                    return;
                }

                Component leftComp = getComponent(Theme.ZERO);
                Component rightComp = getComponent(Theme.ONE);

                int posterWidth = Math.max(Theme.POSTER_MIN_WIDTH, totalWidth / Theme.TWO);
                int controlsWidth = Math.max(Theme.ZERO, totalWidth - posterWidth);

                leftComp.setBounds(Theme.ZERO, Theme.ZERO, posterWidth, totalHeight);
                rightComp.setBounds(posterWidth, Theme.ZERO, controlsWidth, totalHeight);
            }
        };
        splitContainer.setLayout(null);
        splitContainer.setOpaque(false);

        // 1. LEFT POSTER PANEL (50% width, stretches full height)
        lcdPanel = new LcdPanel(controller);
        splitContainer.add(lcdPanel);

        // 2. RIGHT CONTAINER (50% width, contains Controls + Collapsible Playlist Drawer)
        JPanel rightContainer = new JPanel(new BorderLayout(Theme.ZERO, Theme.ZERO));
        rightContainer.setOpaque(false);

        // Right Control Panel (dark, from Theme.PANEL_BG)
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBackground(Theme.PANEL_BG);
        controlsPanel.setOpaque(true);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(
                Theme.SIXTEEN, Theme.TWENTY_FOUR,
                Theme.SIXTEEN, Theme.TWENTY_FOUR
        ));

        // Transport bar instance (provides controls and top-row PL button)
        transportBar = new TransportBar(controller);
        transportBar.setOnPlaylistToggle(this::setPlaylistVisible);
        transportBar.setPlaylistButtonSelected(false);

        // Top Row: Centered "Now Playing" in Theme.TEXT_MUTED + Top-Right "PL" button
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Theme.ONE_THOUSAND, Theme.TWENTY_EIGHT));

        JLabel nowPlayingLabel = new JLabel("Now Playing", JLabel.CENTER);
        nowPlayingLabel.setFont(Theme.FONT_TOP_TITLE);
        nowPlayingLabel.setForeground(Theme.TEXT_MUTED);

        // Balance left strut so label is perfectly centered
        topRow.add(Box.createHorizontalStrut(Theme.PL_BUTTON_WIDTH), BorderLayout.WEST);
        topRow.add(nowPlayingLabel, BorderLayout.CENTER);
        topRow.add(transportBar.getPlaylistButton(), BorderLayout.EAST);

        // Track Block: artist in Theme.TEXT_MUTED above, large bold title below
        statusStrip = new StatusStrip(controller);

        // Progress Bar: 3px line in Theme.PROGRESS_TRACK, Theme.ACCENT played, hover/drag thumb
        SeekBar seekBar = new SeekBar(controller);

        // Assemble controls panel
        controlsPanel.add(topRow);
        controlsPanel.add(Box.createVerticalGlue());
        controlsPanel.add(statusStrip);
        controlsPanel.add(Box.createVerticalStrut(Theme.SIXTEEN));
        controlsPanel.add(seekBar);
        controlsPanel.add(Box.createVerticalStrut(Theme.TWELVE));
        controlsPanel.add(transportBar);
        controlsPanel.add(Box.createVerticalGlue());

        rightContainer.add(controlsPanel, BorderLayout.CENTER);

        // Collapsible Playlist Drawer on the right of controls
        playlistPanel = new PlaylistPanel(controller);
        playlistPanel.setVisible(false);
        rightContainer.add(playlistPanel, BorderLayout.EAST);

        splitContainer.add(rightContainer);

        cardPanel.add(splitContainer, BorderLayout.CENTER);
        rootPanel.add(cardPanel, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
        setContentPane(rootPanel);

        setupGlobalKeyBindings();

        pack();

        applySettings();

        controller.addListener(this);
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

    private void handleExit() {
        saveSettings();
        controller.stop();
        dispose();
        System.exit(Theme.ZERO);
    }

    private void applySettings() {
        settingsManager.load();

        controller.setVolume(settingsManager.getVolume());
        controller.setMuted(settingsManager.isMuted());
        controller.setRepeatMode(settingsManager.getRepeatMode());
        controller.setShuffle(settingsManager.isShuffle());

        if (settingsManager.getWindowWidth() >= Theme.WINDOW_MIN_WIDTH &&
                settingsManager.getWindowHeight() >= Theme.WINDOW_MIN_HEIGHT) {
            setSize(settingsManager.getWindowWidth(), settingsManager.getWindowHeight());
        } else {
            setSize(Theme.WINDOW_DEFAULT_WIDTH, Theme.WINDOW_DEFAULT_HEIGHT);
        }
        setLocationRelativeTo(null);

        // Reload last playlist without auto-play if exists
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
        settingsManager.setMuted(controller.getMediaEngine().isMuted());
        settingsManager.setRepeatMode(controller.getPlaylistManager().getRepeatMode());
        settingsManager.setShuffle(controller.getPlaylistManager().isShuffle());
        settingsManager.setWindowWidth(getWidth());
        settingsManager.setWindowHeight(getHeight());
        settingsManager.save();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.MENUBAR_BG_TOP);
                g2.fillRect(Theme.ZERO, Theme.ZERO, getWidth(), getHeight());
                g2.dispose();
            }
        };
        menuBar.setBackground(Theme.MENUBAR_BG_TOP);
        menuBar.setBorder(BorderFactory.createMatteBorder(
                Theme.ZERO, Theme.ZERO, Theme.ONE, Theme.ZERO, Theme.MENUBAR_BORDER
        ));

        // --- File Menu ---
        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(Theme.MENUBAR_TEXT);

        JMenuItem openFileItem = new JMenuItem("Open File...");
        openFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openFileItem.addActionListener(e -> handleOpenFile());
        fileMenu.add(openFileItem);

        JMenuItem openFolderItem = new JMenuItem("Open Folder...");
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
        playPauseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Theme.ZERO));
        playPauseItem.addActionListener(e -> controller.playPause());
        playMenu.add(playPauseItem);

        JMenuItem stopItem = new JMenuItem("Stop");
        stopItem.addActionListener(e -> controller.stop());
        playMenu.add(stopItem);

        JMenuItem prevItem = new JMenuItem("Previous");
        prevItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Theme.ZERO));
        prevItem.addActionListener(e -> controller.previous());
        playMenu.add(prevItem);

        JMenuItem nextItem = new JMenuItem("Next");
        nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Theme.ZERO));
        nextItem.addActionListener(e -> controller.next());
        playMenu.add(nextItem);

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
        shuffleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Theme.ZERO));
        shuffleMenuItem.addActionListener(e -> controller.setShuffle(shuffleMenuItem.isSelected()));
        playMenu.add(shuffleMenuItem);

        // --- View Menu ---
        JMenu viewMenu = new JMenu("View");
        viewMenu.setForeground(Theme.MENUBAR_TEXT);

        playlistViewItem = new JCheckBoxMenuItem("Playlist", false);
        playlistViewItem.addActionListener(e -> setPlaylistVisible(playlistViewItem.isSelected()));
        viewMenu.add(playlistViewItem);

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

        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Theme.ZERO), "playPause", controller::playPause);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Theme.ZERO), "seekBack5", () -> controller.skip(-Theme.SEEK_STEP_SECONDS));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Theme.ZERO), "seekFwd5", () -> controller.skip(Theme.SEEK_STEP_SECONDS));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_UP, Theme.ZERO), "volUp", () -> controller.setVolume(Math.min(Theme.DOUBLE_ONE, controller.getMediaEngine().getVolume() + Theme.VOLUME_STEP)));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Theme.ZERO), "volDown", () -> controller.setVolume(Math.max(Theme.DOUBLE_ZERO, controller.getMediaEngine().getVolume() - Theme.VOLUME_STEP)));
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_M, Theme.ZERO), "mute", controller::toggleMute);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_N, Theme.ZERO), "next", controller::next);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_P, Theme.ZERO), "prev", controller::previous);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_S, Theme.ZERO), "shuffle", controller::toggleShuffle);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_R, Theme.ZERO), "repeat", controller::cycleRepeatMode);
        registerKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "openFileKey", this::handleOpenFile);
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
        chooser.setDialogTitle("Open Audio File");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Supported Audio (*.mp3, *.wav, *.m4a)",
                "mp3", "wav", "m4a"
        ));

        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            if (files != null && files.length > Theme.ZERO) {
                List<Path> paths = Arrays.stream(files).map(File::toPath).toList();
                controller.openFiles(paths);
            }
        }
    }

    private void handleOpenFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Music Folder");
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
                Now Playing Dark Edition

                Built with Java Swing and JavaFX Media.
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
}
