package com.jjkcraft.curse.mechanic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sound.JjkSounds;

/**
 * Wick Moth (Grade 4): swarms in darkness and eats light. It snuffs candles and campfires and
 * knocks out torches and lanterns nearby (only when mob griefing is on), healing a little and
 * gaining CE each time. In darkness (light 4 or less) it flies faster and bites harder.
 *
 * MECH_A: 1 while empowered by darkness.
 */
public final class WickMechanic implements CurseMechanic {
	private static final net.minecraft.resources.Identifier DARK_SPEED = JjkCraft.id("wick_dark_speed");

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		boolean dark = level.getMaxLocalRawBrightness(curse.blockPosition()) <= 4;
		curse.setMechA(dark ? 1 : 0);
		AttributeInstance speed = curse.getAttribute(Attributes.FLYING_SPEED);
		if (speed != null) {
			if (dark) {
				speed.addOrUpdateTransientModifier(new AttributeModifier(DARK_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			} else {
				speed.removeModifier(DARK_SPEED);
			}
		}

		if (curse.tickCount % 20 != 0 || !MechanicUtil.griefing(level)) {
			return;
		}

		int radius = (int) curse.parameter("eat_radius", 6);
		BlockPos center = curse.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
			if (eat(level, pos.immutable())) {
				curse.heal(2);
				CursedEnergy.restore(curse, 8);
				MechanicUtil.sound(level, curse, JjkSounds.ATTRACT, 0.4f, 1.8f);
				level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
				return;
			}
		}
	}

	/** Puts out one light source. Returns true if it did. */
	private static boolean eat(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if ((state.is(BlockTags.CANDLES) || state.is(BlockTags.CAMPFIRES)) && state.hasProperty(BlockStateProperties.LIT)
				&& state.getValue(BlockStateProperties.LIT)) {
			level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, false));
			return true;
		}

		if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
				|| state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.JACK_O_LANTERN)) {
			level.destroyBlock(pos, true);
			return true;
		}

		return false;
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return curse.mechA() == 1 ? 1.4 : 1.0;
	}
}
