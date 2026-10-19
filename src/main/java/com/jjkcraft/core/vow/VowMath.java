package com.jjkcraft.core.vow;

import java.util.Locale;

/**
 * Pure Binding Vow formulas. Unit-tested in {@code VowMathTest}.
 *
 * <p>A vow is one to three restrictions, a focus (what the power goes into) and a term. Its
 * points grow with the restrictions' weight (with diminishing returns) and the term's length.
 * Breaking a vow costs CE, stuns, and leaves a debt of slower regen; a fulfilled vow costs nothing.
 */
public final class VowMath {
	public static final int MAX_RESTRICTIONS = 3;

	/** What the vow's power feeds. */
	public enum Focus {
		/** More damage from techniques and blows. */
		POWER,
		/** Cheaper techniques. */
		EFFICIENCY,
		/** A larger, faster-refilling pool. */
		RESERVE;

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * How long the vow holds.
	 *
	 * @param ticks  length, or -1 until renounced
	 * @param factor multiplier on points
	 */
	public enum Term {
		SKIRMISH(6000, 0.8),
		DAY(24000, 1.0),
		BINDING(-1, 1.25);

		public final int ticks;
		public final double factor;

		Term(int ticks, double factor) {
			this.ticks = ticks;
			this.factor = factor;
		}

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private VowMath() {
	}

	public static double points(double totalWeight, Term term) {
		return totalWeight <= 0 ? 0 : Math.pow(totalWeight, 0.85) * term.factor;
	}

	public static double damageMultiplier(Focus focus, double points) {
		return focus == Focus.POWER ? 1 + 0.06 * points : 1;
	}

	/** Melee (non-technique) damage gets a smaller share of a power vow. */
	public static double meleeMultiplier(Focus focus, double points) {
		return focus == Focus.POWER ? 1 + 0.04 * points : 1;
	}

	public static double costMultiplier(Focus focus, double points) {
		return focus == Focus.EFFICIENCY ? Math.max(0.6, 1 - 0.05 * points) : 1;
	}

	public static double capacityMultiplier(Focus focus, double points) {
		return focus == Focus.RESERVE ? 1 + 0.05 * points : 1;
	}

	public static double regenMultiplier(Focus focus, double points) {
		return focus == Focus.RESERVE ? 1 + 0.04 * points : 1;
	}

	/** Share of max CE lost when the vow breaks (halved when renounced early). */
	public static double breakDrain(double points, boolean renounced) {
		double drain = Math.min(0.5, 0.06 * points);
		return renounced ? drain / 2 : drain;
	}

	/** Ticks of vow debt (halved regen) after breaking: one minute per point. */
	public static int debtTicks(double points, boolean renounced) {
		int ticks = (int) Math.round(1200 * points);
		return renounced ? ticks / 2 : ticks;
	}

	public static final double DEBT_REGEN = 0.5;
}
