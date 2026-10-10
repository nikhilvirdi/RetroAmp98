package io.github.nikhilvirdi.retroamp98.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Center display panel for video and audio visualization.
 * Hosts the engine's plain Swing JComponent video surface and
 * seamlessly toggles to an AWT canvas for cover art, spectrum bars,
 * or the "No video" placeholder.
 */
public class VideoPanel extends JPanel implements ControllerListener {

    private static final Font NO_VIDEO_FONT = new Font("Tahoma", Font.PLAIN, 13);

    private final CardLayout cardLayout = new CardLayout();
    private final JComponent videoSurface;
    private final CanvasPanel canvasPanel;

    private Runnable onDoubleClickListener;
    private boolean hasVideo = false;

    public VideoPanel(PlayerController controller) {
        setLayout(cardLayout);
        setBackground(Theme.VIDEO_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.VIDEO_FRAME_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        this.videoSurface = controller.getMediaEngine().getVideoComponent();
        this.canvasPanel = new CanvasPanel();

        add(canvasPanel, "CANVAS");
        if (videoSurface != null) {
            add(videoSurface, "VIDEO");
            videoSurface.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && onDoubleClickListener != null) {
                        onDoubleClickListener.run();
                    }
                }
            });
        }

        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && onDoubleClickListener != null) {
                    onDoubleClickListener.run();
                }
            }
        });

        cardLayout.show(this, "CANVAS");
        controller.addListener(this);
    }

    public void setOnDoubleClickListener(Runnable listener) {
        this.onDoubleClickListener = listener;
    }

    public void setCoverArt(Image coverImage) {
        canvasPanel.setCoverArt(coverImage);
    }

    public void setSpectrumData(float[] magnitudes) {
        canvasPanel.setSpectrumData(magnitudes);
    }

    public void setVisualizerEnabled(boolean enabled) {
        canvasPanel.setVisualizerEnabled(enabled);
    }

    public boolean isVisualizerEnabled() {
        return canvasPanel.isVisualizerEnabled();
    }

    public void clearVideo() {
        this.hasVideo = false;
        canvasPanel.clear();
        cardLayout.show(this, "CANVAS");
        revalidate();
        repaint();
    }

    @Override
    public void onHasVideoChanged(boolean hasVideo) {
        this.hasVideo = hasVideo;
        if (hasVideo) {
            cardLayout.show(this, "VIDEO");
        } else {
            cardLayout.show(this, "CANVAS");
        }
        revalidate();
        repaint();
    }

    @Override
    public void onTrackInfoReady(io.github.nikhilvirdi.retroamp98.engine.TrackInfo trackInfo) {
        if (trackInfo != null) {
            setCoverArt(trackInfo.coverImage());
        } else {
            setCoverArt(null);
        }
    }

    @Override
    public void onSpectrumData(float[] magnitudes) {
        if (!hasVideo && isShowing()) {
            setSpectrumData(magnitudes);
        }
    }

    @Override
    public void onStateChanged(io.github.nikhilvirdi.retroamp98.engine.PlayerState state) {
        if (state == io.github.nikhilvirdi.retroamp98.engine.PlayerState.PAUSED
                || state == io.github.nikhilvirdi.retroamp98.engine.PlayerState.STOPPED
                || state == io.github.nikhilvirdi.retroamp98.engine.PlayerState.IDLE) {
            setSpectrumData(null);
        }
    }

    /**
     * Inner canvas drawing cover art, spectrum bars, or the "No video" placeholder.
     */
    private static class CanvasPanel extends JPanel {
        private Image coverArt;
        private float[] spectrumMagnitudes;
        private boolean visualizerEnabled = true;

        CanvasPanel() {
            setBackground(Theme.VIDEO_BG);
        }

        void setCoverArt(Image image) {
            this.coverArt = image;
            repaint();
        }

        void setSpectrumData(float[] magnitudes) {
            this.spectrumMagnitudes = magnitudes;
            if (visualizerEnabled && coverArt == null) {
                repaint();
            }
        }

        void setVisualizerEnabled(boolean enabled) {
            this.visualizerEnabled = enabled;
            repaint();
        }

        boolean isVisualizerEnabled() {
            return visualizerEnabled;
        }

        void clear() {
            this.coverArt = null;
            this.spectrumMagnitudes = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();

            g2.setColor(Theme.VIDEO_BG);
            g2.fillRect(0, 0, w, h);

            if (coverArt != null) {
                paintCoverArt(g2, w, h);
            } else if (visualizerEnabled && spectrumMagnitudes != null && spectrumMagnitudes.length > 0) {
                paintSpectrum(g2, w, h);
            } else {
                paintNoVideoPlaceholder(g2, w, h);
            }

            g2.dispose();
        }

        private void paintCoverArt(Graphics2D g2, int panelW, int panelH) {
            int imgW = coverArt.getWidth(null);
            int imgH = coverArt.getHeight(null);
            if (imgW <= 0 || imgH <= 0) {
                paintNoVideoPlaceholder(g2, panelW, panelH);
                return;
            }

            double imgAspect = (double) imgW / imgH;
            double panelAspect = (double) panelW / panelH;
            int drawW, drawH;
            if (imgAspect > panelAspect) {
                drawW = (int) (panelW * 0.75);
                drawH = (int) Math.round(drawW / imgAspect);
            } else {
                drawH = (int) (panelH * 0.75);
                drawW = (int) Math.round(drawH * imgAspect);
            }

            int x = (panelW - drawW) / 2;
            int y = (panelH - drawH) / 2;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(coverArt, x, y, drawW, drawH, null);

            // Subtle border around cover art
            g2.setColor(Theme.VIDEO_FRAME_BORDER);
            g2.drawRect(x, y, drawW, drawH);
        }

        private void paintSpectrum(Graphics2D g2, int panelW, int panelH) {
            int numBands = Math.min(spectrumMagnitudes.length, 32);
            if (numBands == 0) return;

            int padding = 16;
            int availW = panelW - (padding * 2);
            int barGap = 3;
            int totalGaps = (numBands - 1) * barGap;
            int barWidth = Math.max(2, (availW - totalGaps) / numBands);
            int totalBarsW = (numBands * barWidth) + totalGaps;
            int startX = (panelW - totalBarsW) / 2;
            int maxBarHeight = panelH - 32;

            for (int i = 0; i < numBands; i++) {
                float mag = spectrumMagnitudes[i]; // Typically -60dB to 0dB
                // Normalize from [-60, 0] dB to [0.0, 1.0]
                float normalized = Math.max(0.0f, Math.min(1.0f, (mag + 60.0f) / 60.0f));
                int barHeight = Math.max(2, (int) (normalized * maxBarHeight));
                int bx = startX + i * (barWidth + barGap);
                int by = panelH - 16 - barHeight;

                // Color gradient from bright blue to cyan
                g2.setColor(Theme.SLIDER_TRACK_FILL);
                g2.fillRect(bx, by, barWidth, barHeight);

                // Top highlight cap
                g2.setColor(Theme.PLAYLIST_ACTIVE_TEXT);
                g2.fillRect(bx, by, barWidth, Math.min(2, barHeight));
            }
        }

        private void paintNoVideoPlaceholder(Graphics2D g2, int w, int h) {
            g2.setColor(Theme.VIDEO_TEXT_COLOR);
            g2.setFont(NO_VIDEO_FONT);
            FontMetrics fm = g2.getFontMetrics();
            String message = "No video";
            int textW = fm.stringWidth(message);
            int textH = fm.getAscent();
            int x = (w - textW) / 2;
            int y = (h + textH) / 2 - 2;
            g2.drawString(message, x, y);
        }
    }
}
