package com.jjkcraft.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Test-only: Fabric parks the server at its phase barrier while the render thread disconnects.
 * 26.3's new executeBlocking in halt otherwise waits on that parked server forever.
 * Queue the player cleanup; normal server shutdown still saves and closes the temporary world.
 */
@Mixin(IntegratedServer.class)
abstract class IntegratedServerShutdownMixin {
    @WrapOperation(method="halt",at=@At(value="INVOKE",target="Lnet/minecraft/client/server/IntegratedServer;executeBlocking(Ljava/lang/Runnable;)V"))
    private void jjkcraft$queueTestCleanup(IntegratedServer server,Runnable cleanup,Operation<Void> original){server.execute(cleanup);}
}
