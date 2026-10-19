package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.vow.Vows;

/**
 * A strike sworn on a vow: it cuts {@code per_point} harder for every point of the caster's active
 * Binding Vow, and only half as hard without one.
 */
public record VowStrikeBehavior(double damage, double perPoint, double knockback) implements TechniqueBehavior {
	public static final MapCodec<VowStrikeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(VowStrikeBehavior::damage),
			Codec.doubleRange(0, 2).optionalFieldOf("per_point", 0.1).forGetter(VowStrikeBehavior::perPoint),
			Codec.doubleRange(0, 10).optionalFieldOf("knockback", 0.6).forGetter(VowStrikeBehavior::knockback)
	).apply(i, VowStrikeBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.VOW_STRIKE;
	}

	/** The multiplier a caster's vow gives right now. */
	public double vowFactor(LivingEntity caster) {
		if (caster instanceof ServerPlayer player && Vows.state(player).active()) {
			return 1 + perPoint * Vows.state(player).points();
		}

		return 0.5;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 4.5;
		Optional<LivingEntity> target = ctx.rayTarget(reach, 0.4);
		if (target.isEmpty()) {
			ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.blockLimit(reach));
			return CastResult.miss(ctx.blockLimit(reach));
		}

		double factor = vowFactor(ctx.caster());
		if (factor < 1 && ctx.caster() instanceof ServerPlayer player) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.oath_unsworn"), true);
		}

		LivingEntity hit = target.get();
		ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.caster().position().add(0, 1.1, 0).add(ctx.look()), ctx.look(), List.of());
		ctx.hurt(hit, damage * ctx.power() * factor, false);
		ctx.knockback(hit, ctx.caster().position(), knockback);
		ctx.fx(TechniqueFxPayload.Phase.HIT, hit.position().add(0, hit.getBbHeight() * 0.6, 0), ctx.look(), List.of(hit));
		return CastResult.hit(List.of(hit), hit.position());
	}
}
