package io.github.nikhilvirdi.retroamp98.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Windows Media Player 9 Series ("Corona" XP era) theme palette.
 * Light silver-blue metallic body with dark video/status areas and glossy jewel controls.
 */
public final class Theme {

    private Theme() {
        // Central palette repository
    }

    // --- Window Body & Gradients (Light Silver-Blue Metallic) ---
    public static final Color BODY_GRADIENT_TOP = new Color(0xEA, 0xF1, 0xF9);     // #EAF1F9
    public static final Color BODY_GRADIENT_MID = new Color(0xC5, 0xD7, 0xEB);     // #C5D7EB
    public static final Color BODY_GRADIENT_BOTTOM = new Color(0x8E, 0xAC, 0xCB);  // #8EACCB
    public static final Color BODY_BORDER_OUTER = new Color(0x52, 0x72, 0x96);     // #527296
    public static final Color BODY_HIGHLIGHT = new Color(0xFF, 0xFF, 0xFF, 200);   // #FFFFFF highlight line

    // --- Menu Bar (Blends with light silver-blue body) ---
    public static final Color MENUBAR_BG_TOP = new Color(0xF2, 0xF6, 0xFC);        // #F2F6FC
    public static final Color MENUBAR_BG_BOTTOM = new Color(0xD8, 0xE5, 0xF4);     // #D8E5F4
    public static final Color MENUBAR_BORDER = new Color(0xA4, 0xBA, 0xD2);        // Subtle bottom border #A4BAD2
    public static final Color MENUBAR_TEXT = new Color(0x18, 0x2A, 0x3E);          // #182A3E

    // --- Video Display Viewport ---
    public static final Color VIDEO_BG = Color.BLACK;
    public static final Color VIDEO_FRAME_BORDER = new Color(0x1B, 0x2A, 0x3B);    // #1B2A3B
    public static final Color VIDEO_TEXT_COLOR = new Color(0x78, 0x8C, 0xA0);      // #788CA0

    // --- Status Strip (Dark Bar with Green LCD Text) ---
    public static final Color STATUS_BG_TOP = new Color(0x0C, 0x14, 0x1E);         // #0C141E
    public static final Color STATUS_BG_BOTTOM = new Color(0x16, 0x22, 0x30);      // #162230
    public static final Color STATUS_BORDER = new Color(0x28, 0x3A, 0x4E);         // #283A4E
    public static final Color STATUS_LCD_TEXT = new Color(0x32, 0xFF, 0x32);       // Phosphor green #32FF32
    public static final Color STATUS_LCD_DIM = new Color(0x1C, 0xAA, 0x1C);        // Dim green #1CAA1C
    public static final Color STATUS_TIME_TEXT = new Color(0x32, 0xFF, 0x32);      // Green LCD counter

    // LED Indicator Colors
    public static final Color LED_PLAYING = new Color(0x00, 0xE6, 0x4D);          // Bright green #00E64D
    public static final Color LED_PAUSED = new Color(0xFF, 0xBB, 0x00);           // Amber yellow #FFBB00
    public static final Color LED_LOADING = new Color(0x00, 0xB4, 0xD8);          // Cyan #00B4D8
    public static final Color LED_STOPPED = new Color(0x40, 0x55, 0x40);          // Dim olive-gray #405540
    public static final Color LED_ERROR = new Color(0xE6, 0x39, 0x46);            // Red #E63946

    // --- Glossy Button Colors (Transport & Inactive Toggles) ---
    public static final Color BTN_BORDER = new Color(0x28, 0x46, 0x68);            // #284668
    public static final Color BTN_GRAD_TOP = new Color(0xE4, 0xF0, 0xFA);          // #E4F0FA
    public static final Color BTN_GRAD_MID = new Color(0x8C, 0xB5, 0xDE);          // #8CB5DE
    public static final Color BTN_GRAD_BOTTOM = new Color(0x44, 0x72, 0xA2);       // #4472A2
    public static final Color BTN_HOVER_GLOW = new Color(0xB8, 0xDC, 0xFF);        // #B8DCFF
    public static final Color BTN_PRESSED_TOP = new Color(0x2C, 0x53, 0x7E);       // #2C537E
    public static final Color BTN_PRESSED_BOTTOM = new Color(0x56, 0x82, 0xB4);    // #5682B4
    public static final Color BTN_ICON_NORMAL = Color.WHITE;
    public static final Color BTN_ICON_SHADOW = new Color(0x18, 0x30, 0x4C);       // #18304C
    public static final Color BTN_TEXT_INACTIVE = new Color(0x18, 0x2E, 0x46);     // Dark navy text for inactive pills

