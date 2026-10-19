package com.jjkcraft.registry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Ids renamed by the canon naming pass (1.1.0; the full table and sources are in docs/NAMING.md).
 * Worlds and data packs made before it still load:
 * <ul>
 * <li>built-in registries (items, entity types, mob effects, item components, technique behavior
 * types) get Fabric registry aliases, so saved stacks, entities, effects and a data pack's
 * {@code "type"} fields resolve old ids to the new entries;</li>
 * <li>every codec that reads a technique, domain, Culling Game rule, item or entity id from a save
 * or a data pack maps old ids to new ones on read and writes the new id back.</li>
 * </ul>
 * Data packs that still use an old id get one warning per id in the log.
 */
public final class LegacyIds {
	public static final Map<String, String> TECHNIQUES = Map.ofEntries(
			Map.entry("inward_hollow", "lapse_blue"),
			Map.entry("outward_bloom", "reversal_red"),
			Map.entry("null_convergence", "hollow_purple"),
			Map.entry("hollow_star", "maximum_output_blue"),
			Map.entry("crescent_rend", "cleave"),
			Map.entry("hairline_sever", "dismantle"),
			Map.entry("crimson_meridian", "piercing_blood"),
			Map.entry("effigy_nail", "straw_doll_resonance"),
			Map.entry("kennel_call", "ten_shadows_divine_dogs"),
			Map.entry("crane_fold", "ten_shadows_nue"),
			Map.entry("tortoise_ward", "ten_shadows_toad"),
			Map.entry("command_halt", "cursed_speech_dont_move"),
			Map.entry("command_kneel", "cursed_speech_crumble_away"),
			Map.entry("command_rise", "cursed_speech_blast_away"),
			Map.entry("flesh_sculpt", "idle_transfiguration"),
			Map.entry("stolen_frame", "projection_sorcery"),
			Map.entry("reverse_flow", "reverse_cursed_technique"),
			Map.entry("kindred_mend", "positive_energy_output"),
			Map.entry("still_air_veil", "infinity"));

	public static final Map<String, String> DOMAINS = Map.of(
			"hanging_edges", "malevolent_shrine",
			"boundless_index", "unlimited_void",
			"lightless_kennels", "chimera_shadow_garden",
			"lantern_row_wager", "idle_death_gamble",
			"borrowed_oaths", "authentic_mutual_love");

	public static final Map<String, String> CULLING_RULES = Map.of("the_tallykeeper", "game_master");

	/** Items, and the cursed tool entries named after them. */
	public static final Map<String, String> ITEMS = Map.of(
			"bound_effigy", "straw_doll",
			"inverted_tine", "inverted_spear_of_heaven",
			"split_soul_nodachi", "split_soul_katana",
			"thousand_link_chainwhip", "chain_of_a_thousand_miles");

	public static final Map<String, String> ENTITIES = Map.of(
			"ink_hound", "divine_dog",
			"paper_crane", "nue",
			"stone_tortoise", "toad",
			"shadow_hound", "shadow_divine_dog",
			"tallykeeper", "kogane");

	public static final Map<String, String> EFFECTS = Map.of("skipped", "frozen_frame");

	public static final Map<String, String> COMPONENTS = Map.of("effigy_link", "straw_doll_link");

	public static final Map<String, String> TECHNIQUE_BEHAVIORS = Map.of(
			"effigy_link", "straw_doll",
			"time_skip", "projection_sorcery",
			"reshape", "idle_transfiguration",
			"projectile_veil", "infinity",
			"command", "cursed_speech");

	public static final Codec<Identifier> TECHNIQUE_ID = aliased(TECHNIQUES);
	public static final Codec<Identifier> DOMAIN_ID = aliased(DOMAINS);
	public static final Codec<Identifier> CULLING_RULE_ID = aliased(CULLING_RULES);
	public static final Codec<Identifier> ITEM_ID = aliased(ITEMS);
	public static final Codec<Identifier> ENTITY_ID = aliased(ENTITIES);

	private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

	private LegacyIds() {
	}

	public static Identifier technique(Identifier id) {
		return map(TECHNIQUES, id);
	}

	public static Identifier domain(Identifier id) {
		return map(DOMAINS, id);
	}

	/** Registers the built-in registry aliases. Runs once, after the mod's own entries are registered. */
	public static void registerAliases() {
		alias(BuiltInRegistries.ITEM, ITEMS);
		alias(BuiltInRegistries.ENTITY_TYPE, ENTITIES);
		alias(BuiltInRegistries.MOB_EFFECT, EFFECTS);
		alias(BuiltInRegistries.DATA_COMPONENT_TYPE, COMPONENTS);
		alias(TechniqueBehaviors.REGISTRY, TECHNIQUE_BEHAVIORS);
	}

	private static void alias(Registry<?> registry, Map<String, String> table) {
		table.forEach((from, to) -> registry.addAlias(JjkCraft.id(from), JjkCraft.id(to)));
	}

	private static Codec<Identifier> aliased(Map<String, String> table) {
		return Identifier.CODEC.xmap(id -> map(table, id), Function.identity());
	}

	private static Identifier map(Map<String, String> table, Identifier id) {
		if (!JjkCraft.MOD_ID.equals(id.getNamespace())) {
			return id;
		}

		String renamed = table.get(id.getPath());
		if (renamed == null) {
			return id;
		}

		Identifier to = JjkCraft.id(renamed);
		if (WARNED.add(id)) {
			JjkCraft.LOGGER.warn("Old id {} read; it is now {} (docs/NAMING.md)", id, to);
		}
		return to;
	}
}
