package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.network.payload.CombatPosePayload;

/** Material-specific, layered volume effects. Geometry budgets and accessibility apply to every layer. */
public final class SpectralFx {
    private static final Map<String,Float> RECENT=new HashMap<>();
    private static final Map<Integer,Long> MELEE=new HashMap<>();
    private SpectralFx() { }
    public static void clear(){RECENT.clear();MELEE.clear();MaterialFx.clear();}
    public static boolean event(TechniqueFxPayload fx,int color,float now){
        if(ClientConfig.effectsOff())return false;
        if(MaterialFx.event(fx,color,now))return true;
        String key=fx.caster()+":"+fx.technique()+":"+fx.phase();
        if(now-RECENT.getOrDefault(key,-1000f)<4)return fx.phase()==TechniqueFxPayload.Phase.FIELD||fx.phase()==TechniqueFxPayload.Phase.WINDUP;
        RECENT.put(key,now);RECENT.values().removeIf(t->now-t>240);
        String name=fx.technique().getPath();
        int style=name.contains("blood")||name.contains("crimson")?1:name.contains("ice")||name.contains("frost")?2:name.contains("flame")||name.contains("volcan")||name.contains("ember")||name.contains("furnace")?3:name.contains("blue")||name.contains("purple")||name.contains("reversal_red")||name.contains("infinity")?4:name.contains("shadow")||name.contains("summon")||name.contains("dreg")||name.contains("fusion")?5:name.contains("heal")||name.contains("reverse_cursed")||name.contains("positive")?6:0;
        boolean field=fx.phase()==TechniqueFxPayload.Phase.FIELD,wind=fx.phase()==TechniqueFxPayload.Phase.WINDUP;
        boolean burst=fx.phase()==TechniqueFxPayload.Phase.BURST,hit=fx.phase()==TechniqueFxPayload.Phase.HIT;
        if(!field&&!wind&&!burst&&!hit&&fx.phase()!=TechniqueFxPayload.Phase.CAST)return false;
        var mc=Minecraft.getInstance();if(mc.level==null)return false;
        var caster=mc.level.getEntity(fx.caster());
        Vec3 origin=wind&&caster!=null?caster.getEyePosition().add(caster.getLookAngle().scale(1)):fx.origin();
        float life=field?(float)Math.clamp(fx.direction().y,8,240):wind?11:burst?22:hit?10:14;
        double radius=field||burst?Math.clamp(Math.abs(fx.direction().x),.5,24):wind?.6:hit?.8:1.2;
        if(style==5)origin=origin.add(0,-.5,0);
        final Vec3 center=origin;final boolean inward=!name.contains("red")&&!burst;
        if(style==4||style==5){
            WorldFx.addSolid(f->{float t=(f.time()-now)/life;if(t>=1)return false;if(t<0)return true;
                if(f.camera.distanceToSqr(center)>96*96)return true;
                sphere(f,center,Math.min(radius*.22,.8)*(wind?.65:1),0x101024,(1-t)*.9f);return true;
            });
        }
        WorldFx.add(f->{
            float t=(f.time()-now)/life;if(t>=1)return false;if(t<0)return true;if(f.camera.distanceToSqr(center)>96*96)return true;
            float alpha=(float)Math.pow(1-t,.7);double r=radius*(field?.8+.05*Math.sin(f.time()*.3):wind?.5+.5*t:.25+1.1*t);
            int segments=f.segments(field?32:24);int c=WorldFx.fade(color,alpha*.58f);
            if(style==4){
                for(int plane=0;plane<3;plane++)ring(f,center,r*(.6+plane*.2),plane,f.time()*(plane==1?-.09:.07),color,alpha*.5f);
                int strands=f.segments(12);for(int j=0;j<strands;j++){
                    Vec3 previous=null;for(int i=0;i<14&&f.budgetLeft();i++){
                        double u=i/13.0,rr=r*(inward?1-u:u),a=j*Math.PI*2/strands+u*4+f.time()*.05;
                        Vec3 next=center.add(Math.cos(a)*rr,Math.sin(u*Math.PI*2+j)*rr*.35,Math.sin(a)*rr);
                        if(previous!=null)f.beam(previous,next,.035+u*.035,c,WorldFx.fade(color,alpha*.12f));previous=next;
                    }
                }
            }else if(style==5){
                ring(f,center,r,1,-f.time()*.08,color,alpha*.8f);ring(f,center,r*.65,1,f.time()*.1,color,alpha*.6f);
                for(int j=0;j<8;j++){double a=j*Math.PI/4;Vec3 base=center.add(Math.cos(a)*r,0,Math.sin(a)*r);f.beam(base,base.add(-Math.cos(a)*.4,t*.9,-Math.sin(a)*.4),.15,WorldFx.fade(0x252034,alpha),c);}
            }else if(style==2){
                for(int j=0;j<f.segments(14);j++){double a=j*2.399;Vec3 direction=new Vec3(Math.cos(a),.3+Math.sin(j)*.6,Math.sin(a));crystal(f,center.add(direction.scale(r)),direction,.13+.14*(1-t),color,alpha);}
            }else if(style==3){
                for(int j=0;j<f.segments(14);j++){double a=j*2.399+f.time()*.09;Vec3 base=center.add(Math.cos(a)*r*.6,0,Math.sin(a)*r*.6);Vec3 mid=base.add(Math.sin(a+t*5)*.2,.4+t,Math.cos(a+t*5)*.2);f.beam(base,mid,.19*(1-t),WorldFx.fade(color,alpha*.8f),WorldFx.fade(0xFFC578,alpha*.2f));f.beam(mid,mid.add(0,.35,0),.06,WorldFx.fade(0xFFDFA7,alpha*.4f),0);}
            }else if(style==1){
                for(int j=0;j<f.segments(12);j++){double a=j*2.399;Vec3 end=center.add(Math.cos(a)*r,Math.sin(a)*r*.65,Math.sin(a*3)*r*.4);f.beam(center.lerp(end,.25),end,.065*(1-t),WorldFx.fade(color,alpha),WorldFx.fade(0x44071C,alpha*.25f));sphere(f,end,.045,color,alpha);}
            }else if(style==6){
                for(int j=0;j<2;j++){Vec3 last=null;for(int i=0;i<segments;i++){double u=i/(double)segments,a=u*Math.PI*4+j*Math.PI+f.time()*.07;Vec3 at=center.add(Math.cos(a)*r*.5,(u-.5)*r*2,Math.sin(a)*r*.5);if(last!=null)f.beam(last,at,.035,WorldFx.fade(color,alpha*.7f),WorldFx.fade(0xDAFFF3,alpha*.5f));last=at;}}
            }else{
                ring(f,center,r,0,t*.8,color,alpha*.5f);ring(f,center,r*.7,2,-t,color,alpha*.4f);
                for(int j=0;j<f.segments(10);j++){double a=j*2.399;Vec3 to=center.add(Math.cos(a)*r,Math.sin(a)*r,Math.cos(a*2)*r*.3);f.beam(center.lerp(to,.7),to,.035,c,0);}
            }
            if(burst)ring(f,center.add(0,.05,0),r*1.6,1,0,color,alpha*.4f);
            return true;
        });
        return field||wind||burst;
    }
    private static void ring(WorldFx.Frame f,Vec3 at,double r,int plane,double spin,int color,float alpha){
        int count=f.segments(32);for(int i=0;i<count&&f.budgetLeft();i++){
            double a=i*Math.PI*2/count+spin,b=(i+.8)*Math.PI*2/count+spin;
            Vec3 p=point(a,r,plane),q=point(b,r,plane);f.beam(at.add(p),at.add(q),.045,WorldFx.fade(color,alpha),WorldFx.fade(color,alpha*.35f));
        }
    }
    private static Vec3 point(double a,double r,int plane){return plane==0?new Vec3(Math.cos(a)*r,Math.sin(a)*r,0):plane==1?new Vec3(Math.cos(a)*r,0,Math.sin(a)*r):new Vec3(0,Math.cos(a)*r,Math.sin(a)*r);}
    private static void sphere(WorldFx.Frame f,Vec3 at,double r,int color,float alpha){
        int n=f.segments(12);for(int band=0;band<4;band++)for(int j=0;j<n&&f.budgetLeft();j++){
            double y0=-Math.PI/2+band*Math.PI/4,y1=y0+Math.PI/4,a=j*Math.PI*2/n,b=(j+1)*Math.PI*2/n;
            Vec3 p=at.add(Math.cos(a)*Math.cos(y0)*r,Math.sin(y0)*r,Math.sin(a)*Math.cos(y0)*r),q=at.add(Math.cos(b)*Math.cos(y0)*r,Math.sin(y0)*r,Math.sin(b)*Math.cos(y0)*r);
            Vec3 s=at.add(Math.cos(b)*Math.cos(y1)*r,Math.sin(y1)*r,Math.sin(b)*Math.cos(y1)*r),t=at.add(Math.cos(a)*Math.cos(y1)*r,Math.sin(y1)*r,Math.sin(a)*Math.cos(y1)*r);f.quad(p,q,s,t,WorldFx.fade(color,alpha),WorldFx.fade(color,alpha*.8f));
        }
    }
    private static void crystal(WorldFx.Frame f,Vec3 at,Vec3 axis,double radius,int color,float alpha){
        Vec3 up=axis.normalize().scale(radius*3),side=axis.cross(new Vec3(0,1,0)).normalize().scale(radius),back=axis.normalize().cross(side).normalize().scale(radius);
        Vec3[] base={at.add(side),at.add(back),at.subtract(side),at.subtract(back)};
        for(int i=0;i<4;i++)f.quad(base[i],base[(i+1)%4],at.add(up),at.add(up),WorldFx.fade(color,alpha*.65f),WorldFx.fade(0xD8F9FF,alpha*.85f));
    }
    public static void melee(CombatPosePayload pose){
        if(ClientConfig.effectsOff()||!Set.of("jab","hook","backfist","heavy","uppercut","sweep","feint").contains(pose.move())||MELEE.getOrDefault(pose.entity(),-1L)==pose.started())return;
        MELEE.put(pose.entity(),pose.started());if(MELEE.size()>256)MELEE.clear();
        WorldFx.add(f->{var mc=Minecraft.getInstance();var entity=mc.level==null?null:mc.level.getEntity(pose.entity());if(entity==null)return false;
            float age=f.time()-pose.started();if(age>pose.startup()+pose.active()+4)return false;if(age<pose.startup()-1)return true;
            double yaw=Math.toRadians(entity.getYRot()),sweep=(age-pose.startup()+1)/Math.max(1,pose.active());
            Vec3 center=entity.position().add(0,pose.move().equals("sweep")?.4:entity.getBbHeight()*.7,0),right=new Vec3(Math.cos(yaw),0,Math.sin(yaw)),forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
            int color=pose.move().equals("feint")?0x526071:pose.move().equals("heavy")?0xDD8499:0x8ED3EC;
            for(int i=0;i<8;i++){double a=(i/8.0-.5)*2+sweep*.8;Vec3 p=center.add(forward.scale(Math.cos(a)*1.4)).add(right.scale(Math.sin(a)*1.4));Vec3 q=center.add(forward.scale(Math.cos(a+.12)*1.4)).add(right.scale(Math.sin(a+.12)*1.4));f.beam(p,q,.06,WorldFx.fade(color,.45f),WorldFx.fade(color,.1f));}return true;
        });
    }
}
