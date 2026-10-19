package com.jjkcraft.curse.mechanic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Thorn Matriarch (Grade 2): a rooted mother of briars that grows stronger as the day goes on:
 * weakest at night and dawn, strongest at dusk (more damage, faster roots). It marks the ground
 * under its target (the soil cracks and red spores rise for a second) and then roots erupt there:
 * whoever is still on the mark is bound and cut, a ring of roots walls the spot, and a hedge of
 * roots blocks the path behind the target. Close in and its bark bites back. Fight it early in the
 * day, or at night.
 *
 * MECH_A: 1 while a root mark is pending. MECH_B: day strength in percent. Roots are temporary
 * blocks; they never drop.
 */
public final class ThornMechanic implements CurseMechanic {
	private static final int SPORE_COLOR = 0xB8333F;

	/** 0 at dawn rising to 1 at dusk; night counts as 0.2. */
	public static double dayStrength(ServerLevel level) {
		long time = level.getOverworldClockTime() % 24000;
		return time < 12000 ? time / 12000.0 : 0.2;
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		double strength = dayStrength(level);
		curse.setMechB((int) Math.round(strength * 100));
		int windup = MechanicUtil.get(curse, "root_windup");
		if (windup > 0) {
			Vec3 mark = (Vec3) curse.memory.get("root_mark");
			if (mark != null && windup % 3 == 0) {
				MechanicUtil.ring(level, JjkParticles.of(JjkParticles.CE_AURA, SPORE_COLOR, 0.7f), mark, 1.4, 14);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(BlockPos.containing(mark).below())),
						mark.x, mark.y + 0.1, mark.z, 6, 0.6, 0.05, 0.6, 0.1);
			}

			MechanicUtil.set(curse, "root_windup", windup - 1);
			if (windup == 1 && mark != null) {
				erupt(curse, level, mark);
			}

			return;
		}

		LivingEntity target = curse.getTarget();
		if (MechanicUtil.countDown(curse, "root_cooldown") <= 0 && target != null && target.onGround()
				&& curse.distanceTo(target) <= curse.parameter("root_range", 14) && curse.getSensing().hasLineOfSight(target)) {
			curse.memory.put("root_mark", target.position());
			MechanicUtil.set(curse, "root_windup", (int) curse.parameter("root_windup", 22));
			curse.setMechA(1);
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(30);
			MechanicUtil.sound(level, curse, JjkSounds.CHAIN, 0.8f, 0.6f);
		}
	}

	private void erupt(CurseEntity curse, ServerLevel level, Vec3 mark) {
		curse.setMechA(0);
		double strength = 1 + curse.parameter("day_bonus", 0.6) * dayStrength(level);
		int bound = (int) curse.parameter("bind_ticks", 40);
		float damage = (float) (curse.parameter("root_damage", 5) * strength);
		AABB zone = new AABB(mark.x - 1.3, mark.y - 0.5, mark.z - 1.3, mark.x + 1.3, mark.y + 2, mark.z + 1.3);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, zone, e -> e.isAlive() && !(e instanceof CurseEntity))) {
			if (victim instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) {
				continue;
			}

			victim.hurtServer(level, MechanicUtil.attack(level, curse), damage);
			victim.addEffect(new MobEffectInstance(JjkEffects.BOUND, bound, 0, false, true), curse);
		}

		// A ring of roots around the mark (not on it, so nobody gets stuck inside a block).
		int lifetime = (int) curse.parameter("root_lifetime", 200);
		BlockPos center = BlockPos.containing(mark);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != 2 || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
					continue;
				}

				if (curse.getRandom().nextFloat() < 0.75f) {
					BlockPos pos = groundAbove(level, center.offset(dx, 0, dz));
					if (pos != null) {
						JjkBlocks.CURSED_ROOT.placeFor(level, pos, lifetime + curse.getRandom().nextInt(40));
					}
				}
			}
		}

		hedge(curse, level, mark, lifetime);
		level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, SPORE_COLOR, 1f), mark.x, mark.y + 0.6, mark.z, 20, 0.8, 0.6, 0.8, 0.1);
		MechanicUtil.sound(level, curse, JjkSounds.STRIKE, 1.2f, 0.5f);
		MechanicUtil.set(curse, "root_cooldown", (int) (curse.parameter("root_interval", 120) * (1 - 0.4 * dayStrength(level))));
	}

	/** A hedge of roots across the path behind the mark, cutting off the retreat. */
	private static void hedge(CurseEntity curse, ServerLevel level, Vec3 mark, int lifetime) {
		Vec3 away = mark.subtract(curse.position()).multiply(1, 0, 1);
		if (away.lengthSqr() < 1e-4) {
			return;
		}

		away = away.normalize();
		Vec3 across = new Vec3(-away.z, 0, away.x);
		Vec3 center = mark.add(away.scale(3));
		int half = (int) curse.parameter("hedge_half_width", 3);
		for (int i = -half; i <= half; i++) {
			BlockPos pos = groundAbove(level, BlockPos.containing(center.add(across.scale(i))));
			if (pos != null) {
				JjkBlocks.CURSED_ROOT.placeFor(level, pos, lifetime);
				JjkBlocks.CURSED_ROOT.placeFor(level, pos.above(), lifetime);
			}
		}
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return curse.level() instanceof ServerLevel level ? 1 + curse.parameter("day_bonus", 0.6) * dayStrength(level) : 1;
	}

	/** The free spot resting on solid ground at or near {@code pos}, or null. */
	private static BlockPos groundAbove(ServerLevel level, BlockPos pos) {
		for (int dy = 1; dy >= -2; dy--) {
			BlockPos at = pos.above(dy);
			if ((level.getBlockState(at).isAir() || level.getBlockState(at).canBeReplaced())
					&& level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
				return at;
			}
		}

		return null;
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity() && attacker != curse
				&& attacker.distanceTo(curse) < 5) {
			float reflected = amount * (float) curse.parameter("thorns_fraction", 0.2);
			if (reflected > 0.1f) {
				attacker.hurtServer(level, level.damageSources().thorns(curse), reflected);
			}
		}

		return amount;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return MechanicUtil.get(curse, "root_windup") == 0;
	}
}
