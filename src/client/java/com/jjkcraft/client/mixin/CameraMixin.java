package com.jjkcraft.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;

import com.jjkcraft.client.vfx.ClientFx;

/** Adds the client-only camera shake after the camera is aligned with its entity. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private float xRot;

	@Shadow
	private float yRot;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void jjkcraft$applyShake(float partialTicks, CallbackInfo ci) {
		float[] offsets = ClientFx.shakeOffsets();
		if (offsets != null) {
			setRotation(yRot + offsets[0], xRot + offsets[1]);
		}
	}
}
