package com.vodx.visuals;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/** Gameplay hooks for the effects. All drawing is done by {@link Fx} (custom glow renderer, no vanilla particles). */
public final class Effects {
    private static LivingEntity target;
    private static boolean killed;
    private static int sinceHit;
    private static boolean wasGround = true;

    private Effects() { }

    public static void init() {
        Fx.init();
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity le) onAttack(le);
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(Effects::tick);
    }

    private static void onAttack(LivingEntity e) {
        HudRenderer.onHit(e);
        target = e;
        killed = false;
        sinceHit = 0;
        if (Modules.HIT_PARTICLES.enabled) Fx.spawnHit(e);
        if (Modules.HIT_SOUND.enabled) playHitSound();
    }

    private static void playHitSound() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        SoundEvent s = switch (Modules.HIT_SOUND_TYPE.index) {
            case 1 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case 2 -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        p.playSound(s, 0.7f, 1.2f);
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        if (target != null) {
            sinceHit++;
            if (!killed && Modules.KILL_EFFECT.enabled && (target.isDead() || target.getHealth() <= 0f)) {
                Fx.spawnKill(target);
                killed = true;
            }
            if (killed || sinceHit > 100) target = null;
        }

        boolean ground = p.isOnGround();
        if (Modules.JUMP_CIRCLE.enabled && wasGround && !ground && p.getVelocity().y > 0.1) {
            Fx.spawnJump(p.getX(), p.getY(), p.getZ());
        }
        wasGround = ground;
    }
}
