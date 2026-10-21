package com.jjkcraft.entity.shikigami;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import com.jjkcraft.particle.JjkParticles;

/**
 * A shikigami: a summon bound to the sorcerer who called it. It fights whoever its owner fights,
 * never its owner or the owner's other summons, and comes undone when its time runs out, when the
 * owner is gone or too far, or when it is beaten. Not saved: summons don't outlive a session.
 */
public abstract class ShikigamiEntity extends PathfinderMob implements GeoEntity, OwnableEntity {
	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> owner;
	private int lifetime = 600;
	protected double power = 1.0;

	protected ShikigamiEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	/** Binds the summon to its caller. {@code power} scales its health and damage. */
	public void bind(LivingEntity owner, int lifetime, double power) {
		this.owner = EntityReference.of(owner);
		this.lifetime = lifetime;
		this.power = power;
		var health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(health.getBaseValue() * Math.max(0.5, power));
			setHealth(getMaxHealth());
		}
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return owner;
	}

	public int lifetime() {
		return lifetime;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		LivingEntity mine = getOwner();
		if (mine != null && (other == mine || (other instanceof OwnableEntity ownable && ownable.getOwner() == mine))) {
			return true;
		}

		return super.considersEntityAsAlly(other);
	}

	/** Whom the owner is fighting: their target, whoever they last hit, or whoever last hit them. */
	protected @Nullable LivingEntity ownerTarget() {
		LivingEntity mine = getOwner();
		if (mine == null) {
			return null;
		}

		LivingEntity candidate = mine instanceof Mob mob ? mob.getTarget() : null;
		if (candidate == null && mine.getLastHurtMob() != null && mine.tickCount - mine.getLastHurtMobTimestamp() < 200) {
			candidate = mine.getLastHurtMob();
		}

		if (candidate == null && mine.getLastHurtByMob() != null) {
			candidate = mine.getLastHurtByMob();
		}

		return candidate != null && candidate.isAlive() && candidate != this && !considersEntityAsAlly(candidate)
				&& candidate.distanceTo(this) < 24 ? candidate : null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity mine = getOwner();
		if (--lifetime <= 0 || mine == null || !mine.isAlive() || mine.level() != level() || mine.distanceTo(this) > 48) {
			dissolve(level);
			return;
		}

		LivingEntity target = ownerTarget();
		if (target != getTarget()) {
			setTarget(target);
		}

		behave(level, mine, target);
	}

	/** The summon's own AI, every tick. */
	protected abstract void behave(ServerLevel level, LivingEntity owner, @Nullable LivingEntity target);

	/** Comes undone in a puff of its colour. */
	public void dissolve(ServerLevel level) {
		level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, dissolveColor(), 1.2f), getX(), getY() + getBbHeight() / 2, getZ(), 20,
				getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 0.04);
		discard();
	}

	protected abstract int dissolveColor();

	/** Every summon of this type the owner has out. */
	public static <T extends ShikigamiEntity> List<T> ownedBy(ServerLevel level, Class<T> type, LivingEntity owner) {
		return level.getEntitiesOfClass(type, owner.getBoundingBox().inflate(64), s -> s.getOwner() == owner);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public void die(net.minecraft.world.damagesource.DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			dissolve(level);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 3, test -> test.setAndContinue(test.isMoving() ? getDeltaMovement().horizontalDistanceSqr()>.025?RawAnimation.begin().thenLoop("run"):WALK : IDLE)));
		controllers.add(new AnimationController<>("action", 2, test -> PlayState.STOP)
				.triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}
}
