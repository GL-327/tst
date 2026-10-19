package com.jjkcraft.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import com.jjkcraft.core.technique.TechniqueMath;

/**
 * The balance pass as a test: it reads the shipped technique, curse and grade data and checks the
 * budgets BALANCE.md describes, so a data change that breaks them fails the build.
 */
class BalanceSimulationTest {
	private static final Path DATA = Path.of("src/main/resources/data/jjkcraft/jjkcraft");
	private static final List<String> LADDER = List.of("grade_4", "grade_3", "semi_grade_2", "grade_2", "semi_grade_1", "grade_1", "special_grade");
	/** Grades whose curses are fought alone; special-grade bosses are meant for groups or many pools. */
	private static final int SOLO_LIMIT = LADDER.indexOf("grade_1");

	private record Technique(String id, int rank, String category, double damage, double cost) {
		double perCe() {
			return damage / cost;
		}
	}

	private record Curse(String id, int rank, double health) {
	}

	private static Map<String, JsonObject> read(String registry) throws IOException {
		Map<String, JsonObject> out = new LinkedHashMap<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(DATA.resolve(registry), "*.json")) {
			for (Path file : files) {
				try (Reader reader = Files.newBufferedReader(file)) {
					out.put(file.getFileName().toString().replace(".json", ""), JsonParser.parseReader(reader).getAsJsonObject());
				}
			}
		}

		return out;
	}

	private static int rank(String grade) {
		return LADDER.indexOf(grade.substring(grade.indexOf(':') + 1));
	}

	/** Damage one cast deals to one target, at base power. */
	private static double damage(JsonObject behavior) {
		String type = behavior.get("type").getAsString().replace("jjkcraft:", "");
		double base = behavior.has("damage") ? behavior.get("damage").getAsDouble() : 0;
		return switch (type) {
			case "strike", "line_cut", "vow_strike", "shockwave", "arc_slash", "singularity" -> base;
			case "projectile" -> base * (behavior.has("count") ? behavior.get("count").getAsInt() : 1);
			case "straw_doll" -> base * (behavior.has("nails") ? behavior.get("nails").getAsInt() : 3);
			// Half the lances seek someone in the rain: about 15 of 30 land on one target.
			case "barrage" -> base * 15;
			default -> 0;
		};
	}

	private static List<Technique> techniques() throws IOException {
		List<Technique> out = new ArrayList<>();
		for (Map.Entry<String, JsonObject> e : read("technique").entrySet()) {
			JsonObject json = e.getValue();
			double damage = damage(json.getAsJsonObject("behavior"));
			JsonObject cast = json.getAsJsonObject("cast");
			if (cast.has("mul_cost") && cast.get("cost").getAsDouble() == 0) {
				// Paid in Mul energy only (the Modulo expansion): checked by mulPaidTechniquesStayUnderTheirCeiling.
				continue;
			}

			if (damage > 0) {
				out.add(new Technique(e.getKey(), rank(json.get("grade").getAsString()), json.get("category").getAsString(), damage,
						json.getAsJsonObject("cast").get("cost").getAsDouble()));
			}
		}

		return out;
	}

	private static List<Curse> curses() throws IOException {
		List<Curse> out = new ArrayList<>();
		for (Map.Entry<String, JsonObject> e : read("curse").entrySet()) {
			JsonObject json = e.getValue();
			out.add(new Curse(e.getKey(), rank(json.get("grade").getAsString()), json.getAsJsonObject("stats").get("health").getAsDouble()));
		}

		return out;
	}

	@Test
	void everyGradeAndCurseIsOnTheLadder() throws IOException {
		assertTrue(read("grade").keySet().containsAll(LADDER), "the seven grades");
		for (Curse curse : curses()) {
			assertTrue(curse.rank() >= 0, curse.id() + " has a ladder grade");
		}
	}

	@Test
	void ordinaryTechniquesStayUnderADamagePerCeCeiling() throws IOException {
		for (Technique t : techniques()) {
			if (!t.category().equals("ultimate")) {
				assertTrue(t.perCe() <= 0.9, t.id() + " deals " + t.perCe() + " per CE");
			}
		}
	}

	/**
	 * Techniques paid in Mul energy instead of CE. One Mul energy costs 1 / mulEnergyRatio CE (2 at
	 * the default 0.5), so the same 0.9-per-CE ceiling is 1.8 per Mul energy (BALANCE.md section 10).
	 */
	@Test
	void mulPaidTechniquesStayUnderTheirCeiling() throws IOException {
		Path dir = DATA.resolve("technique");
		try (var files = Files.list(dir)) {
			for (Path file : files.toList()) {
				JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				JsonObject cast = json.getAsJsonObject("cast");
				if (!cast.has("mul_cost")) {
					continue;
				}

				double perMul = damage(json.getAsJsonObject("behavior")) / cast.get("mul_cost").getAsDouble();
				assertTrue(perMul <= 1.8, file.getFileName() + " deals " + perMul + " per Mul energy");
			}
		}
	}

	@Test
	void ultimatesCantOneShotACurseOfTheirTier() throws IOException {
		List<Curse> curses = curses();
		for (Technique t : techniques()) {
			if (!t.category().equals("ultimate") && t.cost() < 150) {
				continue;
			}

			// Fully grown: mastered, 20 Output points.
			double best = t.damage() * TechniqueMath.power(t.rank(), 20, true, 1.0);
			int tier = Math.max(t.rank(), LADDER.indexOf("grade_1"));
			double weakest = curses.stream().filter(c -> c.rank() == tier).mapToDouble(Curse::health).min().orElse(Double.MAX_VALUE);
			assertTrue(best < weakest, t.id() + " hits for " + best + ", the weakest curse of its tier has " + weakest);
		}
	}

	@Test
	void aSorcererCanFinishACurseOfTheirGradeOnOnePool() throws IOException {
		List<Technique> techniques = techniques();
		for (Curse curse : curses()) {
			if (curse.rank() > SOLO_LIMIT) {
				continue;
			}

			// The most CE-efficient technique a sorcerer of that grade has, at base growth.
			double power = TechniqueMath.power(curse.rank(), 0, false, 1.0);
			double bestPerCe = techniques.stream().filter(t -> t.rank() <= curse.rank()).mapToDouble(Technique::perCe).max().orElse(0) * power;
			double pool = 100 * multiplier(curse.rank());
			double needed = curse.health() / bestPerCe;
			assertTrue(needed <= pool, curse.id() + " needs " + needed + " CE, a " + LADDER.get(curse.rank()) + " has " + pool);
		}
	}

	@Test
	void noOrdinaryTechniqueOneShotsACurseOfItsOwnGrade() throws IOException {
		List<Curse> curses = curses();
		for (Technique t : techniques()) {
			if (t.category().equals("ultimate")) {
				continue;
			}

			double hit = t.damage() * TechniqueMath.power(t.rank(), 0, false, 1.0);
			for (Curse curse : curses) {
				if (curse.rank() == t.rank() && curse.rank() >= 1) {
					assertFalse(hit >= curse.health(), t.id() + " one-shots " + curse.id());
				}
			}
		}
	}

	/** Grade CE multipliers, as in the grade data. */
	private static double multiplier(int rank) {
		return new double[] {1.0, 1.4, 1.8, 2.3, 2.9, 3.6, 5.0}[rank];
	}
}