    // --- Active Highlighted State for Toggle Buttons (Rpt, Shuf, PL) ---
    public static final Color BTN_ACTIVE_BORDER = new Color(0x0B, 0x42, 0x76);     // #0B4276
    public static final Color BTN_ACTIVE_GRAD_TOP = new Color(0x6E, 0xBA, 0xF6);   // Bright cyan-blue #6EBAF6
    public static final Color BTN_ACTIVE_GRAD_MID = new Color(0x2B, 0x80, 0xCE);   // Mid blue #2B80CE
    public static final Color BTN_ACTIVE_GRAD_BOTTOM = new Color(0x14, 0x4E, 0x88);// Deep blue #144E88
    public static final Color BTN_ACTIVE_TEXT = Color.WHITE;
    public static final Color BTN_ACTIVE_GLOW = new Color(0x50, 0xC4, 0xFF, 160);

    // --- Seek & Volume Sliders ---
    public static final Color SLIDER_TRACK_BG = new Color(0xB0, 0xC4, 0xD8);       // Thicker silver track #B0C4D8
    public static final Color SLIDER_TRACK_BORDER = new Color(0x7E, 0x96, 0xB0);   // Track groove border #7E96B0
    public static final Color SLIDER_TRACK_FILL = new Color(0x2A, 0x7E, 0xD8);     // Played blue fill #2A7ED8
    public static final Color SLIDER_TRACK_FILL_DARK = new Color(0x19, 0x5B, 0xA8);// #195BA8
    public static final Color SLIDER_THUMB_TOP = new Color(0xF4, 0xF8, 0xFD);      // Rounded glossy thumb #F4F8FD
    public static final Color SLIDER_THUMB_MID = new Color(0xB2, 0xCE, 0xEA);      // #B2CEEA
    public static final Color SLIDER_THUMB_BOTTOM = new Color(0x60, 0x88, 0xB6);   // #6088B6
    public static final Color SLIDER_THUMB_BORDER = new Color(0x32, 0x4E, 0x70);   // #324E70

    // --- Playlist Panel ---
    public static final Color PLAYLIST_BG = new Color(0x13, 0x1E, 0x2B);           // #131E2B
    public static final Color PLAYLIST_HEADER_BG = new Color(0x2D, 0x42, 0x5A);    // #2D425A
    public static final Color PLAYLIST_BORDER = new Color(0x62, 0x7E, 0x9E);       // #627E9E
    public static final Color PLAYLIST_TEXT = new Color(0xD8, 0xE6, 0xF4);         // #D8E6F4
    public static final Color PLAYLIST_TEXT_DIM = new Color(0x8A, 0xA2, 0xBC);     // #8AA2BC
    public static final Color PLAYLIST_SELECTION_BG = new Color(0x2A, 0x5E, 0x98); // #2A5E98
    public static final Color PLAYLIST_ACTIVE_TEXT = new Color(0x50, 0xD2, 0xFF);   // Glowing cyan #50D2FF

    // --- Resize Grip ---
    public static final Color GRIP_DOT_LIGHT = new Color(0xFF, 0xFF, 0xFF, 220);
    public static final Color GRIP_DOT_DARK = new Color(0x56, 0x74, 0x96);

    // --- Typography ---
    public static final Font FONT_STATUS_TITLE = new Font("Tahoma", Font.BOLD, 11);
    public static final Font FONT_STATUS_LCD = new Font("Monospaced", Font.BOLD, 12);
    public static final Font FONT_BUTTON_LABEL = new Font("Tahoma", Font.BOLD, 10);
    public static final Font FONT_PLAYLIST_ITEM = new Font("Tahoma", Font.PLAIN, 11);
    public static final Font FONT_PLAYLIST_ACTIVE = new Font("Tahoma", Font.BOLD, 11);
}
