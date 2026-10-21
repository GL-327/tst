package com.jjkcraft.client.vfx;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.CombatPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.phys.Vec3;

/** Contact effects only arrive after an authoritative hit, block, break or parry. */
public final class CombatImpactFx {
    private CombatImpactFx() { }
    public static void init(){ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.Impact.TYPE,(p,c)->accept(p));}
    public static void accept(CombatPayloads.Impact p){
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||ClientConfig.effectsOff()||ClientConfig.effect(ClientConfig.get().vfx.impacts)==0)return;
        Vec3 at=new Vec3(p.x(),p.y(),p.z());float start=mc.level.getGameTime();
        int color=p.kind()==2?0xF2D38B:p.kind()==3?0xF14458:p.kind()==4?0x8FC9E4:0xD5C5FF;
        boolean strong=p.kind()==1||p.kind()==3;
        WorldFx.add(frame->{
            float t=(frame.time()-start)/(strong?10f:6f);if(t>=1)return false;if(t<0)return true;
            double r=(strong?1.1:0.65)*(0.2+t);int n=frame.segments(strong?16:10);
            for(int i=0;i<n;i++){
                double angle=i*Math.PI*2/n;Vec3 dir=new Vec3(Math.cos(angle),Math.sin(angle)*0.7,Math.sin(angle*3)*0.3);
                frame.beam(at.add(dir.scale(r*0.3)),at.add(dir.scale(r)),0.07*(1-t),WorldFx.fade(0xFFFFFF,1-t),WorldFx.fade(color,0));
            }return true;
        });
        if(strong||p.kind()==2)WorldFx.add(WorldFx.burst(at,strong?1.35:0.8,color,start));
        if(mc.player!=null&&(mc.player.getId()==p.attacker()||mc.player.getId()==p.target())){
            if(strong)ClientFx.hitStop(1,p.attacker(),p.target());ClientFx.shake(strong?0.045f:0.015f,strong?110:55);
        }
    }
}
