package com.jjkcraft.technique;

import java.util.Locale;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Why the server refused a cast. Sent back so the HUD can explain it. */
public enum CastFailure {
	NONE,
	EMPTY_SLOT,
	NOT_KNOWN,
	GRADE_TOO_LOW,
	ON_COOLDOWN,
	NOT_ENOUGH_ENERGY,
	HEAVENLY_RESTRICTION,
	STUNNED,
	DISABLED,
	BUSY,
	/** After a domain: techniques are burnt out for a while. */
	BURNOUT,
	/** An information-flood domain has overloaded the caster. */
	OVERLOADED,
	/** No domain to expand. */
	NO_DOMAIN,
	/** A Binding Vow forbids it. */
	VOW,
	/** The technique also costs Mul energy (the Modulo expansion) and the caster has too little. */
	NOT_ENOUGH_MUL,
	NOT_ENOUGH_HEALTH;

	public static final StreamCodec<ByteBuf, CastFailure> STREAM_CODEC =
			ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], CastFailure::ordinal);

	public String translationKey() {
		return "cast_failure.jjkcraft." + name().toLowerCase(Locale.ROOT);
	}
}
