package com.jjkcraft.client.state;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.network.payload.TechniqueMenuPayloads;

public final class ClientTechniqueMenus {
    private static TechniqueMenuPayloads.View view;
    private static final java.util.Map<Identifier,TechniqueMenuPayloads.View> VIEWS=new java.util.HashMap<>();
    private static final java.util.Map<Identifier,Long> RECEIVED=new java.util.HashMap<>();
    private static final java.util.Map<Identifier,Identifier> SELECTED=new java.util.HashMap<>();
    private static final java.util.Map<Identifier,Integer> BRANCHES=new java.util.HashMap<>();
    private static final java.util.Map<Identifier,String> FEEDBACK=new java.util.HashMap<>();
    private static final java.util.Map<Identifier,Long> FEEDBACK_AT=new java.util.HashMap<>();
    private ClientTechniqueMenus() { }
    public static void reset(){view=null;VIEWS.clear();RECEIVED.clear();SELECTED.clear();BRANCHES.clear();FEEDBACK.clear();FEEDBACK_AT.clear();}
    public static TechniqueMenuPayloads.View view(){return view;}
    public static TechniqueMenuPayloads.View view(Identifier family){return VIEWS.get(family);}
    public static String feedback(Identifier family){return System.currentTimeMillis()-FEEDBACK_AT.getOrDefault(family,0L)<5000?FEEDBACK.getOrDefault(family,""):"";}
    public static Identifier selected(Identifier binding){return SELECTED.get(binding);}
    public static int branch(Identifier binding){return BRANCHES.getOrDefault(binding,0);}
    public static void remember(Identifier binding,Identifier selected,int branch){if(SELECTED.size()>=256&&!SELECTED.containsKey(binding)){SELECTED.clear();BRANCHES.clear();}SELECTED.put(binding,selected);BRANCHES.put(binding,branch);}
    public static int trialSeconds(){var id=com.jjkcraft.JjkCraft.id("ten_shadows");var v=VIEWS.get(id);var mc=Minecraft.getInstance();return v==null||mc.level==null?0:(int)Math.max(0,Math.ceil((v.trialTicks()-(mc.level.getGameTime()-RECEIVED.getOrDefault(id,mc.level.getGameTime())))/20.0));}
    public static List<Identifier> tamed(){return view==null?List.of():view.tamed();}
    public static boolean allowed(com.jjkcraft.technique.TechniqueDefinition d){
        if(d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior b)return !com.jjkcraft.entity.shikigami.TenShadows.BEASTS.contains(b.entity())||tamed().contains(b.entity());
        if(d.behavior() instanceof com.jjkcraft.technique.behavior.FusionBehavior f)return f.components().stream().allMatch(id->!com.jjkcraft.entity.shikigami.TenShadows.BEASTS.contains(id)||tamed().contains(id));
        return true;
    }
    public static void accept(TechniqueMenuPayloads.View next){
        view=next;
        VIEWS.put(next.family(),next);
        if(!next.feedback().isEmpty()){FEEDBACK.put(next.family(),next.feedback());FEEDBACK_AT.put(next.family(),System.currentTimeMillis());}
        if(Minecraft.getInstance().level!=null)RECEIVED.put(next.family(),Minecraft.getInstance().level.getGameTime());
        if(next.open())Minecraft.getInstance().gui.setScreen(new com.jjkcraft.client.screen.TechniqueAbilityScreen(next.family()));
    }
    public static void request(Identifier family,Identifier move,int action){ClientPlayNetworking.send(new TechniqueMenuPayloads.Action(family,move,action));}
}
