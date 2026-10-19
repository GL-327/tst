package com.jjkcraft.core.fear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FearMathTest {
	private static final double EPS = 1e-9;

	@Test
	void clampKeepsFearBetweenZeroAndHundred() {
		assertEquals(0, FearMath.clamp(-5), EPS);
		assertEquals(42, FearMath.clamp(42), EPS);
		assertEquals(100, FearMath.clamp(250), EPS);
	}

	@Test
	void decayLosesThreePointsPerMinute() {
		assertEquals(27, FearMath.decayed(30, 0, 1200), EPS);
		assertEquals(30, FearMath.decayed(30, 500, 500), EPS);
		assertEquals(0, FearMath.decayed(2, 0, 12_000), EPS);
		// A clock that runs backwards (a reloaded world) never adds fear.
		assertEquals(30, FearMath.decayed(30, 1000, 0), EPS);
	}

	@Test
	void environmentAddsDarknessNightDepthAndHaunting() {
		assertEquals(0, FearMath.environmental(15, false, false, false), EPS);
		assertEquals(40, FearMath.environmental(0, false, false, false), EPS);
		assertEquals(40 + 12 + 10 + 25, FearMath.environmental(0, true, true, true), EPS);
		assertEquals(100, FearMath.clamp(FearMath.environmental(-3, true, true, true) + 50), EPS);
		assertTrue(FearMath.environmental(7, false, false, false) > FearMath.environmental(8, false, false, false));
	}

	@Test
	void spawnWeightIsZeroBelowMinimumAndGrowsAbove() {
		assertEquals(0, FearMath.spawnWeight(10, 14.9, 15), EPS);
		assertEquals(10, FearMath.spawnWeight(10, 15, 15), EPS);
		assertEquals(20, FearMath.spawnWeight(10, 40, 15), EPS);
	}
}
