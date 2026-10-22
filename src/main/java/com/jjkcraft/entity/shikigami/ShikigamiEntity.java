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
	private boolean ritual, soloRitual=true;
	private @Nullable LivingEntity ritualist;
	public boolean isRitual(){return ritual;}
	public void beginRitual(net.minecraft.server.level.ServerPlayer player,int ticks){
		bind(player,ticks,1);ritual=true;ritualist=player;
		var hp=getAttribute(Attributes.MAX_HEALTH);if(hp!=null){hp.setBaseValue(Math.max(20,hp.getBaseValue()));setHealth(getMaxHealth());}
		setCustomName(net.minecraft.network.chat.Component.literal("Subjugation · ").append(getType().getDescription()));setCustomNameVisible(true);
	}
	protected double power = 1.0;

	protected ShikigamiEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	/** Binds the summon to its caller. {@code power} scales its health and damage. */
	public void bind(LivingEntity owner, int lifetime, double power) {
		this.owner = EntityReference.of(owner);
		this.lifetime = lifetime;
		this.power = power;
        var attack=getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(attack.getBaseValue()*Math.max(.5,power)*1.75);
		var health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(health.getBaseValue() * Math.max(0.5, power) * 1.25);
			setHealth(getMaxHealth());
		}
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		// A hostile trial must not appear as the caller's pet to combat/technique targeting.
		return ritual?null:owner;
	}

	public int lifetime() {
		return lifetime;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		if(ritual)return other==this;
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
		LivingEntity mine = ritual?ritualist:getOwner();
		if (--lifetime <= 0 || mine == null || !mine.isAlive() || mine.level() != level() || mine.distanceTo(this) > 48) {
			dissolve(level);
			return;
		}

		LivingEntity target = ownerTarget();
		if(ritual){
			setTarget(mine);getLookControl().setLookAt(mine,60,60);
			if(isNoGravity())setDeltaMovement(mine.getEyePosition().subtract(getEyePosition()).normalize().scale(.22));
			else if(tickCount%5==0)getNavigation().moveTo(mine,1.25);
			if(tickCount%25==0&&distanceTo(mine)<Math.max(2.2,getBbWidth()+.7)&&hasLineOfSight(mine)){
				var attack=getAttribute(Attributes.ATTACK_DAMAGE);float damage=attack==null?3:(float)Math.max(2,attack.getValue());
				mine.hurtServer(level,level.damageSources().mobAttack(this),damage);triggerAnim("action","attack");
			}
			return;
		}
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
		return level.getEntitiesOfClass(type, owner.getBoundingBox().inflate(64), s -> s.getOwner() == owner&&!s.isRitual());
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
		if(ritual&&ritualist instanceof net.minecraft.server.level.ServerPlayer p){
			Entity killer=source.getEntity();boolean yours=killer==p||killer instanceof ShikigamiEntity pet&&!pet.isRitual()&&pet.getOwner()==p;
			TenShadows.defeated(this,p,soloRitual&&yours);
		}
		super.die(source);
		if (level() instanceof ServerLevel level) {
			dissolve(level);
		}
	}

	@Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
		if(ritual&&source.getEntity()!=null){Entity attacker=source.getEntity();LivingEntity caller=ritualist;
			if(attacker!=caller&&!(attacker instanceof ShikigamiEntity pet&&!pet.isRitual()&&pet.getOwner()==caller))soloRitual=false;
		}
		return super.hurtServer(level,source,amount);
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
