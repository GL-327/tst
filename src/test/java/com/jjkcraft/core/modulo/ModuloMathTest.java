package com.jjkcraft.core.modulo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Modulo expansion's numbers (docs/MODULO.md, BALANCE.md section 10). */
class ModuloMathTest {
	@Test
	void mulEnergyFollowsTheRatio() {
		assertEquals(10, ModuloMath.mulFromCursedEnergy(20, 0.5), 1e-9);
		assertEquals(0, ModuloMath.mulFromCursedEnergy(-5, 0.5), 1e-9);
		assertEquals(0, ModuloMath.mulFromCursedEnergy(20, -1), 1e-9);
	}

	@Test
	void aPourIsLimitedByRatePieceEnergyAndRoom() {
		assertEquals(5, ModuloMath.pourAmount(5, 40, 100, 0, 200, 0.5), 1e-9, "the pour rate");
		assertEquals(3, ModuloMath.pourAmount(5, 3, 100, 0, 200, 0.5), 1e-9, "what the piece still holds");
		assertEquals(2, ModuloMath.pourAmount(5, 40, 2, 0, 200, 0.5), 1e-9, "the pourer's cursed energy");
		assertEquals(4, ModuloMath.pourAmount(5, 40, 100, 198, 200, 0.5), 1e-9, "room for 2 Mul energy takes 4 cursed energy");
		assertEquals(0, ModuloMath.pourAmount(5, 40, 100, 200, 200, 0.5), 1e-9, "a full pool takes nothing");
	}

	@Test
	void bossHealthGrowsWithFighters() {
		assertEquals(520, ModuloMath.bossHealth(520, 1, 0.6), 1e-9);
		assertEquals(832, ModuloMath.bossHealth(520, 2, 0.6), 1e-9);
		assertEquals(1456, ModuloMath.bossHealth(520, 4, 0.6), 1e-9);
		assertEquals(520, ModuloMath.bossHealth(520, 0, 0.6), 1e-9, "no fighters counts as one");
	}

	@Test
	void healthAboveTheCeilingBecomesDamageScaling() {
		assertEquals(1, ModuloMath.damageScale(520), 1e-9);
		assertEquals(1, ModuloMath.damageScale(1024), 1e-9);
		double scale = ModuloMath.damageScale(1456);
		assertEquals(1456, 1024 / scale, 1e-9, "the capped bar plus the scaling lasts as long as the full health");
	}

	@Test
	void adaptationStartsAtTheFirstHitAndIsCapped() {
		assertEquals(0, ModuloMath.adaptation(0, 0.2, 0.1, 0.95), 1e-9);
		assertEquals(0.2, ModuloMath.adaptation(1, 0.2, 0.1, 0.95), 1e-9);
		assertEquals(0.5, ModuloMath.adaptation(4, 0.2, 0.1, 0.95), 1e-9);
		assertEquals(0.95, ModuloMath.adaptation(50, 0.2, 0.1, 0.95), 1e-9);
		// Mahoraga's data: Light counts as fully adapted (0.9) on the eighth hit.
		int hits = 0;
		while (ModuloMath.adaptation(hits, 0.2, 0.1, 0.95) < 0.9 - 1e-9) {
			hits++;
		}
		assertEquals(8, hits);
	}

	@Test
	void aMediumLosesDurabilityByCostButAlwaysSome() {
		assertEquals(3, ModuloMath.mediumDurability(30, 0.1));
		assertEquals(1, ModuloMath.mediumDurability(0, 0.1), "free casts still wear the tool");
		assertEquals(4, ModuloMath.mediumDurability(31, 0.1), "rounded up");
	}

	@Test
	void toolDurabilityComesFromTheGradeOrRank() {
		assertEquals(288, ModuloMath.toolDurability(288, 3));
		assertEquals(96, ModuloMath.toolDurability(0, 0));
		assertEquals(416, ModuloMath.toolDurability(0, 5));
	}

	@Test
	void theDescentRunsForItsLengthAndRecurs() {
		assertTrue(ModuloMath.descentActive(1000, 200, 1000));
		assertTrue(ModuloMath.descentActive(1000, 200, 1199));
		assertFalse(ModuloMath.descentActive(1000, 200, 1200));
		assertFalse(ModuloMath.descentActive(-1, 200, 0), "never started");
		assertEquals(500, ModuloMath.nextDescent(-1, 500, 3), "the first one is due at once");
		assertEquals(1000 + 3 * 24_000, ModuloMath.nextDescent(1000, 5000, 3));
	}
}
