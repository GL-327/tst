package com.jjkcraft.core.technique;

import com.jjkcraft.core.progress.ProgressionMath;

/**
 * Pure technique formulas: power, cost, cooldown and per-behavior damage scaling. No Minecraft
 * types; unit-tested in {@code TechniqueMathTest}, which also checks max damage against curse HP.
 */
public final class TechniqueMath {
    public static final double DEFAULT_DAMAGE_MULTIPLIER=2.5;
    public static double damage(double amount,double multiplier){return Math.max(0,amount)*(Double.isFinite(multiplier)?Math.clamp(multiplier,.1,10):DEFAULT_DAMAGE_MULTIPLIER);}
	/** Damage bonus per grade rank above Grade 4. */
	public static final double RANK_POWER = 0.08;
	/** Mastered techniques deal this much more damage. */
	public static final double MASTERY_POWER = 1.10;
	/** Mastered techniques cost this much less. */
	public static final double MASTERY_COST = 0.90;
	/** Upper bound on a target's CE ratio for scaling techniques. */
	public static final double MAX_CE_RATIO = 3.0;

	private TechniqueMath() {
	}

	/** Damage multiplier for a caster. */
	public static double power(int rank, int outputPoints, boolean mastered, double vowMultiplier) {
		return (1 + RANK_POWER * Math.max(0, rank)) * ProgressionMath.damageMultiplier(outputPoints)
				* (mastered ? MASTERY_POWER : 1.0) * vowMultiplier;
	}

	/** CE cost after Control points, mastery and vows. */
	public static double cost(double base, int controlPoints, boolean mastered, double vowMultiplier) {
		return base * ProgressionMath.costMultiplier(controlPoints) * (mastered ? MASTERY_COST : 1.0) * vowMultiplier;
	}

	/** Cooldown ticks after Efficiency points. Never below 1 for techniques that have one. */
	public static int cooldown(int base, int efficiencyPoints) {
		if (base <= 0) {
			return 0;
		}

		return Math.max(1, (int) Math.round(base * ProgressionMath.cooldownMultiplier(efficiencyPoints)));
	}

	/**
	 * Arc slash: damage grows with the target's current CE. A target with no CE takes base damage;
	 * each 100 CE adds {@code scaling} times base, up to {@link #MAX_CE_RATIO}.
	 */
	public static double ceScaledDamage(double base, double scaling, double targetCe) {
		double ratio = Math.min(MAX_CE_RATIO, Math.max(0, targetCe) / 100.0);
		return base * (1 + scaling * ratio);
	}

	/** Splits damage into an armored part and an armor-ignoring part. */
	public static double[] armorSplit(double damage, double pierceFraction) {
		double p = Math.clamp(pierceFraction, 0, 1);
		return new double[] {damage * (1 - p), damage * p};
	}

	/** Linear falloff from full at the center to {@code edge} at the radius. */
	public static double falloff(double distance, double radius, double edge) {
		if (radius <= 0) {
			return 1;
		}

		double t = Math.clamp(distance / radius, 0, 1);
		return 1 - (1 - edge) * t;
	}

	/** CE converted to health by healing techniques: {@code efficiency} health per CE, scaled by power. */
	public static double healing(double ceSpent, double efficiency, double power) {
		return Math.max(0, ceSpent) * efficiency * power;
	}
}
