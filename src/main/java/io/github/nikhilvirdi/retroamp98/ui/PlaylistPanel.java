package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collapsible playlist drawer for RetroAmp98.
 * Styled in the dark theme with selected row in Theme.ACCENT at Theme.SELECTION_OPACITY,
 * active playing track in Theme.ACCENT with bold text and play marker, and double-click / delete key handlers.
 */
public class PlaylistPanel extends JPanel implements ControllerListener {

    private static final String PLAY_PREFIX = "\u25B6 ";
    private static final String IDLE_PREFIX = "   ";
    private static final String DOT_SPACE = ". ";
    private static final String HYPHEN_SPACE = " - ";

    private final PlayerController controller;
    private final DefaultListModel<Track> listModel = new DefaultListModel<>();
    private final JList<Track> trackList = new JList<>(listModel);
    private final Map<Integer, String> tagTitles = new HashMap<>();
    private int activeIndex = -Theme.ONE;

    public PlaylistPanel(PlayerController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(Theme.ZERO, Theme.ZERO));
        setOpaque(true);
        setBackground(Theme.PLAYLIST_BG);
        setBorder(BorderFactory.createLineBorder(Theme.PLAYLIST_BORDER, Theme.ONE));
        setPreferredSize(new Dimension(Theme.PLAYLIST_DRAWER_WIDTH, Theme.THREE_HUNDRED));
        setMinimumSize(new Dimension(Theme.ONE_HUNDRED_EIGHTY, Theme.TWO_HUNDRED));

        // Header strip
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.PLAYLIST_HEADER_BG);
                g2.fillRect(Theme.ZERO, Theme.ZERO, getWidth(), getHeight());
                g2.dispose();
            }
        };
        headerPanel.setBorder(BorderFactory.createEmptyBorder(Theme.FOUR, Theme.EIGHT, Theme.FOUR, Theme.EIGHT));

        JLabel titleLabel = new JLabel("Now Playing List");
        titleLabel.setFont(Theme.FONT_BUTTON_LABEL);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Track list configuration
        trackList.setBackground(Theme.PLAYLIST_BG);
        trackList.setForeground(Theme.PLAYLIST_TEXT);
        trackList.setSelectionBackground(Theme.PLAYLIST_SELECTION_BG);
        trackList.setSelectionForeground(Theme.PLAYLIST_SELECTION_TEXT);
        trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trackList.setCellRenderer(new DarkTrackCellRenderer());

        // Double-click to play
        trackList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == Theme.TWO && e.getButton() == MouseEvent.BUTTON1) {
                    int selected = trackList.locationToIndex(e.getPoint());
                    if (selected >= Theme.ZERO && PlaylistPanel.this.controller != null) {
                        PlaylistPanel.this.controller.playTrack(selected);
                    }
                }
            }
        });

        // Delete key to remove track
        trackList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int selected = trackList.getSelectedIndex();
                    if (selected >= Theme.ZERO && PlaylistPanel.this.controller != null) {
                        PlaylistPanel.this.controller.removeTrack(selected);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(trackList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.PLAYLIST_BG);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.FOUR, Theme.THREE));
        bottomBar.setBackground(Theme.PLAYLIST_HEADER_BG);
        bottomBar.setBorder(BorderFactory.createMatteBorder(Theme.ONE, Theme.ZERO, Theme.ZERO, Theme.ZERO, Theme.PLAYLIST_BORDER));

        RoundGlossButton removeBtn = new RoundGlossButton("- Remove", Theme.SIXTY_FOUR, Theme.TWENTY, "Remove selected track (Delete)");
        removeBtn.addActionListener(e -> {
            int selected = trackList.getSelectedIndex();
            if (selected >= Theme.ZERO && PlaylistPanel.this.controller != null) {
                PlaylistPanel.this.controller.removeTrack(selected);
            }
        });

        RoundGlossButton clearBtn = new RoundGlossButton("Clear", Theme.FIFTY, Theme.TWENTY, "Clear entire playlist");
        clearBtn.addActionListener(e -> {
            if (PlaylistPanel.this.controller != null) {
                PlaylistPanel.this.controller.clearPlaylist();
            }
        });

        bottomBar.add(removeBtn);
        bottomBar.add(clearBtn);
        add(bottomBar, BorderLayout.SOUTH);

        if (controller != null) {
            controller.addListener(this);
            refreshList();
        }
    }

    private void refreshList() {
        listModel.clear();
        if (controller != null && controller.getPlaylistManager() != null) {
            List<Track> tracks = controller.getPlaylistManager().getPlaylist().getTracks();
            for (Track t : tracks) {
                listModel.addElement(t);
            }
            this.activeIndex = controller.getPlaylistManager().getCurrentIndex();
        }
        trackList.repaint();
    }

    @Override
    public void onPlaylistUpdated() {
        refreshList();
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        this.activeIndex = index;
        trackList.repaint();
        if (index >= Theme.ZERO && index < listModel.size()) {
            trackList.ensureIndexIsVisible(index);
        }
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        if (trackInfo != null && trackInfo.getDisplayTitle() != null && activeIndex >= Theme.ZERO) {
            tagTitles.put(activeIndex, trackInfo.getDisplayTitle());
            trackList.repaint();
        }
    }

    private class DarkTrackCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Track track) {
                boolean isActive = (index == activeIndex);
                String prefix = isActive ? PLAY_PREFIX : IDLE_PREFIX;
                String displayTitle = tagTitles.get(index);
                if (displayTitle == null) {
                    displayTitle = (track.getArtist() != null && !track.getArtist().isBlank())
                            ? track.getArtist() + HYPHEN_SPACE + track.getTitle()
                            : track.getTitle();
                }
                setText(prefix + (index + Theme.ONE) + DOT_SPACE + displayTitle);

                if (isActive) {
                    setFont(Theme.FONT_PLAYLIST_ACTIVE);
                    setForeground(isSelected ? Theme.PLAYLIST_SELECTION_TEXT : Theme.PLAYLIST_ACTIVE_TEXT);
                } else {
                    setFont(Theme.FONT_PLAYLIST_ITEM);
                    setForeground(isSelected ? Theme.PLAYLIST_SELECTION_TEXT : Theme.PLAYLIST_TEXT);
                }

                setBackground(isSelected ? Theme.PLAYLIST_SELECTION_BG : Theme.PLAYLIST_BG);
                setBorder(BorderFactory.createEmptyBorder(Theme.TWO, Theme.FOUR, Theme.TWO, Theme.FOUR));
            }
            return c;
        }
    }
}
