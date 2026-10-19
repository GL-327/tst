package com.jjkcraft.core.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The stored cursed energy of one entity. Immutable.
 *
 * <p>Regeneration is lazy: {@code current} is the value at {@code lastUpdateTick}. The value at any
 * later game time is computed by {@link EnergyMath#advance}. The client runs the same function to
 * predict the bar between syncs, so the server only syncs when something changes.
 *
 * @param current          CE at {@code lastUpdateTick}
 * @param lastUpdateTick   game time the value was last advanced to
 * @param overexertedUntil game time until which regeneration is blocked (overexertion)
 * @param lastActionTick   game time of the last cast, drain or other CE action (idle regen starts after it)
 */
public record EnergyState(double current, long lastUpdateTick, long overexertedUntil, long lastActionTick) {
	public static final EnergyState EMPTY = new EnergyState(0, 0, 0, 0);

	public static final Codec<EnergyState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.fieldOf("current").forGetter(EnergyState::current),
			Codec.LONG.optionalFieldOf("last_update", 0L).forGetter(EnergyState::lastUpdateTick),
			Codec.LONG.optionalFieldOf("overexerted_until", 0L).forGetter(EnergyState::overexertedUntil),
			Codec.LONG.optionalFieldOf("last_action", 0L).forGetter(EnergyState::lastActionTick)
	).apply(i, EnergyState::new));

	public static EnergyState full(double max, long now) {
		return new EnergyState(max, now, 0, 0);
	}

	public boolean isOverexerted(long now) {
		return now < overexertedUntil;
	}

	public EnergyState withCurrent(double value) {
		return new EnergyState(value, lastUpdateTick, overexertedUntil, lastActionTick);
	}
}
