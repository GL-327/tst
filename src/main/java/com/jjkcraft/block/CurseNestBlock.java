package com.jjkcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.jjkcraft.fear.Fear;
import com.jjkcraft.sorcerer.Progression;

/**
 * A curse nest: a clot of fear that keeps spawning curses until a player breaks it. Breaking it
 * calms the area (lowers stored fear) and gives grade experience.
 */
public class CurseNestBlock extends BaseEntityBlock {
	public CurseNestBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CurseNestBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, JjkBlocks.CURSE_NEST_ENTITY, CurseNestBlockEntity::serverTick);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel server) {
			Fear.add(server, pos, -40);
			if (player instanceof ServerPlayer serverPlayer) {
				Progression.addXp(serverPlayer, 60);
			}
		}

		return super.playerWillDestroy(level, pos, state, player);
	}
}
