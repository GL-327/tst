package com.jjkcraft.technique.field;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.technique.*;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.*;

/** A bounded traveling release, followed by the ordinary attraction or repulsion field. */
public final class SpatialShots {
    private record Shot(CastContext ctx,Vec3 from,Vec3 to,double radius,double strength,double dps,double burst,double blast,int duration,int flight,long born) {}
    private static final List<Shot> SHOTS=new ArrayList<>();
    private SpatialShots() {}
    public static int active(){return SHOTS.size();}
    public static Vec3 origin(CastContext c){return c.eye().add(c.look().scale(1.25));}
    public static int flight(Vec3 from,Vec3 to){return Math.max(2,(int)Math.ceil(from.distanceTo(to)/1.8));}
    public static void visual(CastContext c,Vec3 from,Vec3 to,double radius,int ticks){
        var payload=new SpatialShotPayload(c.caster().getId(),c.techniqueId(),from,to,(float)radius,ticks,c.technique().visuals().color());
        for(var p:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(c.caster()))JjkNetworking.send(p,payload);
        if(c.caster() instanceof net.minecraft.server.level.ServerPlayer p)JjkNetworking.send(p,payload);
    }
    public static void launch(CastContext c,Vec3 to,double radius,double strength,double dps,double burst,double blast,int duration){
        if(SHOTS.size()>=64)SHOTS.removeFirst();
        Vec3 from=origin(c);int flight=flight(from,to);
        boolean maximum=c.techniqueId().getPath().contains("maximum");
        visual(c,from,to,Math.clamp(radius*(maximum?.65:.38),.35,maximum?8:3.5),flight);
        SHOTS.add(new Shot(c,from,to,radius,strength,dps,burst,blast,duration,flight,c.now()));
    }
    public static void init(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->SHOTS.clear());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var it=SHOTS.iterator();it.hasNext();){var shot=it.next();var c=shot.ctx;
                if(c.caster().isRemoved()||!c.caster().isAlive()||c.caster().level()!=c.level()){it.remove();continue;}
                if(c.now()-shot.born>=shot.flight){
                    var end=c.level().clip(new net.minecraft.world.level.ClipContext(shot.from,shot.to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,c.caster())).getLocation();
                    SpatialFields.place(c,end,shot.radius,shot.strength,shot.dps,shot.burst,shot.blast,shot.duration);
                    c.fx(TechniqueFxPayload.Phase.FIELD,end,new Vec3(shot.radius,shot.duration,shot.strength),List.of());it.remove();
                }
            }
        });
    }
}
