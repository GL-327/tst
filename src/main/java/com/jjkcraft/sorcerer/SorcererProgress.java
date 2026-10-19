package com.jjkcraft.sorcerer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/**
 * A player's sorcerer progression. Immutable; use the {@code with...} methods and store the result.
 *
 * @param awakened           whether the player has awakened (rolled an innate technique or Heavenly Restriction)
 * @param awakenedAtTick     game time of awakening, or -1
 * @param heavenlyRestricted Heavenly Restriction: no CE, enhanced body
 * @param grade              id of the current grade definition
 * @param gradeXp            grade experience accumulated toward the next grade
 * @param growth             points spent in the four growth paths (unspent points are derived; see Progression)
 * @param trials             ids of completed trial achievements (see grade data)
 * @param counters           progress counters that trials read, for example {@code kill/jjkcraft:grade_2}
 * @param specialUnlocked    the one-time Special Grade unlock ("The Unbound Ritual") has been completed
 * @param ceSpentTotal       lifetime CE spent on techniques (feeds Output)
 * @param castsLanded        casts that hit or completed successfully (feeds Control)
 * @param castsAttempted     all casts started (feeds Control)
 */
public record SorcererProgress(
		boolean awakened,
		long awakenedAtTick,
		boolean heavenlyRestricted,
		Identifier grade,
		int gradeXp,
		Growth growth,
		Set<String> trials,
		Map<String, Integer> counters,
		boolean specialUnlocked,
		double ceSpentTotal,
		int castsLanded,
		int castsAttempted
) {
	public static final Identifier STARTING_GRADE = JjkCraft.id("grade_4");

	public static final SorcererProgress DEFAULT = new SorcererProgress(
			false, -1, false, STARTING_GRADE, 0, Growth.NONE, Set.of(), Map.of(), false, 0, 0, 0);

	public static final Codec<SorcererProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("awakened", false).forGetter(SorcererProgress::awakened),
			Codec.LONG.optionalFieldOf("awakened_at", -1L).forGetter(SorcererProgress::awakenedAtTick),
			Codec.BOOL.optionalFieldOf("heavenly_restricted", false).forGetter(SorcererProgress::heavenlyRestricted),
			Identifier.CODEC.optionalFieldOf("grade", STARTING_GRADE).forGetter(SorcererProgress::grade),
			Codec.INT.optionalFieldOf("grade_xp", 0).forGetter(SorcererProgress::gradeXp),
			Growth.CODEC.optionalFieldOf("growth", Growth.NONE).forGetter(SorcererProgress::growth),
			Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("trials", Set.of()).forGetter(SorcererProgress::trials),
			Codec.unboundedMap(Codec.STRING, Codec.INT).<Map<String, Integer>>xmap(Map::copyOf, Map::copyOf)
					.optionalFieldOf("counters", Map.of()).forGetter(SorcererProgress::counters),
			Codec.BOOL.optionalFieldOf("special_unlocked", false).forGetter(SorcererProgress::specialUnlocked),
			Codec.DOUBLE.optionalFieldOf("ce_spent_total", 0.0).forGetter(SorcererProgress::ceSpentTotal),
			Codec.INT.optionalFieldOf("casts_landed", 0).forGetter(SorcererProgress::castsLanded),
			Codec.INT.optionalFieldOf("casts_attempted", 0).forGetter(SorcererProgress::castsAttempted)
	).apply(i, SorcererProgress::new));

	/**
	 * Points spent in the Growth Tree paths.
	 *
	 * @param output     raises CE capacity and technique damage
	 * @param control    lowers CE cost and raises Black Flash chance
	 * @param efficiency raises regen and lowers cooldowns
	 * @param domain     strengthens domains and clash power
	 */
	public record Growth(int output, int control, int efficiency, int domain) {
		public static final Growth NONE = new Growth(0, 0, 0, 0);

		public static final Codec<Growth> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.optionalFieldOf("output", 0).forGetter(Growth::output),
				Codec.INT.optionalFieldOf("control", 0).forGetter(Growth::control),
				Codec.INT.optionalFieldOf("efficiency", 0).forGetter(Growth::efficiency),
				Codec.INT.optionalFieldOf("domain", 0).forGetter(Growth::domain)
		).apply(i, Growth::new));

		public int total() {
			return output + control + efficiency + domain;
		}

		public int get(GrowthPath path) {
			return switch (path) {
				case OUTPUT -> output;
				case CONTROL -> control;
				case EFFICIENCY -> efficiency;
				case DOMAIN -> domain;
			};
		}

		public Growth plus(GrowthPath path) {
			return switch (path) {
				case OUTPUT -> new Growth(output + 1, control, efficiency, domain);
				case CONTROL -> new Growth(output, control + 1, efficiency, domain);
				case EFFICIENCY -> new Growth(output, control, efficiency + 1, domain);
				case DOMAIN -> new Growth(output, control, efficiency, domain + 1);
			};
		}
	}

	public int counter(String key) {
		return counters.getOrDefault(key, 0);
	}

	public SorcererProgress withAwakened(long tick, boolean heavenly) {
		return new SorcererProgress(true, tick, heavenly, grade, gradeXp, growth, trials, counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withGrade(Identifier newGrade, int newXp) {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, newGrade, newXp, growth, trials, counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withXp(int newXp) {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, newXp, growth, trials, counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withGrowth(Growth newGrowth) {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, newGrowth, trials, counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withTrial(String trial) {
		Set<String> next = new HashSet<>(trials);
		next.add(trial);
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, growth, Set.copyOf(next), counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withCounterAdded(String key, int amount) {
		Map<String, Integer> next = new HashMap<>(counters);
		next.merge(key, amount, Integer::sum);
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, growth, trials, Map.copyOf(next), specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withSpecialUnlocked() {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, growth, trials, counters, true, ceSpentTotal, castsLanded, castsAttempted);
	}

	public SorcererProgress withCast(double ceSpent, boolean landed) {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, growth, trials, counters, specialUnlocked,
				ceSpentTotal + ceSpent, castsLanded + (landed ? 1 : 0), castsAttempted + 1);
	}

	/** Credits a hit that landed after its cast was recorded (a bolt reaching its target). */
	public SorcererProgress withLandedCredit() {
		return new SorcererProgress(awakened, awakenedAtTick, heavenlyRestricted, grade, gradeXp, growth, trials, counters, specialUnlocked,
				ceSpentTotal, Math.min(castsAttempted, castsLanded + 1), castsAttempted);
	}

	public SorcererProgress withHeavenlyRestriction(boolean value) {
		return new SorcererProgress(awakened, awakenedAtTick, value, grade, gradeXp, growth, trials, counters, specialUnlocked, ceSpentTotal, castsLanded, castsAttempted);
	}

	/** Cast accuracy in [0, 1]; 0.5 before any casts. */
	public double controlRatio() {
		return castsAttempted == 0 ? 0.5 : (double) castsLanded / castsAttempted;
	}
}
