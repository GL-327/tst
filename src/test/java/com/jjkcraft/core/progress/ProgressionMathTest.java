package com.jjkcraft.core.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jjkcraft.core.progress.ProgressionMath.Blocker;
import com.jjkcraft.core.progress.ProgressionMath.Requirements;
import com.jjkcraft.core.progress.ProgressionMath.Standing;
import com.jjkcraft.core.progress.ProgressionMath.TrialKind;
import com.jjkcraft.core.progress.ProgressionMath.TrialRequirement;

class ProgressionMathTest {
	private static final Requirements GRADE_3 = new Requirements(300, 0.30, 0, false,
			List.of(new TrialRequirement("defeat", TrialKind.DEFEAT_CURSE, 0, 5)));

	private static Standing standing(int xp, double control, int casts, Map<String, Integer> counters, boolean heavenly) {
		return new Standing(xp, control, 1000, casts, counters, Set.of(), heavenly);
	}

	@Test
	void killXpGrowsWithRankAndClamps() {
		assertEquals(10, ProgressionMath.killXp(0));
		assertEquals(500, ProgressionMath.killXp(6));
		assertEquals(500, ProgressionMath.killXp(99));
		assertEquals(10, ProgressionMath.killXp(-3));
	}

	@Test
	void growthPointsFromXpAndRank() {
		assertEquals(0, ProgressionMath.pointsEarned(199, 0));
		assertEquals(1, ProgressionMath.pointsEarned(200, 0));
		assertEquals(4 + 10, ProgressionMath.pointsEarned(800, 5));
		assertEquals(0, ProgressionMath.pointsUnspent(200, 0, 5));
	}

	@Test
	void allBlockersReported() {
		List<Blocker> blockers = ProgressionMath.promotionBlockers(GRADE_3, standing(100, 0.9, 3, Map.of(), false));
		assertTrue(blockers.contains(Blocker.XP));
		assertTrue(blockers.contains(Blocker.CONTROL), "fewer than 10 casts blocks control");
		assertTrue(blockers.contains(Blocker.TRIAL));
	}

	@Test
	void qualifiesWhenEverythingIsMet() {
		Standing s = standing(300, 0.31, 10, Map.of(ProgressionMath.curseKillKey(2), 5), false);
		assertEquals(List.of(), ProgressionMath.promotionBlockers(GRADE_3, s));
	}

	@Test
	void defeatCurseCountsKillsAtOrAboveTheRank() {
		Map<String, Integer> counters = Map.of(
				ProgressionMath.curseKillKey(1), 2,
				ProgressionMath.curseKillKey(3), 1,
				ProgressionMath.curseKillKey(5), 4);
		assertEquals(7, ProgressionMath.trialProgress(TrialKind.DEFEAT_CURSE, 0, counters));
		assertEquals(5, ProgressionMath.trialProgress(TrialKind.DEFEAT_CURSE, 3, counters));
		assertEquals(0, ProgressionMath.trialProgress(TrialKind.DEFEAT_CURSE, 6, counters));
	}

	@Test
	void heavenlyRestrictionSkipsControlAndOutput() {
		Requirements req = new Requirements(0, 0.5, 5000, false, List.of());
		assertEquals(List.of(), ProgressionMath.promotionBlockers(req, new Standing(0, 0, 0, 0, Map.of(), Set.of(), true)));
	}

	@Test
	void specialGradeNeedsTheRitualEvenWithEndlessGrinding() {
		Requirements special = new Requirements(0, 0, 0, true,
				List.of(new TrialRequirement("ritual", TrialKind.UNBOUND_RITUAL, 5, 1)));
		Standing grinder = standing(10_000_000, 1.0, 100_000, Map.of(ProgressionMath.curseKillKey(6), 9999), false);
		assertEquals(List.of(Blocker.SPECIAL_RITUAL), ProgressionMath.promotionBlockers(special, grinder));

		Standing ritual = standing(0, 1.0, 100, Map.of(ProgressionMath.UNBOUND_RITUAL, 1), false);
		assertEquals(List.of(), ProgressionMath.promotionBlockers(special, ritual));
	}

	@Test
	void specialGradeWithoutRitualTrialIsRefused() {
		Requirements broken = new Requirements(0, 0, 0, true, List.of());
		assertEquals(List.of(Blocker.SPECIAL_RITUAL), ProgressionMath.promotionBlockers(broken, standing(0, 1, 100, Map.of(), false)));
	}

	@Test
	void growthEffectsAreCapped() {
		assertEquals(0.70, ProgressionMath.costMultiplier(99), 1e-9);
		assertEquals(0.70, ProgressionMath.cooldownMultiplier(20), 1e-9);
		assertEquals(1.6, ProgressionMath.domainPowerMultiplier(20), 1e-9);
	}
}
