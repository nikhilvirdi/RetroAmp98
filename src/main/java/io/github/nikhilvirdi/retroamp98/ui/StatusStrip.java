package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.Track;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Duration;

/**
 * Track block in the Right Control Panel for RetroAmp98.
 * Displays artist above in Theme.TEXT_MUTED and large bold title below in Theme.TEXT_PRIMARY,
 * truncated with "..." when exceeding available width.
 */
public class StatusStrip extends JPanel implements ControllerListener {

    private static final String DEFAULT_ARTIST = "RetroAmp98";
    private static final String DEFAULT_TITLE = "No Track Loaded";
    private static final String ELLIPSIS = "...";

    private String currentArtist = DEFAULT_ARTIST;
    private String currentTitle = DEFAULT_TITLE;

    public StatusStrip(PlayerController controller) {
        setOpaque(false);
        setPreferredSize(new Dimension(Theme.THREE_HUNDRED, Theme.SIXTY_FOUR));
        setMinimumSize(new Dimension(Theme.TWO_HUNDRED, Theme.FORTY_EIGHT));

        if (controller != null) {
            controller.addListener(this);
        }
    }

    public void setTrack(String artist, String title) {
        this.currentArtist = (artist != null && !artist.isBlank()) ? artist : DEFAULT_ARTIST;
        this.currentTitle = (title != null && !title.isBlank()) ? title : DEFAULT_TITLE;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int maxTextWidth = Math.max(Theme.ZERO, w - Theme.FOUR);

        // 1. Draw Artist above in Theme.TEXT_MUTED
        g2.setFont(Theme.FONT_TRACK_ARTIST);
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics fmArtist = g2.getFontMetrics();
        String displayArtist = truncate(currentArtist, maxTextWidth, fmArtist);
        int artistY = fmArtist.getAscent() + Theme.TWO;
        g2.drawString(displayArtist, Theme.ZERO, artistY);

        // 2. Draw Title below in Theme.TEXT_PRIMARY (Large & Bold)
        g2.setFont(Theme.FONT_TRACK_TITLE);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fmTitle = g2.getFontMetrics();
        String displayTitle = truncate(currentTitle, maxTextWidth, fmTitle);
        int titleY = artistY + fmTitle.getAscent() + Theme.SIX;
        g2.drawString(displayTitle, Theme.ZERO, titleY);

        g2.dispose();
    }

    private String truncate(String text, int maxWidth, FontMetrics fm) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = fm.stringWidth(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return ELLIPSIS;
        }

        int len = text.length();
        while (len > Theme.ZERO && (fm.stringWidth(text.substring(Theme.ZERO, len)) + ellipsisWidth) > maxWidth) {
            len--;
        }
        return text.substring(Theme.ZERO, len) + ELLIPSIS;
    }

    @Override
    public void onStateChanged(PlayerState state) {
        if (state == PlayerState.IDLE && (currentTitle == null || currentTitle.equals(DEFAULT_TITLE))) {
            setTrack(DEFAULT_ARTIST, DEFAULT_TITLE);
        }
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        // Handled by progress bar
    }

    @Override
    public void onDurationKnown(Duration duration) {
        // Handled by progress bar
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        if (track != null) {
            String art = track.getArtist();
            String tit = track.getTitle();
            setTrack(art, tit);
        } else {
            setTrack(DEFAULT_ARTIST, DEFAULT_TITLE);
        }
    }

    @Override
    public void onTrackInfoReady(TrackInfo trackInfo) {
        if (trackInfo != null) {
            String art = trackInfo.artist();
            String tit = trackInfo.title();
            if (tit == null || tit.isBlank()) {
                tit = trackInfo.getDisplayTitle();
            }
            setTrack(art, tit);
        }
    }
}
