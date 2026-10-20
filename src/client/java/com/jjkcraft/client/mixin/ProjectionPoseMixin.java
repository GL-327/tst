package com.jjkcraft.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

/** Fabric has no event for replacing the completed vanilla living-entity pose state. */
@Mixin(LivingEntityRenderer.class)
public abstract class ProjectionPoseMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void jjkcraft$projectionPose(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
        com.jjkcraft.client.vfx.AdvancedFx.quantize(entity,state,partial);
    }
}
