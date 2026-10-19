package com.jjkcraft.domain;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import com.jjkcraft.domain.behavior.DomainBehavior;
import com.jjkcraft.domain.behavior.DomainBehaviors;

/**
 * A Domain Expansion ({@code data/<ns>/jjkcraft/domain/*.json}). Synced, so the HUD, the title
 * sequence and the dome read the same file. What it does inside is a list of behaviors; the
 * numbers and looks are data.
 *
 * @param name          translation key of the name (shown in the opening title)
 * @param description   translation key of the description
 * @param grade         lowest grade that can cast it (the grade must also allow domains)
 * @param castCost      CE paid to open it
 * @param upkeep        CE per second while it stands
 * @param radius        radius in blocks
 * @param maxDuration   ticks before it closes by itself
 * @param burnout       ticks after it closes during which the caster can't use techniques
 * @param sureHit       true: effects on targets can't be dodged, blocked by armor or shields
 * @param clashPriority added clash power (+5% each) and the first tie-breaker
 * @param behaviors     what happens inside
 * @param visuals       colors, ambience and sounds
 * @param innateWeight  weight when a sorcerer reaching a domain-capable grade is given a domain (0: never)
 */
public record DomainDefinition(
		String name,
		String description,
		Identifier grade,
		double castCost,
		double upkeep,
		double radius,
		int maxDuration,
		int burnout,
		boolean sureHit,
		int clashPriority,
		List<DomainBehavior> behaviors,
		Visuals visuals,
		int innateWeight
) {
	public static final Codec<DomainDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(DomainDefinition::name),
			Codec.STRING.fieldOf("description").forGetter(DomainDefinition::description),
			Identifier.CODEC.fieldOf("grade").forGetter(DomainDefinition::grade),
			Codec.doubleRange(0, 100_000).fieldOf("cast_cost").forGetter(DomainDefinition::castCost),
			Codec.doubleRange(0, 10_000).fieldOf("upkeep").forGetter(DomainDefinition::upkeep),
			Codec.doubleRange(3, 48).fieldOf("radius").forGetter(DomainDefinition::radius),
			Codec.intRange(20, 72_000).optionalFieldOf("max_duration", 1200).forGetter(DomainDefinition::maxDuration),
			Codec.intRange(0, 72_000).optionalFieldOf("burnout", 400).forGetter(DomainDefinition::burnout),
			Codec.BOOL.optionalFieldOf("sure_hit", true).forGetter(DomainDefinition::sureHit),
			Codec.intRange(0, 20).optionalFieldOf("clash_priority", 0).forGetter(DomainDefinition::clashPriority),
			DomainBehaviors.CODEC.listOf().fieldOf("behaviors").forGetter(DomainDefinition::behaviors),
			Visuals.CODEC.fieldOf("visuals").forGetter(DomainDefinition::visuals),
			Codec.intRange(0, 10_000).optionalFieldOf("innate_weight", 10).forGetter(DomainDefinition::innateWeight)
	).apply(i, DomainDefinition::new));

	/**
	 * @param color        main accent: title, glyph ring
	 * @param secondary    second accent: decorations and particles
	 * @param dome         color of the barrier dome seen from inside and outside
	 * @param ambience     client decoration inside: {@code blades}, {@code glyphs}, {@code shadows},
	 *                     {@code lanterns}, {@code arsenal}, {@code rings} or {@code none}
	 * @param openSound    sound when it opens (default {@code jjkcraft:domain.open})
	 * @param ambientSound loop while a player is inside (default {@code jjkcraft:domain.ambient})
	 */
	public record Visuals(int color, int secondary, int dome, String ambience, Optional<Identifier> openSound, Optional<Identifier> ambientSound) {
		public static final Codec<Visuals> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.STRING_RGB_COLOR.fieldOf("color").forGetter(Visuals::color),
				ExtraCodecs.STRING_RGB_COLOR.fieldOf("secondary").forGetter(Visuals::secondary),
				ExtraCodecs.STRING_RGB_COLOR.fieldOf("dome").forGetter(Visuals::dome),
				Codec.STRING.optionalFieldOf("ambience", "none").forGetter(Visuals::ambience),
				Identifier.CODEC.optionalFieldOf("open_sound").forGetter(Visuals::openSound),
				Identifier.CODEC.optionalFieldOf("ambient_sound").forGetter(Visuals::ambientSound)
		).apply(i, Visuals::new));
	}
}
