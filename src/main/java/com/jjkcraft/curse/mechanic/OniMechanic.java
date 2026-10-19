package com.jjkcraft.curse.mechanic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Ash Oni (Grade 1): a heavy hitter. Its signature is the charge-sweep: it lowers its horns
 * (embers gather and it paws the ground), locks a direction, and charges in a straight line,
 * sweeping aside everything near its path. Dodge sideways. If it charges into a wall, or takes a
 * heavy beating in a short time, it is staggered: stunned for a few seconds and easy to wound.
 * Up close it slams instead. Below 40% health it rages: faster, with shorter wind-ups.
 *
 * MECH_A: 0 ready, 1 winding up a charge, 2 charging, 3 staggered. MECH_B: wind-up or stagger ticks left.
 */
public final class OniMechanic implements CurseMechanic {
	private static final Identifier RAGE_SPEED = JjkCraft.id("oni_rage_speed");
	private static final int EMBER_COLOR = 0xFF6A2B;

	private static final int READY = 0;
	private static final int WINDUP = 1;
	private static final int CHARGING = 2;
	private static final int STAGGERED = 3;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setMechB(-1);
	}

	private static boolean enraged(CurseEntity curse) {
		return curse.getHealth() / curse.getMaxHealth() <= curse.parameter("rage_threshold", 0.4);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		boolean rage = enraged(curse);
		if (rage && !(curse.memory.get("raged") instanceof Boolean)) {
			curse.memory.put("raged", true);
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_ROAR, 2f, 0.8f);
			AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.addOrUpdateTransientModifier(new AttributeModifier(RAGE_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}

		if (curse.tickCount % 5 == 0) {
			level.sendParticles(rage ? ParticleTypes.FLAME : ParticleTypes.SMOKE, curse.getX(), curse.getY() + curse.getBbHeight() * 0.7, curse.getZ(), 1,
					curse.getBbWidth() * 0.4, 0.4, curse.getBbWidth() * 0.4, 0.01);
		}

		decayPoise(curse);
		double pace = rage ? 0.6 : 1.0;
		switch (curse.mechA()) {
			case WINDUP -> windup(curse, level, pace);
			case CHARGING -> charge(curse, level);
			case STAGGERED -> {
				curse.getNavigation().stop();
				if (curse.tickCount % 4 == 0) {
					MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xFFFFFF, 0.6f), curse.getEyePosition(), 3, 0.4, 0.05);
				}

				if (countDownB(curse) <= 0) {
					curse.setMechA(READY);
				}
			}
			default -> ready(curse, level, pace);
		}
	}

	private static int countDownB(CurseEntity curse) {
		int next = Math.max(0, curse.mechB() - 1);
		curse.setMechB(next);
		return next;
	}

	private void ready(CurseEntity curse, ServerLevel level, double pace) {
		LivingEntity target = curse.getTarget();
		if (target == null) {
			return;
		}

		double distance = curse.distanceTo(target);
		double slamRadius = curse.parameter("slam_radius", 4.0);
		if (distance <= slamRadius && MechanicUtil.countDown(curse, "slam_cooldown") <= 0) {
			slam(curse, level, slamRadius);
			MechanicUtil.set(curse, "slam_cooldown", (int) (curse.parameter("slam_interval", 100) * pace));
			return;
		}

		if (MechanicUtil.countDown(curse, "charge_cooldown") <= 0 && distance >= curse.parameter("charge_min", 5)
				&& distance <= curse.parameter("charge_max", 18) && curse.getSensing().hasLineOfSight(target)) {
			curse.setMechA(WINDUP);
			curse.setMechB((int) (curse.parameter("charge_windup", 22) * pace));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(60);
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_ROAR, 1.4f, 1.1f);
		}
	}

	private void windup(CurseEntity curse, ServerLevel level, double pace) {
		curse.getNavigation().stop();
		LivingEntity target = curse.getTarget();
		if (target != null) {
			curse.brain().aimAt(target);
		}

		if (curse.tickCount % 2 == 0) {
			level.sendParticles(JjkParticles.of(JjkParticles.CE_AURA, EMBER_COLOR, 1f), curse.getX(), curse.getY() + 0.2, curse.getZ(), 4,
					curse.getBbWidth() * 0.5, 0.1, curse.getBbWidth() * 0.5, 0.05);
		}

		if (countDownB(curse) > 0) {
			return;
		}

		// Lock the direction now: from here on it can't steer.
		Vec3 dir = target == null ? curse.getLookAngle() : target.position().subtract(curse.position());
		dir = dir.multiply(1, 0, 1);
		if (dir.lengthSqr() < 1e-4) {
			dir = curse.getLookAngle().multiply(1, 0, 1);
		}

		curse.memory.put("charge_dir", dir.normalize());
		curse.memory.put("charge_hit", new HashSet<UUID>());
		curse.setMechA(CHARGING);
		curse.setMechB((int) curse.parameter("charge_ticks", 25));
		MechanicUtil.set(curse, "charge_cooldown", (int) (curse.parameter("charge_interval", 160) * pace));
	}

	@SuppressWarnings("unchecked")
	private void charge(CurseEntity curse, ServerLevel level) {
		Vec3 dir = (Vec3) curse.memory.get("charge_dir");
		Set<UUID> hit = (Set<UUID>) curse.memory.get("charge_hit");
		if (dir == null || hit == null || countDownB(curse) <= 0) {
			curse.setMechA(READY);
			return;
		}

		curse.getNavigation().stop();
		double speed = curse.parameter("charge_speed", 0.85);
		curse.setDeltaMovement(dir.x * speed, Math.min(0, curse.getDeltaMovement().y), dir.z * speed);
		curse.needsSync = true;
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, curse.getX(), curse.getY() + 0.3, curse.getZ(), 2, 0.4, 0.1, 0.4, 0.01);

		// The sweep: everything near the path is thrown aside, once per charge.
		float damage = (float) curse.parameter("sweep_damage", 10);
		double width = curse.parameter("sweep_width", 2.2);
		for (LivingEntity victim : MechanicUtil.victims(level, curse, width + 1)) {
			if (hit.add(victim.getUUID())) {
				victim.hurtServer(level, MechanicUtil.attack(level, curse), damage);
				Vec3 side = new Vec3(-dir.z, 0, dir.x);
				Vec3 rel = victim.position().subtract(curse.position());
				Vec3 push = side.scale(Math.signum(rel.dot(side)) == 0 ? 1 : Math.signum(rel.dot(side))).add(dir.scale(0.5));
				victim.setDeltaMovement(push.x * 1.2, 0.45, push.z * 1.2);
				victim.needsSync = true;
				victim.igniteForSeconds(2);
			}
		}

		// Into a wall: the charge breaks and so does its footing.
		if (curse.horizontalCollision) {
			stagger(curse, level, (int) curse.parameter("wall_stagger_ticks", 70));
		}
	}

	private void slam(CurseEntity curse, ServerLevel level, double radius) {
		curse.triggerAnim("action", "attack");
		float damage = (float) curse.parameter("slam_damage", 9);
		for (LivingEntity victim : MechanicUtil.victims(level, curse, radius)) {
			if (Math.abs(victim.getY() - curse.getY()) <= 2.5) {
				victim.hurtServer(level, MechanicUtil.attack(level, curse), damage);
				MechanicUtil.push(curse, level, victim, curse.position(), 1.0);
			}
		}

		Vec3 at = curse.position();
		level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, EMBER_COLOR, 1.3f), at.x, at.y + 0.3, at.z, 30, radius / 2, 0.3, radius / 2, 0.2);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 1, 0.3, 0.1, 0.3, 0);
		MechanicUtil.sound(level, curse, JjkSounds.BLACK_FLASH, 1.4f, 0.5f);
	}

	private void stagger(CurseEntity curse, ServerLevel level, int ticks) {
		curse.setMechA(STAGGERED);
		curse.setMechB(ticks);
		curse.setDeltaMovement(curse.getDeltaMovement().multiply(-0.3, 1, -0.3));
		curse.addEffect(new MobEffectInstance(JjkEffects.STUNNED, ticks, 0, false, true));
		MechanicUtil.set(curse, "poise", 0);
		MechanicUtil.sound(level, curse, JjkSounds.CURSE_HURT, 1.6f, 0.5f);
		level.sendParticles(ParticleTypes.CRIT, curse.getX(), curse.getEyeY(), curse.getZ(), 20, 0.6, 0.4, 0.6, 0.2);
	}

	/** Poise: damage taken in the last few seconds. Too much at once staggers it. */
	private static void decayPoise(CurseEntity curse) {
		if (curse.tickCount % 20 == 0) {
			MechanicUtil.set(curse, "poise", (int) (MechanicUtil.get(curse, "poise") * 0.6));
		}
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechA() == STAGGERED) {
			return amount * (float) curse.parameter("stagger_damage_taken", 1.5);
		}

		int poise = MechanicUtil.get(curse, "poise") + Math.round(amount);
		MechanicUtil.set(curse, "poise", poise);
		if (poise >= curse.parameter("stagger_poise", 30) && curse.mechA() != CHARGING) {
			stagger(curse, level, (int) curse.parameter("poise_stagger_ticks", 40));
		}

		return amount;
	}

	@Override
	public boolean canAct(CurseEntity curse) {
		return curse.mechA() != STAGGERED;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechA() == READY;
	}

	/** Whether the oni is staggered (tests, HUD). */
	public static boolean staggered(CurseEntity curse) {
		return curse.mechA() == STAGGERED;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(RAGE_SPEED);
		}
	}
}
