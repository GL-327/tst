package com.jjkcraft.technique;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * A cursed technique ({@code data/<ns>/jjkcraft/technique/*.json}). Data supplies names, numbers,
 * visuals and which behavior runs; code implements the behaviors.
 *
 * @param name         translation key of the display name
 * @param description  translation key of the description
 * @param category     UI category
 * @param grade        lowest grade that can cast it
 * @param cast         costs, timing and range
 * @param behavior     what it does, with its parameters
 * @param visuals      particle, sound, color, shake, hit-stop and trail keys
 * @param icon         icon texture
 * @param vowModifiers multipliers that apply while the caster holds a given Binding Vow
 * @param innateWeight weight when rolling an innate technique at awakening (0: never innate)
 * @param masteryUses  landed casts needed to master it
 */
public record TechniqueDefinition(
		String name,
		String description,
		TechniqueCategory category,
		Identifier grade,
		Cast cast,
		TechniqueBehavior behavior,
		Visuals visuals,
		Identifier icon,
		List<VowModifier> vowModifiers,
		int innateWeight,
		int masteryUses
) {
	public static final Codec<TechniqueDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(TechniqueDefinition::name),
			Codec.STRING.fieldOf("description").forGetter(TechniqueDefinition::description),
			TechniqueCategory.CODEC.fieldOf("category").forGetter(TechniqueDefinition::category),
			Identifier.CODEC.fieldOf("grade").forGetter(TechniqueDefinition::grade),
			Cast.CODEC.fieldOf("cast").forGetter(TechniqueDefinition::cast),
			TechniqueBehaviors.CODEC.fieldOf("behavior").forGetter(TechniqueDefinition::behavior),
			Visuals.CODEC.fieldOf("visuals").forGetter(TechniqueDefinition::visuals),
			Identifier.CODEC.fieldOf("icon").forGetter(TechniqueDefinition::icon),
			VowModifier.CODEC.listOf().optionalFieldOf("vow_modifiers", List.of()).forGetter(TechniqueDefinition::vowModifiers),
			Codec.intRange(0, 10_000).optionalFieldOf("innate_weight", 0).forGetter(TechniqueDefinition::innateWeight),
			Codec.intRange(1, 100_000).optionalFieldOf("mastery_uses", 50).forGetter(TechniqueDefinition::masteryUses)
	).apply(i, TechniqueDefinition::new));

	/**
	 * @param cost        CE per cast (instant) or to start (channel, toggle)
	 * @param cooldown    ticks before it can be cast again
	 * @param castTime    windup ticks between the key press and the effect
	 * @param range       max reach in blocks
	 * @param mode        instant, channel or toggle
	 * @param upkeep      CE per second while a channel or toggle is active
	 * @param maxDuration max ticks a channel or toggle stays active (0: no limit)
	 * @param mulCost     Mul energy per cast, paid besides the CE cost (the Modulo expansion; 0: none)
	 */
	public record Cast(double cost, int cooldown, int castTime, double range, CastMode mode, double upkeep, int maxDuration, double mulCost) {
		public static final Codec<Cast> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.doubleRange(0, 100_000).fieldOf("cost").forGetter(Cast::cost),
				Codec.intRange(0, 72_000).fieldOf("cooldown").forGetter(Cast::cooldown),
				Codec.intRange(0, 200).optionalFieldOf("cast_time", 0).forGetter(Cast::castTime),
				Codec.doubleRange(0, 256).optionalFieldOf("range", 0.0).forGetter(Cast::range),
				CastMode.CODEC.optionalFieldOf("mode", CastMode.INSTANT).forGetter(Cast::mode),
				Codec.doubleRange(0, 10_000).optionalFieldOf("upkeep", 0.0).forGetter(Cast::upkeep),
				Codec.intRange(0, 72_000).optionalFieldOf("max_duration", 0).forGetter(Cast::maxDuration),
				Codec.doubleRange(0, 100_000).optionalFieldOf("mul_cost", 0.0).forGetter(Cast::mulCost)
		).apply(i, Cast::new));
	}

	/** Visual and sound keys. Clients read these to build effects; the server never does. */
	public record Visuals(int color, Optional<Identifier> particle, Optional<Identifier> sound, Optional<Identifier> hitSound,
			float shake, int hitStop, Trail trail) {
		public static final Codec<Visuals> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.STRING_RGB_COLOR.fieldOf("color").forGetter(Visuals::color),
				Identifier.CODEC.optionalFieldOf("particle").forGetter(Visuals::particle),
				Identifier.CODEC.optionalFieldOf("sound").forGetter(Visuals::sound),
				Identifier.CODEC.optionalFieldOf("hit_sound").forGetter(Visuals::hitSound),
				Codec.floatRange(0, 5).optionalFieldOf("shake", 0f).forGetter(Visuals::shake),
				Codec.intRange(0, 20).optionalFieldOf("hit_stop", 0).forGetter(Visuals::hitStop),
				Trail.CODEC.optionalFieldOf("trail", Trail.NONE).forGetter(Visuals::trail)
		).apply(i, Visuals::new));
	}

	/** The world effect the client draws for a cast. */
	public enum Trail implements StringRepresentable {
		NONE("none"), SLASH("slash"), LINE("line"), BEAM("beam"), RING("ring"), CHAIN("chain"), ORB("orb"), AURA("aura"), SHOUT("shout");

		public static final Codec<Trail> CODEC = StringRepresentable.fromEnum(Trail::values);

		private final String name;

		Trail(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	/** Multipliers applied while the caster holds {@code vow}. */
	public record VowModifier(Identifier vow, double damage, double cost, double range) {
		public static final Codec<VowModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("vow").forGetter(VowModifier::vow),
				Codec.doubleRange(0, 100).optionalFieldOf("damage", 1.0).forGetter(VowModifier::damage),
				Codec.doubleRange(0, 100).optionalFieldOf("cost", 1.0).forGetter(VowModifier::cost),
				Codec.doubleRange(0, 100).optionalFieldOf("range", 1.0).forGetter(VowModifier::range)
		).apply(i, VowModifier::new));
	}
}
