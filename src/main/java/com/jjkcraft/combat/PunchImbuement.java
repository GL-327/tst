package com.jjkcraft.combat;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.CombatPayloads;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.*;

/** Independent fist coatings. Energy and spatial effects are paid only on a real unarmed contact. */
public final class PunchImbuement {
    private static final Set<UUID> CE=new HashSet<>();
    private static final Map<UUID,CastContext> SPATIAL=new HashMap<>();
    private static final Set<UUID> RESOLVING=new HashSet<>();
    private PunchImbuement() {}
    public static boolean ce(Player p){return CE.contains(p.getUUID());}
    public static int mode(Player p){var c=SPATIAL.get(p.getUUID());return c==null?0:c.techniqueId().getPath().equals("blue_punch")?1:2;}
    public static boolean same(LivingEntity p,net.minecraft.resources.Identifier id){var c=SPATIAL.get(p.getUUID());return c!=null&&c.techniqueId().equals(id);}
    public static void clear(LivingEntity p){CE.remove(p.getUUID());SPATIAL.remove(p.getUUID());if(p instanceof ServerPlayer s)sync(s);}
    public static void toggleCe(ServerPlayer p){
        if(!com.jjkcraft.sorcerer.Progression.get(p).awakened()||com.jjkcraft.sorcerer.Progression.get(p).heavenlyRestricted())return;
        if(!CE.remove(p.getUUID())&&CursedEnergy.current(p)>=3)CE.add(p.getUUID());sync(p);
    }
    public static CastResult toggle(CastContext c){
        if(same(c.caster(),c.techniqueId()))SPATIAL.remove(c.caster().getUUID());else SPATIAL.put(c.caster().getUUID(),c);
        if(c.caster() instanceof ServerPlayer p)sync(p);
        return CastResult.done(c.eye());
    }
    public static void sync(ServerPlayer p){
        var payload=new CombatPayloads.Imbuement(p.getId(),mode(p),ce(p));JjkNetworking.send(p,payload);
        for(var observer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p))JjkNetworking.send(observer,payload);
    }
    public static void init(){
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->clear(h.getPlayer()));
        com.jjkcraft.sorcerer.SorcererLifecycle.addDeathCopier((old,p,policy,cfg)->clear(old));
        com.jjkcraft.sorcerer.Awakening.addResetHook(PunchImbuement::clear);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{CE.clear();SPATIAL.clear();RESOLVING.clear();});
    }
    public static void vanillaContact(LivingEntity target,net.minecraft.world.damagesource.DamageSource source){
        if(source.getEntity() instanceof ServerPlayer p&&source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&!CursedCombat.enabled(p))contact(p,target);
    }
    public static void contact(LivingEntity attacker,LivingEntity target){
        if(!(attacker instanceof ServerPlayer p)||!attacker.getMainHandItem().isEmpty()||!target.isAlive()||!p.isAlive()||p.isSpectator()||RESOLVING.contains(p.getUUID())
            ||PersonalPacts.forbids(p,TechniquePact.COMBAT)||com.jjkcraft.sorcerer.Progression.get(p).heavenlyRestricted())return;
        RESOLVING.add(p.getUUID());
        try {
            var spatial=SPATIAL.get(p.getUUID());boolean changed=false;
            if(ce(p)){
                if(CursedEnergy.trySpend(p,3)){
                    var d=Techniques.get(p.registryAccess(),JjkCraft.id("ink_iron_palm")).orElseThrow();
                    var ctx=new CastContext(p,p.level(),JjkCraft.id("ink_iron_palm"),d,1,1);
                    target.damageCooldownTime=0;ctx.hurt(target,4*(1+com.jjkcraft.sorcerer.Progression.get(p).growth().output()*.025),false);
                    ctx.fx(TechniqueFxPayload.Phase.HIT,target.getBoundingBox().getCenter(),p.getLookAngle(),List.of(target));
                }else{CE.remove(p.getUUID());changed=true;}
            }
            if(spatial!=null){
                if(spatial.level()!=p.level()||TechniqueService.check(p,spatial.techniqueId(),Personalization.definition(p,spatial.techniqueId(),Techniques.get(p.registryAccess(),spatial.techniqueId()).orElse(spatial.technique())))!=CastFailure.NONE||!CursedEnergy.trySpend(p,mode(p)==1?5:7)){SPATIAL.remove(p.getUUID());changed=true;}
                else {
                    target.damageCooldownTime=0;
                    double base=spatial.technique().behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior b?b.damage():10;
                    boolean landed=spatial.hurt(target,base*spatial.power(),false);
                    if(landed){
                        var direction=p.position().subtract(target.position()).multiply(1,0,1).normalize();
                        if(mode(p)==1)target.setDeltaMovement(direction.scale(.55*Math.sqrt(spatial.power())).add(0,.12,0));
                        else target.setDeltaMovement(direction.scale(-1.15*Math.sqrt(spatial.power())).add(0,.2,0));
                        target.needsSync=true;spatial.fx(TechniqueFxPayload.Phase.HIT,target.getBoundingBox().getCenter(),p.getLookAngle(),List.of(target));
                        TechniqueService.creditHit(p,spatial.techniqueId());
                    }
                }
            }
            if(changed)sync(p);
        } finally {RESOLVING.remove(p.getUUID());}
    }
}
