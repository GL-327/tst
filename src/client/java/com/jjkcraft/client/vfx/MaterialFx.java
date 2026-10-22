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
        String kind=!family.isEmpty()?family:name.equals("shrine_cage")?"cut_cage":name.contains("projection")||name.contains("frame_")?"frames":name.contains("soul")||name.contains("transfigur")?"soul":name.equals("shrine_web")||name.contains("cleave")||name.contains("dismantle")||name.contains("slash")||name.contains("ratio")?"cut":name.contains("speech")||name.contains("crumble")||name.contains("dont_move")||name.contains("blast_away")?"speech":name.contains("straw")||name.contains("resonance")||name.contains("hairpin")||name.contains("thread")?"suture":name.contains("electric")||name.contains("lightning")||name.contains("return_stroke")?"electric":name.contains("construct")||name.contains("tool_")||name.contains("granite")?"metal":name.contains("sky_")||name.contains("mirror")||name.contains("swap")||name.contains("boogie")?"glass":"";
        if(kind.isEmpty())return false;var phase=fx.phase();if(!Set.of(TechniqueFxPayload.Phase.WINDUP,TechniqueFxPayload.Phase.CAST,TechniqueFxPayload.Phase.HIT,TechniqueFxPayload.Phase.BURST,TechniqueFxPayload.Phase.FIELD).contains(phase))return false;
        String key=fx.caster()+":"+name+":"+phase;if(now-RECENT.getOrDefault(key,-999f)<3)return true;RECENT.put(key,now);RECENT.values().removeIf(t->now-t>240);
        var mc=Minecraft.getInstance();if(mc.level==null)return true;var caster=mc.level.getEntity(fx.caster());Vec3 direction=EffectAim.forward(fx,caster);
        Vec3 origin=phase==TechniqueFxPayload.Phase.WINDUP&&caster!=null?caster.getEyePosition().add(direction):fx.origin();float life=phase==TechniqueFxPayload.Phase.FIELD?(float)Math.clamp(fx.direction().y,10,180):phase==TechniqueFxPayload.Phase.WINDUP?10:phase==TechniqueFxPayload.Phase.BURST?24:14;
        double radius=phase==TechniqueFxPayload.Phase.FIELD?Math.clamp(Math.abs(fx.direction().x),.5,12):phase==TechniqueFxPayload.Phase.BURST?Math.clamp(Math.abs(fx.direction().x),1,8):1.4;
        if(kind.equals("cut")&&phase!=TechniqueFxPayload.Phase.WINDUP){var target=fx.targets().isEmpty()?null:mc.level.getEntity(fx.targets().getFirst());origin=target==null?origin.add(direction.scale(2)):target.getBoundingBox().getCenter();}
        final String material=kind;final Vec3 at=origin,axis=direction;
        var basis=com.jjkcraft.vfx.EffectOrientation.of(axis);
        WorldFx.add(f->{float t=(f.time()-now)/life;if(t>1)return false;if(t<0)return true;if(f.camera.distanceToSqr(at)>96*96)return true;float alpha=(1-t)*.68f;double r=radius*(.45+t*.7);int c=WorldFx.fade(color,alpha);Vec3 side=basis.right(),up=basis.up();
            switch(material){
                case "cut_cage"->{
                    // Crossing incisions travel through the affected volume instead of drawing a wire globe.
                    for(int cut=0;cut<f.segments(8);cut++){
                        double cutPhase=(f.time()*.065+cut*.381)%1,angle=cut*2.399;
                        Vec3 blade=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                        Vec3 offset=side.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle))).scale((cutPhase-.5)*radius*1.6);
                        Vec3 center=at.add(offset).add(axis.scale(Math.sin(cut*3.1)*radius*.4));
                        Vec3 from=center.subtract(blade.scale(radius*.8)),to=center.add(blade.scale(radius*.8));
                        float flash=alpha*(float)Math.sin(cutPhase*Math.PI);
                        f.beam(from,to,.13,WorldFx.fade(0x24131C,flash*.8f),WorldFx.fade(0x24131C,flash*.4f));
                        f.beam(from,to,.022,WorldFx.fade(0xFFF4E9,flash),WorldFx.fade(color,flash*.25f));
                    }
                }
                case "cut","porcelain"->{for(int line=0;line<(material.equals("cut")?3:5);line++){Vec3 last=null;int n=f.segments(24);for(int i=0;i<n;i++){
                    double u=i/(double)(n-1),a=(u-.5)*2.5+t*.4;
                    Vec3 p=at.add(side.scale(Math.sin(a)*r)).add(up.scale(Math.cos(a)*r*.28+(line-1)*.25)).add(axis.scale((line-1)*.22));
                    if(last!=null){double width=.02+.09*Math.sin(u*Math.PI);f.beam(last,p,width*2.2,WorldFx.fade(0x251521,alpha*.75f),WorldFx.fade(color,alpha*.15f));f.beam(last,p,width*.45,WorldFx.fade(0xFFF1DE,alpha),WorldFx.fade(color,alpha*.35f));}last=p;
                }}}
                case "frames","echo_memory","stencil"->{int n=f.segments(5);for(int i=0;i<n;i++){Vec3 center=at.add(axis.scale((i*.65+t*2))).add(side.scale(Math.sin(i+t)*.25));frame(f,center,side,up,.45+i*.05,.75,WorldFx.fade(color,alpha*.55f));f.quad(center.add(side.scale(-.45)).add(up.scale(-.75)),center.add(side.scale(.45)).add(up.scale(-.75)),center.add(side.scale(.45)).add(up.scale(.75)),center.add(side.scale(-.45)).add(up.scale(.75)),WorldFx.fade(color,alpha*.035f),WorldFx.fade(color,alpha*.015f));if(material.equals("frames"))for(int j=0;j<3;j++)f.beam(center.add(side.scale(-.55)).add(up.scale(-.5+j*.5)),center.add(side.scale(-.45)).add(up.scale(-.5+j*.5)),.04,c,c);}}
                case "speech","tollbell"->{for(int i=0;i<5;i++){Vec3 center=at.add(axis.scale(i*.8+t*2));for(int j=0;j<f.segments(24);j++){double a=j*Math.PI*2/f.segments(24),b=(j+.65)*Math.PI*2/f.segments(24);f.beam(center.add(side.scale(Math.cos(a)*r*(.4+i*.17))).add(up.scale(Math.sin(a)*r*(.4+i*.17))),center.add(side.scale(Math.cos(b)*r*(.4+i*.17))).add(up.scale(Math.sin(b)*r*(.4+i*.17))),.025,WorldFx.fade(color,alpha*(1-i*.15f)),0);}}}
                case "electric"->{for(int i=0;i<f.segments(7);i++){
                    double angle=i*2.399;Vec3 from=at.add(side.scale(Math.cos(angle)*r*.2)).add(up.scale(Math.sin(angle)*r*.2)),to=at.add(side.scale(Math.cos(angle+t)*r)).add(up.scale(Math.sin(angle+t)*r)).add(axis.scale(Math.sin(angle*2)*r));
                    Vec3 last=from;for(int j=1;j<=6;j++){double u=j/6.0;Vec3 next=from.lerp(to,u).add(up.scale(Math.sin(j*5.7+i+t*20)*.13*Math.sin(u*Math.PI))).add(side.scale(Math.cos(j*3+i)*.12*Math.sin(u*Math.PI)));
                        f.beam(last,next,.045,WorldFx.fade(color,alpha*.45f),WorldFx.fade(color,alpha*.15f));f.beam(last,next,.010,WorldFx.fade(0xE5F7FF,alpha*.8f),WorldFx.fade(color,alpha*.3f));last=next;
                    }
                }}
                case "glass","salt","sand","resin","kintsugi","metal","lodestone"->{
                    boolean metal=material.equals("metal")||material.equals("lodestone");
                    for(int i=0;i<f.segments(9);i++){
                        double angle=i*2.399;Vec3 p=at.add(Math.cos(angle)*r,Math.sin(angle*2)*r*.7,Math.sin(angle)*r);
                        Vec3 longAxis=up.scale(Math.cos(angle*.7)).add(side.scale(Math.sin(angle*.7))),cross=side.scale(Math.cos(angle*.7)).subtract(up.scale(Math.sin(angle*.7)));
                        Vec3 top=p.add(longAxis.scale(.28+i%3*.12)),left=p.subtract(cross.scale(.13)),right=p.add(cross.scale(.18)),bottom=p.subtract(longAxis.scale(.30)),ridge=p.add(axis.scale(.12));
                        int light=WorldFx.fade(color,alpha*(metal?.8f:.42f)),dark=WorldFx.fade(tint(color,.35),alpha*(metal?.8f:.55f));
                        f.quad(top,left,ridge,top,light,dark);f.quad(left,bottom,ridge,left,dark,light);
                        f.quad(bottom,right,ridge,bottom,dark,light);f.quad(right,top,ridge,right,light,WorldFx.fade(tint(color,.65),alpha*.6f));
                        f.beam(top,ridge,.009,WorldFx.fade(0xE6F3FF,alpha*.55f),WorldFx.fade(color,alpha*.12f));
                    }
                }
                case "soul","suture","thorn","ink"->{for(int i=0;i<f.segments(10);i++){Vec3 last=at;for(int j=0;j<9;j++){double a=i*2.399+j*.27+t*3;Vec3 p=at.add(Math.cos(a)*r*j/9,Math.sin(a*2)*r*.5,Math.sin(a)*r*j/9);f.beam(last,p,material.equals("ink")?.07:.025,WorldFx.fade(color,alpha*(1-j*.08f)),0);last=p;}if(material.equals("thorn"))f.beam(last,last.add(0,.3,0),.03,c,0);}}
                case "origami","carrion"->{for(int i=0;i<f.segments(10);i++){double a=i*2.399+t*2;Vec3 p=at.add(Math.cos(a)*r,Math.sin(a*2)*r*.4,Math.sin(a)*r);Vec3 tip=p.add(axis.scale(.3)),fold=p.add(up.scale(.18));f.quad(tip,p.add(side.scale(-.3)),fold,tip,WorldFx.fade(color,alpha*.5f),c);f.quad(tip,fold,p.add(side.scale(.3)),tip,c,WorldFx.fade(color,alpha*.15f));}}
                case "wax","rust"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 p=at.add(Math.cos(a)*r,(1-t)*.9,Math.sin(a)*r);f.beam(p,p.add(0,-.2-t*.5,0),.07,c,0);f.beam(p,p.add(Math.sin(a)*.1,.15,Math.cos(a)*.1),.025,WorldFx.fade(color,alpha*.4f),0);}}
                case "orbit","pressure","momentum"->{for(int i=0;i<f.segments(12);i++){double a=i*2.399;Vec3 p=at.add(Math.cos(a)*r,Math.sin(a*2)*r*.3,Math.sin(a)*r);Vec3 q=at.add(Math.cos(a+t*2)*r*.45,Math.sin(a*2+t)*r*.2,Math.sin(a+t*2)*r*.45);f.beam(p,q,.025,c,0);f.beam(p,p.add(axis.scale(.3)),.045,WorldFx.fade(color,alpha*.5f),0);}}
                default->{ }
            }return true;
        });return true;
    }
    private static int tint(int c,double factor){return ((int)(((c>>16)&255)*factor)<<16)|((int)(((c>>8)&255)*factor)<<8)|(int)((c&255)*factor);}
    private static void frame(WorldFx.Frame f,Vec3 p,Vec3 side,Vec3 up,double w,double h,int c){Vec3 a=p.add(side.scale(-w)).add(up.scale(-h)),b=p.add(side.scale(w)).add(up.scale(-h)),d=p.add(side.scale(-w)).add(up.scale(h)),e=p.add(side.scale(w)).add(up.scale(h));f.beam(a,b,.02,c,c);f.beam(b,e,.02,c,c);f.beam(e,d,.02,c,c);f.beam(d,a,.02,c,c);}
}
