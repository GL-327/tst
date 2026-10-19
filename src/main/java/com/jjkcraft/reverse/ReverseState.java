package com.jjkcraft.reverse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reverse Cursed Technique progress (persistent).
 *
 * @param unlocked       the technique has awakened
 * @param autoHeal       heal automatically when hurt (toggle)
 * @param lastSaveTick   game time of the last emergency save from a lethal blow
 */
public record ReverseState(boolean unlocked, boolean autoHeal, long lastSaveTick) {
	public static final ReverseState EMPTY = new ReverseState(false, true, Long.MIN_VALUE / 2);

	public static final Codec<ReverseState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(ReverseState::unlocked),
			Codec.BOOL.optionalFieldOf("auto_heal", true).forGetter(ReverseState::autoHeal),
			Codec.LONG.optionalFieldOf("last_save", Long.MIN_VALUE / 2).forGetter(ReverseState::lastSaveTick)
	).apply(i, ReverseState::new));

	public ReverseState withUnlocked(boolean value) {
		return new ReverseState(value, autoHeal, lastSaveTick);
	}

	public ReverseState withAutoHeal(boolean value) {
		return new ReverseState(unlocked, value, lastSaveTick);
	}

	public ReverseState withLastSave(long tick) {
		return new ReverseState(unlocked, autoHeal, tick);
	}
}
