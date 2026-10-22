package com.jjkcraft.reverse;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.energy.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.ReverseStudyPayload;

/** Three deliberate, interruptible reverse-flow exercises replace buying an instant unlock. */
public final class ReverseStudy {
    public static final int LESSON_TICKS=200,LESSONS=3,LEVELS=3;
    public static final double CE_COST=30;
    public static final AttachmentType<Integer> LESSON=AttachmentRegistry.create(JjkCraft.id("reverse_lessons"),b->b.persistent(Codec.intRange(0,3)).copyOnDeath().initializer(()->0));
    private static final Map<UUID,Run> RUNS=new HashMap<>();
    private static final class Run {boolean physical;final Vec3 anchor;final net.minecraft.server.level.ServerLevel level;int ticks;Run(ServerPlayer p){anchor=p.position();level=p.level();}}
    private ReverseStudy() { }
    public static int completed(ServerPlayer p){return p.getAttachedOrCreate(LESSON);}
    public static boolean running(ServerPlayer p){return RUNS.containsKey(p.getUUID());}
    public static int ticks(ServerPlayer p){var run=RUNS.get(p.getUUID());return run==null?0:run.ticks;}
    private static void send(ServerPlayer p){JjkNetworking.send(p,new ReverseStudyPayload(completed(p),ticks(p),running(p)));}
    public static boolean startInWorld(ServerPlayer p){return start(p,true);}
    public static boolean start(ServerPlayer p){return start(p,false);}
    private static boolean start(ServerPlayer p,boolean physical){
        if(ReverseTechnique.unlocked(p)||running(p)||!com.jjkcraft.sorcerer.Progression.get(p).awakened()||com.jjkcraft.sorcerer.Progression.get(p).heavenlyRestricted()||!SorceryPractice.get(p).reinforcement()||!p.isAlive()||p.isSpectator()||!p.onGround()||PersonalPacts.forbids(p,TechniquePact.HEALING))return false;
        if(!physical&&!p.isCreative()&&p.experienceLevel<LEVELS){p.sendSystemMessage(Component.literal("Each reverse-flow drill requires 3 experience levels and 30 CE."),true);return false;}
        if(!CursedEnergy.trySpend(p,CE_COST))return false;
        var run=new Run(p);run.physical=physical;RUNS.put(p.getUUID(),run);send(p);return true;
    }
    public static void cancel(ServerPlayer p){if(RUNS.remove(p.getUUID())!=null)send(p);}
    /** One authoritative tick, also exercised directly by server GameTests. */
    public static void step(ServerPlayer p){
        var run=RUNS.get(p.getUUID());if(run==null)return;
        if(!p.isAlive()||p.isSpectator()||p.level()!=run.level||!p.onGround()||p.position().distanceToSqr(run.anchor)>.0225||p.isShiftKeyDown()||p.isUsingItem()||TechniqueService.active(p).isPresent()||TechniqueService.chargePercent(p)>0||PersonalPacts.forbids(p,TechniquePact.HEALING)){cancel(p);return;}
        run.ticks++;
        if(run.ticks%20==0)send(p);
        if(run.ticks<LESSON_TICKS)return;
        if(!run.physical&&!p.isCreative()&&p.experienceLevel<LEVELS){cancel(p);return;}
        if(!run.physical&&!p.isCreative())p.giveExperienceLevels(-LEVELS);
        int lesson=Math.min(LESSONS,completed(p)+1);p.setAttached(LESSON,lesson);RUNS.remove(p.getUUID());
        if(lesson==LESSONS){ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true).withAutoHeal(false),true);p.sendSystemMessage(Component.literal("Reverse Cursed Technique learned. Healing and positive-energy output are available in your loadout."));}
        else p.sendSystemMessage(Component.literal("Reverse-flow drill "+lesson+"/3 complete. Use a focus stone for the next exercise."));
        send(p);
    }
    public static void init(){
        TechniqueService.addCastListener((caster,id,d)->{if(caster instanceof ServerPlayer p)cancel(p);});
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(ReverseStudy::send);
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{cancel(p);p.setAttached(LESSON,0);send(p);});
        com.jjkcraft.damage.DamageModifiers.add((level,target,source,amount)->{if(amount>0&&target instanceof ServerPlayer p)cancel(p);return amount;});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->RUNS.remove(h.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())step(p);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->RUNS.clear());
    }
}
