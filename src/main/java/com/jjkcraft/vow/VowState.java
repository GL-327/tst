package com.jjkcraft.vow;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.jjkcraft.core.vow.VowMath;

/**
 * A player's Binding Vow (persistent, kept through death).
 *
 * @param vows      restrictions held (empty: no vow)
 * @param focus     where the power goes
 * @param term      how long it holds
 * @param startedAt game time it was sworn
 * @param endsAt    game time it is fulfilled, or -1 until renounced
 * @param points    power it is worth (fixed when sworn)
 * @param fulfilled vows kept to the end
 * @param broken    vows broken or renounced early
 * @param debtUntil game time the debt (halved regen) from the last broken vow ends
 */
public record VowState(List<Identifier> vows, VowMath.Focus focus, VowMath.Term term, long startedAt, long endsAt, double points,
		int fulfilled, int broken, long debtUntil) {
	public static final VowState EMPTY = new VowState(List.of(), VowMath.Focus.POWER, VowMath.Term.DAY, 0, -1, 0, 0, 0, 0);

	private static final Codec<VowMath.Focus> FOCUS = named(VowMath.Focus.values());
	private static final Codec<VowMath.Term> TERM = named(VowMath.Term.values());

	/** Saves an enum by its lowercase name, so reordering constants never corrupts saves. */
	private static <E extends Enum<E>> Codec<E> named(E[] values) {
		return Codec.STRING.comapFlatMap(name -> {
			for (E value : values) {
				if (value.name().equalsIgnoreCase(name)) {
					return DataResult.success(value);
				}
			}

			return DataResult.error(() -> "Unknown value " + name);
		}, value -> value.name().toLowerCase(java.util.Locale.ROOT));
	}

	public static final Codec<VowState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.listOf().optionalFieldOf("vows", List.of()).forGetter(VowState::vows),
			FOCUS.optionalFieldOf("focus", VowMath.Focus.POWER).forGetter(VowState::focus),
			TERM.optionalFieldOf("term", VowMath.Term.DAY).forGetter(VowState::term),
			Codec.LONG.optionalFieldOf("started_at", 0L).forGetter(VowState::startedAt),
			Codec.LONG.optionalFieldOf("ends_at", -1L).forGetter(VowState::endsAt),
			Codec.DOUBLE.optionalFieldOf("points", 0.0).forGetter(VowState::points),
			Codec.INT.optionalFieldOf("fulfilled", 0).forGetter(VowState::fulfilled),
			Codec.INT.optionalFieldOf("broken", 0).forGetter(VowState::broken),
			Codec.LONG.optionalFieldOf("debt_until", 0L).forGetter(VowState::debtUntil)
	).apply(i, VowState::new));

	public boolean active() {
		return !vows.isEmpty();
	}

	public boolean inDebt(long now) {
		return now < debtUntil;
	}

	public VowState sworn(List<Identifier> next, VowMath.Focus nextFocus, VowMath.Term nextTerm, long now, double nextPoints) {
		long end = nextTerm.ticks < 0 ? -1 : now + nextTerm.ticks;
		return new VowState(List.copyOf(next), nextFocus, nextTerm, now, end, nextPoints, fulfilled, broken, debtUntil);
	}

	public VowState ended(boolean kept, long debt) {
		return new VowState(List.of(), focus, term, startedAt, -1, 0, kept ? fulfilled + 1 : fulfilled, kept ? broken : broken + 1, debt);
	}
}
