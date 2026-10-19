package com.jjkcraft.culling.rule;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.curse.mechanic.WardenMechanic;
import com.jjkcraft.registry.LegacyIds;

/**
 * A Culling Game rule (datapack registry {@code jjkcraft/culling_rule}, synced so the map can show
 * it). Base rules hold from the start; the rest are added by participants through the Kogane.
 *
 * @param text         translation key of the rule's wording
 * @param textArgs     arguments for the wording: literals, or {@code config:entry_days},
 *                     {@code config:sorcerer_points}, {@code config:non_sorcerer_points},
 *                     {@code config:rule_cost} for the live server values
 * @param base         active from the start; can't be negotiated
 * @param cost         points to add it; absent: the server's {@code ruleCost}
 * @param durationDays how long an added rule lasts (0: for the rest of the game)
 * @param order        position in the rule list
 * @param conflicts    rules it can't stand beside: the Kogane refuses it while any is active
 * @param clauses      when (trigger) this happens, do that (effects)
 * @param wardenEdict  while it stands, a Culling Warden in the game is weak only to this
 * @param bindsWarden  while it stands, a Culling Warden in the game can't die
 */
public record CullingRule(String text, List<String> textArgs, boolean base, Optional<Integer> cost, int durationDays, int order,
		List<Identifier> conflicts, List<Clause> clauses, Optional<String> wardenEdict, boolean bindsWarden) {
	public record Clause(RuleTriggers.Trigger when, List<RuleEffects.Effect> then) {
		public static final Codec<Clause> CODEC = RecordCodecBuilder.create(i -> i.group(
				RuleTriggers.CODEC.fieldOf("when").forGetter(Clause::when),
				RuleEffects.CODEC.listOf(1, 16).fieldOf("then").forGetter(Clause::then)
		).apply(i, Clause::new));
	}

	private static final Codec<String> EDICT = Codec.STRING.validate(s -> {
		for (WardenMechanic.Edict edict : WardenMechanic.Edict.values()) {
			if (edict.key().equals(s)) {
				return com.mojang.serialization.DataResult.success(s);
			}
		}

		return com.mojang.serialization.DataResult.error(() -> "Unknown edict '" + s + "' (steel, distance or energy)");
	});

	public static final Codec<CullingRule> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(CullingRule::text),
			Codec.STRING.listOf().optionalFieldOf("text_args", List.of()).forGetter(CullingRule::textArgs),
			Codec.BOOL.optionalFieldOf("base", false).forGetter(CullingRule::base),
			Codec.intRange(0, 1_000_000).optionalFieldOf("cost").forGetter(CullingRule::cost),
			Codec.intRange(0, 1000).optionalFieldOf("duration_days", 0).forGetter(CullingRule::durationDays),
			Codec.intRange(0, 10_000).optionalFieldOf("order", 100).forGetter(CullingRule::order),
			LegacyIds.CULLING_RULE_ID.listOf().optionalFieldOf("conflicts", List.of()).forGetter(CullingRule::conflicts),
			Clause.CODEC.listOf(1, 16).fieldOf("clauses").forGetter(CullingRule::clauses),
			EDICT.optionalFieldOf("warden_edict").forGetter(CullingRule::wardenEdict),
			Codec.BOOL.optionalFieldOf("binds_warden", false).forGetter(CullingRule::bindsWarden)
	).apply(i, CullingRule::new));

	/** Points to add this rule on this server. */
	public int effectiveCost() {
		return cost.orElse(JjkConfig.server().cullingGame.ruleCost);
	}

	/** True if some clause lets a participant ask for {@code action}. */
	public boolean allows(RuleEvent.Action action) {
		for (Clause clause : clauses) {
			if (clause.when() instanceof RuleTriggers.Action a && a.action() == action) {
				return true;
			}
		}

		return false;
	}

	public Optional<WardenMechanic.Edict> edict() {
		return wardenEdict.flatMap(key -> {
			for (WardenMechanic.Edict edict : WardenMechanic.Edict.values()) {
				if (edict.key().equals(key)) {
					return Optional.of(edict);
				}
			}

			return Optional.empty();
		});
	}
}
