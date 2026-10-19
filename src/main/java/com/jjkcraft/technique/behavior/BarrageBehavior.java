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
 * A rain of lances: the caster marks the ground where they look, then a lance falls every
 * {@code interval} ticks within {@code radius} of the mark (every other one at someone standing in
 * it), striking whoever is within {@code strike_radius} of where it lands.
 */
public record BarrageBehavior(double radius, double strikeRadius, double damage, int interval) implements TechniqueBehavior {
	public static final MapCodec<BarrageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 32).optionalFieldOf("radius", 5.0).forGetter(BarrageBehavior::radius),
			Codec.doubleRange(0.5, 8).optionalFieldOf("strike_radius", 1.6).forGetter(BarrageBehavior::strikeRadius),
			Codec.doubleRange(0, 1000).fieldOf("damage").forGetter(BarrageBehavior::damage),
			Codec.intRange(1, 40).optionalFieldOf("interval", 2).forGetter(BarrageBehavior::interval)
	).apply(i, BarrageBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.BARRAGE;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 32;
		Vec3 mark = ctx.rayTarget(reach, 0.6).map(LivingEntity::position).orElseGet(() -> ctx.blockLimit(reach));
		ctx.memory.put("mark", mark);
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, mark);
		// The marked ground shows where the rain will fall.
		int duration = Math.max(20, ctx.technique().cast().maxDuration());
		ctx.fx(TechniqueFxPayload.Phase.FIELD, mark.add(0, 0.1, 0), new Vec3(radius, duration, 0), List.of());
		return CastResult.done(mark);
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		if (ticksActive % interval != 0 || !(ctx.memory.get("mark") instanceof Vec3 mark)) {
			return true;
		}

		// Every other lance seeks someone standing in the rain; the rest fall where they will.
		List<LivingEntity> inRain = ctx.enemiesInRadius(mark, radius);
		Vec3 at;
		if ((ticksActive / interval) % 2 == 0 && !inRain.isEmpty()) {
			at = inRain.get(ctx.level().getRandom().nextInt(inRain.size())).position();
		} else {
			double angle = ctx.level().getRandom().nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(ctx.level().getRandom().nextDouble()) * radius;
			at = mark.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
		}

		List<LivingEntity> struck = ctx.enemiesInRadius(at, strikeRadius);
		for (LivingEntity enemy : struck) {
			ctx.hurt(enemy, damage * ctx.power(), false);
		}

		ctx.fx(TechniqueFxPayload.Phase.CAST, at.add(0, 9, 0), new Vec3(0, -9, 0), List.of());
		ctx.fx(TechniqueFxPayload.Phase.HIT, at, new Vec3(0, -1, 0), struck);
		return true;
	}
}
