package com.jjkcraft.core.technique;

import com.jjkcraft.core.progress.ProgressionMath;

/**
 * Pure technique formulas: power, cost, cooldown and per-behavior damage scaling. No Minecraft
 * types; unit-tested in {@code TechniqueMathTest}, which also checks max damage against curse HP.
 */
public final class TechniqueMath {
    public static final double DEFAULT_DAMAGE_MULTIPLIER=3.0;
    public static double damage(double amount,double multiplier){return Math.max(0,amount)*(Double.isFinite(multiplier)?Math.clamp(multiplier,.1,10):DEFAULT_DAMAGE_MULTIPLIER);}
	/** Damage bonus per grade rank above Grade 4. */
	public static final double RANK_POWER = 0.12;
	/** Mastered techniques deal this much more damage. */
	public static final double MASTERY_POWER = 1.80;
	/** Mastered techniques cost this much less. */
	public static final double MASTERY_COST = 0.80;
	/** Upper bound on a target's CE ratio for scaling techniques. */
	public static final double MAX_CE_RATIO = 3.0;

	private TechniqueMath() {
	}

    /** Landed uses continue to matter after the first mastery milestone. Four milestones cap it. */
    public static double mastery(int uses, int milestone) {
        return Math.clamp(Math.max(0, uses) / (double)Math.max(1, milestone), 0, 4);
    }
    public static double masteryPower(double mastery) {
        double m=Math.clamp(mastery,0,4);
        return m<=1 ? 1+.8*m : m<=2 ? 1.8+1.2*(m-1) : 3+1.65*(m-2);
    }
    public static double masteryCost(double mastery) {return 1-.20*Math.min(1,Math.max(0,mastery))-.30*Math.clamp((mastery-1)/3,0,1);}
    public static double masteryRecovery(double mastery) {return 1-.20*Math.min(1,Math.max(0,mastery))-.30*Math.clamp((mastery-1)/3,0,1);}
    public static double masteryReach(double mastery) {return 1+.15*Math.clamp(mastery,0,4);}
    public static double masteryDuration(double mastery) {return 1+.25*Math.clamp(mastery,0,4);}
    public static double gradePower(int rank){int r=Math.clamp(rank,0,7);return 1.2*(1+RANK_POWER*r+.018*r*r);}
    public static double power(int rank,int outputPoints,double mastery,double vowMultiplier) {
        return gradePower(rank)*ProgressionMath.damageMultiplier(outputPoints)*masteryPower(mastery)*vowMultiplier;
    }
    public static double cost(double base,int controlPoints,double mastery,double vowMultiplier) {
        return base*ProgressionMath.costMultiplier(controlPoints)*masteryCost(mastery)*vowMultiplier;
    }
    public static int cooldown(int base,int efficiencyPoints,double mastery) {
        return base<=0?0:Math.max(1,(int)Math.round(base*ProgressionMath.cooldownMultiplier(efficiencyPoints)*masteryRecovery(mastery)));
    }

	/** Damage multiplier for a caster. */
	public static double power(int rank, int outputPoints, boolean mastered, double vowMultiplier) {
		return gradePower(rank) * ProgressionMath.damageMultiplier(outputPoints)
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
