package com.jjkcraft.sorcerer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.jjkcraft.technique.TechniqueService;

/**
 * Server-side notices of what players do: jump, swing at something, cast. Vows listen for jumps;
 * the Echo Mimic records all three to play them back.
 */
public final class PlayerActions {
	public enum Kind {
		JUMP, MELEE, CAST
	}

	@FunctionalInterface
	public interface Listener {
		/** {@code technique} is set for casts only. */
		void onAction(ServerPlayer player, Kind kind, Identifier technique, Entity target);
	}

	private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

	private PlayerActions() {
	}

	public static void addListener(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!level.isClientSide() && player instanceof ServerPlayer server) {
				fire(server, Kind.MELEE, null, entity);
			}

			return InteractionResult.PASS;
		});
		TechniqueService.addCastListener((caster, id, definition) -> {
			if (caster instanceof ServerPlayer player) {
				fire(player, Kind.CAST, id, null);
			}
		});
	}

	/** Called from the server player's jump. */
	public static void jumped(ServerPlayer player) {
		fire(player, Kind.JUMP, null, null);
	}

	private static void fire(ServerPlayer player, Kind kind, Identifier technique, Entity target) {
		for (Listener listener : LISTENERS) {
			listener.onAction(player, kind, technique, target);
		}
	}
}
