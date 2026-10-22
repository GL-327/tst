package com.jjkcraft.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
/** Keep ordinary client packets from fighting the temporary server-owned incarnation controller. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PossessionInputMixin {
    @Shadow public ServerPlayer player;
    @Inject(method={"handleMovePlayer","handlePlayerInput","handlePlayerAction","handleUseItemOn","handleUseItem","handleInteract","handleSetCarriedItem"},at=@At("HEAD"),cancellable=true)
    private void jjkcraft$controlled(CallbackInfo ci){if(player.level().getServer().isSameThread()&&com.jjkcraft.vessel.CursedVessels.controlled(player))ci.cancel();}
}
