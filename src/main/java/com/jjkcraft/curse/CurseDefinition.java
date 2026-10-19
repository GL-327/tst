package com.jjkcraft.curse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import com.jjkcraft.registry.LegacyIds;

/**
 * A curse ({@code data/<ns>/jjkcraft/curse/*.json}). The entity type and its mechanic are code;
 * everything else (grade, stats, techniques, spawning, loot, resistances, model keys, codex) is
 * data, so a datapack can add a curse that reuses an existing entity type and mechanic.
 *
 * @param name        translation key of the name
 * @param description translation key of the one-line description
 * @param lore        translation key of the codex entry
 * @param entity      entity type id this curse spawns as
 * @param grade       grade on the shared ladder
 * @param stats       health, armor, speed, damage, CE
 * @param techniques  technique ids it may cast (within its CE budget)
 * @param spawn       natural spawning rules; absent means it never spawns naturally
 * @param lootTable   loot table id; absent uses the entity type's default table
 * @param resistances damage type id to damage multiplier (below 1 resists, above 1 is a weakness)
 * @param model       GeckoLib model, texture and animation ids, and render scale
 * @param mechanic    id of the code mechanic that makes this curse unique
 * @param parameters  numbers the mechanic reads (all optional; the mechanic has defaults)
 * @param codex       codex color and how many kills unlock the full entry
 */
