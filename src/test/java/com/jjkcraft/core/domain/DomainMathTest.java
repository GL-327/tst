package com.jjkcraft.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jjkcraft.core.domain.DomainMath.Clasher;
import com.jjkcraft.core.domain.DomainMath.ClashResult;
import com.jjkcraft.core.domain.DomainMath.Variant;

class DomainMathTest {
	private static final double EPS = 1e-9;

	@Test
	void barrierlessCostsMoreAndHitsHarder() {
		assertEquals(200, DomainMath.castCost(200, Variant.FULL, 0, 1), EPS);
		assertEquals(320, DomainMath.castCost(200, Variant.BARRIERLESS, 0, 1), EPS);
		assertTrue(DomainMath.power(Variant.BARRIERLESS, 0) > DomainMath.power(Variant.FULL, 0));
	}

	@Test
	void growthLowersCostWithAFloor() {
		assertEquals(180, DomainMath.upkeepPerSecond(20, Variant.FULL, 5, 10), EPS);
		// 20 points is the cap: -40%.
		assertEquals(DomainMath.castCost(100, Variant.FULL, 20, 1), DomainMath.castCost(100, Variant.FULL, 50, 1), EPS);
		assertEquals(60, DomainMath.castCost(100, Variant.FULL, 20, 1), EPS);
	}

	/** Domain cost against pool size, for the data's domains at the grades that can cast them (see BALANCE.md). */
	@Test
	void poolsHoldADomainForAReasonableTime() {
		// Grade 1: 360 CE, 7.02 CE/s combat regen. A Grade 1 domain: 140 to cast, 20 per second.
		double seconds = DomainMath.sustainSeconds(360, 140, 20, 7.02);
		assertTrue(seconds > 12 && seconds < 30, "Grade 1 holds a domain 12 to 30 seconds, got " + seconds);
		// Special Grade: 500 CE, 10.88 CE/s. A Special Grade domain: 220 to cast, 28 per second.
		double special = DomainMath.sustainSeconds(500, 220, 28, 10.88);
		assertTrue(special > 12 && special < 30, "Special Grade holds its domain 12 to 30 seconds, got " + special);
		// Barrierless costs 1.6x: a Grade 1 can barely open one and holds it only a few seconds.
		double open = DomainMath.sustainSeconds(360, DomainMath.castCost(140, Variant.BARRIERLESS, 0, 1),
				DomainMath.upkeepPerSecond(20, Variant.BARRIERLESS, 0, 1), 7.02);
		assertTrue(open > 0 && open < 8, "barrierless at Grade 1 is short, got " + open);
		assertEquals(0, DomainMath.sustainSeconds(100, 140, 20, 1), EPS);
		assertEquals(Double.POSITIVE_INFINITY, DomainMath.sustainSeconds(300, 120, 2, 3));
	}

	@Test
	void largerPoolWinsTheClash() {
		Clasher strong = new Clasher(300, 0, 0, Variant.FULL, 100);
		Clasher weak = new Clasher(200, 0, 0, Variant.FULL, 50);
		assertEquals(ClashResult.FIRST_WINS, DomainMath.resolve(strong, weak));
		assertEquals(ClashResult.SECOND_WINS, DomainMath.resolve(weak, strong));
	}

	@Test
	void growthPriorityAndVariantTipTheBalance() {
		Clasher trained = new Clasher(200, 10, 0, Variant.FULL, 0);
		Clasher plain = new Clasher(250, 0, 0, Variant.FULL, 0);
		// +30% from growth beats a 25% bigger pool.
		assertEquals(ClashResult.FIRST_WINS, DomainMath.resolve(trained, plain));
		Clasher open = new Clasher(200, 0, 0, Variant.BARRIERLESS, 0);
		Clasher closed = new Clasher(230, 0, 0, Variant.FULL, 0);
		assertEquals(ClashResult.FIRST_WINS, DomainMath.resolve(open, closed));
	}

	@Test
	void tiesGoToPriorityThenToTheFirstDomain() {
		Clasher a = new Clasher(200, 0, 2, Variant.FULL, 10);
		Clasher b = new Clasher(200 * 1.10 / 1.0, 0, 0, Variant.FULL, 5);
		// Same clash power (200 * 1.10 against 220): priority decides.
		assertEquals(DomainMath.clashPower(a), DomainMath.clashPower(b), 1e-6);
		assertEquals(ClashResult.FIRST_WINS, DomainMath.resolve(a, b));

		Clasher early = new Clasher(150, 0, 1, Variant.FULL, 5);
		Clasher late = new Clasher(150, 0, 1, Variant.FULL, 9);
		assertEquals(ClashResult.FIRST_WINS, DomainMath.resolve(early, late));
		assertEquals(ClashResult.SECOND_WINS, DomainMath.resolve(late, early));
	}

	@Test
	void overlapAndBacklash() {
		assertTrue(DomainMath.overlap(20, 12, 10));
		assertFalse(DomainMath.overlap(22, 12, 10));
		assertEquals(90, DomainMath.backlashEnergy(300, 30), EPS);
		assertEquals(300, DomainMath.backlashEnergy(300, 250), EPS);
	}
}
