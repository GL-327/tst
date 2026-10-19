package com.jjkcraft.entity.shikigami;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.damage.JjkDamageTypes;

/**
 * Nue (Ten Shadows): a winged scout that circles above its owner. Every second it marks
 * enemies near its owner so they glow through walls; every few seconds it dives at the owner's
 * target, pecks and climbs back to its circle. It flies and ignores falls.
 */
public class NueEntity extends ShikigamiEntity {
	private int diveCooldown = 40;
	private boolean diving;
	private int diveTicks;
	private double angle;

	public NueEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setNoGravity(true);
		angle = level.getRandom().nextDouble() * Math.PI * 2;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
		return false;
	}

	@Override
	protected void behave(ServerLevel level, LivingEntity owner, @Nullable LivingEntity target) {
		if (tickCount % 20 == 0) {
			// Mark the enemies around its owner.
			for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(16),
					m -> m.isAlive() && m.getTarget() == owner && !(m instanceof ShikigamiEntity))) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
			}

			if (target != null) {
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
			}
		}

		if (diveCooldown > 0) {
			diveCooldown--;
		}

		Vec3 goal;
		if (diving && target != null) {
			// A dive is a straight stoop at the target, not a steered glide: it either lands or gives up.
			goal = target.position().add(0, target.getBbHeight() * 0.6, 0);
			diveTicks++;
			if (getBoundingBox().inflate(0.6).intersects(target.getBoundingBox()) || distanceTo(target) < 2.0) {
				target.hurtServer(level, level.damageSources().source(JjkDamageTypes.TECHNIQUE, this, getOwner()), (float) (3 * power));
				triggerAnim("action", "attack");
				diving = false;
				diveCooldown = 60;
			} else if (diveTicks > 60) {
				diving = false;
				diveCooldown = 20;
			} else {
				Vec3 stoop = goal.subtract(position());
				setDeltaMovement(stoop.normalize().scale(Math.min(0.7, stoop.length())));
				return;
			}
		} else {
			diving = false;
			if (target != null && diveCooldown <= 0) {
				diving = true;
				diveTicks = 0;
			}

			angle += 0.08;
			goal = owner.position().add(Math.cos(angle) * 3, 3.2 + Math.sin(tickCount * 0.1) * 0.3, Math.sin(angle) * 3);
		}

		Vec3 to = goal.subtract(position());
		double speed = diving ? 0.55 : 0.3;
		setDeltaMovement(getDeltaMovement().scale(0.6).add(to.lengthSqr() > 1e-4 ? to.normalize().scale(speed * 0.5) : Vec3.ZERO));
		if (to.horizontalDistanceSqr() > 1e-4) {
			setYRot((float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90f);
			yBodyRot = getYRot();
		}
	}

	@Override
	protected int dissolveColor() {
		return 0xE9E3D5;
	}
}
