package com.jjkcraft.technique;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Explosive Flesh spends actual body health, independent of armor, CE discounts and offering sliders. */
public final class FleshSacrifice {
    private FleshSacrifice() { }
    public static float cost(Identifier id){return switch(id.getPath()){case "tooth_blast"->2;case "eye_blast"->5;case "flesh_detonation"->8;default->0;};}
    public static boolean canPay(LivingEntity e,Identifier id){return cost(id)==0||e.getHealth()>cost(id)+1;}
    public static boolean pay(LivingEntity e,Identifier id){float n=cost(id);if(!canPay(e,id))return false;if(n>0)e.setHealth(e.getHealth()-n);return true;}
    public static boolean knowsRct(LivingEntity caster){return caster instanceof net.minecraft.world.entity.player.Player p&&com.jjkcraft.reverse.ReverseTechnique.state(p).unlocked();}
    public static void init(){TechniqueService.addCastBlocker((caster,id,d)->!canPay(caster,id)?CastFailure.NOT_ENOUGH_HEALTH:id.getPath().equals("flesh_regrow")&&!knowsRct(caster)?CastFailure.NOT_KNOWN:null);}
}
