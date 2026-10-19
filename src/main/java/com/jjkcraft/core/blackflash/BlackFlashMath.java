package com.jjkcraft.core.blackflash;

/**
 * Pure Black Flash formulas. Unit-tested in {@code BlackFlashMathTest}.
 *
 * <p>A Black Flash is a strike where cursed energy lands within a hair of the blow. Its chance is a
 * base rate plus grade and Control bonuses, raised further while "in the zone" after a recent
 * Black Flash, and scaled by timing: a fully charged swing aligns best, a spammed one rarely.
 */
public final class BlackFlashMath {
	/** Hard cap on the chance, in percent. */
	public static final double MAX_CHANCE = 35.0;
	/** How long the zone lasts after a Black Flash. */
	public static final int ZONE_TICKS = 600;
	/** Chance added per zone stack, in percentage points. */
	public static final double ZONE_BONUS = 8.0;
	public static final int MAX_ZONE_STACKS = 2;
	/** Fraction of max CE given back by a Black Flash. */
	public static final double REFUND_FRACTION = 0.15;

	private BlackFlashMath() {
	}

	/** Timing factor from how charged the physical swing is (0 to 1; mobs count as 1). */
	public static double timing(double swingCharge) {
		if (swingCharge >= 0.9) {
			return 1.5;
		}

		return swingCharge >= 0.5 ? 1.0 : 0.5;
	}

	/**
	 * Chance in percent.
	 *
	 * @param basePercent  config base chance
	 * @param gradeBonus   the grade's {@code black_flash_bonus} perk
	 * @param controlBonus bonus from Control growth points
	 * @param zoneStacks   recent Black Flashes still in the zone
	 * @param swingCharge  0 to 1
	 */
	public static double chance(double basePercent, double gradeBonus, double controlBonus, int zoneStacks, double swingCharge) {
		double raw = Math.max(0, basePercent) + Math.max(0, gradeBonus) + Math.max(0, controlBonus)
				+ ZONE_BONUS * Math.max(0, Math.min(MAX_ZONE_STACKS, zoneStacks));
		return Math.max(0, Math.min(MAX_CHANCE, raw * timing(swingCharge)));
	}

	/** 1.3 with an empty pool to 1.7 with a full one: reserves make the flash hit harder. */
	public static double ceRatio(double current, double max) {
		double fraction = max <= 0 ? 0 : Math.max(0, Math.min(1, current / max));
		return 1.3 + 0.4 * fraction;
	}

	/** Damage multiplier: {@code ce_ratio ^ exponent}. With the default exponent 2.5: about 1.9x to 3.8x. */
	public static double multiplier(double ceRatio, double exponent) {
		return Math.pow(Math.max(1, ceRatio), Math.max(1, exponent));
	}
}
