package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.technique.field.SpatialFields;

/**
 * Opens an attract (positive strength) or repel (negative strength) field at the aimed point.
 * An attract field and a repel field that overlap collide into a burst.
 *
 * @param strength        pull per tick; negative pushes
 * @param radius          field radius
 * @param duration        ticks the field lasts
 * @param damagePerSecond damage to enemies inside
 * @param burstDamage     damage of the collision burst
 * @param burstRadius     radius of the collision burst
 */
public record FieldBehavior(double strength, double radius, int duration, double damagePerSecond, double burstDamage, double burstRadius)
		implements TechniqueBehavior {
	public static final MapCodec<FieldBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(-10, 10).fieldOf("strength").forGetter(FieldBehavior::strength),
			Codec.doubleRange(0.5, 32).fieldOf("radius").forGetter(FieldBehavior::radius),
			Codec.intRange(1, 1200).fieldOf("duration").forGetter(FieldBehavior::duration),
			Codec.doubleRange(0, 1000).optionalFieldOf("damage_per_second", 0.0).forGetter(FieldBehavior::damagePerSecond),
			Codec.doubleRange(0, 10_000).optionalFieldOf("burst_damage", 20.0).forGetter(FieldBehavior::burstDamage),
			Codec.doubleRange(0, 32).optionalFieldOf("burst_radius", 5.0).forGetter(FieldBehavior::burstRadius)
	).apply(i, FieldBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.FIELD;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		Optional<LivingEntity> target = ctx.rayTarget(ctx.range(), 0.5);
		Vec3 center = target.map(t -> t.position().add(0, t.getBbHeight() / 2, 0)).orElseGet(() -> ctx.blockLimit(ctx.range()));
		if(ctx.details().center()==1)center=ctx.caster().position().add(0,1,0);
		SpatialFields.place(ctx, center, radius, strength, damagePerSecond, burstDamage, burstRadius, duration);
		ctx.fx(TechniqueFxPayload.Phase.FIELD, center, new Vec3(radius, duration, strength), target.map(List::of).orElse(List.of()));
		return target.isPresent() ? CastResult.hit(List.of(target.get()), center) : CastResult.done(center);
	}
}
