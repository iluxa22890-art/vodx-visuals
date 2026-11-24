package com.vodx.visuals.gui;

import com.vodx.visuals.Category;
import com.vodx.visuals.Colors;
import com.vodx.visuals.Module;
import com.vodx.visuals.Modules;
import com.vodx.visuals.Setting;
import com.vodx.visuals.VodxVisualsClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** Liquid-glass menu: blurred world behind, frosted panels, animated open/close, tabs, switches and sliders. */
public class ClickGuiScreen extends Screen {
    private static final int SB = 100, HD = 40, ROW_H = 32, SET_H = 24, GAP = 5, TAB_H = 22, TAB_STEP = 26;
    private static final int SWITCH_W = 30, SWITCH_H = 16;

    private record Row(Module m, int y, int h) { }

    private Category tab = null; // null = all
    private String query = "";
    private double scroll, targetScroll;
    private int contentH;
    private Setting.Num dragging;
    private int dragX, dragW;
    private int px, py, pw, ph, cx, cy, cw, ch;

    private final long openStart = System.currentTimeMillis();
    private long closeStart;
    private boolean closing;
    private float tabPillY = -1f, pillA = 1f;

    public ClickGuiScreen() {
        super(Text.literal("Vodx Visuals"));
    }

    // ------------------------------------------------------------ animation
    private static float ease(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private float progress() {
        long now = System.currentTimeMillis();
        if (closing) return 1f - ease(MathHelper.clamp((now - closeStart) / 180f, 0f, 1f));
        return ease(MathHelper.clamp((now - openStart) / 280f, 0f, 1f));
    }

    private void layout(float e) {
        pw = Math.min(520, width - 20);
        ph = Math.min(330, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1f - e) * 20f);
        cx = px + SB + 12;
        cy = py + HD + 8;
        cw = pw - SB - 24;
        ch = ph - HD - 16;
    }

