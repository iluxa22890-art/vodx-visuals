package com.vodx.visuals;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Smooth aim assist: every tick it picks the target closest to your crosshair
 * (inside range + FOV) and rotates the camera towards it by a limited step.
 * NOTE: on most multiplayer servers this counts as cheating. Use it in singleplayer / on your own server.
 */
public final class AimAssist {
    private static LivingEntity current;

    private AimAssist() { }

    public static LivingEntity current() {
        return current;
    }

    public static void tick(MinecraftClient mc) {
        if (!Modules.AIM_ASSIST.enabled) { current = null; return; }
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) { current = null; return; }
        if (Modules.AIM_HOLD.value && !mc.options.attackKey.isPressed()) { current = null; return; }

        LivingEntity t = findTarget(mc, p);
        current = t;
        if (t == null) return;

        Vec3d point = aimPoint(t);
        float[] rot = rotationTo(p, point);
        float rnd = Modules.AIM_RANDOM.f();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        float targetYaw = rot[0] + (float) ((r.nextDouble() - 0.5) * 2.0 * rnd);
        float targetPitch = rot[1] + (float) ((r.nextDouble() - 0.5) * 2.0 * rnd);

        float dYaw = MathHelper.wrapDegrees(targetYaw - p.getYaw());
        float dPitch = targetPitch - p.getPitch();
        float speed = Modules.AIM_SPEED.f();
        float max = Modules.AIM_MAXSTEP.f();

        p.setYaw(p.getYaw() + MathHelper.clamp(dYaw * speed, -max, max));
        p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch * speed, -max, max), -90f, 90f));
    }

    private static LivingEntity findTarget(MinecraftClient mc, ClientPlayerEntity p) {
        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;
        double range = Modules.AIM_RANGE.value;
        double halfFov = Modules.AIM_FOV.value / 2.0;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isSpectator()) continue;
            boolean ok = (Modules.AIM_PLAYERS.value && e instanceof PlayerEntity)
                    || (Modules.AIM_MOBS.value && e instanceof MobEntity);
            if (!ok) continue;
            if (p.distanceTo(le) > range) continue;
            if (Modules.AIM_VISIBLE.value && !p.canSee(le)) continue;

            float[] rot = rotationTo(p, aimPoint(le));
            double dy = MathHelper.wrapDegrees(rot[0] - p.getYaw());
            double dp = rot[1] - p.getPitch();
            double angle = Math.hypot(dy, dp);
            if (angle > halfFov) continue;
            if (angle < bestAngle) { bestAngle = angle; best = le; }
        }
        return best;
    }

    private static Vec3d aimPoint(LivingEntity e) {
        double y;
        switch (Modules.AIM_POINT.index) {
            case 0 -> y = e.getEyeY();
            case 2 -> y = e.getY() + e.getHeight() * 0.5;
            default -> y = e.getY() + e.getHeight() * 0.7;
        }
        return new Vec3d(e.getX(), y, e.getZ());
    }

    /** Returns {yaw, pitch} needed to look from the player's eyes at the given point. */
    public static float[] rotationTo(ClientPlayerEntity p, Vec3d point) {
        Vec3d eye = p.getEyePos();
        double dx = point.x - eye.x, dy = point.y - eye.y, dz = point.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));
        return new float[] { yaw, pitch };
    }

    /** Smoothly turns the player's camera towards a point (shared with Kill Aura). */
    public static void turnTo(ClientPlayerEntity p, Vec3d point, float speed, float max, float rnd) {
        float[] rot = rotationTo(p, point);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        float ty = rot[0] + (float) ((r.nextDouble() - 0.5) * 2.0 * rnd);
        float tp = rot[1] + (float) ((r.nextDouble() - 0.5) * 2.0 * rnd);
        float dYaw = MathHelper.wrapDegrees(ty - p.getYaw());
        float dPitch = tp - p.getPitch();
        p.setYaw(p.getYaw() + MathHelper.clamp(dYaw * speed, -max, max));
        p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch * speed, -max, max), -90f, 90f));
    }
}
