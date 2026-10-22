package com.jjkcraft.client.combat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
/** Resting items remain vanilla; a paid weapon strike drives an actual held model through its arc. */
public final class WeaponAnimations{
    private WeaponAnimations(){}
    public static boolean active(ItemStack item){var m=Minecraft.getInstance();if(m.player==null||item.isEmpty()||m.player.isUsingItem()||!CombatClient.enabled())return false;var p=CombatAnimations.pose(m.player.getId());return p!=null&&(p.move().startsWith("blade_")||p.move().startsWith("pole_")||p.move().startsWith("club_")||p.move().startsWith("item_"));}
    public static void apply(PoseStack pose,InteractionHand hand,float partial){
        var m=Minecraft.getInstance();var p=CombatAnimations.pose(m.player.getId());if(p==null)return;
        int side=(hand==InteractionHand.MAIN_HAND)==(m.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)?1:-1;
        float age=m.level.getGameTime()-p.started()+partial;
        float w=Math.clamp(age/Math.max(1,p.startup()),0,1),hit=Math.clamp((age-p.startup())/Math.max(1,p.active()),0,1),out=Math.clamp((age-p.startup()-p.active())/Math.max(1,p.recovery()),0,1);
        w=w*w*(3-2*w);hit=hit*hit*(3-2*hit);out=out*out*(3-2*out);
        float blend=(1-out)*(age<p.startup()?w:1),arc=age<p.startup()?-w:-1+2*hit;
        boolean thrust=p.move().endsWith("thrust"),heavy=p.move().endsWith("heavy");
        // Rotate about the vanilla hand grip, keeping the windup inside the player's view.
        pose.translate(side*.56,-.52,-.72);
        pose.translate(side*(thrust?-.08*blend:.15*arc),heavy?.1*blend:0,thrust?-.5*blend:-.12*blend);
        pose.rotateDegrees(Axis.YP,side*(thrust?10:45)*arc*blend);pose.rotateDegrees(Axis.ZP,side*(heavy?60:35)*arc*blend);pose.rotateDegrees(Axis.XP,(heavy?-45:-12)*blend);
        pose.translate(-side*.56,.52,.72);
    }
}
