package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * A passive field (toggle) that slows projectiles flying toward the caster. The caster's own
 * projectiles are not affected.
 *
 * @param radius   field radius
 * @param slow     velocity multiplier applied each tick to incoming projectiles
 * @param minSpeed projectiles are not slowed below this speed (blocks per tick)
 */
public record InfinityBehavior(double radius, double slow, double minSpeed) implements TechniqueBehavior {
	public static final MapCodec<InfinityBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 32).fieldOf("radius").forGetter(InfinityBehavior::radius),
			Codec.doubleRange(0, 1).fieldOf("slow").forGetter(InfinityBehavior::slow),
			Codec.doubleRange(0, 4).optionalFieldOf("min_speed", 0.05).forGetter(InfinityBehavior::minSpeed)
	).apply(i, InfinityBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.INFINITY;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, ctx.caster().position(), new Vec3(radius, 0, 0), List.of());
		return CastResult.done(ctx.caster().position());
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		Vec3 center = ctx.caster().position().add(0, ctx.caster().getBbHeight() / 2, 0);
		AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
		for (Projectile projectile : ctx.level().getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() != ctx.caster())) {
			Vec3 velocity = projectile.getDeltaMovement();
			Vec3 toCaster = center.subtract(projectile.position());
			if (velocity.lengthSqr() < minSpeed * minSpeed || toCaster.length() > radius || velocity.dot(toCaster) <= 0) {
				continue;
			}

			projectile.setDeltaMovement(velocity.scale(slow));
			projectile.needsSync = true;
		}

		return true;
	}

	@Override
	public void end(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_END, ctx.caster().position());
	}
}
