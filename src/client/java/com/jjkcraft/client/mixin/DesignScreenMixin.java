package com.jjkcraft.client.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.jjkcraft.client.design.*;
@Mixin(Screen.class)
public abstract class DesignScreenMixin{
    @Inject(method={"init(II)V","rebuildWidgets"},at=@At("TAIL"))
    private void jjkcraft$design(CallbackInfo ci){UiDesign.capture((Screen)(Object)this);}
    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void jjkcraft$editor(KeyEvent event,CallbackInfoReturnable<Boolean> cir){Screen screen=(Screen)(Object)this;if(UiDesign.mod(screen)&&com.jjkcraft.client.JjkKeys.DESIGN_STUDIO.matches(event)){net.minecraft.client.Minecraft.getInstance().gui.setScreen(new DesignStudioScreen(screen));cir.setReturnValue(true);}}
}
