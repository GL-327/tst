package com.jjkcraft.grade;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** The kinds of promotion trials that grade data can ask for. Each is counted in code. */
public enum TrialType implements StringRepresentable {
	/** Defeat {@code count} curses of at least {@code grade}. */
	DEFEAT_CURSE("defeat_curse"),
	/** Master {@code count} techniques (mastery 100). */
	MASTER_TECHNIQUES("master_techniques"),
	/** Earn {@code count} Culling Game points in total. */
	CULLING_POINTS("culling_points"),
	/** Keep a Binding Vow active for {@code count} in-game days in total. */
	VOW_DAYS("vow_days"),
	/** Win {@code count} domain clashes. */
	DOMAIN_CLASH_WINS("domain_clash_wins"),
	/**
	 * The Special Grade unlock, "The Unbound Ritual": defeat a curse of at least {@code grade}
	 * inside your own Domain Expansion while holding a Binding Vow and having mastered your innate
	 * technique. Counted by {@code SpecialGradeRitual}.
	 */
	UNBOUND_RITUAL("unbound_ritual");

	public static final Codec<TrialType> CODEC = StringRepresentable.fromEnum(TrialType::values);

	private final String name;

	TrialType(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
