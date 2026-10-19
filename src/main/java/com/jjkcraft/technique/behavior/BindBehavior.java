package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Binds the aimed target in cursed chains: it cannot move or jump for the duration.
 *
 * @param duration ticks the binding holds at power 1
 * @param damage   damage when the chains strike
 */
public record BindBehavior(int duration, double damage) implements TechniqueBehavior {
	public static final MapCodec<BindBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 1200).fieldOf("duration").forGetter(BindBehavior::duration),
			Codec.doubleRange(0, 1000).optionalFieldOf("damage", 0.0).forGetter(BindBehavior::damage)
	).apply(i, BindBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.BIND;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		Optional<LivingEntity> target = ctx.rayTarget(ctx.range(), 0.6);
		if (target.isEmpty()) {
			ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.blockLimit(ctx.range()));
			return CastResult.miss(ctx.blockLimit(ctx.range()));
		}

		LivingEntity hit = target.get();
		int ticks = (int) Math.round(duration * Math.min(2.0, ctx.power()));
		hit.addEffect(new MobEffectInstance(JjkEffects.BOUND, ticks, 0, false, true), ctx.caster());
		hit.setDeltaMovement(Vec3.ZERO);
		hit.needsSync = true;
		ctx.hurt(hit, damage * ctx.power(), false);
		ctx.fx(TechniqueFxPayload.Phase.HIT, hit.position(), new Vec3(ticks, 0, 0), List.of(hit));
		return CastResult.hit(List.of(hit), hit.position());
	}
}
