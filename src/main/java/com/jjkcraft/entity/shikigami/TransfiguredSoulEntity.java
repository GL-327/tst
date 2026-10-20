package com.jjkcraft.entity.shikigami;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import com.jjkcraft.damage.JjkDamageTypes;

/** A temporary shaped soul: follows its creator, lunges at their enemies, and cannot hurt allies. */
public final class TransfiguredSoulEntity extends ShikigamiEntity {
    private int cooldown;
    public TransfiguredSoulEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,14).add(Attributes.MOVEMENT_SPEED,0.32).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void behave(ServerLevel level,LivingEntity owner,@Nullable LivingEntity target){
        if(cooldown>0)cooldown--;
        if(target==null){if(distanceTo(owner)>3&&tickCount%10==0)getNavigation().moveTo(owner,1.1);return;}
        getLookControl().setLookAt(target,50,50);if(tickCount%5==0)getNavigation().moveTo(target,1.35);
        if(distanceTo(target)<2.2&&hasLineOfSight(target)&&cooldown==0){
            target.hurtServer(level,level.damageSources().source(JjkDamageTypes.TECHNIQUE,this,owner),(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)*power));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.jjkcraft.effect.JjkEffects.BOUND,8,0));
            triggerAnim("action","attack");cooldown=26;
        }
    }
    @Override protected int dissolveColor(){return 0xB79BCB;}
}
