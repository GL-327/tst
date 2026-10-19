package com.jjkcraft.core.fear;

/** Pure fear formulas. Unit-tested in {@code FearMathTest}. */
public final class FearMath {
	/** Event fear lost per tick (3 points per minute). */
	public static final double DECAY_PER_TICK = 3.0 / 1200.0;

	private FearMath() {
	}

	public static double clamp(double value) {
		return Math.max(0, Math.min(100, value));
	}

	public static double decayed(double value, long since, long now) {
		return Math.max(0, value - Math.max(0, now - since) * DECAY_PER_TICK);
	}

	/**
	 * Fear from the surroundings: darkness up to 40, night 12, below sea level 10, inside an
	 * abandoned or haunted structure 25.
	 */
	public static double environmental(int light, boolean night, boolean deep, boolean haunted) {
		double darkness = 40.0 * (15 - Math.max(0, Math.min(15, light))) / 15.0;
		return clamp(darkness + (night ? 12 : 0) + (deep ? 10 : 0) + (haunted ? 25 : 0));
	}

	/** Spawn weight multiplier: curses spawn more readily the further fear is above their minimum. */
	public static double spawnWeight(int baseWeight, double fear, double minFear) {
		if (fear < minFear) {
			return 0;
		}

		return baseWeight * (1 + (fear - minFear) / 25.0);
	}
}
