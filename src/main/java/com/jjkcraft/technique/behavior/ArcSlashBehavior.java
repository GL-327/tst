package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * A wide arc slash in front of the caster. Damage grows with each target's current CE, so it hits
 * sorcerers and curses harder than ordinary mobs.
 *
 * @param damage    base damage
 * @param arc       arc width in degrees
 * @param ceScaling extra base damage per 100 CE the target holds
 * @param knockback knockback strength
 */
public record ArcSlashBehavior(double damage, double arc, double ceScaling, double knockback) implements TechniqueBehavior {
	public static final MapCodec<ArcSlashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(ArcSlashBehavior::damage),
			Codec.doubleRange(10, 360).optionalFieldOf("arc", 120.0).forGetter(ArcSlashBehavior::arc),
			Codec.doubleRange(0, 10).optionalFieldOf("ce_scaling", 0.5).forGetter(ArcSlashBehavior::ceScaling),
			Codec.doubleRange(0, 10).optionalFieldOf("knockback", 0.4).forGetter(ArcSlashBehavior::knockback)
	).apply(i, ArcSlashBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.ARC_SLASH;
	}

	@Override
	public CastResult cast(CastContext ctx) {
        for(int i=-2;i<=2;i++)com.jjkcraft.technique.TechniqueTerrain.line(ctx,ctx.eye(),ctx.eye().add(ctx.aim(i*arc/4,0).scale(ctx.range())),.8);
		List<LivingEntity> targets = ctx.enemiesInCone(ctx.range(), arc);
		for (LivingEntity target : targets) {
			double ce = CursedEnergy.isCapable(target) ? CursedEnergy.current(target) : 0;
			ctx.hurt(target, TechniqueMath.ceScaledDamage(damage, ceScaling, ce) * ctx.power(), false);
			ctx.knockback(target, ctx.caster().position(), knockback);
		}

		ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.caster().position().add(0, ctx.caster().getBbHeight() * 0.6, 0), ctx.look(), targets);
		return CastResult.hit(List.copyOf(targets), ctx.eye());
	}
}
