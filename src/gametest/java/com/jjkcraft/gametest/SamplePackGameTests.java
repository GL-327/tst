package com.jjkcraft.gametest;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.culling.rule.CullingRule;
import com.jjkcraft.domain.DomainDefinition;
import com.jjkcraft.energy.EnergyProfile;
import com.jjkcraft.item.tool.CursedToolDefinition;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.vow.VowDefinition;

/**
 * The sample data pack in {@code samples/} must stay valid: every file decodes with the codec the
 * world would use, and the ids it points at exist (in the mod or in the pack itself).
 */
public class SamplePackGameTests {
	private static final Map<String, Codec<?>> CODECS = Map.of(
			"technique", TechniqueDefinition.CODEC,
			"domain", DomainDefinition.CODEC,
			"vow", VowDefinition.CODEC,
			"culling_rule", CullingRule.CODEC,
			"energy_profile", EnergyProfile.CODEC,
			"cursed_tool", CursedToolDefinition.CODEC);

	/** The project's {@code samples/jjkcraft-sample-pack}, found from wherever the test runs. */
	private static Path pack() {
		Path dir = Path.of("").toAbsolutePath();
		while (dir != null) {
			Path candidate = dir.resolve("samples/jjkcraft-sample-pack");
			if (Files.isDirectory(candidate)) {
				return candidate;
			}

			dir = dir.getParent();
		}

		return null;
	}

	@GameTest
	public void theSamplePackDecodes(GameTestHelper helper) throws IOException {
		Path pack = pack();
		helper.assertTrue(pack != null, "samples/jjkcraft-sample-pack is found");
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		List<Path> files;
		try (Stream<Path> walk = Files.walk(pack.resolve("data"))) {
			files = walk.filter(p -> p.toString().endsWith(".json")).toList();
		}

		helper.assertTrue(files.size() >= 8, "the sample's files are there, found " + files.size());
		Set<String> sampleIds = new HashSet<>();
		int decoded = 0;
		for (Path file : files) {
			Path relative = pack.resolve("data").relativize(file);
			if (relative.getNameCount() < 4 || !relative.getName(1).toString().equals("jjkcraft")) {
				continue;
			}

			String namespace = relative.getName(0).toString();
			String registry = relative.getName(2).toString();
			Codec<?> codec = CODECS.get(registry);
			if (codec == null) {
				continue;
			}

			JsonElement json;
			try (Reader reader = Files.newBufferedReader(file)) {
				json = JsonParser.parseReader(reader);
			}

			DataResult<?> result = codec.parse(ops, json);
			helper.assertTrue(result.result().isPresent(), relative + " decodes: " + result.error().map(e -> e.message()).orElse(""));
			sampleIds.add(namespace + ":" + file.getFileName().toString().replace(".json", ""));
			decoded++;
		}

		helper.assertTrue(decoded >= 7, "seven content files decode, did " + decoded);
		helper.assertTrue(sampleIds.contains("jjksample:ember_lance") && sampleIds.contains("jjksample:still_feet"),
				"the ids other sample files point at are in the pack");
		String meta = Files.readString(pack.resolve("pack.mcmeta"));
		helper.assertTrue(meta.contains("\"min_format\": 121"), "the pack targets data format 121 (26.3)");
		helper.succeed();
	}
}
