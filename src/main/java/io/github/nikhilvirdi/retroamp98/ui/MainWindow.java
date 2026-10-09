package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Windows Media Player 9 Series ("Corona" XP era) styled main application window.
 * Features silver-blue metallic body, top menu bar, central stretching video area,
 * dark status strip, collapsible playlist, and glossy bottom docked controls.
 */
public class MainWindow extends JFrame implements ControllerListener {

    private final PlayerController controller;

    private VideoPanel videoPanel;
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
        super("RetroAmp98");
        this.controller = controller;
        this.controller.setParentWindow(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        JPanel videoContainer = new JPanel(new BorderLayout(0, 2));
        videoContainer.setOpaque(false);

        videoPanel = new VideoPanel();
        statusStrip = new StatusStrip(controller);

        videoContainer.add(videoPanel, BorderLayout.CENTER);
        videoContainer.add(statusStrip, BorderLayout.SOUTH);

        centerWorkspace.add(videoContainer, BorderLayout.CENTER);

        // Collapsible Playlist Panel on the side
        playlistPanel = new PlaylistPanel(controller);
        playlistPanel.setPreferredSize(new Dimension(250, 200));
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

        bottomControls.add(seekBar);
        bottomControls.add(transportBar);

        rootPanel.add(bottomControls, BorderLayout.SOUTH);

        setJMenuBar(buildMenuBar());
        setContentPane(rootPanel);

        pack();
        setLocationRelativeTo(null);

        controller.addListener(this);
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

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- File Menu ---
        JMenu fileMenu = new JMenu("File");

        JMenuItem openFileItem = new JMenuItem("Open File...");
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
        exitItem.addActionListener(e -> {
            controller.stop();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        // --- Play Menu ---
        JMenu playMenu = new JMenu("Play");

        JMenuItem playPauseItem = new JMenuItem("Play / Pause");
        playPauseItem.addActionListener(e -> controller.playPause());
        playMenu.add(playPauseItem);

        JMenuItem stopItem = new JMenuItem("Stop");
        stopItem.addActionListener(e -> controller.stop());
        playMenu.add(stopItem);

        JMenuItem prevItem = new JMenuItem("Previous");
        prevItem.addActionListener(e -> controller.previous());
        playMenu.add(prevItem);

        JMenuItem nextItem = new JMenuItem("Next");
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
        shuffleMenuItem.addActionListener(e -> controller.setShuffle(shuffleMenuItem.isSelected()));
        playMenu.add(shuffleMenuItem);

        // --- View Menu ---
        JMenu viewMenu = new JMenu("View");
        playlistViewItem = new JCheckBoxMenuItem("Playlist", true);
        playlistViewItem.addActionListener(e -> setPlaylistVisible(playlistViewItem.isSelected()));
        viewMenu.add(playlistViewItem);

        // --- Help Menu ---
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About RetroAmp98");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(playMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        return menuBar;
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
                controller.savePlaylist(file.toPath());
            }
        }
    }

    private void showAboutDialog() {
        String message = """
                RetroAmp98
                Version 0.1.0

                Windows Media Player 9 Series (XP Era) Styled Media Player.
                Built with Java 21, Swing Java2D Skin, and JavaFX Media.
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
