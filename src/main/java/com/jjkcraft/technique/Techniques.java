package com.jjkcraft.technique;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import com.jjkcraft.grade.Grades;
import com.jjkcraft.registry.JjkRegistries;

/** Lookups over the synced technique registry. */
public final class Techniques {
	private Techniques() {
	}

	public record Entry(Identifier id, TechniqueDefinition definition) {
	}

	public static Optional<Registry<TechniqueDefinition>> registry(RegistryAccess access) {
		return access.lookup(JjkRegistries.TECHNIQUE);
	}

	public static Optional<TechniqueDefinition> get(RegistryAccess access, Identifier id) {
		return registry(access).flatMap(r -> r.getOptional(id));
	}

	/** Every technique, ordered by grade requirement then id. */
	public static List<Entry> all(RegistryAccess access) {
		return registry(access).map(r -> r.listElements()
				.map(h -> new Entry(h.key().identifier(), h.value()))
				.sorted(Comparator.<Entry>comparingInt(e -> Grades.rank(access, e.definition().grade()))
						.thenComparing(e -> e.id().toString()))
				.toList()).orElse(List.of());
	}

	public static int gradeRank(RegistryAccess access, TechniqueDefinition definition) {
		return Grades.rank(access, definition.grade());
	}
}
