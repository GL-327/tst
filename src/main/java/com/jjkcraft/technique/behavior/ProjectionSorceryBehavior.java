package com.jjkcraft.technique.behavior;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Snap-speed: after a clearly shown windup (the technique's cast time), the caster takes a sliver
 * of time for themselves. Everyone hostile within {@code radius} loses the moment (they freeze for
 * {@code skip_ticks}) while the caster steps up to {@code blink} blocks forward and moves quickly.
 */
public record ProjectionSorceryBehavior(double radius, int skipTicks, double blink) implements TechniqueBehavior {
	public static final MapCodec<ProjectionSorceryBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 32).optionalFieldOf("radius", 8.0).forGetter(ProjectionSorceryBehavior::radius),
			Codec.intRange(1, 200).optionalFieldOf("skip_ticks", 14).forGetter(ProjectionSorceryBehavior::skipTicks),
			Codec.doubleRange(0, 16).optionalFieldOf("blink", 6.0).forGetter(ProjectionSorceryBehavior::blink)
	).apply(i, ProjectionSorceryBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.PROJECTION_SORCERY;
	}

	@Override
	public CastResult cast(CastContext ctx) { return com.jjkcraft.technique.ProjectionPlans.cast(ctx, Math.max(1,blink * ctx.power())); }
}
