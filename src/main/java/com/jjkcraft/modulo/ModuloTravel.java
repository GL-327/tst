package com.jjkcraft.modulo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.modulo.data.SiteDefinition;
import com.jjkcraft.modulo.world.SiteBuilder;

/** Moving players into, around and out of the Modulo expansion (docs/MODULO.md 3.3). */
public final class ModuloTravel {
	/** The Naunax's arrival hall, where boarding and the beacon bring you. */
	public static final Identifier SHIP_SITE = Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, "naunax_deck");
	/** Kyoto Jujutsu High's yard, where the ship's gate lets you down in Tokyo 2086. */
	public static final Identifier TOKYO_SITE = Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, "kyoto_jujutsu_high");
	private static final int GATE_COOLDOWN_TICKS = 60;
	private static final Map<UUID, Long> LAST_GATE = new HashMap<>();

	private ModuloTravel() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_GATE.remove(handler.getPlayer().getUUID()));
	}

	/** Boards the Naunax from wherever the player stands; leaving later brings them back here. */
	public static boolean board(ServerPlayer player) {
		if (!Modulo.storyEnabled()) {
			return false;
		}
		if(com.jjkcraft.culling.CullingGame.state(player.level().getServer()).active(player.getUUID())!=null) {
			player.sendSystemMessage(Component.literal("An active Culling participant needs a Vessel's expedition release agreement."),true);return false;
		}

		GlobalPos from = GlobalPos.of(player.level().dimension(), player.blockPosition());
		if (!Modulo.isModuloLevel(player.level())) {
			Modulo.setProgress(player, Modulo.progress(player).withBoarded(from));
		}

		return toSite(player, SHIP_SITE, "arrival");
	}

	/** Back to the ship's arrival hall (the beacon, or Maru's Harmony). Only after a first boarding. */
	public static boolean toShip(ServerPlayer player) {
		if (!Modulo.storyEnabled() || !Modulo.progress(player).boarded()) {
			return false;
		}

		if (!Modulo.isModuloLevel(player.level())) {
			Modulo.setProgress(player, Modulo.progress(player).withReturnTo(Optional.of(GlobalPos.of(player.level().dimension(), player.blockPosition()))));
		}

		return toSite(player, SHIP_SITE, "arrival");
	}

	public static boolean toTokyo(ServerPlayer player) {
		return Modulo.storyEnabled() && toSite(player, TOKYO_SITE, "arrival");
	}

	/** Out of the expansion: to where the player boarded, or their respawn point. */
	public static void leave(ServerPlayer player) {
		Optional<GlobalPos> back = Modulo.progress(player).returnTo();
		ServerLevel level = back.map(pos -> player.level().getServer().getLevel(pos.dimension())).orElse(null);
		if (level != null) {
			teleport(player, level, back.get().pos());
		} else {
			player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
		}
	}

	/** A player stepped on a gate pad. Gates rest for a few seconds after use so arrival pads don't bounce you back. */
	public static void throughGate(ServerPlayer player, NaunaxGateBlock.Destination destination) {
		if(!Modulo.storyEnabled())return;
		long now = player.level().getServer().overworld().getGameTime();
		Long last = LAST_GATE.get(player.getUUID());
		if (last != null && now - last < GATE_COOLDOWN_TICKS) {
			return;
		}

		LAST_GATE.put(player.getUUID(), now);
		if (!Modulo.enabled()) {
			leave(player);
			return;
		}

		switch (destination) {
			case EARTH -> leave(player);
			case TOKYO -> toTokyo(player);
			case SHIP -> toShip(player);
		}
	}

	/** Rests the gates for a player who just arrived somewhere, so they can step off the arrival pad. */
	public static void restGates(ServerPlayer player) {
		LAST_GATE.put(player.getUUID(), player.level().getServer().overworld().getGameTime());
	}

	public static boolean toSite(ServerPlayer player, Identifier siteId, String marker) {
		if(!Modulo.storyEnabled())return false;
		Optional<SiteDefinition> site = SiteBuilder.ensure(player.level().getServer(), siteId);
		if (site.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.no_site", siteId.toString()));
			return false;
		}

		ServerLevel level = player.level().getServer().getLevel(site.get().dimension());
		if (level == null) {
			return false;
		}

		restGates(player);
		teleport(player, level, site.get().marker(marker));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
		return true;
	}

	public static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
		player.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(pos), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
	}
}
