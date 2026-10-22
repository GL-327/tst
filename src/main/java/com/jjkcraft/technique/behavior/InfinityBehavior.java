package com.jjkcraft.technique.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.core.technique.InfinityMath;

/**
 * A passive field (toggle) that slows projectiles flying toward the caster. The caster's own
 * projectiles are not affected.
 *
 * @param radius   field radius
 * @param slow     velocity multiplier applied each tick to incoming projectiles
 * @param minSpeed legacy datapack parameter; inward motion can now stop completely
 */
public record InfinityBehavior(double radius, double slow, double minSpeed, int filter) implements TechniqueBehavior {
    private record Held(Projectile projectile,boolean gravity,Vec3 velocity) {}
    @SuppressWarnings("unchecked") private java.util.Map<Integer,Held> held(CastContext c){return (java.util.Map<Integer,Held>)c.memory.computeIfAbsent("infinity_held",key->new java.util.LinkedHashMap<Integer,Held>());}
    private static void release(Held h){if(!h.projectile.isRemoved()){h.projectile.setNoGravity(h.gravity);h.projectile.setDeltaMovement(h.velocity);h.projectile.needsSync=true;}}
	public InfinityBehavior(double radius,double slow,double minSpeed){this(radius,slow,minSpeed,0);}
	public static final MapCodec<InfinityBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(1, 32).fieldOf("radius").forGetter(InfinityBehavior::radius),
			Codec.doubleRange(0, 1).fieldOf("slow").forGetter(InfinityBehavior::slow),
			Codec.doubleRange(0, 4).optionalFieldOf("min_speed", 0.05).forGetter(InfinityBehavior::minSpeed),
			Codec.intRange(0,2).optionalFieldOf("filter",0).forGetter(InfinityBehavior::filter)
	).apply(i, InfinityBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.INFINITY;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, ctx.caster().position(), new Vec3(radius, 0, 0), List.of());
		return CastResult.done(ctx.caster().position());
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		Vec3 center = ctx.caster().position().add(0, ctx.caster().getBbHeight() / 2, 0);
		AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        var held=held(ctx);
        held.values().removeIf(h->{if(h.projectile.isRemoved())return true;if(h.projectile.level()!=ctx.level()||h.projectile.position().distanceTo(center)>radius){release(h);return true;}return false;});
		if(filter!=1)for (Projectile projectile : ctx.level().getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() != ctx.caster())) {
			if(held.containsKey(projectile.getId())){projectile.setDeltaMovement(Vec3.ZERO);continue;}
			Vec3 velocity = projectile.getDeltaMovement();
			Vec3 away = projectile.position().subtract(center);double distance=away.length();
            if(distance>radius||distance<.001)continue;
            var normal=away.scale(1/distance);double inward=-velocity.dot(normal);
            if(inward<=0)continue;
            double contact=ctx.caster().getBbWidth()*.5+.65;
            double speed=InfinityMath.inwardSpeed(inward,distance,contact,radius,slow);
            projectile.setDeltaMovement(velocity.add(normal.scale(inward-speed)));
            if(speed<.025&&held.size()<256){held.put(projectile.getId(),new Held(projectile,projectile.isNoGravity(),velocity));projectile.setNoGravity(true);projectile.setDeltaMovement(Vec3.ZERO);}
			projectile.needsSync = true;
		}
		if(filter!=0)for(var enemy:ctx.enemiesInRadius(center,radius)) {
			Vec3 away=enemy.position().subtract(ctx.caster().position()).multiply(1,0,1);
			if(away.lengthSqr()<0.001)away=new Vec3(0,0,1);
			Vec3 velocity=enemy.getDeltaMovement();
            double distance=away.length(),contact=(enemy.getBbWidth()+ctx.caster().getBbWidth())*.5+.35;
            Vec3 normal=away.normalize();double inward=-velocity.dot(normal);
            if(inward>0)velocity=velocity.add(normal.scale(inward-InfinityMath.inwardSpeed(inward,distance,contact,radius,slow)));
            double push=InfinityMath.contactPush(distance,contact);
            if(push>0&&velocity.dot(normal)<push)velocity=velocity.add(normal.scale(push-velocity.dot(normal)));
			enemy.setDeltaMovement(velocity);
			enemy.needsSync=true;
		}

		return true;
	}

	@Override
	public void end(CastContext ctx) {
        held(ctx).values().forEach(InfinityBehavior::release);held(ctx).clear();
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_END, ctx.caster().position());
	}
}
