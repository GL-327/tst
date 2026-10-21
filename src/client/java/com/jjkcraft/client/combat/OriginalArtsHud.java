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
            int x=6,y=g.guiHeight()-96;g.fill(x-2,y-2,x+108,y+26,0xD0141726);Ink.text(g,Ink.fit(Component.translatable(family.name()),104),x,y,Palette.energyHigh(),1);
            Ink.text(g,"RESONANCE "+value.resonance()+" / 8",x,y+11,Palette.TEXT,1);for(int n=0;n<8;n++)g.fill(x+n*13,y+22,x+n*13+10,y+24,n<value.resonance()?Palette.gold():0xFF343548);
            if(value.wards()>0&&mc.level.getGameTime()-received<value.wardTicks())Ink.text(g,"WARD · "+value.wards()+" HITS",x,y-12,Palette.TEXT_DIM,1);
        });
    }
}
