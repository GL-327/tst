package com.jjkcraft.energy;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.energy.EnergyState;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.EnergySyncPayload;
import com.jjkcraft.network.payload.SettingsPayload;

/**
 * Sends a player's CE to their client only when it changes. Mutations mark the player dirty and
 * are flushed once at the end of the server tick, so several changes in one tick cost one packet.
 * Once a second the server also checks whether max CE or the regen modifier moved on its own (a
 * grade change, a domain), and resyncs only then.
 */
public final class EnergySync {
	private static final Set<UUID> DIRTY = new LinkedHashSet<>();
	private static final Map<UUID, Sent> LAST_SENT = new HashMap<>();
	private static final int DRIFT_CHECK_INTERVAL = 20;

	private record Sent(double max, double regenModifier) {
	}

	private EnergySync() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(EnergySync::endTick);
	}

	public static synchronized void markDirty(ServerPlayer player) {
		DIRTY.add(player.getUUID());
	}

	/** Sends immediately (join, respawn, dimension change). */
	public static void sendNow(ServerPlayer player) {
		double max = CursedEnergy.max(player);
		double modifier = CursedEnergy.regenModifier(player);
		EnergyState state = CursedEnergy.state(player);
		JjkNetworking.send(player, new EnergySyncPayload(state, max, modifier));
		synchronized (EnergySync.class) {
			LAST_SENT.put(player.getUUID(), new Sent(max, modifier));
			DIRTY.remove(player.getUUID());
		}
	}

	public static void sendSettings(ServerPlayer player) {
		JjkNetworking.send(player, new SettingsPayload(JjkConfig.energyParams(),
				JjkConfig.server().systems.domains, JjkConfig.server().systems.cullingGame));
	}

	public static synchronized void forget(ServerPlayer player) {
		DIRTY.remove(player.getUUID());
		LAST_SENT.remove(player.getUUID());
	}

	private static void endTick(MinecraftServer server) {
		Set<UUID> toSend;
		synchronized (EnergySync.class) {
			toSend = new LinkedHashSet<>(DIRTY);
			DIRTY.clear();
		}

		boolean driftCheck = server.getTickCount() % DRIFT_CHECK_INTERVAL == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (toSend.contains(player.getUUID())) {
				sendNow(player);
			} else if (driftCheck) {
				Sent sent;
				synchronized (EnergySync.class) {
					sent = LAST_SENT.get(player.getUUID());
				}

				double max = CursedEnergy.max(player);
				double modifier = CursedEnergy.regenModifier(player);
				if (sent == null || Math.abs(sent.max() - max) > 1e-6 || Math.abs(sent.regenModifier() - modifier) > 1e-6) {
					// Store regen so far. Callers that change max or regen on purpose checkpoint first;
					// for anything else the error is at most one check interval at the new rate.
					CursedEnergy.checkpoint(player);
					sendNow(player);
				}
			}
		}
	}
}
