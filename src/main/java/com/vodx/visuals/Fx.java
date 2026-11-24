package com.vodx.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Custom world-space effect renderer: additive glow sprites, stars, flat rings, beams, ribbons, cones.
 * Everything is drawn with our own geometry (soft radial gradients), no vanilla particles.
 */
public final class Fx {
    private static final class P {
        double x, y, z, px, py, pz, vx, vy, vz, grav, drag;
        int life, max, color, shape;
        float size;
    }

    private static final class Ring {
        double x, y, z;
        int age, max, off;
        float r0, r1, width;
    }

    private static final class Beam {
        double x, y, z;
        int age, max, off;
    }

    private static final List<P> particles = new ArrayList<>();
    private static final List<Ring> rings = new ArrayList<>();
    private static final List<Beam> beams = new ArrayList<>();
    private static final ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    private static int ticks;
    private static double camX, camY, camZ;

    private Fx() { }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(Fx::tick);
        WorldRenderEvents.LAST.register(Fx::render);
    }

    // ------------------------------------------------------------------ spawning
    public static void spawnHit(LivingEntity e) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int style = Modules.HIT_STYLE.index;
        int n = Modules.HIT_AMOUNT.i();
        double cx = e.getX(), cy = e.getY() + e.getHeight() * (0.4 + r.nextDouble() * 0.4), cz = e.getZ();
        for (int i = 0; i < n && particles.size() < 1800; i++) {
            double dx = r.nextGaussian(), dy = r.nextGaussian() * 0.8 + 0.3, dz = r.nextGaussian();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-6;
            double sp = style == 2 ? 0.05 + r.nextDouble() * 0.07 : 0.10 + r.nextDouble() * 0.22;
            P p = new P();
            p.x = p.px = cx + r.nextGaussian() * 0.08;
            p.y = p.py = cy + r.nextGaussian() * 0.08;
            p.z = p.pz = cz + r.nextGaussian() * 0.08;
            p.vx = dx / len * sp;
            p.vy = dy / len * sp;
            p.vz = dz / len * sp;
            p.life = p.max = 12 + r.nextInt(14);
            p.size = style == 2 ? 0.10f + r.nextFloat() * 0.10f : 0.05f + r.nextFloat() * 0.07f;
            p.shape = style == 1 ? 1 : 0;
            p.grav = 0.012;
            p.drag = 0.9;
            p.color = Colors.accent(i * 17);
            particles.add(p);
        }
    }

    public static void spawnKill(LivingEntity e) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Beam b = new Beam();
        b.x = e.getX();
        b.y = e.getY();
        b.z = e.getZ();
        b.max = 26;
        b.off = r.nextInt(360);
        beams.add(b);
        for (int k = 0; k < 2; k++) {
            Ring ring = new Ring();
            ring.x = e.getX();
            ring.y = e.getY() + 0.05;
            ring.z = e.getZ();
            ring.age = -k * 5;
            ring.max = 18;
            ring.r0 = 0.3f;
            ring.r1 = 2.4f;
            ring.width = 0.18f;
            ring.off = k * 60;
            rings.add(ring);
        }
        for (int i = 0; i < 40 && particles.size() < 1800; i++) {
            P p = new P();
            p.x = p.px = e.getX() + r.nextGaussian() * 0.25;
            p.y = p.py = e.getY() + r.nextDouble() * e.getHeight();
            p.z = p.pz = e.getZ() + r.nextGaussian() * 0.25;
            p.vx = r.nextGaussian() * 0.02;
            p.vy = 0.12 + r.nextDouble() * 0.18;
            p.vz = r.nextGaussian() * 0.02;
            p.life = p.max = 30 + r.nextInt(20);
            p.size = 0.07f + r.nextFloat() * 0.07f;
            p.shape = 1;
            p.grav = -0.002;
            p.drag = 0.94;
            p.color = Colors.accent(i * 9);
            particles.add(p);
        }
    }

    public static void spawnJump(double x, double y, double z) {
        Ring ring = new Ring();
        ring.x = x;
        ring.y = y + 0.03;
        ring.z = z;
        ring.max = Math.max(6, (int) (1.8 / Modules.JUMP_SPEED.value));
        ring.r0 = 0.25f;
        ring.r1 = 1.5f;
        ring.width = 0.10f;
        rings.add(ring);
    }

    // ---------------------------------------------------------------------- tick
    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity pl = mc.player;
        if (pl == null || mc.world == null) {
            particles.clear();
            rings.clear();
            beams.clear();
            trail.clear();
            return;
        }
        ticks++;

        for (P p : particles) {
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.vx *= p.drag;
            p.vz *= p.drag;
            p.vy = (p.vy - p.grav) * p.drag;
            p.life--;
        }
        particles.removeIf(p -> p.life <= 0);
        for (Ring r : rings) r.age++;
        rings.removeIf(r -> r.age >= r.max);
        for (Beam b : beams) b.age++;
        beams.removeIf(b -> b.age >= b.max);

        if (Modules.TRAIL.enabled) {
            Vec3d pos = pl.getPos();
            if (trail.isEmpty() || trail.peekFirst().squaredDistanceTo(pos) > 0.0004) trail.addFirst(pos);
            else if (trail.size() > 1 && ticks % 2 == 0) trail.pollLast();
            while (trail.size() > Modules.TRAIL_LENGTH.i()) trail.pollLast();
        } else {
            trail.clear();
        }

        if (Modules.AMBIENT.enabled && ticks % 2 == 0 && particles.size() < 1500) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            P p = new P();
            p.x = p.px = pl.getX() + (r.nextDouble() - 0.5) * 12;
            p.y = p.py = pl.getY() + r.nextDouble() * 4;
            p.z = p.pz = pl.getZ() + (r.nextDouble() - 0.5) * 12;
            p.vx = r.nextGaussian() * 0.004;
            p.vy = 0.008 + r.nextDouble() * 0.01;
            p.vz = r.nextGaussian() * 0.004;
            p.life = p.max = 50 + r.nextInt(30);
            p.size = 0.03f + r.nextFloat() * 0.03f;
            p.shape = 0;
            p.drag = 1.0;
            p.color = Colors.accent(ticks * 3);
            particles.add(p);
        }
    }

    // -------------------------------------------------------------------- render
    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity pl = mc.player;
        MatrixStack ms = ctx.matrixStack();
        if (pl == null || ms == null) return;

        LivingEntity marker = null;
        if (Modules.TARGET_MARKER.enabled) {
            marker = KillAura.current();
            if (marker == null) marker = AimAssist.current();
            if (marker == null) marker = HudRenderer.target();
            if (marker != null && !marker.isAlive()) marker = null;
        }
        boolean hat = Modules.CHINA_HAT.enabled && !mc.options.getPerspective().isFirstPerson();
        boolean trailOn = Modules.TRAIL.enabled && trail.size() > 1;
        if (particles.isEmpty() && rings.isEmpty() && beams.isEmpty() && marker == null && !hat && !trailOn) return;

        float pt = ctx.tickCounter().getTickDelta(false);
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        camX = cp.x;
        camY = cp.y;
        camZ = cp.z;
        Matrix4f m = ms.peek().getPositionMatrix();
        Vector3f right = new Vector3f(1, 0, 0).rotate(cam.getRotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(cam.getRotation());

        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        long now = System.currentTimeMillis();

        // particles (glow dots / stars)
        for (P p : particles) {
            double x = p.px + (p.x - p.px) * pt, y = p.py + (p.y - p.py) * pt, z = p.pz + (p.z - p.pz) * pt;
            float f = Math.max(0f, (p.life - pt) / p.max);
            int a = (int) (230 * Math.pow(f, 0.7));
            if (a < 4) continue;
            double s = p.size * (0.55 + 0.45 * f);
            if (p.shape == 1) {
                star(b, m, x, y, z, s * 3.4, Colors.alpha(p.color, a), right, up);
                glow(b, m, x, y, z, s * 1.5, Colors.alpha(p.color, a / 3), right, up);
            } else {
                glow(b, m, x, y, z, s * 2.8, Colors.alpha(p.color, a), right, up);
                glow(b, m, x, y, z, s * 0.9, Colors.alpha(0xFFFFFF, a), right, up);
            }
        }

        // flat rings (jump circle / kill shockwave)
        for (Ring r : rings) {
            if (r.age < 0) continue;
            float t = Math.min(1f, (r.age + pt) / r.max);
            float e = 1f - (1f - t) * (1f - t) * (1f - t);
            double rad = r.r0 + (r.r1 - r.r0) * e;
            double w = r.width * (1.0 - t * 0.6);
            int a = (int) (220 * Math.pow(1.0 - t, 1.4));
            if (a < 4) continue;
            int segs = 56;
            for (int i = 0; i < segs; i++) {
                double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
                int col = Colors.alpha(Colors.accent(r.off + i * 6), a);
                ringBand(b, m, r.x, r.y, r.z, rad - w * 2.2, rad, a0, a1, Colors.alpha(col, 0), col);
                ringBand(b, m, r.x, r.y, r.z, rad, rad + w * 0.6, a0, a1, col, Colors.alpha(col, 0));
            }
        }

        // kill beams
        for (Beam bm : beams) {
            float t = Math.min(1f, (bm.age + pt) / bm.max);
            double h = 3.6 * Math.min(1f, t * 4f);
            double rad = 0.32 * (1.0 - t);
            int a = (int) (200 * (1.0 - t));
            if (a < 4) continue;
            cylinder(b, m, bm.x, bm.y, bm.z, rad, h, Colors.alpha(Colors.accent(bm.off), a));
            cylinder(b, m, bm.x, bm.y, bm.z, rad * 0.35, h, Colors.alpha(0xFFFFFF, a));
        }

        // trail ribbons
        if (trailOn) {
            List<Vec3d> pts = new ArrayList<>(trail.size() + 1);
            pts.add(pl.getLerpedPos(pt));
            pts.addAll(trail);
            ribbon(b, m, pts, 0.12, 0.28, 0);
            ribbon(b, m, pts, pl.getHeight() * 0.55, 0.22, 90);
        }

        // china hat
        if (hat) {
            Vec3d pos = pl.getLerpedPos(pt);
            double baseY = pos.y + pl.getHeight() + 0.02;
            double apexY = baseY + 0.42, rad = 0.62;
            int n = 40;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                int col = Colors.accent(i * 9 + (int) (now / 12));
                tri(b, m, pos.x, apexY, pos.z, Colors.alpha(col, 0x70),
                        pos.x + Math.cos(a0) * rad, baseY, pos.z + Math.sin(a0) * rad, Colors.alpha(col, 0xB0),
                        pos.x + Math.cos(a1) * rad, baseY, pos.z + Math.sin(a1) * rad, Colors.alpha(col, 0xB0));
                ringBand(b, m, pos.x, baseY, pos.z, rad - 0.07, rad, a0, a1, Colors.alpha(col, 0), Colors.alpha(col, 0xF0));
                ringBand(b, m, pos.x, baseY, pos.z, rad, rad + 0.08, a0, a1, Colors.alpha(col, 0xF0), Colors.alpha(col, 0));
            }
        }

        // target marker
        if (marker != null) {
            Vec3d tp = marker.getLerpedPos(pt);
            double rad = marker.getWidth() * 0.7 + 0.4;
            double yy = tp.y + marker.getHeight() * (0.5 + 0.5 * Math.sin(now / 450.0));
            double rot = now / 500.0;
            int n = 72;
            for (int i = 0; i < n; i++) {
                if (i % 12 >= 8) continue;
                double a0 = rot + Math.PI * 2 * i / n, a1 = rot + Math.PI * 2 * (i + 1) / n;
                int col = Colors.alpha(Colors.accent(i * 5), 0xD0);
                ringBand(b, m, tp.x, yy, tp.z, rad - 0.12, rad, a0, a1, Colors.alpha(col, 0), col);
                ringBand(b, m, tp.x, yy, tp.z, rad, rad + 0.04, a0, a1, col, Colors.alpha(col, 0));
            }
            for (int i = 0; i < 48; i++) {
                double a0 = Math.PI * 2 * i / 48, a1 = Math.PI * 2 * (i + 1) / 48;
                int col = Colors.alpha(Colors.accent(i * 7), 0x40);
                ringBand(b, m, tp.x, tp.y + 0.03, tp.z, rad - 0.05, rad, a0, a1, Colors.alpha(col, 0), col);
            }
        }

        BuiltBuffer built = b.endNullable();
        if (built == null) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- primitives
    private static void vtx(BufferBuilder b, Matrix4f m, double x, double y, double z, int c) {
        b.vertex(m, (float) (x - camX), (float) (y - camY), (float) (z - camZ)).color(c);
    }

    private static void tri(BufferBuilder b, Matrix4f m,
                            double x1, double y1, double z1, int c1,
                            double x2, double y2, double z2, int c2,
                            double x3, double y3, double z3, int c3) {
        vtx(b, m, x1, y1, z1, c1);
        vtx(b, m, x2, y2, z2, c2);
        vtx(b, m, x3, y3, z3, c3);
        vtx(b, m, x3, y3, z3, c3);
    }

    private static void quad(BufferBuilder b, Matrix4f m,
                             double x1, double y1, double z1, int c1,
                             double x2, double y2, double z2, int c2,
                             double x3, double y3, double z3, int c3,
                             double x4, double y4, double z4, int c4) {
        vtx(b, m, x1, y1, z1, c1);
        vtx(b, m, x2, y2, z2, c2);
        vtx(b, m, x3, y3, z3, c3);
        vtx(b, m, x4, y4, z4, c4);
    }

    /** Soft radial glow facing the camera (bright centre, transparent rim). */
    private static void glow(BufferBuilder b, Matrix4f m, double cx, double cy, double cz, double s,
                             int color, Vector3f r, Vector3f u) {
        int rim = Colors.alpha(color, 0);
        int seg = 8;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(a0) * s, s0 = Math.sin(a0) * s, c1 = Math.cos(a1) * s, s1 = Math.sin(a1) * s;
            tri(b, m, cx, cy, cz, color,
                    cx + r.x * c0 + u.x * s0, cy + r.y * c0 + u.y * s0, cz + r.z * c0 + u.z * s0, rim,
                    cx + r.x * c1 + u.x * s1, cy + r.y * c1 + u.y * s1, cz + r.z * c1 + u.z * s1, rim);
        }
    }

    /** Four-pointed shining star. */
    private static void star(BufferBuilder b, Matrix4f m, double cx, double cy, double cz, double s,
                             int color, Vector3f r, Vector3f u) {
        int rim = Colors.alpha(color, 0);
        double t = s * 0.16;
        quad(b, m,
                cx + r.x * s, cy + r.y * s, cz + r.z * s, rim,
                cx + u.x * t, cy + u.y * t, cz + u.z * t, color,
                cx - r.x * s, cy - r.y * s, cz - r.z * s, rim,
                cx - u.x * t, cy - u.y * t, cz - u.z * t, color);
        quad(b, m,
                cx + u.x * s, cy + u.y * s, cz + u.z * s, rim,
                cx + r.x * t, cy + r.y * t, cz + r.z * t, color,
                cx - u.x * s, cy - u.y * s, cz - u.z * s, rim,
                cx - r.x * t, cy - r.y * t, cz - r.z * t, color);
    }

    /** Flat ring segment on the XZ plane between two radii and two angles. */
    private static void ringBand(BufferBuilder b, Matrix4f m, double cx, double cy, double cz,
                                 double rIn, double rOut, double a0, double a1, int cIn, int cOut) {
        double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
        quad(b, m,
                cx + c0 * rIn, cy, cz + s0 * rIn, cIn,
                cx + c0 * rOut, cy, cz + s0 * rOut, cOut,
                cx + c1 * rOut, cy, cz + s1 * rOut, cOut,
                cx + c1 * rIn, cy, cz + s1 * rIn, cIn);
    }

    /** Vertical light pillar: bright at the bottom, fading to nothing at the top. */
    private static void cylinder(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                 double rad, double h, int color) {
        int top = Colors.alpha(color, 0);
        int sides = 10;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2 * i / sides, a1 = Math.PI * 2 * (i + 1) / sides;
            quad(b, m,
                    x + Math.cos(a0) * rad, y, z + Math.sin(a0) * rad, color,
                    x + Math.cos(a1) * rad, y, z + Math.sin(a1) * rad, color,
                    x + Math.cos(a1) * rad, y + h, z + Math.sin(a1) * rad, top,
                    x + Math.cos(a0) * rad, y + h, z + Math.sin(a0) * rad, top);
        }
    }

    /** Camera-facing glowing ribbon through the given points (newest first), tapering and fading. */
    private static void ribbon(BufferBuilder b, Matrix4f m, List<Vec3d> pts, double yOff, double width, int colorOff) {
        int n = pts.size();
        Vec3d cam = new Vec3d(camX, camY, camZ);
        for (int i = 0; i < n - 1; i++) {
            Vec3d a = pts.get(i).add(0, yOff, 0), c = pts.get(i + 1).add(0, yOff, 0);
            Vec3d dir = c.subtract(a);
            if (dir.lengthSquared() < 1e-8) continue;
            Vec3d mid = a.add(c).multiply(0.5);
            Vec3d side = dir.crossProduct(cam.subtract(mid));
            if (side.lengthSquared() < 1e-10) continue;
            side = side.normalize();
            double f0 = 1.0 - (double) i / n, f1 = 1.0 - (double) (i + 1) / n;
            Vec3d s0 = side.multiply(width * f0), s1 = side.multiply(width * f1);
            int col0 = Colors.alpha(Colors.accent(colorOff + i * 8), (int) (200 * Math.pow(f0, 1.2)));
            int col1 = Colors.alpha(Colors.accent(colorOff + (i + 1) * 8), (int) (200 * Math.pow(f1, 1.2)));
            int r0 = Colors.alpha(col0, 0), r1 = Colors.alpha(col1, 0);
            quad(b, m,
                    a.x - s0.x, a.y - s0.y, a.z - s0.z, r0,
                    a.x, a.y, a.z, col0,
                    c.x, c.y, c.z, col1,
                    c.x - s1.x, c.y - s1.y, c.z - s1.z, r1);
            quad(b, m,
                    a.x, a.y, a.z, col0,
                    a.x + s0.x, a.y + s0.y, a.z + s0.z, r0,
                    c.x + s1.x, c.y + s1.y, c.z + s1.z, r1,
                    c.x, c.y, c.z, col1);
        }
    }
}
