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
		int mode=ctx.details().expression().dash();
		Vec3 direction=switch(mode){case 1->ctx.look().scale(-1);case 2->ctx.look().yRot((float)(Math.PI/2));case 3->ctx.look().yRot((float)(-Math.PI/2));case 4->new Vec3(0,1,0);case 5->new Vec3(0,-1,0);default->ctx.look();};
		if(mode<4)direction=direction.multiply(1,0,1);direction=direction.normalize();
		var ray=ctx.level().clip(new net.minecraft.world.level.ClipContext(ctx.eye(),ctx.eye().add(direction.scale(distance)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
		double length=ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS?distance:Math.max(0,ray.getLocation().distanceTo(ctx.eye())-0.5);
		Vec3 step=direction.scale(length);
		if (step.length() < 0.8) {
			return CastResult.miss(from);
		}

		Vec3 safe=Vec3.ZERO;
		for(double n=0.25;n<=step.length();n+=0.25){Vec3 candidate=direction.scale(n);if(!ctx.level().noCollision(caster,caster.getBoundingBox().move(candidate)))break;safe=candidate;}
		if(safe.length()<0.5)return CastResult.miss(from);
		Vec3 to = from.add(safe);
		// Eye-ray clearance alone does not guarantee that the feet/body fit at the destination.
		Vec3 delta=to.subtract(from);
		while(delta.length()>0.5 && !ctx.level().noCollision(caster,caster.getBoundingBox().move(delta)))delta=delta.subtract(delta.normalize().scale(0.25));
		if(delta.length()<=0.5)return CastResult.miss(from);
		to=from.add(delta);
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
