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
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.technique.TechniqueBehaviorType;

/**
 * A thin, straight cut along the look ray that hits everything on the line. Part of its damage
 * ignores armor.
 *
 * @param damage   base damage
 * @param width    half-width of the line in blocks
 * @param pierce   share of the damage that ignores armor, 0 to 1
 */
public record LineCutBehavior(double damage, double width, double pierce) implements TechniqueBehavior {
	public static final MapCodec<LineCutBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(LineCutBehavior::damage),
			Codec.doubleRange(0, 4).optionalFieldOf("width", 0.3).forGetter(LineCutBehavior::width),
			Codec.doubleRange(0, 1).optionalFieldOf("pierce", 0.4).forGetter(LineCutBehavior::pierce)
	).apply(i, LineCutBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.LINE_CUT;
	}

	@Override
	public CastResult cast(CastContext ctx) {
        com.jjkcraft.technique.TechniqueTerrain.line(ctx,ctx.eye(),ctx.eye().add(ctx.look().scale(ctx.range())),width);
		List<LivingEntity> targets = ctx.enemiesOnLine(ctx.range(), width);
		for (LivingEntity target : targets) {
			ctx.hurtSplit(target, damage * ctx.power(), pierce);
		}

		Vec3 end = ctx.blockLimit(ctx.range());
		ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.eye(), end.subtract(ctx.eye()), targets);
		return CastResult.hit(List.copyOf(targets), end);
	}
}
