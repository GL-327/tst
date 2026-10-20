package com.jjkcraft.client.vfx;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import net.minecraft.world.phys.Vec3;

/** Short, readable material accents layered over each move's own authored trail. */
final class SignatureFx {
    private SignatureFx() { }
    static void event(TechniqueFxPayload fx,int color,float now){
        if(ClientConfig.effectsOff()||ClientConfig.effect(ClientConfig.get().vfx.impacts)==0)return;
        boolean hit=fx.phase()==TechniqueFxPayload.Phase.HIT;
        if(!hit&&fx.phase()!=TechniqueFxPayload.Phase.CAST&&fx.phase()!=TechniqueFxPayload.Phase.BURST)return;
        String id=fx.technique().getPath();
        int style=id.contains("blood")||id.contains("crimson")||id.contains("red_scale")?1:id.contains("ice")||id.contains("frost")?2:id.contains("flame")||id.contains("volcan")||id.contains("ember")?3:id.contains("blue")||id.contains("purple")||id.contains("reversal_red")||id.contains("infinity")?4:id.contains("shadow")||id.contains("divine_dog")?5:id.equals("ratio")?6:0;
        if(style==0)return;
        Vec3 origin=fx.origin();var mc=net.minecraft.client.Minecraft.getInstance();
        if(!hit&&mc.player!=null&&mc.player.getId()==fx.caster())origin=origin.add(mc.player.getLookAngle().scale(1.6));
        final Vec3 at=origin;final float duration=hit?7:11;
        WorldFx.add(f->{
            float t=(f.time()-now)/duration;if(t>=1)return false;if(t<0)return true;
            float alpha=(1-t)*(hit?0.8f:0.55f);int c=WorldFx.fade(color,alpha);double r=0.3+t*(hit?1.2:0.8);
            if(style==1){
                for(int n=0;n<5;n++){double a=n*2.399;Vec3 d=new Vec3(Math.cos(a),Math.sin(a),Math.sin(a*2)*0.2);f.beam(at.add(d.scale(r*0.15)),at.add(d.scale(r)),0.035,c,WorldFx.fade(color,0));}
            }else if(style==2){
                for(int n=0;n<6;n++){double a=n*Math.PI/3;Vec3 d=new Vec3(Math.cos(a),Math.sin(a),0);Vec3 end=at.add(d.scale(r));f.beam(at,end,0.055,c,WorldFx.fade(0xE6F9FF,alpha));f.beam(end,end.add(-Math.sin(a)*0.12,Math.cos(a)*0.12,0.15),0.04,c,c);}
            }else if(style==3){
                for(int n=0;n<6;n++){double a=n*Math.PI/3+t*2;Vec3 base=at.add(Math.cos(a)*r,0,Math.sin(a)*r);f.beam(base,base.add(-Math.cos(a)*0.15,0.4+0.5*t,-Math.sin(a)*0.15),0.08*(1-t),c,WorldFx.fade(0xFFDCAE,0));}
            }else if(style==4){
                int seg=f.segments(24);for(int n=0;n<seg;n++){double a=n*Math.PI*2/seg,b=(n+0.7)*Math.PI*2/seg;f.beam(at.add(Math.cos(a)*r,Math.sin(a)*r,0),at.add(Math.cos(b)*r,Math.sin(b)*r,0),0.035,c,c);}
            }else if(style==5){
                Vec3 ground=at.add(0,-0.5,0);for(int n=0;n<6;n++){double a=n*Math.PI/3;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a));f.beam(ground.add(d.scale(0.2)),ground.add(d.scale(r*1.5)),0.12,WorldFx.fade(0x221630,alpha),WorldFx.fade(color,0));}
            }else{
                f.beam(at.add(-r,0,0),at.add(r,0,0),0.025,c,c);f.beam(at.add(r*0.4,-r*0.5,0),at.add(r*0.4,r*0.5,0),0.05,WorldFx.fade(0xF8DF8C,alpha),c);
            }return true;
        });
    }
}
