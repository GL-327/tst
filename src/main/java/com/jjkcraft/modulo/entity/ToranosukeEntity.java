package com.jjkcraft.modulo.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Toranosuke, the tiger of mission 3: a Modulo boss on a four-legged GeckoLib model (original art). */
public class ToranosukeEntity extends ModuloBossEntity implements GeoEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public ToranosukeEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 3, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	/** As in chapter 3, a cub comes out of the bushes once the tiger is down. */
	@Override
	public void die(net.minecraft.world.damagesource.DamageSource source) {
		super.die(source);
		if (level() instanceof net.minecraft.server.level.ServerLevel level) {
			spawnAtLocation(level, new net.minecraft.world.item.ItemStack(com.jjkcraft.modulo.ModuloItems.TIGER_CUB));
		}
	}
}
