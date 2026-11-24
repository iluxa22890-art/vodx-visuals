package com.vodx.visuals;

import net.minecraft.util.math.MathHelper;

public final class Colors {
    public static final int PANEL = 0xF0161622;
    public static final int SIDEBAR = 0xFF121219;
    public static final int CARD = 0xFF1E1E2C;
    public static final int CARD_HOVER = 0xFF26263A;
    public static final int FIELD = 0xFF1A1A26;
    public static final int TEXT = 0xFFE8E8F2;
    public static final int MUTED = 0xFF8C8CA6;
    public static final int HUD_BG = 0x90101018;

    private Colors() { }

    /** Accent color (ARGB). Animated when the Rainbow theme is on. */
    public static int accent(int offset) {
        if (Modules.RAINBOW.enabled) {
            float h = (float) (((System.currentTimeMillis() / 20L) + offset) % 360L) / 360f;
            return 0xFF000000 | MathHelper.hsvToRgb(h, 0.65f, 1f);
        }
        return 0xFF8E6BFF;
    }

    public static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }

    public static int alpha(int color, int a) {
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
