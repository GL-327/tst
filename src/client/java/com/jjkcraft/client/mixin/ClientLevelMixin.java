package com.jjkcraft.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import com.jjkcraft.client.vfx.ClientFx;

/**
 * Hit-stop: entities frozen by {@link ClientFx#hitStop} skip their client-side tick for a few
 * ticks, so their animation and movement pause. Server state is untouched; queued position
 * updates are applied when the freeze ends.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void jjkcraft$hitStop(Entity entity, CallbackInfo ci) {
		if (ClientFx.isFrozen(entity.getId())) {
			// Pin the render interpolation to the current position so the entity holds still.
			entity.setOldPosAndRot();
			ci.cancel();
		}
	}
}
