package com.jjkcraft.technique;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.behavior.SummonBehavior;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Original families share bounded resource plumbing; their marks, defenses and payoffs differ. */
public final class OriginalArts {
    public static final Set<String> FAMILIES=Set.of("glass","salt","lodestone","momentum","suture","origami","wax","rust","kintsugi","tollbell","ink","stencil","pressure","orbit","thorn","carrion","porcelain","echo_memory","sand","resin");
    private record Mark(int layers,long until){}
    private static final class Family {int charges,wards;double ward=.2;long guardUntil;Vec3 anchor;final Map<UUID,Mark> marks=new HashMap<>();}
    private static final class State {final LivingEntity owner;final Map<String,Family> families=new HashMap<>();long until;String guard="",lastFamily="";CastContext context;State(LivingEntity p){owner=p;}}
    private record Echo(CastContext ctx,UUID target,long at,double damage){}
    private static final Map<UUID,State> STATES=new HashMap<>();private static final List<Echo> ECHOES=new ArrayList<>();
    private static boolean reflecting;
    private OriginalArts(){}
    public static boolean handles(String ability){var s=ability.split("/");return s.length==3&&s[0].equals("original")&&FAMILIES.contains(s[1]);}
    private static State state(LivingEntity p){var s=STATES.computeIfAbsent(p.getUUID(),id->new State(p));s.until=p.level().getGameTime()+6000;return s;}
    public static int charges(LivingEntity p,String family){var s=STATES.get(p.getUUID());return s==null||!s.families.containsKey(family)?0:s.families.get(family).charges;}
    private static void publish(LivingEntity entity){if(!(entity instanceof ServerPlayer p))return;var s=STATES.get(p.getUUID());var f=s==null?null:s.families.get(s.lastFamily);com.jjkcraft.network.JjkNetworking.send(p,new com.jjkcraft.network.payload.OriginalArtsPayload(s==null?"":s.lastFamily,f==null?0:f.charges,f==null?0:f.wards,f==null?0:(int)Math.max(0,f.guardUntil-p.level().getGameTime())));}
    public static void init(){
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(OriginalArts::publish);
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{STATES.remove(p.getUUID());ECHOES.removeIf(e->e.ctx.caster().getUUID().equals(p.getUUID()));publish(p);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{STATES.clear();ECHOES.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            STATES.values().removeIf(s->s.owner.isRemoved()||!s.owner.isAlive()||s.owner.level().getGameTime()>s.until);
            for(var s:STATES.values())for(var f:s.families.values())f.marks.values().removeIf(m->m.until<s.owner.level().getGameTime());
            for(var it=ECHOES.iterator();it.hasNext();){var e=it.next();if(e.ctx.caster().isRemoved()||e.ctx.caster().level()!=e.ctx.level()){it.remove();continue;}if(e.ctx.now()<e.at)continue;var target=e.ctx.level().getEntity(e.target);if(target instanceof LivingEntity t&&e.ctx.isEnemy(t)&&t.distanceToSqr(e.ctx.caster())<32*32)e.ctx.hurt(t,e.damage,false);it.remove();}
        });
        DamageModifiers.add((level,target,source,amount)->{
            var s=STATES.get(target.getUUID());if(s==null||amount<=0)return amount;
            var golden=s.families.get("kintsugi");if(golden!=null&&source.getEntity()!=target){golden.charges=Math.min(8,golden.charges+1);publish(target);}
            var f=s.families.get(s.guard);long now=level.getGameTime();if(f==null||f.guardUntil<now||f.wards<=0||reflecting||source.is(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT))return amount;
            if(!CursedEnergy.trySpend(target,Math.min(8,amount*.8)))return amount;f.wards--;publish(target);
            if(source.getEntity() instanceof LivingEntity attacker&&s.context!=null){
                if(s.guard.equals("thorn")){try{reflecting=true;s.context.hurt(attacker,Math.min(4,amount*.2),false);}finally{reflecting=false;}}
                if(s.guard.equals("tollbell"))attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,30,0));
                if(s.guard.equals("lodestone"))attacker.knockback(.35,target.getX()-attacker.getX(),target.getZ()-attacker.getZ(),source,0f);
            }
            return (float)(amount*(1-f.ward));
        });
    }
    private static void effect(LivingEntity e,net.minecraft.core.Holder<MobEffect> effect,int duration){e.addEffect(new MobEffectInstance(effect,Math.clamp(duration,1,200),0));}
    private static void heal(CastContext ctx,float amount){if(!PersonalPacts.forbids(ctx.caster(),TechniquePact.HEALING))ctx.caster().heal(amount);}
    private static void secondary(CastContext ctx,String family,LivingEntity t,int duration){
        switch(family){
            case "salt"->CursedEnergy.drain(t,5);
            case "lodestone","suture","pressure","stencil"->ctx.knockback(t,ctx.caster().position(),family.equals("pressure")?-.7:-.35);
            case "momentum"->ctx.knockback(t,ctx.caster().position(),.5+Math.min(.4,ctx.caster().getDeltaMovement().length()));
            case "wax"->{t.igniteForSeconds(2);effect(t,MobEffects.SLOWNESS,40);}
            case "rust","porcelain"->effect(t,MobEffects.WEAKNESS,60);
            case "tollbell"->effect(t,JjkEffects.OVERLOADED,4);
            case "ink","sand"->effect(t,MobEffects.BLINDNESS,30);
            case "orbit"->{t.setDeltaMovement(t.getDeltaMovement().add(0,.3,0));t.needsSync=true;}
            case "thorn"->effect(t,MobEffects.POISON,60);
            case "carrion","origami"->effect(t,MobEffects.GLOWING,100);
            case "resin"->effect(t,MobEffects.SLOWNESS,60);
            case "echo_memory"->{if(ECHOES.size()<128)ECHOES.add(new Echo(ctx,t.getUUID(),ctx.now()+12,Math.min(8,2*ctx.power())));}
            default->{ }
        }
    }
    public static CastResult cast(CastContext ctx,String ability,double damage,int duration,double radius){
        var parts=ability.split("/");String family=parts[1],op=parts[2];var p=ctx.caster();var s=state(p);var f=s.families.computeIfAbsent(family,k->new Family());s.context=ctx;s.lastFamily=family;
        int ticks=Math.clamp(duration,20,200);double range=Math.clamp(ctx.range()>0?ctx.range():14,1,24);Vec3 origin=p.position();
        if(op.equals("guard")){
            if(PersonalPacts.forbids(p,TechniquePact.GUARD))return CastResult.miss(origin);
            s.guard=family;f.guardUntil=ctx.now()+ticks;f.wards=3;f.ward=Math.clamp(.2*Math.sqrt(ctx.power()),.1,.35);
            if(family.equals("kintsugi")){heal(ctx,Math.min(6,f.charges*.75f));f.charges=0;}
            if(family.equals("origami")||family.equals("carrion"))new SummonBehavior(JjkCraft.id("paper_heron"),1,160).cast(ctx);
            if(family.equals("wax"))effect(p,MobEffects.FIRE_RESISTANCE,ticks);
            if(family.equals("momentum")||family.equals("resin"))effect(p,MobEffects.SPEED,ticks);
            if(family.equals("salt"))p.removeEffect(MobEffects.POISON);
            if(family.equals("ink")||family.equals("sand"))for(var e:ctx.enemiesInRadius(origin,Math.min(10,radius)))effect(e,MobEffects.BLINDNESS,30);
            publish(p);ctx.fx(TechniqueFxPayload.Phase.FIELD,origin.add(0,1,0),new Vec3(2,ticks,0),List.of());return CastResult.done(origin);
        }
        if(op.equals("step")){
            if(PersonalPacts.forbids(p,TechniquePact.DASH))return CastResult.miss(origin);
            double distance=Math.min(12,range*Math.sqrt(ctx.power()));
            Vec3 candidate=family.equals("echo_memory")&&f.anchor!=null&&origin.distanceTo(f.anchor)<=distance?f.anchor:origin.add(ctx.look().multiply(1,0,1).normalize().scale(distance));
            Vec3 safe=ProjectionPlans.ground(p,candidate);if(safe==null||!ProjectionPlans.sweep(p,safe))return CastResult.miss(origin);
            f.anchor=origin;p.teleportTo(safe.x,safe.y,safe.z);f.charges=Math.min(8,f.charges+1);publish(p);ctx.fx(TechniqueFxPayload.Phase.CAST,origin.add(0,1,0),safe.subtract(origin).normalize(),List.of());return CastResult.done(safe);
        }
        List<LivingEntity> targets=op.equals("mark")?ctx.rayTarget(range,Math.clamp(radius*.2,.1,1.2)).map(List::of).orElse(List.of()):op.equals("salvo")?ctx.enemiesInCone(Math.min(range,radius+4),100):ctx.enemiesInRadius(ctx.details().center()==1?origin:ctx.blockLimit(Math.min(range,8)),radius);
        var hit=new ArrayList<LivingEntity>();int reserve=f.charges;
        for(var t:targets){if(!p.hasLineOfSight(t))continue;var mark=f.marks.get(t.getUUID());int layers=mark!=null&&mark.until>=ctx.now()?mark.layers:0;
            if(op.equals("mark")){if(f.marks.size()<64||f.marks.containsKey(t.getUUID()))f.marks.put(t.getUUID(),new Mark(Math.min(3,layers+1),ctx.now()+Math.clamp(duration,20,1200)));f.charges=Math.min(8,f.charges+1);f.anchor=t.position();}
            if(op.equals("snare")){effect(t,JjkEffects.BOUND,Math.min(20,Math.clamp(duration/10,2,12)+layers*4));if(family.equals("tollbell")&&layers>=2)TechniqueService.cancel(t);}
            double dealt=damage*ctx.power();if(op.equals("finish")){dealt*=.5+layers*.4+Math.min(8,reserve)*.08;f.marks.remove(t.getUUID());}
            if(family.equals("lodestone")||family.equals("rust")){int armor=0;for(var slot:List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET))if(!t.getItemBySlot(slot).isEmpty())armor++;dealt*=1+armor*.05;}
            if(ctx.hurt(t,dealt,false)){hit.add(t);secondary(ctx,family,t,ticks);if(family.equals("glass")&&op.equals("finish")&&layers>0)CursedEnergy.drain(t,layers*3);}
        }
        if(op.equals("finish"))f.charges=0;
        publish(p);
        Vec3 at=targets.isEmpty()?ctx.blockLimit(Math.min(range,8)):targets.getFirst().position();ctx.fx(op.equals("finish")?TechniqueFxPayload.Phase.BURST:TechniqueFxPayload.Phase.HIT,at.add(0,.8,0),new Vec3(Math.max(1,radius),0,0),hit);return CastResult.hit(List.copyOf(hit),at);
    }
}
