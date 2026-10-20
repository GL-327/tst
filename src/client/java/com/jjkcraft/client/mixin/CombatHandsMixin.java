package com.jjkcraft.client.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private boolean jjkcraft$posed;
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void jjkcraft$pose(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,
            float swing,ItemStack item,float equip,PoseStack stack,SubmitNodeCollector nodes,int light,CallbackInfo ci){
        var local=net.minecraft.client.Minecraft.getInstance().player;
        boolean right=local==null||local.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT;
        jjkcraft$posed=com.jjkcraft.client.combat.CombatAnimations.hands(stack,hand==InteractionHand.MAIN_HAND?right:!right,partial);
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void jjkcraft$restore(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,
            float swing,ItemStack item,float equip,PoseStack stack,SubmitNodeCollector nodes,int light,CallbackInfo ci){
        if(jjkcraft$posed){stack.popPose();jjkcraft$posed=false;}
    }
}
