package com.jjkcraft.core.energy;

/**
 * Pure cursed energy math. No Minecraft types, so it is unit-tested directly and shared by the
 * server (authority) and the client (bar prediction).
 */
public final class EnergyMath {
	private EnergyMath() {
	}

	/** Result of a spend attempt. {@code state} is unchanged (but advanced) when {@code success} is false. */
	public record SpendResult(EnergyState state, boolean success, boolean triggeredOverexertion) {
	}

	/**
	 * Advances regeneration from {@code state.lastUpdateTick()} to {@code now} in closed form.
	 *
	 * <p>Regen is blocked until {@code overexertedUntil}. After that it runs at the normal rate until
	 * {@code lastActionTick + idleDelayTicks}, and at the idle rate from then on. The result is the
	 * same as ticking one tick at a time, which {@code EnergyMathTest} checks.
	 *
	 * @param regenModifier multiplier from grade efficiency, domains and vows (0 disables regen)
	 */
	public static EnergyState advance(EnergyState state, double max, long now, EnergyParams params, double regenModifier) {
		if (now <= state.lastUpdateTick()) {
			return state;
		}

		double current = Math.min(state.current(), max);
		if (current < max && regenModifier > 0 && max > 0) {
			long start = Math.max(state.lastUpdateTick(), state.overexertedUntil());
			if (start < now) {
				long idleStart = Math.max(start, state.lastActionTick() + params.idleDelayTicks());
				long normalTicks = Math.max(0, Math.min(now, idleStart) - start);
				long idleTicks = Math.max(0, now - idleStart);
				double rate = params.regenPerTick(max) * regenModifier;
				current = Math.min(max, current + normalTicks * rate + idleTicks * rate * params.idleMultiplier());
			}
		}

		return new EnergyState(current, now, state.overexertedUntil(), state.lastActionTick());
	}

	/**
	 * Spends {@code cost} CE at {@code now}. Fails without change if there is not enough CE. Spending
	 * that leaves CE below the overexertion threshold starts the no-regen window, unless one is
	 * already running.
	 */
	public static SpendResult spend(EnergyState state, double max, double cost, long now, EnergyParams params, double regenModifier) {
		EnergyState advanced = advance(state, max, now, params, regenModifier);
		if (cost < 0 || advanced.current() + 1e-9 < cost) {
			return new SpendResult(advanced, false, false);
		}

		double remaining = Math.max(0, advanced.current() - cost);
		boolean crossesThreshold = remaining < max * params.overexertThreshold()
				&& advanced.current() >= max * params.overexertThreshold();
		boolean overexert = crossesThreshold && !advanced.isOverexerted(now);
		long overexertedUntil = overexert ? now + params.overexertTicks() : advanced.overexertedUntil();
		return new SpendResult(new EnergyState(remaining, now, overexertedUntil, now), true, overexert);
	}

	/**
	 * Removes up to {@code amount} CE (for example a curse draining on hit). Never fails, never goes
	 * below zero, and counts as an action, so idle regen restarts. It does not trigger overexertion,
	 * because overexertion comes from the entity's own output.
	 *
	 * @return the new state; the amount actually removed is {@code before - after}
	 */
	public static EnergyState drain(EnergyState state, double max, double amount, long now, EnergyParams params, double regenModifier) {
		EnergyState advanced = advance(state, max, now, params, regenModifier);
		double remaining = Math.max(0, advanced.current() - Math.max(0, amount));
		return new EnergyState(remaining, now, advanced.overexertedUntil(), now);
	}

	/** Adds CE (shards, absorbing a curse) up to max. Does not count as an action. */
	public static EnergyState restore(EnergyState state, double max, double amount, long now, EnergyParams params, double regenModifier) {
		EnergyState advanced = advance(state, max, now, params, regenModifier);
		return advanced.withCurrent(Math.min(max, advanced.current() + Math.max(0, amount)));
	}

	/**
	 * Maximum CE for a sorcerer.
	 *
	 * @param base               base capacity from config
	 * @param gradeMultiplier    capacity multiplier from the sorcerer's grade data
	 * @param outputPoints       points in the Output growth path (each adds 3%)
	 * @param heavenlyRestricted Heavenly Restriction sets capacity to zero
	 */
	public static double sorcererCapacity(double base, double gradeMultiplier, int outputPoints, boolean heavenlyRestricted) {
		if (heavenlyRestricted) {
			return 0;
		}

		return base * gradeMultiplier * (1.0 + 0.03 * Math.max(0, outputPoints));
	}

	/** Seconds until {@code target} CE is reached from {@code state}, ignoring future actions. For tooltips and balance tests. */
	public static double secondsToReach(EnergyState state, double max, double target, long now, EnergyParams params, double regenModifier) {
		EnergyState s = advance(state, max, now, params, regenModifier);
		if (s.current() >= target) {
			return 0;
		}

		if (regenModifier <= 0 || max <= 0 || target > max) {
			return Double.POSITIVE_INFINITY;
		}

		double rate = params.regenPerTick(max) * regenModifier;
		long t = now;
		double current = s.current();
		long blockedUntil = Math.max(t, s.overexertedUntil());
		long idleStart = Math.max(blockedUntil, s.lastActionTick() + params.idleDelayTicks());
		double normalGain = (idleStart - blockedUntil) * rate;

		if (current + normalGain >= target) {
			return (blockedUntil - t + (target - current) / rate) / 20.0;
		}

		double afterNormal = current + normalGain;
		double idleRate = rate * params.idleMultiplier();
		return (idleStart - t + (target - afterNormal) / idleRate) / 20.0;
	}
}
