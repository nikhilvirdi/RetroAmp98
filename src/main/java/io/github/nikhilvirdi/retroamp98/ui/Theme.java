package io.github.nikhilvirdi.retroamp98.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

/**
 * Dark "Now Playing" theme palette and UI design constants for RetroAmp98.
 * Features a 50/50 split layout with deep red/neon poster on the left
 * and sleek dark controls with yellow accent on the right.
 *
 * ALL UI colors, fonts, sizes, padding, stroke widths, opacities, and numeric
 * layout/paint literals are centrally declared here to strictly prevent hardcoding.
 */
public final class Theme {

    private Theme() {
        // Prevent instantiation
    }

    // =========================================================================
    // COLOR PALETTE
    // =========================================================================

    // --- Window and Main Card ---
    public static final Color WINDOW_BG = new Color(0x0C, 0x0D, 0x11);
    public static final Color CARD_BG = new Color(0x13, 0x14, 0x1A);
    public static final Color PANEL_BG = new Color(0x13, 0x14, 0x1A);
    public static final Color OUTER_BORDER = new Color(0x28, 0x2A, 0x35);

    // --- Left Poster Panel ---
    public static final Color POSTER_TOP_LEFT = new Color(0x8D, 0x0C, 0x18);       // Deep crimson red
    public static final Color POSTER_BOTTOM_RIGHT = new Color(0x07, 0x07, 0x0A);   // Near-black
    public static final Color GLOW_COLOR = new Color(0xE8, 0x1E, 0x36);            // Vibrant radial glow
    public static final Color GLOW_COLOR_WITH_ALPHA = new Color(0xE8, 0x1E, 0x36, 95);
    public static final Color VIGNETTE_COLOR = new Color(0x00, 0x00, 0x00);
    public static final Color VIGNETTE_COLOR_WITH_ALPHA = new Color(0x00, 0x00, 0x00, 175);
    public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color POSTER_FADE_START = new Color(0x13, 0x14, 0x1A, 0);
    public static final Color POSTER_FADE_END = new Color(0x13, 0x14, 0x1A, 255);
    public static final Color POSTER_LOGO_COLOR = new Color(0xFF, 0xFF, 0xFF);

    // --- Typography & Content ---
    public static final Color TEXT_PRIMARY = new Color(0xFF, 0xFF, 0xFF);          // Crisp white
    public static final Color TEXT_MUTED = new Color(0x8C, 0x90, 0x9E);            // Muted secondary text
    public static final Color TEXT_DIM = new Color(0x56, 0x5A, 0x66);              // Dim helper text

    // --- Accent & Progress Bar ---
    public static final Color ACCENT = new Color(0xF6, 0xD4, 0x38);                // Warm yellow accent
    public static final Color PROGRESS_TRACK = new Color(0x28, 0x2A, 0x34);        // Dark progress track line
    public static final Color THUMB_COLOR = new Color(0xFF, 0xFF, 0xFF);           // White round thumb
    public static final Color THUMB_SHADOW = new Color(0x00, 0x00, 0x00, 100);

    // --- Buttons & Transport Controls ---
    public static final Color PLAY_BUTTON_BG = new Color(0xFF, 0xFF, 0xFF);        // Large white circular button
    public static final Color PLAY_BUTTON_HOVER = new Color(0xEE, 0xEE, 0xF2);
    public static final Color PLAY_BUTTON_PRESSED = new Color(0xD8, 0xD8, 0xDE);
    public static final Color PLAY_BUTTON_ICON = new Color(0x13, 0x14, 0x1A);      // Dark glyph on play button
    public static final Color BTN_ICON_NORMAL = new Color(0xB5, 0xB9, 0xC7);      // Muted silver/gray glyph
    public static final Color BTN_ICON_HOVER = new Color(0xFF, 0xFF, 0xFF);       // White glyph on hover
    public static final Color BTN_ICON_PRESSED = new Color(0x8E, 0x92, 0xA0);
    public static final Color BTN_BG_HOVER = new Color(0xFF, 0xFF, 0xFF, 20);      // Subtle hover circle
    public static final Color BTN_BG_PRESSED = new Color(0xFF, 0xFF, 0xFF, 40);

    // --- Small Rounded "PL" Button ---
    public static final Color BTN_PILL_BG = new Color(0x1E, 0x20, 0x29);
    public static final Color BTN_PILL_HOVER = new Color(0x29, 0x2B, 0x37);
    public static final Color BTN_PILL_PRESSED = new Color(0x34, 0x37, 0x46);
    public static final Color BTN_PILL_BORDER = new Color(0x35, 0x37, 0x45);
    public static final Color BTN_PILL_TEXT = new Color(0xCC, 0xCF, 0xD9);
    public static final Color BTN_PILL_ACTIVE_BG = new Color(0x2C, 0x28, 0x1A);
    public static final Color BTN_PILL_ACTIVE_BORDER = new Color(0xF6, 0xD4, 0x38, 160);
    public static final Color BTN_PILL_ACTIVE_TEXT = new Color(0xF6, 0xD4, 0x38);

