package com.jjkcraft.energy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.technique.PersonalizationMath;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.Personalization;

/** Birth potential survives ordinary death and respecs; training is earned with experience. */
public final class SorceryPractice {
    public static final AttachmentType<PracticeState> STATE=AttachmentRegistry.create(JjkCraft.id("sorcery_practice"),
        b->b.persistent(PracticeState.CODEC).copyOnDeath().initializer(()->PracticeState.EMPTY));
    private static final RateLimiter LIMIT=new RateLimiter(3,4);
    private static final Set<UUID> REINFORCED=new HashSet<>();
    private SorceryPractice() { }
    public static PracticeState get(Player p){return p.getAttachedOrCreate(STATE);}
    public static void set(ServerPlayer p,PracticeState state){CursedEnergy.checkpoint(p);p.setAttached(STATE,state);EnergySync.markDirty(p);Personalization.send(p);}
    public static double base(Player p){var s=get(p);return (s.birth()>0?s.birth():com.jjkcraft.config.JjkConfig.server().energy.baseCapacity)+s.trained();}
    public static void ensureBirth(ServerPlayer p) {
        var s=get(p);
        if(s.rolled()){
            if(s.potentialVersion()<PracticeState.CURRENT_POTENTIAL){
                boolean custom=com.jjkcraft.awakening.CharacterAwakening.get(p).path().equals("creative_custom");
                int birth=custom||s.birth()==0?s.birth():350+10*(int)Math.round(Math.clamp((s.birth()-100)/2900.0,0,1)*20);
                int regen=custom?Math.max(1,s.regenLevel()):4+(int)Math.round(Math.clamp((s.regenLevel()-1)/9.0,0,1)*2);
                set(p,s.withPotential(birth,regen));
            }return;
        }
        // Old rolled reserves survive migration; previously uninitialized saves receive real potential.
        int value=s.birth()>0?s.birth():PersonalizationMath.birthReserve(p.getRandom().nextDouble());
        set(p,s.withPotential(value,PersonalizationMath.regenLevel(p.getRandom().nextDouble())));
    }
    public static void newCharacter(ServerPlayer p){REINFORCED.remove(p.getUUID());p.setAttached(STATE,PracticeState.EMPTY);ensureBirth(p);}
    public static boolean reinforced(Player p){return REINFORCED.contains(p.getUUID())&&get(p).active()&&p.isAlive()&&!p.isSpectator();}
    public static int trainingPrice(Player p){return PersonalizationMath.trainingPrice(CursedEnergy.max(p));}
    public static int trainingGain(Player p){return PersonalizationMath.trainingGain(CursedEnergy.max(p));}
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(PersonalizationPayloads.Practice.TYPE,(payload,ctx)->{
            var p=ctx.player();if(p.isAlive()&&!p.isSpectator()&&LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime()))act(p,payload);
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->ensureBirth(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->REINFORCED.remove(handler.getPlayer().getUUID()));
        SorcererLifecycle.addDeathCopier((old,p,policy,cfg)->{p.setAttached(STATE,get(old).withReinforcement(get(old).reinforcement(),false));REINFORCED.remove(old.getUUID());});
        Awakening.addResetHook(p->{var s=get(p);REINFORCED.remove(p.getUUID());set(p,new PracticeState(s.birth(),s.trained(),false,false,100,100,60,s.regenLevel(),s.rolled()));});
        DamageModifiers.add((level,target,source,amount)-> {
            if(source.getEntity() instanceof Player p && reinforced(p))amount*=1+0.35*get(p).output()/100.0;
            if(target instanceof Player p&&reinforced(p))amount/=1+0.45*get(p).output()/100.0;
            return amount;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%10!=0)return;
            for(var p:server.getPlayerList().getPlayers()) {
                var s=get(p);boolean on=s.active()&&s.reinforcement()&&p.isAlive()&&!p.isSpectator()&&!Progression.get(p).heavenlyRestricted();
                double cost=1.5*Math.pow(s.output()/100.0,2);
                if(on&&CursedEnergy.trySpend(p,cost)){REINFORCED.add(p.getUUID());
                    p.level().sendParticles(com.jjkcraft.particle.JjkParticles.of(com.jjkcraft.particle.JjkParticles.REVERSE_GLOW,0x9270E8,0.45f),p.getX(),p.getY()+1,p.getZ(),2,0.25,0.5,0.25,0.01);
                }else{REINFORCED.remove(p.getUUID());if(s.active())set(p,s.withReinforcement(s.reinforcement(),false));}
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->REINFORCED.clear());
    }
    public static boolean act(ServerPlayer p,PersonalizationPayloads.Practice request) {
        if(!Progression.get(p).awakened()||Progression.get(p).heavenlyRestricted())return false;
        var s=get(p);int action=request.action();
        if(action<=2&&!p.isCreative()){p.sendSystemMessage(Component.literal("Train in the world: straw doll for reinforcement, focus stone for RCT, breathing mat for reserves."),true);return false;}
        if(action==4){set(p,s.tune(request.output(),request.healing(),request.threshold()));return true;}
        if(action==3&&s.reinforcement()){set(p,s.withReinforcement(true,!s.active()));return true;}
        if(action==0&&!s.reinforcement()&&pay(p,3)){set(p,s.withReinforcement(true,false));return true;}
        if(action==1)return com.jjkcraft.reverse.ReverseStudy.start(p);
        if(action==2&&s.trained()<10000&&pay(p,trainingPrice(p))){int gain=trainingGain(p);ensureBirth(p);set(p,get(p).withTraining(gain));return true;}
        return false;
    }
    private static boolean pay(ServerPlayer p,int levels){
        if(p.isCreative())return true;
        if(p.experienceLevel<levels){p.sendSystemMessage(Component.literal("Training requires "+levels+" experience levels."),true);return false;}
        p.giveExperienceLevels(-levels);return true;
    }
}
