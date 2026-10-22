package com.jjkcraft.vessel;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.domain.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.VesselPayload;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
/** A vessel is a physical attribute, separate from its technique. Incarnation never grants invulnerability. */
public final class CursedVessels {
    public static final AttachmentType<VesselState> STATE=AttachmentRegistry.create(JjkCraft.id("sukuna_vessel"),b->b.persistent(VesselState.CODEC).copyOnDeath().initializer(()->VesselState.EMPTY));
    private record Episode(int phase,long ends,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension){}
    private static final Map<UUID,Episode> EPISODES=new HashMap<>();
    private static final Set<UUID> ACTING=new HashSet<>();
    private CursedVessels(){}
    public static VesselState get(Player p){return p.getAttachedOrCreate(STATE);}
    public static boolean controlled(Player p){var e=EPISODES.get(p.getUUID());return e!=null&&e.phase==2&&p.level().getGameTime()<e.ends;}
    public static boolean acting(Player p){return ACTING.contains(p.getUUID());}
    /** Every pre-possession windup is cancelled on entry; surviving paid windups belong to this controller. */
    public static void resolveOwnedCast(ServerPlayer p,Runnable action){
        if(!controlled(p)){action.run();return;}
        ACTING.add(p.getUUID());try{action.run();}finally{ACTING.remove(p.getUUID());}
    }
    public static int phase(Player p){var e=EPISODES.get(p.getUUID());return e==null?0:e.phase;}
    public static boolean consume(ServerPlayer p){
        var s=get(p);if(!p.isAlive()||s.fingers()>=20||controlled(p))return false;
        boolean born=CharacterTraits.has(p,"cursed_vessel");
        if(!born){var old=CharacterTraits.get(p);var list=new ArrayList<>(old.traits());list.add(JjkCraft.id("cursed_vessel"));CharacterTraits.set(p,new TraitState(true,List.copyOf(list),old.form(),old.hostTechniques(),old.hostHealth()));}
        CursedEnergy.checkpoint(p);
        int fingers=s.fingers()+1,pressure=Math.min(100,s.pressure()+(born?17:38));
        p.setAttached(STATE,new VesselState(fingers,pressure,s.safeUntil()));
        String[] moves={"dismantle","cleave","shrine_furnace"};int[] thresholds={1,5,10};
        for(int i=0;i<moves.length;i++)if(fingers>=thresholds[i])TechniqueService.grant(p,JjkCraft.id(moves[i]));
        if(fingers>=15&&DomainService.state(p).known().isEmpty())DomainService.setState(p,DomainService.state(p).withKnown(Optional.of(JjkCraft.id("malevolent_shrine"))));
        CursedCombat.restoreStamina(p);com.jjkcraft.energy.EnergySync.markDirty(p);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Sukuna's presence deepens: "+fingers+" / 20 fingers. Overfeeding, injury and exhaustion weaken your control."));
        if(pressure>=85&&p.level().getGameTime()>=s.safeUntil())begin(p,2,Math.min(400,160+fingers*12));
        else if(pressure>=34)begin(p,1,180+fingers*8);
        sync(p);return true;
    }
    public static void begin(ServerPlayer p,int phase,int ticks){
        if(!p.isAlive()||get(p).fingers()==0||phase<1||phase>2)return;
        if(phase==1&&controlled(p))return;
        if(phase==2){CursedCombat.clearActions(p);TechniqueService.cancel(p);DomainService.close(p);}
        EPISODES.put(p.getUUID(),new Episode(phase,p.level().getGameTime()+Math.clamp(ticks,20,400),p.level().dimension()));sync(p);
    }
    public static void end(ServerPlayer p){
        var old=EPISODES.remove(p.getUUID());ACTING.remove(p.getUUID());
        if(old!=null&&old.phase==2){TechniqueService.cancel(p);CursedCombat.clearActions(p);DomainService.close(p);p.setDeltaMovement(Vec3.ZERO);p.needsSync=true;}
        var s=get(p);p.setAttached(STATE,new VesselState(s.fingers(),Math.min(25,s.pressure()),p.level().getGameTime()+2400));sync(p);
    }
    public static void sync(ServerPlayer p){
        var e=EPISODES.get(p.getUUID());var s=get(p);int remaining=e==null?0:(int)Math.max(0,e.ends-p.level().getGameTime());
        List<Integer> falseTargets=e==null||e.phase!=1?List.of():p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(20),t->t!=p&&t.isAlive()&&!(t instanceof net.minecraft.world.entity.monster.Monster)&&!t.isSpectator()).stream().sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(5).map(Entity::getId).toList();
        JjkNetworking.send(p,new VesselPayload(s.fingers(),s.pressure(),remaining==0?0:e.phase,remaining,falseTargets));
    }
    private static void ravage(ServerPlayer p){
        var target=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24),t->t!=p&&t.isAlive()&&!t.isSpectator()&&!(t instanceof net.minecraft.world.entity.decoration.ArmorStand)&&(!(t instanceof Player other)||!other.isCreative()&&p.canHarmPlayer(other))).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if(target==null)return;
        Vec3 toward=target.getEyePosition().subtract(p.getEyePosition());
        p.setYRot((float)Math.toDegrees(Math.atan2(-toward.x,toward.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(toward.y,toward.horizontalDistance())));p.setYHeadRot(p.getYRot());
        long now=p.level().getGameTime();
        // Server players ordinarily receive vertical travel from the client. During possession
        // simulate gravity and a one-block hop here, because movement packets are deliberately ignored.
        Vec3 step=toward.multiply(1,0,1).normalize().scale(p.distanceTo(target)>2?(CursedCombat.interrupted(p)?.07:.22):0);
        double vertical=p.horizontalCollision&&p.onGround()?.42:p.onGround()?-.08:Math.max(-.8,p.getDeltaMovement().y-.08);
        p.move(MoverType.SELF,new Vec3(step.x,vertical,step.z));p.setDeltaMovement(0,p.verticalCollision?0:vertical,0);p.teleportTo(p.getX(),p.getY(),p.getZ());
        ACTING.add(p.getUUID());try{
            CursedCombat.restoreStamina(p);
            if(p.distanceTo(target)<3.2){var move=JjkCraft.id(now%3==0?"hook":"jab");if(!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty())CursedCombat.start(p,move,toward);else CursedCombat.startPunch(p,move,toward,CursedCombat.currentHand(p)<0?1:-1);}
            if(now%20==0){String move=get(p).fingers()>=10&&p.distanceTo(target)>5?"shrine_furnace":get(p).fingers()>=5&&p.distanceTo(target)<4?"cleave":"dismantle";TechniqueService.cast(p,JjkCraft.id(move),-1);}
            if(get(p).fingers()>=15&&now%80==0&&DomainService.active(p).isEmpty())DomainService.expand(p,JjkCraft.id("malevolent_shrine"),com.jjkcraft.core.domain.DomainMath.Variant.BARRIERLESS,true);
        }finally{ACTING.remove(p.getUUID());}
    }
    public static void init(){
        CursedEnergy.addCapacityModifier((entity,max)->entity instanceof Player p?max*(1+get(p).fingers()*.06):max);
        TechniqueService.addCastBlocker((entity,id,d)->entity instanceof Player p&&controlled(p)&&!acting(p)?CastFailure.STUNNED:null);
        DomainService.addExpandBlocker(e->e instanceof Player p&&controlled(p)&&!acting(p)?CastFailure.STUNNED:null);
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(CursedVessels::sync);
        com.jjkcraft.sorcerer.SorcererLifecycle.addDeathCopier((old,p,policy,cfg)->{EPISODES.remove(old.getUUID());ACTING.remove(old.getUUID());var s=get(old);p.setAttached(STATE,new VesselState(s.fingers(),0,p.level().getGameTime()+2400));});
        ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
            if(e instanceof ServerPlayer p)end(p);
            if(e instanceof com.jjkcraft.curse.CurseEntity curse&&com.jjkcraft.sorcerer.Progression.gradeRank(e.registryAccess(),e)>=com.jjkcraft.grade.Grades.rank(e.registryAccess(),JjkCraft.id("special_grade"))&&e.getRandom().nextInt(120)==0)curse.spawnAtLocation((net.minecraft.server.level.ServerLevel)e.level(),new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.SUKUNAS_FINGER));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{EPISODES.remove(h.getPlayer().getUUID());ACTING.remove(h.getPlayer().getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{EPISODES.clear();ACTING.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            long now=p.level().getGameTime();var episode=EPISODES.get(p.getUUID());
            if(episode!=null){if(!p.isAlive()||p.isSpectator()||episode.dimension!=p.level().dimension()||now>=episode.ends){end(p);continue;}if(episode.phase==2)ravage(p);}
            if(now%20==0){var s=get(p);if(s.fingers()>0&&!controlled(p)){
                boolean strained=p.getHealth()<p.getMaxHealth()*.35||CursedEnergy.current(p)<Math.min(20,CursedEnergy.max(p)*.08);
                int pressure=Math.clamp(s.pressure()+(strained?2:now%100==0?-2:0),0,100);p.setAttached(STATE,new VesselState(s.fingers(),pressure,s.safeUntil()));
                if(now>=s.safeUntil()&&pressure>=85)begin(p,2,Math.min(400,160+s.fingers()*12));
                else if(episode==null&&pressure>=40&&p.getRandom().nextInt(12)==0)begin(p,1,200);
            }sync(p);}
        }});
    }
}