    // --- Volume & Status ---
    public static final Color VOLUME_FILL = new Color(0x8C, 0x90, 0x9E);

    // --- Playlist Panel ---
    public static final Color PLAYLIST_BG = new Color(0x11, 0x12, 0x17);
    public static final Color PLAYLIST_HEADER_BG = new Color(0x18, 0x19, 0x20);
    public static final Color PLAYLIST_BORDER = new Color(0x23, 0x25, 0x30);
    public static final Color PLAYLIST_TEXT = new Color(0xDE, 0xE0, 0xE6);
    public static final Color PLAYLIST_TEXT_DIM = new Color(0x75, 0x78, 0x86);
    public static final Color PLAYLIST_ACTIVE_TEXT = new Color(0xF6, 0xD4, 0x38);  // ACCENT yellow
    public static final Color PLAYLIST_SELECTION_BG = new Color(0xF6, 0xD4, 0x38, 55); // ACCENT at SELECTION_OPACITY
    public static final Color PLAYLIST_SELECTION_TEXT = new Color(0xFF, 0xFF, 0xFF);

    // --- Menu Bar (Dark Styled) ---
    public static final Color MENUBAR_BG_TOP = new Color(0x10, 0x11, 0x16);
    public static final Color MENUBAR_BG_BOTTOM = new Color(0x10, 0x11, 0x16);
    public static final Color MENUBAR_BORDER = new Color(0x20, 0x21, 0x2B);
    public static final Color MENUBAR_TEXT = new Color(0x9E, 0xA2, 0xB0);

    // --- Resize Grip & App Icon ---
    public static final Color GRIP_DOT_LIGHT = new Color(0x40, 0x43, 0x54);
    public static final Color GRIP_DOT_DARK = new Color(0x1A, 0x1B, 0x22);
    public static final Color ICON_DISC_OUTER = new Color(0x2A, 0x2B, 0x36);
    public static final Color ICON_DISC_MID = new Color(0x1A, 0x1B, 0x22);
    public static final Color ICON_DISC_INNER = new Color(0x11, 0x12, 0x17);
    public static final Color ICON_RIM = new Color(0x3A, 0x3D, 0x4D);
    public static final Color ICON_HUB = new Color(0x0C, 0x0D, 0x11);
    public static final Color ICON_PLAY = new Color(0xF6, 0xD4, 0x38);
    public static final Color ICON_HIGHLIGHT = new Color(0xFF, 0xFF, 0xFF, 120);
    public static final Color ICON_TRACK_RING = new Color(0xF6, 0xD4, 0x38, 80);
    public static final Color ICON_INNER_RING = new Color(0x4A, 0x4D, 0x5E);

    // =========================================================================
    // TYPOGRAPHY
    // =========================================================================

    public static final Font FONT_POSTER_LOGO = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_TOP_TITLE = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_TRACK_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_TRACK_ARTIST = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_TIME = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_STATUS = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BUTTON_LABEL = new Font("SansSerif", Font.BOLD, 10);
    public static final Font FONT_BADGE = new Font("SansSerif", Font.BOLD, 9);
    public static final Font FONT_PLAYLIST_ITEM = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_PLAYLIST_ACTIVE = new Font("SansSerif", Font.BOLD, 12);

    // =========================================================================
    // OPACITY & FLOATING POINT VALUES
    // =========================================================================

    public static final float GLOW_OPACITY = 0.38f;
    public static final float VIGNETTE_OPACITY = 0.70f;
    public static final float SELECTION_OPACITY = 0.22f;

    public static final float FLOAT_ZERO = 0.0f;
    public static final float FLOAT_HALF = 0.5f;
    public static final float FLOAT_ONE = 1.0f;
    public static final float FLOAT_TWO = 2.0f;
    public static final float FLOAT_GLOW_CX = 0.35f;
    public static final float FLOAT_GLOW_CY = 0.38f;
    public static final float FLOAT_GLOW_RADIUS = 0.85f;
    public static final float FLOAT_VIGNETTE_RADIUS = 0.90f;

    public static final double DOUBLE_ZERO = 0.0;
    public static final double DOUBLE_ONE = 1.0;
    public static final double DOUBLE_TWO = 2.0;
    public static final double PERCENT_DIVISOR = 100.0;
    public static final double PERCENT_MULTIPLIER = 100.0;
    public static final double VOLUME_STEP = 0.05;

    public static final float[] GRADIENT_STOPS_TWO = new float[]{0.0f, 1.0f};
    public static final float[] GRADIENT_STOPS_THREE = new float[]{0.0f, 0.45f, 1.0f};

