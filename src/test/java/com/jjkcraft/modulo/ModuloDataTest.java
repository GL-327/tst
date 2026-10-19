package com.jjkcraft.modulo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import com.jjkcraft.modulo.data.BossDefinition;
import com.jjkcraft.modulo.data.MissionDefinition;
import com.jjkcraft.modulo.data.MissionStage;
import com.jjkcraft.modulo.data.ModuloCharacters;
import com.jjkcraft.modulo.data.SiteDefinition;

/**
 * The shipped Modulo data decodes with the world's codecs and hangs together: eleven missions in
 * order, every site, boss and character a stage names exists, every character who must be talked
 * to stands at the stage's site, and every text key is in the English lang file.
 */
class ModuloDataTest {
	private static final Path DATA = Path.of("src/main/resources/data/modulo/jjkcraft");
	private static final Path LANG = Path.of("src/main/generated/assets/jjkcraft/lang/en_us.json");

	private static <T> Map<String, T> load(String kind, Codec<T> codec) throws IOException {
		Map<String, T> out = new LinkedHashMap<>();
		try (var files = Files.list(DATA.resolve(kind))) {
			for (Path file : files.sorted().toList()) {
				JsonElement json = JsonParser.parseString(Files.readString(file));
				String id = "modulo:" + file.getFileName().toString().replace(".json", "");
				out.put(id, codec.parse(JsonOps.INSTANCE, json).getOrThrow(message -> new AssertionError(id + ": " + message)));
			}
		}

		return out;
	}

	private static Set<String> langKeys() throws IOException {
		JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
		return lang.keySet();
	}

	@Test
	void elevenMissionsInOrder() throws IOException {
		Map<String, MissionDefinition> missions = load("modulo_mission", MissionDefinition.CODEC);
		assertEquals(11, missions.size());
		List<Integer> orders = new ArrayList<>(missions.values().stream().map(MissionDefinition::order).sorted().toList());
		assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), orders);
	}

	@Test
	void stagesNameRealSitesBossesAndCharacters() throws IOException {
		Map<String, MissionDefinition> missions = load("modulo_mission", MissionDefinition.CODEC);
		Map<String, SiteDefinition> sites = load("modulo_site", SiteDefinition.CODEC);
		Map<String, BossDefinition> bosses = load("modulo_boss", BossDefinition.CODEC);
		for (Map.Entry<String, MissionDefinition> mission : missions.entrySet()) {
			boolean companion = mission.getValue().order() > 1;
			for (MissionStage stage : mission.getValue().stages()) {
				String where = mission.getKey() + " " + stage.type() + " " + stage.base().objective();
				Optional<SiteDefinition> site = stage.base().site().map(id -> sites.get(id.toString()));
				stage.base().site().ifPresent(id -> assertTrue(sites.containsKey(id.toString()), where + ": no site " + id));
				for (MissionStage.Line line : lines(stage)) {
					assertTrue(line.speaker().equals("narrator") || ModuloCharacters.ALL.contains(line.speaker()), where + ": unknown speaker " + line.speaker());
				}

				String npc = switch (stage) {
					case MissionStage.Talk talk -> talk.npc();
					case MissionStage.Deliver deliver -> deliver.npc();
					case MissionStage.Choice choice -> choice.npc();
					default -> null;
				};
				if (npc != null) {
					boolean atSite = site.isPresent() && site.get().npcs().stream().anyMatch(n -> n.character().equals(npc));
					boolean following = npc.equals("maru") && companion;
					assertTrue(atSite || following, where + ": " + npc + " is not at the stage's site");
				}

				Optional<Identifier> boss = switch (stage) {
					case MissionStage.Defeat defeat -> Optional.of(defeat.boss());
					case MissionStage.Defend defend -> defend.boss();
					default -> Optional.empty();
				};
				boss.ifPresent(id -> assertTrue(bosses.containsKey(id.toString()), where + ": no boss " + id));
				if (stage instanceof MissionStage.Defeat defeat) {
					for (String ally : defeat.allies()) {
						assertTrue(ModuloCharacters.ALL.contains(ally), where + ": unknown ally " + ally);
					}
				}
			}
		}

		for (Map.Entry<String, SiteDefinition> site : sites.entrySet()) {
			for (SiteDefinition.Npc npc : site.getValue().npcs()) {
				assertTrue(ModuloCharacters.ALL.contains(npc.character()), site.getKey() + ": unknown character " + npc.character());
			}
		}
	}

	@Test
	void sitesDoNotOverlap() throws IOException {
		Map<String, SiteDefinition> sites = load("modulo_site", SiteDefinition.CODEC);
		List<Map.Entry<String, SiteDefinition>> all = new ArrayList<>(sites.entrySet());
		for (int i = 0; i < all.size(); i++) {
			for (int j = i + 1; j < all.size(); j++) {
				SiteDefinition a = all.get(i).getValue();
				SiteDefinition b = all.get(j).getValue();
				if (a.dimension().equals(b.dimension())) {
					double gap = Math.sqrt(a.center().distSqr(b.center())) - a.radius() - b.radius();
					assertTrue(gap > 4, all.get(i).getKey() + " and " + all.get(j).getKey() + " overlap");
				}
			}
		}
	}

	@Test
	void everyTextKeyIsInTheLangFile() throws IOException {
		Set<String> keys = langKeys();
		Set<String> missing = new HashSet<>();
		for (MissionDefinition mission : load("modulo_mission", MissionDefinition.CODEC).values()) {
			check(keys, missing, mission.title(), mission.summary());
			for (MissionStage stage : mission.stages()) {
				check(keys, missing, stage.base().objective());
				for (MissionStage.Line line : lines(stage)) {
					check(keys, missing, line.text(), "npc.jjkcraft.modulo." + line.speaker());
				}

				if (stage instanceof MissionStage.Choice choice) {
					for (String option : choice.options()) {
						check(keys, missing, "option.jjkcraft.modulo." + choice.flag() + "." + option);
					}
				}
			}
		}

		for (SiteDefinition site : load("modulo_site", SiteDefinition.CODEC).values()) {
			check(keys, missing, site.name());
		}

		for (BossDefinition boss : load("modulo_boss", BossDefinition.CODEC).values()) {
			check(keys, missing, boss.name());
		}

		for (String character : ModuloCharacters.ALL) {
			check(keys, missing, "npc.jjkcraft.modulo." + character, "npc.jjkcraft.modulo." + character + ".idle");
		}

		for (int i = 1; i <= 6; i++) {
			check(keys, missing, "epilogue.jjkcraft.modulo." + i);
		}

		assertTrue(missing.isEmpty(), "missing lang keys: " + missing);
	}

	private static void check(Set<String> keys, Set<String> missing, String... wanted) {
		for (String key : wanted) {
			if (!keys.contains(key)) {
				missing.add(key);
			}
		}
	}

	private static List<MissionStage.Line> lines(MissionStage stage) {
		List<MissionStage.Line> all = new ArrayList<>(stage.base().intro());
		switch (stage) {
			case MissionStage.Talk talk -> all.addAll(talk.lines());
			case MissionStage.Deliver deliver -> all.addAll(deliver.lines());
			default -> {
			}
		}

		return all;
	}
}
