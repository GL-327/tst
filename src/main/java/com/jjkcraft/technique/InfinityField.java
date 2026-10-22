package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import com.jjkcraft.damage.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.technique.behavior.InfinityBehavior;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.InfinityPayload;

/** An independent passive field; ordinary casts and channels do not replace it. */
public final class InfinityField {
    private static final Map<UUID,CastContext> FIELDS=new HashMap<>();
    private static final Map<UUID,Long> DISRUPTED=new HashMap<>();
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> PREFERRED=net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(com.jjkcraft.JjkCraft.id("infinity_enabled"),b->b.persistent(com.mojang.serialization.Codec.BOOL).copyOnDeath().initializer(()->true));
    private InfinityField() { }
    public static boolean preferred(ServerPlayer p){return p.getAttachedOrCreate(PREFERRED);}
    public static boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&com.jjkcraft.sorcerer.Progression.get(p).awakened()&&!com.jjkcraft.sorcerer.Progression.get(p).heavenlyRestricted()&&(TechniqueService.knows(p,com.jjkcraft.JjkCraft.id("infinity"))||com.jjkcraft.awakening.CharacterAwakening.get(p).family().equals("jjkcraft:limitless"));}
    public static boolean on(LivingEntity caster){var ctx=FIELDS.get(caster.getUUID());return ctx!=null&&ctx.caster()==caster&&caster.isAlive()&&(!(caster instanceof ServerPlayer p)||eligible(p)&&preferred(p));}
    public static CastFailure toggle(ServerPlayer p){
        if(!eligible(p))return CastFailure.NOT_KNOWN;
        if(!preferred(p)){
            var id=com.jjkcraft.JjkCraft.id("infinity");var d=Techniques.get(p.registryAccess(),id).orElse(null);
            if(d==null)return CastFailure.NOT_KNOWN;
            var failure=TechniqueService.check(p,id,Personalization.definition(p,id,d));if(failure!=CastFailure.NONE)return failure;
        }
        CursedEnergy.checkpoint(p);p.setAttached(PREFERRED,!preferred(p));
        if(!preferred(p))stop(p);else refresh(p);
        sync(p);return CastFailure.NONE;
    }
    public static void disrupt(LivingEntity caster,int ticks){DISRUPTED.put(caster.getUUID(),caster.level().getGameTime()+ticks);stop(caster);}
    public static void refresh(ServerPlayer p){
        if(!eligible(p)||!preferred(p)||p.level().getGameTime()<DISRUPTED.getOrDefault(p.getUUID(),0L)||com.jjkcraft.combat.CursedCombat.interrupted(p)||p.hasEffect(com.jjkcraft.effect.JjkEffects.STUNNED)||p.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME))return;
        var id=com.jjkcraft.JjkCraft.id("infinity");var raw=Techniques.get(p.registryAccess(),id).orElse(null);if(raw==null)return;
        var d=Personalization.definition(p,id,raw);if(PersonalPacts.check(p,id,d)!=CastFailure.NONE)return;
        var previous=FIELDS.get(p.getUUID());
        if(previous!=null&&(previous.caster()!=p||previous.level()!=p.level()||!previous.technique().equals(d)))stop(p);
        if(!FIELDS.containsKey(p.getUUID())&& (upkeep(p,d)<=0||CursedEnergy.current(p)>=upkeep(p,d)/20)){
            var ctx=new CastContext(p,p.level(),id,d,1,1);CursedEnergy.checkpoint(p);start(ctx);d.behavior().cast(ctx);
        }
    }
    public static double upkeep(LivingEntity caster,TechniqueDefinition d){return ReverseTechnique.available(caster)?0:d.cast().upkeep()*TechniqueService.modifiers(caster,com.jjkcraft.JjkCraft.id("infinity"),d).cost();}
    public static void start(CastContext ctx){FIELDS.put(ctx.caster().getUUID(),ctx);sync(ctx.caster());}
    public static void stop(LivingEntity caster){if(FIELDS.containsKey(caster.getUUID()))CursedEnergy.checkpoint(caster);var ctx=FIELDS.remove(caster.getUUID());if(ctx!=null){ctx.technique().behavior().end(ctx);sync(caster);}}
    private static void sync(LivingEntity caster){if(caster instanceof ServerPlayer p)JjkNetworking.send(p,new InfinityPayload(on(caster),preferred(p)));}
    public static void init(){
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(InfinityField::sync);
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{stop(p);DISRUPTED.remove(p.getUUID());p.setAttached(PREFERRED,true);sync(p);});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{stop(h.getPlayer());DISRUPTED.remove(h.getPlayer().getUUID());});
        // Checkpoint before changing the map so lazy regeneration cannot be recovered on toggle-off.
        CursedEnergy.addRegenModifier(caster->on(caster)&&!ReverseTechnique.available(caster)?0:1);
        DamageModifiers.add((level,target,source,amount)->{
            if(!on(target))return amount;
            var ctx=FIELDS.get(target.getUUID());if(ctx==null||!(ctx.technique().behavior() instanceof InfinityBehavior field))return amount;
            if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.is(JjkDamageTypes.SURE_HIT)||source.is(JjkDamageTypes.BACKLASH))return amount;
            if(source.getEntity() instanceof LivingEntity attacker&&source.getDirectEntity()==attacker&&source.typeHolder().unwrapKey().map(k->k.identifier().getNamespace().equals("minecraft")).orElse(false)
                &&com.jjkcraft.item.tool.CursedTools.get(attacker,attacker.getMainHandItem()).filter(tool->com.jjkcraft.item.tool.CursedTools.usable(attacker,tool)&&tool.onHit()==com.jjkcraft.item.tool.CursedToolDefinition.OnHit.DISPEL).isPresent()){
                disrupt(target,100);TechniqueService.cancel(target);return amount;
            }
            boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE);
            if((projectile&&field.filter()==1)||(!projectile&&field.filter()==0)||source.getEntity()==null||source.getEntity()==target)return amount;
            // Environmental damage and sure-hits still apply; spatial separation stops incoming attacks.
            return 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerList().getPlayers())refresh(p);
            for(var ctx:List.copyOf(FIELDS.values())){
            var caster=ctx.caster();
            if(!caster.isAlive()||caster.isRemoved()||caster.level()!=ctx.level()||caster instanceof ServerPlayer p&&(!eligible(p)||!preferred(p))||caster.hasEffect(com.jjkcraft.effect.JjkEffects.STUNNED)||caster.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME)||PersonalPacts.check(caster,ctx.techniqueId(),ctx.technique())!=CastFailure.NONE){stop(caster);continue;}
            double cost=upkeep(caster,ctx.technique())/20;
            if(cost>0&&!CursedEnergy.trySpend(caster,cost)){stop(caster);continue;}
            ctx.technique().behavior().tick(ctx,0);
        }});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{FIELDS.clear();DISRUPTED.clear();});
    }
}
