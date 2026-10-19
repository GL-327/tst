package com.jjkcraft.vow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A restriction a sorcerer may bind themselves to ({@code data/<ns>/jjkcraft/vow/*.json}). Synced,
 * so the Vow Builder lists them. Techniques can name a vow in {@code vow_modifiers} for extra power
 * while it is held.
 *
 * @param name        translation key of the name
 * @param description translation key of what it forbids
 * @param restriction what is forbidden (code)
 * @param weight      how much power it is worth (1 to 10)
 */
public record VowDefinition(String name, String description, Restriction restriction, double weight) {
	public static final Codec<VowDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(VowDefinition::name),
			Codec.STRING.fieldOf("description").forGetter(VowDefinition::description),
			Restriction.CODEC.fieldOf("restriction").forGetter(VowDefinition::restriction),
			Codec.doubleRange(0.5, 10).fieldOf("weight").forGetter(VowDefinition::weight)
	).apply(i, VowDefinition::new));
}
