package com.jjkcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block a curse raises for a while (roots, arena walls). It removes itself with a scheduled tick,
 * which the game saves with the chunk, so it disappears even after a restart. It never drops.
 */
public class ExpiringBlock extends Block {
	public ExpiringBlock(Properties properties) {
		super(properties);
	}

	/** Places this block if the spot is free (air or replaceable plants) and schedules its removal. */
	public boolean placeFor(ServerLevel level, BlockPos pos, int ticks) {
		BlockState current = level.getBlockState(pos);
		if (!current.isAir() && !current.canBeReplaced()) {
			return false;
		}

		level.setBlock(pos, defaultBlockState(), Block.UPDATE_ALL);
		level.scheduleTick(pos, this, ticks);
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}
}
