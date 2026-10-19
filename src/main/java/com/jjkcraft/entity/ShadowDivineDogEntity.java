package com.jjkcraft.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.domain.DomainSummon;
import com.jjkcraft.particle.JjkParticles;

/**
 * A shadow hound from a shadow-garden domain. It hunts the domain's enemies inside the barrier,
 * never leaves it, and melts away when the domain ends. It is never saved and drops nothing.
 */
public class ShadowDivineDogEntity extends Monster implements GeoEntity, DomainSummon {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private UUID summoner;
	private int domainInstance = -1;

	public ShadowDivineDogEntity(EntityType<? extends ShadowDivineDogEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 14)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.ATTACK_DAMAGE, 4)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	public void bind(LivingEntity owner, int instance) {
		this.summoner = owner.getUUID();
		this.domainInstance = instance;
	}

	@Override
	public UUID summonerId() {
		return summoner == null ? new UUID(0, 0) : summoner;
	}

	@Override
	public int domainInstance() {
		return domainInstance;
	}

	private Optional<ActiveDomain> domain() {
		if (summoner == null) {
			return Optional.empty();
		}

		return DomainService.all().stream()
				.filter(d -> d.id() == domainInstance && d.caster().getUUID().equals(summoner) && d.phase() != ActiveDomain.Phase.CLOSING)
				.findFirst();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HuntInDomainGoal(this));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		Optional<ActiveDomain> domain = domain();
		if (domain.isEmpty()) {
			dissolve(level);
			return;
		}

		// Bound to the garden: a hound that strays is pulled back inside.
		ActiveDomain d = domain.get();
		if (!d.contains(this)) {
			var back = d.center().add(position().subtract(d.center()).normalize().scale(d.radius() * 0.7));
			setPos(back.x, Math.max(back.y, getY()), back.z);
		}
	}

	public void dissolve(ServerLevel level) {
		level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, 0x1A1424, 1.2f), getX(), getY() + 0.4, getZ(), 16, 0.3, 0.3, 0.3, 0.03);
		discard();
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		if (summoner != null && (summoner.equals(other.getUUID()) || other instanceof DomainSummon s && summoner.equals(s.summonerId()))) {
			return true;
		}

		return super.considersEntityAsAlly(other);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 3, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
		controllers.add(new AnimationController<>("action", 2, test -> com.geckolib.animation.object.PlayState.STOP)
				.triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		triggerAnim("action", "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}

	/** Targets the nearest enemy of the domain that is inside it. */
	static final class HuntInDomainGoal extends Goal {
		private final ShadowDivineDogEntity hound;

		HuntInDomainGoal(ShadowDivineDogEntity hound) {
			this.hound = hound;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			return hound.tickCount % 10 == 0 && hound.domain().isPresent();
		}

		@Override
		public void start() {
			hound.domain().flatMap(d -> d.targets().stream()
					.filter(e -> !(e instanceof DomainSummon))
					.min(Comparator.comparingDouble(hound::distanceToSqr)))
					.ifPresentOrElse(hound::setTarget, () -> hound.setTarget(null));
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}
	}
}
