package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/** A slam: a ring of force from the caster's feet. Damage falls off with distance; everyone hit is thrown up and out. */
public record ShockwaveBehavior(double radius, double damage, double launch) implements TechniqueBehavior {
	public static final MapCodec<ShockwaveBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 24).optionalFieldOf("radius", 6.0).forGetter(ShockwaveBehavior::radius),
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(ShockwaveBehavior::damage),
			Codec.doubleRange(0, 3).optionalFieldOf("launch", 0.6).forGetter(ShockwaveBehavior::launch)
	).apply(i, ShockwaveBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.SHOCKWAVE;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		Vec3 center = ctx.caster().position();
        com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,center,Math.min(radius,2+Math.sqrt(ctx.power())),48,false);
		List<LivingEntity> hit = ctx.enemiesInRadius(center, radius);
		for (LivingEntity enemy : hit) {
			double falloff = 1 - 0.5 * Math.min(1, enemy.distanceTo(ctx.caster()) / radius);
			ctx.hurt(enemy, damage * ctx.power() * falloff, false);
			Vec3 out = enemy.position().subtract(center).multiply(1, 0, 1);
			Vec3 push = out.lengthSqr() > 1e-4 ? out.normalize().scale(launch) : Vec3.ZERO;
			enemy.setDeltaMovement(enemy.getDeltaMovement().add(push.x, launch, push.z));
			enemy.needsSync = true;
		}

		ctx.fx(TechniqueFxPayload.Phase.BURST, center.add(0, 0.2, 0), new Vec3(radius, 0, 0), hit);
		return CastResult.hit(List.<Entity>copyOf(hit), center);
	}
}
