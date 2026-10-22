package com.jjkcraft.client.combat;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.OriginalArtsPayload;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
public final class OriginalArtsHud {
    private static OriginalArtsPayload value=new OriginalArtsPayload("",0,0,0);private static long received;
    private OriginalArtsHud(){}
    public static int resonance(){return value.resonance();}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(OriginalArtsPayload.TYPE,(packet,context)->{value=packet;var level=context.client().level;received=level==null?0:level.getGameTime();});
        ClientPlayConnectionEvents.DISCONNECT.register((h,mc)->value=new OriginalArtsPayload("",0,0,0));
        HudElementRegistry.addLast(JjkCraft.id("original_resonance"),(g,dt)->{
            var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null||!mc.player.isAlive()||mc.gui.screen()!=null||mc.gui.hud.isHidden()||value.family().isEmpty()||mc.level.getGameTime()-received>6000)return;
            var id=net.minecraft.resources.Identifier.tryParse("jjkcraft:"+value.family());if(id==null)return;var family=mc.level.registryAccess().lookupOrThrow(com.jjkcraft.awakening.CharacterAwakening.FAMILIES).getValue(id);if(family==null)return;
            var hud=com.jjkcraft.client.config.ClientConfig.get().hud;var cfg=hud.resonance;if(!cfg.visible)return;
            boolean ward=value.wards()>0&&mc.level.getGameTime()-received<value.wardTicks();int w=com.jjkcraft.client.hud.HudStyle.COLUMN_WIDTH,h=ward?40:30;float alpha=cfg.opacity();
            com.jjkcraft.client.hud.HudLayout.pushBelow(g,cfg,w,h,hud.energyBar,hud.gradeBadge,hud.combatPanel,hud.cullingPanel,hud.ritualTracker);
            Ink.panel(g,0,0,w,h,Palette.energyHigh(),alpha);Ink.text(g,Ink.fit(Component.translatable(family.name()),w-9),5,3,Palette.energyHigh(),alpha);
            Ink.text(g,"Resonance "+value.resonance()+" / 8",5,14,Palette.TEXT,alpha);int cell=(w-10)/8;for(int n=0;n<8;n++)g.fill(5+n*cell,25,5+n*cell+cell-2,27,net.minecraft.util.ARGB.multiplyAlpha(n<value.resonance()?Palette.gold():0xFF343548,alpha));
            if(ward)Ink.text(g,"Ward · "+value.wards()+" hits",5,30,Palette.TEXT_DIM,alpha);
            g.pose().popMatrix();
        });
    }
}
