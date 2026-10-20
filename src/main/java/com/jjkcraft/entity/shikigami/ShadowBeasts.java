package com.jjkcraft.entity.shikigami;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import com.jjkcraft.entity.JjkEntities;

/** Shared archetypes make custom chimeras inherit mechanics from the actual consumed summons. */
public final class ShadowBeasts {
    public static final int HOUND=1,NUE=2,TOAD=4,SERPENT=8,ELEPHANT=16,RABBIT=32,DEER=64,OX=128,TIGER=256,WHEEL=512,POISON=1024,FIRE=2048,GUARD=4096,FEATHER=8192;
    public record Kind(String id,int features,float width,float height,double health,double damage,double speed,boolean flying){}
    public static final List<Kind> KINDS=List.of(
        new Kind("great_serpent",SERPENT,1.2f,.8f,32,4,.32,false),new Kind("max_elephant",ELEPHANT,1.8f,2,55,5,.24,false),
        new Kind("rabbit_escape",RABBIT,.35f,.45f,4,0,.45,false),new Kind("round_deer",DEER,.9f,1.8f,28,1,.3,false),
        new Kind("piercing_ox",OX,1.15f,1.35f,38,5,.32,false),new Kind("tiger_funeral",TIGER,1.1f,1.1f,34,5,.38,false),
        new Kind("shadow_mahoraga",WHEEL,1.1f,2.9f,90,9,.3,false),new Kind("divine_dog_totality",HOUND|TIGER,.9f,1.1f,40,6,.4,false),
        new Kind("well_unknown_abyss",NUE|TOAD,.8f,.8f,22,3,.32,true),new Kind("agito",NUE|SERPENT|DEER|TIGER,1.3f,2.7f,72,7,.34,false),
        new Kind("custom_chimera",HOUND,1.1f,1.65f,36,5,.34,false),new Kind("moon_dregs",POISON,.9f,1.4f,24,3,.28,true),
        new Kind("garuda",OX|SERPENT,.8f,.8f,35,5,.36,true),new Kind("rika",GUARD|TIGER,1.4f,2.8f,65,8,.3,false),
        new Kind("paper_heron",FEATHER,.7f,1.1f,16,3,.35,true),new Kind("lantern_moth",FIRE,.75f,.65f,14,3,.3,true),
        new Kind("iron_tortoise",GUARD,1.25f,.7f,48,2,.2,false),new Kind("grave_hound",HOUND|POISON,.8f,.85f,25,4,.37,false));
    public static final Map<Identifier,EntityType<ShadowBeastEntity>> TYPES=new LinkedHashMap<>();
    private static final Map<EntityType<ShadowBeastEntity>,Kind> BY_TYPE=new HashMap<>();
    private static final Map<UUID,Long> HEAL_READY=new HashMap<>();
    static {
        for(var k:KINDS){var type=JjkEntities.register(k.id(),EntityType.Builder.<ShadowBeastEntity>of((t,l)->new ShadowBeastEntity(t,l,k),MobCategory.MISC).sized(k.width(),k.height()).clientTrackingRange(10).noSave().noSummon().noLootTable());TYPES.put(com.jjkcraft.JjkCraft.id(k.id()),type);BY_TYPE.put(type,k);}
    }
    private ShadowBeasts(){}
    public static void init(){for(var entry:BY_TYPE.entrySet()){var k=entry.getValue();FabricDefaultAttributeRegistry.register(entry.getKey(),Mob.createMobAttributes().add(Attributes.MAX_HEALTH,k.health()).add(Attributes.ATTACK_DAMAGE,k.damage()).add(Attributes.MOVEMENT_SPEED,k.speed()).add(Attributes.FLYING_SPEED,.5).add(Attributes.FOLLOW_RANGE,28));}
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->HEAL_READY.clear());
        com.jjkcraft.damage.DamageModifiers.add((level,target,source,amount)->{
            if(!(target instanceof net.minecraft.world.entity.player.Player)||amount<=0||source.is(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT)||com.jjkcraft.technique.PersonalPacts.forbids(target,com.jjkcraft.technique.TechniquePact.GUARD))return amount;
            var guards=ShikigamiEntity.ownedBy(level,ShadowBeastEntity.class,target).stream().filter(e->(e.features()&GUARD)!=0&&e.distanceTo(target)<4&&e.isAlive()).toList();
            if(guards.isEmpty())return amount;float redirected=Math.min(5,amount*.25f);guards.getFirst().hurtServer(level,source,redirected);return amount-redirected;
        });}
    public static int features(EntityType<?> type){if(type==JjkEntities.DIVINE_DOG)return HOUND;if(type==JjkEntities.NUE)return NUE;if(type==JjkEntities.TOAD)return TOAD;var k=BY_TYPE.get(type);return k==null?0:k.features();}
    public static boolean healReady(LivingEntity owner){long now=owner.level().getGameTime();if(HEAL_READY.getOrDefault(owner.getUUID(),0L)>now)return false;HEAL_READY.put(owner.getUUID(),now+60);return true;}
}
