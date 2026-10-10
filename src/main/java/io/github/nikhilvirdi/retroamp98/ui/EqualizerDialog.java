package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.engine.EqualizerPresets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

/**
 * 10-band graphic equalizer dialog styled in the Windows Media Player 9 theme.
 * Offers standard acoustic presets and live band gain adjustment.
 */
public class EqualizerDialog extends JDialog {

    private final PlayerController controller;
    private final JCheckBox enableCheckBox;
    private final JComboBox<String> presetComboBox;
    private final SkinSlider[] bandSliders = new SkinSlider[EqualizerPresets.NUM_BANDS];
    private final JLabel[] dbLabels = new JLabel[EqualizerPresets.NUM_BANDS];

    public EqualizerDialog(JFrame parent, PlayerController controller) {
        super(parent, "Equalizer - RetroAmp98", false);
        this.controller = controller;

        JPanel contentPane = new JPanel(new BorderLayout(6, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint bgGrad = new GradientPaint(
                        0, 0, Theme.BODY_GRADIENT_TOP,
                        0, getHeight(), Theme.BODY_GRADIENT_BOTTOM
                );
                g2.setPaint(bgGrad);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Bar: Enable checkbox + Presets dropdown ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setOpaque(false);

        enableCheckBox = new JCheckBox("Enable Equalizer", controller.getMediaEngine().isEqualizerEnabled());
        enableCheckBox.setOpaque(false);
        enableCheckBox.setFont(Theme.FONT_BUTTON_LABEL);
        enableCheckBox.setForeground(Theme.MENUBAR_TEXT);
        enableCheckBox.addActionListener(e -> {
            boolean enabled = enableCheckBox.isSelected();
            controller.getMediaEngine().setEqualizerEnabled(enabled);
            updateSlidersEnabled(enabled);
        });

        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setFont(Theme.FONT_BUTTON_LABEL);
        presetLabel.setForeground(Theme.MENUBAR_TEXT);

        presetComboBox = new JComboBox<>(EqualizerPresets.getPresetNames().toArray(new String[0]));
        presetComboBox.setFont(Theme.FONT_BUTTON_LABEL);
        presetComboBox.addActionListener(e -> {
            String selected = (String) presetComboBox.getSelectedItem();
            if (selected != null) {
                double[] gains = EqualizerPresets.getPreset(selected);
                for (int i = 0; i < gains.length; i++) {
                    controller.getMediaEngine().setEqualizerBand(i, gains[i]);
                    bandSliders[i].setValue((int) Math.round(gains[i]));
                    dbLabels[i].setText(formatGain(gains[i]));
                }
            }
        });

        topBar.add(enableCheckBox);
        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(presetLabel);
        topBar.add(presetComboBox);

        contentPane.add(topBar, BorderLayout.NORTH);

        // --- Center: 10 vertical sliders ---
        JPanel slidersPanel = new JPanel(new GridLayout(1, EqualizerPresets.NUM_BANDS, 6, 0));
        slidersPanel.setOpaque(false);
        slidersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BODY_BORDER_OUTER, 1),
                BorderFactory.createEmptyBorder(8, 4, 8, 4)
        ));

        for (int i = 0; i < EqualizerPresets.NUM_BANDS; i++) {
            final int bandIdx = i;
            JPanel bandCol = new JPanel(new BorderLayout(0, 4));
            bandCol.setOpaque(false);

            double currentGain = controller.getMediaEngine().getEqualizerBand(bandIdx);

            JLabel dbLabel = new JLabel(formatGain(currentGain), SwingConstants.CENTER);
            dbLabel.setFont(Theme.FONT_BUTTON_LABEL);
            dbLabel.setForeground(Theme.MENUBAR_TEXT);
            dbLabels[bandIdx] = dbLabel;

            SkinSlider slider = new SkinSlider(JSlider.VERTICAL, -12, 12, (int) Math.round(currentGain));
            slider.setPreferredSize(new Dimension(28, 120));
            slider.addChangeListener(e -> {
                int val = slider.getValue();
                controller.getMediaEngine().setEqualizerBand(bandIdx, val);
                dbLabel.setText(formatGain(val));
            });
            bandSliders[bandIdx] = slider;

            JLabel freqLabel = new JLabel(EqualizerPresets.BAND_LABELS[bandIdx], SwingConstants.CENTER);
            freqLabel.setFont(Theme.FONT_BUTTON_LABEL);
            freqLabel.setForeground(Theme.MENUBAR_TEXT);

            bandCol.add(dbLabel, BorderLayout.NORTH);
            bandCol.add(slider, BorderLayout.CENTER);
            bandCol.add(freqLabel, BorderLayout.SOUTH);

            slidersPanel.add(bandCol);
        }

        contentPane.add(slidersPanel, BorderLayout.CENTER);

        setContentPane(contentPane);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void updateSlidersEnabled(boolean enabled) {
        for (SkinSlider s : bandSliders) {
            s.setEnabled(enabled);
        }
    }

    private static String formatGain(double gain) {
        if (gain > 0) return String.format("+%.0fdB", gain);
        return String.format("%.0fdB", gain);
    }
}
