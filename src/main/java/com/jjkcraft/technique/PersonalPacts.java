package com.jjkcraft.technique;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.attachment.v1.*;
import com.mojang.serialization.Codec;
import com.jjkcraft.JjkCraft;

/** Saved seals prevent casting with a restriction and immediately removing it to evade its price. */
public final class PersonalPacts {
    public static final AttachmentType<Long> SEALED=AttachmentRegistry.create(JjkCraft.id("expression_pact_seal"),b->b.persistent(Codec.LONG).copyOnDeath().initializer(()->0L));
    private PersonalPacts(){}
    public static boolean forbids(LivingEntity entity,int flag){
        if(!(entity instanceof Player p))return false;
        var known=TechniqueService.get(p).known();
        return p.getAttachedOrCreate(Personalization.TUNINGS).entrySet().stream().anyMatch(e->known.contains(e.getKey())&&e.getValue().pact().has(flag));
    }
    public static Optional<Identifier> single(Player p){return p.getAttachedOrCreate(Personalization.TUNINGS).entrySet().stream().filter(e->TechniqueService.knows(p,e.getKey())&&e.getValue().pact().has(TechniquePact.SINGLE)).map(java.util.Map.Entry::getKey).findFirst();}
    public static boolean maySave(ServerPlayer p,Identifier id,TechniqueTuning next){
        var old=Personalization.get(p,id);
        var raw=Techniques.get(p.registryAccess(),id).orElse(null);
        if(raw!=null)for(int flag=0;flag<10;flag++)if(next.pact().has(flag)&&!TechniquePact.compatible(raw,flag))return false;
        if(old.pact().equals(next.pact()))return true;
        if(p.getAttachedOrCreate(SEALED)>p.level().getGameTime())return false;
        return !next.pact().has(TechniquePact.SINGLE)||single(p).filter(other->!other.equals(id)).isEmpty();
    }
    public static CastFailure check(LivingEntity caster,Identifier id,TechniqueDefinition definition){
        if(forbids(caster,TechniquePact.DASH)&&TechniquePact.movement(definition))return CastFailure.VOW;
        if(forbids(caster,TechniquePact.COMBAT)&&definition.behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior)return CastFailure.VOW;
        if(forbids(caster,TechniquePact.GUARD)&&definition.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m&&m.ability().startsWith("original/")&&m.ability().endsWith("/guard"))return CastFailure.VOW;
        if(caster instanceof Player p&&single(p).filter(only->!only.equals(id)).isPresent())return CastFailure.VOW;
        if(forbids(caster,TechniquePact.HEALING)&&definition.category()==TechniqueCategory.HEALING)return CastFailure.VOW;
        if(forbids(caster,TechniquePact.DASH)&&(definition.behavior() instanceof com.jjkcraft.technique.behavior.DashBehavior || definition.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m && java.util.Set.of("shadow_escape","soul_dash","frame_rush","mirror_step","swap_feint").contains(m.ability())))return CastFailure.VOW;
        var pact=Personalization.get(caster,id).pact();
        if(pact.has(TechniquePact.STILL)&&(!caster.onGround()||caster.getDeltaMovement().horizontalDistanceSqr()>0.0004))return CastFailure.VOW;
        if(pact.has(TechniquePact.WOUNDED)&&caster.getHealth()>caster.getMaxHealth()*0.5)return CastFailure.VOW;
        return CastFailure.NONE;
    }
    public static void seal(LivingEntity caster,Identifier id){
        if(caster instanceof Player p&&Personalization.get(p,id).pact().flags()!=0){
            int ticks=Techniques.get(p.registryAccess(),id).map(raw->Personalization.definition(p,id,raw)).map(d->d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior summon?summon.lifetime():d.behavior() instanceof com.jjkcraft.technique.behavior.FusionBehavior fusion?fusion.lifetime():1200).orElse(1200);
            p.setAttached(SEALED,Math.max(p.getAttachedOrCreate(SEALED),p.level().getGameTime()+Math.max(1200,ticks)));
        }
    }
}
