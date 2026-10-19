package com.jjkcraft.modulo.data;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

import com.jjkcraft.JjkCraft;

/**
 * The Modulo data registries. They live on the server only: clients get what they show (titles,
 * objectives, lines) through the Modulo payloads. The shipped entries are in the {@code modulo}
 * namespace, for example {@code data/modulo/jjkcraft/modulo_mission/}.
 */
public final class ModuloRegistries {
	public static final ResourceKey<Registry<MissionDefinition>> MISSION = ResourceKey.createRegistryKey(JjkCraft.id("modulo_mission"));
	public static final ResourceKey<Registry<SiteDefinition>> SITE = ResourceKey.createRegistryKey(JjkCraft.id("modulo_site"));
	public static final ResourceKey<Registry<BossDefinition>> BOSS = ResourceKey.createRegistryKey(JjkCraft.id("modulo_boss"));

	/** A mission and its id. */
	public record Mission(Identifier id, MissionDefinition definition) {
	}

	private ModuloRegistries() {
	}

	public static void init() {
		DynamicRegistries.register(MISSION, MissionDefinition.CODEC);
		DynamicRegistries.register(SITE, SiteDefinition.CODEC);
		DynamicRegistries.register(BOSS, BossDefinition.CODEC);
	}

	/** Every mission in play order (by {@code order}, then id). */
	public static List<Mission> missions(RegistryAccess access) {
		return access.lookup(MISSION).map(r -> r.listElements()
				.map(h -> new Mission(h.key().identifier(), h.value()))
				.sorted(Comparator.<Mission>comparingInt(m -> m.definition().order()).thenComparing(m -> m.id().toString()))
				.toList()).orElse(List.of());
	}

	public static Optional<MissionDefinition> mission(RegistryAccess access, Identifier id) {
		return access.lookup(MISSION).flatMap(r -> r.getOptional(id));
	}

	public static Optional<SiteDefinition> site(RegistryAccess access, Identifier id) {
		return access.lookup(SITE).flatMap(r -> r.getOptional(id));
	}

	public static Optional<BossDefinition> boss(RegistryAccess access, Identifier id) {
		return access.lookup(BOSS).flatMap(r -> r.getOptional(id));
	}
}
