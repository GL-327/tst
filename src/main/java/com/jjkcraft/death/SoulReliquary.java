package com.jjkcraft.death;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A placed cursed object. Its identity lives in a persistent per-level attachment. */
public final class SoulReliquary extends Block {
    public SoulReliquary(Properties p) { super(p); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            SoulObject object = SoulSystem.takePlaced(server, pos);
            if (object != null) { var item = SoulSystem.stack(object); if (!player.getInventory().add(item)) popResource(server, pos, item); server.removeBlock(pos, false); }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) { SoulObject object = SoulSystem.takePlaced(server, pos); if (object != null) popResource(server, pos, SoulSystem.stack(object)); }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        SoulObject object = SoulSystem.takePlaced(level, pos);
        if (object != null) SoulSystem.destroy(level.getServer(), object);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
