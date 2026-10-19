package com.jjkcraft.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyMathTest {
	private static final EnergyParams P = EnergyParams.DEFAULT;
	private static final double MAX = 100;
	private static final double EPS = 1e-6;

	@Test
	void regenAtNormalRate() {
		// Last action at 0, idle starts at 100 ticks; 20 ticks of normal regen = 1 s * 1.5% of 100.
		EnergyState s = new EnergyState(50, 0, 0, 0);
		EnergyState after = EnergyMath.advance(s, MAX, 20, P, 1.0);
		assertEquals(51.5, after.current(), EPS);
		assertEquals(20, after.lastUpdateTick());
	}

	@Test
	void regenSwitchesToIdleRate() {
		EnergyState s = new EnergyState(10, 0, 0, 0);
		// 100 ticks normal (5 s * 1.5) + 20 ticks idle (1 s * 1.5 * 2.5).
		EnergyState after = EnergyMath.advance(s, MAX, 120, P, 1.0);
		assertEquals(10 + 7.5 + 3.75, after.current(), EPS);
	}

	@Test
	void regenCapsAtMax() {
		EnergyState s = new EnergyState(99.9, 0, 0, 0);
		assertEquals(MAX, EnergyMath.advance(s, MAX, 10_000, P, 1.0).current(), EPS);
	}

	@Test
	void closedFormMatchesTickByTick() {
		EnergyState start = new EnergyState(3, 0, 40, 10);
		EnergyState stepped = start;
		for (long t = 1; t <= 400; t++) {
			stepped = EnergyMath.advance(stepped, MAX, t, P, 1.3);
		}

		EnergyState direct = EnergyMath.advance(start, MAX, 400, P, 1.3);
		assertEquals(direct.current(), stepped.current(), 1e-9);
	}

	@Test
	void overexertionBlocksRegen() {
		EnergyState s = new EnergyState(5, 0, 60, 0);
		assertEquals(5, EnergyMath.advance(s, MAX, 60, P, 1.0).current(), EPS);
		assertTrue(EnergyMath.advance(s, MAX, 61, P, 1.0).current() > 5);
	}

	@Test
	void zeroModifierDisablesRegen() {
		EnergyState s = new EnergyState(5, 0, 0, 0);
		assertEquals(5, EnergyMath.advance(s, MAX, 1000, P, 0.0).current(), EPS);
	}

	@Test
	void spendDeductsAndMarksAction() {
		EnergyMath.SpendResult r = EnergyMath.spend(new EnergyState(50, 0, 0, 0), MAX, 30, 0, P, 1.0);
		assertTrue(r.success());
		assertFalse(r.triggeredOverexertion());
		assertEquals(20, r.state().current(), EPS);
		assertEquals(0, r.state().lastActionTick());
	}

	@Test
	void spendFailsWithoutEnoughEnergy() {
		EnergyMath.SpendResult r = EnergyMath.spend(new EnergyState(20, 0, 0, 0), MAX, 30, 0, P, 1.0);
		assertFalse(r.success());
		assertEquals(20, r.state().current(), EPS);
	}

	@Test
	void crossingThresholdTriggersOverexertionOnce() {
		EnergyMath.SpendResult first = EnergyMath.spend(new EnergyState(15, 0, 0, 0), MAX, 8, 0, P, 1.0);
		assertTrue(first.triggeredOverexertion());
		assertEquals(P.overexertTicks(), first.state().overexertedUntil());

		// Already below the threshold: spending more does not restart the window.
		EnergyMath.SpendResult second = EnergyMath.spend(first.state(), MAX, 2, 10, P, 1.0);
		assertTrue(second.success());
		assertFalse(second.triggeredOverexertion());
		assertEquals(P.overexertTicks(), second.state().overexertedUntil());
	}

	@Test
	void drainNeverGoesNegativeAndCountsAsAction() {
		EnergyState after = EnergyMath.drain(new EnergyState(10, 0, 0, 0), MAX, 25, 50, P, 1.0);
		assertTrue(after.current() >= 0);
		assertEquals(50, after.lastActionTick());
	}

	@Test
	void restoreCapsAtMax() {
		assertEquals(MAX, EnergyMath.restore(new EnergyState(90, 0, 0, 0), MAX, 50, 0, P, 1.0).current(), EPS);
	}

	@Test
	void sorcererCapacity() {
		assertEquals(100, EnergyMath.sorcererCapacity(100, 1.0, 0, false), EPS);
		assertEquals(360 * 1.3, EnergyMath.sorcererCapacity(100, 3.6, 10, false), EPS);
		assertEquals(0, EnergyMath.sorcererCapacity(100, 5.0, 50, true), EPS);
	}

	@Test
	void secondsToReachMatchesAdvance() {
		EnergyState s = new EnergyState(0, 0, 20, 0);
		double seconds = EnergyMath.secondsToReach(s, MAX, 50, 0, P, 1.0);
		long ticks = (long) Math.ceil(seconds * 20);
		assertTrue(EnergyMath.advance(s, MAX, ticks, P, 1.0).current() >= 50 - 1e-6);
		assertTrue(EnergyMath.advance(s, MAX, ticks - 2, P, 1.0).current() < 50);
	}
}
