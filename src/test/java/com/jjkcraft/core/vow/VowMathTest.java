package com.jjkcraft.core.vow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jjkcraft.core.vow.VowMath.Focus;
import com.jjkcraft.core.vow.VowMath.Term;

class VowMathTest {
	private static final double EPS = 1e-9;

	@Test
	void heavierAndLongerVowsGiveMorePointsWithDiminishingReturns() {
		double light = VowMath.points(3, Term.DAY);
		double heavy = VowMath.points(9, Term.DAY);
		assertTrue(heavy > light && heavy < 3 * light, "diminishing returns: " + light + " -> " + heavy);
		assertTrue(VowMath.points(5, Term.BINDING) > VowMath.points(5, Term.DAY));
		assertTrue(VowMath.points(5, Term.DAY) > VowMath.points(5, Term.SKIRMISH));
		assertEquals(0, VowMath.points(0, Term.BINDING), EPS);
	}

	@Test
	void focusDecidesWhereThePowerGoes() {
		double p = 5;
		assertEquals(1.3, VowMath.damageMultiplier(Focus.POWER, p), EPS);
		assertEquals(1.0, VowMath.damageMultiplier(Focus.EFFICIENCY, p), EPS);
		assertEquals(0.75, VowMath.costMultiplier(Focus.EFFICIENCY, p), EPS);
		assertEquals(1.25, VowMath.capacityMultiplier(Focus.RESERVE, p), EPS);
		assertEquals(1.0, VowMath.capacityMultiplier(Focus.POWER, p), EPS);
	}

	@Test
	void costReductionHasAFloor() {
		assertEquals(0.6, VowMath.costMultiplier(Focus.EFFICIENCY, 50), EPS);
	}

	@Test
	void breakingCostsMoreThanRenouncing() {
		double p = 6;
		assertEquals(0.36, VowMath.breakDrain(p, false), EPS);
		assertEquals(0.18, VowMath.breakDrain(p, true), EPS);
		assertEquals(0.5, VowMath.breakDrain(40, false), EPS);
		assertEquals(7200, VowMath.debtTicks(p, false));
		assertEquals(3600, VowMath.debtTicks(p, true));
	}

	/** The strongest vow (three heaviest restrictions, binding) stays a bonus, not a multiplier explosion. */
	@Test
	void strongestVowStaysBounded() {
		double max = VowMath.points(5 + 4 + 4, Term.BINDING);
		assertTrue(VowMath.damageMultiplier(Focus.POWER, max) < 1.75, "max damage bonus under 75%, got " + VowMath.damageMultiplier(Focus.POWER, max));
	}
}
