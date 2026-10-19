package com.jjkcraft.core.domain;

import com.jjkcraft.core.progress.ProgressionMath;

/**
 * Pure domain formulas: costs, how long a pool can hold a domain, and clash resolution.
 * Unit-tested in {@code DomainMathTest}.
 */
public final class DomainMath {
	/** Seconds two overlapping domains push against each other before the clash resolves. */
	public static final int CLASH_TICKS = 40;

	/**
	 * How a domain is cast.
	 *
	 * @param power  multiplier on the domain's effects
	 * @param cost   multiplier on cast cost and upkeep
	 * @param radius multiplier on the radius
	 * @param clash  multiplier on clash power
	 */
	public enum Variant {
		/** A barrier that holds the targets caught inside. */
		FULL(1.0, 1.0, 1.0, 1.0),
		/** No barrier: wider and stronger, much more expensive, and targets can walk out. */
		BARRIERLESS(1.3, 1.6, 1.4, 1.2);

		public final double power;
		public final double cost;
		public final double radius;
		public final double clash;

		Variant(double power, double cost, double radius, double clash) {
			this.power = power;
			this.cost = cost;
			this.radius = radius;
			this.clash = clash;
		}
	}

	/** One side of a clash. {@code openedAt} breaks exact ties in favor of the domain that was there first. */
	public record Clasher(double currentEnergy, int domainPoints, int priority, Variant variant, long openedAt) {
	}

	public enum ClashResult {
		FIRST_WINS, SECOND_WINS
	}

	private DomainMath() {
	}

	/** Domain growth points: -2% cost each, at most -40% (shared with the growth tree). */
	public static double growthCostMultiplier(int domainPoints) {
		return ProgressionMath.domainCostMultiplier(Math.max(0, domainPoints));
	}

	/** Domain growth points: +3% power each (shared with the growth tree). */
	public static double growthPowerMultiplier(int domainPoints) {
		return ProgressionMath.domainPowerMultiplier(Math.max(0, domainPoints));
	}

	public static double castCost(double base, Variant variant, int domainPoints, double configMultiplier) {
		return Math.max(0, base * variant.cost * growthCostMultiplier(domainPoints) * configMultiplier);
	}

	public static double upkeepPerSecond(double base, Variant variant, int domainPoints, double configMultiplier) {
		return Math.max(0, base * variant.cost * growthCostMultiplier(domainPoints) * configMultiplier);
	}

	public static double power(Variant variant, int domainPoints) {
		return variant.power * growthPowerMultiplier(domainPoints);
	}

	/**
	 * Seconds a domain can be held from a full pool: the cast cost comes off first, then upkeep
	 * minus regen drains the rest. Infinite when regen covers the upkeep; 0 when the pool can't
	 * pay the cast cost.
	 */
	public static double sustainSeconds(double pool, double castCost, double upkeepPerSecond, double regenPerSecond) {
		if (pool < castCost) {
			return 0;
		}

		double net = upkeepPerSecond - regenPerSecond;
		if (net <= 0) {
			return Double.POSITIVE_INFINITY;
		}

		return (pool - castCost) / net;
	}

	/** Clash power: the current CE pool, raised by domain growth, priority (+5% each) and the variant. */
	public static double clashPower(Clasher side) {
		return Math.max(0, side.currentEnergy()) * growthPowerMultiplier(side.domainPoints())
				* (1.0 + 0.05 * Math.max(0, side.priority())) * side.variant().clash;
	}

	/** The larger pool wins. Exact ties go to the higher priority, then to the domain opened first. */
	public static ClashResult resolve(Clasher first, Clasher second) {
		int byPower = Double.compare(clashPower(first), clashPower(second));
		if (byPower != 0) {
			return byPower > 0 ? ClashResult.FIRST_WINS : ClashResult.SECOND_WINS;
		}

		if (first.priority() != second.priority()) {
			return first.priority() > second.priority() ? ClashResult.FIRST_WINS : ClashResult.SECOND_WINS;
		}

		return first.openedAt() <= second.openedAt() ? ClashResult.FIRST_WINS : ClashResult.SECOND_WINS;
	}

	/** Two spheres overlap when their centers are closer than the sum of their radii. */
	public static boolean overlap(double distance, double radiusA, double radiusB) {
		return distance < radiusA + radiusB;
	}

	/** CE the loser of a clash loses. */
	public static double backlashEnergy(double maxEnergy, double percent) {
		return Math.max(0, maxEnergy) * Math.max(0, Math.min(100, percent)) / 100.0;
	}
}
