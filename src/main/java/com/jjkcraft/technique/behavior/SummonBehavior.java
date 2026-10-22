package com.jjkcraft.technique.behavior;

import java.util.List;

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
		if(ctx.caster() instanceof net.minecraft.world.entity.player.Player p&&!com.jjkcraft.entity.shikigami.TenShadows.canSummon(p,entity))return CastResult.miss(ctx.eye());
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entity);
		var owned=ShikigamiEntity.ownedBy(ctx.level(),ShikigamiEntity.class,ctx.caster());
		long retained=owned.stream().filter(e->e.getType()!=type||type==com.jjkcraft.entity.JjkEntities.TRANSFIGURED_SOUL).count();
		if(retained+count>12)return CastResult.miss(ctx.caster().position());
		boolean souls=type==com.jjkcraft.entity.JjkEntities.TRANSFIGURED_SOUL;
		if(souls&&ShikigamiEntity.ownedBy(ctx.level(),com.jjkcraft.entity.shikigami.TransfiguredSoulEntity.class,ctx.caster()).size()+count>6)return CastResult.miss(ctx.caster().position());
		for (ShikigamiEntity old : ShikigamiEntity.ownedBy(ctx.level(), ShikigamiEntity.class, ctx.caster())) {
			if (old.getType() == type && !souls) {
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
			var expression=ctx.details().expression();
			var health=summon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
			if(health!=null){health.setBaseValue(health.getBaseValue()*expression.vitality()/100.0);summon.setHealth(summon.getMaxHealth());}
			for(var attribute:List.of(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED)){
				var speed=summon.getAttribute(attribute);if(speed!=null)speed.setBaseValue(speed.getBaseValue()*expression.mobility()/100.0);
			}
			ctx.level().addFreshEntity(summon);
			ctx.fx(TechniqueFxPayload.Phase.BURST, spot, new Vec3(1, 0, 0), List.of(summon));
		}

		return CastResult.done(at);
	}
}
