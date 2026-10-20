package com.jjkcraft.sense;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.SensePayload;
import com.jjkcraft.network.payload.SenseTogglePayload;
import com.jjkcraft.sorcerer.HeavenlyRestriction;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;

/**
 * Cursed-energy sensing. An awakened sorcerer can toggle it on; while it is on, the server sends
 * the presences within the grade's {@code sense_range} perk twice a second and drains a little CE.
 * Heavenly Restriction users have no CE: they can't sense and can't be sensed. Disguised curses
 * still show up, which is how sorcerers see through the Redacted Saint.
 */
public final class Sensing {
	/** CE per second while sensing is on. */
	public static final double UPKEEP_PER_SECOND = 0.5;
	public static final int MAX_SENSED = 64;
	private static final int INTERVAL = 10;

	private static final Map<UUID, Long> ACTIVE = new HashMap<>();

	private Sensing() {
	}

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(SenseTogglePayload.TYPE, (payload, context) -> toggle(context.player(), payload.on()));
		ServerTickEvents.END_SERVER_TICK.register(Sensing::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE.remove(handler.getPlayer().getUUID()));
	}

	public static boolean isSensing(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static boolean canSense(ServerPlayer player) {
		SorcererProgress progress = Progression.get(player);
		return progress.awakened() && !progress.heavenlyRestricted() && !player.isSpectator();
	}

	public static void toggle(ServerPlayer player, boolean on) {
		long now = player.level().getGameTime();
		Long since = ACTIVE.get(player.getUUID());
		// Ignore toggle spam: at most one change every 5 ticks.
		if (since != null && now - since < 5 && on) {
			return;
		}

		if (on && canSense(player)) {
			ACTIVE.put(player.getUUID(), now);
			send(player);
		} else {
			ACTIVE.remove(player.getUUID());
			JjkNetworking.send(player, SensePayload.off());
		}
	}

	public static double range(ServerPlayer player) {
		return (com.jjkcraft.awakening.CharacterTraits.has(player,"six_eyes")?1.5:1.0) * Grades.get(player.registryAccess(), Progression.get(player).grade())
				.map(g -> g.perks().senseRange())
				.orElse(GradeDefinition.Perks.NONE.senseRange());
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0 || ACTIVE.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ACTIVE.containsKey(player.getUUID())) {
				continue;
			}

			if (!canSense(player) || !CursedEnergy.trySpend(player, UPKEEP_PER_SECOND * INTERVAL / 20.0)) {
				ACTIVE.remove(player.getUUID());
				JjkNetworking.send(player, SensePayload.off());
				continue;
			}

			send(player);
		}
	}

	private static void send(ServerPlayer player) {
		double range = range(player);
		JjkNetworking.send(player, new SensePayload(true, (float) range, sense(player, range)));
	}

	/** Every presence the player can sense, nearest first. */
	public static List<SensePayload.Sensed> sense(ServerPlayer player, double range) {
		List<LivingEntity> found = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
				e -> e != player && e.isAlive() && senseable(e) && e.distanceToSqr(player) <= range * range);
		found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

		List<SensePayload.Sensed> out = new ArrayList<>();
		for (LivingEntity entity : found) {
			if (out.size() >= MAX_SENSED) {
				break;
			}

			var grade = Progression.gradeOf(entity).flatMap(id -> Grades.get(player.registryAccess(), id));
			int color = grade.map(GradeDefinition::color).orElse(0x8F5CFF);
			int rank = grade.map(GradeDefinition::rank).orElse(0);
			out.add(new SensePayload.Sensed(entity.getId(), color, rank, entity instanceof CurseEntity));
		}

		return out;
	}

	public static boolean senseable(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			return !player.isSpectator() && Progression.get(player).awakened() && !HeavenlyRestriction.has(player);
		}

		return entity instanceof CurseEntity || CursedEnergy.isCapable(entity);
	}
}