public record CurseDefinition(
		String name,
		String description,
		String lore,
		Identifier entity,
		Identifier grade,
		Stats stats,
		List<Identifier> techniques,
		Optional<Spawn> spawn,
		Optional<Identifier> lootTable,
		Map<Identifier, Float> resistances,
		Model model,
		Identifier mechanic,
		Map<String, Double> parameters,
		Codex codex
) {
	public static final Codec<CurseDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(CurseDefinition::name),
			Codec.STRING.fieldOf("description").forGetter(CurseDefinition::description),
			Codec.STRING.fieldOf("lore").forGetter(CurseDefinition::lore),
			LegacyIds.ENTITY_ID.fieldOf("entity").forGetter(CurseDefinition::entity),
			Identifier.CODEC.fieldOf("grade").forGetter(CurseDefinition::grade),
			Stats.CODEC.fieldOf("stats").forGetter(CurseDefinition::stats),
			LegacyIds.TECHNIQUE_ID.listOf().optionalFieldOf("techniques", List.of()).forGetter(CurseDefinition::techniques),
			Spawn.CODEC.optionalFieldOf("spawn").forGetter(CurseDefinition::spawn),
			Identifier.CODEC.optionalFieldOf("loot_table").forGetter(CurseDefinition::lootTable),
			Codec.unboundedMap(Identifier.CODEC, Codec.floatRange(0, 10)).optionalFieldOf("resistances", Map.of()).forGetter(CurseDefinition::resistances),
			Model.CODEC.fieldOf("model").forGetter(CurseDefinition::model),
			Identifier.CODEC.fieldOf("mechanic").forGetter(CurseDefinition::mechanic),
			Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("parameters", Map.of()).forGetter(CurseDefinition::parameters),
			Codex.CODEC.optionalFieldOf("codex", Codex.DEFAULT).forGetter(CurseDefinition::codex)
	).apply(i, CurseDefinition::new));

	public double parameter(String key, double fallback) {
		return parameters.getOrDefault(key, fallback);
	}

	/**
	 * @param health           max health
	 * @param armor            armor points
	 * @param speed            movement speed attribute (players walk at 0.1; zombies 0.23)
	 * @param attackDamage     melee damage
	 * @param ceCapacity       max CE
	 * @param regenMultiplier  multiplier on CE regen (near fear sources it rises further)
	 * @param drainOnHit       CE drained from the target on each melee hit
	 * @param followRange      how far it notices targets
	 * @param knockbackResist  knockback resistance, 0 to 1
	 */
	public record Stats(double health, double armor, double speed, double attackDamage, double ceCapacity, double regenMultiplier,
			double drainOnHit, double followRange, double knockbackResist) {
		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.doubleRange(1, 4096).fieldOf("health").forGetter(Stats::health),
				Codec.doubleRange(0, 30).optionalFieldOf("armor", 0.0).forGetter(Stats::armor),
				Codec.doubleRange(0, 2).fieldOf("speed").forGetter(Stats::speed),
				Codec.doubleRange(0, 1000).fieldOf("attack_damage").forGetter(Stats::attackDamage),
				Codec.doubleRange(0, 100_000).fieldOf("ce_capacity").forGetter(Stats::ceCapacity),
				Codec.doubleRange(0, 100).optionalFieldOf("regen_multiplier", 1.0).forGetter(Stats::regenMultiplier),
				Codec.doubleRange(0, 1000).optionalFieldOf("drain_on_hit", 4.0).forGetter(Stats::drainOnHit),
				Codec.doubleRange(4, 128).optionalFieldOf("follow_range", 24.0).forGetter(Stats::followRange),
				Codec.doubleRange(0, 1).optionalFieldOf("knockback_resistance", 0.0).forGetter(Stats::knockbackResist)
		).apply(i, Stats::new));
	}

	/**
	 * @param weight     relative spawn weight among curses that qualify
	 * @param minFear    fear (0 to 100) needed where it spawns
	 * @param groupMin   smallest group
	 * @param groupMax   largest group
	 * @param maxLight   brightest block light it spawns in
	 * @param water      spawns in water instead of on the ground
	 * @param dimensions dimensions it spawns in (empty: any)
	 */
	public record Spawn(int weight, double minFear, int groupMin, int groupMax, int maxLight, boolean water, List<Identifier> dimensions) {
		public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(1, 1000).fieldOf("weight").forGetter(Spawn::weight),
				Codec.doubleRange(0, 100).fieldOf("min_fear").forGetter(Spawn::minFear),
				Codec.intRange(1, 16).optionalFieldOf("group_min", 1).forGetter(Spawn::groupMin),
				Codec.intRange(1, 16).optionalFieldOf("group_max", 1).forGetter(Spawn::groupMax),
				Codec.intRange(0, 15).optionalFieldOf("max_light", 7).forGetter(Spawn::maxLight),
				Codec.BOOL.optionalFieldOf("water", false).forGetter(Spawn::water),
				Identifier.CODEC.listOf().optionalFieldOf("dimensions", List.of()).forGetter(Spawn::dimensions)
		).apply(i, Spawn::new));
	}

	/** GeckoLib asset ids. Paths follow GeckoLib's layout (models and animations under {@code geckolib/}). */
	public record Model(Identifier geo, Identifier texture, Identifier animation, float scale, Optional<Disguise> disguise) {
		public static final Codec<Model> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("geo").forGetter(Model::geo),
				Identifier.CODEC.fieldOf("texture").forGetter(Model::texture),
				Identifier.CODEC.fieldOf("animation").forGetter(Model::animation),
				Codec.floatRange(0.1f, 10f).optionalFieldOf("scale", 1f).forGetter(Model::scale),
				Disguise.CODEC.optionalFieldOf("disguise").forGetter(Model::disguise)
		).apply(i, Model::new));
	}

	/** The model and texture shown while the curse is disguised (its mechanic decides when). */
	public record Disguise(Identifier geo, Identifier texture) {
		public static final Codec<Disguise> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("geo").forGetter(Disguise::geo),
				Identifier.CODEC.fieldOf("texture").forGetter(Disguise::texture)
		).apply(i, Disguise::new));
	}

	/**
	 * @param color       accent color in the codex
	 * @param unlockKills kills before the full entry (lore, weaknesses, model) is revealed
	 */
	public record Codex(int color, int unlockKills) {
		public static final Codex DEFAULT = new Codex(0xFF8F5CFF, 1);

		public static final Codec<Codex> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("color", 0xFF8F5CFF).forGetter(Codex::color),
				Codec.intRange(1, 1000).optionalFieldOf("unlock_kills", 1).forGetter(Codex::unlockKills)
		).apply(i, Codex::new));
	}
}
