package com.jjkcraft.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * Test-only (not in the mod jar): the vanilla GameTest server bakes its world with an empty
 * dimension registry, so datapack dimensions never load there. The Modulo tests need
 * {@code modulo:naunax} and {@code modulo:tokyo_2086}, so the test server bakes with the datapack's
 * dimensions, the way a normal world does (docs/MIXINS.md).
 */
@Mixin(GameTestServer.class)
abstract class GameTestServerMixin {
	@WrapOperation(method = "lambda$create$1", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
	private static WorldDimensions.Complete jjkcraft$bakeWithDatapackDimensions(WorldDimensions dimensions, Registry<LevelStem> noDatapackDimensions,
			Operation<WorldDimensions.Complete> original, @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
		return original.call(dimensions, context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
	}
}
