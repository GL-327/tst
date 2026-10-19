package com.jjkcraft.core.modulo;

/**
 * The Modulo expansion's numbers, free of Minecraft types so they can be unit-tested
 * (docs/MODULO.md, docs/BALANCE.md section 10).
 */
public final class ModuloMath {
	public static final long DAY_TICKS = 24_000L;

	private ModuloMath() {
	}

	/** Mul energy made from {@code cursedEnergy} poured into Mul at {@code ratio}. */
	public static double mulFromCursedEnergy(double cursedEnergy, double ratio) {
		return Math.max(0, cursedEnergy) * Math.max(0, ratio);
	}

	/**
	 * How much cursed energy one pour may take: limited by the pour rate, what the piece of Mul
	 * still holds, what the pourer has, and the room left in their Mul energy pool.
	 */
	public static double pourAmount(double rate, double pieceLeft, double cursedEnergy, double mulEnergy, double mulMax, double ratio) {
		double room = ratio <= 0 ? 0 : Math.max(0, mulMax - mulEnergy) / ratio;
		return Math.max(0, Math.min(Math.min(rate, pieceLeft), Math.min(cursedEnergy, room)));
	}

	/** A boss's health for {@code players} fighters: each one past the first adds {@code perExtraPlayer} of the base. */
	public static double bossHealth(double base, int players, double perExtraPlayer) {
		int extra = Math.max(0, players - 1);
		return base * (1 + extra * Math.max(0, perExtraPlayer));
	}

	/** The highest max health vanilla allows (the ceiling of the {@code max_health} attribute). */
	public static final double HEALTH_CEILING = 1024;

	/**
	 * Damage taken is multiplied by this, so a boss whose health is above the ceiling keeps the
	 * ceiling as its max health and still lasts as long as its full health would.
	 */
	public static double damageScale(double health) {
		return health <= HEALTH_CEILING ? 1 : HEALTH_CEILING / health;
	}

	/**
	 * Mahoraga's resistance to a kind of damage after {@code hits} hits of it: none before the
	 * first, then {@code first}, growing by {@code perHit} with each further hit, up to {@code max}.
	 */
	public static double adaptation(int hits, double first, double perHit, double max) {
		if (hits <= 0) {
			return 0;
		}

		return Math.min(max, first + perHit * (hits - 1));
	}

	/** Durability a medium tool loses for a cast that would have cost {@code cursedEnergy}. Always at least 1. */
	public static int mediumDurability(double cursedEnergy, double perCursedEnergy) {
		return Math.max(1, (int) Math.ceil(Math.max(0, cursedEnergy) * Math.max(0, perCursedEnergy) - 1e-9));
	}

	/** A tool's maximum durability: its grade's value, or a fallback by rank when the grade gives none. */
	public static int toolDurability(int gradeValue, int rank) {
		if (gradeValue > 0) {
			return gradeValue;
		}

		return 96 + 64 * Math.max(0, rank);
	}

	/** Whether a Descent that began at {@code start} and lasts {@code length} ticks is still on at {@code now}. */
	public static boolean descentActive(long start, long length, long now) {
		return start >= 0 && now >= start && now < start + length;
	}

	/** The game time of the next Descent after one at {@code last} (or none yet, {@code last < 0}). */
	public static long nextDescent(long last, long now, int intervalDays) {
		long interval = Math.max(1, intervalDays) * DAY_TICKS;
		if (last < 0) {
			return now;
		}

		return last + interval;
	}
}
