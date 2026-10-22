package com.jjkcraft.training;
import com.geckolib.animatable.*;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
/** A persistent, recoverable practice prop. It gives contact feedback without kills or loot farming. */
public final class TrainingDummyEntity extends PathfinderMob implements GeoEntity{
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public TrainingDummyEntity(EntityType<? extends TrainingDummyEntity> type,Level level){super(type,level);setNoAi(true);setPersistenceRequired();}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,10000).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL))return super.hurtServer(level,source,amount);
        if(!(source.getEntity() instanceof ServerPlayer p)||amount<=0)return false;
        // Black Flash replaces the damage source of a physical strike; it is still contact training.
        boolean technique=!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&!source.is(com.jjkcraft.damage.JjkDamageTypes.BLACK_FLASH);
        if(distanceToSqr(p)>4096)return false;
        boolean hit=super.hurtServer(level,source,Math.min(Math.min(10,amount),Math.max(.01f,getHealth()-1)));
        if(hit){String kind=com.jjkcraft.combat.WeaponCombat.kind(p.getMainHandItem());Training.contact(p,!kind.isEmpty()&&!kind.equals("item"),technique);triggerAnim("action","hit");}return hit;
    }
    @Override public void tick(){super.tick();if(!level().isClientSide()&&tickCount%20==0)setHealth(getMaxHealth());}
    @Override protected void registerGoals(){}
    @Override protected InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p){if(p.isShiftKeyDown()){if(!p.isCreative())spawnAtLocation(p.level(),new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.TRAINING_DOLL));discard();}else p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Straw doll · strike it with fists, weapons or techniques. Sneak-use to pack it up."),true);}return InteractionResult.SUCCESS;
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){r.add(new AnimationController<TrainingDummyEntity>("action",2,s->s.setAndContinue(RawAnimation.begin().thenLoop("idle"))).triggerableAnim("hit",RawAnimation.begin().thenPlay("hit")));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
