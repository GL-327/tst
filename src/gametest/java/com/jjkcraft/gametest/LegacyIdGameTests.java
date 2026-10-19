package com.jjkcraft.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.registry.LegacyIds;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.Techniques;

/**
 * The canon naming pass (docs/NAMING.md) renamed ids. Worlds and data packs from before it must
 * still load: these read old ids the way a save or a data pack would and expect the new entries.
 */
public class LegacyIdGameTests {
	private static Identifier id(String path) {
		return JjkCraft.id(path);
	}

	@GameTest
	public void oldRegistryIdsResolveToTheRenamedEntries(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(id("bound_effigy")) == JjkItems.STRAW_DOLL, "bound_effigy is the Straw Doll");
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(id("inverted_tine")) == JjkItems.INVERTED_SPEAR_OF_HEAVEN, "inverted_tine is the Inverted Spear of Heaven");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(id("ink_hound")) == JjkEntities.DIVINE_DOG, "ink_hound is the Divine Dog");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(id("tallykeeper")) == CullingEntities.KOGANE, "tallykeeper is the Kogane");
		helper.assertTrue(BuiltInRegistries.MOB_EFFECT.getValue(id("skipped")) == JjkEffects.FROZEN_FRAME.value(), "skipped is Frozen Frame");
		helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id("effigy_link")) == JjkComponents.STRAW_DOLL_LINK, "effigy_link is the straw doll link");
		helper.assertTrue(TechniqueBehaviors.REGISTRY.getValue(id("time_skip")) == TechniqueBehaviors.PROJECTION_SORCERY, "time_skip is Projection Sorcery");
		helper.assertTrue(TechniqueBehaviors.REGISTRY.getValue(id("command")) == TechniqueBehaviors.CURSED_SPEECH, "command is Cursed Speech");
		helper.succeed();
	}

	@GameTest
	public void anOldSavedStackLoadsAsTheRenamedItem(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		JsonElement tine = JsonParser.parseString("{\"id\": \"jjkcraft:inverted_tine\", \"count\": 1}");
		ItemStack spear = ItemStack.CODEC.parse(ops, tine).getOrThrow();
		helper.assertTrue(spear.is(JjkItems.INVERTED_SPEAR_OF_HEAVEN), "the old spear loads");

		JsonElement scroll = JsonParser.parseString(
				"{\"id\": \"jjkcraft:technique_scroll\", \"count\": 1, \"components\": {\"jjkcraft:loaded_technique\": \"jjkcraft:inward_hollow\"}}");
		ItemStack loaded = ItemStack.CODEC.parse(ops, scroll).getOrThrow();
		helper.assertValueEqual(loaded.get(JjkComponents.LOADED_TECHNIQUE), id("lapse_blue"), "a scroll of the old attract technique teaches Lapse: Blue");
		helper.succeed();
	}

	@GameTest
	public void anOldDataPackTechniqueDecodes(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		JsonElement json = JsonParser.parseString("""
				{"name": "x", "description": "x", "category": "utility", "grade": "jjkcraft:grade_2",
				 "cast": {"cost": 10, "cooldown": 20}, "behavior": {"type": "jjkcraft:time_skip", "radius": 5, "skip_ticks": 20},
				 "visuals": {"color": "#D0D0E0"}, "icon": "jjkcraft:textures/gui/technique/projection_sorcery.png"}""");
		TechniqueDefinition definition = TechniqueDefinition.CODEC.parse(ops, json).getOrThrow();
		helper.assertTrue(definition.behavior().type() == TechniqueBehaviors.PROJECTION_SORCERY, "an old behavior type still decodes");

		for (String renamed : LegacyIds.TECHNIQUES.values()) {
			helper.assertTrue(Techniques.get(helper.getLevel().registryAccess(), id(renamed)).isPresent(), "technique " + renamed + " is loaded");
		}
		helper.succeed();
	}
}
