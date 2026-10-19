package com.jjkcraft.modulo.world;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import com.jjkcraft.JjkCraft;

/** World generation feature types for the Modulo dimensions; their placement is data under {@code data/modulo/worldgen/}. */
public final class ModuloFeatures {
	private ModuloFeatures() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, JjkCraft.id("naunax_deck"), NaunaxDeckFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, JjkCraft.id("city_block"), CityBlockFeature.CODEC);
	}
}
