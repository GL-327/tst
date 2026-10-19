package com.jjkcraft.core.energy;

/**
 * Tuning for cursed energy regeneration and overexertion. Values come from the server config and
 * are sent to clients so their prediction matches. See docs/BALANCE.md.
 *
 * @param regenFractionPerSecond   share of max CE restored per second while regenerating normally
 * @param idleMultiplier           regen multiplier once the entity has been idle for {@code idleDelayTicks}
 * @param idleDelayTicks           ticks without a CE action before idle regen starts
 * @param overexertThreshold       share of max CE below which a spend triggers overexertion
 * @param overexertTicks           length of the no-regen window after overexertion
 */
public record EnergyParams(
		double regenFractionPerSecond,
		double idleMultiplier,
		int idleDelayTicks,
		double overexertThreshold,
		int overexertTicks
) {
	public static final EnergyParams DEFAULT = new EnergyParams(0.015, 2.5, 100, 0.10, 60);

	public double regenPerTick(double max) {
		return max * regenFractionPerSecond / 20.0;
	}
}
