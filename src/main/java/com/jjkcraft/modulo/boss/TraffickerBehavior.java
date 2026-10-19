package com.jjkcraft.modulo.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.modulo.entity.ModuloBossEntity;

/**
 * The Kyoto trafficker (mission 2, chapter 1): a curse user whose barrier technique moves him
 * through his talismans. Every so often he vanishes and reappears behind his target, with a hammer.
 * Params: {@code teleport_interval} (100 ticks), {@code hammer_damage} (7).
 */
final class TraffickerBehavior implements BossBehavior {
	private int cooldown = 60;

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		LivingEntity target = boss.getTarget();
		if (target == null || !target.isAlive() || --cooldown > 0) {
			return;
		}

		cooldown = (int) boss.param("teleport_interval", 100);
		Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
		level.sendParticles(ParticleTypes.ENCHANT, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 0.8, 0.4, 0.4);
		boss.teleportTo(behind.x, target.getY(), behind.z);
		boss.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 0.7f);
		if (boss.distanceToSqr(target) < 9) {
			target.hurtServer(level, level.damageSources().mobAttack(boss), (float) boss.param("hammer_damage", 7));
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 1.4f);
		}
	}
}
