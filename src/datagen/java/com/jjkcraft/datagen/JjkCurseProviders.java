package com.jjkcraft.datagen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricEntityLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.item.JjkItems;

/** Curse loot tables, the haunted structure tag, and item and block models. */
final class JjkCurseProviders {
	/**
	 * Per curse: shards dropped (min, max) and the trophy's base chance when a player lands the kill.
	 * Looting adds a shard and two points of trophy chance per level.
	 */
	private record Drops(int minShards, int maxShards, float trophyChance) {
	}

	private static final Map<String, Drops> DROPS = Map.ofEntries(
			Map.entry("hollow_hymnal", new Drops(0, 1, 0.20f)),
			Map.entry("ledger_crawler", new Drops(0, 1, 0.20f)),
			Map.entry("wick_moth", new Drops(0, 1, 0.15f)),
			Map.entry("pale_stilt", new Drops(1, 2, 0.30f)),
			Map.entry("echo_mimic", new Drops(1, 2, 0.30f)),
			Map.entry("salt_lantern", new Drops(1, 2, 0.30f)),
			Map.entry("tidewash", new Drops(2, 3, 0.45f)),
			Map.entry("thorn_matriarch", new Drops(2, 3, 0.45f)),
			Map.entry("cracked_bell", new Drops(2, 3, 0.45f)),
			Map.entry("redacted_saint", new Drops(3, 5, 1.0f)),
			Map.entry("ash_oni", new Drops(3, 5, 1.0f)),
			Map.entry("gravebound_gate", new Drops(6, 9, 1.0f)),
			Map.entry("vow_kin", new Drops(3, 5, 1.0f)),
			Map.entry("paper_tide_gravemind", new Drops(10, 14, 1.0f)),
			Map.entry("unfinished_ferryman", new Drops(10, 14, 1.0f)),
			Map.entry("culling_warden", new Drops(10, 14, 1.0f)),
			Map.entry("ten_mouth_sovereign", new Drops(16, 24, 1.0f)),
			Map.entry("doorframe_hunter", new Drops(0, 1, 0.20f)),
			Map.entry("gilded_mimic", new Drops(1, 2, 0.30f)),
			Map.entry("tithe_burrower", new Drops(2, 3, 0.45f)));

	private JjkCurseProviders() {
	}

