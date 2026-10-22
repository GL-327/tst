package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.*;

/** One interpolated charge per caster; packets update it instead of leaving stationary copies. */
public final class ChargeFx {
    private static final class Charge {TechniqueFxPayload event;float updated;Charge(TechniqueFxPayload e,float now){event=e;updated=now;}}
    private static final Map<Integer,Charge> CHARGES=new HashMap<>();
    private ChargeFx() {}
    public static int count(){return CHARGES.size();}
    public static void clear(){CHARGES.clear();}
    public static void stop(int caster){CHARGES.remove(caster);}
    public static Vec3 anchor(Entity e,float partial,Vec3 camera){
        var mc=Minecraft.getInstance();Vec3 look=e.getViewVector(partial),side=look.cross(new Vec3(0,1,0)).normalize();
        if(e instanceof net.minecraft.world.entity.LivingEntity living&&living.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT)side=side.scale(-1);
        Vec3 eye=e==mc.player&&mc.options.getCameraType().isFirstPerson()?camera:e.getPosition(partial).add(0,e.getEyeHeight(),0);
        return eye.add(look.scale(1.25)).add(side.scale(.22)).add(0,-.22,0);
    }
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(SpatialShotPayload.TYPE,(p,c)->shot(p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,m)->clear());
    }
    public static void update(TechniqueFxPayload event,float now){
        var old=CHARGES.get(event.caster());if(old!=null){old.event=event;old.updated=now;return;}
        if(CHARGES.size()>=64)return;
        var charge=new Charge(event,now);CHARGES.put(event.caster(),charge);
        WorldFx.addPriority(f->render(charge,f,true),true);WorldFx.addPriority(f->render(charge,f,false),false);
    }
    private static boolean render(Charge charge,WorldFx.Frame f,boolean solid){
        var e=charge.event;var mc=Minecraft.getInstance();var caster=mc.level==null?null:mc.level.getEntity(e.caster());
        if(CHARGES.get(e.caster())!=charge)return false;
        if(caster==null||!caster.isAlive()||f.time()-charge.updated>13){CHARGES.remove(e.caster(),charge);return false;}
        if(ClientConfig.effectsOff())return true;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);Vec3 at=anchor(caster,partial,f.camera);
        double q=Math.clamp(e.power(),.25,2),r=.12+.23*Math.sqrt(q);int color=e.color();
        boolean purple=e.technique().getPath().contains("purple"),red=e.technique().getPath().contains("red");
        if(solid){core(f,at,r*.62,purple?0xFF503264:red?0xFFFA8068:0xFF2862AD);}
        else {halo(f,at,r*1.6,color,f.time()*.045,.7f);
            if(purple){halo(f,at.add(-r*.6,0,0),r,0xFF4CA9FF,-f.time()*.06,.65f);halo(f,at.add(r*.6,0,0),r,0xFFFF536A,f.time()*.06,.65f);}
            for(int j=0;j<4;j++){double a=j*Math.PI/2+f.time()*.03;Vec3 tail=at.add(Math.cos(a)*r*2,Math.sin(a*2)*r,Math.sin(a)*r*2);f.beam(tail,tail.lerp(at,.75),.012,WorldFx.fade(color,.08f),WorldFx.fade(color,.5f));}
        }return true;
    }
    private static void shot(SpatialShotPayload p){
        var mc=Minecraft.getInstance();if(mc.level==null)return;stop(p.caster());float born=mc.level.getGameTime()+mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        WorldFx.addSolid(f->{float t=(f.time()-born)/Math.max(1,p.ticks());if(t>1)return false;core(f,p.from().lerp(p.to(),Math.clamp(t,0,1)),p.radius()*.55,0xFF111A34);return true;});
        WorldFx.add(f->{float t=(f.time()-born)/Math.max(1,p.ticks());if(t>1)return false;Vec3 at=p.from().lerp(p.to(),Math.clamp(t,0,1));halo(f,at,p.radius(),p.color(),f.time()*.08,.8f);Vec3 tail=p.from().lerp(p.to(),Math.max(0,t-.18));f.beam(tail,at,p.radius()*.4,WorldFx.fade(p.color(),.03f),WorldFx.fade(p.color(),.4f));return true;});
    }
    static void core(WorldFx.Frame f,Vec3 at,double radius,int color){
        int n=f.segments(16);for(int y=0;y<6;y++)for(int x=0;x<n&&f.budgetLeft();x++){
            double a=x*Math.PI*2/n,b=(x+1)*Math.PI*2/n,c=-Math.PI/2+y*Math.PI/6,d=c+Math.PI/6;
            f.quad(point(at,radius,a,c),point(at,radius,b,c),point(at,radius,b,d),point(at,radius,a,d),shade(color,.78+.22*Math.sin(c)),shade(color,.78+.22*Math.sin(d)));
        }
    }
    private static Vec3 point(Vec3 at,double r,double a,double y){return at.add(Math.cos(a)*Math.cos(y)*r,Math.sin(y)*r,Math.sin(a)*Math.cos(y)*r);}
    private static int shade(int color,double factor){return net.minecraft.util.ARGB.color(color>>>24,(int)(((color>>16)&255)*factor),(int)(((color>>8)&255)*factor),(int)((color&255)*factor));}
    static void halo(WorldFx.Frame f,Vec3 at,double radius,int color,double spin,float alpha){
        int n=f.segments(24);for(int i=0;i<n&&f.budgetLeft();i++){double a=i*Math.PI*2/n+spin,b=(i+.83)*Math.PI*2/n+spin;
            Vec3 p=at.add(Math.cos(a)*radius,Math.sin(a)*radius*.72,Math.sin(a*2)*radius*.25),q=at.add(Math.cos(b)*radius,Math.sin(b)*radius*.72,Math.sin(b*2)*radius*.25);
            f.beam(p,q,Math.max(.012,radius*.025),WorldFx.fade(color,alpha),WorldFx.fade(color,alpha*.25f));
        }
    }
}
