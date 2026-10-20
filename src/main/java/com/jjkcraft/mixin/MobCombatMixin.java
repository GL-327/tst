package com.jjkcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

/** Preserve vanilla targeting/navigation; replace hostile melee contact with the shared frame windows. */
@Mixin(Mob.class)
public abstract class MobCombatMixin {
    @Inject(method="doHurtTarget",at=@At("HEAD"),cancellable=true)
    private void jjkcraft$framedMelee(ServerLevel level,Entity target,CallbackInfoReturnable<Boolean> cir){
        var mob=(Mob)(Object)this;
        if(com.jjkcraft.config.JjkConfig.server().systems.cursedCombat && target instanceof LivingEntity living
            && !(mob instanceof com.jjkcraft.curse.CurseEntity)
            && (mob instanceof net.minecraft.world.entity.monster.Monster||mob instanceof com.jjkcraft.culling.entity.EntrantEntity))
            cir.setReturnValue(com.jjkcraft.combat.CursedCombat.startMob(mob,living));
    }
}