    @Override
    protected void init() {
        layout(progress());
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            closeStart = System.currentTimeMillis();
        }
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --------------------------------------------------------------- layout
    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        boolean searching = !query.isBlank();
        int y = cy - (int) Math.round(scroll);
        int total = 0;
        for (Module m : Modules.ALL) {
            if (!searching && tab != null && m.category != tab) continue;
            if (searching && !m.matches(query)) continue;
            int h = ROW_H + Math.round(m.expandAnim * (m.settings.size() * SET_H + 8));
            out.add(new Row(m, y, h));
            y += h + GAP;
            total += h + GAP;
        }
        contentH = total;
        return out;
    }

    private int setY(Row r, int i) {
        return r.y() + ROW_H + 4 + i * SET_H;
    }

    // --------------------------------------------------------------- render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float e = progress();
        if (closing && e <= 0.001f) {
            if (client != null) client.setScreen(null);
            return;
        }
        layout(e);
        for (Module m : Modules.ALL) {
            m.expandAnim += ((m.expanded ? 1f : 0f) - m.expandAnim) * 0.22f;
            if (Math.abs((m.expanded ? 1f : 0f) - m.expandAnim) < 0.003f) m.expandAnim = m.expanded ? 1f : 0f;
        }

        super.renderBackground(ctx, mx, my, delta); // real blur behind (Video settings > Menu Background Blur)
        Glass.alpha = e;
        try {
            ctx.fill(0, 0, width, height, Glass.fade(0x38000000));
            Glass.panel(ctx, px, py, pw, ph, 16);

            // sidebar
            Glass.rect(ctx, px + 8, py + 8, SB - 6, ph - 16, 12, Colors.alpha(0xFFFFFF, 0x1C), Colors.alpha(0xFFFFFF, 0x0A));
            Glass.outline(ctx, px + 8, py + 8, SB - 6, ph - 16, 12, Colors.alpha(0xFFFFFF, 0x26));
            Glass.rect(ctx, px + 18, py + 17, 6, 6, 3, Colors.accent(0), Colors.accent(40));
            Glass.text(ctx, textRenderer, "VODX", px + 28, py + 15, Colors.accent(0), true);
            Glass.text(ctx, textRenderer, "VISUALS", px + 18, py + 27, Colors.MUTED, false);
            drawTabs(ctx, mx, my);
            Glass.text(ctx, textRenderer, "v2.2.0", px + 18, py + ph - 22, Colors.MUTED, false);

            // search pill
            int sx = px + SB + 12, sy = py + 10, sw = pw - SB - 24;
            Glass.rect(ctx, sx, sy, sw, 22, 11, Colors.alpha(0xFFFFFF, 0x20), Colors.alpha(0xFFFFFF, 0x0E));
            int pulse = 0x60 + (int) (0x30 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0)));
            Glass.outline(ctx, sx, sy, sw, 22, 11, Colors.alpha(Colors.accent(0), pulse));
            boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
            if (query.isEmpty()) {
                Glass.text(ctx, textRenderer, "Search functions...", sx + 10, sy + 7, Colors.MUTED, false);
            } else {
                Glass.text(ctx, textRenderer, query + (caret ? "_" : ""), sx + 10, sy + 7, Colors.TEXT, false);
            }

            // content
            List<Row> rows = rows();
            targetScroll = MathHelper.clamp(targetScroll, 0, Math.max(0, contentH - ch));
            scroll += (targetScroll - scroll) * 0.35;

            if (!query.isEmpty()) {
                String cnt = rows.size() + " found";
                Glass.text(ctx, textRenderer, cnt, sx + sw - textRenderer.getWidth(cnt) - 10, sy + 7, Colors.MUTED, false);
            }

            ctx.enableScissor(cx, cy, cx + cw, cy + ch);
            if (rows.isEmpty()) {
                String none = "No functions found";
                Glass.text(ctx, textRenderer, none, cx + (cw - textRenderer.getWidth(none)) / 2, cy + 24, Colors.MUTED, true);
            }
            for (Row r : rows) {
                if (r.y() + r.h() < cy || r.y() > cy + ch) continue;
                drawRow(ctx, r, mx, my);
            }
            ctx.disableScissor();
        } finally {
            Glass.alpha = 1f;
        }
    }

    private void drawTabs(DrawContext ctx, int mx, int my) {
        int x = px + 12, w = SB - 18, y0 = py + HD + 12;
        int idx = tab == null ? 0 : tab.ordinal() + 1;
        float target = y0 + idx * TAB_STEP;
        if (tabPillY < 0) tabPillY = target;
        tabPillY += (target - tabPillY) * 0.25f;
        boolean searching = !query.isBlank();
        pillA += ((searching ? 0f : 1f) - pillA) * 0.2f;
        int acc = Colors.accent(0);
        if (pillA > 0.03f) {
            Glass.rect(ctx, x, (int) tabPillY, w, TAB_H, 9,
                    Colors.alpha(acc, (int) (0x58 * pillA)), Colors.alpha(acc, (int) (0x24 * pillA)));
            Glass.outline(ctx, x, (int) tabPillY, w, TAB_H, 9, Colors.alpha(acc, (int) (0x90 * pillA)));
        }
        Category[] cats = Category.values();
        for (int i = 0; i <= cats.length; i++) {
            String label = i == 0 ? "All" : cats[i - 1].label;
            int y = y0 + i * TAB_STEP;
            boolean active = !searching && idx == i;
            boolean hover = inside(mx, my, x, y, w, TAB_H);
            if (hover && !active) {
                Glass.rect(ctx, x, y, w, TAB_H, 9, Colors.alpha(0xFFFFFF, 0x1A), Colors.alpha(0xFFFFFF, 0x0A));
            }
            Glass.text(ctx, textRenderer, label, x + 10, y + 7, active ? Colors.TEXT : Colors.MUTED, true);
        }
    }

    private void drawRow(DrawContext ctx, Row r, int mx, int my) {
        Module m = r.m();
        boolean hov = !closing && inside(mx, my, cx, r.y(), cw, ROW_H) && my >= cy && my < cy + ch;
        m.hover += ((hov ? 1f : 0f) - m.hover) * 0.3f;
        m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.22f;
        int acc = Colors.accent(r.y());

        Glass.rect(ctx, cx, r.y(), cw, r.h(), 10,
                Colors.alpha(0xFFFFFF, 0x24 + (int) (0x12 * m.hover)),
                Colors.alpha(0xFFFFFF, 0x10 + (int) (0x0C * m.hover)));
        if (m.anim > 0.01f) {
            Glass.rect(ctx, cx, r.y(), cw, r.h(), 10,
                    Colors.alpha(acc, (int) (0x48 * m.anim)), Colors.alpha(acc, (int) (0x14 * m.anim)));
        }
        float lit = Math.max(m.hover, m.anim);
        Glass.outline(ctx, cx, r.y(), cw, r.h(), 10,
                Colors.alpha(Colors.mix(0xFFFFFFFF, acc, m.anim), 0x30 + (int) (0x48 * lit)));
        ctx.fill(cx + 10, r.y() + 1, cx + cw - 10, r.y() + 2, Glass.fade(Colors.alpha(0xFFFFFF, 0x1C + (int) (0x20 * m.hover))));

        Glass.text(ctx, textRenderer, m.name, cx + 14, r.y() + 7, m.enabled ? Colors.TEXT : 0xFFC4C4D8, true);
        String desc = textRenderer.trimToWidth(m.description, cw - 100);
        Glass.text(ctx, textRenderer, desc, cx + 14, r.y() + 19, Colors.MUTED, false);

        // switch
        int sx = cx + cw - SWITCH_W - 12, sy = r.y() + (ROW_H - SWITCH_H) / 2;
        int track = Colors.alpha(Colors.mix(0xFFFFFFFF, acc, m.anim), 0x38 + (int) ((0xD0 - 0x38) * m.anim));
        Glass.rect(ctx, sx, sy, SWITCH_W, SWITCH_H, 8, track, Colors.alpha(track, Math.max(0x20, (track >>> 24) - 0x30)));
        Glass.outline(ctx, sx, sy, SWITCH_W, SWITCH_H, 8, Colors.alpha(0xFFFFFF, 0x40));
        int kx = sx + 2 + Math.round((SWITCH_W - SWITCH_H) * m.anim);
        Glass.rect(ctx, kx, sy + 3, 12, 12, 6, 0x30000000, 0x30000000); // knob shadow
        Glass.rect(ctx, kx, sy + 2, 12, 12, 6, 0xFFFFFFFF, 0xFFD6DBF0);
        if (!m.settings.isEmpty()) {
            Glass.text(ctx, textRenderer, m.expanded ? "-" : "+", sx - 14, r.y() + 12, Colors.MUTED, false);
        }

        if (m.expandAnim <= 0.02f) return;
        float prev = Glass.alpha;
        Glass.alpha = prev * m.expandAnim;
        ctx.enableScissor(cx, r.y() + ROW_H, cx + cw, r.y() + r.h());
        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int y = setY(r, i);
            int x = cx + 18, w = cw - 36;
            Glass.text(ctx, textRenderer, s.name, x, y + 3, Colors.TEXT, false);
            if (s instanceof Setting.Bool b) {
                int bx = x + w - 14;
                Glass.rect(ctx, bx, y + 1, 14, 14, 5, Colors.alpha(0xFFFFFF, 0x34), Colors.alpha(0xFFFFFF, 0x18));
                Glass.outline(ctx, bx, y + 1, 14, 14, 5, Colors.alpha(0xFFFFFF, 0x44));
                if (b.value) Glass.rect(ctx, bx + 3, y + 4, 8, 8, 3, Colors.alpha(Colors.accent(y), 0xF0), Colors.alpha(Colors.accent(y + 30), 0xC0));
            } else if (s instanceof Setting.Mode md) {
                String v = md.get();
                int tw = textRenderer.getWidth(v);
                Glass.rect(ctx, x + w - tw - 14, y, tw + 14, 16, 8,
                        Colors.alpha(Colors.accent(y), 0x50), Colors.alpha(Colors.accent(y), 0x24));
                Glass.outline(ctx, x + w - tw - 14, y, tw + 14, 16, 8, Colors.alpha(Colors.accent(y), 0x90));
                Glass.text(ctx, textRenderer, v, x + w - tw - 7, y + 4, Colors.TEXT, false);
            } else if (s instanceof Setting.Num n) {
                String v = n.display();
                Glass.text(ctx, textRenderer, v, x + w - textRenderer.getWidth(v), y + 3, Colors.MUTED, false);
                double t = (n.value - n.min) / (n.max - n.min);
                int fw = (int) (w * t);
                Glass.rect(ctx, x, y + 16, w, 5, 2, Colors.alpha(0xFFFFFF, 0x30), Colors.alpha(0xFFFFFF, 0x16));
                Glass.rect(ctx, x, y + 16, Math.max(5, fw), 5, 2,
                        Colors.alpha(Colors.accent(y), 0xF0), Colors.alpha(Colors.accent(y + 40), 0xC0));
                boolean drag = dragging == n;
                if (drag) Glass.rect(ctx, x + fw - 7, y + 11, 14, 14, 7, Colors.alpha(Colors.accent(y), 0x40), Colors.alpha(Colors.accent(y), 0x40));
                Glass.rect(ctx, x + fw - 4, y + 14, 8, 9, 4, 0xFFFFFFFF, 0xFFD6DBF0);
            }
        }
        ctx.disableScissor();
        Glass.alpha = prev;
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        layout(progress());

        int x = px + 12, w = SB - 18, y0 = py + HD + 12;
        Category[] cats = Category.values();
        for (int i = 0; i <= cats.length; i++) {
            if (inside(mx, my, x, y0 + i * TAB_STEP, w, TAB_H)) {
                selectTab(i == 0 ? null : cats[i - 1]);
                return true;
            }
        }
        if (!query.isEmpty() && button == 1 && inside(mx, my, px + SB + 12, py + 10, pw - SB - 24, 22)) {
            query = "";
            targetScroll = 0;
            return true;
        }
        if (!inside(mx, my, cx, cy, cw, ch)) return super.mouseClicked(mx, my, button);

        for (Row r : rows()) {
            Module m = r.m();
            if (inside(mx, my, cx, r.y(), cw, ROW_H)) {
                int sx = cx + cw - SWITCH_W - 12;
                boolean arrowZone = !m.settings.isEmpty() && mx >= sx - 26 && mx < sx - 4;
                if (button == 1 || (button == 0 && arrowZone)) {
                    if (!m.settings.isEmpty()) m.expanded = !m.expanded;
                } else if (button == 0) {
                    m.toggle();
                }
                return true;
            }
            if (m.expandAnim > 0.9f) {
                for (int i = 0; i < m.settings.size(); i++) {
                    int y = setY(r, i);
                    if (!inside(mx, my, cx + 10, y, cw - 20, SET_H)) continue;
                    Setting s = m.settings.get(i);
                    if (s instanceof Setting.Bool b) {
                        b.value = !b.value;
                    } else if (s instanceof Setting.Mode md) {
                        if (button == 1) md.prev(); else md.next();
                    } else if (s instanceof Setting.Num n && button == 0) {
                        dragging = n;
                        dragX = cx + 18;
                        dragW = cw - 36;
                        updateSlider(mx);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void selectTab(Category c) {
        tab = c;
        query = "";
        scroll = 0;
        targetScroll = 0;
    }

    private void updateSlider(double mx) {
        if (dragging == null) return;
        double t = MathHelper.clamp((mx - dragX) / dragW, 0, 1);
        dragging.set(dragging.min + t * (dragging.max - dragging.min));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            updateSlider(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        targetScroll = MathHelper.clamp(targetScroll - vertical * 30, 0, Math.max(0, contentH - ch));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            targetScroll = 0;
            return true;
        }
        if (keyCode >= 256 && VodxVisualsClient.MENU_KEY != null
                && VodxVisualsClient.MENU_KEY.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127 && query.length() < 24) {
            query += chr;
            targetScroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Modules.save();
        super.removed();
    }
}
