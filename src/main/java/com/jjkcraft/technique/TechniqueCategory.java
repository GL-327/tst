package com.jjkcraft.technique;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Technique categories from the brief. Used for UI grouping, icons and a few rules (passives toggle). */
public enum TechniqueCategory implements StringRepresentable {
	STRIKE("strike", 0xFFE0303F),
	PROJECTILE("projectile", 0xFF53E0FF),
	AREA("area", 0xFF8F5CFF),
	RESTRICTION("restriction", 0xFFB08D57),
	SUMMON("summon", 0xFF3B3F8C),
	DOMAIN("domain", 0xFFC0392B),
	PASSIVE("passive", 0xFF76C7A5),
	HEALING("healing", 0xFF5BE38C),
	UTILITY("utility", 0xFFE9E3D5),
	ULTIMATE("ultimate", 0xFFF2C14E);

	public static final Codec<TechniqueCategory> CODEC = StringRepresentable.fromEnum(TechniqueCategory::values);

	private final String name;
	private final int color;

	TechniqueCategory(String name, int color) {
		this.name = name;
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int color() {
		return color;
	}

	public String translationKey() {
		return "technique_category.jjkcraft." + name;
	}
}
