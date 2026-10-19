package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/** A step of ink: the caster slips up to {@code distance} blocks along their facing, untouchable for a moment. */
public record DashBehavior(double distance, int invulnerableTicks) implements TechniqueBehavior {
	public static final MapCodec<DashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 24).optionalFieldOf("distance", 6.0).forGetter(DashBehavior::distance),
			Codec.intRange(0, 40).optionalFieldOf("invulnerable_ticks", 6).forGetter(DashBehavior::invulnerableTicks)
	).apply(i, DashBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.DASH;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		LivingEntity caster = ctx.caster();
		Vec3 from = caster.position();
		Vec3 limit = ctx.blockLimit(distance);
		Vec3 step = new Vec3(limit.x - from.x, 0, limit.z - from.z);
		if (step.length() < 0.8) {
			return CastResult.miss(from);
		}

		Vec3 to = from.add(step.normalize().scale(step.length() - 0.5));
		if (caster instanceof ServerPlayer player) {
			player.teleportTo(to.x, to.y, to.z);
		} else {
			caster.teleportTo(to.x, to.y, to.z);
		}

		caster.setInvulnerableTime(invulnerableTicks);
		ctx.fx(TechniqueFxPayload.Phase.CAST, from.add(0, 1, 0), to.subtract(from), List.of());
		return CastResult.done(to);
	}
}
