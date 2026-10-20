package com.jjkcraft.technique.behavior;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.awakening.BodyTransfer;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.*;

/** Distinct utility mechanics shared by data-defined manga technique families. */
public record MangaAbilityBehavior(String ability,double damage,int duration,double radius) implements TechniqueBehavior {
    public static final MapCodec<MangaAbilityBehavior> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("ability").forGetter(MangaAbilityBehavior::ability),Codec.doubleRange(0,100).optionalFieldOf("damage",0.0).forGetter(MangaAbilityBehavior::damage),
        Codec.intRange(1,2400).optionalFieldOf("duration",60).forGetter(MangaAbilityBehavior::duration),Codec.doubleRange(0.5,32).optionalFieldOf("radius",4.0).forGetter(MangaAbilityBehavior::radius)
    ).apply(i,MangaAbilityBehavior::new));
    @Override public TechniqueBehaviorType<?> type(){return TechniqueBehaviors.MANGA_ABILITY;}
    @Override public CastResult cast(CastContext ctx) {
        if(FamilyArts.handles(ability))return FamilyArts.cast(ctx,ability,damage,duration,radius);
        if(ExpressionArts.handles(ability))return ExpressionArts.cast(ctx,ability,damage,duration,radius);
        if(ability.equals("body_transfer"))return BodyTransfer.transplant(ctx);
        if(ability.equals("curse_manipulation"))return com.jjkcraft.awakening.ControlledCurses.cast(ctx);
        if(ability.equals("uzumaki"))return com.jjkcraft.awakening.ControlledCurses.uzumaki(ctx);
        var caster=ctx.caster();var targets=ctx.enemiesInRadius(caster.position(),Math.min(radius,ctx.range()>0?ctx.range():radius));
        if(ability.equals("swap")) {
            var target=ctx.rayTarget(ctx.range(),1).orElse(null);if(target==null)return CastResult.miss(ctx.eye());
            Vec3 a=caster.position(),b=target.position();
            if(!ctx.level().noCollision(caster,caster.getBoundingBox().move(b.subtract(a))) || !ctx.level().noCollision(target,target.getBoundingBox().move(a.subtract(b))))return CastResult.miss(b);
            target.teleportTo(ctx.level(),a.x,a.y,a.z,Set.of(),target.getYRot(),target.getXRot(),true);caster.teleportTo(ctx.level(),b.x,b.y,b.z,Set.of(),caster.getYRot(),caster.getXRot(),true);
            ctx.fx(TechniqueFxPayload.Phase.CAST,b);return CastResult.hit(List.of(target),b);
        }
        int effectTicks=Math.max(1,(int)Math.round(duration*Math.sqrt(ctx.power())));int potency=Math.max(0,Math.min(4,(int)Math.floor(ctx.power())-1));
        if(ability.equals("mass")){caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH,effectTicks,Math.min(2,1+potency)));caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,effectTicks,Math.min(2,potency)));}
        else if(ability.equals("armor")){caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,effectTicks,Math.min(2,1+potency)));caster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,effectTicks,0));}
        else if(ability.equals("flowing_blood")){caster.addEffect(new MobEffectInstance(MobEffects.SPEED,effectTicks,potency));caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH,effectTicks,potency));}
        else if(ability.equals("flight")){caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,effectTicks,0));caster.setDeltaMovement(caster.getDeltaMovement().add(0,Math.min(1.4,0.6*Math.sqrt(ctx.power())),0));caster.needsSync=true;}
        else for(var target:targets) {
            switch(ability) {
                case "freeze" -> target.addEffect(new MobEffectInstance(JjkEffects.FROZEN_FRAME,effectTicks,0));
                case "poison" -> target.addEffect(new MobEffectInstance(MobEffects.POISON,Math.min(200,effectTicks),0));
                case "gravity" -> {target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,effectTicks,3));target.setDeltaMovement(0,-0.8,0);target.needsSync=true;}
                case "antigravity" -> {target.setDeltaMovement(target.getDeltaMovement().add(0,0.8,0));target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,effectTicks,0));target.needsSync=true;}
                case "flames" -> target.igniteForSeconds(5);
                case "roots" -> target.addEffect(new MobEffectInstance(JjkEffects.BOUND,effectTicks,0));
                case "electric" -> {target.addEffect(new MobEffectInstance(JjkEffects.STUNNED,Math.min(12,duration),0));}
                case "repel" -> ctx.knockback(target,caster.position(),1.5*Math.sqrt(ctx.power()));
                default -> { }
            }
            if(damage>0)ctx.hurt(target,damage*ctx.power(),false);
        }
        ctx.fx(TechniqueFxPayload.Phase.CAST,caster.position(),new Vec3(radius,0,0),targets);
        return CastResult.hit(List.copyOf(targets),caster.position());
    }
}
