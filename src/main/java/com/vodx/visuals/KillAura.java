package com.vodx.visuals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Kill Aura. Modes:
 *  Legit   - smooth turn, respects attack cooldown / CPS.
 *  Blatant - instant turn, attacks every tick.
 *  Multi   - blatant + hits several targets per tick.
 * NOTE: on multiplayer servers this is cheating (and anti-cheats flag it instantly). Use it in singleplayer / on your own server.
 */
public final class KillAura {
    private record Cand(LivingEntity e, double score) { }

    private static int delay;
    private static LivingEntity current;

    private KillAura() { }

    public static LivingEntity current() {
        return current;
    }

    public static void tick(MinecraftClient mc) {
        if (!Modules.KILL_AURA.enabled) { current = null; return; }
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null
                || mc.currentScreen != null || !p.isAlive()) {
            current = null;
            return;
        }

        List<LivingEntity> targets = pick(mc, p);
        current = targets.isEmpty() ? null : targets.get(0);
        if (current == null) return;

        int mode = Modules.KA_MODE.index;
        boolean blatant = mode >= 1;

        if (Modules.KA_ROTATE.value) {
            Vec3d point = new Vec3d(current.getX(), current.getY() + current.getHeight() * 0.7, current.getZ());
            if (blatant) AimAssist.turnTo(p, point, 1f, 180f, 0f);
            else AimAssist.turnTo(p, point, Modules.KA_TURN.f(), 40f, 0.3f);
        }

        if (!blatant) {
            if (Modules.KA_COOLDOWN.value) {
                if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
            } else {
                if (delay > 0) { delay--; return; }
                int cps = Math.max(1, Modules.KA_CPS.i());
                delay = Math.max(0, Math.round(20f / cps) - 1 + ThreadLocalRandom.current().nextInt(0, 2));
            }
        }

        int limit = mode == 2 ? Modules.KA_MAXTARGETS.i() : 1;
        for (int i = 0; i < Math.min(limit, targets.size()); i++) {
            mc.interactionManager.attackEntity(p, targets.get(i));
        }
        p.swingHand(Hand.MAIN_HAND);
    }

    private static List<LivingEntity> pick(MinecraftClient mc, ClientPlayerEntity p) {
        List<Cand> list = new ArrayList<>();
        double range = Modules.KA_RANGE.value;
        double halfFov = Modules.KA_FOV.value / 2.0;
        Vec3d eye = p.getEyePos();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isSpectator()) continue;
            boolean ok = (Modules.KA_PLAYERS.value && e instanceof PlayerEntity)
                    || (Modules.KA_MOBS.value && e instanceof MobEntity);
            if (!ok) continue;

            Box box = le.getBoundingBox();
            Vec3d closest = new Vec3d(
                    MathHelper.clamp(eye.x, box.minX, box.maxX),
                    MathHelper.clamp(eye.y, box.minY, box.maxY),
                    MathHelper.clamp(eye.z, box.minZ, box.maxZ));
            double dist = eye.distanceTo(closest);
            if (dist > range) continue;
            if (Modules.KA_VISIBLE.value && !p.canSee(le)) continue;

            float[] rot = AimAssist.rotationTo(p, new Vec3d(le.getX(), le.getY() + le.getHeight() * 0.7, le.getZ()));
            double angle = Math.hypot(MathHelper.wrapDegrees(rot[0] - p.getYaw()), rot[1] - p.getPitch());
            if (angle > halfFov) continue;

            double score = switch (Modules.KA_PRIORITY.index) {
                case 1 -> le.getHealth();
                case 2 -> angle;
                default -> dist;
            };
            list.add(new Cand(le, score));
        }
        list.sort(Comparator.comparingDouble(Cand::score));
        List<LivingEntity> out = new ArrayList<>();
        for (Cand c : list) out.add(c.e());
        return out;
    }
}
