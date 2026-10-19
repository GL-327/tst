package com.jjkcraft.core.culling;

/**
 * Pure Culling Game arithmetic: days, the entry window, kill values, the colony shape and the end
 * condition. No Minecraft types, so it is unit-tested directly.
 */
public final class CullingMath {
	/** One in-game day in ticks. */
	public static final long DAY = 24000;
	/** Height of the barrier's wall above the colony centre before the dome begins. */
	public static final double WALL = 24;

	private CullingMath() {
	}

	/** Whole game days since the start: 0 on the first day. */
	public static int day(long now, long startedAt) {
		return (int) Math.max(0, (now - startedAt) / DAY);
	}

	/** The tick the entry window closes for someone whose window opened at {@code from}. */
	public static long deadline(long from, int windowDays) {
		return from + Math.max(0, windowDays) * DAY;
	}

	/** Days left before {@code deadline}, rounded up; 0 once it has passed. */
	public static int daysLeft(long now, long deadline) {
		long left = deadline - now;
		return left <= 0 ? 0 : (int) ((left + DAY - 1) / DAY);
	}

	/** Points for slaying a curse under the curse tithe: 1 plus half its grade rank, 10 for a special grade. */
	public static int curseValue(int gradeRank, boolean special) {
		return special ? 10 : 1 + Math.max(0, gradeRank) / 2;
	}

	/** What a transfer can actually move: no more than is held, in whole steps. */
	public static int transferable(int requested, int step, int available) {
		int s = Math.max(1, step);
		int amount = Math.max(0, Math.min(requested, available));
		return amount - amount % s;
	}

	/** Points kept after dying as a participant. */
	public static int afterDeath(int points, double lossPercent) {
		double keep = 1 - Math.max(0, Math.min(100, lossPercent)) / 100.0;
		return (int) Math.floor(points * keep);
	}

	/** Grade experience for the final standings: 20 per point, plus 2000, 1000, 500 for the podium. */
	public static int finalXp(int points, int place) {
		int bonus = switch (place) {
			case 1 -> 2000;
			case 2 -> 1000;
			case 3 -> 500;
			default -> 0;
		};
		return Math.max(0, points) * 20 + bonus;
	}

	/**
	 * Whether the game ends: after {@code endAfterDays} (0 = never by days), or when no more than
	 * {@code endWhenLeft} participants still stand (0 = off) once at least two have ever joined.
	 */
	public static boolean shouldEnd(int day, int endAfterDays, int standing, int joinedEver, int endWhenLeft) {
		if (endAfterDays > 0 && day >= endAfterDays) {
			return true;
		}

		return endWhenLeft > 0 && joinedEver >= 2 && standing <= endWhenLeft;
	}

	/**
	 * Whether a point lies inside a colony: within the radius horizontally, and under the barrier:
	 * a wall {@link #WALL} blocks high, then a dome. Anything below the centre counts as inside.
	 */
	public static boolean inside(double dx, double dy, double dz, double radius) {
		double h2 = dx * dx + dz * dz;
		if (h2 > radius * radius) {
			return false;
		}

		return dy <= WALL + Math.sqrt(Math.max(0, radius * radius - h2));
	}

	/** Barrier height above the centre at horizontal distance {@code d}. */
	public static double roofAt(double d, double radius) {
		return WALL + Math.sqrt(Math.max(0, radius * radius - d * d));
	}

	/** Colony centres spaced evenly on a ring, as {x, z} offsets. */
	public static double[][] ring(int count, double distance, double phase) {
		double[][] out = new double[Math.max(0, count)][];
		for (int i = 0; i < out.length; i++) {
			double a = phase + 2 * Math.PI * i / out.length;
			out[i] = new double[] {Math.cos(a) * distance, Math.sin(a) * distance};
		}

		return out;
	}
}
