package com.jjkcraft.modulo.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.modulo.entity.ModuloBossEntity;

/**
 * Toranosuke (mission 3, chapter 3): a man-eating tiger driven down from the mountain. It roars,
 * crouches for a moment (the tell), then pounces; a pounce that lands mauls and slows.
 * Params: {@code pounce_interval} (90 ticks), {@code windup} (16 ticks), {@code pounce_damage} (8).
 */
final class ToranosukeBehavior implements BossBehavior {
	private int cooldown = 60;
	private int windup = -1;
	private boolean leaping;

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		LivingEntity target = boss.getTarget();
		if (leaping) {
			if (target != null && boss.distanceToSqr(target) < 4) {
				target.hurtServer(level, level.damageSources().mobAttack(boss), (float) boss.param("pounce_damage", 8));
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), boss);
				leaping = false;
			} else if (boss.onGround() && boss.tickCount % 2 == 0) {
				leaping = false;
			}
		}

		if (target == null || !target.isAlive()) {
			windup = -1;
			return;
		}

		if (windup > 0) {
			boss.getNavigation().stop();
			level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.2, boss.getZ(), 2, 0.4, 0.05, 0.4, 0.01);
			if (--windup == 0) {
				Vec3 to = target.position().subtract(boss.position());
				Vec3 leap = to.multiply(1, 0, 1).normalize().scale(Math.min(1.6, to.horizontalDistance() * 0.18));
				boss.setDeltaMovement(leap.x, 0.55, leap.z);
				boss.needsSync = true;
				leaping = true;
				cooldown = (int) boss.param("pounce_interval", 90);
			}

			return;
		}

		double distance = boss.distanceTo(target);
		if (--cooldown <= 0 && distance > 3 && distance < 12 && boss.onGround()) {
			windup = (int) boss.param("windup", 16);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 1.3f);
		}
	}
}
