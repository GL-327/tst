package com.jjkcraft.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CullingMathTest {
	@Test
	void daysCountFromTheStart() {
		assertEquals(0, CullingMath.day(1000, 1000));
		assertEquals(0, CullingMath.day(1000 + 23999, 1000));
		assertEquals(1, CullingMath.day(1000 + 24000, 1000));
		assertEquals(0, CullingMath.day(0, 5000), "before the start is day 0");
	}

	@Test
	void entryWindowCountsDownInWholeDays() {
		long deadline = CullingMath.deadline(0, 19);
		assertEquals(19 * 24000L, deadline);
		assertEquals(19, CullingMath.daysLeft(0, deadline));
		assertEquals(1, CullingMath.daysLeft(deadline - 1, deadline), "the last tick still shows a day");
		assertEquals(0, CullingMath.daysLeft(deadline, deadline));
	}

	@Test
	void curseValuesRiseWithGrade() {
		assertEquals(1, CullingMath.curseValue(0, false), "Grade 4");
		assertEquals(3, CullingMath.curseValue(5, false), "Grade 1");
		assertEquals(10, CullingMath.curseValue(6, true), "Special Grade");
	}

	@Test
	void transfersMoveWholeStepsOfWhatIsHeld() {
		assertEquals(10, CullingMath.transferable(12, 5, 40));
		assertEquals(15, CullingMath.transferable(100, 5, 17));
		assertEquals(0, CullingMath.transferable(4, 5, 40));
		assertEquals(0, CullingMath.transferable(-10, 5, 40));
	}

	@Test
	void deathCostsAShareOfPoints() {
		assertEquals(5, CullingMath.afterDeath(11, 50));
		assertEquals(11, CullingMath.afterDeath(11, 0));
		assertEquals(0, CullingMath.afterDeath(11, 150));
	}

	@Test
	void theGameEndsByDaysOrByWhoIsLeft() {
		assertTrue(CullingMath.shouldEnd(60, 60, 10, 10, 1));
		assertFalse(CullingMath.shouldEnd(59, 60, 10, 10, 1));
		assertTrue(CullingMath.shouldEnd(3, 60, 1, 5, 1), "one left standing");
		assertFalse(CullingMath.shouldEnd(3, 60, 1, 1, 1), "a lone first entrant does not end it");
		assertFalse(CullingMath.shouldEnd(500, 0, 1, 5, 0), "both conditions off");
	}

	@Test
	void theBarrierIsAWallUnderADome() {
		assertTrue(CullingMath.inside(0, 80, 0, 64), "under the top of the dome");
		assertFalse(CullingMath.inside(0, 90, 0, 64), "above it");
		assertTrue(CullingMath.inside(63, 20, 0, 64), "near the edge, inside the wall");
		assertFalse(CullingMath.inside(65, 0, 0, 64), "past the radius");
		assertTrue(CullingMath.inside(10, -40, 10, 64), "underground counts as inside");
		assertEquals(CullingMath.WALL + 64, CullingMath.roofAt(0, 64), 1e-9);
	}

	@Test
	void finalStandingsPayThePodium() {
		assertEquals(2000 + 400, CullingMath.finalXp(20, 1));
		assertEquals(400, CullingMath.finalXp(20, 4));
		assertEquals(4, CullingMath.ring(4, 300, 0).length);
		assertEquals(300, Math.hypot(CullingMath.ring(4, 300, 0.3)[2][0], CullingMath.ring(4, 300, 0.3)[2][1]), 1e-9);
	}
}
