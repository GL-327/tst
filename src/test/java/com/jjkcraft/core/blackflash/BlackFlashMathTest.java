package com.jjkcraft.core.blackflash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlackFlashMathTest {
	private static final double EPS = 1e-9;

	@Test
	void chargedSwingAlignsBest() {
		assertEquals(6, BlackFlashMath.chance(4, 0, 0, 0, 1.0), EPS);
		assertEquals(4, BlackFlashMath.chance(4, 0, 0, 0, 0.6), EPS);
		assertEquals(2, BlackFlashMath.chance(4, 0, 0, 0, 0.1), EPS);
	}

	@Test
	void gradeControlAndZoneAddUpToACap() {
		// Grade 1 (3 points), 10 Control (2.5 points), charged swing: (4 + 3 + 2.5) * 1.5.
		assertEquals(14.25, BlackFlashMath.chance(4, 3, 2.5, 0, 1.0), EPS);
		// The zone stacks twice at most.
		assertEquals(BlackFlashMath.chance(4, 0, 0, 2, 0.6), BlackFlashMath.chance(4, 0, 0, 5, 0.6), EPS);
		assertEquals(BlackFlashMath.MAX_CHANCE, BlackFlashMath.chance(4, 4, 5, 2, 1.0), EPS);
	}

	@Test
	void fullReservesHitHarder() {
		double empty = BlackFlashMath.multiplier(BlackFlashMath.ceRatio(0, 100), 2.5);
		double full = BlackFlashMath.multiplier(BlackFlashMath.ceRatio(100, 100), 2.5);
		assertTrue(empty > 1.9 && empty < 2.0, "empty pool about 1.93x, got " + empty);
		assertTrue(full > 3.7 && full < 3.8, "full pool about 3.77x, got " + full);
		assertEquals(BlackFlashMath.ceRatio(500, 100), BlackFlashMath.ceRatio(100, 100), EPS);
	}

	/** Max damage against curse health: a fresh Grade 4 palm Black Flash must not one-shot a Grade 2 curse. */
	@Test
	void gradeFourFlashDoesNotOneShotGradeTwo() {
		double palm = 7;
		double best = palm * BlackFlashMath.multiplier(BlackFlashMath.ceRatio(100, 100), 2.5);
		assertTrue(best < 55, "Grade 4 palm Black Flash stays under Cracked Bell's 55 health, got " + best);
	}
}
