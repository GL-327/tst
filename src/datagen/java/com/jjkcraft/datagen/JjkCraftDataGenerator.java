package com.jjkcraft.datagen;

import net.minecraft.core.RegistrySetBuilder;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import com.jjkcraft.JjkCraft;

public final class JjkCraftDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(JjkEnglishLangProvider::new);
		pack.addProvider(JjkDamageTypeProviders.Types::new);
		pack.addProvider(JjkDamageTypeProviders.Tags::new);
		pack.addProvider(JjkCurseProviders.EntityLoot::new);
		pack.addProvider(JjkCurseProviders.BlockLoot::new);
		pack.addProvider(JjkCurseProviders.HauntedStructures::new);
		pack.addProvider(JjkCurseProviders.Models::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder builder) {
		JjkDamageTypeProviders.bootstrap(builder);
	}

	/** Datagen lives in its own source set and mod, but generates assets and data for the main mod. */
	@Override
	public String getEffectiveModId() {
		return JjkCraft.MOD_ID;
	}
}
