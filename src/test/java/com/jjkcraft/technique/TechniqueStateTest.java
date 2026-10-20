package com.jjkcraft.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.DeathPolicy;

class TechniqueStateTest {
	private static final Identifier PALM = JjkCraft.id("ink_iron_palm");
	private static final Identifier BOLT = JjkCraft.id("grudge_bolt");
	private static final Identifier CHAINS = JjkCraft.id("grave_chains");

	@Test
	void slotsAreUniqueAndSized() {
		TechniqueState state = TechniqueState.EMPTY.withInnate(PALM).withLearned(BOLT)
				.withSlot(0, Optional.of(PALM)).withSlot(3, Optional.of(PALM));
		assertEquals(TechniqueState.SLOTS, state.loadout().size());
		assertEquals(Optional.empty(), state.slot(0), "moving a technique empties its old slot");
		assertEquals(Optional.of(PALM), state.slot(3));
	}

	@Test
	void oldSavesExpandToTwelveWithoutLosingBindings() {
		TechniqueState state = TechniqueState.normalized(Optional.of(PALM), List.of(BOLT, CHAINS),
				List.of(Optional.of(PALM), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(BOLT), Optional.of(CHAINS)),
				5, Map.of(), Map.of());
		assertEquals(12, state.loadout().size());
		assertEquals(3, state.known().size(), "old learned moves remain available for loadout changes");
		assertTrue(state.selected() < 12);
		assertEquals(Optional.of(BOLT),state.slot(4));
        assertEquals(Optional.of(CHAINS),state.slot(5));
	}

	@Test
	void autoSlotFillsOnlyUsableSlots() {
		TechniqueState state = TechniqueState.EMPTY.withInnate(PALM).withLearned(BOLT).withLearned(CHAINS).withAutoSlotted(2);
		assertEquals(Optional.of(PALM), state.slot(0));
		assertEquals(Optional.of(BOLT), state.slot(1));
		assertEquals(Optional.empty(), state.slot(2));
	}

	@Test
	void learningTheInnateTwiceDoesNothing() {
		TechniqueState state = TechniqueState.EMPTY.withInnate(PALM).withLearned(PALM);
		assertEquals(List.of(), state.learned());
		assertEquals(1, state.known().size());
	}

	@Test
	void revokeClearsSlotsButNotTheInnate() {
		TechniqueState state = TechniqueState.EMPTY.withInnate(PALM).withLearned(BOLT).withAutoSlotted(6).withoutLearned(BOLT);
		assertFalse(state.loadout().contains(Optional.of(BOLT)));
		assertTrue(state.loadout().contains(Optional.of(PALM)));
	}

	@Test
	void cooldownMapDropsExpiredEntries() {
		TechniqueState state = TechniqueState.EMPTY.withCooldown(PALM, 10, 0).withCooldown(BOLT, 50, 20);
		assertEquals(Map.of(BOLT, 50L), state.cooldowns());
	}

	@Test
	void deathPolicies() {
		TechniqueState state = TechniqueState.EMPTY.withInnate(PALM).withLearned(BOLT).withAutoSlotted(6)
				.withMasteryAdded(PALM, 40).withMasteryAdded(BOLT, 10).withCooldown(PALM, 999, 0);

		TechniqueState kept = TechniqueService.applyPolicy(state, DeathPolicy.KEEP, 25);
		assertEquals(state.known(), kept.known());
		assertEquals(Map.of(), kept.cooldowns(), "cooldowns reset on death");

		TechniqueState partial = TechniqueService.applyPolicy(state, DeathPolicy.PARTIAL, 25);
		assertEquals(30, partial.masteryOf(PALM));

		TechniqueState reset = TechniqueService.applyPolicy(state, DeathPolicy.RESET_TECHNIQUES, 25);
		assertEquals(List.of(), reset.learned());
		assertEquals(0, reset.masteryOf(PALM));
		assertEquals(Optional.of(PALM), reset.innate());
		assertFalse(reset.loadout().contains(Optional.of(BOLT)));
	}
}
