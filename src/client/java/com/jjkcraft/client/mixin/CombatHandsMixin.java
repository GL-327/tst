package com.jjkcraft.client.mixin;
import org.spongepowered.asm.mixin.Mixin;
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
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void jjkcraft$pose(PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,float partial,float pitch,InteractionHand hand,
            float swing,ItemStack item,float equip,PoseStack stack,SubmitNodeCollector nodes,int light,CallbackInfo ci){
        var local=net.minecraft.client.Minecraft.getInstance().player;
        if(local==null||local.isInvisible()||local.isUsingItem()||!item.isEmpty()||!local.getMainHandItem().isEmpty()||!local.getOffhandItem().isEmpty()
            ||!com.jjkcraft.client.combat.CombatClient.enabled())return;
        boolean right=(hand==InteractionHand.MAIN_HAND)==(local.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT);
        stack.pushPose();
        try {
            if(!com.jjkcraft.client.combat.CombatAnimations.hands(stack,right,partial))return;
            com.jjkcraft.client.combat.CombatFists.submit(stack,nodes,light,player,right);
            ci.cancel();
        } finally {stack.popPose();}
    }
}
