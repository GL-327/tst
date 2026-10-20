package com.jjkcraft.technique.field;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;

/**
 * Attract and repel fields ({@code field} behavior). A field pulls (positive strength) or pushes
 * (negative strength) enemies within its radius every tick. When an attracting field and a
 * repelling field overlap they collide: both vanish and a burst explodes between them, with the
 * larger of their burst damages. Fields are runtime-only and are not saved.
 */
public final class SpatialFields {
	public static final class Field {
		final CastContext ctx;
		final Vec3 center;
		final double radius;
		final double strength;
		final double damagePerTick;
		final double burstDamage;
		final double burstRadius;
		final long expiresAt;
		boolean removed;

		Field(CastContext ctx, Vec3 center, double radius, double strength, double damagePerSecond, double burstDamage, double burstRadius, int duration) {
			this.ctx = ctx;
			this.center = center;
			this.radius = radius;
			this.strength = strength;
			this.damagePerTick = damagePerSecond / 20.0;
			this.burstDamage = burstDamage;
			this.burstRadius = burstRadius;
			this.expiresAt = ctx.now() + duration;
		}

		public Vec3 center() {
			return center;
		}
        public LivingEntity caster(){return ctx.caster();}

		public boolean attracts() {
			return strength > 0;
		}

		public Identifier technique() {
			return ctx.techniqueId();
		}
	}

	private static final Map<Level, List<Field>> FIELDS = new IdentityHashMap<>();

	private SpatialFields() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(SpatialFields::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FIELDS.clear());
	}

	/** Places a field, resolving a collision with an opposite field right away. */
	public static Field place(CastContext ctx, Vec3 center, double radius, double strength, double damagePerSecond,
			double burstDamage, double burstRadius, int duration) {
		Field field = new Field(ctx, center, radius, strength, damagePerSecond, burstDamage, burstRadius, duration);
		List<Field> list = FIELDS.computeIfAbsent(ctx.level(), l -> new ArrayList<>());
		list.add(field);
        com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,center,Math.min(radius,2.5+Math.sqrt(ctx.power())*2),24,strength>0);
		resolveCollisions(ctx.level(), list);
		return field;
	}

	public static List<Field> active(ServerLevel level) {
		return FIELDS.getOrDefault(level, List.of()).stream().filter(f -> !f.removed).toList();
	}

	private static void tick(ServerLevel level) {
		List<Field> list = FIELDS.get(level);
		if (list == null || list.isEmpty()) {
			return;
		}

		long now = level.getGameTime();
		resolveCollisions(level, list);
		for (Iterator<Field> it = list.iterator(); it.hasNext(); ) {
			Field field = it.next();
			if (field.removed || now >= field.expiresAt) {
				it.remove();
				continue;
			}

            if(now%5==0)com.jjkcraft.technique.TechniqueTerrain.sphere(field.ctx,field.center,Math.min(field.radius,2.5+Math.sqrt(field.ctx.power())*2),12,field.attracts());
			for (LivingEntity target : field.ctx.enemiesInRadius(field.center, field.radius)) {
				Vec3 toCenter = field.center.subtract(target.position().add(0, target.getBbHeight() / 2, 0));
				double distance = Math.max(0.5, toCenter.length());
				// Pull harder near the edge so targets gather at the center instead of orbiting it.
				double pull = field.strength * 0.08 * Math.min(1.5, distance / field.radius + 0.3);
				target.push(toCenter.normalize().scale(pull));
				target.needsSync = true;
				int pulse=field.ctx.details().expression().pulse()==0?10:field.ctx.details().expression().pulse();
				if (field.damagePerTick > 0 && now % pulse == 0) {
					target.damageCooldownTime=0;
					field.ctx.hurt(target, field.damagePerTick * pulse * field.ctx.power()*field.ctx.details().expression().falloff(distance,field.radius), false);
				}
			}
		}
	}

	private static void resolveCollisions(ServerLevel level, List<Field> list) {
		for (Field a : list) {
			if (a.removed || !a.attracts()) {
				continue;
			}

			for (Field b : list) {
				if (b.removed || b.attracts() || a == b) {
					continue;
				}

				if (a.center.distanceTo(b.center) <= a.radius + b.radius) {
					burst(level, a, b);
				}
			}
		}
	}

	private static void burst(ServerLevel level, Field attract, Field repel) {
		attract.removed = true;
		repel.removed = true;
		Vec3 at = attract.center.add(repel.center).scale(0.5);
		Field stronger = attract.burstDamage >= repel.burstDamage ? attract : repel;
		double radius = Math.max(attract.burstRadius, repel.burstRadius);
		CastContext ctx = stronger.ctx;
        com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,at,radius,64,false);
		List<LivingEntity> hit = ctx.enemiesInRadius(at, radius);
		for (LivingEntity target : hit) {
			double falloff = 1 - 0.5 * Math.min(1, target.position().distanceTo(at) / radius);
			ctx.hurt(target, stronger.burstDamage * ctx.power() * falloff, false);
			ctx.knockback(target, at, 1.6);
		}

		ctx.fx(TechniqueFxPayload.Phase.BURST, at, new Vec3(radius, 0, 0), hit);
	}
}
