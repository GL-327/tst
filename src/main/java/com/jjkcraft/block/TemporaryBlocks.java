package com.jjkcraft.block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Temporary vanilla blocks (Tidewash's flood water). Each placement remembers what was there and
 * restores it when it expires, and every pending change is restored when the server stops.
 * Only air is ever replaced, so nothing a player built is touched.
 */
public final class TemporaryBlocks {
	private record Pending(ServerLevel level, BlockPos pos, BlockState placed, BlockState original, long expiresAt) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private TemporaryBlocks() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TemporaryBlocks::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> restoreAll());
	}

	/** Places {@code state} at {@code pos} if it is air, for {@code ticks}. */
	public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
		BlockState current = level.getBlockState(pos);
		if (!current.isAir() || PENDING.size() > 2048) {
			return false;
		}

		level.setBlock(pos, state, Block.UPDATE_ALL);
		PENDING.add(new Pending(level, pos.immutable(), state, current, level.getGameTime() + ticks));
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}

		for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
			Pending p = it.next();
			if (p.level().getGameTime() >= p.expiresAt()) {
				restore(p);
				it.remove();
			}
		}
	}

	private static void restore(Pending p) {
		// Only undo our own change: if something else replaced the block since, leave it.
		if (p.level().getBlockState(p.pos()).getBlock() == p.placed().getBlock()) {
			p.level().setBlock(p.pos(), p.original(), Block.UPDATE_ALL);
		}
	}

	private static void restoreAll() {
		PENDING.forEach(TemporaryBlocks::restore);
		PENDING.clear();
	}
}
