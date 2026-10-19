package com.jjkcraft.modulo.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.modulo.entity.ModuloBossEntity;

/**
 * Maru's duel (mission 9, chapters 14 to 21), optional: Chaos "distorts the laws of order". On a
 * timer the arena's gravity turns over for a few seconds (the tell: the air shimmers and rises for
 * {@code inversion_tell} ticks first; slow falling cushions the drop back). His third eye confuses
 * whoever he looks at, and projectiles near him turn back. He yields at {@code yield_below} health.
 * Params: {@code inversion_interval} (220), {@code inversion_tell} (40), {@code inversion_ticks} (80),
 * {@code arena_radius} (24), {@code gaze_interval} (110), {@code yield_below} (0.15).
 */
final class MaruDuelBehavior implements BossBehavior {
	static final Identifier INVERSION = JjkCraft.id("modulo_chaos_inversion");

	private int inversionTimer = 160;
	private int inverted;
	private int gazeCooldown = 80;
	private final Set<UUID> turned = new HashSet<>();

	@Override
	public float onHurt(ModuloBossEntity boss, ServerLevel level, DamageSource source, float amount) {
		return amount;
	}

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		double radius = boss.param("arena_radius", 24);
		if (boss.getHealth() <= boss.getMaxHealth() * boss.param("yield_below", 0.15)) {
			restore(boss, level);
			boss.yieldFight(Component.translatable("message.jjkcraft.modulo.maru.yield"));
			return;
		}

		// Projectiles that come near turn back.
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, boss.getBoundingBox().inflate(5))) {
			if (projectile.getOwner() != boss && projectile.getDeltaMovement().lengthSqr() > 0.04) {
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.8));
				projectile.needsSync = true;
			}
		}

		if (inverted > 0) {
			for (ServerPlayer player : BossBehavior.playersNear(boss, level, radius)) {
				turn(player, true);
			}

			if (--inverted == 0) {
				restore(boss, level);
			}
		} else if (--inversionTimer <= 0) {
			inverted = (int) boss.param("inversion_ticks", 80);
			inversionTimer = (int) boss.param("inversion_interval", 220);
			boss.setFlag(ModuloBossEntity.FLAG_INVERTED, true);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.4f, 0.5f);
		} else if (inversionTimer <= boss.param("inversion_tell", 40)) {
			for (ServerPlayer player : BossBehavior.playersNear(boss, level, radius)) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.2, player.getZ(), 4, 0.5, 0.1, 0.5, 0.05);
				if (inversionTimer == (int) boss.param("inversion_tell", 40)) {
					player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.maru.chaos"), true);
				}
			}
		}

		LivingEntity target = boss.getTarget();
		if (target != null && --gazeCooldown <= 0 && boss.hasLineOfSight(target)) {
			if (gazeCooldown == -15) {
				target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0), boss);
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), boss);
				gazeCooldown = (int) boss.param("gaze_interval", 110);
			} else {
				// The tell: his third eye opens.
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, boss.getX(), boss.getEyeY() + 0.25, boss.getZ(), 3, 0.05, 0.05, 0.05, 0);
			}
		}
	}

	private void turn(ServerPlayer player, boolean on) {
		AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
		if (gravity == null) {
			return;
		}

		if (on) {
			gravity.addOrUpdateTransientModifier(new AttributeModifier(INVERSION, -2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			turned.add(player.getUUID());
		} else if (gravity.hasModifier(INVERSION)) {
			gravity.removeModifier(INVERSION);
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
		}
	}

	private void restore(ModuloBossEntity boss, ServerLevel level) {
		boss.setFlag(ModuloBossEntity.FLAG_INVERTED, false);
		inverted = 0;
		for (UUID id : Set.copyOf(turned)) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) {
				turn(player, false);
			}
		}

		turned.clear();
	}

	@Override
	public void onRemoved(ModuloBossEntity boss, ServerLevel level) {
		restore(boss, level);
	}
}
