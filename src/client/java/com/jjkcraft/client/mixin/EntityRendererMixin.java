package com.jjkcraft.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import com.jjkcraft.client.state.ClientSensing;

/** Sensed presences glow in their grade color, the same way vanilla draws glowing entities. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void jjkcraft$senseOutline(T entity, S state, float partialTicks, CallbackInfo ci) {
		com.jjkcraft.client.combat.CombatAnimations.identify(state,entity.getId());
        if(com.jjkcraft.client.vessel.VesselClient.hallucinated(entity.getId())){state.outlineColor=0xFFCF294A;state.nameTag=net.minecraft.network.chat.Component.literal("CURSE · DO NOT TRUST IT").withColor(0xCF294A);state.nameTagAttachment=new net.minecraft.world.phys.Vec3(0,entity.getBbHeight()+.35,0);}
        if (state.outlineColor == 0) {
			int color = ClientSensing.outlineColor(entity);
			if (color != 0) {
				state.outlineColor = color;
			}
		}
	}
}
