package com.vodx.visuals.gui;

import com.vodx.visuals.Colors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** "Liquid glass" drawing helpers: rounded translucent gradients, soft shadows, specular borders. */
public final class Glass {
    /** Global fade multiplier (used for open/close animations). */
    public static float alpha = 1f;

    private Glass() { }

    public static int fade(int argb) {
        int a = (int) ((argb >>> 24) * alpha);
        return (MathHelper.clamp(a, 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    /** Interpolates ARGB including the alpha channel. */
    public static int lerp(int a, int b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Rounded rectangle with a vertical gradient (colors are ARGB, fade is applied). */
    public static void rect(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bot) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        top = fade(top);
        bot = fade(bot);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, lerp(top, bot, (float) i / h));
            ctx.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, lerp(top, bot, (float) (h - 1 - i) / h));
        }
        if (h - 2 * r > 0) {
            ctx.fillGradient(x, y + r, x + w, y + h - r,
                    lerp(top, bot, (float) r / h), lerp(top, bot, (float) (h - r) / h));
        }
    }

    /** 1px rounded outline. */
    public static void outline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        color = fade(color);
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            if (i == 0) {
                ctx.fill(x + in, y, x + w - in, y + 1, color);
                ctx.fill(x + in, y + h - 1, x + w - in, y + h, color);
            } else {
                int prev = Math.max(in + 1, inset(r, i - 1));
                ctx.fill(x + in, y + i, x + prev, y + i + 1, color);
                ctx.fill(x + w - prev, y + i, x + w - in, y + i + 1, color);
                ctx.fill(x + in, y + h - 1 - i, x + prev, y + h - i, color);
                ctx.fill(x + w - prev, y + h - 1 - i, x + w - in, y + h - i, color);
            }
        }
        if (h - 2 * r > 0) {
            ctx.fill(x, y + r, x + 1, y + h - r, color);
            ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
    }

    /** Big glass window: soft shadow, frosted body, specular top edge and sheen. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, int r) {
        for (int k = 1; k <= 4; k++) {
            rect(ctx, x - k * 2, y - k * 2 + 5, w + k * 4, h + k * 4, r + k * 2, 0x07000000, 0x07000000);
        }
        rect(ctx, x, y, w, h, r, Colors.alpha(0xFFFFFF, 0x30), Colors.alpha(0xB4C0FF, 0x14));
        outline(ctx, x, y, w, h, r, Colors.alpha(0xFFFFFF, 0x66));
        // specular highlight + sheen
        ctx.fill(x + r, y + 1, x + w - r, y + 2, fade(Colors.alpha(0xFFFFFF, 0x55)));
        ctx.fillGradient(x + r, y + 2, x + w - r, y + h / 4,
                fade(Colors.alpha(0xFFFFFF, 0x1E)), fade(Colors.alpha(0xFFFFFF, 0x00)));
    }

    /** Small dark glass pill used by the HUD. */
    public static void pill(DrawContext ctx, int x, int y, int w, int h) {
        int r = Math.min(5, h / 2);
        rect(ctx, x, y, w, h, r, Colors.alpha(0x1E1E32, 0x98), Colors.alpha(0x10101C, 0x98));
        outline(ctx, x, y, w, h, r, Colors.alpha(0xFFFFFF, 0x36));
    }

    /** Text with the global fade; skipped when nearly transparent (MC treats tiny alpha as opaque). */
    public static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        int c = fade(color);
        if ((c >>> 24) < 8) return;
        ctx.drawText(tr, s, x, y, c, shadow);
    }
}
