package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.behavior.*;

/** Family extensions use distinct utility rules, with the normal per-move tuning and vow pipeline. */
public final class FamilyArts {
    private static final Set<String> ARTS=Set.of("shape_soul","soul_multiplicity","soul_recall","soul_blades","soul_dash","soul_touch","soul_body","soul_detonate","frame_rush","frame_lock","projection_overclock","ratio_overtime","ratio_collapse","ratio_expose","straw_hairpin","straw_stitch","straw_effigy","shadow_recall","shadow_escape","tide_prison","tidal_guard","flame_vents","flower_drain","swap_feint","swap_recall","curse_recall","curse_command","brain_recover","blood_condense","ice_armor","sky_counter","mass_anchor","construction_blades","echo_silence","electric_armor","granite_charge","love_orbit","mirror_step","thread_snare","tool_recall","flesh_regrow","harmony_restore","shadow_siphon");
    private FamilyArts(){}
    public static boolean handles(String ability){return ARTS.contains(ability);}
    public static CastResult cast(CastContext ctx,String ability,double damage,int duration,double radius){
        var caster=ctx.caster();int ticks=Math.max(1,(int)(duration*Math.sqrt(ctx.power())));
        if(ability.equals("shape_soul")||ability.equals("soul_multiplicity")){
            int count=ability.equals("shape_soul")?1:3;var existing=ShikigamiEntity.ownedBy(ctx.level(),TransfiguredSoulEntity.class,caster);
            int requested=ctx.details().expression().summons();if(requested>0)count=Math.min(6,requested);
            // Six living souls is a hard ownership cap, independent of cooldown and personalization.
            if(existing.size()+count>6)return CastResult.miss(caster.position());
            return new SummonBehavior(JjkCraft.id("transfigured_soul"),count,Math.min(12000,ticks)).cast(ctx);
        }
        if(ability.equals("soul_recall")||ability.equals("soul_detonate")){
            var souls=ShikigamiEntity.ownedBy(ctx.level(),TransfiguredSoulEntity.class,caster);if(souls.isEmpty())return CastResult.miss(caster.position());
            var hit=new LinkedHashSet<LivingEntity>();for(var soul:souls){
                if(ability.equals("soul_detonate")){for(var e:ctx.enemiesInRadius(soul.position(),radius)){ctx.hurt(e,damage*ctx.power(),true);hit.add(e);}ctx.fx(TechniqueFxPayload.Phase.BURST,soul.position(),new Vec3(radius,0,0),List.copyOf(hit));soul.dissolve(ctx.level());}
                else{double angle=2*Math.PI*hit.size()/souls.size();Vec3 at=caster.position().add(Math.cos(angle)*2,0,Math.sin(angle)*2);if(ctx.level().noCollision(soul,soul.getBoundingBox().move(at.subtract(soul.position()))))soul.teleportTo(at.x,at.y,at.z);hit.add(soul);}
            }return CastResult.hit(List.copyOf(hit),caster.position());
        }
        if(ability.equals("shadow_recall")){var pets=ShikigamiEntity.ownedBy(ctx.level(),ShikigamiEntity.class,caster);for(var pet:pets)pet.dissolve(ctx.level());return CastResult.done(caster.position());}
        if(ability.equals("curse_recall")&&caster instanceof net.minecraft.server.level.ServerPlayer p){
            var stock=new ArrayList<>(p.getAttachedOrCreate(com.jjkcraft.awakening.ControlledCurses.STOCK));var curses=ctx.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,caster.getBoundingBox().inflate(64),e->e.getAttachedOrCreate(com.jjkcraft.awakening.ControlledCurses.OWNER).equals(caster.getUUID().toString()));
            for(var curse:curses){if(stock.size()>=8)break;stock.add(curse.curseId());curse.discard();}p.setAttached(com.jjkcraft.awakening.ControlledCurses.STOCK,List.copyOf(stock));return CastResult.done(caster.position());
        }
        if(ability.equals("curse_command")){
            var target=ctx.rayTarget(ctx.range(),0.8).orElse(null);if(target==null)return CastResult.miss(ctx.eye());
            caster.setLastHurtMob(target);for(var curse:ctx.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,caster.getBoundingBox().inflate(64),e->e.getAttachedOrCreate(com.jjkcraft.awakening.ControlledCurses.OWNER).equals(caster.getUUID().toString())))curse.setTarget(target);
            return CastResult.hit(List.of(target),target.position());
        }
        if(Set.of("soul_dash","frame_rush","mirror_step","swap_feint","shadow_escape").contains(ability)){
            if(ability.equals("shadow_escape"))effect(caster,MobEffects.INVISIBILITY,ticks,0);
            if(ability.equals("frame_rush"))effect(caster,MobEffects.SPEED,ticks,2);
            if(ability.equals("mirror_step"))effect(caster,MobEffects.RESISTANCE,12,1);
            return new DashBehavior(Math.min(24,ctx.range()),6).cast(ctx);
        }
        var target=ctx.rayTarget(ctx.range()>0?ctx.range():6,0.6).orElse(null);
        if(Set.of("soul_touch","frame_lock","straw_stitch","ratio_expose","tide_prison","thread_snare").contains(ability)){
            if(target==null)return CastResult.miss(ctx.eye());
            effect(target,ability.equals("frame_lock")?JjkEffects.FROZEN_FRAME:ability.equals("ratio_expose")?MobEffects.WEAKNESS:JjkEffects.BOUND,ticks,0);
            if(ability.equals("soul_touch"))effect(target,JjkEffects.SHRUNK,ticks,0);
            if(damage>0)ctx.hurt(target,damage*ctx.power(),ability.equals("soul_touch"));
            caster.setLastHurtMob(target);ctx.fx(TechniqueFxPayload.Phase.HIT,target.position(),ctx.look(),List.of(target));return CastResult.hit(List.of(target),target.position());
        }
        if(ability.equals("swap_recall")){
            var pet=ShikigamiEntity.ownedBy(ctx.level(),ShikigamiEntity.class,caster).stream().filter(e->caster.distanceTo(e)<=ctx.range()).findFirst().orElse(null);if(pet==null)return CastResult.miss(ctx.eye());
            Vec3 a=caster.position(),b=pet.position();if(!ctx.level().noCollision(caster,caster.getBoundingBox().move(b.subtract(a)))||!ctx.level().noCollision(pet,pet.getBoundingBox().move(a.subtract(b))))return CastResult.miss(b);
            pet.teleportTo(a.x,a.y,a.z);caster.teleportTo(b.x,b.y,b.z);return CastResult.done(b);
        }
        if(ability.equals("straw_effigy"))return new StrawDollBehavior(damage,6,ticks).cast(ctx);
        if(ability.equals("brain_recover")||ability.equals("flesh_regrow")||ability.equals("harmony_restore")){
            if(ability.equals("flesh_regrow")&&!FleshSacrifice.knowsRct(caster))return CastResult.miss(caster.position());
            caster.heal((float)(damage*ctx.power()));effect(caster,MobEffects.REGENERATION,ticks,0);ctx.fx(TechniqueFxPayload.Phase.CAST,caster.position());return CastResult.done(caster.position());
        }
        if(Set.of("soul_blades","soul_body","ratio_overtime","projection_overclock","tidal_guard","flame_vents","ice_armor","sky_counter","mass_anchor","electric_armor","granite_charge","love_orbit","tool_recall","blood_condense").contains(ability)){
            switch(ability){
                case "soul_blades"->{effect(caster,MobEffects.STRENGTH,ticks,1);effect(caster,MobEffects.HASTE,ticks,1);}
                case "soul_body"->{effect(caster,JjkEffects.BULKED,ticks,0);effect(caster,MobEffects.RESISTANCE,ticks,1);}
                case "ratio_overtime"->{effect(caster,MobEffects.STRENGTH,ticks,2);effect(caster,MobEffects.HUNGER,ticks,1);}
                case "projection_overclock"->{effect(caster,MobEffects.SPEED,ticks,3);effect(caster,MobEffects.HASTE,ticks,1);}
                case "mass_anchor","granite_charge"->{effect(caster,MobEffects.RESISTANCE,ticks,2);effect(caster,MobEffects.SLOWNESS,ticks,1);}
                case "electric_armor"->{effect(caster,MobEffects.SPEED,ticks,1);effect(caster,MobEffects.STRENGTH,ticks,0);}
                case "blood_condense"->{effect(caster,MobEffects.STRENGTH,ticks,1);effect(caster,MobEffects.WEAKNESS,Math.max(1,ticks/4),0);}
                case "tool_recall"->{effect(caster,MobEffects.SLOW_FALLING,ticks,0);caster.setDeltaMovement(caster.getDeltaMovement().add(0,0.65,0));caster.needsSync=true;}
                default->{effect(caster,MobEffects.RESISTANCE,ticks,1);effect(caster,MobEffects.ABSORPTION,ticks,0);}
            }
            ctx.fx(TechniqueFxPayload.Phase.CAST,caster.position(),new Vec3(radius,0,0),List.of());return CastResult.done(caster.position());
        }
        var targets=ctx.enemiesInRadius(caster.position(),radius);
        for(var enemy:targets){
            if(ability.equals("echo_silence"))effect(enemy,JjkEffects.STUNNED,Math.min(18,ticks),0);
            if(ability.equals("flower_drain")||ability.equals("shadow_siphon")){double drained=com.jjkcraft.energy.CursedEnergy.drain(enemy,damage*ctx.power());com.jjkcraft.energy.CursedEnergy.restore(caster,drained*0.5);effect(enemy,JjkEffects.BOUND,ticks,0);}
            else if(damage>0)ctx.hurt(enemy,damage*ctx.power(),ability.equals("ratio_collapse"));
            if(ability.equals("straw_hairpin"))ctx.knockback(enemy,caster.position(),0.7);
        }
        ctx.fx(TechniqueFxPayload.Phase.BURST,caster.position(),new Vec3(radius,0,0),targets);return CastResult.hit(List.copyOf(targets),caster.position());
    }
    private static void effect(LivingEntity e,net.minecraft.core.Holder<MobEffect> effect,int ticks,int level){e.addEffect(new MobEffectInstance(effect,ticks,level));}
}
