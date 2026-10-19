package com.jjkcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

import com.jjkcraft.sorcerer.PlayerActions;

/** The server runs this when it sees a player jump (vows against jumping, the Echo Mimic). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"))
	private void jjkcraft$onJump(CallbackInfo ci) {
		PlayerActions.jumped((ServerPlayer) (Object) this);
	}
}
