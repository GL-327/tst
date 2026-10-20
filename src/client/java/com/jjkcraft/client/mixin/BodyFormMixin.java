package com.jjkcraft.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** The dispatcher can render a synced host body without replacing the networked player entity. */
@Mixin(EntityRenderDispatcher.class)
public abstract class BodyFormMixin {
    @Unique private final java.util.Map<EntityRenderState,EntityRenderState> jjkcraft$forms=new java.util.WeakHashMap<>();
    @Inject(method="extractEntity",at=@At("RETURN"))
    private void jjkcraft$bodyForm(Entity entity,float partial,CallbackInfoReturnable<EntityRenderState> cir) {
        if(entity instanceof Player player) {
            Entity proxy=com.jjkcraft.client.state.ClientTraits.form(player);
            if(proxy!=null)jjkcraft$forms.put(cir.getReturnValue(),((EntityRenderDispatcher)(Object)this).extractEntity(proxy,partial));
            else jjkcraft$forms.remove(cir.getReturnValue());
        }
    }
    @Inject(method="submit",at=@At("HEAD"),cancellable=true)
    private void jjkcraft$submitBody(EntityRenderState state,net.minecraft.client.renderer.state.level.CameraRenderState camera,
            double x,double y,double z,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.SubmitNodeCollector nodes,CallbackInfo ci) {
        var form=jjkcraft$forms.get(state);
        if(form!=null && form!=state){((EntityRenderDispatcher)(Object)this).submit(form,camera,x,y,z,poses,nodes);ci.cancel();}
    }
}
