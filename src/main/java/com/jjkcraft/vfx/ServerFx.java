package com.jjkcraft.vfx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side effect helpers that respect the performance budget. */
public final class ServerFx {
	private static final double VIEW_RANGE_SQR = 64 * 64;

	private ServerFx() {
	}

	/**
	 * Sends particles to every nearby player except {@code except}. Used for effects centered on a
	 * player, whose own view would be filled by particles spawning at the camera; that player sees
	 * a client-side overlay instead.
	 */
	public static void particlesForOthers(ServerLevel level, ServerPlayer except, ParticleOptions particle,
			double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		for (ServerPlayer viewer : level.players()) {
			if (viewer != except && viewer.distanceToSqr(x, y, z) < VIEW_RANGE_SQR) {
				level.sendParticles(viewer, particle, false, false, x, y, z, count, dx, dy, dz, speed);
			}
		}
	}
}
