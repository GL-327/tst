package com.jjkcraft.domain.behavior;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;

/**
 * One piece of what a domain does. Behaviors are immutable data; per-domain state goes in
 * {@link ActiveDomain#memory()}. Hooks run on the server; {@code tick} runs only while the domain
 * is active (not while opening, clashing or closing).
 */
public interface DomainBehavior {
	DomainBehaviorType<?> type();

	/** When the opening sequence ends and the domain takes hold. */
	default void open(ActiveDomain domain) {
	}

	default void tick(ActiveDomain domain) {
	}

	/** When the domain ends for any reason. Undo anything {@code open} or {@code tick} left behind. */
	default void close(ActiveDomain domain) {
	}

	/** Multiplier on the CE regen of {@code entity} while it is inside as a target. */
	default double regenMultiplier(ActiveDomain domain, LivingEntity entity) {
		return 1.0;
	}
}