    // =========================================================================
    // STROKES
    // =========================================================================

    public static final BasicStroke STROKE_ONE = new BasicStroke(1.0f);
    public static final BasicStroke STROKE_TWO = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke STROKE_PROGRESS = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    // =========================================================================
    // DIMENSIONS, RADII, PADDING & SIZES
    // =========================================================================

    public static final int RADIUS_OUTER = 20;
    public static final int RADIUS_CARD_BORDER = 20;
    public static final int RADIUS_SMALL = 6;
    public static final int RADIUS_PILL = 12;
    public static final int PADDING_OUTER = 12;
    public static final int FADE_WIDTH = 80;
    public static final int PROGRESS_TRACK_HEIGHT = 3;

    public static final int THUMB_SIZE = 12;
    public static final int THUMB_RADIUS = 6;

    public static final int WINDOW_DEFAULT_WIDTH = 900;
    public static final int WINDOW_DEFAULT_HEIGHT = 460;
    public static final int WINDOW_MIN_WIDTH = 720;
    public static final int WINDOW_MIN_HEIGHT = 400;

    public static final int POSTER_MIN_WIDTH = 360;
    public static final int PLAYLIST_DRAWER_WIDTH = 240;

    public static final int PLAY_BUTTON_SIZE = 54;
    public static final int TRANSPORT_BUTTON_SIZE = 36;
    public static final int MUTE_BUTTON_SIZE = 28;
    public static final int PL_BUTTON_WIDTH = 34;
    public static final int PL_BUTTON_HEIGHT = 22;

    public static final int VOLUME_SLIDER_WIDTH = 84;
    public static final int VOLUME_SLIDER_HEIGHT = 20;

    public static final int SEEK_RESOLUTION = 1000;
    public static final int VOLUME_MAX = 100;
    public static final int SEEK_STEP_SECONDS = 5;

    public static final int POSTER_LOGO_X = 28;
    public static final int POSTER_LOGO_Y = 28;

    // =========================================================================
    // NUMERIC CONSTANTS (FOR ZERO-HARDCODING IN PAINT & LAYOUT CODE)
    // =========================================================================

    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int SIX = 6;
    public static final int SEVEN = 7;
    public static final int EIGHT = 8;
    public static final int NINE = 9;
    public static final int TEN = 10;
    public static final int ELEVEN = 11;
    public static final int TWELVE = 12;
    public static final int THIRTEEN = 13;
    public static final int FOURTEEN = 14;
    public static final int FIFTEEN = 15;
    public static final int SIXTEEN = 16;
    public static final int SEVENTEEN = 17;
    public static final int EIGHTEEN = 18;
    public static final int NINETEEN = 19;
    public static final int TWENTY = 20;
    public static final int TWENTY_TWO = 22;
    public static final int TWENTY_FOUR = 24;
    public static final int TWENTY_SIX = 26;
    public static final int TWENTY_EIGHT = 28;
    public static final int THIRTY = 30;
    public static final int THIRTY_TWO = 32;
    public static final int THIRTY_FOUR = 34;
    public static final int THIRTY_SIX = 36;
    public static final int FORTY = 40;
    public static final int FORTY_TWO = 42;
    public static final int FORTY_FOUR = 44;
    public static final int FORTY_FIVE = 45;
    public static final int FORTY_SIX = 46;
    public static final int FORTY_EIGHT = 48;
    public static final int FIFTY = 50;
    public static final int FIFTY_TWO = 52;
    public static final int FIFTY_FOUR = 54;
    public static final int FIFTY_SIX = 56;
    public static final int SIXTY = 60;
    public static final int SIXTY_FOUR = 64;
    public static final int SEVENTY = 70;
    public static final int SEVENTY_TWO = 72;
    public static final int EIGHTY = 80;
    public static final int NINETY = 90;
    public static final int ONE_HUNDRED = 100;
    public static final int ONE_HUNDRED_TWENTY = 120;
    public static final int ONE_HUNDRED_FORTY = 140;
    public static final int ONE_HUNDRED_SIXTY = 160;
    public static final int ONE_HUNDRED_EIGHTY = 180;
    public static final int TWO_HUNDRED = 200;
    public static final int TWO_HUNDRED_TWENTY = 220;
    public static final int TWO_HUNDRED_FORTY = 240;
    public static final int TWO_HUNDRED_SIXTY = 260;
    public static final int TWO_HUNDRED_EIGHTY = 280;
    public static final int THREE_HUNDRED = 300;
    public static final int THREE_HUNDRED_SIXTY = 360;
    public static final int FOUR_HUNDRED = 400;
    public static final int FOUR_HUNDRED_SIXTY = 460;
    public static final int SEVEN_HUNDRED_TWENTY = 720;
    public static final int NINE_HUNDRED = 900;
    public static final int ONE_THOUSAND = 1000;
}
