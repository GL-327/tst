package com.jjkcraft.technique;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/** Sends technique visuals to the caster and every player tracking them. */
public final class TechniqueFx {
	private TechniqueFx() {
	}

	public static void send(CastContext ctx, TechniqueFxPayload.Phase phase, Vec3 origin, Vec3 direction, List<? extends Entity> targets) {
		List<Integer> ids = targets.stream().limit(64).map(Entity::getId).toList();
		TechniqueFxPayload payload = new TechniqueFxPayload(ctx.caster().getId(), ctx.techniqueId(), phase, origin, direction, ids, (float) ctx.power());
		for (ServerPlayer viewer : PlayerLookup.tracking(ctx.caster())) {
			JjkNetworking.send(viewer, payload);
		}

		if (ctx.caster() instanceof ServerPlayer self) {
			JjkNetworking.send(self, payload);
		}
	}
}
