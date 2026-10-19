package com.jjkcraft.modulo.boss;

import java.util.List;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.modulo.entity.ModuloBossEntity;

/**
 * What a Modulo boss does (docs/MODULO.md 3.9). Numbers come from the boss's data
 * ({@link ModuloBossEntity#param}); each behavior lists the keys it reads, with their defaults.
 */
public interface BossBehavior {
	void tick(ModuloBossEntity boss, ServerLevel level);

	/** Adjusts damage the boss takes. */
	default float onHurt(ModuloBossEntity boss, ServerLevel level, DamageSource source, float amount) {
		return amount;
	}

	default void onSpawn(ModuloBossEntity boss, ServerLevel level) {
	}

	default void onRemoved(ModuloBossEntity boss, ServerLevel level) {
	}

	static BossBehavior create(String kind) {
		return switch (kind) {
			case "trafficker" -> new TraffickerBehavior();
			case "toranosuke" -> new ToranosukeBehavior();
			case "masaki_takeda" -> new MasakiTakedaBehavior();
			case "maru_duel" -> new MaruDuelBehavior();
			case "dabura" -> new DaburaBehavior();
			case "mahoraga_scene" -> new MahoragaSceneBehavior();
			default -> new MinionBehavior();
		};
	}

	/** Players near the boss, alive and not spectating. */
	static List<ServerPlayer> playersNear(ModuloBossEntity boss, ServerLevel level, double radius) {
		return level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(boss) < radius * radius);
	}

	/** Whether nothing solid stands between two points. */
	static boolean clear(ServerLevel level, Vec3 from, Vec3 to, LivingEntity source) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source)).getType() == HitResult.Type.MISS;
	}

	/** Particles along a line, {@code step} blocks apart. */
	static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		Vec3 dir = delta.normalize();
		for (double d = 0; d <= length; d += step) {
			Vec3 at = from.add(dir.scale(d));
			level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
		}
	}

	/** Living things whose box the segment passes through (within {@code width}). */
	static List<LivingEntity> along(ServerLevel level, LivingEntity except, Vec3 from, Vec3 to, double width) {
		AABB area = new AABB(from, to).inflate(width + 1);
		return level.getEntitiesOfClass(LivingEntity.class, area, e -> e != except && e.isAlive()
				&& e.getBoundingBox().inflate(width).clip(from, to).isPresent());
	}
}
