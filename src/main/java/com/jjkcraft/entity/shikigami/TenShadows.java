package com.jjkcraft.entity.shikigami;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.*;

/** Persistent subjugation unlocks, transient hostile trials, and a single server-side cast gate. */
public final class TenShadows {
    public static final Set<Identifier> BEASTS=Set.of("divine_dog","nue","toad","great_serpent","max_elephant","rabbit_escape","round_deer","piercing_ox","tiger_funeral","shadow_mahoraga").stream().map(JjkCraft::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    public static final AttachmentType<List<Identifier>> TAMED=AttachmentRegistry.create(JjkCraft.id("subjugated_shadows"),b->b.persistent(Identifier.CODEC.listOf()).copyOnDeath().initializer(List::of));
    private record Trial(ServerLevel level,UUID beast,Identifier kind) { }
    private static final Map<UUID,Trial> TRIALS=new HashMap<>();
    private TenShadows() { }
    public static List<Identifier> tamed(Player p){return p.getAttachedOrCreate(TAMED);}
    public static String trial(Player p){var trial=TRIALS.get(p.getUUID());return trial==null?"":trial.kind.toString();}
    public static int remaining(Player p){var t=TRIALS.get(p.getUUID());return t!=null&&t.level.getEntity(t.beast) instanceof ShikigamiEntity beast?Math.max(0,beast.lifetime()):0;}
    public static double distance(Player p){var t=TRIALS.get(p.getUUID());var e=t==null?null:t.level.getEntity(t.beast);return e==null?0:p.distanceTo(e);}
    public static boolean canSummon(Player p,Identifier kind){return !BEASTS.contains(kind)||p.isCreative()||tamed(p).contains(kind);}
    public static boolean allowed(Player p,TechniqueDefinition d){
        if(d.behavior() instanceof SummonBehavior b)return canSummon(p,b.entity());
        if(d.behavior() instanceof FusionBehavior b){
            var components=b.components().isEmpty()?p.getAttachedOrCreate(ShikigamiFusion.RECIPE):b.components();
            return components.stream().allMatch(id->canSummon(p,id));
        }return true;
    }
    public static void init(){
        TechniqueService.addCastBlocker((caster,id,d)->caster instanceof Player p&&!allowed(p,d)?CastFailure.UNTAMED:CastFailure.NONE);
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{cancel(p);p.setAttached(TAMED,List.of());});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->cancel(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->TRIALS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var id:List.copyOf(TRIALS.keySet())){
                var t=TRIALS.get(id);var p=server.getPlayerList().getPlayer(id);var e=t.level.getEntity(t.beast);
                if(p==null||!p.isAlive()||p.isSpectator()||p.level()!=t.level||e==null||!e.isAlive()||p.distanceTo(e)>48){
                    TRIALS.remove(id);if(e!=null)e.discard();if(p!=null)TechniqueFamilies.open(p,JjkCraft.id("ten_shadows"),false,"Ritual ended: stay alive, within 48 blocks and in the same dimension.");
                }
                else if(server.getTickCount()%20==0)TechniqueFamilies.open(p,JjkCraft.id("ten_shadows"),false,"");
            }
        });
    }
    public static void cancel(Player p){var t=TRIALS.remove(p.getUUID());if(t!=null){var e=t.level.getEntity(t.beast);if(e!=null)e.discard();}}
    public static String challenge(ServerPlayer p,Identifier move){
        if(!p.isAlive()||p.isSpectator()||!TechniqueService.knows(p,move)||!TechniqueFamilies.access(p,JjkCraft.id("ten_shadows")))return "Learn this Ten Shadows move first.";
        var d=Techniques.get(p.registryAccess(),move).orElse(null);
        if(d==null||!(d.behavior() instanceof SummonBehavior b)||!BEASTS.contains(b.entity()))return "Only the ten base shikigami have subjugation rituals.";
        if(tamed(p).contains(b.entity()))return "Already subjugated. You can summon this shikigami.";
        if(TRIALS.containsKey(p.getUUID()))return "Finish your active ritual first.";
        if(com.jjkcraft.sorcerer.Progression.gradeRank(p.registryAccess(),p)<Techniques.gradeRank(p.registryAccess(),d))return "Reach this move's required grade first.";
        if(p.hasEffect(com.jjkcraft.effect.JjkEffects.STUNNED)||p.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME)||com.jjkcraft.combat.CursedCombat.interrupted(p))return "You cannot begin a ritual while stunned.";
        var created=BuiltInRegistries.ENTITY_TYPE.getValue(b.entity()).create(p.level(),EntitySpawnReason.MOB_SUMMONED);
        if(!(created instanceof ShikigamiEntity beast))return "This shikigami is unavailable.";
        Vec3 aim=p.getLookAngle().multiply(1,0,1).normalize();if(aim.lengthSqr()<.1)aim=new Vec3(0,0,1);
        boolean placed=false;
        for(int n=0;n<12&&!placed;n++){
            Vec3 spot=p.position().add(aim.scale(4+n/5)).add(0,new int[]{0,1,-1,2,-2}[n%5],0);
            beast.snapTo(spot.x,spot.y,spot.z,p.getYRot()+180,0);BlockPos floor=BlockPos.containing(spot).below();
            placed=p.level().noCollision(beast,beast.getBoundingBox())&&!p.level().getBlockState(floor).getCollisionShape(p.level(),floor).isEmpty();
        }
        if(!placed){beast.discard();return "Find clear ground with enough room for this shikigami.";}
        double cost=Math.max(20,d.cast().cost()*.5);
        if(!com.jjkcraft.energy.CursedEnergy.trySpend(p,cost)){beast.discard();return "The ritual needs "+Math.round(cost)+" CE.";}
        beast.beginRitual(p,3600);
        if(!p.level().addFreshEntity(beast)){com.jjkcraft.energy.CursedEnergy.restore(p,cost);return "The ritual could not start.";}
        TRIALS.put(p.getUUID(),new Trial(p.level(),beast.getUUID(),b.entity()));
        return "Ritual begun. Defeat the shikigami yourself; stay within 48 blocks.";
    }
    public static void defeated(ShikigamiEntity beast,ServerPlayer p,boolean valid){
        var trial=TRIALS.get(p.getUUID());if(trial==null||!trial.beast.equals(beast.getUUID()))return;
        TRIALS.remove(p.getUUID());
        if(valid){var next=new ArrayList<>(tamed(p));if(!next.contains(trial.kind))next.add(trial.kind);p.setAttached(TAMED,List.copyOf(next));}
        String message=valid?"Subjugation complete. This shikigami now answers your summons.":"The ritual failed: another combatant intervened or the shikigami was not defeated by you.";
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        TechniqueFamilies.open(p,JjkCraft.id("ten_shadows"),false,message);
    }
}
