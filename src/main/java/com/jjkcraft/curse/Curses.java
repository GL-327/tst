package com.jjkcraft.curse;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import com.jjkcraft.grade.Grades;
import com.jjkcraft.registry.JjkRegistries;

/** Lookups over the synced curse registry. */
public final class Curses {
	private Curses() {
	}

	public record Entry(Identifier id, CurseDefinition definition) {
	}

	public static Optional<Registry<CurseDefinition>> registry(RegistryAccess access) {
		return access.lookup(JjkRegistries.CURSE);
	}

	public static Optional<CurseDefinition> get(RegistryAccess access, Identifier id) {
		return registry(access).flatMap(r -> r.getOptional(id));
	}

	/** The first curse definition that spawns as {@code type}. Curses and entity types usually share an id. */
	public static Optional<Entry> forEntityType(RegistryAccess access, EntityType<?> type) {
		Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		Optional<Entry> sameId = get(access, typeId).map(def -> new Entry(typeId, def));
		if (sameId.isPresent() && sameId.get().definition().entity().equals(typeId)) {
			return sameId;
		}

		return all(access).stream().filter(e -> e.definition().entity().equals(typeId)).findFirst();
	}

	/** Every curse, ordered by grade then id (codex order). */
	public static List<Entry> all(RegistryAccess access) {
		return registry(access).map(r -> r.listElements()
				.map(h -> new Entry(h.key().identifier(), h.value()))
				.sorted(Comparator.<Entry>comparingInt(e -> Grades.rank(access, e.definition().grade()))
						.thenComparing(e -> e.id().toString()))
				.toList()).orElse(List.of());
	}

	/** Codex progress counter key for a curse. */
	public static String codexKey(Identifier curse) {
		return "codex/" + curse;
	}
}
