package com.jjkcraft.client.state;

import net.minecraft.client.Minecraft;

import com.jjkcraft.core.energy.EnergyMath;
import com.jjkcraft.core.energy.EnergyParams;
import com.jjkcraft.core.energy.EnergyState;
import com.jjkcraft.network.payload.EnergySyncPayload;
import com.jjkcraft.network.payload.SettingsPayload;

/**
 * The local player's CE as last sent by the server, plus prediction. Between syncs the client runs
 * the same {@link EnergyMath#advance} the server uses, so the bar regenerates smoothly without
 * per-tick packets. The displayed value eases toward the predicted value.
 */
public final class ClientEnergy {
	private static EnergyParams params = EnergyParams.DEFAULT;
	private static EnergyState state = EnergyState.EMPTY;
	private static double max;
	private static double regenModifier = 1.0;
	private static boolean received;
	private static boolean domainsEnabled = true;
	private static boolean cullingEnabled = true;

	private static double displayed;
	private static long lastFrameNanos;

	private ClientEnergy() {
	}

	public static void accept(EnergySyncPayload payload) {
		boolean first = !received;
		state = payload.state();
		max = payload.max();
		regenModifier = payload.regenModifier();
		received = true;
		if (first) {
			displayed = state.current();
		}
	}

	public static void accept(SettingsPayload payload) {
		params = payload.energy();
		domainsEnabled = payload.domainsEnabled();
		cullingEnabled = payload.cullingEnabled();
	}

	public static void reset() {
		params = EnergyParams.DEFAULT;
		state = EnergyState.EMPTY;
		max = 0;
		regenModifier = 1.0;
		received = false;
		displayed = 0;
	}

	public static boolean hasData() {
		return received;
	}

	public static double max() {
		return max;
	}

	public static EnergyParams params() {
		return params;
	}

	public static boolean domainsEnabled() {
		return domainsEnabled;
	}

	public static boolean cullingEnabled() {
		return cullingEnabled;
	}

	public static long gameTime() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? state.lastUpdateTick() : mc.level.getGameTime();
	}

	/** The predicted current CE right now. */
	public static double predicted() {
		return EnergyMath.advance(state, max, gameTime(), params, regenModifier).current();
	}

	public static boolean overexerted() {
		return state.isOverexerted(gameTime());
	}

	public static boolean regenerating() {
		return max > 0 && regenModifier > 0 && !overexerted() && predicted() < max - 1e-6;
	}

	public static boolean idleBoosted() {
		return regenerating() && gameTime() >= state.lastActionTick() + params.idleDelayTicks();
	}

	/** Ticks of overexertion left, for the vignette fade. */
	public static long overexertTicksLeft() {
		return Math.max(0, state.overexertedUntil() - gameTime());
	}

	/** The smoothed value for drawing. Call once per frame. */
	public static double displayed() {
		long nowNanos = System.nanoTime();
		double dt = lastFrameNanos == 0 ? 0 : Math.min(0.25, (nowNanos - lastFrameNanos) / 1e9);
		lastFrameNanos = nowNanos;
		double target = predicted();
		// Exponential ease: about 90% of the gap closes in 0.2 s; drops show faster than gains.
		double rate = target < displayed ? 18.0 : 11.0;
		displayed += (target - displayed) * (1 - Math.exp(-rate * dt));
		if (Math.abs(target - displayed) < 0.01) {
			displayed = target;
		}

		return displayed;
	}
}
