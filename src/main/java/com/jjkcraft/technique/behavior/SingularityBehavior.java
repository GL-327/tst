package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * A collapsing star (channel): a point of crushing pull opens {@code distance} blocks ahead and
 * drags everyone within {@code radius} toward it for {@code pull_ticks}, then bursts, striking all
 * within {@code blast_radius} with damage that mostly ignores armor.
 */
public record SingularityBehavior(double distance, double radius, int pullTicks, double strength, double damage, double blastRadius)
		implements TechniqueBehavior {
	public static final MapCodec<SingularityBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 48).optionalFieldOf("distance", 12.0).forGetter(SingularityBehavior::distance),
			Codec.doubleRange(1, 32).optionalFieldOf("radius", 10.0).forGetter(SingularityBehavior::radius),
			Codec.intRange(1, 200).optionalFieldOf("pull_ticks", 40).forGetter(SingularityBehavior::pullTicks),
			Codec.doubleRange(0, 2).optionalFieldOf("strength", 0.25).forGetter(SingularityBehavior::strength),
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(SingularityBehavior::damage),
			Codec.doubleRange(0.5, 32).optionalFieldOf("blast_radius", 6.0).forGetter(SingularityBehavior::blastRadius)
	).apply(i, SingularityBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.SINGULARITY;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		Vec3 star = ctx.blockLimit(distance);
		ctx.memory.put("star", star);
        var start=com.jjkcraft.technique.field.SpatialShots.origin(ctx);int flight=com.jjkcraft.technique.field.SpatialShots.flight(start,star);
        ctx.memory.put("flight",flight);com.jjkcraft.technique.field.SpatialShots.visual(ctx,start,star,Math.min(3,radius*.2),flight);
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, star);
		return CastResult.done(star);
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		if (!(ctx.memory.get("star") instanceof Vec3 star)) {
			return false;
		}
        int flight=(int)ctx.memory.getOrDefault("flight",0);if(ticksActive<flight)return true;ticksActive-=flight;

		if (ticksActive < pullTicks) {
			for (LivingEntity enemy : ctx.enemiesInRadius(star, radius)) {
				Vec3 toward = star.subtract(enemy.position());
				if (toward.lengthSqr() > 0.25) {
					enemy.setDeltaMovement(enemy.getDeltaMovement().scale(0.5).add(toward.normalize().scale(strength)));
					enemy.needsSync = true;
				}
			}

			if (ticksActive % 5 == 0) {
                com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,star,radius,16,true);
				ctx.fx(TechniqueFxPayload.Phase.FIELD, star, new Vec3(radius, 6, 1), List.of());
			}

			return true;
		}

		List<LivingEntity> caught = ctx.enemiesInRadius(star, blastRadius);
        com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,star,blastRadius,64,false);
		for (LivingEntity enemy : caught) {
			ctx.hurtSplit(enemy, damage * ctx.power(), 0.7);
			ctx.knockback(enemy, star, 1.6);
		}

		ctx.fx(TechniqueFxPayload.Phase.BURST, star, new Vec3(blastRadius, 0, 0), caught);
		return false;
	}
}
