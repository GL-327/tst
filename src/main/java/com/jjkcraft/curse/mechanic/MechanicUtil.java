package com.jjkcraft.curse.mechanic;

import java.util.List;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;

/** Small helpers shared by curse mechanics: timers kept in the curse's memory, effects, targets. */
final class MechanicUtil {
	private MechanicUtil() {
	}

	static int get(CurseEntity curse, String key) {
		return curse.memory.get(key) instanceof Integer i ? i : 0;
	}

	static void set(CurseEntity curse, String key, int value) {
		curse.memory.put(key, value);
	}

	/** Counts a timer down by one and returns the new value (never below zero). */
	static int countDown(CurseEntity curse, String key) {
		int next = Math.max(0, get(curse, key) - 1);
		set(curse, key, next);
		return next;
	}

	static void sound(ServerLevel level, CurseEntity curse, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	static void particles(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** Particles around a horizontal ring. */
	static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0);
		}
	}

	/** Particles along a segment. */
	static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
		for (int i = 0; i <= points; i++) {
			Vec3 p = from.lerp(to, (double) i / points);
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
		}
	}

	/** Living things the curse may harm within {@code radius}: not curses, not spectators or creative players. */
	static List<LivingEntity> victims(ServerLevel level, CurseEntity curse, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, curse.getBoundingBox().inflate(radius), e -> e != curse && e.isAlive()
				&& !(e instanceof CurseEntity) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()))
				&& e.distanceTo(curse) <= radius);
	}

	static DamageSource attack(ServerLevel level, CurseEntity curse) {
		return level.damageSources().mobAttack(curse);
	}

	/** Knocks {@code victim} away from {@code from}. */
	static void push(CurseEntity curse, ServerLevel level, LivingEntity victim, Vec3 from, double strength) {
		Vec3 away = victim.position().subtract(from).multiply(1, 0, 1);
		if (away.lengthSqr() < 1e-6) {
			away = curse.getLookAngle().multiply(1, 0, 1);
		}

		away = away.normalize();
		victim.knockback(strength, -away.x, -away.z, attack(level, curse), 0f);
		victim.needsSync = true;
	}

	/** Drags {@code victim} toward {@code to}, a little off the ground. */
	static void pull(LivingEntity victim, Vec3 to, double strength) {
		Vec3 toward = to.subtract(victim.position()).multiply(1, 0, 1);
		if (toward.lengthSqr() < 1e-6) {
			return;
		}

		Vec3 velocity = toward.normalize().scale(strength);
		victim.setDeltaMovement(victim.getDeltaMovement().add(velocity.x, 0.25, velocity.z));
		victim.needsSync = true;
	}

	/** Whether {@code player} is looking at {@code target} (within a narrow cone, with line of sight). */
	static boolean isLookingAt(LivingEntity player, LivingEntity target, double maxDistance) {
		Vec3 eye = player.getEyePosition();
		Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(eye);
		double distance = to.length();
		if (distance > maxDistance || distance < 1e-3) {
			return false;
		}

		// Wider cone up close, so a tall curse can't slip out of view by standing next to you.
		double cone = Math.max(0.85, 0.97 - 0.12 / Math.max(1, distance / 3));
		return player.getViewVector(1f).dot(to.scale(1 / distance)) > cone && player.hasLineOfSight(target);
	}

	/**
	 * Keeps a boss bar for {@code curse} up to date and shown to players within {@code range}.
	 * Pass a non-null {@code title} to rename it (phases, edicts). Call {@link #clearBossBar} on removal.
	 */
	static ServerBossEvent bossBar(CurseEntity curse, ServerLevel level, double range, BossEvent.BossBarColor color, Component title) {
		ServerBossEvent bar = (ServerBossEvent) curse.memory.computeIfAbsent("bar", k -> new ServerBossEvent(java.util.UUID.randomUUID(),
				curse.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10));
		bar.setProgress(curse.getHealth() / curse.getMaxHealth());
		if (title != null) {
			bar.setName(title);
		}

		if (curse.tickCount % 10 == 0) {
			for (ServerPlayer player : level.players()) {
				boolean near = player.distanceTo(curse) <= range && player.isAlive();
				if (near && !bar.getPlayers().contains(player)) {
					bar.addPlayer(player);
				} else if (!near && bar.getPlayers().contains(player)) {
					bar.removePlayer(player);
				}
			}
		}

		return bar;
	}

	static void clearBossBar(CurseEntity curse) {
		if (curse.memory.remove("bar") instanceof ServerBossEvent bar) {
			bar.removeAllPlayers();
		}
	}

	/** True while {@code entity} is off the ground and not swimming or climbing. */
	static boolean airborne(LivingEntity entity) {
		return !entity.onGround() && !entity.isInWater() && !entity.onClimbable() && !entity.isPassenger();
	}

	static boolean griefing(ServerLevel level) {
		return level.getGameRules().get(GameRules.MOB_GRIEFING);
	}
}
