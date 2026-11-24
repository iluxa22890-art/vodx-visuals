package com.vodx.visuals;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.vodx.visuals.gui.Glass;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class HudRenderer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasAttack;
    private static LivingEntity lastTarget, shownFor;
    private static long lastTargetTime;
    private static float shownHealth;

    private HudRenderer() { }

    public static void init() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(HudRenderer::tick);
    }

    public static LivingEntity target() {
        if (lastTarget != null && lastTarget.isAlive() && System.currentTimeMillis() - lastTargetTime < 4000) return lastTarget;
        return null;
    }

    public static void onHit(LivingEntity e) {
        lastTarget = e;
        lastTargetTime = System.currentTimeMillis();
    }

    private static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean a = mc.options.attackKey.isPressed();
        if (a && !wasAttack) clicks.add(now);
        wasAttack = a;
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
    }

    private static void label(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color) {
        Glass.pill(ctx, x - 4, y - 3, tr.getWidth(s) + 8, 13);
        ctx.drawText(tr, s, x, y, color, true);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        if (Modules.WATERMARK.enabled) {
            String s = "Vodx | " + mc.getSession().getUsername() + " | " + mc.getCurrentFps() + " fps";
            int tw = tr.getWidth(s);
            Glass.pill(ctx, 4, 4, tw + 10, 14);
            ctx.fill(9, 16, 4 + tw + 5, 17, Colors.accent(0));
            ctx.drawText(tr, s, 9, 8, Colors.TEXT, true);
        }

        if (Modules.CLOCK.enabled) {
            String s = LocalTime.now().format(TIME);
            label(ctx, tr, s, (w - tr.getWidth(s)) / 2, 6, Colors.TEXT);
        }

        if (Modules.MODULE_LIST.enabled) {
            List<String> names = new ArrayList<>();
            for (Module m : Modules.ALL) {
                if (m.enabled && m != Modules.MODULE_LIST && m != Modules.RAINBOW) names.add(m.name);
            }
            names.sort((a, b) -> tr.getWidth(b) - tr.getWidth(a));
            int y = 4;
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i);
                int tw = tr.getWidth(n);
                int x = w - 4 - tw - 8;
                Glass.pill(ctx, x, y, w - 2 - x, 12);
                ctx.fill(w - 5, y + 3, w - 3, y + 9, Colors.accent(i * 25));
                ctx.drawText(tr, n, x + 3, y + 2, Colors.TEXT, true);
                y += 12;
            }
        }

        if (Modules.POTION_HUD.enabled) {
            int y = 28;
            for (StatusEffectInstance e : p.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
                String time;
                if (e.isInfinite()) {
                    time = "inf";
                } else {
                    int sec = e.getDuration() / 20;
                    time = String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
                }
                int col = e.getEffectType().value().isBeneficial() ? 0xFF7CFFB2 : 0xFFFF7C7C;
                label(ctx, tr, name + "  " + time, 6, y, col);
                y += 13;
            }
        }

        if (Modules.KEYSTROKES.enabled) {
            int kx = 6, ky = h / 2 - 30;
            key(ctx, tr, kx + 22, ky, "W", mc.options.forwardKey.isPressed());
            key(ctx, tr, kx, ky + 22, "A", mc.options.leftKey.isPressed());
            key(ctx, tr, kx + 22, ky + 22, "S", mc.options.backKey.isPressed());
            key(ctx, tr, kx + 44, ky + 22, "D", mc.options.rightKey.isPressed());
            boolean space = mc.options.jumpKey.isPressed();
            ctx.fill(kx, ky + 44, kx + 64, ky + 56, space ? Colors.alpha(Colors.accent(0), 200) : Colors.HUD_BG);
            ctx.fill(kx + 20, ky + 49, kx + 44, ky + 51, Colors.TEXT);
            label(ctx, tr, "CPS " + clicks.size(), kx, ky + 62, Colors.TEXT);
        }

        // bottom-left info
        List<String> info = new ArrayList<>();
        if (Modules.INFO.enabled) {
            info.add(String.format(Locale.ROOT, "XYZ %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()));
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            info.add("Ping " + ping + " ms");
        }
        if (Modules.SPEED.enabled) {
            Vec3d v = p.getVelocity();
            info.add(String.format(Locale.ROOT, "Speed %.2f m/s", Math.hypot(v.x, v.z) * 20.0));
        }
        int iy = h - 4 - info.size() * 13;
        for (String s : info) {
            label(ctx, tr, s, 6, iy, Colors.TEXT);
            iy += 13;
        }

        if (Modules.ARMOR_HUD.enabled) {
            EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
            int ay = h - 4 - 4 * 18;
            for (EquipmentSlot slot : slots) {
                ItemStack st = p.getEquippedStack(slot);
                if (!st.isEmpty()) {
                    ctx.drawItem(st, w - 24, ay);
                    ctx.drawStackOverlay(tr, st, w - 24, ay);
                }
                ay += 18;
            }
        }

        if (Modules.TARGET_HUD.enabled) targetHud(ctx, mc, tr, w, h);
        if (Modules.CROSSHAIR.enabled) crosshair(ctx, w, h);
        if (Modules.HEALTH_ALERT.enabled) healthAlert(ctx, p, w, h);
    }

    private static void key(DrawContext ctx, TextRenderer tr, int x, int y, String s, boolean down) {
        ctx.fill(x, y, x + 20, y + 20, down ? Colors.alpha(Colors.accent(x), 200) : Colors.HUD_BG);
        ctx.drawCenteredTextWithShadow(tr, s, x + 10, y + 6, Colors.TEXT);
    }

    private static void targetHud(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int w, int h) {
        LivingEntity t = null;
        if (lastTarget != null && lastTarget.isAlive() && System.currentTimeMillis() - lastTargetTime < 4000) {
            t = lastTarget;
        } else if (mc.targetedEntity instanceof LivingEntity le) {
            t = le;
        }
        if (t == null) { shownFor = null; return; }
        if (t != shownFor) { shownFor = t; shownHealth = t.getHealth(); }
        shownHealth += (t.getHealth() - shownHealth) * 0.12f;

        int x = w / 2 + 24, y = h / 2 + 14, bw = 120;
        ctx.fill(x, y, x + bw, y + 34, Colors.HUD_BG);
        ctx.fill(x, y, x + 2, y + 34, Colors.accent(0));
        ctx.drawText(tr, t.getName().getString(), x + 8, y + 5, Colors.TEXT, true);
        float ratio = MathHelper.clamp(shownHealth / Math.max(1f, t.getMaxHealth()), 0f, 1f);
        ctx.fill(x + 8, y + 18, x + bw - 6, y + 22, 0xFF2A2A3A);
        ctx.fill(x + 8, y + 18, x + 8 + (int) ((bw - 14) * ratio), y + 22, Colors.mix(0xFFFF4D4D, 0xFF4DFF88, ratio));
        ctx.drawText(tr, String.format(Locale.ROOT, "%.1f / %.0f", t.getHealth(), t.getMaxHealth()), x + 8, y + 24, Colors.MUTED, false);
    }

    private static void crosshair(DrawContext ctx, int w, int h) {
        int cx = (w - 1) / 2, cy = (h - 1) / 2;
        int gap = Modules.CROSSHAIR_GAP.i(), size = Modules.CROSSHAIR_SIZE.i();
        int c = Colors.accent(0);
        ctx.fill(cx, cy - gap - size, cx + 1, cy - gap, c);
        ctx.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + size, c);
        ctx.fill(cx - gap - size, cy, cx - gap, cy + 1, c);
        ctx.fill(cx + gap + 1, cy, cx + gap + 1 + size, cy + 1, c);
        if (Modules.CROSSHAIR_DOT.value) ctx.fill(cx, cy, cx + 1, cy + 1, Colors.TEXT);
    }

    private static void healthAlert(DrawContext ctx, ClientPlayerEntity p, int w, int h) {
        if (p.getHealth() > Modules.ALERT_HEALTH.value || p.isDead()) return;
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 150.0);
        for (int i = 0; i < 12; i++) {
            int a = (int) ((70 + 60 * pulse) * (1.0 - i / 12.0));
            int c = Colors.alpha(0xFF2020, a);
            ctx.fill(0, i, w, i + 1, c);
            ctx.fill(0, h - i - 1, w, h - i, c);
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - i - 1, 0, w - i, h, c);
        }
    }
}
