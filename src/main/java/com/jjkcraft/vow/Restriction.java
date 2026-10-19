package com.jjkcraft.vow;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The kinds of restriction a vow can hold. Breakable ones end the vow when the sorcerer does the
 * forbidden thing; enforced ones simply refuse (a cast, a domain) without breaking it.
 */
public enum Restriction implements StringRepresentable {
	/** Breakable: never jump. */
	NO_JUMP(true),
	/** Breakable: never sprint. */
	NO_SPRINT(true),
	/** Breakable: wear no armor. */
	NO_ARMOR(true),
	/** Breakable: never raise a shield. */
	NO_SHIELD(true),
	/** Enforced: cast only the innate technique. */
	INNATE_ONLY(false),
	/** Enforced: never expand a domain. */
	NO_DOMAIN(false),
	/** Enforced: cast techniques only in darkness (night or deep underground). */
	NIGHT_ONLY(false),
	/** Enforced: hold only half the usual CE pool. */
	HALVED_RESERVE(false);

	public static final Codec<Restriction> CODEC = StringRepresentable.fromEnum(Restriction::values);

	public final boolean breakable;

	Restriction(boolean breakable) {
		this.breakable = breakable;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
