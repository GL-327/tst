package com.jjkcraft.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import com.jjkcraft.damage.JjkDamageTypes;

/** Damage types and their vanilla tags. */
final class JjkDamageTypeProviders {
	static final Map<ResourceKey<DamageType>, DamageType> TYPES = new LinkedHashMap<>();

	static {
		TYPES.put(JjkDamageTypes.TECHNIQUE, new DamageType("jjkcraft.cursed_technique", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
		TYPES.put(JjkDamageTypes.TECHNIQUE_PIERCING, new DamageType("jjkcraft.cursed_technique", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
		TYPES.put(JjkDamageTypes.BLACK_FLASH, new DamageType("jjkcraft.black_flash", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.2f));
		TYPES.put(JjkDamageTypes.SURE_HIT, new DamageType("jjkcraft.sure_hit", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0f));
		TYPES.put(JjkDamageTypes.BACKLASH, new DamageType("jjkcraft.backlash", DamageScaling.NEVER, 0.0f));
		TYPES.put(JjkDamageTypes.LIGHT, new DamageType("jjkcraft.light", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
		TYPES.put(JjkDamageTypes.DARKNESS, new DamageType("jjkcraft.darkness", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0f));
	}

	private JjkDamageTypeProviders() {
	}

	static void bootstrap(RegistrySetBuilder builder) {
		builder.add(Registries.DAMAGE_TYPE, context -> TYPES.forEach(context::register));
	}

	/** Writes {@code data/jjkcraft/damage_type/*.json}. */
	static final class Types implements DataProvider {
		private final PackOutput.PathProvider path;

		Types(FabricPackOutput output) {
			this.path = output.createPathProvider(PackOutput.Target.DATA_PACK, "damage_type/");
		}

		@Override
		public CompletableFuture<?> run(CachedOutput writer) {
			List<CompletableFuture<?>> futures = new ArrayList<>();
			TYPES.forEach((key, type) -> {
				JsonObject json = new JsonObject();
				json.addProperty("exhaustion", type.exhaustion());
				json.addProperty("message_id", type.msgId());
				json.addProperty("scaling", type.scaling().getSerializedName());
				futures.add(DataProvider.saveStable(writer, json, path.json(key.identifier())));
			});
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		}

		@Override
		public String getName() {
			return "Jujutsu Craft damage types";
		}
	}

	static final class Tags extends FabricTagsProvider<DamageType> {
		Tags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, Registries.DAMAGE_TYPE, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(DamageTypeTags.BYPASSES_ARMOR)
					.add(JjkDamageTypes.DARKNESS)
					.add(JjkDamageTypes.TECHNIQUE_PIERCING)
					.add(JjkDamageTypes.SURE_HIT)
					.add(JjkDamageTypes.BACKLASH);
			builder(DamageTypeTags.BYPASSES_SHIELD)
					.add(JjkDamageTypes.SURE_HIT)
					.add(JjkDamageTypes.BLACK_FLASH)
					.add(JjkDamageTypes.BACKLASH);
			builder(DamageTypeTags.NO_KNOCKBACK)
					.add(JjkDamageTypes.BACKLASH);
		}
	}
}
