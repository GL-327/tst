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
	public CastResult cast(CastContext ctx) {
		LivingEntity caster = ctx.caster();
		Vec3 from = caster.position();
		List<Entity> frozen = new ArrayList<>();
		for (LivingEntity enemy : ctx.enemiesInRadius(from, radius)) {
			enemy.addEffect(new MobEffectInstance(JjkEffects.FROZEN_FRAME, (int) Math.round(skipTicks * Math.min(1.5, ctx.power())), 0, false, true), caster);
			frozen.add(enemy);
		}

		Vec3 look = new Vec3(ctx.look().x, 0, ctx.look().z);
		if (blink > 0 && look.lengthSqr() > 1e-4) {
			Vec3 limit = ctx.blockLimit(blink);
			Vec3 to = new Vec3(limit.x, from.y, limit.z);
			Vec3 step = to.subtract(from);
			if (step.length() > 0.8) {
				to = from.add(step.normalize().scale(step.length() - 0.6));
			}

			if (caster instanceof ServerPlayer player) {
				if(!ctx.level().noCollision(caster,caster.getBoundingBox().move(to.subtract(from))))to=from;
				player.teleportTo(to.x, to.y, to.z);
			} else {
				caster.teleportTo(to.x, to.y, to.z);
			}
		}

		caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false));
		ctx.fx(TechniqueFxPayload.Phase.BURST, from.add(0, 1, 0), new Vec3(radius * 0.5, 0, 0), frozen);
		return frozen.isEmpty() ? CastResult.done(caster.position()) : CastResult.hit(frozen, caster.position());
	}
}
