package com.jjkcraft.client.state;

import java.util.Locale;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.technique.TechniqueBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Shared live readiness for wheels, the ability browser and tooltips. The server still validates casts. */
public final class ClientTechniqueStatus {
    public record Status(String label,int color,boolean actionable) {}
    private ClientTechniqueStatus() {}
    public static Status get(Identifier id){
        var d=ClientTechniques.definition(id).orElse(null);
        if(d==null)return new Status("Unavailable",Palette.TEXT_DIM,false);
        if(TechniqueBindings.family(id))return new Status("Open variants",Palette.gold(),true);
        if(id.getPath().equals("blue_punch")&&com.jjkcraft.client.combat.PunchImbuementClient.mode()==1||id.getPath().equals("red_punch")&&com.jjkcraft.client.combat.PunchImbuementClient.mode()==2)return new Status("Fists imbued / click to stop",Palette.good(),true);
        if(id.equals(JjkCraft.id("infinity"))&&ClientTechniques.infinityEnabled())return new Status(infinity(),ClientTechniques.infinityActive()?Palette.good():Palette.gold(),true);
        if(ClientTechniques.activeIs(id))return new Status("Active / click to stop",Palette.good(),true);
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&(mc.player.hasEffect(com.jjkcraft.effect.JjkEffects.STUNNED)||mc.player.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME)||com.jjkcraft.client.combat.CombatClient.staggered()))return new Status("Stunned",Palette.danger(),false);
        if(mc.player!=null&&mc.player.hasEffect(com.jjkcraft.effect.JjkEffects.OVERLOADED))return new Status("Domain overload",Palette.danger(),false);
        if(ClientDomains.self().burnoutUntil()>ClientEnergy.gameTime())return new Status("Technique burnout",Palette.danger(),false);
        if(!ClientTechniques.gradeAllows(d))return new Status("Higher grade required",Palette.TEXT_DIM,false);
        if(!ClientTechniques.known().contains(id))return new Status("Not learned",Palette.TEXT_DIM,false);
        if(mc.player!=null&&!mc.player.isCreative()&&!ClientTechniqueMenus.allowed(d))return new Status("Untamed / subjugate first",Palette.gold(),false);
        long ticks=ClientTechniques.state().readyAt(id)-ClientEnergy.gameTime();
        if(ticks>0)return new Status(String.format(Locale.ROOT,"Ready in %.1fs",ticks/20.0),Palette.TEXT_DIM,false);
        if(!ClientTechniques.affordable(id))return new Status("Not enough CE",Palette.danger(),false);
        return new Status("Ready",Palette.good(),true);
    }
    public static String infinity(){
        if(!ClientTechniques.infinityEnabled())return "Infinity OFF";
        if(ClientTechniques.infinityActive())return ClientTechniques.infinityUpkeep()<=0?"Infinity ON / no upkeep":String.format(Locale.ROOT,"Infinity ON / %.1f CE/s",ClientTechniques.infinityUpkeep());
        return switch(ClientTechniques.infinityReason()){case 2->"Infinity waiting for CE";case 3->"Infinity suppressed";default->"Infinity starting";};
    }
}
