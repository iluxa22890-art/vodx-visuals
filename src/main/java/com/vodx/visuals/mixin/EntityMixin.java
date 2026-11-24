package com.vodx.visuals.mixin;

import com.vodx.visuals.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    // The crosshair raycast expands every entity box by this margin -> bigger hitboxes for aiming.
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true, require = 0)
    private void vodx$expandHitbox(CallbackInfoReturnable<Float> cir) {
        if (!Modules.HITBOXES.enabled) return;
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity) || self == MinecraftClient.getInstance().player) return;
        boolean player = self instanceof PlayerEntity;
        if (player ? !Modules.HB_PLAYERS.value : !Modules.HB_MOBS.value) return;
        cir.setReturnValue(cir.getReturnValueF() + Modules.HB_EXPAND.f());
    }
}
