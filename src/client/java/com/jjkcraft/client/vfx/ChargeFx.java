package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.*;

/** A single camera-centered volume follows each charging caster, including between packets. */
public final class ChargeFx {
    private static final class Charge {TechniqueFxPayload event;float updated;Charge(TechniqueFxPayload e,float now){event=e;updated=now;}}
    private static final Map<Integer,Charge> CHARGES=new HashMap<>();
    private ChargeFx() {}
    public static int count(){return CHARGES.size();}
    public static void clear(){CHARGES.clear();}
    public static void stop(int caster){CHARGES.remove(caster);}
    public static Vec3 anchor(Entity e,float partial,Vec3 camera){
        var mc=Minecraft.getInstance();Vec3 eye=e==mc.player&&mc.options.getCameraType().isFirstPerson()?camera:e.getPosition(partial).add(0,e.getEyeHeight(),0);
        return eye.add(e.getViewVector(partial).scale(1.25));
    }
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(SpatialShotPayload.TYPE,(p,c)->shot(p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,m)->clear());
    }
    public static void update(TechniqueFxPayload event,float now){
        var old=CHARGES.get(event.caster());if(old!=null){old.event=event;old.updated=now;return;}
        if(CHARGES.size()>=64)return;
        var charge=new Charge(event,now);CHARGES.put(event.caster(),charge);
        WorldFx.addMaterial(f->render(charge,f));
    }
    private static boolean render(Charge charge,WorldFx.Frame f){
        var e=charge.event;var mc=Minecraft.getInstance();var caster=mc.level==null?null:mc.level.getEntity(e.caster());
        if(CHARGES.get(e.caster())!=charge)return false;
        if(caster==null||!caster.isAlive()||f.time()-charge.updated>13){CHARGES.remove(e.caster(),charge);return false;}
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 at=caster==mc.player&&mc.options.getCameraType().isFirstPerson()?f.aimPoint(1.25):anchor(caster,partial,f.camera);
        double q=Math.clamp(e.power(),.25,2),r=.13+.20*Math.sqrt(q);
        String name=e.technique().getPath();boolean spatial=SpectralFx.spatial(name);
        // Authored colors may contain an opaque ARGB byte and therefore be negative.
        // Only -1 is the absent-color sentinel; otherwise Blue/Red/Purple lose their hues.
        int color=e.color()==-1?com.jjkcraft.client.state.ClientTechniques.definition(e.technique()).map(d->d.visuals().color()).orElse(0x9ebaff):e.color();
        if(spatial){
            int tile=EnergySprites.spatialTile(name);
            EnergySprites.orb(f,at,r,tile,color,.98f);
            if(tile==2){
                double spread=r*(.50-.18*Math.min(1,q));Vec3 right=f.viewRight.scale(spread);
                VolumeMesh.orb(f,at.subtract(right),new Vec3(r*.3,r*.3,r*.3),0,0x499FFF,.65f,ClientConfig.reducedMotion()?0:-f.time()*.03);
                VolumeMesh.orb(f,at.add(right),new Vec3(r*.3,r*.3,r*.3),1,0xFF4261,.65f,ClientConfig.reducedMotion()?0:f.time()*.03);
            }
        }else{
            int tile=TechniqueMaterials.tile(e.technique());
            VolumeMesh.orb(f,at,new Vec3(r,r*1.1,r),tile,color,.85f,ClientConfig.reducedMotion()?0:f.time()*.016);
            f.sprite(at,r*1.6,r*1.6,4,color,.15f,ClientConfig.reducedMotion()?0:-f.time()*.018);
        }
        return true;
    }
    private static void shot(SpatialShotPayload p){
        var mc=Minecraft.getInstance();if(mc.level==null)return;stop(p.caster());float born=mc.level.getGameTime()+mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        WorldFx.addMaterial(f->{float t=(f.time()-born)/Math.max(1,p.ticks());if(t>1)return false;
            Vec3 at=p.from().lerp(p.to(),Math.clamp(t,0,1));
            EnergySprites.orb(f,at,p.radius()*.60,EnergySprites.spatialTile(p.technique().getPath()),p.color(),.95f);
            Vec3 axis=p.to().subtract(p.from()).normalize();
            for(int i=1;i<=f.segments(8);i++){
                double length=Math.min(p.from().distanceTo(at),i*p.radius()*.5);
                float alpha=(float)(.34*(1-i/9.0));double r=p.radius()*(.55-i*.035);
                f.sprite(at.subtract(axis.scale(length)),r,r,4,p.color(),alpha,f.time()*.04+i*.4);
            }return true;
        });
    }
}
