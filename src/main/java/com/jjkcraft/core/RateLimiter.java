package com.jjkcraft.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Token bucket per key. Used to drop client requests that arrive faster than any real player could
 * send them. Time is passed in (game ticks) so it is deterministic and testable.
 */
public final class RateLimiter {
	private final double tokensPerTick;
	private final double burst;
	private final Map<UUID, double[]> buckets = new HashMap<>();

	/**
	 * @param perSecond sustained requests per second
	 * @param burst     requests allowed at once
	 */
	public RateLimiter(double perSecond, double burst) {
		this.tokensPerTick = perSecond / 20.0;
		this.burst = burst;
		ALL.add(this);
	}

	/** Every limiter made, so a player who leaves can be forgotten by all of them at once. */
	private static final java.util.List<RateLimiter> ALL = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** Drops {@code key}'s buckets in every limiter (call when a player disconnects). */
	public static void forgetEverywhere(UUID key) {
		for (RateLimiter limiter : ALL) {
			limiter.forget(key);
		}
	}

	/** Takes a token for {@code key} at game tick {@code now}. Returns false if the bucket is empty. */
	public synchronized boolean tryAcquire(UUID key, long now) {
		double[] bucket = buckets.computeIfAbsent(key, k -> new double[] {burst, now});
		double elapsed = Math.max(0, now - bucket[1]);
		bucket[0] = Math.min(burst, bucket[0] + elapsed * tokensPerTick);
		bucket[1] = now;
		if (bucket[0] < 1) {
			return false;
		}

		bucket[0] -= 1;
		return true;
	}

	public synchronized void forget(UUID key) {
		buckets.remove(key);
	}
}
