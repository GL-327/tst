package com.jjkcraft.technique.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.phys.Vec3;

import com.jjkcraft.entity.CursedBoltEntity;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Fires a cursed energy bolt.
 *
 * @param damage         damage on hit (and at the center of a blast)
 * @param speed          blocks per tick
 * @param homing         whether the bolt steers toward enemies ahead of it
 * @param homingStrength how hard it steers, 0 to 1 per tick
 * @param blastRadius    explode on impact with this radius (0: no blast)
 * @param pierce         pass through entities instead of stopping
 * @param size           visual size and hit box scale
 * @param lifetime       ticks before it fizzles
 * @param gravity        downward acceleration per tick
 */
public record ProjectileBehavior(double damage, double speed, boolean homing, double homingStrength, double blastRadius,
		boolean pierce, float size, int lifetime, double gravity, int count, double spread, int burnSeconds) implements TechniqueBehavior {
	public ProjectileBehavior(double damage,double speed,boolean homing,double homingStrength,double blastRadius,boolean pierce,float size,int lifetime,double gravity,int count,double spread){this(damage,speed,homing,homingStrength,blastRadius,pierce,size,lifetime,gravity,count,spread,0);}
	public static final MapCodec<ProjectileBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 10_000).fieldOf("damage").forGetter(ProjectileBehavior::damage),
			Codec.doubleRange(0.05, 8).fieldOf("speed").forGetter(ProjectileBehavior::speed),
			Codec.BOOL.optionalFieldOf("homing", false).forGetter(ProjectileBehavior::homing),
			Codec.doubleRange(0, 1).optionalFieldOf("homing_strength", 0.15).forGetter(ProjectileBehavior::homingStrength),
			Codec.doubleRange(0, 32).optionalFieldOf("blast_radius", 0.0).forGetter(ProjectileBehavior::blastRadius),
			Codec.BOOL.optionalFieldOf("pierce", false).forGetter(ProjectileBehavior::pierce),
			Codec.floatRange(0.05f, 8f).optionalFieldOf("size", 0.35f).forGetter(ProjectileBehavior::size),
			Codec.intRange(1, 1200).optionalFieldOf("lifetime", 60).forGetter(ProjectileBehavior::lifetime),
			Codec.doubleRange(0, 1).optionalFieldOf("gravity", 0.0).forGetter(ProjectileBehavior::gravity),
			Codec.intRange(1, 12).optionalFieldOf("count", 1).forGetter(ProjectileBehavior::count),
			Codec.doubleRange(0, 90).optionalFieldOf("spread", 0.0).forGetter(ProjectileBehavior::spread),
			Codec.intRange(0,20).optionalFieldOf("burn_seconds",0).forGetter(ProjectileBehavior::burnSeconds)
	).apply(i, ProjectileBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.PROJECTILE;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		Vec3 look = ctx.look();
		Vec3 start = ctx.eye().add(look.scale(0.6)).add(0, -0.15, 0);
		for (int n = 0; n < count; n++) {
			// A volley fans out evenly across the spread, centred on the aim.
			double yaw = count == 1 ? 0 : Math.toRadians((n - (count - 1) / 2.0) * spread / Math.max(1, count - 1) * 2);
			Vec3 direction = look.yRot((float) yaw);
			direction=switch(ctx.details().expression().pattern()){
				case 1->ctx.aim(n*360.0/count,0);case 2->ctx.aim(0,Math.toDegrees(yaw));
				case 3->n%2==0?ctx.aim(Math.toDegrees(yaw),0):ctx.aim(0,Math.toDegrees(yaw));default->direction;};
			CursedBoltEntity bolt = new CursedBoltEntity(JjkEntities.CURSED_BOLT, ctx.level());
			bolt.setPos(start.x, start.y, start.z);
			bolt.configure(ctx, damage, homing, homingStrength, blastRadius, pierce, lifetime, gravity, size);
			bolt.burnSeconds(burnSeconds);
			bolt.setDeltaMovement(direction.scale(speed));
			ctx.level().addFreshEntity(bolt);
		}

		ctx.fx(TechniqueFxPayload.Phase.CAST, start);
		// A fired bolt counts as landed for Control only when it hits; the bolt credits the hit itself.
		return CastResult.miss(start);
	}
}
