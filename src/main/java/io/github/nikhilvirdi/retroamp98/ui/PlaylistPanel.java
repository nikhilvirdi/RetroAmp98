package io.github.nikhilvirdi.retroamp98.ui;

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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Windows Media Player 9 Series styled playlist panel.
 * Features dark slate library background, cyan active track indicator,
 * and matching sleek controls.
 */
public class PlaylistPanel extends JPanel implements ControllerListener {

    private final PlayerController controller;
    private final DefaultListModel<Track> listModel = new DefaultListModel<>();
    private final JList<Track> trackList = new JList<>(listModel);
    private final java.util.Map<Integer, String> tagTitles = new java.util.HashMap<>();
    private int activeIndex = -1;

    public PlaylistPanel(PlayerController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createLineBorder(Theme.PLAYLIST_BORDER, 1));

        // Header strip
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint headerGrad = new GradientPaint(
                        0, 0, Theme.PLAYLIST_HEADER_BG,
                        0, getHeight(), Theme.PLAYLIST_HEADER_BG.darker()
                );
                g2.setPaint(headerGrad);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel titleLabel = new JLabel("Now Playing List");
        titleLabel.setFont(Theme.FONT_BUTTON_LABEL);
        titleLabel.setForeground(Theme.STATUS_TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // List configuration
        trackList.setBackground(Theme.PLAYLIST_BG);
        trackList.setForeground(Theme.PLAYLIST_TEXT);
        trackList.setSelectionBackground(Theme.PLAYLIST_SELECTION_BG);
        trackList.setSelectionForeground(Theme.PLAYLIST_SELECTION_TEXT);
        trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trackList.setCellRenderer(new WmpTrackCellRenderer());

        // Double-click to play
        trackList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int selected = trackList.locationToIndex(e.getPoint());
                    if (selected >= 0) {
                        controller.playTrack(selected);
                    }
                }
            }
        });

        // Delete key removal
        trackList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int selected = trackList.getSelectedIndex();
                    if (selected >= 0) {
                        controller.removeTrack(selected);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(trackList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.PLAYLIST_BG);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
        bottomBar.setBackground(Theme.PLAYLIST_HEADER_BG.darker());
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.PLAYLIST_BORDER));

        RoundGlossButton removeBtn = new RoundGlossButton("- Remove", 64, 20, "Remove selected track (Delete)");
        removeBtn.addActionListener(e -> {
            int selected = trackList.getSelectedIndex();
            if (selected >= 0) {
                controller.removeTrack(selected);
            }
        });

        RoundGlossButton clearBtn = new RoundGlossButton("Clear", 50, 20, "Clear entire playlist");
        clearBtn.addActionListener(e -> controller.clearPlaylist());

        bottomBar.add(removeBtn);
        bottomBar.add(clearBtn);
        add(bottomBar, BorderLayout.SOUTH);

        controller.addListener(this);
        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        List<Track> tracks = controller.getPlaylistManager().getPlaylist().getTracks();
        for (Track t : tracks) {
            listModel.addElement(t);
        }
        this.activeIndex = controller.getPlaylistManager().getCurrentIndex();
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
        if (index >= 0 && index < listModel.size()) {
            trackList.ensureIndexIsVisible(index);
        }
    }

    @Override
    public void onTrackInfoReady(io.github.nikhilvirdi.retroamp98.engine.TrackInfo trackInfo) {
        if (trackInfo != null && trackInfo.getDisplayTitle() != null && activeIndex >= 0) {
            tagTitles.put(activeIndex, trackInfo.getDisplayTitle());
            trackList.repaint();
        }
    }

    private class WmpTrackCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Track track) {
                boolean isActive = (index == activeIndex);
                String prefix = isActive ? "\u25B6 " : "   ";
                String displayTitle = tagTitles.get(index);
                if (displayTitle == null) {
                    displayTitle = (track.getArtist() != null && !track.getArtist().isBlank())
                            ? track.getArtist() + " - " + track.getTitle()
                            : track.getTitle();
                }
                setText(prefix + (index + 1) + ". " + displayTitle);

                if (isActive) {
                    setFont(Theme.FONT_PLAYLIST_ACTIVE);
                    setForeground(isSelected ? Theme.PLAYLIST_SELECTION_TEXT : Theme.PLAYLIST_ACTIVE_TEXT);
                } else {
                    setFont(Theme.FONT_PLAYLIST_ITEM);
                    setForeground(isSelected ? Theme.PLAYLIST_SELECTION_TEXT : Theme.PLAYLIST_TEXT);
                }

                setBackground(isSelected ? Theme.PLAYLIST_SELECTION_BG : Theme.PLAYLIST_BG);
                setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            }
            return c;
        }
    }
}
