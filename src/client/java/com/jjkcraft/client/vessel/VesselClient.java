package com.jjkcraft.client.vessel;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.network.payload.VesselPayload;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Temporary, server-authoritative incarnation effects. A missed end packet cannot strand a blackout. */
public final class VesselClient {
    private static VesselPayload state=new VesselPayload(0,0,0,0,java.util.List.of());
    private static int remaining,ticks;
    private VesselClient(){}
    public static VesselPayload state(){return state;}
    public static boolean controlled(){return remaining>0&&state.phase()==2;}
    public static boolean hallucinated(int id){return remaining>0&&state.phase()==1&&state.falseTargets().contains(id);}
    public static float sway(float partial){return remaining>0&&state.phase()==1&&!ClientConfig.reducedMotion()?(float)Math.sin((ticks+partial)*.035)*.6f:0;}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(VesselPayload.TYPE,(p,c)->{state=p;remaining=p.remaining();});
        ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{state=new VesselPayload(0,0,0,0,java.util.List.of());remaining=0;});
        ClientTickEvents.END_CLIENT_TICK.register(m->{if(m.level!=null&&!m.isPaused()){ticks++;if(remaining>0)remaining--;}});
        HudElementRegistry.addLast(JjkCraft.id("incarnation"),(g,dt)->{
            var mc=Minecraft.getInstance();if(mc.player==null)return;
            int w=g.guiWidth(),h=g.guiHeight();
            if(remaining<=0){if(state.fingers()>0&&!mc.gui.hud.isHidden()&&mc.gui.screen()==null)Ink.centered(g,net.minecraft.network.chat.Component.literal("Sukuna · "+state.fingers()+" / 20 fingers · pressure "+state.pressure()+"%"),w/2,52,state.pressure()>=40?0xFFE18390:0xFFBBA6B2,1);return;}
            if(state.phase()==2){
                g.fill(0,0,w,h,0xFF000000);
                Ink.centered(g,net.minecraft.network.chat.Component.literal("SUKUNA HAS TAKEN CONTROL"),w/2,h/2-10,0xFFCE6C72,1);
                Ink.centered(g,net.minecraft.network.chat.Component.literal("Regaining control · "+(int)Math.ceil(remaining/20.0)+" s"),w/2,h/2+7,0xFF9A8F99,1);
            }else if(state.phase()==1){
                // Stable translucent edges, without repeated flashes or changes to real entity identity.
                for(int i=0;i<14;i++){int alpha=(14-i)*4,color=(alpha<<24)|0x77142A;g.fill(i,i,w-i,i+1,color);g.fill(i,h-i-1,w-i,h-i,color);g.fill(i,i,i+1,h-i,color);g.fill(w-i-1,i,w-i,h-i,color);}
                if(!mc.gui.hud.isHidden())Ink.centered(g,net.minecraft.network.chat.Component.literal("“That face is hiding a curse. Strike first.”"),w/2,h-45,0xFFD894A2,1);
            }
        });
    }
}
