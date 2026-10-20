package com.jjkcraft.modulo;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.config.JjkServerConfig;
import com.jjkcraft.core.modulo.ModuloMath;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.sorcerer.Progression;

/**
 * The Naunax Descent (docs/MODULO.md 3.3): every few game days the mothership hangs above the
 * badlands (canon: 30,000 m above Nevada) and lowers a ramp of light. Eligible players who step
 * onto the ramp's foot board it. Nothing is built in the overworld; clients draw the ship.
 */
public final class NaunaxDescent {
	/** How close to the ramp's foot boards the ship. */
	private static final double BOARD_RADIUS = 2.5;

	private NaunaxDescent() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(NaunaxDescent::tick);
	}

	private static JjkServerConfig.Modulo config() {
		return JjkConfig.server().modulo;
	}

	private static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	/** Whether this player may see and board the Descent: the expansion is on and they reached the grade. */
	public static boolean eligible(ServerPlayer player) {
		if (!Modulo.storyEnabled()) {
			return false;
		}

		Identifier min = Identifier.tryParse(config().descentMinGrade);
		int minRank = min == null ? 5 : Grades.rank(player.registryAccess(), min);
		return Progression.gradeRank(player.registryAccess(), player) >= minRank;
	}

	public static boolean active(MinecraftServer server) {
		if(!Modulo.storyEnabled())return false;
		ModuloState state = Modulo.state(server);
		return state.descentPos.isPresent() && ModuloMath.descentActive(state.descentStart, state.descentLength, now(server));
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}

		ModuloState state = Modulo.state(server);
		if (!Modulo.enabled()) {
			if (active(server)) {
				end(server);
			}

			return;
		}

		long now = now(server);
		if (active(server)) {
			BlockPos foot = state.descentPos.get();
			for (ServerPlayer player : List.copyOf(server.overworld().players())) {
				double dx = player.getX() - (foot.getX() + 0.5);
				double dz = player.getZ() - (foot.getZ() + 0.5);
				if (dx * dx + dz * dz <= BOARD_RADIUS * BOARD_RADIUS && Math.abs(player.getY() - foot.getY()) < 4 && eligible(player)
						&& !player.isSpectator()) {
					ModuloTravel.board(player);
				}
			}

			return;
		}

		if (state.descentPos.isPresent()) {
			// It ran out since the last check.
			end(server);
		}

		if (state.nextDescent >= 0 && now < state.nextDescent) {
			return;
		}

		server.overworld().players().stream().filter(NaunaxDescent::eligible).findFirst().ifPresent(player -> start(server, player.blockPosition()));
	}

	/** Starts a Descent above the badlands nearest to {@code near} (or above {@code near} itself without any). */
	public static BlockPos start(MinecraftServer server, BlockPos near) {
		if(!Modulo.storyEnabled())return near;
		ServerLevel overworld = server.overworld();
		BlockPos at = near;
		Pair<BlockPos, Holder<Biome>> badlands = overworld.findClosestBiome3d(biome -> biome.is(BiomeTags.IS_BADLANDS), near,
				Math.max(64, config().descentSearchRadius), 64, 64);
		if (badlands != null) {
			at = badlands.getFirst();
		} else {
			JjkCraft.LOGGER.info("Naunax Descent: no badlands within {} blocks of {}; descending there instead", config().descentSearchRadius, near);
		}

		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
		BlockPos foot = new BlockPos(at.getX(), y, at.getZ());
		return start(server, foot, config().descentMinutes * 1200L);
	}

	/** Starts a Descent with its ramp's foot exactly at {@code foot} (commands and tests). */
	public static BlockPos start(MinecraftServer server, BlockPos foot, long lengthTicks) {
		if(!Modulo.storyEnabled())return foot;
		ModuloState state = Modulo.state(server);
		long now = now(server);
		state.descentStart = now;
		state.descentLength = Math.max(20, lengthTicks);
		state.descentPos = Optional.of(foot.immutable());
		state.nextDescent = ModuloMath.nextDescent(now, now, config().descentIntervalDays);
		state.setDirty();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (eligible(player)) {
				if (player.level().dimension() == Level.OVERWORLD) {
					player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.descent", foot.getX(), foot.getZ()));
					player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 1f, 0.5f);
				}

				Modulo.send(player, new ModuloPayloads.Event("descent", List.of(Integer.toString(foot.getX()), Integer.toString(foot.getZ()))));
			}

			Modulo.markDirty(player);
		}

		JjkCraft.LOGGER.info("Naunax Descent at {} for {} ticks", foot, state.descentLength);
		return foot;
	}

	/** Ends the current Descent; the next one stays scheduled. */
	public static void end(MinecraftServer server) {
		ModuloState state = Modulo.state(server);
		state.descentLength = Math.max(0, now(server) - state.descentStart);
		state.descentPos = Optional.empty();
		state.setDirty();
		server.getPlayerList().getPlayers().forEach(Modulo::markDirty);
	}
}
