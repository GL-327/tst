package com.jjkcraft.curse.mechanic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.block.TemporaryBlocks;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.sound.JjkSounds;

/**
 * Tidewash (Grade 2): a drowned grudge, born in water. It rises toward prey near water, and it
 * raises the water: the ground around its target floods for a while (only with mob griefing on;
 * the water drains away by itself and never replaces anything but air). Its long arms rise, then
 * grab: the target is dragged toward it and pulled under, losing breath. In water or rain it is
 * quick and mends itself; on dry ground it is sluggish and takes more damage. Watch for the arms,
 * and fight it on high, dry ground.
 *
 * MECH_A: 1 while wet. MECH_B: arm wind-up ticks left (-1 when none).
 */
public final class TidewashMechanic implements CurseMechanic {
	private static final Identifier WET_SPEED = JjkCraft.id("tidewash_wet_speed");

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		boolean wet = curse.isInWater() || level.isRainingAt(curse.blockPosition()) || level.isRainingAt(curse.blockPosition().above());
		curse.setMechA(wet ? 1 : 0);
		AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(WET_SPEED, wet ? 0.3 : -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}

		if (wet && curse.tickCount % 40 == 0) {
			curse.heal((float) curse.parameter("wet_regen", 1.5));
		}

		if (curse.tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.FALLING_WATER, curse.getX(), curse.getY() + curse.getBbHeight() * 0.8, curse.getZ(), 2,
					curse.getBbWidth() * 0.4, 0.2, curse.getBbWidth() * 0.4, 0);
		}

		LivingEntity target = curse.getTarget();
		// Rising: in water it swims up toward prey above it.
		if (curse.isInWater() && target != null && target.getY() > curse.getY() + 0.5) {
			curse.setDeltaMovement(curse.getDeltaMovement().add(0, 0.06, 0));
		}

		int windup = curse.mechB();
		if (windup > 0) {
			curse.getNavigation().stop();
			if (windup % 3 == 0) {
				Vec3 c = curse.position();
				level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + curse.getBbHeight(), c.z, 12, 0.6, 0.6, 0.6, 0.1);
			}

			curse.setMechB(windup - 1);
			if (windup == 1 && target != null) {
				grab(curse, level, target);
			}

			return;
		}

		if (target == null) {
			return;
		}

		double distance = curse.distanceTo(target);
		if (MechanicUtil.countDown(curse, "flood_cooldown") <= 0 && distance <= curse.parameter("flood_range", 10) && MechanicUtil.griefing(level)) {
			flood(curse, level, target);
		}

		if (MechanicUtil.countDown(curse, "grab_cooldown") <= 0 && distance <= curse.parameter("arm_reach", 7) && curse.getSensing().hasLineOfSight(target)) {
			curse.setMechB((int) curse.parameter("arm_windup", 16));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(40);
			MechanicUtil.sound(level, curse, JjkSounds.ATTRACT, 1.2f, 0.5f);
		}
	}

	/** Raises the water: a shallow flood around the target's feet, in air only, draining by itself. */
	private void flood(CurseEntity curse, ServerLevel level, LivingEntity target) {
		int radius = (int) curse.parameter("flood_radius", 2);
		int ticks = (int) curse.parameter("flood_ticks", 200);
		BlockPos feet = target.blockPosition();
		int placed = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if ((dx + dz) % 2 != 0) {
					continue;
				}

				BlockPos pos = feet.offset(dx, 0, dz);
				if (!level.getBlockState(pos.below()).isAir() && TemporaryBlocks.place(level, pos, Blocks.WATER.defaultBlockState(), ticks)) {
					placed++;
				}
			}
		}

		if (placed > 0) {
			MechanicUtil.sound(level, curse, JjkSounds.REPEL, 1f, 0.4f);
		}

		MechanicUtil.set(curse, "flood_cooldown", (int) curse.parameter("flood_interval", 300));
	}

	/** The long arms: drag the target in and down, stealing breath if it is in water. */
	private void grab(CurseEntity curse, ServerLevel level, LivingEntity target) {
		MechanicUtil.set(curse, "grab_cooldown", (int) curse.parameter("grab_interval", 140));
		if (curse.distanceTo(target) > curse.parameter("arm_reach", 7) + 1.5 || !curse.getSensing().hasLineOfSight(target)) {
			return;
		}

		MechanicUtil.pull(target, curse.position(), curse.parameter("pull_strength", 0.9));
		target.setDeltaMovement(target.getDeltaMovement().add(0, target.isInWater() ? -0.8 : -0.2, 0));
		target.needsSync = true;
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) curse.parameter("grab_slow_ticks", 60), 2, false, true), curse);
		if (target.isInWater()) {
			target.setAirSupply(Math.max(-10, target.getAirSupply() - (int) curse.parameter("breath_stolen", 120)));
		}

		target.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("grab_damage", 4));
		level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1, target.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
		MechanicUtil.sound(level, curse, JjkSounds.CHAIN, 1f, 0.5f);
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		return curse.mechA() == 0 ? amount * (float) curse.parameter("dry_damage_taken", 1.25) : amount;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechB() <= 0;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(WET_SPEED);
		}
	}
}
