package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Shape-alter: aimed at someone within reach, their body shrinks (quicker, feebler); aimed at
 * nothing, the caster bulks up (bigger, armored, slower). Either way a stat is traded for another.
 */
public record IdleTransfigurationBehavior(int selfTicks, int otherTicks) implements TechniqueBehavior {
	public static final MapCodec<IdleTransfigurationBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 12_000).optionalFieldOf("self_ticks", 400).forGetter(IdleTransfigurationBehavior::selfTicks),
			Codec.intRange(1, 12_000).optionalFieldOf("other_ticks", 200).forGetter(IdleTransfigurationBehavior::otherTicks)
	).apply(i, IdleTransfigurationBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.IDLE_TRANSFIGURATION;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 6;
		Optional<LivingEntity> target = ctx.rayTarget(reach, 0.4);
		if (target.isPresent()) {
			LivingEntity other = target.get();
			other.addEffect(new MobEffectInstance(JjkEffects.SHRUNK, (int) (otherTicks * ctx.power()), 0, false, true), ctx.caster());
			ctx.fx(TechniqueFxPayload.Phase.HIT, other.position().add(0, other.getBbHeight() * 0.5, 0), ctx.look(), List.of(other));
			return CastResult.hit(List.of(other), other.position());
		}

		ctx.caster().addEffect(new MobEffectInstance(JjkEffects.BULKED, (int) (selfTicks * ctx.power()), 0, false, true));
		ctx.fx(TechniqueFxPayload.Phase.BURST, ctx.caster().position().add(0, 1, 0));
		return CastResult.done(ctx.caster().position());
	}
}
