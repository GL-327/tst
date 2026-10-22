package com.jjkcraft.core.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TechniqueMathTest {
	private static final double EPS = 1e-9;
    @Test void offensiveMultiplierIsIndependentAndBounded(){assertEquals(30,TechniqueMath.damage(10,TechniqueMath.DEFAULT_DAMAGE_MULTIPLIER));assertEquals(1,TechniqueMath.damage(10,-5));assertEquals(100,TechniqueMath.damage(10,50));assertEquals(30,TechniqueMath.damage(10,Double.NaN));}

	@Test
	void powerCombinesRankOutputMasteryAndVows() {
		assertEquals(1.0, TechniqueMath.power(0, 0, false, 1.0), EPS);
		assertEquals(1.4 * 1.2 * 1.6 * 1.35, TechniqueMath.power(5, 10, true, 1.35), EPS);
	}

	@Test
	void costReductionsStack() {
		assertEquals(10.0, TechniqueMath.cost(10, 0, false, 1), EPS);
		assertEquals(10 * 0.7 * 0.85, TechniqueMath.cost(10, 40, true, 1), EPS);
	}

	@Test
	void cooldownNeverDropsToZero() {
		assertEquals(0, TechniqueMath.cooldown(0, 20));
		assertEquals(1, TechniqueMath.cooldown(1, 20));
		assertEquals(28, TechniqueMath.cooldown(40, 20));
	}

	@Test
	void ceScalingIsCapped() {
		assertEquals(6.0, TechniqueMath.ceScaledDamage(6, 0.6, 0), EPS);
		assertEquals(6 * 1.6, TechniqueMath.ceScaledDamage(6, 0.6, 100), EPS);
		assertEquals(6 * (1 + 0.6 * TechniqueMath.MAX_CE_RATIO), TechniqueMath.ceScaledDamage(6, 0.6, 1_000_000), EPS);
	}

	@Test
	void armorSplitSumsToTotal() {
		double[] parts = TechniqueMath.armorSplit(9, 0.45);
		assertEquals(9, parts[0] + parts[1], EPS);
		assertEquals(9 * 0.45, parts[1], EPS);
		assertEquals(9, TechniqueMath.armorSplit(9, 3)[1], EPS);
	}

	@Test
	void falloffRunsFromCenterToEdge() {
		assertEquals(1.0, TechniqueMath.falloff(0, 5, 0.5), EPS);
		assertEquals(0.5, TechniqueMath.falloff(5, 5, 0.5), EPS);
		assertEquals(0.5, TechniqueMath.falloff(50, 5, 0.5), EPS);
	}

	/**
	 * Balance guard: the strongest Grade 4 technique on a fresh Grade 4 sorcerer must not one-shot
	 * a full-health player (20), and the merge technique at Grade 1 with full Output and mastery
	 * must stay under the 600 CE pool of the strongest non-special curses' health budget.
	 */
	@Test
	void maxDamageStaysInBudget() {
		double palm = 7 * TechniqueMath.power(0, 0, false, 1.0);
		assertTrue(palm < 20, "Grade 4 palm one-shots a player: " + palm);
		double merge = 40 * TechniqueMath.power(5, 20, true, 1.0);
		assertTrue(merge < 150, "merge technique too strong at Grade 1: " + merge);
	}
}
