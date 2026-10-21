package com.jjkcraft.technique;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-owned choreography. Every movement substep checks the swept body and safe footing. */
public final class ProjectionPlans {
    public static final AttachmentType<ProjectionPlan> PLAN=AttachmentRegistry.create(JjkCraft.id("projection_plan"),b->b.persistent(ProjectionPlan.CODEC).copyOnDeath().initializer(()->ProjectionPlan.DEFAULT));
    private record Stage(Vec3 destination,int action){}
    private static final class Run {final CastContext ctx;final List<Stage> stages;final long expires;final int frameTicks;int index;long next;Run(CastContext c,List<Stage>s){ctx=c;stages=s;expires=c.now()+200;next=c.now();frameTicks=c.technique().behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior b?Math.clamp(b.skipTicks()/2,4,12):7;}}
    private static final Map<UUID,Run> RUNS=new HashMap<>();
    private static final com.jjkcraft.core.RateLimiter EDITS=new com.jjkcraft.core.RateLimiter(2,3);
    private ProjectionPlans(){}
    public static boolean running(LivingEntity p){return RUNS.containsKey(p.getUUID());}
    public static boolean set(ServerPlayer p,ProjectionPlan plan){if(!plan.valid()||(!p.isCreative()&&!TechniqueService.knows(p,JjkCraft.id("projection_sorcery")))||running(p))return false;p.setAttached(PLAN,plan);return true;}
    public static void init(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->RUNS.clear());
        com.jjkcraft.sorcerer.Awakening.addResetHook(p->{p.removeAttached(PLAN);RUNS.remove(p.getUUID());});
        ServerPlayNetworking.registerGlobalReceiver(ProjectionPayloads.Edit.TYPE,(payload,ctx)->{
            var p=ctx.player();int result=!p.isAlive()||p.isSpectator()?5:!EDITS.tryAcquire(p.getUUID(),p.level().getGameTime())?1:payload.read()?0:!payload.plan().valid()?4:running(p)?3:!p.isCreative()&&!TechniqueService.knows(p,JjkCraft.id("projection_sorcery"))?2:set(p,payload.plan())?0:5;
            JjkNetworking.send(p,new ProjectionPayloads.Reply(p.getAttachedOrCreate(PLAN),result==0,payload.request(),result));
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var it=RUNS.entrySet().iterator();it.hasNext();){var r=it.next().getValue();var p=r.ctx.caster();long now=p.level().getGameTime();
            if(p.isRemoved()||!p.isAlive()||p.isShiftKeyDown()||p.isSpectator()||p.level()!=r.ctx.level()||now>r.expires||CursedCombat.interrupted(p)||p.hasEffect(JjkEffects.STUNNED)||p.hasEffect(JjkEffects.FROZEN_FRAME)){it.remove();continue;}
            if(now<r.next||!CursedCombat.currentMove(p).isEmpty())continue;if(r.index>=r.stages.size()){it.remove();continue;}
            var stage=r.stages.get(r.index++);Vec3 from=p.position();if(!sweep(p,stage.destination)){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(JjkEffects.FROZEN_FRAME,8,0));it.remove();continue;}
            var enemy=r.ctx.enemiesInRadius(stage.destination,4).stream().filter(p::hasLineOfSight).findFirst().orElse(null);
            Vec3 aim=enemy==null?r.ctx.look():enemy.getEyePosition().subtract(stage.destination.add(0,p.getEyeHeight(),0)).normalize();
            float yaw=enemy==null?p.getYRot():(float)Math.toDegrees(Math.atan2(-aim.x,aim.z)),pitch=enemy==null?p.getXRot():(float)-Math.toDegrees(Math.asin(aim.y));
            p.teleportTo(r.ctx.level(),stage.destination.x,stage.destination.y,stage.destination.z,Set.of(),yaw,pitch,true);r.ctx.fx(TechniqueFxPayload.Phase.CAST,from.add(0,1,0),stage.destination.subtract(from).normalize(),List.of());
            int action=stage.action;if(action==6&&p instanceof ServerPlayer sp)CursedCombat.input(sp,new CombatPayloads.Input(2,1));
            else if(action>0){String move=switch(action){case 2->"hook";case 3->"uppercut";case 4->"sweep";case 5->"heavy";default->"jab";};if(CursedCombat.start(p,JjkCraft.id(move),aim)&&enemy!=null&&enemy.distanceTo(p)<2.5)enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(JjkEffects.FROZEN_FRAME,Math.min(6,r.frameTicks),0));}
            r.next=now+r.frameTicks;
        }});
    }
    public static Vec3 ground(LivingEntity p,Vec3 point){
        if(!finite(point))return null;var level=p.level();double half=p.getBbWidth()/2.0,best=Double.POSITIVE_INFINITY;Vec3 result=null;
        int bx=(int)Math.floor(point.x-half+.001),ex=(int)Math.floor(point.x+half-.001),bz=(int)Math.floor(point.z-half+.001),ez=(int)Math.floor(point.z+half-.001),base=(int)Math.floor(point.y);
        for(int dy:new int[]{-1,0,-2,1,-3})for(int x=bx;x<=ex;x++)for(int z=bz;z<=ez;z++){
            var pos=new BlockPos(x,base+dy,z);if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getFluidState(pos).isEmpty())continue;
            var shape=level.getBlockState(pos).getCollisionShape(level,pos);if(shape.isEmpty())continue;
            for(var box:shape.toAabbs()){
                if(point.x+half<=x+box.minX||point.x-half>=x+box.maxX||point.z+half<=z+box.minZ||point.z-half>=z+box.maxZ)continue;
                double top=pos.getY()+box.maxY,gap=Math.abs(top-point.y);if(gap>2.01||gap>=best)continue;
                Vec3 at=new Vec3(point.x,top,point.z);if(!level.getFluidState(BlockPos.containing(at.add(0,.01,0))).isEmpty()||!level.noCollision(p,p.getBoundingBox().move(at.subtract(p.position()))))continue;
                best=gap;result=at;if(gap<.0001)return result;
            }
        }return result;
    }
    private static boolean finite(Vec3 at){return Double.isFinite(at.x)&&Double.isFinite(at.y)&&Double.isFinite(at.z);}
    private static boolean clear(LivingEntity p,Vec3 from,Vec3 to){int n=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.2));for(int i=0;i<=n;i++)if(!p.level().noCollision(p,p.getBoundingBox().move(from.lerp(to,i/(double)n).subtract(p.position()))))return false;return true;}
    public static boolean sweep(LivingEntity p,Vec3 to){return sweep(p,p.position(),to);}
    public static boolean sweep(LivingEntity p,Vec3 from,Vec3 to){
        if(!finite(from)||!finite(to))return false;int count=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.25));if(count>96)return false;Vec3 last=from;
        for(int i=1;i<=count;i++){
            Vec3 foot=ground(p,from.lerp(to,i/(double)count));if(foot==null||Math.abs(foot.y-last.y)>1.01)return false;
            Vec3 corner=foot.y>last.y?new Vec3(last.x,foot.y,last.z):new Vec3(foot.x,last.y,foot.z);
            if(!clear(p,last,corner)||!clear(p,corner,foot))return false;last=foot;
        }return Math.abs(last.y-to.y)<.05;
    }
    public static List<Vec3> preview(CastContext ctx,double range){return stages(ctx,range).stream().map(Stage::destination).toList();}
    private static List<Stage> stages(CastContext ctx,double range){
        var p=ctx.caster();Vec3 origin=p.position(),forward=ctx.look().multiply(1,0,1).normalize(),right=new Vec3(forward.z,0,-forward.x);var stages=new ArrayList<Stage>();
        var plan=p instanceof ServerPlayer sp?sp.getAttachedOrCreate(PLAN):ProjectionPlan.DEFAULT;
        if(!plan.automatic()){for(var s:plan.steps()){Vec3 at=origin.add(right.scale(s.right())).add(forward.scale(s.forward()));if(at.distanceTo(origin)>range)return List.of();Vec3 safe=ground(p,at);if(safe==null)return List.of();stages.add(new Stage(safe,s.action()));}return stages;}
        double search=ctx.technique().behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior b?Math.min(range+3,b.radius()):range+3;
        var enemy=ctx.enemiesInCone(search,110).stream().filter(p::hasLineOfSight).findFirst().orElse(null);Vec3 focus=enemy==null?origin.add(forward.scale(Math.min(5,range*.65))):enemy.position();
        if(enemy==null){Vec3 safe=ground(p,focus);if(safe!=null&&sweep(p,safe))stages.add(new Stage(safe,0));return stages;}
        Vec3 facing=focus.subtract(origin).multiply(1,0,1).normalize();if(facing.lengthSqr()<.1)facing=forward;Vec3 lateral=new Vec3(facing.z,0,-facing.x),last=origin;
        int[] actions={1,2,3,5};for(int i=0;i<4;i++){
            double preferred=i%2==0?-.8:.8;Vec3 chosen=null;
            for(double side:new double[]{preferred,-preferred,0,preferred*1.6}){Vec3 at=focus.subtract(facing.scale(1.8)).add(lateral.scale(side));if(at.distanceTo(origin)>range)continue;Vec3 safe=ground(p,at);if(safe!=null&&sweep(p,last,safe)){chosen=safe;break;}}
            if(chosen!=null){stages.add(new Stage(chosen,actions[i]));last=chosen;}
        }return stages;
    }
    public static CastResult cast(CastContext ctx,double range){var route=stages(ctx,Math.clamp(range,1,16));if(route.isEmpty()||running(ctx.caster()))return CastResult.miss(ctx.eye());RUNS.put(ctx.caster().getUUID(),new Run(ctx,route));ctx.fx(TechniqueFxPayload.Phase.BURST,ctx.caster().position(),new Vec3(.8,0,0),List.of());return CastResult.done(ctx.caster().position());}
}