	static final class EntityLoot extends FabricEntityLootSubProvider {
		EntityLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			for (EntityType<? extends CurseEntity> type : CurseEntities.ALL) {
				String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
				Drops drops = DROPS.getOrDefault(name, new Drops(0, 1, 0.2f));
				LootTable.Builder table = LootTable.lootTable()
						.withPool(LootPool.lootPool()
								.setRolls(ContextIntProviders.exactly(1))
								.add(LootItem.lootTableItem(JjkItems.CURSED_ENERGY_SHARD)
										.apply(SetItemCountFunction.setCount(ContextIntProviders.between(drops.minShards(), drops.maxShards())))
										.apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)))));
				Item trophy = JjkItems.TROPHIES.get(name);
				if (trophy != null) {
					var pool = LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(trophy))
							.when(LootItemKilledByPlayerCondition.killedByPlayer());
					if (drops.trophyChance() < 1f) {
						pool.when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(this.enchantments, drops.trophyChance(), 0.02F));
					}

					table.withPool(pool);
				}

				add(type, table);
			}
		}
	}

	static final class BlockLoot extends FabricBlockLootSubProvider {
		BlockLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			// Breaking a nest scatters the energy it gathered.
			add(JjkBlocks.CURSE_NEST, LootTable.lootTable().withPool(LootPool.lootPool()
					.setRolls(ContextIntProviders.exactly(1))
					.add(LootItem.lootTableItem(JjkItems.CURSED_ENERGY_SHARD)
							.apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4))))));
			// The Modulo expansion's blocks.
			add(com.jjkcraft.modulo.ModuloBlocks.MUL_ORE, createOreDrop(com.jjkcraft.modulo.ModuloBlocks.MUL_ORE, com.jjkcraft.modulo.ModuloItems.MUL));
			dropSelf(com.jjkcraft.modulo.ModuloBlocks.NAUNAX_HULL);
			dropSelf(com.jjkcraft.modulo.ModuloBlocks.NAUNAX_DECK_PLATE);
			dropSelf(com.jjkcraft.modulo.ModuloBlocks.MUL_CONDUIT);
		}
	}

	/** Structures where fear runs high: ruins, mines and other places people died or fled. */
	static final class HauntedStructures extends FabricTagsProvider<Structure> {
		private static final String[] PREFIXES = {"ABANDONED_CAMP", "MINESHAFT", "RUINED_PORTAL", "OCEAN_RUIN", "SHIPWRECK", "ANCIENT_CITY",
				"WOODLAND_MANSION", "STRONGHOLD", "TRIAL_CHAMBERS", "SWAMP_HUT", "TRAIL_RUINS", "FORTRESS", "BASTION_REMNANT"};

		HauntedStructures(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, Registries.STRUCTURE, registries);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void addTags(HolderLookup.Provider registries) {
			var tag = builder(Fear.HAUNTED);
			for (Field field : BuiltinStructures.class.getFields()) {
				if (!Modifier.isStatic(field.getModifiers()) || field.getType() != ResourceKey.class) {
					continue;
				}

				for (String prefix : PREFIXES) {
					if (field.getName().startsWith(prefix)) {
						try {
							tag.add((ResourceKey<Structure>) field.get(null));
						} catch (IllegalAccessException e) {
							throw new IllegalStateException(e);
						}

						break;
					}
				}
			}
		}
	}

	static final class Models extends FabricModelProvider {
		Models(FabricPackOutput output) {
			super(output);
		}

		@Override
		public void generateBlockStateModels(BlockModelGenerators blocks) {
			blocks.createTrivialCube(com.jjkcraft.death.SoulSystem.RELIQUARY);
			blocks.createTrivialCube(JjkBlocks.CURSED_ROOT);
			blocks.createTrivialCube(JjkBlocks.GATE_BARRIER);
			blocks.createTrivialCube(JjkBlocks.CURSE_NEST);
			blocks.createTrivialCube(JjkBlocks.RULE_SEAL);
			for (var block : java.util.List.of(com.jjkcraft.modulo.ModuloBlocks.MUL_ORE, com.jjkcraft.modulo.ModuloBlocks.NAUNAX_HULL,
					com.jjkcraft.modulo.ModuloBlocks.NAUNAX_DECK_PLATE, com.jjkcraft.modulo.ModuloBlocks.MUL_CONDUIT, com.jjkcraft.modulo.ModuloBlocks.HARD_LIGHT)) {
				blocks.createTrivialCube(block);
			}

			// The gate is a thin pad whatever its destination: a carpet-shaped model.
			var gate = com.jjkcraft.modulo.ModuloBlocks.NAUNAX_GATE;
			var model = net.minecraft.client.data.models.model.ModelTemplates.CARPET.create(gate, net.minecraft.client.data.models.model.TextureMapping.wool(gate),
					blocks.modelOutput);
			blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(gate, BlockModelGenerators.plainVariant(model)));
		}

		@Override
		public void generateItemModels(ItemModelGenerators items) {
			items.generateFlatItem(com.jjkcraft.death.SoulSystem.OBJECT, ModelTemplates.FLAT_ITEM);
			items.generateFlatItem(JjkItems.CURSED_ENERGY_SHARD, ModelTemplates.FLAT_ITEM);
			JjkItems.TROPHIES.values().forEach(item -> items.generateFlatItem(item, ModelTemplates.FLAT_ITEM));
			JjkItems.SPAWN_EGGS.values().forEach(item -> items.generateFlatItem(item, ModelTemplates.FLAT_ITEM));
			items.generateFlatItem(JjkItems.ARSENAL_BLADE, ModelTemplates.FLAT_HANDHELD_ITEM);
			for (var weapon : java.util.List.of(JjkItems.SPLIT_SOUL_KATANA, JjkItems.CHAIN_OF_A_THOUSAND_MILES, JjkItems.INVERTED_SPEAR_OF_HEAVEN,
					JjkItems.DEBT_CLEAVER, JjkItems.BONEWOOD_STAFF)) {
				items.generateFlatItem(weapon, ModelTemplates.FLAT_HANDHELD_ITEM);
			}

			for (var item : java.util.List.of(JjkItems.WITHERED_DIGIT, JjkItems.DRIFTING_GOURD, JjkItems.TALLY_LEDGER, JjkItems.STRAW_DOLL,
					JjkItems.TECHNIQUE_SCROLL)) {
				items.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
			}

			for (var item : com.jjkcraft.modulo.ModuloItems.ALL) {
				if (item instanceof net.minecraft.world.item.BlockItem) {
					continue;
				}

				boolean blade = item == com.jjkcraft.modulo.ModuloItems.RUMELIAN_BLADE || item == com.jjkcraft.modulo.ModuloItems.HONOYAGI;
				items.generateFlatItem(item, blade ? ModelTemplates.FLAT_HANDHELD_ITEM : ModelTemplates.FLAT_ITEM);
			}
		}
	}
}
