package com.jjkcraft.culling.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import com.jjkcraft.culling.CullingGame;

/**
 * The Kogane: the Culling Game's game master. A paper-mask lantern that floats at each
 * colony's centre, keeps the tally and hears proposals for new rules. It can't be hurt or moved.
 * Not saved: the game raises a new one whenever a colony's centre is loaded without one.
 */
public class KoganeEntity extends Mob implements GeoEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private int colony = -1;

	public KoganeEntity(EntityType<? extends Mob> type, Level level) {
		super(type, level);
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
	}

	public void setColony(int colony) {
		this.colony = colony;
	}

	public int colony() {
		return colony;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12, 1f));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			CullingGame.openNegotiation(serverPlayer, this);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(getDeltaMovement().scale(0.5));
		if (!level().isClientSide() && tickCount % 40 == 0 && colony < 0) {
			// A stray with no colony (from a reload): it has no game to keep.
			discard();
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 3, test -> test.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}
}
