package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Mending another: heals the ally in sight (anyone who is not an enemy) by {@code amount} and
 * gives them regeneration. With no ally in sight the caster mends themselves, less well.
 */
public record MendOtherBehavior(double amount, int regenTicks, double selfFraction) implements TechniqueBehavior {
	public static final MapCodec<MendOtherBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1000).fieldOf("amount").forGetter(MendOtherBehavior::amount),
			Codec.intRange(0, 1200).optionalFieldOf("regen_ticks", 100).forGetter(MendOtherBehavior::regenTicks),
			Codec.doubleRange(0, 1).optionalFieldOf("self_fraction", 0.6).forGetter(MendOtherBehavior::selfFraction)
	).apply(i, MendOtherBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.MEND_OTHER;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 10;
		// The nearest ally within a narrow cone of the caster's facing (forgiving for small allies).
		LivingEntity ally = null;
		Vec3 eye = ctx.eye();
		double best = Double.MAX_VALUE;
		for (LivingEntity candidate : ctx.level().getEntitiesOfClass(LivingEntity.class, ctx.caster().getBoundingBox().inflate(reach),
				e -> e != ctx.caster() && e.isAlive() && !ctx.isEnemy(e))) {
			Vec3 to = candidate.position().add(0, candidate.getBbHeight() / 2, 0).subtract(eye);
			double distance = to.length();
			if (distance <= reach && distance > 1e-3 && to.normalize().dot(ctx.look()) > 0.9 && distance < best) {
				best = distance;
				ally = candidate;
			}
		}

		LivingEntity mended = ally != null ? ally : ctx.caster();
		double heal = amount * ctx.power() * (ally != null ? 1 : selfFraction);
		mended.heal((float) heal);
		if (regenTicks > 0) {
			mended.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0, false, true));
		}

		if (ally != null) {
			Vec3 from = ctx.eye().add(0, -0.3, 0);
			ctx.fx(TechniqueFxPayload.Phase.CAST, from, mended.position().add(0, mended.getBbHeight() * 0.6, 0).subtract(from), List.of());
		}

		ctx.fx(TechniqueFxPayload.Phase.HIT, mended.position().add(0, mended.getBbHeight() * 0.6, 0), ctx.look(), List.of(mended));
		return CastResult.hit(List.of(mended), mended.position());
	}
}
