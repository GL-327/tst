package com.jjkcraft.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import com.jjkcraft.grade.Grades;
import com.jjkcraft.registry.JjkRegistries;

/** Lookups over the synced domain registry. */
public final class Domains {
	private Domains() {
	}

	public record Entry(Identifier id, DomainDefinition definition) {
	}

	public static Optional<Registry<DomainDefinition>> registry(RegistryAccess access) {
		return access.lookup(JjkRegistries.DOMAIN);
	}

	public static Optional<DomainDefinition> get(RegistryAccess access, Identifier id) {
		return registry(access).flatMap(r -> r.getOptional(id));
	}

	/** Every domain, ordered by grade then id. */
	public static List<Entry> all(RegistryAccess access) {
		return registry(access).map(r -> r.listElements()
				.map(h -> new Entry(h.key().identifier(), h.value()))
				.sorted(Comparator.<Entry>comparingInt(e -> Grades.rank(access, e.definition().grade()))
						.thenComparing(e -> e.id().toString()))
				.toList()).orElse(List.of());
	}
}
