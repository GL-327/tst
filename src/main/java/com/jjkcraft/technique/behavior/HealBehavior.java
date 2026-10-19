package com.jjkcraft.technique.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Channelled healing: while held, CE flows into health. Stops when health is full or CE runs out.
 *
 * @param healthPerSecond health restored per second at power 1
 * @param cePerHealth     CE spent per point of health
 */
public record HealBehavior(double healthPerSecond, double cePerHealth) implements TechniqueBehavior {
	public static final MapCodec<HealBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1000).fieldOf("health_per_second").forGetter(HealBehavior::healthPerSecond),
			Codec.doubleRange(0, 1000).fieldOf("ce_per_health").forGetter(HealBehavior::cePerHealth)
	).apply(i, HealBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.HEAL;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, ctx.caster().position());
		return CastResult.done(ctx.caster().position());
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		LivingEntity caster = ctx.caster();
		float missing = caster.getMaxHealth() - caster.getHealth();
		if (missing <= 0.01f) {
			return false;
		}

		double heal = Math.min(missing, healthPerSecond / 20.0 * ctx.power());
		double ce = heal * cePerHealth;
		if (!CursedEnergy.trySpend(caster, ce)) {
			return false;
		}

		caster.heal((float) TechniqueMath.healing(ce, 1 / cePerHealth, 1.0));
		return true;
	}

	@Override
	public void end(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_END, ctx.caster().position());
	}
}
