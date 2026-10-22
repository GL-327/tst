package com.jjkcraft.training;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.energy.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.network.*;
import com.jjkcraft.network.payload.TrainingPayload;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

/** Server-observed drills: real contact, measured travel, timed breathing, and paid domain use. */
public final class Training {
    public static final AttachmentType<TrainingState> STATE=AttachmentRegistry.create(JjkCraft.id("physical_training"),b->b.persistent(TrainingState.CODEC).copyOnDeath().initializer(()->TrainingState.EMPTY));
    private static final class Run{Vec3 last;boolean ground;double distance;int breath;long hit=-100,jump=-100,magic=-100;}
    private static final Map<UUID,Run> RUNS=new HashMap<>();
    private static final PlayerSyncQueue.Channel SYNC=PlayerSyncQueue.channel("training",p->JjkNetworking.send(p,new TrainingPayload(get(p))));
    private Training(){}
    public static TrainingState get(Player p){return p.getAttachedOrCreate(STATE);}
    public static boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!p.isPassenger()&&!com.jjkcraft.vessel.CursedVessels.controlled(p)&&Progression.get(p).awakened();}
    public static void award(ServerPlayer p,String key,int amount){
        if(!eligible(p)||!TrainingState.SKILLS.contains(key)||amount<=0)return;
        var old=get(p);var next=old.add(key,amount);p.setAttached(STATE,next);SYNC.markDirty(p);
        if(next.level(key)>old.level(key)){refresh(p);p.sendSystemMessage(Component.literal("Training · "+key+" "+next.level(key)+" / 20"),true);}
        if(List.of("output","control","efficiency","domain").contains(key)){
            GrowthPath path=GrowthPath.valueOf(key.toUpperCase(Locale.ROOT));
            if(next.xp(key)/120>Progression.get(p).growth().get(path))Progression.spendGrowth(p,path);
        }
    }
    public static void contact(ServerPlayer p,boolean weapon,boolean technique){
        if(!eligible(p))return;var r=RUNS.computeIfAbsent(p.getUUID(),x->new Run());long now=p.level().getGameTime();
        if(technique){if(now-r.magic<10)return;r.magic=now;award(p,"control",3);award(p,"output",2);return;}
        if(now-r.hit<8)return;r.hit=now;award(p,weapon?"weaponry":"strength",3);award(p,"endurance",1);
        if(!Progression.get(p).heavenlyRestricted()&&CursedEnergy.trySpend(p,1)){
            award(p,"control",1);if(!SorceryPractice.get(p).reinforcement()&&get(p).xp("control")>=40){SorceryPractice.set(p,SorceryPractice.get(p).withReinforcement(true,false));p.sendSystemMessage(Component.literal("Reinforcement learned through contact drills. Toggle CE fists with your configured key."));}
        }
    }
    public static void refresh(ServerPlayer p){
        var s=get(p);attribute(p,Attributes.ATTACK_DAMAGE,"strength",s.level("strength")*.12,AttributeModifier.Operation.ADD_VALUE);
        attribute(p,Attributes.MAX_HEALTH,"endurance",s.level("endurance")*.4,AttributeModifier.Operation.ADD_VALUE);
        attribute(p,Attributes.MOVEMENT_SPEED,"agility",s.level("agility")*.003,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    private static void attribute(ServerPlayer p,net.minecraft.core.Holder<Attribute> type,String key,double value,AttributeModifier.Operation operation){var a=p.getAttribute(type);if(a==null)return;var id=JjkCraft.id("training/"+key);if(value==0)a.removeModifier(id);else a.addOrUpdateTransientModifier(new AttributeModifier(id,value,operation));}
    public static void step(ServerPlayer p){
        var r=RUNS.computeIfAbsent(p.getUUID(),x->new Run());Vec3 here=p.position();
        if(!eligible(p)){r.last=here;r.breath=0;r.distance=0;r.ground=p.onGround();return;}
        boolean moved=r.last!=null&&here.distanceToSqr(r.last)>.0004;
        if(r.last!=null&&p.onGround()&&p.isSprinting()&&here.distanceToSqr(r.last)<1.44){r.distance+=here.distanceTo(r.last);if(r.distance>=4){r.distance-=4;award(p,"endurance",1);award(p,"agility",1);}}
        var below=p.level().getBlockState(p.blockPosition().below()).getBlock();var at=p.level().getBlockState(p.blockPosition()).getBlock();
        boolean mat=at==com.jjkcraft.block.JjkBlocks.TRAINING_MAT||below==com.jjkcraft.block.JjkBlocks.TRAINING_MAT;
        if(mat&&!moved&&p.isShiftKeyDown()&&p.onGround())r.breath=Math.min(121,r.breath+1);
        else if(r.breath>0){
            if(mat&&!moved&&!p.isShiftKeyDown()&&r.breath>=40&&r.breath<=100&&!Progression.get(p).heavenlyRestricted()&&CursedEnergy.trySpend(p,5)){
                int before=get(p).xp("reserves")/60;award(p,"reserves",6);award(p,"efficiency",4);var s=SorceryPractice.get(p);
                if(get(p).xp("reserves")/60>before)SorceryPractice.set(p,s.withTraining(SorceryPractice.trainingGain(p)));
                p.sendSystemMessage(Component.literal("Breath complete · reserves / efficiency"),true);
            }r.breath=0;
        }
        if(mat&&r.breath%20==0&&r.breath>0)p.sendSystemMessage(Component.literal(r.breath<40?"Breathe in · hold sneak":"Breathe out · release sneak now"),true);
        if(below==com.jjkcraft.block.JjkBlocks.BALANCE_BEAM&&moved&&p.onGround()&&p.level().getGameTime()%10==0)award(p,"agility",2);
        r.last=here;r.ground=p.onGround();
    }
    public static void init(){
        SorcererLifecycle.addFullSync(p->{refresh(p);SYNC.sendNow(p);});
        SorcererLifecycle.addDeathCopier((old,p,policy,cfg)->{p.setAttached(STATE,get(old));RUNS.remove(old.getUUID());});
        Awakening.addResetHook(p->{RUNS.remove(p.getUUID());p.setAttached(STATE,TrainingState.EMPTY);refresh(p);SYNC.markDirty(p);});
        com.jjkcraft.domain.DomainService.addCloseListener(d->{if(d.caster() instanceof ServerPlayer p&&d.tookHold()&&d.activeTicks()>=80)award(p,"domain",12);});
        TechniqueService.addCastListener((e,id,d)->{if(e instanceof ServerPlayer p&&eligible(p)&&CursedEnergy.current(p)<CursedEnergy.max(p)*.8)award(p,"output",1);});
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())step(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->RUNS.remove(h.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->RUNS.clear());
    }
}
