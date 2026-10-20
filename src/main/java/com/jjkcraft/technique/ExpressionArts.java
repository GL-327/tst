package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.effect.JjkEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Combat-only marks belong to their caster, expire, and never modify shared technique definitions. */
public final class ExpressionArts {
    private record Mark(int charges,long expires){}
    private record Key(UUID caster,UUID target,String kind){}
    private record ReturnPoint(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 position,long expires){}
    private record Mirror(long expires,double stored){}
    private static final Map<Key,Mark> MARKS=new HashMap<>();
    private static final Map<UUID,ReturnPoint> RETURNS=new HashMap<>();
    private static final Map<UUID,Mirror> MIRRORS=new HashMap<>();
    private static final Set<String> ARTS=Set.of("charge_mark","charge_release","charge_chain","star_mark","star_pull","star_repel","echo_mark","echo_release","echo_return","mirror_guard","mirror_release");
    private ExpressionArts(){}
    public static boolean handles(String ability){return ARTS.contains(ability);}
    public static int charges(LivingEntity caster,LivingEntity target,String kind){var m=MARKS.get(new Key(caster.getUUID(),target.getUUID(),kind));return m!=null&&m.expires()>caster.level().getGameTime()?m.charges():0;}
    public static void init(){
        DamageModifiers.add((level,target,source,amount)->{
            var mirror=MIRRORS.get(target.getUUID());
            if(mirror==null||mirror.expires()<=level.getGameTime()||source.getEntity()==null||source.is(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT))return amount;
            double held=Math.min(40-mirror.stored(),amount*0.65);
            if(held<=0)return amount;
            MIRRORS.put(target.getUUID(),new Mirror(mirror.expires(),mirror.stored()+held));return (float)(amount-held);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%200==0){long now=server.overworld().getGameTime();MARKS.values().removeIf(m->m.expires()<=now);RETURNS.values().removeIf(m->m.expires()<=now);MIRRORS.values().removeIf(m->m.expires()+200<=now);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{MARKS.clear();RETURNS.clear();MIRRORS.clear();});
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{MARKS.keySet().removeIf(k->k.caster().equals(p.getUUID()));RETURNS.remove(p.getUUID());MIRRORS.remove(p.getUUID());});
    }
    public static CastResult cast(CastContext ctx,String ability,double damage,int duration,double radius){
        var caster=ctx.caster();long now=ctx.now();
        if(ability.equals("echo_return")){
            var point=RETURNS.get(caster.getUUID());
            if(point==null||point.expires()<=now||point.dimension()!=ctx.level().dimension()||point.position().distanceTo(caster.position())>ctx.range())return CastResult.miss(caster.position());
            if(!ctx.level().noCollision(caster,caster.getBoundingBox().move(point.position().subtract(caster.position()))))return CastResult.miss(caster.position());
            RETURNS.remove(caster.getUUID());caster.teleportTo(point.position().x,point.position().y,point.position().z);ctx.fx(TechniqueFxPayload.Phase.CAST,point.position());return CastResult.done(point.position());
        }
        if(ability.equals("mirror_guard")){
            var old=MIRRORS.get(caster.getUUID());MIRRORS.put(caster.getUUID(),new Mirror(now+duration,old!=null&&old.expires()+200>now?old.stored():0));ctx.fx(TechniqueFxPayload.Phase.CAST,caster.position());return CastResult.done(caster.position());
        }
        if(ability.equals("mirror_release")){
            var mirror=MIRRORS.get(caster.getUUID());if(mirror==null||mirror.stored()<=0||mirror.expires()+200<=now)return CastResult.miss(caster.position());
            var targets=ctx.enemiesInRadius(caster.position(),radius);if(targets.isEmpty())return CastResult.miss(caster.position());
            MIRRORS.remove(caster.getUUID());for(var t:targets){ctx.hurt(t,(damage+mirror.stored()/Math.sqrt(targets.size()))*ctx.power(),false);ctx.knockback(t,caster.position(),0.6);}
            ctx.fx(TechniqueFxPayload.Phase.HIT,caster.position(),Vec3.ZERO,targets);return CastResult.hit(new ArrayList<net.minecraft.world.entity.Entity>(targets),caster.position());
        }
        if(ability.endsWith("_mark")){
            var target=ctx.rayTarget(ctx.range(),0.65).orElse(null);if(target==null)return CastResult.miss(ctx.eye());
            String kind=ability.substring(0,ability.length()-5);int count=Math.min(3,charges(caster,target,kind)+1);
            MARKS.put(new Key(caster.getUUID(),target.getUUID(),kind),new Mark(count,now+1200));
            if(kind.equals("echo"))RETURNS.put(caster.getUUID(),new ReturnPoint(ctx.level().dimension(),caster.position(),now+1200));
            ctx.hurt(target,damage*ctx.power(),false);ctx.fx(TechniqueFxPayload.Phase.HIT,target.position(),new Vec3(count,0,0),List.of(target));return CastResult.hit(List.of(target),target.position());
        }
        if(ability.equals("charge_release")||ability.equals("echo_release")){
            var t=ctx.rayTarget(ctx.range(),0.65).orElse(null);String kind=ability.startsWith("charge")?"charge":"echo";
            if(t==null||charges(caster,t,kind)==0)return CastResult.miss(ctx.eye());int count=charges(caster,t,kind);
            MARKS.remove(new Key(caster.getUUID(),t.getUUID(),kind));ctx.hurt(t,damage*count*ctx.power(),false);
            if(kind.equals("charge"))t.addEffect(new MobEffectInstance(JjkEffects.STUNNED,Math.min(12,count*3),0));
            ctx.fx(TechniqueFxPayload.Phase.HIT,t.position(),t.position().subtract(ctx.eye()),List.of(t));return CastResult.hit(List.of(t),t.position());
        }
        var targets=ctx.enemiesInRadius(caster.position(),radius);var hit=new ArrayList<LivingEntity>();
        var anchor=ctx.rayTarget(ctx.range(),0.65).filter(t->charges(caster,t,"star")>0).orElse(null);
        if(ability.equals("star_pull")&&anchor==null)return CastResult.miss(ctx.eye());
        for(var t:targets){
            String kind=ability.equals("charge_chain")?"charge":"star";int count=charges(caster,t,kind);if(count==0)continue;
            if(ability.equals("charge_chain")){MARKS.remove(new Key(caster.getUUID(),t.getUUID(),kind));ctx.hurt(t,damage*count*ctx.power(),false);}
            else{
                Vec3 delta=ability.equals("star_pull")?anchor.position().subtract(t.position()):t.position().subtract(caster.position());
                if(delta.lengthSqr()>0.1){t.setDeltaMovement(t.getDeltaMovement().add(delta.normalize().scale(Math.min(1.3,0.5*Math.sqrt(ctx.power())))));t.needsSync=true;}
                t.addEffect(new MobEffectInstance(JjkEffects.BOUND,Math.min(40,duration),0));ctx.hurt(t,damage*ctx.power(),false);
            }
            hit.add(t);
        }
        ctx.fx(TechniqueFxPayload.Phase.HIT,caster.position(),Vec3.ZERO,hit);return hit.isEmpty()?CastResult.miss(caster.position()):CastResult.hit(List.copyOf(hit),caster.position());
    }
}
