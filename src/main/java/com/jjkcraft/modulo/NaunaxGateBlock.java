package com.jjkcraft.modulo;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pad of Simurian light: step on it to travel. Mission sites place these; which way one goes is
 * its {@code destination} (back to Earth, down to Tokyo 2086, or up to the ship).
 */
public class NaunaxGateBlock extends Block {
	public enum Destination implements StringRepresentable {
		EARTH, TOKYO, SHIP;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Destination> DESTINATION = EnumProperty.create("destination", Destination.class);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

	public NaunaxGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DESTINATION, Destination.EARTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DESTINATION);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
		if (entity instanceof ServerPlayer player && !level.isClientSide()) {
			ModuloTravel.throughGate(player, state.getValue(DESTINATION));
		}
	}
}
