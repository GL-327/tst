package com.jjkcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.particle.JjkParticles;

/** Spawns curses around a nest while a player is near, keeping at most a few alive at once. */
public class CurseNestBlockEntity extends BlockEntity {
	private static final int INTERVAL = 160;
	private static final int MAX_ALIVE = 3;
	private int cooldown = 40;
	private int spawned;

	public CurseNestBlockEntity(BlockPos pos, BlockState state) {
		super(JjkBlocks.CURSE_NEST_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CurseNestBlockEntity nest) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		if (server.getGameTime() % 10 == 0) {
			server.sendParticles(JjkParticles.of(JjkParticles.CE_AURA, 0x3A1A4A, 1.4f), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
					3, 0.4, 0.3, 0.4, 0.01);
		}

		if (--nest.cooldown > 0) {
			return;
		}

		nest.cooldown = INTERVAL;
		if (server.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 24, false) == null) {
			return;
		}

		AABB box = new AABB(pos).inflate(16);
		if (server.getEntitiesOfClass(CurseEntity.class, box).size() >= MAX_ALIVE) {
			return;
		}

		if (CurseSpawner.spawnNear(server, pos.above(), 2, 6) > 0) {
			nest.spawned++;
			nest.setChanged();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("spawned", spawned);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		spawned = input.getIntOr("spawned", 0);
	}
}
