package com.jjkcraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.domain.DomainState;
import com.jjkcraft.technique.TechniqueState;

/** The canon naming pass's alias table (docs/NAMING.md): old saves read as the renamed ids. */
class LegacyIdsTest {
	private static final List<Map<String, String>> TABLES = List.of(LegacyIds.TECHNIQUES, LegacyIds.DOMAINS, LegacyIds.CULLING_RULES,
			LegacyIds.ITEMS, LegacyIds.ENTITIES, LegacyIds.EFFECTS, LegacyIds.COMPONENTS, LegacyIds.TECHNIQUE_BEHAVIORS);

	@Test
	void oldIdsMapToNewOnesAndOthersPassThrough() {
		assertEquals(JjkCraft.id("lapse_blue"), LegacyIds.technique(JjkCraft.id("inward_hollow")));
		assertEquals(JjkCraft.id("malevolent_shrine"), LegacyIds.domain(JjkCraft.id("hanging_edges")));
		assertEquals(JjkCraft.id("grudge_bolt"), LegacyIds.technique(JjkCraft.id("grudge_bolt")), "unrenamed ids are kept");
		Identifier foreign = Identifier.fromNamespaceAndPath("otherpack", "inward_hollow");
		assertEquals(foreign, LegacyIds.technique(foreign), "only the mod's namespace is mapped");
	}

	@Test
	void noOldIdIsAlsoANewIdOfTheSameKind() {
		for (Map<String, String> table : TABLES) {
			Set<String> overlap = new HashSet<>(table.keySet());
			overlap.retainAll(table.values());
			assertTrue(overlap.isEmpty(), "an id is both old and new: " + overlap);
		}
	}

	@Test
	void everyRenamedContentIdHasItsDataFileAndNoOldFileRemains() {
		check("technique", LegacyIds.TECHNIQUES);
		check("domain", LegacyIds.DOMAINS);
		check("culling_rule", LegacyIds.CULLING_RULES);
		check("cursed_tool", Map.of("inverted_tine", "inverted_spear_of_heaven", "split_soul_nodachi", "split_soul_katana",
				"thousand_link_chainwhip", "chain_of_a_thousand_miles"));
	}

	private static void check(String kind, Map<String, String> table) {
		table.forEach((from, to) -> {
			assertNotNull(LegacyIdsTest.class.getResource("/data/jjkcraft/jjkcraft/" + kind + "/" + to + ".json"), kind + " " + to + " exists");
			assertNull(LegacyIdsTest.class.getResource("/data/jjkcraft/jjkcraft/" + kind + "/" + from + ".json"), kind + " " + from + " is gone");
		});
	}

	@Test
	void anOldPlayerSaveLoadsWithTheNewIds() {
		String save = """
				{"innate": "jjkcraft:inward_hollow", "learned": ["jjkcraft:crescent_rend", "jjkcraft:grudge_bolt"],
				 "loadout": ["jjkcraft:inward_hollow", "", "jjkcraft:crescent_rend", "", "", ""],
				 "mastery": {"jjkcraft:inward_hollow": 12}, "cooldowns": {}}""";
		TechniqueState state = TechniqueState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(save)).getOrThrow();
		assertEquals(Optional.of(JjkCraft.id("lapse_blue")), state.innate());
		assertEquals(List.of(JjkCraft.id("cleave"), JjkCraft.id("grudge_bolt")), state.learned());
		assertEquals(Optional.of(JjkCraft.id("lapse_blue")), state.slot(0));
		assertEquals(Optional.of(JjkCraft.id("cleave")), state.slot(2));
		assertEquals(12, state.mastery().get(JjkCraft.id("lapse_blue")));

		DomainState domain = DomainState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"known\": \"jjkcraft:boundless_index\"}")).getOrThrow();
		assertEquals(Optional.of(JjkCraft.id("unlimited_void")), domain.known());
	}
}
