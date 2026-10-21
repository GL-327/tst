package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.behavior.MangaAbilityBehavior;

/** Spatial signatures for cuts, speech, frames, souls, construction and every original material family. */
final class MaterialFx {
    private static final Map<String,Float> RECENT=new HashMap<>();
    static void clear(){RECENT.clear();}
    static boolean event(TechniqueFxPayload fx,int color,float now){
        var def=ClientTechniques.definition(fx.technique()).orElse(null);if(def==null)return false;String name=fx.technique().getPath(),family="";
        if(def.behavior() instanceof MangaAbilityBehavior m&&m.ability().startsWith("original/"))family=m.ability().split("/")[1];
        String kind=!family.isEmpty()?family:name.contains("projection")||name.contains("frame_")?"frames":name.contains("soul")||name.contains("transfigur")?"soul":name.contains("cleave")||name.contains("dismantle")||name.contains("slash")||name.contains("ratio")?"cut":name.contains("speech")||name.contains("crumble")||name.contains("dont_move")||name.contains("blast_away")?"speech":name.contains("straw")||name.contains("resonance")||name.contains("hairpin")||name.contains("thread")?"suture":name.contains("electric")||name.contains("lightning")||name.contains("return_stroke")?"electric":name.contains("construct")||name.contains("tool_")||name.contains("granite")?"metal":name.contains("sky_")||name.contains("mirror")||name.contains("swap")||name.contains("boogie")?"glass":"";
        if(kind.isEmpty())return false;var phase=fx.phase();if(!Set.of(TechniqueFxPayload.Phase.WINDUP,TechniqueFxPayload.Phase.CAST,TechniqueFxPayload.Phase.HIT,TechniqueFxPayload.Phase.BURST,TechniqueFxPayload.Phase.FIELD).contains(phase))return false;
        String key=fx.caster()+":"+name+":"+phase;if(now-RECENT.getOrDefault(key,-999f)<3)return true;RECENT.put(key,now);RECENT.values().removeIf(t->now-t>240);
        var mc=Minecraft.getInstance();if(mc.level==null)return true;var caster=mc.level.getEntity(fx.caster());Vec3 direction=fx.direction().lengthSqr()>.01&&phase!=TechniqueFxPayload.Phase.FIELD?fx.direction().normalize():caster==null?new Vec3(0,0,1):caster.getLookAngle();
        Vec3 origin=phase==TechniqueFxPayload.Phase.WINDUP&&caster!=null?caster.getEyePosition().add(direction):fx.origin();float life=phase==TechniqueFxPayload.Phase.FIELD?(float)Math.clamp(fx.direction().y,10,180):phase==TechniqueFxPayload.Phase.WINDUP?10:phase==TechniqueFxPayload.Phase.BURST?24:14;
        double radius=phase==TechniqueFxPayload.Phase.FIELD?Math.clamp(Math.abs(fx.direction().x),.5,12):phase==TechniqueFxPayload.Phase.BURST?Math.clamp(Math.abs(fx.direction().x),1,8):1.4;
        final String material=kind;final Vec3 at=origin,axis=direction;
        WorldFx.add(f->{float t=(f.time()-now)/life;if(t>1)return false;if(t<0)return true;if(f.camera.distanceToSqr(at)>96*96)return true;float alpha=(1-t)*.68f;double r=radius*(.45+t*.7);int c=WorldFx.fade(color,alpha);Vec3 side=axis.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);Vec3 up=side.cross(axis).normalize();
            switch(material){
                case "cut","porcelain"->{for(int line=0;line<(material.equals("cut")?3:5);line++){Vec3 last=null;for(int i=0;i<f.segments(24);i++){double a=(i/(double)f.segments(24)-.5)*2.5+t*.4;Vec3 p=at.add(side.scale(Math.sin(a)*r)).add(up.scale(Math.cos(a)*r*.28+(line-1)*.25)).add(axis.scale((line-1)*.22));if(last!=null)f.beam(last,p,.028,c,WorldFx.fade(color,alpha*.1f));last=p;}}}
                case "frames","echo_memory","stencil"->{int n=f.segments(8);for(int i=0;i<n;i++){Vec3 center=at.add(axis.scale((i*.65+t*2))).add(side.scale(Math.sin(i+t)*.25));frame(f,center,side,up,.45+i*.05,.75,c);if(material.equals("frames"))for(int j=0;j<3;j++)f.beam(center.add(side.scale(-.55)).add(up.scale(-.5+j*.5)),center.add(side.scale(-.45)).add(up.scale(-.5+j*.5)),.04,c,c);}}
                case "speech","tollbell"->{for(int i=0;i<5;i++){Vec3 center=at.add(axis.scale(i*.8+t*2));for(int j=0;j<f.segments(24);j++){double a=j*Math.PI*2/f.segments(24),b=(j+.65)*Math.PI*2/f.segments(24);f.beam(center.add(side.scale(Math.cos(a)*r*(.4+i*.17))).add(up.scale(Math.sin(a)*r*(.4+i*.17))),center.add(side.scale(Math.cos(b)*r*(.4+i*.17))).add(up.scale(Math.sin(b)*r*(.4+i*.17))),.025,WorldFx.fade(color,alpha*(1-i*.15f)),0);}}}
                case "electric","lodestone","metal"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 from=at.add(side.scale(Math.cos(a)*r*.3)).add(up.scale(Math.sin(a)*r*.3)),to=at.add(side.scale(Math.cos(a+t)*r)).add(up.scale(Math.sin(a+t)*r)).add(axis.scale(Math.sin(a*2)*r));Vec3 mid=from.lerp(to,.5).add(up.scale(Math.sin(i*13+t*30)*.3));f.beam(from,mid,.035,c,WorldFx.fade(color,alpha*.3f));f.beam(mid,to,.02,c,0);if(material.equals("metal"))frame(f,to,side,up,.12,.22,c);}}
                case "glass","salt","sand","resin","kintsugi"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 p=at.add(Math.cos(a)*r,Math.sin(a*2)*r*.7,Math.sin(a)*r);Vec3 top=p.add(up.scale(.3+i%3*.1)),left=p.add(side.scale(-.16)),right=p.add(side.scale(.16)),bottom=p.subtract(up.scale(.3));f.quad(top,left,bottom,right,WorldFx.fade(color,alpha*.18f),WorldFx.fade(color,alpha*.04f));f.beam(top,left,.025,c,c);f.beam(top,right,.025,c,c);f.beam(left,bottom,.02,c,0);f.beam(right,bottom,.02,c,0);}}
                case "soul","suture","thorn","ink"->{for(int i=0;i<f.segments(10);i++){Vec3 last=at;for(int j=0;j<9;j++){double a=i*2.399+j*.27+t*3;Vec3 p=at.add(Math.cos(a)*r*j/9,Math.sin(a*2)*r*.5,Math.sin(a)*r*j/9);f.beam(last,p,material.equals("ink")?.07:.025,WorldFx.fade(color,alpha*(1-j*.08f)),0);last=p;}if(material.equals("thorn"))f.beam(last,last.add(0,.3,0),.03,c,0);}}
                case "origami","carrion"->{for(int i=0;i<f.segments(10);i++){double a=i*2.399+t*2;Vec3 p=at.add(Math.cos(a)*r,Math.sin(a*2)*r*.4,Math.sin(a)*r);Vec3 tip=p.add(axis.scale(.3)),fold=p.add(up.scale(.18));f.quad(tip,p.add(side.scale(-.3)),fold,tip,WorldFx.fade(color,alpha*.5f),c);f.quad(tip,fold,p.add(side.scale(.3)),tip,c,WorldFx.fade(color,alpha*.15f));}}
                case "wax","rust"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 p=at.add(Math.cos(a)*r,(1-t)*.9,Math.sin(a)*r);f.beam(p,p.add(0,-.2-t*.5,0),.07,c,0);f.beam(p,p.add(Math.sin(a)*.1,.15,Math.cos(a)*.1),.025,WorldFx.fade(color,alpha*.4f),0);}}
                case "orbit","pressure","momentum"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 p=at.add(Math.cos(a)*r,Math.sin(a*2)*r*.3,Math.sin(a)*r);Vec3 q=at.add(Math.cos(a+t*2)*r*.45,Math.sin(a*2+t)*r*.2,Math.sin(a+t*2)*r*.45);f.beam(p,q,.025,c,0);f.beam(p,p.add(axis.scale(.3)),.045,WorldFx.fade(color,alpha*.5f),0);}}
                default->{ }
            }return true;
        });return true;
    }
    private static void frame(WorldFx.Frame f,Vec3 p,Vec3 side,Vec3 up,double w,double h,int c){Vec3 a=p.add(side.scale(-w)).add(up.scale(-h)),b=p.add(side.scale(w)).add(up.scale(-h)),d=p.add(side.scale(-w)).add(up.scale(h)),e=p.add(side.scale(w)).add(up.scale(h));f.beam(a,b,.02,c,c);f.beam(b,e,.02,c,c);f.beam(e,d,.02,c,c);f.beam(d,a,.02,c,c);}
}
