package com.jjkcraft.entity.shikigami;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Toad (Ten Shadows): a slow guardian that keeps to its owner's side. It draws the
 * anger of enemies near its owner onto itself, and swats down projectiles flying at its owner.
 * Heavily armored, it never strikes back.
 */
public class ToadEntity extends ShikigamiEntity {
	public ToadEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.ARMOR, 12)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 16);
	}

	@Override
	protected void behave(ServerLevel level, LivingEntity owner, @Nullable LivingEntity target) {
		setTarget(null);
		if (distanceTo(owner) > 2.5 && tickCount % 10 == 0) {
			getNavigation().moveTo(owner, 1.1);
		}

		if (tickCount % 40 == 0) {
			// Taunt: enemies hunting its owner turn on the Toad instead.
			for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(10),
					m -> m.isAlive() && m.getTarget() == owner && !(m instanceof ShikigamiEntity))) {
				mob.setTarget(this);
			}

			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 2, 0.3, 0.1, 0.3, 0);
		}

		// Swat down projectiles about to reach its owner.
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(3.5), p -> p.isAlive())) {
			Entity shooter = projectile.getOwner();
			if (shooter == owner || shooter == this || (shooter != null && considersEntityAsAlly(shooter))) {
				continue;
			}

			Vec3 toOwner = owner.position().subtract(projectile.position());
			if (projectile.getDeltaMovement().dot(toOwner) > 0) {
				level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.NEUTRAL, 0.8f, 0.7f);
				projectile.discard();
				triggerAnim("action", "attack");
			}
		}
	}

	@Override
	protected int dissolveColor() {
		return 0x7A8A7A;
	}
}
