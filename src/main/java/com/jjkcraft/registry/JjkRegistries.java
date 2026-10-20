package com.jjkcraft.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.rule.CullingRule;
import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.domain.DomainDefinition;
import com.jjkcraft.vow.VowDefinition;
import com.jjkcraft.energy.EnergyProfile;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.technique.TechniqueDefinition;

/**
 * Data-driven content registries. Entries load from {@code data/<namespace>/jjkcraft/<registry>/<name>.json},
 * so a datapack can add content without code. They load with the world; {@code /reload} does not
 * refresh them (see docs/DESIGN.md 3.1).
 */
public final class JjkRegistries {
	/** Server only: CE capacity for vanilla and modded mobs. */
	public static final ResourceKey<Registry<EnergyProfile>> ENERGY_PROFILE = key("energy_profile");
	/** Synced: the grade ladder. */
	public static final ResourceKey<Registry<GradeDefinition>> GRADE = key("grade");
	/** Synced: cursed techniques. */
	public static final ResourceKey<Registry<TechniqueDefinition>> TECHNIQUE = key("technique");
	/** Synced: curses (stats, spawning, model keys, codex). */
	public static final ResourceKey<Registry<CurseDefinition>> CURSE = key("curse");
	/** Synced: Domain Expansions. */
	public static final ResourceKey<Registry<DomainDefinition>> DOMAIN = key("domain");
	/** Synced: Binding Vow restrictions. */
	public static final ResourceKey<Registry<VowDefinition>> VOW = key("vow");
	/** Synced: Culling Game rules (the map lists them). */
	public static final ResourceKey<Registry<CullingRule>> CULLING_RULE = key("culling_rule");
	/** Synced: cursed tools (grade, granted technique, on-hit effect) for the mod's tool items. */
	public static final ResourceKey<Registry<com.jjkcraft.item.tool.CursedToolDefinition>> CURSED_TOOL = key("cursed_tool");

	private JjkRegistries() {
	}

	public static void init() {
		com.jjkcraft.awakening.CharacterAwakening.register();
		com.jjkcraft.awakening.CharacterTraits.register();
		com.jjkcraft.combat.CursedCombat.register();
		com.jjkcraft.death.SoulSystem.register();
		DynamicRegistries.register(ENERGY_PROFILE, EnergyProfile.CODEC);
		DynamicRegistries.registerSynced(GRADE, GradeDefinition.CODEC);
		DynamicRegistries.registerSynced(TECHNIQUE, TechniqueDefinition.CODEC);
		DynamicRegistries.registerSynced(CURSE, CurseDefinition.CODEC);
		DynamicRegistries.registerSynced(DOMAIN, DomainDefinition.CODEC);
		DynamicRegistries.registerSynced(VOW, VowDefinition.CODEC);
		DynamicRegistries.registerSynced(CULLING_RULE, CullingRule.CODEC);
		DynamicRegistries.registerSynced(CURSED_TOOL, com.jjkcraft.item.tool.CursedToolDefinition.CODEC);
	}

	private static <T> ResourceKey<Registry<T>> key(String path) {
		return ResourceKey.createRegistryKey(JjkCraft.id(path));
	}
}
