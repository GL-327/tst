package com.jjkcraft.technique.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.entity.shikigami.ShikigamiEntity;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.registry.LegacyIds;

/**
 * Calls shikigami: {@code count} summons of {@code entity} around the caster for {@code lifetime}
 * ticks. Casting again dismisses the ones already out first. The entity must be a shikigami type.
 */
public record SummonBehavior(Identifier entity, int count, int lifetime) implements TechniqueBehavior {
	public static final MapCodec<SummonBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			LegacyIds.ENTITY_ID.fieldOf("entity").forGetter(SummonBehavior::entity),
			Codec.intRange(1, 8).optionalFieldOf("count", 1).forGetter(SummonBehavior::count),
			Codec.intRange(20, 12_000).optionalFieldOf("lifetime", 600).forGetter(SummonBehavior::lifetime)
	).apply(i, SummonBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.SUMMON;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entity);
		for (ShikigamiEntity old : ShikigamiEntity.ownedBy(ctx.level(), ShikigamiEntity.class, ctx.caster())) {
			if (old.getType() == type) {
				old.dissolve(ctx.level());
			}
		}

		Vec3 at = ctx.caster().position();
		for (int n = 0; n < count; n++) {
			Entity created = type.create(ctx.level(), EntitySpawnReason.MOB_SUMMONED);
			if (!(created instanceof ShikigamiEntity summon)) {
				return CastResult.miss(at);
			}

			double angle = ctx.caster().getYRot() * Math.PI / 180 + Math.PI / 2 + (n - (count - 1) / 2.0) * 0.9;
			Vec3 spot = at.add(Math.cos(angle) * 1.6, 0.1, Math.sin(angle) * 1.6);
			summon.snapTo(spot.x, spot.y, spot.z, ctx.caster().getYRot(), 0);
			summon.bind(ctx.caster(), lifetime, ctx.power());
			ctx.level().addFreshEntity(summon);
			ctx.fx(TechniqueFxPayload.Phase.BURST, spot);
		}

		return CastResult.done(at);
	}
}
