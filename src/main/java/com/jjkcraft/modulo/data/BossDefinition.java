package com.jjkcraft.modulo.data;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A Modulo boss or fighting scene ({@code data/<ns>/jjkcraft/modulo_boss/*.json}). The numbers are
 * data; what the boss does is the code behavior it names (docs/MODULO.md 3.9, docs/SCHEMAS.md).
 *
 * @param behavior   the code behavior: trafficker, toranosuke, masaki_takeda, maru_duel, dabura, mahoraga_scene, minion
 * @param name       translation key of its name (boss bar)
 * @param skin       texture name under {@code textures/entity/modulo/}
 * @param health     health with one fighter; each extra fighter adds {@code modulo.bossHealthPerExtraPlayer} of it
 * @param armor      armor points
 * @param damage     melee damage
 * @param speed      movement speed attribute
 * @param scale      model scale
 * @param color      boss bar colour: pink, blue, red, green, yellow, purple, white
 * @param adaptation Mahoraga-style adaptation, if it has one
 * @param params     behavior numbers (each behavior documents its own; unknown keys are ignored)
 */
public record BossDefinition(String behavior, String name, String skin, double health, double armor, double damage, double speed, double scale,
		String color, Optional<Adaptation> adaptation, Map<String, Double> params) {
	public static final Set<String> BEHAVIORS = Set.of("trafficker", "toranosuke", "masaki_takeda", "maru_duel", "dabura", "mahoraga_scene", "minion");
	public static final Set<String> COLORS = Set.of("pink", "blue", "red", "green", "yellow", "purple", "white");

	public static final Codec<BossDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.validate(s -> BEHAVIORS.contains(s) ? DataResult.success(s) : DataResult.error(() -> "Unknown boss behavior: " + s + ", expected one of " + BEHAVIORS))
					.fieldOf("behavior").forGetter(BossDefinition::behavior),
			Codec.STRING.fieldOf("name").forGetter(BossDefinition::name),
			Codec.STRING.optionalFieldOf("skin", "").forGetter(BossDefinition::skin),
			Codec.doubleRange(1, 100_000).fieldOf("health").forGetter(BossDefinition::health),
			Codec.doubleRange(0, 30).optionalFieldOf("armor", 0.0).forGetter(BossDefinition::armor),
			Codec.doubleRange(0, 1000).optionalFieldOf("damage", 4.0).forGetter(BossDefinition::damage),
			Codec.doubleRange(0, 2).optionalFieldOf("speed", 0.3).forGetter(BossDefinition::speed),
			Codec.doubleRange(0.25, 4).optionalFieldOf("scale", 1.0).forGetter(BossDefinition::scale),
			Codec.STRING.validate(s -> COLORS.contains(s) ? DataResult.success(s) : DataResult.error(() -> "Unknown boss bar colour: " + s))
					.optionalFieldOf("color", "red").forGetter(BossDefinition::color),
			Adaptation.CODEC.optionalFieldOf("adaptation").forGetter(BossDefinition::adaptation),
			Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(BossDefinition::params)
	).apply(i, BossDefinition::new));

	public double param(String key, double fallback) {
		return params.getOrDefault(key, fallback);
	}

	/**
	 * Adaptation: the first hit of a kind of damage gives {@code first} resistance to that kind, each
	 * further hit adds {@code perHit}, up to {@code max}; it lasts for the rest of the fight.
	 *
	 * @param key           what counts as a kind: {@code technique} (the technique that dealt it, or the damage
	 *                      type for anything else) or {@code damage_type}
	 * @param fullAdaptation resistance at which a kind counts as fully adapted (the wheel completes a turn)
	 */
	public record Adaptation(String key, double first, double perHit, double max, double fullAdaptation) {
		public static final Codec<Adaptation> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.validate(s -> s.equals("technique") || s.equals("damage_type") ? DataResult.success(s)
						: DataResult.error(() -> "Adaptation key must be technique or damage_type: " + s)).optionalFieldOf("key", "technique").forGetter(Adaptation::key),
				Codec.doubleRange(0, 1).fieldOf("first").forGetter(Adaptation::first),
				Codec.doubleRange(0, 1).fieldOf("per_hit").forGetter(Adaptation::perHit),
				Codec.doubleRange(0, 1).fieldOf("max").forGetter(Adaptation::max),
				Codec.doubleRange(0, 1).optionalFieldOf("full_adaptation", 0.9).forGetter(Adaptation::fullAdaptation)
		).apply(i, Adaptation::new));
	}
}
