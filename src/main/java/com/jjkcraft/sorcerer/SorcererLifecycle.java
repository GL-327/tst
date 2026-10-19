package com.jjkcraft.sorcerer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.config.DeathPolicy;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.config.JjkServerConfig;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.energy.EnergySync;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;

/**
 * Keeps sorcerer data across join, death, respawn and dimension change, and resyncs the client
 * after each. Death copies data with the configured {@link DeathPolicy}; other respawns (leaving
 * the End) are copied by Fabric because the player is alive.
 */
public final class SorcererLifecycle {
	/** Copies one more piece of player data on death. Later systems (techniques, vows) add one. */
	@FunctionalInterface
	public interface DeathCopier {
		void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer, DeathPolicy policy, JjkServerConfig.Death config);
	}

	/** Sends one more piece of client state on join or respawn. Later systems add one. */
	@FunctionalInterface
	public interface FullSync {
		void send(ServerPlayer player);
	}

	private static final List<DeathCopier> DEATH_COPIERS = new ArrayList<>();
	private static final List<FullSync> FULL_SYNCS = new ArrayList<>();

	private SorcererLifecycle() {
	}

	public static void addDeathCopier(DeathCopier copier) {
		DEATH_COPIERS.add(copier);
	}

	public static void addFullSync(FullSync sync) {
		FULL_SYNCS.add(sync);
	}

	public static void init() {
		addDeathCopier(SorcererLifecycle::copyProgress);

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				copyOnDeath(oldPlayer, newPlayer);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> fullSync(newPlayer));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> fullSync(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EnergySync.forget(handler.getPlayer()));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> fullSync(player));
	}

	/** Applies the death policy to every registered piece of data, then sets the respawn CE. */
	public static void copyOnDeath(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
		JjkServerConfig.Death config = JjkConfig.server().death;
		for (DeathCopier copier : DEATH_COPIERS) {
			copier.copy(oldPlayer, newPlayer, config.policy, config);
		}

		double max = CursedEnergy.max(newPlayer);
		CursedEnergy.set(newPlayer, max * config.respawnEnergyPercent / 100.0);
	}

	public static void fullSync(ServerPlayer player) {
		EnergySync.sendSettings(player);
		EnergySync.sendNow(player);
		for (FullSync sync : FULL_SYNCS) {
			sync.send(player);
		}
	}

	private static void copyProgress(ServerPlayer oldPlayer, ServerPlayer newPlayer, DeathPolicy policy, JjkServerConfig.Death config) {
		SorcererProgress progress = oldPlayer.getAttachedOrCreate(JjkAttachments.PROGRESS);
		int floor = Grades.get(oldPlayer.registryAccess(), progress.grade()).map(GradeDefinition::xpRequired).orElse(0);
		newPlayer.setAttached(JjkAttachments.PROGRESS, applyPolicy(progress, policy, config, floor));
	}

	/**
	 * Pure policy step for progress, unit-tested in {@code DeathPolicyTest}. PARTIAL only loses
	 * experience earned inside the current grade ({@code gradeFloorXp} is that grade's
	 * requirement), so death never demotes.
	 */
	public static SorcererProgress applyPolicy(SorcererProgress progress, DeathPolicy policy, JjkServerConfig.Death config, int gradeFloorXp) {
		return switch (policy) {
			case KEEP, RESET_TECHNIQUES -> progress;
			case PARTIAL -> {
				int above = Math.max(0, progress.gradeXp() - gradeFloorXp);
				int kept = (int) Math.floor(above * (1.0 - config.partialXpLossPercent / 100.0));
				yield progress.withXp(Math.min(progress.gradeXp(), gradeFloorXp + kept));
			}
		};
	}
}
