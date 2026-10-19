package com.jjkcraft.modulo.data;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * A Modulo mission ({@code data/<ns>/jjkcraft/modulo_mission/*.json}). Missions run one after
 * another by {@code order}; each is a list of stages (docs/MODULO.md 3.7, docs/SCHEMAS.md).
 *
 * @param order    position in the chain, 1 to 11 for the shipped missions
 * @param title    translation key of the title
 * @param summary  translation key of a one-line summary
 * @param chapters the Modulo chapters it follows (for the docs and the mission log)
 * @param stages   the stages, in order
 * @param rewards  given when the last stage is done
 */
public record MissionDefinition(int order, String title, String summary, String chapters, List<MissionStage> stages, Rewards rewards) {
	public static final Codec<MissionDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, 1000).fieldOf("order").forGetter(MissionDefinition::order),
			Codec.STRING.fieldOf("title").forGetter(MissionDefinition::title),
			Codec.STRING.fieldOf("summary").forGetter(MissionDefinition::summary),
			Codec.STRING.optionalFieldOf("chapters", "").forGetter(MissionDefinition::chapters),
			MissionStage.CODEC.listOf(1, 64).fieldOf("stages").forGetter(MissionDefinition::stages),
			Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(MissionDefinition::rewards)
	).apply(i, MissionDefinition::new));

	/**
	 * @param items       items given (id and count)
	 * @param gradeXp     grade experience
	 * @param mulEnergy   Mul energy
	 * @param companion   Maru joins as a companion
	 * @param advancement an advancement granted (the epilogue's)
	 */
	public record Rewards(List<ItemReward> items, int gradeXp, double mulEnergy, boolean companion, Optional<Identifier> advancement) {
		public static final Rewards NONE = new Rewards(List.of(), 0, 0, false, Optional.empty());
		public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemReward.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Rewards::items),
				Codec.intRange(0, 1_000_000).optionalFieldOf("grade_xp", 0).forGetter(Rewards::gradeXp),
				Codec.doubleRange(0, 100_000).optionalFieldOf("mul_energy", 0.0).forGetter(Rewards::mulEnergy),
				Codec.BOOL.optionalFieldOf("companion", false).forGetter(Rewards::companion),
				Identifier.CODEC.optionalFieldOf("advancement").forGetter(Rewards::advancement)
		).apply(i, Rewards::new));
	}

	public record ItemReward(Identifier item, int count) {
		public static final Codec<ItemReward> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("item").forGetter(ItemReward::item),
				Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ItemReward::count)
		).apply(i, ItemReward::new));
	}
}
