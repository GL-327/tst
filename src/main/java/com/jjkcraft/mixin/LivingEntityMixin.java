package com.jjkcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.damage.DamageModifiers;

/** Routes incoming damage through {@link DamageModifiers} before vanilla processes it. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float jjkcraft$modifyIncomingDamage(float damage, ServerLevel level, DamageSource source) {
		return DamageModifiers.apply(level, (LivingEntity) (Object) this, source, damage);
	}
}
