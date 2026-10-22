package com.jjkcraft.technique;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.energy.SorceryPractice;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import com.jjkcraft.sorcerer.*;

public final class Personalization {
    public static final AttachmentType<Map<Identifier,TechniqueTuning>> TUNINGS=AttachmentRegistry.create(JjkCraft.id("technique_tunings"),
        b->b.persistent(PersonalizationPayloads.TUNINGS).copyOnDeath().initializer(Map::of));
    private static final RateLimiter LIMIT=new RateLimiter(4,8);
    private Personalization() { }
    public static TechniqueTuning get(LivingEntity caster,Identifier id) {
        if(caster instanceof Player p){var saved=p.getAttachedOrCreate(TUNINGS).get(id);if(saved!=null)return constrain(p,id,saved);}
        return Techniques.get(caster.registryAccess(),id).map(TechniqueTuning::defaultFor).orElse(TechniqueTuning.DEFAULT);
    }
    public static TechniqueDefinition definition(LivingEntity caster,Identifier id,TechniqueDefinition raw) { return get(caster,id).apply(raw); }
    private static TechniqueTuning constrain(Player p,Identifier id,TechniqueTuning tuning){
        double mastery=Techniques.get(p.registryAccess(),id).map(d->TechniqueService.mastery(p,id,d)).orElse(0.0);
        return tuning.limited(Progression.gradeRank(p.registryAccess(),p),Progression.get(p).growth().output(),mastery,p.isCreative());
    }
    public static void init() {
        ExpressionArts.init();
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var p=handler.getPlayer();var resistance=p.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
            boolean oldBuff=TechniqueService.get(p).known().stream().anyMatch(id->Techniques.get(p.registryAccess(),id).map(d->d.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior b&&java.util.Set.of("mass","armor").contains(b.ability())).orElse(false));
            if(oldBuff&&resistance!=null&&resistance.getAmplifier()>2){int duration=resistance.getDuration();p.removeEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE,duration,2));}
        });
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((p,level,hand)->
            p.getItemInHand(hand).has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS)&&PersonalPacts.forbids(p,TechniquePact.GUARD)
                ?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
        ServerPlayNetworking.registerGlobalReceiver(PersonalizationPayloads.Tune.TYPE,(payload,ctx)-> {
            var p=ctx.player();int result=!p.isAlive()||p.isSpectator()?3:!LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime())?2:!PersonalPacts.maySave(p,payload.id(),payload.tuning())?4:save(p,payload.id(),payload.tuning())?0:1;
            send(p);JjkNetworking.send(p,new PersonalizationPayloads.TuneReply(payload.id(),payload.request(),result));
        });
        SorcererLifecycle.addFullSync(Personalization::send);
        SorcererLifecycle.addDeathCopier((old,p,policy,cfg)->p.setAttached(TUNINGS,Map.copyOf(old.getAttachedOrCreate(TUNINGS))));
        Awakening.addResetHook(p->{p.setAttached(TUNINGS,Map.of());p.setAttached(PersonalPacts.SEALED,0L);p.removeEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);p.removeEffect(com.jjkcraft.effect.JjkEffects.BULKED);p.removeEffect(com.jjkcraft.effect.JjkEffects.SHRUNK);send(p);});
    }
    public static boolean save(ServerPlayer p,Identifier id,TechniqueTuning tuning) {
        tuning=constrain(p,id,tuning);
        if(!PersonalPacts.maySave(p,id,tuning))return false;
        if(!TechniqueService.knows(p,id)||Techniques.get(p.registryAccess(),id).isEmpty())return false;
        tuning=tuning.normalized(Techniques.get(p.registryAccess(),id).orElseThrow());
        var stored=p.getAttachedOrCreate(TUNINGS).getOrDefault(id,TechniqueTuning.defaultFor(Techniques.get(p.registryAccess(),id).orElseThrow()));
        if(tuning.equals(stored))return true;
        var map=new HashMap<>(p.getAttachedOrCreate(TUNINGS));
        if(tuning.equals(TechniqueTuning.defaultFor(Techniques.get(p.registryAccess(),id).orElseThrow())))map.remove(id); else if(map.size()<256||map.containsKey(id))map.put(id,tuning);else return false;
        if(id.getPath().equals("infinity"))InfinityField.stop(p);
        // End the old snapshot before applying new upkeep. A pending cast retains its paid snapshot.
        TechniqueService.active(p).filter(a->a.id().equals(id)).ifPresent(a->TechniqueService.stop(p));
        if(!get(p,id).pact().equals(tuning.pact()))TechniqueService.stop(p);
        com.jjkcraft.energy.CursedEnergy.checkpoint(p);p.setAttached(TUNINGS,Map.copyOf(map));
        if(PersonalPacts.forbids(p,TechniquePact.GUARD)&&p.isUsingItem()&&p.getUseItem().has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS))p.stopUsingItem();
        send(p);return true;
    }
    public static void send(ServerPlayer p) { JjkNetworking.send(p,new PersonalizationPayloads.View(p.getAttachedOrCreate(TUNINGS),SorceryPractice.get(p))); }
}
