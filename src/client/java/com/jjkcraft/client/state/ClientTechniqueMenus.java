package com.jjkcraft.client.state;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.network.payload.TechniqueMenuPayloads;

public final class ClientTechniqueMenus {
    private static TechniqueMenuPayloads.View view;
    private ClientTechniqueMenus() { }
    public static void reset(){view=null;}
    public static TechniqueMenuPayloads.View view(){return view;}
    public static List<Identifier> tamed(){return view==null?List.of():view.tamed();}
    public static boolean allowed(com.jjkcraft.technique.TechniqueDefinition d){
        if(d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior b)return !com.jjkcraft.entity.shikigami.TenShadows.BEASTS.contains(b.entity())||tamed().contains(b.entity());
        if(d.behavior() instanceof com.jjkcraft.technique.behavior.FusionBehavior f)return f.components().stream().allMatch(id->!com.jjkcraft.entity.shikigami.TenShadows.BEASTS.contains(id)||tamed().contains(id));
        return true;
    }
    public static void accept(TechniqueMenuPayloads.View next){
        view=next;
        if(next.open())Minecraft.getInstance().gui.setScreen(new com.jjkcraft.client.screen.TechniqueAbilityScreen(next.family()));
    }
    public static void request(Identifier family,Identifier move,int action){ClientPlayNetworking.send(new TechniqueMenuPayloads.Action(family,move,action));}
}
