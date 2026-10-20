package com.jjkcraft.awakening;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.entity.*;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.*;

/** Persistent fallen hosts; Brain Transplant never takes control of a living player. */
public final class BodyTransfer {
    public record Body(String type,String name,List<Identifier> techniques,double health,long expires) {
        public static final Codec<Body> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("type").forGetter(Body::type),Codec.STRING.fieldOf("name").forGetter(Body::name),
            Identifier.CODEC.listOf().fieldOf("techniques").forGetter(Body::techniques),Codec.DOUBLE.fieldOf("health").forGetter(Body::health),
            Codec.LONG.fieldOf("expires").forGetter(Body::expires)).apply(i,Body::new));
    }
    public static final AttachmentType<Map<String,Body>> BODIES=AttachmentRegistry.create(JjkCraft.id("fallen_bodies"),b->b.persistent(Codec.unboundedMap(Codec.STRING,Body.CODEC)).initializer(HashMap::new));
    private BodyTransfer() { }
    public static void init() {
        com.jjkcraft.damage.DamageModifiers.add((level,target,source,amount)->level.getAttachedOrCreate(BODIES).containsKey(target.getUUID().toString())?0:amount);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(entity.level() instanceof ServerLevel level) || level.getAttachedOrCreate(BODIES).containsKey(entity.getUUID().toString()))return;
            if(entity instanceof ServerPlayer || entity instanceof EntrantEntity || entity instanceof net.minecraft.world.entity.npc.villager.Villager)
                leaveBody(level,entity);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%100==0)for(var level:server.getAllLevels()){
            var bodies=new HashMap<>(level.getAttachedOrCreate(BODIES)); boolean changed=false;
            for(var entry:List.copyOf(bodies.entrySet())) if(entry.getValue().expires()<=server.overworld().getGameTime()) {
                var marker=level.getEntity(UUID.fromString(entry.getKey()));if(marker!=null)marker.discard();bodies.remove(entry.getKey());changed=true;
            }
            if(changed)level.setAttached(BODIES,bodies);
        }});
    }
    public static LivingEntity leaveBody(ServerLevel level,LivingEntity fallen) {
        String type=fallen instanceof Player?CharacterTraits.get((Player)fallen).form():BuiltInRegistries.ENTITY_TYPE.getKey(fallen.getType()).toString();
        if(type.isEmpty())type="jjkcraft:entrant";
        EntityType<?> kind=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type));
        Entity created=kind==null?null:kind.create(level,EntitySpawnReason.EVENT);
        if(!(created instanceof Mob marker))return null;
        marker.snapTo(fallen.getX(),fallen.getY(),fallen.getZ(),fallen.getYRot(),0);marker.setNoAi(true);marker.setSilent(true);
        marker.setPersistenceRequired();marker.setCustomName(Component.literal("Fallen body: "+fallen.getName().getString()));marker.setCustomNameVisible(true);
        List<Identifier> moves=fallen instanceof Player p?new ArrayList<>(TechniqueService.get(p).known()):fallen instanceof EntrantEntity entrant?entrant.techniques():List.of();
        Body body=new Body(type,fallen.getName().getString(),List.copyOf(moves),fallen.getMaxHealth(),level.getServer().overworld().getGameTime()+24000);
        var bodies=new HashMap<>(level.getAttachedOrCreate(BODIES));bodies.put(marker.getUUID().toString(),body);level.setAttached(BODIES,bodies);level.addFreshEntity(marker);return marker;
    }
    public static CastResult transplant(CastContext ctx) {
        if(!(ctx.caster() instanceof ServerPlayer p))return CastResult.miss(ctx.eye());
        double reach=Math.min(18,ctx.range());Vec3 from=ctx.eye(),to=ctx.blockLimit(reach);
        var hit=ctx.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(reach),body->ctx.level().getAttachedOrCreate(BODIES).containsKey(body.getUUID().toString()))
            .stream().filter(body->body.getBoundingBox().inflate(0.6).clip(from,to).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if(hit==null)return CastResult.miss(ctx.eye());
        return occupy(p,hit,reach)?CastResult.done(p.position()):CastResult.miss(hit.position());
    }
    public static boolean occupy(ServerPlayer p,LivingEntity host) {
        return occupy(p,host,6);
    }
    private static boolean occupy(ServerPlayer p,LivingEntity host,double reach) {
        var level=p.level(); var bodies=new HashMap<>(level.getAttachedOrCreate(BODIES));var body=bodies.get(host.getUUID().toString());
        if(body==null || p.distanceToSqr(host)>reach*reach || body.expires()<=level.getServer().overworld().getGameTime() || !p.hasLineOfSight(host))return false;
        Vec3 position=host.position(); bodies.remove(host.getUUID().toString());level.setAttached(BODIES,bodies);
        leaveBody(level,p);host.discard(); TechniqueService.stop(p);
        var old=CharacterTraits.get(p);
        for(var id:old.hostTechniques())TechniqueService.revoke(p,id);
        var incoming=body.techniques().stream().filter(id->!id.equals(JjkCraft.id("brain_transplant")) && Techniques.get(p.registryAccess(),id).isPresent()).limit(3).toList();
        CharacterTraits.set(p,old.withForm(body.type(),incoming,body.health()));
        var state=TechniqueService.get(p);var slots=new ArrayList<Optional<Identifier>>(Collections.nCopies(TechniqueState.SLOTS,Optional.empty()));
        slots.set(0,Optional.of(JjkCraft.id("brain_transplant")));
        for(int n=0;n<incoming.size();n++){TechniqueService.grant(p,incoming.get(n));slots.set(n+1,Optional.of(incoming.get(n)));}
        state=TechniqueService.get(p);TechniqueService.set(p,TechniqueState.normalized(state.innate(),state.learned(),slots,0,state.mastery(),state.cooldowns()));
        p.teleportTo(level,position.x,position.y,position.z,Set.of(),p.getYRot(),0,true);
        p.setHealth(Math.max(1,Math.min(p.getMaxHealth(),(float)body.health())));
        p.sendSystemMessage(Component.literal("Brain Transplant: now inhabiting "+body.name()+". Host moves occupy at most three slots; F5 shows your body."));return true;
    }
}
