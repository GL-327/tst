package com.jjkcraft.domain;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.DomainProfilePayload;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
public final class DomainCustomization {
    public static final AttachmentType<DomainProfile> PROFILE=AttachmentRegistry.create(JjkCraft.id("domain_expression"),b->b.persistent(DomainProfile.CODEC).copyOnDeath().initializer(()->DomainProfile.DEFAULT));
    private static final com.jjkcraft.core.RateLimiter LIMITER=new com.jjkcraft.core.RateLimiter(2,4);
    private DomainCustomization(){}
    public static DomainProfile get(Player p){return p.getAttachedOrCreate(PROFILE);}
    public static boolean allowsOpen(Identifier id){return !Set.of("unlimited_void","deadly_sentencing","idle_death_gamble").contains(id.getPath());}
    public static boolean save(ServerPlayer p,DomainProfile profile){
        if(profile.size()<50||profile.size()>200||profile.output()<50||profile.output()>200||profile.duration()<50||profile.duration()>200||profile.barrier()<50||profile.barrier()>200||profile.color() < -1||profile.color()>0xffffff||profile.secondary() < -1||profile.secondary()>0xffffff||profile.title().length()>48||profile.ambience().length()>24)return false;
        if(!p.isAlive()||p.isSpectator()||com.jjkcraft.vessel.CursedVessels.controlled(p)||DomainService.active(p).isPresent()||profile.open()&&DomainService.state(p).known().filter(DomainCustomization::allowsOpen).isEmpty())return false;
        if(!Set.of("","none","blades","glyphs","shadows","lanterns","arsenal","rings").contains(profile.ambience())||profile.title().codePoints().anyMatch(c->Character.isISOControl(c)||c==167))return false;
        p.setAttached(PROFILE,profile);return true;
    }
    public static DomainDefinition apply(DomainDefinition d,DomainProfile p){
        var v=d.visuals();var visuals=new DomainDefinition.Visuals(p.color()<0?v.color():p.color(),p.secondary()<0?v.secondary():p.secondary(),v.dome(),p.ambience().isEmpty()?v.ambience():p.ambience(),v.openSound(),v.ambientSound());
        return new DomainDefinition(d.name(),d.description(),d.grade(),d.castCost()*p.cost(),d.upkeep()*p.cost(),Math.clamp(d.radius()*p.size()/100.0,3,48),Math.clamp(d.maxDuration()*p.duration()/100,20,72000),d.burnout(),d.sureHit(),d.clashPriority(),d.behaviors(),visuals,d.innateWeight());
    }
    public static void init(){
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->LIMITER.forget(h.getPlayer().getUUID()));
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->p.setAttached(PROFILE,DomainProfile.DEFAULT));
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(p->JjkNetworking.send(p,new DomainProfilePayload(get(p),false)));
        ServerPlayNetworking.registerGlobalReceiver(DomainProfilePayload.TYPE,(payload,ctx)->{var p=ctx.player();if(!LIMITER.tryAcquire(p.getUUID(),p.level().getGameTime()))return;boolean accepted=!payload.save()||save(p,payload.profile());JjkNetworking.send(p,new DomainProfilePayload(get(p),accepted));});
    }
}
