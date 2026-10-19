package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * A barrier held in front of the caster. While channelled it absorbs a share of damage coming
 * from the front arc; absorbed damage costs CE, and the barrier breaks when CE runs out.
 *
 * @param block       share of frontal damage absorbed, 0 to 1
 * @param arc         width of the protected arc in degrees
 * @param cePerDamage CE spent per point of damage absorbed
 */
public record BarrierBehavior(double block, double arc, double cePerDamage) implements TechniqueBehavior {
	public static final MapCodec<BarrierBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1).fieldOf("block").forGetter(BarrierBehavior::block),
			Codec.doubleRange(10, 360).optionalFieldOf("arc", 150.0).forGetter(BarrierBehavior::arc),
			Codec.doubleRange(0, 100).optionalFieldOf("ce_per_damage", 1.0).forGetter(BarrierBehavior::cePerDamage)
	).apply(i, BarrierBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.BARRIER;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, ctx.caster().position());
		return CastResult.done(ctx.caster().position());
	}

	@Override
	public void end(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_END, ctx.caster().position());
	}

	/** Applies the barrier to incoming damage. Returns the damage that gets through. */
	public float absorb(CastContext ctx, LivingEntity holder, DamageSource source, float amount) {
		Vec3 from = source.getSourcePosition();
		if (from == null) {
			return amount;
		}

		Vec3 toSource = from.subtract(holder.getEyePosition()).multiply(1, 0, 1);
		Vec3 facing = holder.getLookAngle().multiply(1, 0, 1);
		if (toSource.lengthSqr() < 1e-6 || facing.lengthSqr() < 1e-6) {
			return amount;
		}

		if (toSource.normalize().dot(facing.normalize()) < Math.cos(Math.toRadians(arc / 2))) {
			return amount;
		}

		double absorbed = amount * Math.min(1.0, block * ctx.power());
		if (!CursedEnergy.trySpend(holder, absorbed * cePerDamage)) {
			// Not enough CE to hold: the barrier shatters and the hit lands in full.
			return amount;
		}

		ctx.fx(TechniqueFxPayload.Phase.BLOCK, holder.getEyePosition().add(facing.normalize().scale(0.8)), toSource.normalize(), List.of());
		return (float) (amount - absorbed);
	}
}
