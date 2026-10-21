package com.jjkcraft.client.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class CombatHandsMixin {
    @Invoker("renderPlayerArm") protected abstract void jjkcraft$arm(PoseStack stack,SubmitNodeCollector nodes,int light,float equip,float swing,net.minecraft.world.entity.HumanoidArm arm,PlayerRenderState player);
    @Unique private boolean jjkcraft$posed;
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void jjkcraft$pose(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,
            float swing,ItemStack item,float equip,PoseStack stack,SubmitNodeCollector nodes,int light,CallbackInfo ci){
        var local=net.minecraft.client.Minecraft.getInstance().player;
        boolean right=local==null||local.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT;
        var combat=local==null?null:com.jjkcraft.client.combat.CombatAnimations.pose(local.getId());
        if(local!=null&&combat!=null&&(!combat.move().isEmpty()||combat.guard()>=0||combat.stun()>0)&&hand==InteractionHand.OFF_HAND&&item.isEmpty()&&com.jjkcraft.client.combat.CombatClient.enabled()&&!local.isUsingItem()&&(local.getMainHandItem().isEmpty()||local.getMainHandItem().getItem() instanceof com.jjkcraft.item.tool.CursedToolItem)){
            boolean posed=com.jjkcraft.client.combat.CombatAnimations.hands(stack,!right,partial);
            jjkcraft$arm(stack,nodes,light,0,0,right?net.minecraft.world.entity.HumanoidArm.LEFT:net.minecraft.world.entity.HumanoidArm.RIGHT,player);
            if(posed)stack.popPose();jjkcraft$posed=false;ci.cancel();return;
        }
        jjkcraft$posed=com.jjkcraft.client.combat.CombatAnimations.hands(stack,hand==InteractionHand.MAIN_HAND?right:!right,partial);
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void jjkcraft$restore(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,
            float swing,ItemStack item,float equip,PoseStack stack,SubmitNodeCollector nodes,int light,CallbackInfo ci){
        if(jjkcraft$posed){stack.popPose();jjkcraft$posed=false;}
    }
}
