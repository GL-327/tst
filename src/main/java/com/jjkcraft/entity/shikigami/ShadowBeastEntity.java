package com.jjkcraft.entity.shikigami;

import java.util.*;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import net.minecraft.network.syncher.*;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.technique.*;
import static com.jjkcraft.entity.shikigami.ShadowBeasts.*;

/** Owned beasts and chimeras. Adaptation and support healing have explicit, non-immune bounds. */
public final class ShadowBeastEntity extends ShikigamiEntity {
    private static final EntityDataAccessor<Integer> FEATURES=SynchedEntityData.defineId(ShadowBeastEntity.class,EntityDataSerializers.INT);
    private final ShadowBeasts.Kind kind;
    private final Map<String,Integer> adaptation=new HashMap<>();
    private int attackCooldown,chargeTicks;
    public ShadowBeastEntity(EntityType<? extends PathfinderMob> type,Level level,ShadowBeasts.Kind kind){super(type,level);this.kind=kind;entityData.set(FEATURES,kind.features());setNoGravity(kind.flying());}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(FEATURES,0);}
    public int features(){return entityData.get(FEATURES);}
    public void setFeatures(int flags){entityData.set(FEATURES,flags);}
    private boolean has(int flag){return (features()&flag)!=0;}
    public double adaptedResistance(String key){return Math.min(.6,adaptation.getOrDefault(key,0)*.12);}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
        String key=source instanceof com.jjkcraft.damage.TechniqueDamageSource t?t.technique().toString():source.typeHolder().unwrapKey().map(k->k.identifier().toString()).orElse("unknown");
        float scaled=has(WHEEL)&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)?(float)(amount*(1-adaptedResistance(key))):amount;
        boolean hit=super.hurtServer(level,source,scaled);if(hit&&has(WHEEL)){adaptation.merge(key,1,Integer::sum);level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+getBbHeight(),getZ(),6,.4,.1,.4,.02);}return hit;
    }
    private boolean enemy(LivingEntity e,LivingEntity owner){return e!=this&&e!=owner&&e.isAlive()&&!e.isSpectator()&&!owner.isAlliedTo(e)&&!(e instanceof OwnableEntity pet&&pet.getOwner()==owner);}
    @Override protected void behave(ServerLevel level,LivingEntity owner,@Nullable LivingEntity target){
        if(attackCooldown>0)attackCooldown--;
        if(has(DEER)&&tickCount%20==0&&owner.getHealth()<owner.getMaxHealth()&&!PersonalPacts.forbids(owner,TechniquePact.HEALING)&&ShadowBeasts.healReady(owner)&&com.jjkcraft.energy.CursedEnergy.trySpend(owner,12)){
            owner.heal(1);heal(1);level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,owner.getX(),owner.getY()+1,owner.getZ(),5,.3,.4,.3,.01);
        }
        if(has(RABBIT)){
            var threat=target;double angle=tickCount*.09+getId();Vec3 at=owner.position().add(Math.cos(angle)*3,0,Math.sin(angle)*3);if(tickCount%8==0)getNavigation().moveTo(at.x,at.y,at.z,1.25);
            if(threat instanceof Mob mob&&tickCount%25==0&&mob.getTarget()==owner&&distanceTo(threat)<7)mob.setTarget(this);return;
        }
        boolean flying=kind.flying();
        if(target==null){chargeTicks=0;if(flying){Vec3 at=owner.position().add(Math.cos(tickCount*.04+getId())*2,2,Math.sin(tickCount*.04+getId())*2);setDeltaMovement(at.subtract(position()).scale(.12));}else if(distanceTo(owner)>4&&tickCount%10==0)getNavigation().moveTo(owner,1.15);return;}
        if(!enemy(target,owner))return;
        getLookControl().setLookAt(target,60,60);double distance=distanceTo(target);
        if(flying){Vec3 toward=target.getEyePosition().subtract(position());setDeltaMovement(toward.normalize().scale(Math.min(.45,toward.length()*.18)));}
        else if(tickCount%5==0)getNavigation().moveTo(target,has(OX)?1.6:1.35);
        if(has(OX)&&distance>2.2&&hasLineOfSight(target))chargeTicks=Math.min(40,chargeTicks+1);
        if(attackCooldown==0&&hasLineOfSight(target)&&distance<(has(NUE)||has(FIRE)||has(FEATHER)?8:has(ELEPHANT)?3.5:2.6)){
            double hit=getAttributeValue(Attributes.ATTACK_DAMAGE)*power*(has(OX)?1+chargeTicks/40.0:1);chargeTicks=0;
            target.hurtServer(level,level.damageSources().source(JjkDamageTypes.TECHNIQUE,this,owner),(float)hit);
            if(has(TOAD)||has(SERPENT))target.addEffect(new MobEffectInstance(com.jjkcraft.effect.JjkEffects.BOUND,18,0));
            if(has(POISON))target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
            if(has(FIRE))target.igniteForSeconds(3);
            if(has(NUE))target.addEffect(new MobEffectInstance(com.jjkcraft.effect.JjkEffects.STUNNED,6,0));
            if(has(NUE)||has(FIRE)||has(FEATHER))for(int n=0;n<8;n++){Vec3 at=getEyePosition().lerp(target.getEyePosition(),n/8.0);level.sendParticles(has(FIRE)?net.minecraft.core.particles.ParticleTypes.FLAME:has(NUE)?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:net.minecraft.core.particles.ParticleTypes.END_ROD,at.x,at.y,at.z,1,0,0,0,0);}
            if(has(ELEPHANT)){for(var e:level.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(2.5),e->enemy(e,owner))){e.knockback(.7,getX()-e.getX(),getZ()-e.getZ(),level.damageSources().mobAttack(this),0);e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,35,0));}}
            triggerAnim("action","attack");attackCooldown=has(TIGER)?22:has(DEER)?50:32;
        }
    }
    @Override protected int dissolveColor(){return 0x8D82C5;}
}
