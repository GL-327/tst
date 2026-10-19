package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.StrikeHooks;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * A reinforced melee strike against the target in reach. With {@code black_flash} set, the strike
 * can become a Black Flash (rolled by {@link StrikeHooks}).
 *
 * @param damage     base damage
 * @param knockback  knockback strength
 * @param blackFlash whether this strike may become a Black Flash
 */
public record StrikeBehavior(double damage, double knockback, boolean blackFlash) implements TechniqueBehavior {
	public static final MapCodec<StrikeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(StrikeBehavior::damage),
			Codec.doubleRange(0, 10).optionalFieldOf("knockback", 0.5).forGetter(StrikeBehavior::knockback),
			Codec.BOOL.optionalFieldOf("black_flash", false).forGetter(StrikeBehavior::blackFlash)
	).apply(i, StrikeBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.STRIKE;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 4.0;
		Optional<LivingEntity> target = ctx.rayTarget(reach, 0.4);
		if (target.isEmpty()) {
			ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.blockLimit(reach));
			return CastResult.miss(ctx.blockLimit(reach));
		}

		LivingEntity hit = target.get();
		double amount = damage * ctx.power();
		StrikeHooks.Outcome outcome = StrikeHooks.resolve(ctx, hit, amount, blackFlash);
		if (outcome.handled()) {
			return CastResult.hit(List.of(hit), hit.getEyePosition());
		}

		ctx.hurt(hit, outcome.damage(), false);
		ctx.knockback(hit, ctx.caster().position(), knockback);
		ctx.fx(TechniqueFxPayload.Phase.HIT, hit.position().add(0, hit.getBbHeight() * 0.6, 0), ctx.look(), List.of(hit));
		return CastResult.hit(List.of(hit), hit.position());
	}
}
