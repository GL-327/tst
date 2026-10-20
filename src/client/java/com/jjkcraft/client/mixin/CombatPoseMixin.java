package com.jjkcraft.client.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
@Mixin(HumanoidModel.class)
public abstract class CombatPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
    private void jjkcraft$combat(HumanoidRenderState state,CallbackInfo ci){com.jjkcraft.client.combat.CombatAnimations.apply((HumanoidModel<?>)(Object)this,state);}
}
