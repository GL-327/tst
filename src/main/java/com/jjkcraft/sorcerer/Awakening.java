package com.jjkcraft.sorcerer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.energy.CursedEnergy;

/**
 * Awakening: the moment a player's innate nature is rolled. With the default config it happens on
 * first join. A configurable share of players roll Heavenly Restriction; everyone else rolls an
 * innate technique (through the hook the technique system registers). Awakening also starts the
 * Culling Game entry window when that event is running.
 */
public final class Awakening {
	/** Runs after awakening. The technique system rolls the innate technique here. */
	@FunctionalInterface
	public interface Hook {
		void onAwakened(ServerPlayer player, boolean heavenlyRestricted);
	}

	private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();
	private static final List<Consumer<ServerPlayer>> RESET_HOOKS = new CopyOnWriteArrayList<>();

	private Awakening() {
	}

	public static void addHook(Hook hook) {
		HOOKS.add(hook);
	}

	/** Runs when a player's awakening is reset; systems clear what came from it. */
	public static void addResetHook(Consumer<ServerPlayer> hook) {
		RESET_HOOKS.add(hook);
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (JjkConfig.server().progression.awakenOnFirstJoin && !Progression.get(player).awakened()) {
				com.jjkcraft.awakening.CharacterAwakening.firstJoin(player);
			}
		});
	}

	/**
	 * Awakens the player. Does nothing if they are already awake.
	 *
	 * @param forceHeavenly null to roll, or true/false to force the outcome (commands, items)
	 * @return true if the player was awakened by this call
	 */
	public static boolean awaken(ServerPlayer player, Boolean forceHeavenly) {
		SorcererProgress progress = Progression.get(player);
		if (progress.awakened()) {
			return false;
		}

		boolean heavenly = forceHeavenly != null ? forceHeavenly
				: player.getRandom().nextDouble() * 100 < JjkConfig.server().progression.heavenlyRestrictionChancePercent;
		Progression.set(player, progress.withAwakened(player.level().getGameTime(), heavenly));
		CursedEnergy.fill(player);

		for (Hook hook : HOOKS) {
			hook.onAwakened(player, heavenly);
		}

		player.sendSystemMessage(Component.translatable(heavenly ? "message.jjkcraft.awakened.heavenly" : "message.jjkcraft.awakened"));
		return true;
	}

	/** Clears awakening (commands). Techniques are cleared by their own reset hook. */
	public static void reset(ServerPlayer player) {
		Progression.set(player, SorcererProgress.DEFAULT);
		for (Consumer<ServerPlayer> hook : RESET_HOOKS) {
			hook.accept(player);
		}

		CursedEnergy.fill(player);
	}
}
