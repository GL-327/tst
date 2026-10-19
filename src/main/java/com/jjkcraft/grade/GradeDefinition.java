package com.jjkcraft.grade;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/**
 * A grade on the sorcerer and curse ladder ({@code data/<ns>/jjkcraft/grade/*.json}).
 *
 * @param rank           position on the ladder; 0 is Grade 4, 6 is Special Grade
 * @param name           translation key of the display name
 * @param color          badge color, {@code #RRGGBB}
 * @param ceMultiplier   multiplier on base CE capacity
 * @param techniqueSlots number of equipped technique slots (1 to 6)
 * @param domainTier     0: no domains; 1: domains that need tier 1; 2: every domain
 * @param xpRequired     grade experience needed to be promoted into this grade
 * @param minControl     minimum cast accuracy (0 to 1) needed to be promoted into this grade
 * @param minOutput      minimum lifetime CE spent on techniques needed to be promoted into this grade
 * @param trials         trials that must be complete to be promoted into this grade
 * @param special        an anomaly grade: never reached through experience, only through its unlock trial
 * @param perks          numeric perks applied while holding this grade
 * @param toolDurability durability of a cursed tool of this grade used as a medium (0: a fallback by rank)
 */
public record GradeDefinition(
		int rank,
		String name,
		int color,
		double ceMultiplier,
		int techniqueSlots,
		int domainTier,
		int xpRequired,
		double minControl,
		double minOutput,
		List<Trial> trials,
		boolean special,
		Perks perks,
		int toolDurability
) {
	public static final Codec<GradeDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(0, 32).fieldOf("rank").forGetter(GradeDefinition::rank),
			Codec.STRING.fieldOf("name").forGetter(GradeDefinition::name),
			ExtraCodecs.STRING_RGB_COLOR.fieldOf("color").forGetter(GradeDefinition::color),
			Codec.doubleRange(0, 1000).fieldOf("ce_multiplier").forGetter(GradeDefinition::ceMultiplier),
			Codec.intRange(1, 6).fieldOf("technique_slots").forGetter(GradeDefinition::techniqueSlots),
			Codec.intRange(0, 2).optionalFieldOf("domain_tier", 0).forGetter(GradeDefinition::domainTier),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("xp_required", 0).forGetter(GradeDefinition::xpRequired),
			Codec.doubleRange(0, 1).optionalFieldOf("min_control", 0.0).forGetter(GradeDefinition::minControl),
			Codec.doubleRange(0, 1e9).optionalFieldOf("min_output", 0.0).forGetter(GradeDefinition::minOutput),
			Trial.CODEC.listOf().optionalFieldOf("trials", List.of()).forGetter(GradeDefinition::trials),
			Codec.BOOL.optionalFieldOf("special", false).forGetter(GradeDefinition::special),
			Perks.CODEC.optionalFieldOf("perks", Perks.NONE).forGetter(GradeDefinition::perks),
			Codec.intRange(0, 1_000_000).optionalFieldOf("tool_durability", 0).forGetter(GradeDefinition::toolDurability)
	).apply(i, GradeDefinition::new));

	/**
	 * A promotion requirement.
	 *
	 * @param id    stable id stored in the player's completed trials
	 * @param type  what is counted; see {@link TrialType}
	 * @param grade for {@code defeat_curse} and {@code domain_kill}: the minimum curse grade
	 * @param count how many are needed
	 */
	public record Trial(String id, TrialType type, Identifier grade, int count) {
		public static final Codec<Trial> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("id").forGetter(Trial::id),
				TrialType.CODEC.fieldOf("type").forGetter(Trial::type),
				Identifier.CODEC.optionalFieldOf("grade", Identifier.withDefaultNamespace("none")).forGetter(Trial::grade),
				Codec.intRange(1, 100_000).optionalFieldOf("count", 1).forGetter(Trial::count)
		).apply(i, Trial::new));
	}

	/**
	 * Perks of a grade.
	 *
	 * @param senseRange      blocks at which this sorcerer senses CE through walls
	 * @param regenBonus      multiplier added to CE regen (0.1 = +10%)
	 * @param blackFlashBonus percentage points added to Black Flash chance
	 */
	public record Perks(double senseRange, double regenBonus, double blackFlashBonus) {
		public static final Perks NONE = new Perks(16, 0, 0);

		public static final Codec<Perks> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.doubleRange(0, 512).optionalFieldOf("sense_range", 16.0).forGetter(Perks::senseRange),
				Codec.doubleRange(-1, 10).optionalFieldOf("regen_bonus", 0.0).forGetter(Perks::regenBonus),
				Codec.doubleRange(0, 100).optionalFieldOf("black_flash_bonus", 0.0).forGetter(Perks::blackFlashBonus)
		).apply(i, Perks::new));
	}
}
