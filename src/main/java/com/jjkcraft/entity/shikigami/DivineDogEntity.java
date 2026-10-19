package com.jjkcraft.entity.shikigami;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
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
 * Ink Hound (Kennel Call): hunts in pairs. A hound circles its owner's target to flank it, then
 * lunges and bites, then circles again. With no fight on, it heels beside its owner.
 */
public class DivineDogEntity extends ShikigamiEntity {
	private int circle;
	private int lungeCooldown;
	private double angle;

	public DivineDogEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		angle = level.getRandom().nextDouble() * Math.PI * 2;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, 0.38)
				.add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 24);
	}

	@Override
	protected void behave(ServerLevel level, LivingEntity owner, @Nullable LivingEntity target) {
		if (target == null) {
			if (distanceTo(owner) > 4 && tickCount % 10 == 0) {
				getNavigation().moveTo(owner, 1.2);
			}

			return;
		}

		if (lungeCooldown > 0) {
			lungeCooldown--;
		}

		if (circle > 0) {
			// Flank: run round the target at three blocks.
			circle--;
			angle += 0.12;
			Vec3 spot = target.position().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
			if (tickCount % 5 == 0) {
				getNavigation().moveTo(spot.x, spot.y, spot.z, 1.3);
			}

			return;
		}

		getNavigation().moveTo(target, 1.5);
		if (distanceTo(target) < 1.8 && lungeCooldown <= 0) {
			lookAt(target, 60, 60);
			target.hurtServer(level, level.damageSources().source(JjkDamageTypes.TECHNIQUE, this, getOwner()), (float) (4 * power));
			setDeltaMovement(getDeltaMovement().add(target.position().subtract(position()).normalize().scale(0.4).add(0, 0.2, 0)));
			triggerAnim("action", "attack");
			lungeCooldown = 20;
			circle = 25 + getRandom().nextInt(20);
		}
	}

	@Override
	protected int dissolveColor() {
		return 0x4A3A6A;
	}
}
