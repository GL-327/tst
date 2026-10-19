package com.jjkcraft.grade;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import com.jjkcraft.registry.JjkRegistries;

/** Lookups over the grade registry. Works on both sides, because the registry is synced. */
public final class Grades {
	private Grades() {
	}

	public static Optional<Registry<GradeDefinition>> registry(RegistryAccess access) {
		return access.lookup(JjkRegistries.GRADE);
	}

	public static Optional<GradeDefinition> get(RegistryAccess access, Identifier id) {
		return registry(access).flatMap(r -> r.getOptional(id));
	}

	/** All grades ordered by rank, lowest first. */
	public static List<Entry> ladder(RegistryAccess access) {
		return registry(access).map(r -> r.listElements()
				.map(h -> new Entry(h.key().identifier(), h.value()))
				.sorted(Comparator.comparingInt(e -> e.definition().rank()))
				.toList()).orElse(List.of());
	}

	/** The next non-special grade above {@code current}, if any. */
	public static Optional<Entry> next(RegistryAccess access, Identifier current) {
		int rank = get(access, current).map(GradeDefinition::rank).orElse(-1);
		return ladder(access).stream()
				.filter(e -> !e.definition().special() && e.definition().rank() > rank)
				.findFirst();
	}

	/** The special grade, if a datapack defines one. */
	public static Optional<Entry> special(RegistryAccess access) {
		return ladder(access).stream().filter(e -> e.definition().special()).findFirst();
	}

	public static int rank(RegistryAccess access, Identifier id) {
		return get(access, id).map(GradeDefinition::rank).orElse(0);
	}

	public static ResourceKey<GradeDefinition> key(Identifier id) {
		return ResourceKey.create(JjkRegistries.GRADE, id);
	}

	public record Entry(Identifier id, GradeDefinition definition) {
	}
}
