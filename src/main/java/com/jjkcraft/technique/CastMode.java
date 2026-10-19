package com.jjkcraft.technique;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** How a technique is used once the cast key is pressed. */
public enum CastMode implements StringRepresentable {
	/** Fires once (after the cast time). */
	INSTANT("instant"),
	/** Stays active while the cast key is held, draining {@code upkeep} CE per second. */
	CHANNEL("channel"),
	/** Pressing toggles it on and off; drains {@code upkeep} CE per second while on. */
	TOGGLE("toggle");

	public static final Codec<CastMode> CODEC = StringRepresentable.fromEnum(CastMode::values);

	private final String name;

	CastMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
