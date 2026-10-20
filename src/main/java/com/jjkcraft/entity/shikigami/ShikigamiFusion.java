package com.jjkcraft.entity.shikigami;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.ShikigamiPayloads;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.SummonBehavior;
import com.jjkcraft.network.payload.TechniqueFxPayload;

public final class ShikigamiFusion {
    public static final AttachmentType<List<Identifier>> RECIPE=AttachmentRegistry.create(JjkCraft.id("chimera_recipe"),b->b.persistent(Identifier.CODEC.listOf(0,4)).copyOnDeath().initializer(List::of));
    private static final com.jjkcraft.core.RateLimiter LIMIT=new com.jjkcraft.core.RateLimiter(2,2);
    private ShikigamiFusion(){}
    public static void init(){
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{p.setAttached(RECIPE,List.of());for(var summon:ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p))summon.dissolve(p.level());});
        ServerPlayNetworking.registerGlobalReceiver(ShikigamiPayloads.Recipe.TYPE,(payload,ctx)->{
            var p=ctx.player();boolean accepted=p.isAlive()&&!p.isSpectator()&&LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime())&&(payload.components().isEmpty()||save(p,payload.components()));
            JjkNetworking.send(p,new ShikigamiPayloads.Reply(p.getAttachedOrCreate(RECIPE),payload.request(),accepted));
        });
    }
    public static boolean save(ServerPlayer p,List<Identifier> ids){
        if(!TechniqueService.knows(p,JjkCraft.id("chimera_fusion"))||ids.size()<2||ids.size()>4||new HashSet<>(ids).size()!=ids.size())return false;
        var allowed=Techniques.all(p.registryAccess()).stream().filter(e->TechniqueService.knows(p,e.id())&&e.definition().behavior() instanceof SummonBehavior).map(e->((SummonBehavior)e.definition().behavior()).entity()).toList();
        if(ids.stream().anyMatch(id->!allowed.contains(id)||id.equals(JjkCraft.id("transfigured_soul"))||id.equals(JjkCraft.id("custom_chimera"))))return false;
        p.setAttached(RECIPE,List.copyOf(ids));return true;
    }
    public static CastResult cast(CastContext ctx,List<Identifier> fixed,Identifier result,int lifetime){
        var recipe=fixed.isEmpty()?ctx.caster().getAttachedOrCreate(RECIPE):fixed;
        if(recipe.size()<2||recipe.size()>4)return miss(ctx,"Configure two to four components in Moves → Fusion first.");
        var type=ShadowBeasts.TYPES.get(result);if(type==null)return CastResult.miss(ctx.eye());
        var owned=ShikigamiEntity.ownedBy(ctx.level(),ShikigamiEntity.class,ctx.caster());var consumed=new ArrayList<ShikigamiEntity>();int flags=0;
        for(var id:recipe){var pet=owned.stream().filter(e->!consumed.contains(e)&&net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(id)).findFirst().orElse(null);if(pet==null)return miss(ctx,"Summon every selected component before fusing.");consumed.add(pet);flags|=ShadowBeasts.features(pet.getType());}
        var at=ctx.caster().position().add(ctx.look().multiply(1,0,1).normalize().scale(2));
        var chimera=type.create(ctx.level(),EntitySpawnReason.MOB_SUMMONED);if(chimera==null)return CastResult.miss(at);
        chimera.snapTo(at.x,at.y,at.z,ctx.caster().getYRot(),0);if(!ctx.level().noCollision(chimera,chimera.getBoundingBox()))return miss(ctx,"The fused body needs clear space in front of you.");
        chimera.bind(ctx.caster(),lifetime,ctx.power());chimera.setFeatures(flags);
        if(result.equals(JjkCraft.id("custom_chimera"))){var hp=chimera.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);hp.setBaseValue((24+8*recipe.size())*Math.max(.5,ctx.power()));chimera.setHealth(chimera.getMaxHealth());}
        var hp=chimera.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);hp.setBaseValue(hp.getBaseValue()*ctx.details().expression().vitality()/100.0);chimera.setHealth(chimera.getMaxHealth());
        var speed=chimera.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);speed.setBaseValue(speed.getBaseValue()*ctx.details().expression().mobility()/100.0);
        if(!ctx.details().title().isEmpty()){chimera.setCustomName(net.minecraft.network.chat.Component.literal(ctx.details().title()));chimera.setCustomNameVisible(true);}
        if(ctx.level().addFreshEntity(chimera)){for(var old:owned)if(old.getType()==type&&!consumed.contains(old))old.dissolve(ctx.level());for(var pet:consumed)pet.dissolve(ctx.level());ctx.fx(TechniqueFxPayload.Phase.BURST,at,new net.minecraft.world.phys.Vec3(2,0,0),List.of(chimera));return CastResult.done(at);}
        return CastResult.miss(at);
    }
    private static CastResult miss(CastContext ctx,String message){if(ctx.caster() instanceof ServerPlayer p)p.sendSystemMessage(net.minecraft.network.chat.Component.literal(message),true);return CastResult.miss(ctx.eye());}
}
