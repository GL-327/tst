package com.jjkcraft.core.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure progression formulas: experience, growth points, growth effects, trial progress and
 * promotion checks. No Minecraft types; unit-tested in {@code ProgressionMathTest}.
 */
public final class ProgressionMath {
	/** Max points in one growth path. */
	public static final int MAX_POINTS_PER_PATH = 20;
	/** Grade experience per growth point. */
	public static final int XP_PER_POINT = 200;
	/** Bonus growth points granted per grade rank reached. */
	public static final int POINTS_PER_RANK = 2;

	/** Grade experience for defeating a cursed-capable enemy of each rank (Grade 4 = 0 ... Special = 6). */
	private static final int[] KILL_XP = {10, 25, 50, 90, 150, 240, 500};

	/** Counter keys stored on {@code SorcererProgress.counters}. */
	public static final String CURSE_KILLS_PREFIX = "curse_kills/";
	public static final String MASTERED = "mastered";
	public static final String CULLING_POINTS = "culling_points";
	public static final String VOW_TICKS = "vow_ticks";
	public static final String CLASH_WINS = "clash_wins";
	public static final String UNBOUND_RITUAL = "unbound_ritual";

	private ProgressionMath() {
	}

	public static int killXp(int rank) {
		return KILL_XP[Math.clamp(rank, 0, KILL_XP.length - 1)];
	}

	public static String curseKillKey(int rank) {
		return CURSE_KILLS_PREFIX + rank;
	}

	/** Growth points earned from total experience and the highest rank reached. */
	public static int pointsEarned(int totalXp, int rank) {
		return Math.max(0, totalXp) / XP_PER_POINT + POINTS_PER_RANK * Math.max(0, rank);
	}

	public static int pointsUnspent(int totalXp, int rank, int spent) {
		return Math.max(0, pointsEarned(totalXp, rank) - spent);
	}

	// ---------------------------------------------------------------- growth effects

	/** Technique damage multiplier from Output points (+2% each). */
	public static double damageMultiplier(int output) {
		return 1.0 + 0.02 * output;
	}

	/** Technique CE cost multiplier from Control points (-1.5% each). */
	public static double costMultiplier(int control) {
		return 1.0 - 0.015 * Math.min(control, MAX_POINTS_PER_PATH);
	}

	/** Cooldown multiplier from Efficiency points (-1.5% each). */
	public static double cooldownMultiplier(int efficiency) {
		return 1.0 - 0.015 * Math.min(efficiency, MAX_POINTS_PER_PATH);
	}

	/** Black Flash chance bonus in percentage points from Control points. */
	public static double blackFlashBonus(int control) {
		return 0.25 * control;
	}

	/** Domain clash power multiplier from Domain points (+3% each). */
	public static double domainPowerMultiplier(int domain) {
		return 1.0 + 0.03 * domain;
	}

	/** Domain CE cost multiplier from Domain points (-2% each). */
	public static double domainCostMultiplier(int domain) {
		return 1.0 - 0.02 * Math.min(domain, MAX_POINTS_PER_PATH);
	}

	// ---------------------------------------------------------------- trials and promotion

	/** The kinds of trial progress the math understands. Mirrors {@code TrialType}. */
	public enum TrialKind {
		DEFEAT_CURSE, MASTER_TECHNIQUES, CULLING_POINTS, VOW_DAYS, DOMAIN_CLASH_WINS, UNBOUND_RITUAL
	}

	/** Current progress toward a trial, in the same unit as its {@code count}. */
	public static int trialProgress(TrialKind kind, int minRank, Map<String, Integer> counters) {
		return switch (kind) {
			case DEFEAT_CURSE -> {
				int sum = 0;
				for (int rank = Math.max(0, minRank); rank < KILL_XP.length; rank++) {
					sum += counters.getOrDefault(curseKillKey(rank), 0);
				}

				yield sum;
			}
			case MASTER_TECHNIQUES -> counters.getOrDefault(MASTERED, 0);
			case CULLING_POINTS -> counters.getOrDefault(CULLING_POINTS, 0);
			case VOW_DAYS -> counters.getOrDefault(VOW_TICKS, 0) / 24000;
			case DOMAIN_CLASH_WINS -> counters.getOrDefault(CLASH_WINS, 0);
			case UNBOUND_RITUAL -> counters.getOrDefault(UNBOUND_RITUAL, 0);
		};
	}

	/** A trial's requirement as the promotion check sees it. */
	public record TrialRequirement(String id, TrialKind kind, int minRank, int count) {
	}

	/** What a grade asks for, flattened from grade data. */
	public record Requirements(int xpRequired, double minControl, double minOutput, boolean special, List<TrialRequirement> trials) {
	}

	/** The sorcerer's numbers that a promotion check reads. */
	public record Standing(int totalXp, double controlRatio, double ceSpentTotal, int castsAttempted, Map<String, Integer> counters,
			Set<String> completedTrials, boolean heavenlyRestricted) {
	}

	/** Why a promotion is blocked. Empty means the sorcerer qualifies. */
	public enum Blocker {
		XP, CONTROL, OUTPUT, TRIAL, SPECIAL_RITUAL
	}

	public static List<Blocker> promotionBlockers(Requirements next, Standing standing) {
		List<Blocker> blockers = new ArrayList<>();
		if (standing.totalXp() < next.xpRequired()) {
			blockers.add(Blocker.XP);
		}

		// Heavenly Restriction has no CE output or casts; physical trials replace those checks.
		if (!standing.heavenlyRestricted()) {
			if (next.minControl() > 0 && (standing.castsAttempted() < 10 || standing.controlRatio() < next.minControl())) {
				blockers.add(Blocker.CONTROL);
			}

			if (standing.ceSpentTotal() < next.minOutput()) {
				blockers.add(Blocker.OUTPUT);
			}
		}

		for (TrialRequirement trial : next.trials()) {
			boolean done = standing.completedTrials().contains(trial.id())
					|| trialProgress(trial.kind(), trial.minRank(), standing.counters()) >= trial.count();
			if (!done) {
				blockers.add(trial.kind() == TrialKind.UNBOUND_RITUAL ? Blocker.SPECIAL_RITUAL : Blocker.TRIAL);
			}
		}

		if (next.special() && next.trials().stream().noneMatch(t -> t.kind() == TrialKind.UNBOUND_RITUAL)) {
			// A special grade without a ritual trial would be reachable by grinding; refuse it.
			blockers.add(Blocker.SPECIAL_RITUAL);
		}

		return blockers;
	}
}
